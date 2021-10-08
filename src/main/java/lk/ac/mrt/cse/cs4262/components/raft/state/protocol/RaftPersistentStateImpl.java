package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftLog;
import lombok.Getter;
import lombok.Synchronized;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RaftPersistentStateImpl implements RaftPersistentState {
    /**
     * Log entries.
     */
    private final List<RaftLog> log;

    /**
     * Latest term server has seen (initialized to 0 on first boot).
     */
    @Getter
    private int currentTerm;

    /**
     * ID of the candidate that received vote in current
     * term (or null if none).
     */
    @Nullable
    private ServerId votedFor;

    /**
     * See {@link RaftPersistentStateImpl}.
     */
    public RaftPersistentStateImpl() {
        this.log = new ArrayList<>();
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
    public void addLogEntry(RaftLog raftLog) {
        log.add(raftLog);
        saveState();
    }

    @Override
    public int getLastLogTerm() {
        if (log.isEmpty()) {
            return 0;
        }
        return log.get(log.size() - 1).getTerm();
    }

    @Override
    public int getLastLogIndex() {
        return log.size();
    }

    @Synchronized
    private void saveState() {
        // TODO: Save this state
    }
}
