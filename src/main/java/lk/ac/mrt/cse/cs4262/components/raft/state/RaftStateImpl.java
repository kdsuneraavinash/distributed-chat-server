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

import java.util.Collection;
import java.util.Optional;

/**
 * Implementation for the {@link RaftState}.
 */
@ToString(onlyExplicitlyIncluded = true)
@Log4j2
public class RaftStateImpl implements RaftState {
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
    private final RaftStateStructure state;

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
        this.state = new RaftStateStructure();
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
            state.createServer(serverId);
            state.createRoom(systemUserId, mainRoomId);
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
    public boolean hasRoom(RoomId roomId) {
        return state.hasRoom(roomId);
    }

    @Override
    public Optional<RoomId> getRoomOwnedByParticipant(ParticipantId participantId) {
        return state.getRoomOwnedByParticipant(participantId);
    }

    @Override
    public Collection<ParticipantId> getParticipantsInServer(ServerId serverId) {
        return state.getParticipantsInServer(serverId);
    }

    @Override
    public Collection<RoomId> getRoomsInSystem() {
        return state.getRoomsInSystem();
    }

    @Override
    public ParticipantId getOwnerOfRoom(RoomId roomId) {
        return state.getOwnerOfRoom(roomId);
    }

    @Override
    public ServerId getServerOfRoom(RoomId roomId) {
        return state.getServerOfRoom(roomId);
    }

    @Override
    public ParticipantId getSystemUserId(ServerId serverId) {
        return state.getSystemUserId(serverId);
    }

    @Override
    public RoomId getMainRoomId(ServerId serverId) {
        return state.getMainRoomId(serverId);
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
        state.createParticipant(serverId, participantId);
        if (currentServerId.equals(serverId) && eventHandler != null) {
            eventHandler.participantIdCreated(participantId);
        }
    }

    private void applyCreateRoomLog(CreateRoomLog logEntry) {
        ParticipantId participantId = logEntry.getParticipantId();
        RoomId roomId = logEntry.getRoomId();
        state.createRoom(participantId, roomId);
        ServerId serverId = state.getServerOfParticipant(participantId);
        if (currentServerId.equals(serverId) && eventHandler != null) {
            eventHandler.roomIdCreated(participantId, roomId);
        }
    }

    private void applyDeleteIdentityLog(DeleteIdentityLog logEntry) {
        ParticipantId participantId = logEntry.getIdentity();
        ServerId serverId = state.getServerOfParticipant(participantId);
        RoomId ownedRoomId = state.getRoomOwnedByParticipant(participantId).orElse(null);
        state.deleteParticipant(participantId);
        if (currentServerId.equals(serverId) && eventHandler != null) {
            eventHandler.participantIdDeleted(participantId, ownedRoomId);
        }
    }

    private void applyDeleteRoomLog(DeleteRoomLog logEntry) {
        RoomId roomId = logEntry.getRoomId();
        ParticipantId ownerId = state.getOwnerOfRoom(roomId);
        ServerId serverId = state.getServerOfParticipant(ownerId);
        state.deleteRoom(roomId);
        if (currentServerId.equals(serverId) && eventHandler != null) {
            eventHandler.roomIdDeleted(roomId, ownerId);
        }
    }

    private void applyServerChangeLog(ServerChangeLog logEntry) {
        ParticipantId participantId = logEntry.getParticipantId();
        ServerId fromServerId = logEntry.getFormerServerId();
        ServerId toServerId = logEntry.getNewServerId();
        state.moveParticipant(participantId, toServerId);
        if (currentServerId.equals(fromServerId) && eventHandler != null) {
            eventHandler.participantMoved(participantId);
        }
        if (currentServerId.equals(toServerId) && eventHandler != null) {
            eventHandler.participantJoined(participantId, toServerId);
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
                log.debug("setting commit index, from={}, to={}", currentCommitIndex, commitIndex);
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
            return state.hasServer(newServerId)
                    && state.hasParticipant(participantId)
                    && formerServerId.equals(state.getServerOfParticipant(participantId));
        } else {
            return persistentState.isAcceptable(baseLog);
        }
    }
}
