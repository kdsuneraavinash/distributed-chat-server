package lk.ac.mrt.cse.cs4262.components.client;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.common.state.SystemState;
import lk.ac.mrt.cse.cs4262.common.state.SystemStateReadView;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.DeleteRoomLog;
import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.client.connector.ChatClient;
import lk.ac.mrt.cse.cs4262.components.client.connector.ClientSocketListener;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.BaseClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.CreateRoomClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.DeleteRoomClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.JoinRoomClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.ListClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.MessageClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.NewIdentityClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.WhoClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.CreateRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.DeleteRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.ListClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.MessageBroadcastResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.NewIdentityClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.RoomChangeBroadcastResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.WhoClientResponse;
import lombok.NonNull;
import lombok.Synchronized;
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
    private final Map<@NonNull ParticipantId, @NonNull ClientId> waitingForParticipantIdCreation;

    /**
     * Clients that are waiting for a new room id to be accepted.
     */
    private final Map<@NonNull RoomId, @NonNull ClientId> waitingForRoomIdCreation;

    /**
     * Clients that are waiting for a new room id deletion to be accepted.
     */
    private final Map<@NonNull RoomId, @NonNull ClientId> waitingForRoomIdDeletion;

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
        this.waitingForParticipantIdCreation = new HashMap<>();
        this.waitingForRoomIdCreation = new HashMap<>();
        this.waitingForRoomIdDeletion = new HashMap<>();
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
        } else {
            // Participant ID required section.
            // If client does not have a participant id, ignore.
            ParticipantId participantId = clientParticipantMap.get(clientId);
            if (participantId == null) {
                log.info("Ignoring Client({}): {}", clientId, rawRequest);
                return;
            }

            if (baseRequest instanceof ListClientRequest) {
                processChatRoomListRequest(clientId);
            } else if (baseRequest instanceof MessageClientRequest) {
                MessageClientRequest request = (MessageClientRequest) baseRequest;
                processMessageRequest(clientId, participantId, request.getContent());
            } else if (baseRequest instanceof WhoClientRequest) {
                processWhoRequest(clientId, participantId);
            } else if (baseRequest instanceof CreateRoomClientRequest) {
                CreateRoomClientRequest request = (CreateRoomClientRequest) baseRequest;
                processCreateRoomRequest(clientId, participantId, new RoomId(request.getRoomId()));
            } else if (baseRequest instanceof DeleteRoomClientRequest) {
                DeleteRoomClientRequest request = (DeleteRoomClientRequest) baseRequest;
                processDeleteRoomRequest(clientId, participantId, new RoomId(request.getRoomId()));
            } else if (baseRequest instanceof JoinRoomClientRequest) {
                JoinRoomClientRequest request = (JoinRoomClientRequest) baseRequest;
                processJoinRoomRequest(clientId, participantId, new RoomId(request.getRoomId()));
            } else {
                log.error("Unknown command from Client({}): {}", clientId, rawRequest);
            }
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
    Private Handlers for Client Events
    ========================================================
     */

    @Synchronized
    private void processNewIdentityRequest(ClientId clientId, ParticipantId participantId) {
        ChatClient chatClient = allClients.get(clientId);

        // If participant id is invalid locally, REJECT.
        if (systemState.hasParticipant(participantId)) {
            NewIdentityClientResponse response1 = NewIdentityClientResponse.builder()
                    .approved(false).build();
            String message1 = serializer.toJson(response1);
            chatClient.sendMessage(message1);
            return;
        }

        // Add client to waiting list.
        waitingForParticipantIdCreation.put(participantId, clientId);

        // TODO: Send to leader via RAFT.
        // Placeholder - put a log manually.
        systemState.apply(new CreateIdentityLog(currentServerId.getValue(), participantId.getValue()));
    }

    private void processChatRoomListRequest(ClientId clientId) {
        ChatClient chatClient = allClients.get(clientId);

        // Get all participants of current server.
        Collection<RoomId> roomIds = systemState.serverRoomIds(currentServerId);

        // Send room list.
        ListClientResponse response1 = ListClientResponse.builder()
                .rooms(roomIds).build();
        String message1 = serializer.toJson(response1);
        chatClient.sendMessage(message1);
    }

    private void processMessageRequest(ClientId clientId, ParticipantId participantId, String content) {
        // Send message to everyone (except originator) in room.
        RoomId currentRoomId = participantRoomMap.get(participantId);
        MessageBroadcastResponse response1 = MessageBroadcastResponse.builder()
                .content(content).participantId(participantId).build();
        String message1 = serializer.toJson(response1);
        sendMessageToRoomExceptOriginator(currentRoomId, message1, clientId);
    }

    private void processWhoRequest(ClientId clientId, ParticipantId participantId) {
        ChatClient chatClient = allClients.get(clientId);

        // Find information for the response.
        RoomId currentRoomId = participantRoomMap.get(participantId);
        ParticipantId roomOwnerId = systemState.getOwnerId(currentRoomId);
        Collection<ParticipantId> friendIds = new ArrayList<>();
        for (ClientId friendClientId : roomClientListMap.get(currentRoomId)) {
            ParticipantId fiendParticipantId = clientParticipantMap.get(friendClientId);
            friendIds.add(fiendParticipantId);
        }

        // Send who data list.
        WhoClientResponse response1 = WhoClientResponse.builder()
                .ownerId(roomOwnerId)
                .roomId(currentRoomId)
                .participantIds(friendIds).build();
        String message1 = serializer.toJson(response1);
        chatClient.sendMessage(message1);
    }

    @Synchronized
    private void processCreateRoomRequest(ClientId clientId, ParticipantId participantId, RoomId roomId) {
        ChatClient chatClient = allClients.get(clientId);

        // If room id is invalid locally, REJECT.
        if (systemState.hasRoom(roomId)) {
            CreateRoomClientResponse response1 = CreateRoomClientResponse.builder()
                    .approved(false).build();
            String message1 = serializer.toJson(response1);
            chatClient.sendMessage(message1);
            return;
        }
        // Add client to waiting list.
        waitingForRoomIdCreation.put(roomId, clientId);

        // TODO: Send to leader via RAFT.
        // Placeholder - put a log manually.
        systemState.apply(new CreateRoomLog(roomId.getValue(), participantId.getValue()));
    }

    @Synchronized
    private void processDeleteRoomRequest(ClientId clientId, ParticipantId participantId, RoomId roomId) {
        ChatClient chatClient = allClients.get(clientId);

        // If the room does not exist or client is not the owner of the room, REJECT
        if (!systemState.hasRoom(roomId) || !participantId.equals(systemState.getOwnerId(roomId))) {
            DeleteRoomClientResponse response1 = DeleteRoomClientResponse.builder()
                    .approved(false)
                    .roomId(roomId).build();
            String message1 = serializer.toJson(response1);
            chatClient.sendMessage(message1);
            return;
        }

        // Add client to waiting list.
        waitingForRoomIdDeletion.put(roomId, clientId);

        // TODO: Send to leader via RAFT.
        // Placeholder - put a log manually.
        systemState.apply(new DeleteRoomLog(roomId.getValue()));
    }

    @Synchronized
    private void processJoinRoomRequest(ClientId clientId, ParticipantId participantId, RoomId roomId) {
        ChatClient chatClient = allClients.get(clientId);

        // Cant change if owns a room or room id is invalid.
        RoomId formerRoomId = participantRoomMap.get(participantId);
        if (!systemState.hasRoom(roomId)
                || systemState.ownsRoom(participantId)
                // TODO: Remove condition to move only in the same server
                || !currentServerId.equals(systemState.getRoomServerId(roomId))) {
            RoomChangeBroadcastResponse response1 = RoomChangeBroadcastResponse.builder()
                    .currentRoomId(formerRoomId)
                    .formerRoomId(formerRoomId)
                    .participantId(participantId).build();
            String message2 = serializer.toJson(response1);
            chatClient.sendMessage(message2);
            return;
        }

        // Update chat room maps.
        participantRoomMap.put(participantId, roomId);
        roomClientListMap.get(formerRoomId).remove(clientId);
        roomClientListMap.get(roomId).add(clientId);

        // Send room change to all in new/old room.
        RoomChangeBroadcastResponse response2 = RoomChangeBroadcastResponse.builder()
                .participantId(participantId)
                .currentRoomId(roomId)
                .formerRoomId(formerRoomId).build();
        String message2 = serializer.toJson(response2);
        sendMessageToRoom(formerRoomId, message2);
        sendMessageToRoom(roomId, message2);
    }

    /*
    ========================================================
    State Machine Event Handling
    TODO: Handle errors/corner cases
    TODO: Consider server id when handling.
    ========================================================
     */

    @Override
    @Synchronized
    public void participantIdCreated(@NonNull ServerId serverId, @NonNull ParticipantId createParticipantId) {
        // Remove client from waiting list.
        ClientId clientId = waitingForParticipantIdCreation.remove(createParticipantId);
        ChatClient chatClient = allClients.get(clientId);

        // Send APPROVED message to client.
        NewIdentityClientResponse response1 = NewIdentityClientResponse.builder()
                .approved(true).build();
        String message1 = serializer.toJson(response1);
        chatClient.sendMessage(message1);

        // Update chat room maps.
        clientParticipantMap.put(clientId, createParticipantId);
        roomClientListMap.get(mainRoomId).add(clientId);
        participantRoomMap.put(createParticipantId, mainRoomId);

        // Send room change to all in main room.
        RoomChangeBroadcastResponse response2 = RoomChangeBroadcastResponse.builder()
                .participantId(createParticipantId)
                .currentRoomId(mainRoomId)
                .formerRoomId(RoomId.NULL).build();
        String message2 = serializer.toJson(response2);
        sendMessageToRoom(mainRoomId, message2);
    }

    @Override
    @Synchronized
    public void roomIdCreated(@NonNull ServerId serverId, @NonNull ParticipantId ownerId,
                              @NonNull RoomId createdRoomId) {
        // Remove owner from waiting list
        ClientId ownerClientId = waitingForRoomIdCreation.remove(createdRoomId);
        ChatClient ownerChatClient = allClients.get(ownerClientId);

        // Update chat room maps.
        RoomId formerRoomId = participantRoomMap.get(ownerId);
        List<ClientId> newRoomClients = new ArrayList<>();
        newRoomClients.add(ownerClientId);
        roomClientListMap.get(formerRoomId).remove(ownerClientId);
        roomClientListMap.put(createdRoomId, newRoomClients);
        participantRoomMap.put(ownerId, createdRoomId);

        // Send APPROVED message.
        CreateRoomClientResponse response1 = CreateRoomClientResponse.builder()
                .roomId(createdRoomId)
                .approved(true).build();
        String message1 = serializer.toJson(response1);
        ownerChatClient.sendMessage(message1);

        // Send room change to previous room.
        RoomChangeBroadcastResponse response2 = RoomChangeBroadcastResponse.builder()
                .currentRoomId(createdRoomId)
                .formerRoomId(formerRoomId)
                .participantId(ownerId).build();
        String message2 = serializer.toJson(response2);
        ownerChatClient.sendMessage(message2);
        sendMessageToRoom(formerRoomId, message2);
    }

    @Override
    @Synchronized
    public void participantIdDeleted(@NonNull ServerId serverId,
                                     @NonNull ParticipantId deletedId,
                                     RoomId deletedRoomId) {
        // TODO
    }

    @Override
    @Synchronized
    public void roomIdDeleted(@NonNull ServerId serverId, RoomId deletedRoomId) {
        // Remove owner from waiting list
        ClientId ownerClientId = waitingForRoomIdDeletion.remove(deletedRoomId);
        ChatClient ownerChatClient = allClients.get(ownerClientId);

        // Update chat room maps.
        // Remove old participants from room and add to main room.
        List<ClientId> prevClientIds = roomClientListMap.remove(deletedRoomId);
        roomClientListMap.get(mainRoomId).addAll(prevClientIds);
        for (ClientId prevClientId : prevClientIds) {
            ParticipantId prevParticipantId = clientParticipantMap.get(prevClientId);
            participantRoomMap.put(prevParticipantId, mainRoomId);
        }

        // Send APPROVED message.
        DeleteRoomClientResponse response1 = DeleteRoomClientResponse.builder()
                .roomId(deletedRoomId)
                .approved(true).build();
        String message1 = serializer.toJson(response1);
        ownerChatClient.sendMessage(message1);

        // Send room change to all old users.
        for (ClientId prevClientId : prevClientIds) {
            ParticipantId prevParticipantId = clientParticipantMap.get(prevClientId);
            RoomChangeBroadcastResponse response2 = RoomChangeBroadcastResponse.builder()
                    .currentRoomId(mainRoomId)
                    .formerRoomId(deletedRoomId)
                    .participantId(prevParticipantId).build();
            String message2 = serializer.toJson(response2);
            sendMessageToRoom(mainRoomId, message2);
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

    /*
    ========================================================
    Util functions
    ========================================================
     */

    private void sendMessageToRoom(RoomId roomId, String message) {
        for (ClientId clientId : roomClientListMap.get(roomId)) {
            allClients.get(clientId).sendMessage(message);
        }
    }

    private void sendMessageToRoomExceptOriginator(RoomId roomId, String message, ClientId originatorId) {
        for (ClientId clientId : roomClientListMap.get(roomId)) {
            if (!originatorId.equals(clientId)) {
                allClients.get(clientId).sendMessage(message);
            }
        }
    }
}
