package lk.ac.mrt.cse.cs4262.components.raft.state;

import lk.ac.mrt.cse.cs4262.components.raft.state.protocol.RaftCommonState;
import lk.ac.mrt.cse.cs4262.components.raft.state.protocol.RaftLeaderState;
import lk.ac.mrt.cse.cs4262.components.raft.state.protocol.RaftPersistentState;


/**
 * The State containing the primary system state.
 * Will contain all the servers, participants and rooms.
 * This is only updated via log entries and will be persisted.
 */
public interface RaftState extends RaftStateReadView, RaftCommonState, RaftLeaderState, RaftPersistentState {
    /**
     * Initializes state by applying all persisted logs.
     */
    void initialize();

    /**
     * Adds a log to the state. This can alter the state.
     * Any changes done to the state will be persisted.
     *
     * @param logEntry Log entry to apply to the system.
     */
    @Deprecated
    void commit(RaftLog logEntry);

    /**
     * Commit log entries if necessary.
     * This checks match indices and decides to commit if it is possible to commit
     * depending on replicated logs in the system.
     */
    void performCommitIfNecessary();
}
