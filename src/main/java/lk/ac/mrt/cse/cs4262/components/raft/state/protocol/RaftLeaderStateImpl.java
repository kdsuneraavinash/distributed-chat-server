package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;

import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Non-persistent state of server.
 */
public class RaftLeaderStateImpl implements RaftLeaderState {
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

    /**
     * See {@link RaftLeaderStateImpl}.
     *
     * @param lastLogIndex        Index of last log.
     * @param serverConfiguration Configuration of all servers.
     */
    public RaftLeaderStateImpl(int lastLogIndex, ServerConfiguration serverConfiguration) {
        this.nextIndices = new HashMap<>();
        this.matchIndices = new HashMap<>();
        serverConfiguration.allServerIds().forEach(serverId -> {
            nextIndices.put(serverId, lastLogIndex + 1);
            matchIndices.put(serverId, 0);
        });
    }

    /**
     * See {@link RaftLeaderStateImpl}.
     * Next indices will be set to 1.
     *
     * @param serverConfiguration Configuration of all servers.
     */
    public RaftLeaderStateImpl(ServerConfiguration serverConfiguration) {
        this(0, serverConfiguration);
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
