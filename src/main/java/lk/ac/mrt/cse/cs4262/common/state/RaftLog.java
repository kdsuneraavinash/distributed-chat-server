package lk.ac.mrt.cse.cs4262.common.state;

import lk.ac.mrt.cse.cs4262.common.state.logs.BaseLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * The log entry with a term.
 * This is used for raft.
 */
@Getter
@ToString
@AllArgsConstructor
public class RaftLog {
    private final BaseLog command;
    private final int term;
}
