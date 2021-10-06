package lk.ac.mrt.cse.cs4262.components.raft;

import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.tcp.server.shared.SharedTcpRequestHandler;
import lk.ac.mrt.cse.cs4262.common.utils.TimedInvoker;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftState;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

/**
 * A component that runs RAFT protocol.
 */
@Log4j2
public class RaftComponent implements ServerComponent, SharedTcpRequestHandler, TimedInvoker.EventHandler {
    protected static final int RAFT_INITIAL_DELAY_MS = 1000;
    protected static final int RAFT_PERIOD_MS = 5000;
    protected static final int RAFT_REQUEST_TIMEOUT = 1000;

    private final ServerId currentServerId;
    private final RaftState raftState;
    private final ServerConfiguration serverConfiguration;
    private final TimedInvoker timedInvoker;

    /**
     * Create a raft component. See {@link RaftComponent}.
     *
     * @param currentServerId     Current server id.
     * @param raftState           System global state (write view).
     * @param serverConfiguration All server configuration.
     */
    public RaftComponent(ServerId currentServerId, RaftState raftState, ServerConfiguration serverConfiguration) {
        this.currentServerId = currentServerId;
        this.raftState = raftState;
        this.serverConfiguration = serverConfiguration;
        this.timedInvoker = new TimedInvoker();
    }

    @Override
    public void connect() {
        timedInvoker.startExecution(this, RAFT_INITIAL_DELAY_MS, RAFT_PERIOD_MS);
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
            log.info(currentServerId);
            log.info(raftState);
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
        // Can use TcpClient.request()
        log.trace("Ping from raft timer");
    }
}
