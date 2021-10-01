package lk.ac.mrt.cse.cs4262.components.raft.state;

import lk.ac.mrt.cse.cs4262.ServerConfiguration;

/**
 * The State containing the primary system state.
 * Will contain all the servers, participants and rooms.
 * This is only updated via log entries and will be persisted.
 */
public interface RaftState extends RaftStateReadView {
    /**
     * Initializes state by applying all persisted logs.
     *
     * @param serverConfiguration System configuration.
     */
    void initialize(ServerConfiguration serverConfiguration);

    /**
     * Adds a log to the state. This can alter the state.
     * Any changes done to the state will be persisted.
     *
     * @param logEntry Log entry to apply to the system.
     */
    void commit(RaftLog logEntry);
}
