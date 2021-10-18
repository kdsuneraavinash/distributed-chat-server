package lk.ac.mrt.cse.cs4262.components.client;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.tcp.TcpClient;
import lk.ac.mrt.cse.cs4262.common.tcp.server.shared.SharedTcpRequestHandler;
import lk.ac.mrt.cse.cs4262.common.utils.NamedThreadFactory;
import lk.ac.mrt.cse.cs4262.common.utils.PeriodicInvoker;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lk.ac.mrt.cse.cs4262.components.client.chat.ChatRoomState;
import lk.ac.mrt.cse.cs4262.components.client.chat.ChatRoomWaitingList;
import lk.ac.mrt.cse.cs4262.components.client.chat.MessageSender;
import lk.ac.mrt.cse.cs4262.components.client.chat.client.ChatClient;
import lk.ac.mrt.cse.cs4262.components.client.chat.client.ChatClientImpl;
import lk.ac.mrt.cse.cs4262.components.client.chat.client.ClientSocketListener;
import lk.ac.mrt.cse.cs4262.components.client.chat.events.RaftStateEventHandler;
import lk.ac.mrt.cse.cs4262.components.client.chat.events.SocketEventHandler;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.MoveJoinValidateRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.MoveJoinValidateResponse;
import lk.ac.mrt.cse.cs4262.components.gossip.state.GossipStateReadView;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftState;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server component that handles client requests.
 * Manages the chat messages etc... among clients.
 * Command messages will be proxied to other servers.
 */
@Log4j2
public class ClientComponent implements ServerComponent, Runnable, AutoCloseable, MessageSender, PeriodicInvoker.EventHandler, SharedTcpRequestHandler {
    private static final int PROXY_TIMEOUT = 1000;
    private static final int CHECK_DELETED_ID_ROOMID_TIMEOUT = 5000;

    private final RaftStateEventHandler raftStateEventHandler;
    private final RaftState raftState;
    private final ServerConfiguration serverConfiguration;
    private final SocketEventHandler socketEventHandler;
    private final Map<ClientId, ChatClient> allClients;
    private final ChatRoomState chatRoomState;
    private final ExecutorService executorService;
    private final int port;
    private final Gson serializer;

    private final ServerId currentServerId;
    private final PeriodicInvoker periodicInvoker;


    /**
     * Create a client connector. See {@link ClientComponent}.
     *
     * @param port                Port to listen.
     * @param currentServerId     Current server id.
     * @param gossipState         Gossip read only view.
     * @param raftState           System read only view.
     * @param serverConfiguration System server information.
     */
    public ClientComponent(int port, ServerId currentServerId,
                           GossipStateReadView gossipState, RaftState raftState,
                           ServerConfiguration serverConfiguration) {
        RoomId mainRoomId = raftState.getMainRoomId(currentServerId);
        Gson serializer = new Gson();
        ChatRoomWaitingList waitingList = new ChatRoomWaitingList();

        this.port = port;
        this.allClients = new HashMap<>();
        this.raftState = raftState;
        this.chatRoomState = new ChatRoomState(mainRoomId);
        this.serializer = new Gson();
        this.socketEventHandler = SocketEventHandler.builder()
                .currentServerId(currentServerId)
                .gossipState(gossipState)
                .raftState(raftState)
                .chatRoomState(chatRoomState)
                .waitingList(waitingList)
                .serializer(serializer)
                .serverConfiguration(serverConfiguration).build();
        this.raftStateEventHandler = RaftStateEventHandler.builder()
                .mainRoomId(mainRoomId)
                .chatRoomState(chatRoomState)
                .waitingList(waitingList)
                .serializer(serializer).build();

        this.executorService = Executors.newCachedThreadPool(
                new NamedThreadFactory("client"));
        this.serverConfiguration = serverConfiguration;

        this.currentServerId = currentServerId;
        this.periodicInvoker = new PeriodicInvoker("check-deleted-id-roomId");

    }

    @Override
    public void connect() {
        socketEventHandler.attachMessageSender(this);
        raftStateEventHandler.attachMessageSender(this);
        raftState.attachListener(raftStateEventHandler);
        log.info("client component connected");

        periodicInvoker.startExecution(this, 0, CHECK_DELETED_ID_ROOMID_TIMEOUT);
    }

    @Override
    public void run() {
        log.info("starting client server on port {}", port);
        try {
            // Listen on client port and connect each new client to the manager.
            @Cleanup ServerSocket serverSocket = new ServerSocket(port);
            while (!Thread.currentThread().isInterrupted()) {
                // Create a new client from each socket connection.
                Socket socket = serverSocket.accept();
                ClientId clientId = ClientId.real();
                ChatClient client = new ChatClientImpl(clientId, socket);
                allClients.put(clientId, client);
                this.executorService.submit(new ClientSocketListener(clientId, socket, socketEventHandler));
            }
        } catch (IOException e) {
            log.error("Server socket opening failed on port {}.", port);
            log.throwing(e);
        } finally {
            this.executorService.shutdownNow();
        }
    }

    @Override
    public void close() throws Exception {
        for (ChatClient chatClient : allClients.values()) {
            chatClient.close();
        }
    }

    /*
    ========================================================
    Message Sender
    ========================================================
     */

    @Override
    public void sendToClient(ClientId clientId, String message) {
        if (allClients.containsKey(clientId)) {
            allClients.get(clientId).sendMessage(message);
        }
    }

    @Override
    public void sendToRoom(RoomId roomId, String message) {
        for (ClientId clientId : chatRoomState.getClientIdsOf(roomId)) {
            if (allClients.containsKey(clientId)) {
                allClients.get(clientId).sendMessage(message);
            }
        }
    }

    @Override
    public void sendToRoom(RoomId roomId, String message, ClientId excludeClientId) {
        for (ClientId clientId : chatRoomState.getClientIdsOf(roomId)) {
            if (!clientId.equals(excludeClientId) && allClients.containsKey(clientId)) {
                allClients.get(clientId).sendMessage(message);
            }
        }
    }

    @Override
    public void disconnect(ClientId clientId) {
        try {
            if (allClients.containsKey(clientId)) {
                ChatClient client = allClients.remove(clientId);
                if (client != null) {
                    client.close();
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public void sendToServer(ServerId serverId, String message) {
        String serverAddress = serverConfiguration.getServerAddress(serverId);
        int serverPort = serverConfiguration.getCoordinationPort(serverId);
        try {
            TcpClient.request(serverAddress, serverPort, message, PROXY_TIMEOUT);
        } catch (Exception e) {
            log.trace("sending to server failed: {}", e.toString());
        }
    }

    @Override
    public void handleTimedEvent() {
        Collection<ParticipantId> raftStateParticipantIds = raftState.getParticipantsInServer(currentServerId);
        Collection<ParticipantId> chatStateParticipantIds = chatRoomState.getAllParticipantIds();
        raftStateParticipantIds.forEach(participantId -> {
            if (!chatStateParticipantIds.contains(participantId)) {
                chatRoomState.participantDelete(participantId);
            }
        });

        Collection<RoomId> raftStateroomIds = raftState.getRoomsInServer(currentServerId);
        Collection<RoomId> chatStateRoomIds = chatRoomState.getAllRoomIds();
        raftStateroomIds.forEach(roomId -> {
            if (!chatStateRoomIds.contains(roomId)) {
                chatRoomState.roomDelete(roomId);
            }
        });
    }

    @Override
    public Optional<String> handleRequest(String request) {
        log.debug("client component movejoin handler: {}", request);
        try {
            MoveJoinValidateRequest validateRequest = serializer.fromJson(request, MoveJoinValidateRequest.class);
            ParticipantId participantId = new ParticipantId(validateRequest.getParticipantId());
            RoomId formerRoomId = new RoomId(validateRequest.getFormerRoomId());
            boolean isValid = socketEventHandler.validateMoveJoinRequest(participantId, formerRoomId);
            MoveJoinValidateResponse response = MoveJoinValidateResponse.builder()
                    .validated(isValid).build();
            return Optional.of(serializer.toJson(response));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
