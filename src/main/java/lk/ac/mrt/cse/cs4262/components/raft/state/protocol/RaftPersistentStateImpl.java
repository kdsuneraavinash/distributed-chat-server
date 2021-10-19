package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.BaseLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.DeleteIdentityLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.DeleteRoomLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.NoOpLog;
import lombok.Data;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Log4j2
public class RaftPersistentStateImpl implements RaftPersistentState {
    private static final String SAVE_FILE_PREFIX = "save.";
    private static final String SAVE_FILE_SUFFIX = ".state.json";

    private final Map<RoomId, ParticipantId> uncommittedRooms;
    private final Set<ParticipantId> uncommittedParticipants;
    private final Map<ParticipantId, RoomId> uncommittedOwners;

    private final File saveFile;
    private final Gson serializer;

    /**
     * Log entries; each entry contains command
     * for state machine, and term when entry
     * was received by leader (first index is 1).
     * This is exposed as 1-indexed.
     */
    private final List<RaftLog> raftLogs;
    /**
     * Latest term server has seen (initialized to 0
     * on first boot, increases monotonically).
     */
    @Getter
    private int currentTerm;
    /**
     * CandidateId that received vote in current
     * term (or null if none).
     */
    @Nullable
    private ServerId votedFor;

    /**
     * See {@link RaftPersistentStateImpl}.
     *
     * @param currentServerId Current Server ID.
     */
    public RaftPersistentStateImpl(ServerId currentServerId) {
        this.raftLogs = new ArrayList<>();
        this.currentTerm = 0;
        this.votedFor = null;
        this.uncommittedRooms = new HashMap<>();
        this.uncommittedParticipants = new HashSet<>();
        this.uncommittedOwners = new HashMap<>();

        String fileName = SAVE_FILE_PREFIX + currentServerId.getValue() + SAVE_FILE_SUFFIX;
        this.saveFile = new File(fileName);
        this.serializer = new Gson();
    }

    @Override
    public void initialize() {
        loadState();
    }

    @Override
    public void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
        saveState();
    }

    @Override
    public Optional<ServerId> getVotedFor() {
        return Optional.ofNullable(votedFor);
    }

    @Override
    public void setVotedFor(@Nullable ServerId votedFor) {
        this.votedFor = votedFor;
        saveState();
    }

    @Override
    public void appendLogEntry(RaftLog raftLog) {
        raftLogs.add(raftLog);
        forwardState(raftLog.getCommand());
        saveState();
        log.debug("appended uncommitted log: {}", raftLog);
    }

    @Override
    public void insertLogEntry(RaftLog raftLog, int index) {
        if (index > raftLogs.size()) {
            // If inserting at the end, we can simply append.
            raftLogs.add(raftLog);
            forwardState(raftLog.getCommand());
        } else {
            // We have to remove some log entries.
            // Decrementing index because index is 1-indexed.
            raftLogs.subList(index - 1, raftLogs.size()).clear();
            raftLogs.add(raftLog);
            // We also have to re-evaluate the logged state.
            uncommittedParticipants.clear();
            uncommittedRooms.clear();
            raftLogs.stream().map(RaftLog::getCommand)
                    .forEach(this::forwardState);
        }
        saveState();
        log.debug("inserted uncommitted log at {}: {}", index, raftLog);
        log.traceExit("loggedParticipants={} loggedRooms={}");
    }

    @Override
    public RaftLog getLogEntry(int index) {
        // Decrementing index because index is 1-indexed.
        return raftLogs.get(index - 1);
    }

    @Override
    public int getLogTermOf(int index) {
        // log term of 0th log entry (non-existent) is 0.
        if (index == 0) {
            return 0;
        }
        return getLogEntry(index).getTerm();
    }

    @Override
    public int getLogSize() {
        return raftLogs.size();
    }

    @Override
    public boolean isAcceptable(BaseLog baseLog) {
        if (baseLog instanceof NoOpLog) {
            // No-op always accepted.
            return true;
        } else if (baseLog instanceof CreateIdentityLog) {
            // Identity must not exist.
            CreateIdentityLog createIdentityLog = (CreateIdentityLog) baseLog;
            return !uncommittedParticipants.contains(createIdentityLog.getIdentity());
        } else if (baseLog instanceof DeleteIdentityLog) {
            // Identity deletion always accepted.
            return true;
        } else if (baseLog instanceof CreateRoomLog) {
            // Room must not exist and owner must be free.
            CreateRoomLog createRoomLog = (CreateRoomLog) baseLog;
            return !uncommittedRooms.containsKey(createRoomLog.getRoomId())
                    && !uncommittedOwners.containsKey(createRoomLog.getParticipantId());
        } else if (baseLog instanceof DeleteRoomLog) {
            // Room must exist.
            DeleteRoomLog deleteRoomLog = (DeleteRoomLog) baseLog;
            return uncommittedRooms.containsKey(deleteRoomLog.getRoomId());
        }
        return false;
    }

    @Synchronized
    private void loadState() {
        if (saveFile.exists() && saveFile.isFile()) {
            try (FileReader fileReader = new FileReader(saveFile)) {
                SerializedState previousState = serializer.fromJson(fileReader, SerializedState.class);
                this.currentTerm = previousState.getCurrentTerm();
                this.votedFor = new ServerId(previousState.getVotedFor());
                for (RaftLog raftLog : previousState.getRaftLogs()) {
                    appendLogEntry(raftLog);
                }
            } catch (IOException e) {
                // Grave Error XP
                log.fatal("state loading failed!!!");
                log.throwing(e);
            }
        }
    }

    @Synchronized
    private void saveState() {
        try (FileWriter fileWriter = new FileWriter(saveFile)) {
            String votedForStr = votedFor != null ? votedFor.getValue() : "";
            serializer.toJson(new SerializedState(raftLogs, currentTerm, votedForStr), fileWriter);
        } catch (IOException e) {
            // Grave Error XP
            log.fatal("state saving failed!!!");
            log.throwing(e);
        }
    }

    /**
     * @param baseLog Log to apply for volatile state.
     */
    private void forwardState(BaseLog baseLog) {
        if (baseLog instanceof CreateIdentityLog) {
            // Create identity.
            CreateIdentityLog createIdentityLog = (CreateIdentityLog) baseLog;
            uncommittedParticipants.add(createIdentityLog.getIdentity());
        } else if (baseLog instanceof DeleteIdentityLog) {
            // Delete identity and room if there were any owned rooms.
            DeleteIdentityLog deleteIdentityLog = (DeleteIdentityLog) baseLog;
            uncommittedParticipants.remove(deleteIdentityLog.getIdentity());
            Optional.ofNullable(uncommittedOwners.remove(deleteIdentityLog.getIdentity()))
                    .ifPresent(uncommittedRooms::remove);
        } else if (baseLog instanceof CreateRoomLog) {
            // Create room and add owner.
            CreateRoomLog createRoomLog = (CreateRoomLog) baseLog;
            uncommittedRooms.put(createRoomLog.getRoomId(), createRoomLog.getParticipantId());
            uncommittedOwners.put(createRoomLog.getParticipantId(), createRoomLog.getRoomId());
        } else if (baseLog instanceof DeleteRoomLog) {
            // Delete room and owner information.
            DeleteRoomLog deleteRoomLog = (DeleteRoomLog) baseLog;
            Optional.ofNullable(uncommittedRooms.remove(deleteRoomLog.getRoomId()))
                    .ifPresent(uncommittedOwners::remove);
        }
    }

    /**
     * Helper class for serialization.
     */
    @Data
    private static final class SerializedState {
        private final List<RaftLog> raftLogs;
        private final int currentTerm;
        private final String votedFor;
    }
}
