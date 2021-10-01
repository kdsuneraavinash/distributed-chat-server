package lk.ac.mrt.cse.cs4262.components.gossip.state;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;

import java.util.Collection;

/**
 * Interface for the read only view of gossip state.
 */
public interface GossipStateReadView {
    /**
     * Get the failed servers.
     *
     * @return Collection of all (known) failed servers.
     */
    Collection<ServerId> failedServerIds();

    /**
     * Convert the gossip state to a json format.
     *
     * @param serializer Serializer to use.
     * @return Gossip string.
     */
    String toJson(Gson serializer);
}
