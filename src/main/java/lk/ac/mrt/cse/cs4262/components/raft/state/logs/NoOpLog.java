package lk.ac.mrt.cse.cs4262.components.raft.state.logs;

import lombok.ToString;

/**
 * The log that does nothing.
 */
@ToString
public class NoOpLog extends BaseLog {
    /**
     * See {@link NoOpLog}.
     */
    public NoOpLog() {
        super(NO_OP_LOG);
    }
}
