package lk.ac.mrt.cse.cs4262.components.client;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.common.state.SystemState;
import lk.ac.mrt.cse.cs4262.common.state.SystemStateReadView;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.DeleteIdentityLog;
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
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.QuitClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.WhoClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.CreateRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.DeleteRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.ListClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.MessageBroadcastResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.NewIdentityClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.RoomChangeBroadcastResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.WhoClientResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Synchronized;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public boolean processClientRequest(ClientId clientId, String rawRequest) {
        BaseClientRequest baseRequest = serializer.fromJson(rawRequest, BaseClientRequest.class);
        if (baseRequest instanceof NewIdentityClientRequest) {
            NewIdentityClientRequest request = (NewIdentityClientRequest) baseRequest;
            processNewIdentityRequest(clientId, new ParticipantId(request.getIdentity()));
        } else {
            Optional<ParticipantClient> participantClientOptional = getParticipantClient(clientId);
            // Ignore if authentication(?) failed.
            if (participantClientOptional.isEmpty()) {
                return false;
            }
            if (baseRequest instanceof ListClientRequest) {
                processChatRoomListRequest(participantClientOptional.get());
            } else if (baseRequest instanceof MessageClientRequest) {
                MessageClientRequest request = (MessageClientRequest) baseRequest;
                processMessageRequest(participantClientOptional.get(), request.getContent());
            } else if (baseRequest instanceof WhoClientRequest) {
                processWhoRequest(participantClientOptional.get());
            } else if (baseRequest instanceof CreateRoomClientRequest) {
                CreateRoomClientRequest request = (CreateRoomClientRequest) baseRequest;
                processCreateRoomRequest(participantClientOptional.get(), new RoomId(request.getRoomId()));
            } else if (baseRequest instanceof DeleteRoomClientRequest) {
                DeleteRoomClientRequest request = (DeleteRoomClientRequest) baseRequest;
                processDeleteRoomRequest(participantClientOptional.get(), new RoomId(request.getRoomId()));
            } else if (baseRequest instanceof JoinRoomClientRequest) {
                JoinRoomClientRequest request = (JoinRoomClientRequest) baseRequest;
                processJoinRoomRequest(participantClientOptional.get(), new RoomId(request.getRoomId()));
            } else if (baseRequest instanceof QuitClientRequest) {
                processQuitRequest(participantClientOptional.get());
                return true;
            } else {
                log.warn("Unknown command from Client({}): {}", clientId, rawRequest);
            }
        }
        return false;
    }

    @Override
    public void clientSideDisconnect(ClientId clientId) {
        // Process only if authentication(?) failed.
        getParticipantClient(clientId).ifPresent(this::processQuitRequest);
    }

    /*
    ========================================================
    Private Handlers for Client Events
    TODO: Implement delete client.
    ========================================================
     */

    /**
     * Create new identity for user.
     * 1. If participant id is invalid locally, REJECT.
     * 2. Add client to waiting list.
     * 3. Send to leader via RAFT.
     *
     * @param clientId      Client requesting identity.
     * @param participantId Requested identity.
     */
    @Synchronized
    private void processNewIdentityRequest(ClientId clientId, ParticipantId participantId) {
        log.trace("Processing: clientId={} participantId={}", clientId, participantId);
        ChatClient chatClient = allClients.get(clientId);
        // If participant id is invalid locally, REJECT.
        if (systemState.hasParticipant(participantId)) {
            NewIdentityClientResponse response = NewIdentityClientResponse.builder()
                    .approved(false).build();
            String message = serializer.toJson(response);
            chatClient.sendMessage(message);
            return;
        }
        // Add client to waiting list.
        waitingForParticipantIdCreation.put(participantId, clientId);
        // TODO: Send to leader via RAFT.
        // Placeholder - put a log manually.
        systemState.apply(new CreateIdentityLog(currentServerId.getValue(), participantId.getValue()));
    }

    /**
     * Lists all the chat rooms.
     * TODO: Integrate gossip state.
     *
     * @param participantClient Participant client.
     */
    private void processChatRoomListRequest(ParticipantClient participantClient) {
        log.trace("Processing: participantClient={}", participantClient);
        Collection<RoomId> roomIds = systemState.serverRoomIds(currentServerId);
        ListClientResponse response = ListClientResponse.builder()
                .rooms(roomIds).build();
        String message = serializer.toJson(response);
        participantClient.sendMessage(message);
    }

    /**
     * Broadcast message to the room.
     * Sends message to everyone (except originator) in room.
     *
     * @param participantClient Participant client.
     * @param content           Content to broadcast.
     */
    private void processMessageRequest(ParticipantClient participantClient, String content) {
        log.trace("Processing: participantClient={} content={}", participantClient, content);
        MessageBroadcastResponse response = MessageBroadcastResponse.builder()
                .participantId(participantClient.getParticipantId())
                .content(content).build();
        String message = serializer.toJson(response);
        sendMessageToRoomExceptOriginator(participantClient.getCurrentRoomId(), message,
                participantClient.getClientId());
    }

    /**
     * Show room information.
     * 1. Find information for the response.
     * 2. Send who data list.
     *
     * @param participantClient Participant client.
     */
    private void processWhoRequest(ParticipantClient participantClient) {
        log.trace("Processing: participantClient={}", participantClient);
        // Find information for the response.
        RoomId currentRoomId = participantClient.getCurrentRoomId();
        ParticipantId roomOwnerId = systemState.getOwnerId(currentRoomId);
        Collection<ParticipantId> friendIds = new ArrayList<>();
        for (ClientId friendClientId : roomClientListMap.get(currentRoomId)) {
            ParticipantId fiendParticipantId = clientParticipantMap.get(friendClientId);
            friendIds.add(fiendParticipantId);
        }
        // Send who data list.
        WhoClientResponse response = WhoClientResponse.builder()
                .ownerId(roomOwnerId)
                .roomId(currentRoomId)
                .participantIds(friendIds).build();
        String message = serializer.toJson(response);
        participantClient.sendMessage(message);
    }

    /**
     * Create room.
     * 1. If room id is invalid locally, REJECT.
     * 2. Add client to waiting list.
     * 2. Send to leader via RAFT.
     *
     * @param participantClient Participant client.
     * @param roomId            Room to create.
     */
    @Synchronized
    private void processCreateRoomRequest(ParticipantClient participantClient, RoomId roomId) {
        log.trace("Processing: participantClient={} roomId={}", participantClient, roomId);
        // If room id is invalid locally, REJECT.
        if (systemState.hasRoom(roomId)) {
            CreateRoomClientResponse response = CreateRoomClientResponse.builder()
                    .roomId(roomId)
                    .approved(false).build();
            String message = serializer.toJson(response);
            participantClient.sendMessage(message);
            return;
        }
        // Add client to waiting list.
        waitingForRoomIdCreation.put(roomId, participantClient.getClientId());
        // TODO: Send to leader via RAFT.
        // Placeholder - put a log manually.
        systemState.apply(new CreateRoomLog(roomId.getValue(),
                participantClient.getParticipantId().getValue()));
    }

    /**
     * Delete room.
     * 1. If the room does not exist or client is not the owner of the room, REJECT
     * 2. Add client to waiting list.
     * 2. Send to leader via RAFT.
     *
     * @param participantClient Participant client.
     * @param roomId            Room to delete.
     */
    @Synchronized
    private void processDeleteRoomRequest(ParticipantClient participantClient, RoomId roomId) {
        log.trace("Processing: participantClient={} roomId={}", participantClient, roomId);
        // If the room does not exist or client is not the owner of the room, REJECT
        if (!systemState.hasRoom(roomId)
                || !participantClient.getParticipantId().equals(systemState.getOwnerId(roomId))) {
            DeleteRoomClientResponse response = DeleteRoomClientResponse.builder()
                    .roomId(roomId)
                    .approved(false).build();
            String message = serializer.toJson(response);
            participantClient.sendMessage(message);
            return;
        }
        // Add client to waiting list.
        waitingForRoomIdDeletion.put(roomId, participantClient.getClientId());
        // TODO: Send to leader via RAFT.
        // Placeholder - put a log manually.
        systemState.apply(new DeleteRoomLog(roomId.getValue()));
    }

    /**
     * Join room.
     * 1. Cant change if owns a room or room id is invalid.
     * 2. Update chat room maps.
     * 3. Send room change to all in new/old room.
     * TODO: Remove condition to move only in the same server
     *
     * @param participantClient Participant client.
     * @param roomId            Room to join.
     */
    @Synchronized
    private void processJoinRoomRequest(ParticipantClient participantClient, RoomId roomId) {
        log.trace("Processing: participantClient={} roomId={}", participantClient, roomId);
        // Cant change if owns a room or room id is invalid.
        RoomId formerRoomId = participantClient.getCurrentRoomId();
        ParticipantId participantId = participantClient.getParticipantId();
        ClientId clientId = participantClient.getClientId();
        if (!systemState.hasRoom(roomId)
                || systemState.owningRoom(participantId) != null
                || !currentServerId.equals(systemState.getRoomServerId(roomId))) {
            RoomChangeBroadcastResponse response1 = RoomChangeBroadcastResponse.builder()
                    .currentRoomId(formerRoomId)
                    .formerRoomId(formerRoomId)
                    .participantId(participantId).build();
            String message2 = serializer.toJson(response1);
            participantClient.sendMessage(message2);
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

    /**
     * Quit.
     *
     * @param participantClient Participant client.
     */
    @Synchronized
    private void processQuitRequest(ParticipantClient participantClient) {
        log.trace("Processing: participantClient={}", participantClient);
        ClientId clientId = participantClient.getClientId();
        RoomId currentRoomId = participantClient.getCurrentRoomId();

        allClients.remove(clientId);
        clientParticipantMap.remove(clientId);
        participantRoomMap.remove(participantClient.getParticipantId());
        roomClientListMap.get(currentRoomId).remove(clientId);

        RoomChangeBroadcastResponse response = RoomChangeBroadcastResponse.builder()
                .participantId(participantClient.getParticipantId())
                .currentRoomId(RoomId.NULL)
                .formerRoomId(currentRoomId).build();
        String message = serializer.toJson(response);
        sendMessageToRoom(currentRoomId, message);
        participantClient.sendMessage(message);
        try {
            participantClient.getChatClient().close();
        } catch (Exception ignored) {
        }

        systemState.apply(new DeleteIdentityLog(participantClient.getParticipantId().getValue()));
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
    public void participantIdCreated(@NonNull ParticipantId createParticipantId) {
        log.trace("Processing: createParticipantId={}", createParticipantId);
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
    public void roomIdCreated(@NonNull ParticipantId ownerId, @NonNull RoomId createdRoomId) {
        log.trace("Processing: ownerId={} createdRoomId={}", ownerId, createdRoomId);
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
    public void participantIdDeleted(@NonNull ParticipantId deletedId, RoomId deletedRoomId) {
        log.trace("Processing: deletedId={} deletedRoomId={}", deletedId, deletedRoomId);
        if (deletedRoomId == null) {
            // Nothing more to do
            return;
        }

        // Update chat room maps.
        // Remove old participants from room and add to main room.
        List<ClientId> prevClientIds = roomClientListMap.remove(deletedRoomId);
        roomClientListMap.get(mainRoomId).addAll(prevClientIds);
        for (ClientId prevClientId : prevClientIds) {
            ParticipantId prevParticipantId = clientParticipantMap.get(prevClientId);
            participantRoomMap.put(prevParticipantId, mainRoomId);
        }

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

    @Override
    @Synchronized
    public void roomIdDeleted(RoomId deletedRoomId) {
        log.trace("Processing: deletedRoomId={}", deletedRoomId);
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

    private Optional<ParticipantClient> getParticipantClient(ClientId clientId) {
        // Participant ID required section.
        // If client does not have a participant id, ignore.
        ParticipantId participantId = clientParticipantMap.get(clientId);
        if (participantId == null) {
            log.warn("Ignoring: Client({})", clientId);
            return Optional.empty();
        }
        // Ignore if not from this server.
        // This should not happen technically.
        RoomId currentRoomId = participantRoomMap.get(participantId);
        ServerId serverId = systemState.getRoomServerId(currentRoomId);
        if (!currentServerId.equals(serverId)) {
            log.warn("Wrong Server: Client({}) expected Server({})", clientId, serverId);
            return Optional.empty();
        }

        // Compose all the known client information.
        ChatClient chatClient = allClients.get(clientId);
        RoomId owningRoomId = systemState.owningRoom(participantId);
        ParticipantClient participantClient = ParticipantClient.builder()
                .clientId(clientId)
                .chatClient(chatClient)
                .participantId(participantId)
                .serverId(serverId)
                .currentRoomId(currentRoomId)
                .owningRoomId(owningRoomId).build();
        return Optional.of(participantClient);
    }

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

    @ToString
    @Getter
    @Builder
    private static class ParticipantClient {
        @NonNull
        private final ClientId clientId;
        @NonNull
        private final ChatClient chatClient;
        @NonNull
        private final ParticipantId participantId;
        @NonNull
        private final ServerId serverId;
        @NonNull
        private final RoomId currentRoomId;
        private final RoomId owningRoomId;

        private void sendMessage(String message) {
            chatClient.sendMessage(message);
        }
    }
}
