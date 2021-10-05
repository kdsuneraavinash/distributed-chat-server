package lk.ac.mrt.cse.cs4262.components.raft;

import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.tcp.TcpClient;
import lk.ac.mrt.cse.cs4262.components.raft.messages.RaftMessageDeserializer;
import lk.ac.mrt.cse.cs4262.components.raft.messages.requests.RequestVoteReqRequest;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftState;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.tcp.server.shared.SharedTcpRequestHandler;
import lk.ac.mrt.cse.cs4262.common.utils.TimedInvoker;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A component that runs RAFT protocol.
 */
@Log4j2
public class RaftComponent implements ServerComponent, SharedTcpRequestHandler, TimedInvoker.EventHandler {
    private static final int RAFT_INITIAL_DELAY_MS = 1000;
    private static final int RAFT_PERIOD_MS = 5000;
    private static final int RAFT_REQUEST_TIMEOUT = 1000;

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

    /*
    ========================================================
    Helpers
    ========================================================
     */

    private void startElection(){
        raftState.setToCandidate();
        int votes = requestVotes();

        // TODO: check if  election is cancelled
        if (hasMajority((votes))) {
            raftState.setToLeader();
        }
        else {
            // TODO: handle else
        }
    }

    private int requestVotes() {
        RequestVoteReqRequest req = RequestVoteReqRequest.builder()
                .term(raftState.incrementTerm())
                .candidateId(currentServerId)
                .lastLogIndex(0)
                .lastLogTerm(0).build();

        Map<ServerId, String> res = sendToAll(req.toString());
        // TODO: deserialize responses and get vote count
        int votes = 5;
        return votes;
    }


    private Map<ServerId, String> sendToAll(String message) {
        Map<ServerId, String> responses = new HashMap<>();
        serverConfiguration.allServerIds().forEach((serverId -> {
            Optional<Integer> coordPort = serverConfiguration.getCoordinationPort(serverId);
            Optional<String> serverAddress = serverConfiguration.getServerAddress(serverId);

            String res = "";
            if (coordPort.isPresent() && serverAddress.isPresent()) {
                try {
                    res = TcpClient.request(serverAddress.get(), coordPort.get(), message, RAFT_REQUEST_TIMEOUT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            responses.put(serverId, res);
        }));
        return responses;
    }

    public boolean hasMajority(int numVotes) {
        int tot = serverConfiguration.allServerIds().toArray().length;
        return numVotes > tot / 2;
    }
}
