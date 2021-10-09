package lk.ac.mrt.cse.cs4262.components.gossip.state;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.Synchronized;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The implementation of a gossip state.
 */
@Log4j2
@ToString
public class GossipStateImpl implements GossipState {
    private static final int T_FAIL = 10000;

    private final ServerId currentServerId;
    private final Map<ServerId, Integer> heartBeatCounters;
    private final Map<ServerId, Long> lastUpdatedTimestamps;
    private final ServerConfiguration serverConfiguration;

    /**
     * Create a gossip state. See {@link GossipState}.
     *
     * @param currentServerId     The server id of current server.
     * @param serverConfiguration Server configuration obj.
     */
    public GossipStateImpl(ServerId currentServerId, ServerConfiguration serverConfiguration) {
        this.currentServerId = currentServerId;
        this.serverConfiguration = serverConfiguration;
        this.heartBeatCounters = new HashMap<>();
        this.lastUpdatedTimestamps = new HashMap<>();
    }

    @Synchronized
    @Override
    public void initialize() {
        for (ServerId serverId : serverConfiguration.allServerIds()) {
            // Put 0 counter as the oldest entry (0 timestamp)
            heartBeatCounters.put(serverId, 0);
            lastUpdatedTimestamps.put(serverId, 0L);
        }
    }

    @Synchronized
    @Override
    public void updateHeartBeatCounter(Map<String, Integer> newHeartBeatCounters) {
        for (String serverIdStr : newHeartBeatCounters.keySet()) {
            ServerId serverId = new ServerId(serverIdStr);
            updateHeartBeatCounter(serverId, newHeartBeatCounters.get(serverIdStr));
        }
        log.trace("updated gossip state: {}", heartBeatCounters);
    }

    @Synchronized
    @Override
    public void incrementCurrentHeartBeatCount() {
        if (heartBeatCounters.containsKey(currentServerId)) {
            Integer currentHeartBeatCount = heartBeatCounters.get(currentServerId);
            updateCount(currentServerId, currentHeartBeatCount + 1);
        } else {
            updateCount(currentServerId, 0);
        }
    }

    @Override
    public Collection<ServerId> failedServerIds() {
        long currentTimestamp = System.currentTimeMillis();
        List<ServerId> failedServers = new ArrayList<>();
        lastUpdatedTimestamps.forEach(((serverId, lastUpdated) -> {
            if (currentTimestamp - lastUpdated > T_FAIL) {
                failedServers.add(serverId);
            }
        }));
        return failedServers;
    }

    @Override
    public String toJson(Gson serializer) {
        return serializer.toJson(heartBeatCounters);
    }

    /**
     * Update heart beat counter of a specific server.
     * Updated only if new count is higher than previous.
     *
     * @param serverId     Server ID.
     * @param updatedCount New count.
     */
    private void updateHeartBeatCounter(ServerId serverId, Integer updatedCount) {
        if (heartBeatCounters.containsKey(serverId)) {
            Integer currentHeartBeatCount = heartBeatCounters.get(serverId);
            if (currentHeartBeatCount >= updatedCount) {
                // Ignore if count is already greater than the new one
                return;
            }
        }
        updateCount(serverId, updatedCount);
    }

    /**
     * Updates count of a server and record updated timestamp.
     *
     * @param serverId     Server ID.
     * @param updatedCount New count.
     */
    private void updateCount(ServerId serverId, Integer updatedCount) {
        heartBeatCounters.put(serverId, updatedCount);
        lastUpdatedTimestamps.put(serverId, System.currentTimeMillis());
    }
}
