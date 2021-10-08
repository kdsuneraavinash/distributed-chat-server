package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;

import java.util.Optional;

public interface RaftCommonState {
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
}
