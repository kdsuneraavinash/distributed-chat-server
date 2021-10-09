package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * Non-persistent state of server.
 */
public class RaftCommonStateImpl implements RaftCommonState {
    /**
     * Current state; could be leader, candidate, follower.
     */
    @Getter
    @Setter
    private NodeState state;

    /**
     * ID of the leader.
     */
    @Nullable
    private ServerId leaderId;

    /**
     * Index of highest log entry known to be
     * committed (initialized to 0, increases
     * monotonically).
     */
    @Getter
    @Setter
    private int commitIndex;

    /**
     * See {@link RaftCommonStateImpl}.
     */
    public RaftCommonStateImpl() {
        this.state = NodeState.FOLLOWER;
        this.leaderId = null;
        this.commitIndex = 0;
    }

    @Override
    public Optional<ServerId> getLeaderId() {
        return Optional.ofNullable(leaderId);
    }

    @Override
    public void setLeaderId(@NonNull ServerId serverId) {
        this.leaderId = serverId;
    }
}
