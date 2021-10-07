package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftLog;
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
     * TODO: Can we just increment this?
     *
     * @param currentTerm The current term of server.
     */
    void setCurrentTerm(int currentTerm);

    /**
     * @return The voted server for this term.
     */
    Optional<ServerId> getVotedFor();

    /**
     * TODO: Can we remove nullable requirement?
     *
     * @param votedFor The voted server for this term. (null if none)
     */
    void setVotedFor(@Nullable ServerId votedFor);

    /**
     * @param raftLog Log entry to add.
     */
    void addLogEntry(RaftLog raftLog);

    /**
     * @return The latest term of the log
     */
    int getLastLogTerm();

    /**
     * @return The latest log index
     */
    int getLastLogIndex();
}
