package lk.ac.mrt.cse.cs4262.components.client;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.common.state.SystemState;
import lk.ac.mrt.cse.cs4262.common.state.SystemStateReadView;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.BaseClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.CreateRoomClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.ListClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.MessageClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.NewIdentityClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.WhoClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.CreateRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.ListClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.MessageClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.NewIdentityClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.RoomChangeClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.WhoClientResponse;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Server component that handles client requests.
 * Manages the chat messages etc... among clients.
 * Command messages will be proxied to other servers.
 */
@Log4j2
public class ClientComponent implements ServerComponent, ClientSocketListener.Reporter, SystemStateReadView.Listener {
    private final int port;
    private final SystemState systemState;
    private final HashSet<Client> clients;
    private final HashMap<RoomId, HashSet<Client>> roomParticipants;
    private final HashMap<ParticipantId, RoomId> participantRoom;
    private final HashMap<ParticipantId, Client> waitingForParticipantId;
    private final HashMap<RoomId, Client> waitingForRoomId;
    private final Gson serializer;

    /**
     * Create a client connector. See {@link ClientComponent}.
     *
     * @param port        Port to listen.
     * @param systemState System read only view.
     */
    public ClientComponent(int port, SystemState systemState) {
        this.port = port;
        this.systemState = systemState;
        this.clients = new HashSet<>();
        this.participantRoom = new HashMap<>();
        this.serializer = new Gson();
        this.roomParticipants = new HashMap<>();
        this.waitingForParticipantId = new HashMap<>();
        this.waitingForRoomId = new HashMap<>();
        RoomId mainRoomId = this.systemState.getMainRoomId(this.systemState.getCurrentServerId());
        this.roomParticipants.put(mainRoomId, new HashSet<>());
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
                Thread listener = new Thread(new ClientSocketListener(client, this));
                listener.start();
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
    public void receiveRequest(Client client, String rawRequest) {
        log.info("{} -> {}", client, rawRequest);
        BaseClientRequest baseRequest = serializer.fromJson(rawRequest, BaseClientRequest.class);
        if (baseRequest instanceof NewIdentityClientRequest) {
            NewIdentityClientRequest request = (NewIdentityClientRequest) baseRequest;
            newIdentityRequest(client, request.getIdentity());
        } else if (baseRequest instanceof ListClientRequest) {
            chatRoomListRequest(client);
        } else if (baseRequest instanceof MessageClientRequest) {
            MessageClientRequest request = (MessageClientRequest) baseRequest;
            messageRequest(client, request.getContent());
        } else if (baseRequest instanceof CreateRoomClientRequest) {
            CreateRoomClientRequest request = (CreateRoomClientRequest) baseRequest;
            createRoomRequest(client, request.getRoomId());
        } else if (baseRequest instanceof WhoClientRequest) {
            whoRequest(client);
        } else {
            throw new UnsupportedOperationException();
        }
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
    public void createdParticipantId(@NonNull ServerId serverId, @NonNull ParticipantId participantId) {
        Client client = waitingForParticipantId.remove(participantId);
        ServerId currentServerId = systemState.getCurrentServerId();
        RoomId currentRoomId = systemState.getMainRoomId(currentServerId);
        NewIdentityClientResponse newIdentityClientResponse = new NewIdentityClientResponse(true);
        sendMessage(client, newIdentityClientResponse);
        client.setParticipantId(participantId);
        roomParticipants.get(currentRoomId).add(client);
        participantRoom.put(participantId, currentRoomId);
        RoomChangeClientResponse roomChangeClientResponse = new RoomChangeClientResponse(participantId,
                RoomId.NULL, currentRoomId);
        for (Client otherClient : roomParticipants.get(currentRoomId)) {
            sendMessage(otherClient, roomChangeClientResponse);
        }
    }

    @Override
    public void createdRoom(@NonNull ServerId serverId, @NonNull ParticipantId ownerId, @NonNull RoomId roomId) {
        Client ownerClient = waitingForRoomId.remove(roomId);
        CreateRoomClientResponse createRoomClientResponse = new CreateRoomClientResponse(roomId, true);
        RoomId oldRoomId = participantRoom.get(ownerId);
        roomParticipants.put(roomId, new HashSet<>());
        roomParticipants.get(oldRoomId).remove(ownerClient);
        roomParticipants.get(roomId).add(ownerClient);
        participantRoom.put(ownerId, roomId);
        sendMessage(ownerClient, createRoomClientResponse);
        ServerId currentServerId = systemState.getCurrentServerId();
        RoomId currentRoomId = systemState.getMainRoomId(currentServerId);
        RoomChangeClientResponse roomChangeClientResponse = new RoomChangeClientResponse(ownerId, oldRoomId, roomId);
        for (Client otherClient : roomParticipants.get(currentRoomId)) {
            sendMessage(otherClient, roomChangeClientResponse);
        }
    }

    @Override
    public void deletedIdentity(@NonNull ServerId serverId, @NonNull ParticipantId participantId,
                                RoomId deletedRoomId) {

    }

    @Override
    public void deletedRoom(@NonNull ServerId serverId, RoomId deletedRoomId) {

    }

    private void newIdentityRequest(Client client, String identity) {
        ParticipantId newParticipantId = new ParticipantId(identity);
        if (systemState.hasParticipant(newParticipantId)) {
            sendMessage(client, new NewIdentityClientResponse(false));
            return;
        }
        // TODO: Check with global state.
        ServerId currentServerId = systemState.getCurrentServerId();
        waitingForParticipantId.put(newParticipantId, client);
        systemState.apply(new CreateIdentityLog(currentServerId.getValue(), identity));
    }

    private void chatRoomListRequest(Client client) {
        ServerId currentServerId = systemState.getCurrentServerId();
        sendMessage(client, new ListClientResponse(systemState.serverRoomIds(currentServerId)));
    }

    private void messageRequest(Client client, String content) {
        if (!client.isParticipating()) {
            return;
        }
        ParticipantId participantId = client.getParticipantId();
        MessageClientResponse response = new MessageClientResponse(participantId, content);
        for (Client otherClient : roomParticipants.get(participantRoom.get(participantId))) {
            if (otherClient != client) {
                sendMessage(otherClient, response);
            }
        }
    }

    private void whoRequest(Client client) {
        ParticipantId participantId = client.getParticipantId();
        RoomId roomId = participantRoom.get(participantId);
        Collection<ParticipantId> participantIds = new ArrayList<>();
        for (Client otherClient : roomParticipants.get(roomId)) {
            participantIds.add(otherClient.getParticipantId());
        }
        sendMessage(client, new WhoClientResponse(roomId, participantIds, systemState.getOwnerId(roomId)));
    }

    private void createRoomRequest(Client client, String roomId) {
        RoomId newRoomId = new RoomId(roomId);
        ParticipantId ownerId = client.getParticipantId();
        if (systemState.hasRoom(newRoomId)) {
            sendMessage(client, new CreateRoomClientResponse(newRoomId, false));
            return;
        }
        // TODO: Check with global state.
        waitingForRoomId.put(newRoomId, client);
        systemState.apply(new CreateRoomLog(roomId, ownerId.getValue()));
    }
}
