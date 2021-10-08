package lk.ac.mrt.cse.cs4262.components.gossip.state;

import java.util.Map;

/**
 * A read/write view into gossip state.
 */
public interface GossipState extends GossipStateReadView {
    /**
     * Initialize the gossip state.
     */
    void initialize();

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
