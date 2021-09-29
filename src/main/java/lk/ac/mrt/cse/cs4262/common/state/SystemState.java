package lk.ac.mrt.cse.cs4262.common.state;

import lk.ac.mrt.cse.cs4262.common.state.logs.BaseLog;

/**
 * The State containing the primary system state.
 * Will contain all the servers, participants and rooms.
 * This is only updated via log entries and will be persisted.
 */
public interface SystemState extends SystemStateReadView {
    /**
     * Adds a log to the state. This can alter the state.
     * Any changes done to the state will be persisted.
     * TODO: change log entry to wrapped log entry with term.
     *
     * @param logEntry Log entry to apply to the system.
     */
    void apply(BaseLog logEntry);
}
