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
    private static final int NEXT_INDEX_INITIAL = 1;
    private static final int MATCH_INDEX_INITIAL = 0;

    /**
     * Index of next log entry to send to peer.
     */
    private final Map<ServerId, Integer> nextIndices;

    /**
     * Index of the highest log entry known to be replicated.
     */
    private final Map<ServerId, Integer> matchIndices;

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
     * Index of the highest log entry known to be committed.
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
            nextIndices.put(serverId, NEXT_INDEX_INITIAL);
            matchIndices.put(serverId, MATCH_INDEX_INITIAL);
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
        return Optional.ofNullable(nextIndices.get(serverId))
                .orElse(NEXT_INDEX_INITIAL);
    }

    @Override
    public void setNextIndex(ServerId serverId, int nextIndex) {
        nextIndices.put(serverId, nextIndex);
    }

    @Override
    public int getMatchIndex(ServerId serverId) {
        return Optional.ofNullable(matchIndices.get(serverId))
                .orElse(MATCH_INDEX_INITIAL);
    }

    @Override
    public void setMatchIndex(ServerId serverId, int matchIndex) {
        matchIndices.put(serverId, matchIndex);
    }
}
