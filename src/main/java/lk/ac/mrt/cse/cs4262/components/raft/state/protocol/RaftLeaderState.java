package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;

public interface RaftLeaderState {
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
