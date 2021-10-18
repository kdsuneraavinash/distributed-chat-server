package lk.ac.mrt.cse.cs4262.components.raft.state;

import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.BaseLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.DeleteIdentityLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.DeleteRoomLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.NoOpLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.ServerChangeLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.protocol.NodeState;
import lk.ac.mrt.cse.cs4262.components.raft.state.protocol.RaftCommonState;
import lk.ac.mrt.cse.cs4262.components.raft.state.protocol.RaftCommonStateImpl;
import lk.ac.mrt.cse.cs4262.components.raft.state.protocol.RaftLeaderState;
import lk.ac.mrt.cse.cs4262.components.raft.state.protocol.RaftLeaderStateImpl;
import lk.ac.mrt.cse.cs4262.components.raft.state.protocol.RaftPersistentState;
import lk.ac.mrt.cse.cs4262.components.raft.state.protocol.RaftPersistentStateImpl;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation for the {@link RaftState}.
 */
@ToString(onlyExplicitlyIncluded = true)
@Log4j2
public class RaftStateImpl implements RaftState {
    /**
     * A reserved participant name prefix for SYSTEM user.
     * This is the owner main rooms for each server.
     */
    private static final String SYSTEM_USER_PREFIX = "SYSTEM-";
    /**
     * A reserved room name prefix for Main Rooms.
     * This is the default room for each server.
     */
    private static final String MAIN_ROOM_PREFIX = "MainHall-";

    /**
     * ID of current server.
     */
    private final ServerId currentServerId;
    /**
     * Map for each server containing its participants.
     * If the participant owns a room, it will also be recorded.
     * This is the primary state object.
     * Other objects are simply for performance-sake.
     */
    @ToString.Include
    private final Map<ServerId, HashMap<ParticipantId, @Nullable RoomId>> state;

    /**
     * Map for each participant to record the server that they are part of.
     * This is a derived value of {@code state}. Used for reverse relations.
     */
    @ToString.Include
    private final Map<ParticipantId, ServerId> participantServerMap;
    /**
     * Map for each room to record their owner.
     * This is a derived value of {@code state}. Used for reverse relations.
     */
    @ToString.Include
    private final Map<RoomId, ParticipantId> roomOwnerMap;
    private final ServerConfiguration serverConfiguration;
    private final RaftPersistentState persistentState;
    private final RaftCommonState commonState;
    private RaftLeaderState leaderState;

    /**
     * Listener to attach for the changes in the system.
     */
    @Nullable
    private EventHandler eventHandler;

    /**
     * Create a system state. See {@link RaftStateImpl}.
     *
     * @param currentServerId     Current Server ID.
     * @param serverConfiguration Server configuration obj.
     */
    public RaftStateImpl(ServerId currentServerId, ServerConfiguration serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
        this.currentServerId = currentServerId;
        this.state = new HashMap<>();
        this.participantServerMap = new HashMap<>();
        this.roomOwnerMap = new HashMap<>();
        this.persistentState = new RaftPersistentStateImpl(currentServerId);
        this.commonState = new RaftCommonStateImpl();
        this.leaderState = new RaftLeaderStateImpl(serverConfiguration);
    }

    @Override
    public void initialize() {
        // Add main rooms and system users
        for (ServerId serverId : serverConfiguration.allServerIds()) {
            RoomId mainRoomId = getMainRoomId(serverId);
            ParticipantId systemUserId = getSystemUserId(serverId);
            this.state.put(serverId, new HashMap<>());
            this.state.get(serverId).put(systemUserId, mainRoomId);
            this.participantServerMap.put(systemUserId, serverId);
            this.roomOwnerMap.put(mainRoomId, systemUserId);
        }

        // Restore persisted state.
        this.persistentState.initialize();
    }

    /*
    ========================================================
    Write Methods
    ========================================================
     */

    @Override
    public void performCommitIfNecessary() {
        int myCommitIndex = getCommitIndex();
        int myCurrentTerm = getCurrentTerm();

        // Condition:
        // If there exists an N such that N > commitIndex, a majority
        // of matchIndex[i] ≥ N, and log[N].term == currentTerm:
        // set commitIndex = N

        // Note: newCommitIndex is N.
        // Go from LEN to my commit index (since N > commitIndex).
        // We go from biggest to smallest since we need largest N.
        for (int newCommitIndex = getLogSize(); newCommitIndex > myCommitIndex; newCommitIndex--) {
            int newCommitTerm = getLogTermOf(newCommitIndex);
            if (newCommitTerm < myCurrentTerm) {
                // If N term is less, the going forward its going to be
                // lesser. So no need to even continue.
                break;
            } else if (newCommitTerm == myCurrentTerm) {
                // N should have the same term as leader.
                int replicatedServers = 0;
                for (ServerId serverId : serverConfiguration.allServerIds()) {
                    // The log is replicated if match index is equal or greater than n
                    if (leaderState.getMatchIndex(serverId) >= newCommitIndex) {
                        replicatedServers++;
                    }
                }
                // If the majority of servers have replicated, we can safely commit.
                if (replicatedServers > serverConfiguration.allServerIds().size() / 2) {
                    setCommitIndex(newCommitIndex);
                    break;
                }
            }
        }
    }

    private void commit(BaseLog logEntry) {
        log.info("committing log: {}", logEntry);
        if (logEntry instanceof CreateIdentityLog) {
            applyCreateIdentityLog((CreateIdentityLog) logEntry);
        } else if (logEntry instanceof CreateRoomLog) {
            applyCreateRoomLog((CreateRoomLog) logEntry);
        } else if (logEntry instanceof DeleteIdentityLog) {
            applyDeleteIdentityLog((DeleteIdentityLog) logEntry);
        } else if (logEntry instanceof DeleteRoomLog) {
            applyDeleteRoomLog((DeleteRoomLog) logEntry);
        } else if (logEntry instanceof ServerChangeLog) {
            applyServerChangeLog((ServerChangeLog) logEntry);
        } else if (logEntry instanceof NoOpLog) {
            // No-op does nothing
            log.debug("no-op log");
        } else {
            throw new UnsupportedOperationException();
        }
        log.debug("state after: {}", this);
    }

    /*
    ========================================================
    Read Methods
    ========================================================
     */

    @Override
    public boolean hasParticipant(ParticipantId participantId) {
        return participantServerMap.containsKey(participantId);
    }

    @Override
    public boolean hasRoom(RoomId roomId) {
        if (RoomId.NULL.equals(roomId)) {
            return true;
        }
        return roomOwnerMap.containsKey(roomId);
    }

    @Override
    public Optional<RoomId> getRoomOwnedByParticipant(ParticipantId participantId) {
        if (participantServerMap.containsKey(participantId)) {
            ServerId serverId = participantServerMap.get(participantId);
            if (state.containsKey(serverId)) {
                Map<ParticipantId, @Nullable RoomId> prMap = state.get(serverId);
                if (prMap.containsKey(participantId)) {
                    return Optional.ofNullable(prMap.get(participantId));
                }
            }
        }
        return Optional.empty();
    }


    @Override
    public Collection<ParticipantId> getParticipantsInServer(ServerId serverId) {
        if (state.containsKey(serverId)) {
            return new ArrayList<>(state.get(serverId).keySet());
        }
        return List.of();
    }

    @Override
    public Collection<RoomId> getRoomsInServer(ServerId serverId) {
        if (state.containsKey(serverId)) {
            return state.get(serverId).values().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public Collection<RoomId> getRoomsInSystem() {
        return Collections.unmodifiableCollection(roomOwnerMap.keySet());
    }

    @Override
    public ParticipantId getOwnerOfRoom(RoomId roomId) {
        if (roomOwnerMap.containsKey(roomId)) {
            return roomOwnerMap.get(roomId);
        }
        throw new IllegalStateException("room does not exist");
    }

    @Override
    public ServerId getServerOfRoom(RoomId roomId) {
        ParticipantId ownerId = getOwnerOfRoom(roomId);
        if (participantServerMap.containsKey(ownerId)) {
            return participantServerMap.get(ownerId);
        }
        throw new IllegalStateException("owner does not belong to a server");
    }

    @Override
    public ServerId getServerOfParticipant(ParticipantId participantId) {
        if (participantServerMap.containsKey(participantId)) {
            return participantServerMap.get(participantId);
        }
        throw new IllegalStateException("participant does not exist");
    }

    @Override
    public ParticipantId getSystemUserId(ServerId serverId) {
        return new ParticipantId(SYSTEM_USER_PREFIX + serverId.getValue());
    }

    @Override
    public RoomId getMainRoomId(ServerId serverId) {
        return new RoomId(MAIN_ROOM_PREFIX + serverId.getValue());
    }

    @Override
    public void attachListener(EventHandler newEventHandler) {
        this.eventHandler = newEventHandler;
    }

    /*
    ========================================================
    Write Helper Methods
    ========================================================
     */

    private void applyCreateIdentityLog(CreateIdentityLog logEntry) {
        ParticipantId participantId = logEntry.getIdentity();
        ServerId serverId = logEntry.getServerId();
        if (!state.containsKey(serverId)) {
            throw new IllegalStateException("unknown server id");
        }
        state.get(serverId).put(participantId, null);
        participantServerMap.put(participantId, serverId);
        if (currentServerId.equals(serverId) && eventHandler != null) {
            eventHandler.participantIdCreated(participantId);
        }
    }

    private void applyCreateRoomLog(CreateRoomLog logEntry) {
        ParticipantId participantId = logEntry.getParticipantId();
        RoomId roomId = logEntry.getRoomId();
        ServerId serverId = participantServerMap.get(participantId);
        if (serverId == null || !state.containsKey(serverId)) {
            throw new IllegalStateException("unknown server id");
        }
        state.get(serverId).put(participantId, roomId);
        roomOwnerMap.put(roomId, participantId);
        if (currentServerId.equals(serverId) && eventHandler != null) {
            eventHandler.roomIdCreated(participantId, roomId);
        }
    }

    private void applyDeleteIdentityLog(DeleteIdentityLog logEntry) {
        ParticipantId participantId = logEntry.getIdentity();
        ServerId serverId = participantServerMap.remove(participantId);
        if (serverId == null || !state.containsKey(serverId)) {
            throw new IllegalStateException("unknown server id");
        }
        RoomId ownedRoomId = state.get(serverId).remove(participantId);
        if (ownedRoomId != null) {
            roomOwnerMap.remove(ownedRoomId);
        }
        if (currentServerId.equals(serverId) && eventHandler != null) {
            eventHandler.participantIdDeleted(participantId, ownedRoomId);
        }
    }

    private void applyDeleteRoomLog(DeleteRoomLog logEntry) {
        RoomId roomId = logEntry.getRoomId();
        ParticipantId ownerId = roomOwnerMap.remove(roomId);
        if (ownerId == null) {
            throw new IllegalStateException("owner cannot be null");
        }
        ServerId serverId = participantServerMap.get(ownerId);
        if (serverId == null || !state.containsKey(serverId)) {
            throw new IllegalStateException("unknown server id");
        }
        state.get(serverId).put(ownerId, null);
        if (currentServerId.equals(serverId) && eventHandler != null) {
            eventHandler.roomIdDeleted(roomId, ownerId);
        }
    }

    private void applyServerChangeLog(ServerChangeLog logEntry) {
        ParticipantId participantId = logEntry.getParticipantId();
        ServerId former = logEntry.getFormerServerId();
        ServerId newer = logEntry.getNewServerId();
        if (!participantServerMap.containsKey(participantId)) {
            throw new IllegalStateException("Participant Unknown");
        }
        if (!state.containsKey(former)
                || !state.containsKey(newer)) {
            throw new IllegalStateException("Unknown server id");
        }
        state.get(former).remove(participantId);
        state.get(newer).put(participantId, null);
        participantServerMap.put(participantId, newer);
        if (currentServerId.equals(former) && eventHandler != null) {
            eventHandler.participantMoved(participantId);
        }
        if (currentServerId.equals(newer) && eventHandler != null) {
            eventHandler.participantJoined(participantId, newer);
        }
    }

    /*
    ========================================================
    Common Non Persistent State
    ========================================================
    */

    @Override
    public NodeState getState() {
        return commonState.getState();
    }

    @Override
    public void setState(NodeState state) {
        commonState.setState(state);
    }

    @Override
    public Optional<ServerId> getLeaderId() {
        return commonState.getLeaderId();
    }

    @Override
    public void setLeaderId(ServerId serverId) {
        if (currentServerId.equals(serverId)) {
            log.info("appointed myself as leader");
            leaderState = new RaftLeaderStateImpl(getLogSize(), serverConfiguration);
        }
        commonState.setLeaderId(serverId);
        log.info("leader set as {}", serverId);
    }

    @Override
    public int getCommitIndex() {
        return commonState.getCommitIndex();
    }

    @Override
    public void setCommitIndex(int commitIndex) {
        synchronized (commonState) {
            int currentCommitIndex = commonState.getCommitIndex();
            if (commitIndex != currentCommitIndex) {
                log.info("setting commit index, from={}, to={}", currentCommitIndex, commitIndex);
                for (int i = currentCommitIndex + 1; i <= commitIndex; i++) {
                    commit(getLogEntry(i).getCommand());
                }
                commonState.setCommitIndex(commitIndex);
            }
        }
    }

    /*
    ========================================================
    Leader Non Persistent State
    ========================================================
    */

    @Override
    public int getNextIndex(ServerId serverId) {
        return leaderState.getNextIndex(serverId);
    }

    @Override
    public void setNextIndex(ServerId serverId, int nextIndex) {
        leaderState.setNextIndex(serverId, nextIndex);
    }

    @Override
    public int getMatchIndex(ServerId serverId) {
        return leaderState.getMatchIndex(serverId);
    }

    @Override
    public void setMatchIndex(ServerId serverId, int matchIndex) {
        leaderState.setMatchIndex(serverId, matchIndex);
    }

    /*
    ========================================================
    Persistent State
    ========================================================
    */

    @Override
    public int getCurrentTerm() {
        return persistentState.getCurrentTerm();
    }

    @Override
    public void setCurrentTerm(int currentTerm) {
        persistentState.setCurrentTerm(currentTerm);
    }

    @Override
    public Optional<ServerId> getVotedFor() {
        return persistentState.getVotedFor();
    }

    @Override
    public void setVotedFor(@Nullable ServerId votedFor) {
        persistentState.setVotedFor(votedFor);
    }

    @Override
    public void appendLogEntry(RaftLog raftLog) {
        persistentState.appendLogEntry(raftLog);
    }

    @Override
    public void insertLogEntry(RaftLog raftLog, int index) {
        persistentState.insertLogEntry(raftLog, index);
    }

    @Override
    public RaftLog getLogEntry(int index) {
        return persistentState.getLogEntry(index);
    }

    @Override
    public int getLogTermOf(int index) {
        return persistentState.getLogTermOf(index);
    }

    @Override
    public int getLogSize() {
        return persistentState.getLogSize();
    }

    @Override
    public boolean isAcceptable(BaseLog baseLog) {
        if (baseLog instanceof ServerChangeLog) {
            // Handling this here since server change logs does not alter
            // participants nor rooms.
            ServerChangeLog serverChangeLog = (ServerChangeLog) baseLog;
            ParticipantId participantId = serverChangeLog.getParticipantId();
            ServerId newServerId = serverChangeLog.getNewServerId();
            ServerId formerServerId = serverChangeLog.getFormerServerId();
            return state.containsKey(newServerId)
                    && participantServerMap.containsKey(participantId)
                    && formerServerId.equals(participantServerMap.get(participantId));
        } else {
            return persistentState.isAcceptable(baseLog);
        }
    }
}
