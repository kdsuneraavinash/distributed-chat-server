package lk.ac.mrt.cse.cs4262.components.client;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftState;
import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lk.ac.mrt.cse.cs4262.components.client.chat.ChatRoomState;
import lk.ac.mrt.cse.cs4262.components.client.chat.ChatRoomWaitingList;
import lk.ac.mrt.cse.cs4262.components.client.chat.MessageSender;
import lk.ac.mrt.cse.cs4262.components.client.chat.client.ChatClient;
import lk.ac.mrt.cse.cs4262.components.client.chat.client.ChatClientImpl;
import lk.ac.mrt.cse.cs4262.components.client.chat.client.ClientSocketListener;
import lk.ac.mrt.cse.cs4262.components.client.chat.events.RaftStateEventHandler;
import lk.ac.mrt.cse.cs4262.components.client.chat.events.SocketEventHandler;
import lk.ac.mrt.cse.cs4262.components.gossip.state.GossipState;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server component that handles client requests.
 * Manages the chat messages etc... among clients.
 * Command messages will be proxied to other servers.
 */
@Log4j2
public class ClientComponent implements ServerComponent, Runnable, AutoCloseable, MessageSender {
    private final RaftStateEventHandler raftStateEventHandler;
    private final RaftState raftState;
    private final SocketEventHandler socketEventHandler;
    private final Map<ClientId, ChatClient> allClients;
    private final ChatRoomState chatRoomState;
    private final ExecutorService executorService;
    private final int port;

    /**
     * Create a client connector. See {@link ClientComponent}.
     *
     * @param port            Port to listen.
     * @param currentServerId Current server id.
     * @param gossipState     Gossip read only view.
     * @param raftState       System read only view.
     */
    public ClientComponent(int port, ServerId currentServerId, GossipState gossipState, RaftState raftState) {
        RoomId mainRoomId = raftState.getMainRoomId(currentServerId);
        Gson serializer = new Gson();
        ChatRoomWaitingList waitingList = new ChatRoomWaitingList();

        this.port = port;
        this.allClients = new HashMap<>();
        this.raftState = raftState;
        this.chatRoomState = new ChatRoomState(mainRoomId);
        this.socketEventHandler = SocketEventHandler.builder()
                .currentServerId(currentServerId)
                .gossipState(gossipState)
                .raftState(raftState)
                .chatRoomState(chatRoomState)
                .waitingList(waitingList)
                .serializer(serializer).build();
        this.raftStateEventHandler = RaftStateEventHandler.builder()
                .mainRoomId(mainRoomId)
                .chatRoomState(chatRoomState)
                .waitingList(waitingList)
                .serializer(serializer).build();

        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void connect() {
        socketEventHandler.attachMessageSender(this);
        raftStateEventHandler.attachMessageSender(this);
        raftState.attachListener(raftStateEventHandler);
        log.info("client component connected");
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
                ClientId clientId = ClientId.unique();
                ChatClient client = new ChatClientImpl(clientId, socket);
                allClients.put(clientId, client);
                this.executorService.submit(new ClientSocketListener(clientId, socket, socketEventHandler));
            }
        } catch (IOException e) {
            log.error("Server socket opening failed on port {}.", port);
            log.throwing(e);
        } finally {
            this.executorService.shutdown();
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
            if (clientId != excludeClientId && allClients.containsKey(clientId)) {
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
}
