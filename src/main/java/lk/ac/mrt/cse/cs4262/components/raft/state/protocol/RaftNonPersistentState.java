package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;

import java.util.Optional;

public interface RaftNonPersistentState {
    /**
     * @return The server current state.
     */
    NodeState getState();

    /**
     * @param state The server current state.
     */
    void setState(NodeState state);

    /**
     * @return The current leader of system.
     */
    Optional<ServerId> getLeaderId();

    /**
     * @param serverId The current leader of system.
     */
    void setLeaderId(ServerId serverId);

    /**
     * @return highest committed log entry.
     */
    int getCommitIndex();

    /**
     * @param commitIndex highest committed log entry.
     */
    void setCommitIndex(int commitIndex);

    /**
     * @param serverId Server ID.
     * @return Next log entry to send.
     */
    int getNextIndex(ServerId serverId);

    /**
     * @param serverId  Server ID.
     * @param nextIndex Next log entry to send.
     */
    void setNextIndex(ServerId serverId, int nextIndex);

    /**
     * @param serverId Server ID.
     * @return Highest replicated log entry.
     */
    int getMatchIndex(ServerId serverId);

    /**
     * @param serverId   Server ID.
     * @param matchIndex Highest replicated log entry.
     */
    void setMatchIndex(ServerId serverId, int matchIndex);
}
