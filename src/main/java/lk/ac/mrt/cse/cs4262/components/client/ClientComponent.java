package lk.ac.mrt.cse.cs4262.components.client;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.common.state.SystemState;
import lk.ac.mrt.cse.cs4262.common.state.SystemStateImpl;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.CreateRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.ListClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.MessageClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.NewIdentityClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.RoomChangeClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.WhoClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.models.Client;
import lk.ac.mrt.cse.cs4262.components.client.models.ClientListener;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Server component that handles client requests.
 * Manages the chat messages etc... among clients.
 * Command messages will be proxied to other servers.
 */
@Log4j2
public class ClientComponent implements ServerComponent, ClientListener.EventHandler {
    private final int port;
    private final SystemState serverState;
    private final HashSet<Client> clients;
    private final HashMap<RoomId, HashSet<Client>> roomParticipants;
    private final HashMap<ParticipantId, RoomId> participantRoom;
    private final Gson serializer;

    public ClientComponent(int port) {
        this.port = port;
        this.clients = new HashSet<>();
        this.serverState = new SystemStateImpl();
        this.participantRoom = new HashMap<>();
        this.serializer = new Gson();
        this.roomParticipants = new HashMap<>();
        this.roomParticipants.put(serverState.getMainRoomId(serverState.getCurrentServerId()), new HashSet<>());
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
            log.error("Server socket opening failed on port {}.", port);
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
        ServerId currentServerId = serverState.getCurrentServerId();
        serverState.apply(new CreateIdentityLog(currentServerId.getValue(), identity));
        // TODO: Remove following and put to Raft, This should run when committing
        RoomId currentRoomId = serverState.getMainRoomId(currentServerId);
        NewIdentityClientResponse newIdentityClientResponse = new NewIdentityClientResponse(true);
        sendMessage(client, newIdentityClientResponse);
        clients.add(client);
        client.setParticipantId(newParticipantId);
        roomParticipants.get(currentRoomId).add(client);
        participantRoom.put(newParticipantId, currentRoomId);
        RoomChangeClientResponse roomChangeClientResponse = new RoomChangeClientResponse(newParticipantId, RoomId.NULL, currentRoomId);
        for (Client otherClient : roomParticipants.get(currentRoomId)) {
            sendMessage(otherClient, roomChangeClientResponse);
        }
    }

    @Override
    public void chatRoomListRequest(Client client) {
        ServerId currentServerId = serverState.getCurrentServerId();
        sendMessage(client, new ListClientResponse(serverState.serverRoomIds(currentServerId)));
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

    @Override
    public void whoRequest(Client client) {
        ParticipantId participantId = client.getParticipantId();
        RoomId roomId = participantRoom.get(participantId);
        Collection<ParticipantId> participantIds = roomParticipants.get(roomId).stream().map(Client::getParticipantId).collect(Collectors.toList());
        sendMessage(client, new WhoClientResponse(roomId, participantIds, serverState.getOwnerId(roomId)));
    }

    @Override
    public void createRoomRequest(Client client, String roomId) {
        RoomId newRoomId = new RoomId(roomId);
        ParticipantId ownerId = client.getParticipantId();
        if (serverState.hasRoom(newRoomId)) {
            sendMessage(client, new CreateRoomClientResponse(newRoomId, false));
            return;
        }
        // TODO: Check with global state.
        serverState.apply(new CreateRoomLog(roomId, ownerId.getValue()));
        // TODO: Remove following and put to Raft, This should run when committing
        CreateRoomClientResponse createRoomClientResponse = new CreateRoomClientResponse(newRoomId, true);
        RoomId oldRoomId = participantRoom.get(ownerId);
        roomParticipants.put(newRoomId, new HashSet<>());
        roomParticipants.get(oldRoomId).remove(client);
        roomParticipants.get(newRoomId).add(client);
        participantRoom.put(ownerId, newRoomId);
        sendMessage(client, createRoomClientResponse);
        ServerId currentServerId = serverState.getCurrentServerId();
        RoomId currentRoomId = serverState.getMainRoomId(currentServerId);
        RoomChangeClientResponse roomChangeClientResponse = new RoomChangeClientResponse(ownerId, oldRoomId, newRoomId);
        for (Client otherClient : roomParticipants.get(currentRoomId)) {
            sendMessage(otherClient, roomChangeClientResponse);
        }
    }
}
