package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.BaseLog;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * Each server persists the following state to stable storage
 * synchronously before responding to RPCs.
 */
public interface RaftPersistentState {
    /**
     * Initialize and load persisted state.
     */
    void initialize();

    /**
     * @return The current term of server.
     */
    int getCurrentTerm();

    /**
     * @param currentTerm The current term of server.
     */
    void setCurrentTerm(int currentTerm);

    /**
     * @return The voted server for this term.
     */
    Optional<ServerId> getVotedFor();

    /**
     * @param votedFor The voted server for this term. (null if none)
     */
    void setVotedFor(@Nullable ServerId votedFor);

    /**
     * Add uncommitted log entry.
     * The added log entry is not committed but is just added.
     * The command in the log entry is also not executed.
     *
     * @param raftLog Log entry to add.
     */
    void appendLogEntry(RaftLog raftLog);

    /**
     * Inserts uncommitted log entry at an index.
     * All the logs before that index are discarded.
     * If index is same as log size, this is same as appending.
     *
     * @param raftLog Log entry to add.
     * @param index   Index to add. 1-indexed.
     */
    void insertLogEntry(RaftLog raftLog, int index);

    /**
     * @param index Log entry index to fetch. 1-indexed.
     * @return log entry.
     */
    RaftLog getLogEntry(int index);

    /**
     * Get the log term of the log of the given index.
     *
     * @param index Index to get. 1-indexed.
     * @return Log term.
     */
    int getLogTermOf(int index);

    /**
     * @return Number of logs
     */
    int getLogSize();

    /**
     * @param baseLog Log to check.
     * @return Whether the log can be safely applied.
     */
    boolean isAcceptable(BaseLog baseLog);
}
