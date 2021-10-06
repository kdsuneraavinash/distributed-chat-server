package lk.ac.mrt.cse.cs4262.components.raft;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.tcp.TcpClient;
import lk.ac.mrt.cse.cs4262.common.tcp.server.shared.SharedTcpRequestHandler;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lk.ac.mrt.cse.cs4262.components.raft.controller.RaftController;
import lk.ac.mrt.cse.cs4262.components.raft.controller.RaftControllerImpl;
import lk.ac.mrt.cse.cs4262.components.raft.controller.RaftMessageSender;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.AppendReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.AppendRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.BaseRaftMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.CommandRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.VoteReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.VoteRequestMessage;
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
        try {
            BaseRaftMessage baseRaftMessage = serializer.fromJson(request, BaseRaftMessage.class);
            log.info("{} -> {}", baseRaftMessage.getSenderId(), request);
            if (baseRaftMessage instanceof VoteRequestMessage) {
                raftController.handleVoteRequest((VoteRequestMessage) baseRaftMessage);
            } else if (baseRaftMessage instanceof VoteReplyMessage) {
                raftController.handleVoteReply((VoteReplyMessage) baseRaftMessage);
            } else if (baseRaftMessage instanceof CommandRequestMessage) {
                raftController.handleCommandRequest((CommandRequestMessage) baseRaftMessage);
            } else if (baseRaftMessage instanceof AppendRequestMessage) {
                raftController.handleAppendRequest((AppendRequestMessage) baseRaftMessage);
            } else if (baseRaftMessage instanceof AppendReplyMessage) {
                raftController.handleAppendReply((AppendReplyMessage) baseRaftMessage);
            } else {
                return Optional.empty();
            }
            return Optional.of("ok");
        } catch (JsonSyntaxException e) {
            return Optional.empty();
        }
    }

    @Override
    public void sendToServer(ServerId serverId, BaseRaftMessage message) {
        log.info("{} <- {}", serverId, serializer.toJson(message));
        String serverAddress = serverConfiguration.getServerAddress(serverId).orElseThrow();
        int coordinationPort = serverConfiguration.getCoordinationPort(serverId).orElseThrow();
        TcpClient.requestIgnoreErrors(serverAddress, coordinationPort, serializer.toJson(message));
    }
}
