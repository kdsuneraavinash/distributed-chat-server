package lk.ac.mrt.cse.cs4262.components.client;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.common.state.SystemState;
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
import lk.ac.mrt.cse.cs4262.components.client.chat.events.SocketEventHandler;
import lk.ac.mrt.cse.cs4262.components.client.chat.events.SystemStateEventHandler;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Server component that handles client requests.
 * Manages the chat messages etc... among clients.
 * Command messages will be proxied to other servers.
 */
@Log4j2
public class ClientComponent implements ServerComponent, Runnable, AutoCloseable, MessageSender {
    private final SystemStateEventHandler systemStateEventHandler;
    private final SystemState systemState;
    private final SocketEventHandler socketEventHandler;
    private final Map<ClientId, ChatClient> allClients;
    private final ChatRoomState chatRoomState;
    private final int port;

    /**
     * Create a client connector. See {@link ClientComponent}.
     *
     * @param port        Port to listen.
     * @param systemState System read only view.
     */
    public ClientComponent(int port, SystemState systemState) {
        ServerId currentServerId = systemState.getCurrentServerId();
        RoomId mainRoomId = systemState.getMainRoomId(currentServerId);
        Gson serializer = new Gson();
        ChatRoomWaitingList waitingList = new ChatRoomWaitingList();

        this.port = port;
        this.allClients = new HashMap<>();
        this.systemState = systemState;
        this.chatRoomState = new ChatRoomState(mainRoomId);
        this.socketEventHandler = SocketEventHandler.builder()
                .currentServerId(currentServerId)
                .systemState(systemState)
                .chatRoomState(chatRoomState)
                .waitingList(waitingList)
                .serializer(serializer).build();
        this.systemStateEventHandler = SystemStateEventHandler.builder()
                .mainRoomId(mainRoomId)
                .chatRoomState(chatRoomState)
                .waitingList(waitingList)
                .serializer(serializer).build();
    }

    @Override
    public void connect() {
        socketEventHandler.attachMessageSender(this);
        systemStateEventHandler.attachMessageSender(this);
        systemState.attachListener(systemStateEventHandler);
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
                Thread thread = new Thread(new ClientSocketListener(clientId, socket, socketEventHandler));
                ChatClient client = new ChatClientImpl(clientId, socket, thread);
                allClients.put(clientId, client);
                thread.start();
            }
        } catch (IOException e) {
            log.error("Server socket opening failed on port {}.", port);
            log.throwing(e);
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
