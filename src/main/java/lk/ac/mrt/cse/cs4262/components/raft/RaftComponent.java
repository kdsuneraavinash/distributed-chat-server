package lk.ac.mrt.cse.cs4262.components.raft;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.tcp.TcpClient;
import lk.ac.mrt.cse.cs4262.common.tcp.server.shared.SharedTcpRequestHandler;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lk.ac.mrt.cse.cs4262.components.raft.controller.RaftController;
import lk.ac.mrt.cse.cs4262.components.raft.controller.RaftControllerImpl;
import lk.ac.mrt.cse.cs4262.components.raft.controller.RaftMessageSender;
import lk.ac.mrt.cse.cs4262.components.raft.messages.AppendReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.AppendRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.BaseRaftMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.CommandRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.RaftAckMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.VoteReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.VoteRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftState;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

/**
 * A component that runs RAFT protocol.
 */
@Log4j2
public class RaftComponent implements ServerComponent, SharedTcpRequestHandler, RaftMessageSender {
    private final ServerConfiguration serverConfiguration;
    private final RaftController raftController;
    private final Gson serializer;

    /**
     * Create a raft component. See {@link RaftComponent}.
     *
     * @param currentServerId     Current server id.
     * @param raftState           System global state (write view).
     * @param serverConfiguration All server configuration.
     */
    public RaftComponent(ServerId currentServerId, RaftState raftState, ServerConfiguration serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
        this.raftController = new RaftControllerImpl(currentServerId, raftState, serverConfiguration);
        this.serializer = new Gson();
    }

    @Override
    public void connect() {
        this.raftController.attachMessageSender(this);
        this.raftController.initialize();
    }

    @Override
    public void close() throws Exception {
        this.raftController.close();
    }

    /*
    ========================================================
    REQUEST HANDLER
    ========================================================
     */

    @Override
    public Optional<String> handleRequest(String request) {
        // Try to parse and if it fails, respond with unhandled
        BaseRaftMessage baseRaftMessage;
        try {
            baseRaftMessage = serializer.fromJson(request, BaseRaftMessage.class);
            log.debug("{} -> {}", baseRaftMessage.getSenderId(), request);
        } catch (Exception e) {
            return Optional.empty();
        }

        // Process parsed message
        boolean isAccepted = true;
        if (baseRaftMessage instanceof VoteRequestMessage) {
            raftController.handleVoteRequest((VoteRequestMessage) baseRaftMessage);
        } else if (baseRaftMessage instanceof VoteReplyMessage) {
            raftController.handleVoteReply((VoteReplyMessage) baseRaftMessage);
        } else if (baseRaftMessage instanceof CommandRequestMessage) {
            isAccepted = raftController.handleCommandRequest((CommandRequestMessage) baseRaftMessage);
        } else if (baseRaftMessage instanceof AppendRequestMessage) {
            raftController.handleAppendRequest((AppendRequestMessage) baseRaftMessage);
        } else if (baseRaftMessage instanceof AppendReplyMessage) {
            raftController.handleAppendReply((AppendReplyMessage) baseRaftMessage);
        } else {
            // Unknown type of raft message
            return Optional.empty();
        }
        RaftAckMessage raftAckMessage = new RaftAckMessage(isAccepted);
        return Optional.of(serializer.toJson(raftAckMessage));
    }

    @Override
    public void sendToServer(ServerId serverId, BaseRaftMessage message) {
        log.debug("{} <- {}", serverId, serializer.toJson(message));
        String serverAddress = serverConfiguration.getServerAddress(serverId).orElseThrow();
        int coordinationPort = serverConfiguration.getCoordinationPort(serverId).orElseThrow();
        TcpClient.requestIgnoreErrors(serverAddress, coordinationPort, serializer.toJson(message));
    }
}
