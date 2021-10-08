package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;

import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Non-persistent state of server.
 */
public class RaftNonPersistentStateImpl implements RaftNonPersistentState {
    /*
    ========================================================
    FOR LEADERS
    ========================================================
    */

    /**
     * For each server, index of the next log entry
     * to send to that server (initialized to leader
     * last log index + 1).
     * Initially initialized to 1.
     */
    private final Map<ServerId, Integer> nextIndices;

    /**
     * For each server, index of highest log entry
     * known to be replicated on server
     * (initialized to 0, increases monotonically).
     */
    private final Map<ServerId, Integer> matchIndices;

    /*
    ========================================================
    ALL SERVERS
    ========================================================
    */

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
     * See {@link RaftNonPersistentStateImpl}.
     *
     * @param serverConfiguration Configuration of all servers.
     */
    public RaftNonPersistentStateImpl(ServerConfiguration serverConfiguration) {
        this.state = NodeState.FOLLOWER;
        this.leaderId = null;
        this.commitIndex = 0;
        this.nextIndices = new HashMap<>();
        this.matchIndices = new HashMap<>();
        serverConfiguration.allServerIds().forEach(serverId -> {
            nextIndices.put(serverId, 1);
            matchIndices.put(serverId, 0);
        });
    }

    @Override
    public Optional<ServerId> getLeaderId() {
        return Optional.ofNullable(leaderId);
    }

    @Override
    public void setLeaderId(@NonNull ServerId serverId) {
        this.leaderId = serverId;
    }

    @Override
    public int getNextIndex(ServerId serverId) {
        return Optional.ofNullable(nextIndices.get(serverId)).orElseThrow();
    }

    @Override
    public void setNextIndex(ServerId serverId, int nextIndex) {
        nextIndices.put(serverId, nextIndex);
    }

    @Override
    public int getMatchIndex(ServerId serverId) {
        return Optional.ofNullable(matchIndices.get(serverId)).orElseThrow();
    }

    @Override
    public void setMatchIndex(ServerId serverId, int matchIndex) {
        matchIndices.put(serverId, matchIndex);
    }
}
