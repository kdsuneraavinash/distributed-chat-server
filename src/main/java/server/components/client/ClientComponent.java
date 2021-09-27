package server.components.client;

import com.google.gson.Gson;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import server.components.ServerComponent;
import server.components.client.messages.ClientMessageSerializer;
import server.components.client.messages.responses.MessageClientResponse;
import server.components.client.messages.responses.NewIdentityClientResponse;
import server.components.client.messages.responses.RoomChangeClientResponse;
import server.components.client.models.Client;
import server.components.client.models.ClientListener;
import server.core.ParticipantId;
import server.core.RoomId;
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
public class ClientComponent extends ServerComponent implements ClientListener.EventHandler {
    private static final RoomId MAIN_HALL = new RoomId("MainHall");
    private final ServerState serverState;
    private final HashSet<Client> clients;
    private final HashMap<RoomId, HashSet<Client>> roomParticipants;
    private final HashMap<ParticipantId, RoomId> participantRoom;
    private final Gson serializer;

    public ClientComponent(int port) {
        super(port);
        this.clients = new HashSet<>();
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
                client.startListening(this, this.serializer);
            }
        } catch (IOException e) {
            log.error("Server socket opening failed on port {}.", getPort());
            log.throwing(e);
        }
    }

    @Override
    public void close() throws Exception {
        for (Client client : clients) {
            client.close();
        }
    }

    private void sendMessage(Client client, Object message) {
        client.sendMessage(serializer.toJson(message));
    }

    @Override
    public void connectClient(Client client) {
        clients.add(client);
        log.info("Connected {}. Total {} clients connected.", client, clients.size());
    }

    @Override
    public void disconnectClient(Client client) {
        try {
            client.close();
        } catch (Exception ignored) {
        }
        clients.remove(client);
        log.info("Disconnected {}. Total {} clients connected.", client, clients.size());
    }

    @Override
    public void newIdentityRequest(Client client, String identity) {
        ParticipantId newParticipantId = new ParticipantId(identity);
        if (serverState.hasParticipant(newParticipantId)) {
            sendMessage(client, new NewIdentityClientResponse(false));
            return;
        }
        // TODO: Check with global state.
        serverState.addLog(new CreateIdentityLog(ServerState.SERVER.getValue(), newParticipantId.getValue()));
        // TODO: Remove following and put to Raft, This should run when committing
        NewIdentityClientResponse newIdentityClientResponse = new NewIdentityClientResponse(true);
        sendMessage(client, newIdentityClientResponse);
        clients.add(client);
        client.setParticipantId(newParticipantId);
        roomParticipants.get(MAIN_HALL).add(client);
        participantRoom.put(newParticipantId, MAIN_HALL);
        RoomChangeClientResponse roomChangeClientResponse = new RoomChangeClientResponse(newParticipantId, MAIN_HALL);
        for (Client otherClient : roomParticipants.get(MAIN_HALL)) {
            sendMessage(otherClient, roomChangeClientResponse);
        }
    }

    @Override
    public void chatRoomListRequest(Client client) {

    }

    @Override
    public void messageRequest(Client client, String content) {
        ParticipantId participantId = client.getParticipantId();
        if (participantId == null) {
            return;
        }
        MessageClientResponse response = new MessageClientResponse(participantId, content);
        for (Client otherClient : roomParticipants.get(participantRoom.get(participantId))) {
            if (otherClient != client) {
                sendMessage(otherClient, response);
            }
        }
    }
}
