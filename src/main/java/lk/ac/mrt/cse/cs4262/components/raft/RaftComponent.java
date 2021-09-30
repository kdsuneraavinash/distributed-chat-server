package lk.ac.mrt.cse.cs4262.components.raft;

import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.state.SystemState;
import lk.ac.mrt.cse.cs4262.common.tcp.server.shared.SharedTcpRequestHandler;
import lk.ac.mrt.cse.cs4262.common.utils.TimedInvoker;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

/**
 * A component that runs RAFT protocol.
 */
@Log4j2
public class RaftComponent implements ServerComponent, SharedTcpRequestHandler, TimedInvoker.EventHandler {
    private static final int INITIAL_DELAY_S = 5;
    private static final int PERIOD_S = 10;

    private final SystemState systemState;
    private final ServerConfiguration serverConfiguration;
    private final TimedInvoker timedInvoker;

    /**
     * Create a raft component. See {@link RaftComponent}.
     *
     * @param systemState         System global state (write view).
     * @param serverConfiguration All server configuration.
     */
    public RaftComponent(SystemState systemState, ServerConfiguration serverConfiguration) {
        this.systemState = systemState;
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
        if (request.startsWith("R")) {
            log.info(systemState);
            log.info(serverConfiguration);
            return Optional.of("WAHHH!!!");
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
        log.info("Ping from raft timer");
    }
}
