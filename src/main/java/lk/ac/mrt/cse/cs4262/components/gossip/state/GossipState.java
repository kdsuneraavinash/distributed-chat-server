package lk.ac.mrt.cse.cs4262.components.gossip.state;

import lk.ac.mrt.cse.cs4262.ServerConfiguration;

import java.util.Map;

/**
 * A read/write view into gossip state.
 */
public interface GossipState extends GossipStateReadView {
    /**
     * Initialize the gossip state.
     *
     * @param serverConfiguration Server configuration
     */
    void initialize(ServerConfiguration serverConfiguration);

    /**
     * Update the gossip state.
     *
     * @param newHeartBeatCounters New state to use.
     */
    void updateHeartBeatCounter(Map<String, Integer> newHeartBeatCounters);

    /**
     * Increment the self heart beat counter.
     */
    void incrementCurrentHeartBeatCount();
}
