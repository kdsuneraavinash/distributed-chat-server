package lk.ac.mrt.cse.cs4262.components.client;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.common.state.SystemState;
import lk.ac.mrt.cse.cs4262.common.state.SystemStateReadView;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.client.connector.ChatClient;
import lk.ac.mrt.cse.cs4262.components.client.connector.ClientSocketListener;
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
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class ChatConnector implements ClientSocketListener.Reporter, SystemStateReadView.Reporter, AutoCloseable {
    private final SystemState systemState;
    private final ServerId currentServerId;
    private final RoomId mainRoomId;

    /**
     * Data structure to track all the clients.
     */
    private final Map<@NonNull ClientId, @NonNull ChatClient> allClients;

    /**
     * Data structure to track participant id of clients.
     */
    private final Map<@NonNull ClientId, @NonNull ParticipantId> clientParticipantMap;

    /**
     * Data structure to track all the participants in a given room.
     * The clients in the map should have a participant id.
     */
    private final Map<@NonNull RoomId, List<@NonNull ClientId>> roomClientListMap;

    /**
     * Data structure to track the current room of a given participant.
     */
    private final Map<@NonNull ParticipantId, @NonNull RoomId> participantRoomMap;

    /**
     * Clients that are waiting for a participant id to be accepted.
     */
    private final Map<@NonNull ParticipantId, @NonNull ClientId> waitingForParticipantId;

    /**
     * Clients that are waiting for a new room id to be accepted.
     */
    private final Map<@NonNull RoomId, @NonNull ClientId> waitingForRoomId;

    /**
     * Serializer used to convert to/from json.
     */
    private final Gson serializer;

    /**
     * Create a client connector. See {@link ChatConnector}.
     *
     * @param systemState System read only view.
     */
    public ChatConnector(SystemState systemState) {
        this.currentServerId = systemState.getCurrentServerId();
        this.mainRoomId = systemState.getMainRoomId(this.currentServerId);
        this.systemState = systemState;

        this.allClients = new HashMap<>();

        this.clientParticipantMap = new HashMap<>();
        this.roomClientListMap = new HashMap<>();
        this.roomClientListMap.put(this.mainRoomId, new ArrayList<>());
        this.participantRoomMap = new HashMap<>();

        this.serializer = new Gson();
        this.waitingForParticipantId = new HashMap<>();
        this.waitingForRoomId = new HashMap<>();
    }

    /**
     * Add a client to the chat connector state.
     * This client will be tracked until it gets disconnected.
     *
     * @param chatClient Chat Client to track.
     */
    public void addClient(ChatClient chatClient) {
        allClients.put(chatClient.getClientId(), chatClient);
    }

    /*
    ========================================================
    Client Event Handling
    ========================================================
     */

    @Override
    public void clientConnected(ClientId clientId) {
        ChatClient chatClient = allClients.get(clientId);
        log.info("Connected {}. Total {} clients connected.", chatClient, allClients.size());
    }

    @Override
    public void clientRequestReceived(ClientId clientId, String rawRequest) {
        BaseClientRequest baseRequest = serializer.fromJson(rawRequest, BaseClientRequest.class);
        if (baseRequest instanceof NewIdentityClientRequest) {
            NewIdentityClientRequest request = (NewIdentityClientRequest) baseRequest;
            processNewIdentityRequest(clientId, new ParticipantId(request.getIdentity()));
        } else if (baseRequest instanceof ListClientRequest) {
            processChatRoomListRequest(clientId);
        } else if (baseRequest instanceof MessageClientRequest) {
            MessageClientRequest request = (MessageClientRequest) baseRequest;
            processMessageRequest(clientId, request.getContent());
        } else if (baseRequest instanceof CreateRoomClientRequest) {
            CreateRoomClientRequest request = (CreateRoomClientRequest) baseRequest;
            processCreateRoomRequest(clientId, new RoomId(request.getRoomId()));
        } else if (baseRequest instanceof WhoClientRequest) {
            processWhoRequest(clientId);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void clientDisconnected(ClientId clientId) {
        ChatClient chatClient = allClients.remove(clientId);
        ParticipantId participantId = clientParticipantMap.remove(clientId);
        if (participantId != null) {
            RoomId roomId = participantRoomMap.remove(participantId);
            if (roomId != null) {
                roomClientListMap.get(roomId).remove(clientId);
            }
        }
        log.info("Disconnected {}. Total {} clients connected.", chatClient, allClients.size());
    }

    /*
    ========================================================
    State Machine Event Handling
    TODO: Handle errors/corner cases
    ========================================================
     */

    @Override
    public void createdParticipantId(@NonNull ServerId serverId, @NonNull ParticipantId participantId) {
        // Remove client from waiting list.
        ClientId clientId = waitingForParticipantId.remove(participantId);
        ChatClient chatClient = allClients.get(clientId);

        // Send APPROVED message to client.
        NewIdentityClientResponse response1 = NewIdentityClientResponse.builder()
                .approved(true).build();
        String message1 = serializer.toJson(response1);
        chatClient.sendMessage(message1);

        // Update chat room maps.
        clientParticipantMap.put(clientId, participantId);
        roomClientListMap.get(mainRoomId).add(clientId);
        participantRoomMap.put(participantId, mainRoomId);

        // Send room change to all in main room.
        RoomChangeClientResponse response2 = RoomChangeClientResponse.builder()
                .participantId(participantId)
                .currentRoomId(mainRoomId)
                .formerRoomId(RoomId.NULL).build();
        String message2 = serializer.toJson(response2);
        sendMessageToRoom(mainRoomId, message2);
    }

    @Override
    public void createdRoomId(@NonNull ServerId serverId, @NonNull ParticipantId ownerId, @NonNull RoomId roomId) {
        // Remove owner from waiting list
        ClientId ownerClientId = waitingForRoomId.remove(roomId);
        ChatClient ownerChatClient = allClients.get(ownerClientId);


        // Update chat room maps.
        List<ClientId> newRoomClients = new ArrayList<>();
        newRoomClients.add(ownerClientId);
        roomClientListMap.put(roomId, newRoomClients);
        participantRoomMap.put(ownerId, roomId);

        // Send APPROVED message.
        CreateRoomClientResponse response1 = CreateRoomClientResponse.builder()
                .approved(true).build();
        String message1 = serializer.toJson(response1);
        ownerChatClient.sendMessage(message1);

        // Send room change to previous room.
        RoomId formerRoomId = participantRoomMap.get(ownerId);
        RoomChangeClientResponse response2 = RoomChangeClientResponse.builder()
                .currentRoomId(roomId)
                .formerRoomId(formerRoomId)
                .participantId(ownerId).build();
        String message2 = serializer.toJson(response2);
        sendMessageToRoom(formerRoomId, message2);
    }

    @Override
    public void deletedParticipantId(@NonNull ServerId serverId,
                                     @NonNull ParticipantId participantId,
                                     RoomId deletedRoomId) {
        // TODO
    }

    @Override
    public void deletedRoomId(@NonNull ServerId serverId, RoomId deletedRoomId) {
        // TODO
    }

    /*
    ========================================================
    Private Handlers for Client Events
    ========================================================
     */

    private void processNewIdentityRequest(ClientId clientId, ParticipantId participantId) {
        ChatClient chatClient = allClients.get(clientId);

        // If participant id is invalid locally, REJECT.
        if (systemState.hasParticipant(participantId)) {
            NewIdentityClientResponse request1 = NewIdentityClientResponse.builder()
                    .approved(false).build();
            String message1 = serializer.toJson(request1);
            chatClient.sendMessage(message1);
            return;
        }

        // Add client to waiting list.
        waitingForParticipantId.put(participantId, clientId);

        // TODO: Send to leader via RAFT.
        // Placeholder - put a log manually.
        systemState.apply(new CreateIdentityLog(currentServerId.getValue(), participantId.getValue()));
    }

    private void processChatRoomListRequest(ClientId clientId) {
        ChatClient chatClient = allClients.get(clientId);

        // If client does not have a participant id, ignore.
        ParticipantId participantId = clientParticipantMap.get(clientId);
        if (participantId == null) {
            return;
        }

        // Get all participants of current server.
        Collection<RoomId> roomIds = systemState.serverRoomIds(currentServerId);

        // Send room list.
        ListClientResponse request1 = ListClientResponse.builder()
                .rooms(roomIds).build();
        String message1 = serializer.toJson(request1);
        chatClient.sendMessage(message1);
    }

    private void processMessageRequest(ClientId clientId, String content) {
        // If client does not have a participant id, ignore.
        ParticipantId participantId = clientParticipantMap.get(clientId);
        if (participantId == null) {
            return;
        }

        // Send message to everyone (except originator) in room.
        RoomId currentRoomId = participantRoomMap.get(participantId);
        MessageClientResponse request1 = MessageClientResponse.builder()
                .content(content).participantId(participantId).build();
        String message1 = serializer.toJson(request1);
        for (ClientId friendId : roomClientListMap.get(currentRoomId)) {
            if (friendId != clientId) {
                allClients.get(friendId).sendMessage(message1);
            }
        }
    }

    private void processWhoRequest(ClientId clientId) {
        ChatClient chatClient = allClients.get(clientId);

        // If client does not have a participant id, ignore.
        ParticipantId participantId = clientParticipantMap.get(clientId);
        if (participantId == null) {
            return;
        }

        // Find information for the response.
        RoomId currentRoomId = participantRoomMap.get(participantId);
        ParticipantId roomOwnerId = systemState.getOwnerId(currentRoomId);
        Collection<ParticipantId> friendIds = new ArrayList<>();
        for (ClientId friendClientId : roomClientListMap.get(currentRoomId)) {
            ParticipantId fiendParticipantId = clientParticipantMap.get(friendClientId);
            friendIds.add(fiendParticipantId);
        }

        // Send who data list.
        WhoClientResponse request1 = WhoClientResponse.builder()
                .ownerId(roomOwnerId)
                .roomId(currentRoomId)
                .participantIds(friendIds).build();
        String message1 = serializer.toJson(request1);
        chatClient.sendMessage(message1);
    }

    private void processCreateRoomRequest(ClientId clientId, RoomId roomId) {
        ChatClient chatClient = allClients.get(clientId);

        // If client does not have a participant id, ignore.
        ParticipantId participantId = clientParticipantMap.get(clientId);
        if (participantId == null) {
            return;
        }

        // If room id is invalid locally, REJECT.
        if (systemState.hasRoom(roomId)) {
            CreateRoomClientResponse request1 = CreateRoomClientResponse.builder()
                    .approved(false).build();
            String message1 = serializer.toJson(request1);
            chatClient.sendMessage(message1);
            return;
        }
        // Add client to waiting list.
        waitingForRoomId.put(roomId, clientId);

        // TODO: Send to leader via RAFT.
        // Placeholder - put a log manually.
        systemState.apply(new CreateRoomLog(roomId.getValue(), participantId.getValue()));
    }

    private void sendMessageToRoom(RoomId roomId, String message) {
        for (ClientId clientId : roomClientListMap.get(roomId)) {
            allClients.get(clientId).sendMessage(message);
        }
    }

    /*
    ========================================================
    Auto Closable
    ========================================================
     */

    @Override
    public void close() throws Exception {
        for (ChatClient chatClient : allClients.values()) {
            chatClient.close();
        }
    }
}
