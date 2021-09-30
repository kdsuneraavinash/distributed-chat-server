package lk.ac.mrt.cse.cs4262.components.gossip;

import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.tcp.server.shared.SharedTcpRequestHandler;
import lk.ac.mrt.cse.cs4262.common.utils.TimedInvoker;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

/**
 * A component that runs gossiping for failure detection.
 */
@Log4j2
public class GossipComponent implements ServerComponent, SharedTcpRequestHandler, TimedInvoker.EventHandler {
    private static final int INITIAL_DELAY_S = 5;
    private static final int PERIOD_S = 5;

    private final ServerConfiguration serverConfiguration;
    private final TimedInvoker timedInvoker;

    /**
     * Create a raft component. See {@link GossipComponent}.
     *
     * @param serverConfiguration All server configuration.
     */
    public GossipComponent(ServerConfiguration serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
        this.timedInvoker = new TimedInvoker();
    }

    @Override
    public void connect() {
        timedInvoker.startExecution(this, INITIAL_DELAY_S, PERIOD_S);
    }

    @Override
    public void close() throws Exception {
        timedInvoker.close();
    }

    /*
    ========================================================
    REQUEST HANDLER
    ========================================================
     */

    @Override
    public Optional<String> handleRequest(String request) {
        if (request.startsWith("G")) {
            log.info(serverConfiguration);
            return Optional.of("GOSSIP EH?");
        }
        return Optional.empty();
    }

    /*
    ========================================================
    TIMED INVOKER
    ========================================================
     */

    @Override
    public void handleTimedEvent() {
        // Can use TcpClient.request()
        log.trace("Ping from gossip timer");
    }
}
