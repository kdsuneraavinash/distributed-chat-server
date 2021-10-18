package lk.ac.mrt.cse.cs4262.components.gossip;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.tcp.TcpClient;
import lk.ac.mrt.cse.cs4262.common.tcp.server.shared.SharedTcpRequestHandler;
import lk.ac.mrt.cse.cs4262.common.utils.PeriodicInvoker;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lk.ac.mrt.cse.cs4262.components.gossip.state.GossipState;
import lombok.extern.log4j.Log4j2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * A component that runs gossiping for failure detection.
 */
@Log4j2
public class GossipComponent implements ServerComponent, SharedTcpRequestHandler, PeriodicInvoker.EventHandler {
    private static final int GOSSIP_WAIT_TIMEOUT_MS = 2000;
    private static final int GOSSIP_INITIAL_DELAY_MS = 5000;
    private static final int GOSSIP_PERIOD_MS = 5000;

    private final ServerId currentServerId;
    private final ServerConfiguration serverConfiguration;
    private final PeriodicInvoker periodicInvoker;
    private final GossipState gossipState;
    private final Random randomServerPicker;
    private final Gson serializer;

    /**
     * Create a raft component. See {@link GossipComponent}.
     *
     * @param currentServerId     Current server id.
     * @param gossipState         Gossip state.
     * @param serverConfiguration All server configuration.
     */
    public GossipComponent(ServerId currentServerId, GossipState gossipState, ServerConfiguration serverConfiguration) {
        this.currentServerId = currentServerId;
        this.gossipState = gossipState;
        this.serverConfiguration = serverConfiguration;
        this.periodicInvoker = new PeriodicInvoker("gossip-timer");
        this.randomServerPicker = new Random();
        this.serializer = new Gson();
    }

    @Override
    public void connect() {
        periodicInvoker.startExecution(this, GOSSIP_INITIAL_DELAY_MS, GOSSIP_PERIOD_MS);
    }

    @Override
    public void close() throws Exception {
        periodicInvoker.close();
    }

    /*
    ========================================================
    REQUEST HANDLER
    ========================================================
     */

    @Override
    public Optional<String> handleRequest(String request) {
        // Try to parse and if it fails, respond with unhandled
        Map<String, Integer> gossip;
        try {
            gossip = serializer.fromJson(request, GossipFormat.class);
        } catch (Exception e) {
            return Optional.empty();
        }

        // Process parsed message
        gossipState.updateHeartBeatCounter(gossip);
        return Optional.of(gossipState.toJson(serializer));
    }

    /*
    ========================================================
    TIMED INVOKER
    ========================================================
     */

    @Override
    public void handleTimedEvent() {
        Collection<ServerId> serverIds = serverConfiguration.allServerIds();
        Optional<ServerId> serverIdOp = serverIds.stream()
                .skip(randomServerPicker.nextInt(serverIds.size()))
                .filter(serverId -> !currentServerId.equals(serverId))
                .findFirst();
        gossipState.incrementCurrentHeartBeatCount();
        if (serverIdOp.isPresent()) {
            ServerId serverId = serverIdOp.get();
            try {
                String ipAddress = serverConfiguration.getServerAddress(serverId);
                int coordPort = serverConfiguration.getCoordinationPort(serverId);
                String response = TcpClient.request(ipAddress, coordPort,
                        gossipState.toJson(serializer), GOSSIP_WAIT_TIMEOUT_MS);
                Map<String, Integer> gossip = serializer.fromJson(response, GossipFormat.class);
                gossipState.updateHeartBeatCounter(gossip);
            } catch (Exception ignored) {
                // Ignore any error when asking from a server
            }
        }
    }

    private static final class GossipFormat extends HashMap<String, Integer> {
    }
}
