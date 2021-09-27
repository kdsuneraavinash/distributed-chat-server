package server.components.client;

import com.google.gson.Gson;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import server.components.ServerComponent;
import server.components.client.messages.ClientMessageSerializer;
import server.components.client.messages.requests.ListClientRequest;
import server.components.client.messages.requests.MessageClientRequest;
import server.components.client.messages.requests.NewIdentityClientRequest;
import server.components.client.messages.responses.MessageClientResponse;
import server.components.client.messages.responses.NewIdentityClientResponse;
import server.components.client.messages.responses.RoomChangeClientResponse;
import server.components.client.models.Client;
import server.components.client.models.ClientListener;
import server.state.ServerState;
import server.state.logs.CreateIdentityLog;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Server component that handles client requests.
 * Manages the chat messages etc... among clients.
 * Command messages will be proxied to other servers.
 */
@Log4j2
public class ClientComponent extends ServerComponent {
    private static final String MAIN_HALL = "MainHall";
    private final ServerState serverState;
    // Clients -> Identity
    private final HashMap<Client, String> clientIdentities;
    // Rooms -> Clients
    private final HashMap<String, HashSet<Client>> roomParticipants;
    // Identity -> Room
    private final HashMap<String, String> participantRoom;
    private final Gson serializer;

    public ClientComponent(int port) {
        super(port);
        this.clientIdentities = new HashMap<>();
        this.serverState = new ServerState();
        this.participantRoom = new HashMap<>();
        this.serializer = ClientMessageSerializer.createAttachedSerializer();
        this.roomParticipants = new HashMap<>();
        this.roomParticipants.put(MAIN_HALL, new HashSet<>());
    }

    @Override
    public void run() {
        try {
            // Listen on client port and connect each new client to the manager.
            @Cleanup ServerSocket serverSocket = new ServerSocket(port);
            while (!Thread.currentThread().isInterrupted()) {
                // Create a new client from each socket connection.
                Socket socket = serverSocket.accept();
                Client client = new Client(socket);
                client.startListening(new ClientSideEventHandler(client), this.serializer);
            }
        } catch (IOException e) {
            log.error("Server socket opening failed on port {}.", getPort());
            log.throwing(e);
        }
    }

    @Override
    public void close() throws Exception {
        for (Client client : clientIdentities.keySet()) {
            client.close();
        }
    }

    private void sendMessage(Client client, Object message) {
        client.sendMessage(serializer.toJson(message));
    }

    /**
     * A class for event handling for events from client side.
     * This includes client disconnecting or client sending messages.
     */
    private class ClientSideEventHandler implements ClientListener.EventHandler {
        private final Client client;

        ClientSideEventHandler(@NonNull Client client) {
            this.client = client;
        }

        @Override
        public void connect() {
            clientIdentities.put(client, null);
            log.info("Connected {}. Total {} clients connected.", client, clientIdentities.size());
        }

        @Override
        public void disconnect() {
            try {
                client.close();
            } catch (Exception ignored) {
            }
            clientIdentities.remove(client);
            log.info("Disconnected {}. Total {} clients connected.", client, clientIdentities.size());
        }

        @Override
        public void receiveMessage(NewIdentityClientRequest request) {
            String newParticipantId = request.getIdentity();
            if (serverState.hasParticipant(newParticipantId)) {
                sendMessage(client, new NewIdentityClientResponse("false"));
                return;
            }
            // TODO: Check with global state.
            serverState.createIdentity(new CreateIdentityLog(ServerState.SERVER, newParticipantId));
            // TODO: Remove following and put to Raft
            NewIdentityClientResponse newIdentityClientResponse = new NewIdentityClientResponse("true");
            sendMessage(client, newIdentityClientResponse);
            clientIdentities.put(client, newParticipantId);
            roomParticipants.get(MAIN_HALL).add(client);
            participantRoom.put(newParticipantId, MAIN_HALL);
            RoomChangeClientResponse roomChangeClientResponse = new RoomChangeClientResponse(newParticipantId, "", MAIN_HALL);
            for (Client otherClient : roomParticipants.get(MAIN_HALL)) {
                sendMessage(otherClient, roomChangeClientResponse);
            }
        }

        @Override
        public void receiveMessage(ListClientRequest request) {
        }

        @Override
        public void receiveMessage(MessageClientRequest request) {
            String participantId = clientIdentities.get(client);
            String content = request.getContent();
            MessageClientResponse response = new MessageClientResponse(participantId, content);
            for (Client otherClient : roomParticipants.get(participantRoom.get(participantId))) {
                if (otherClient != client) {
                    sendMessage(otherClient, response);
                }
            }
        }
    }
}
