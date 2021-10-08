package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftLog;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
public class RaftPersistentStateImpl implements RaftPersistentState {
    /**
     * Log entries; each entry contains command
     * for state machine, and term when entry
     * was received by leader (first index is 1).
     * This is exposed as 1-indexed.
     */
    private final List<RaftLog> raftLogs;

    /**
     * Latest term server has seen (initialized to 0
     * on first boot, increases monotonically).
     */
    @Getter
    private int currentTerm;

    /**
     * CandidateId that received vote in current
     * term (or null if none).
     */
    @Nullable
    private ServerId votedFor;

    /**
     * See {@link RaftPersistentStateImpl}.
     */
    public RaftPersistentStateImpl() {
        this.raftLogs = new ArrayList<>();
        this.currentTerm = 0;
        this.votedFor = null;
    }

    @Synchronized
    @Override
    public void initialize() {
        // TODO: Load previously saved state
    }

    @Override
    public void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
        saveState();
    }

    @Override
    public Optional<ServerId> getVotedFor() {
        return Optional.ofNullable(votedFor);
    }

    @Override
    public void setVotedFor(@Nullable ServerId votedFor) {
        this.votedFor = votedFor;
        saveState();
    }

    @Override
    public void appendLogEntry(RaftLog raftLog) {
        raftLogs.add(raftLog);
        saveState();
    }

    @Override
    public void insertLogEntry(RaftLog raftLog, int index) {
        // Decrementing index because index is 1-indexed.
        raftLogs.subList(index - 1, raftLogs.size()).clear();
        raftLogs.add(raftLog);
        log.info("new logs: {}", raftLogs);
        saveState();
    }

    @Override
    public RaftLog getLogEntry(int index) {
        // Decrementing index because index is 1-indexed.
        return raftLogs.get(index - 1);
    }

    @Override
    public int getLogTermOf(int index) {
        // log term of 0th log entry (non-existent) is 0.
        if (index == 0) {
            return 0;
        }
        return getLogEntry(index).getTerm();
    }

    @Override
    public int getLogSize() {
        return raftLogs.size();
    }

    @Synchronized
    private void saveState() {
        // TODO: Save this state
    }
}
