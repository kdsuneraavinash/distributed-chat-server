package lk.ac.mrt.cse.cs4262.components.client.chat;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.common.state.SystemState;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.DeleteIdentityLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.DeleteRoomLog;
import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.client.chat.client.ClientSocketListener;
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
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Log4j2
@Builder
public class ChatSocketReporter implements ClientSocketListener.Reporter {
    private final ServerId currentServerId;
    private final SystemState systemState;
    private final ChatRoomState chatRoomState;
    private final ChatRoomWaitingList waitingList;
    private final Gson serializer;

    @Nullable
    private MessageSender messageSender;

    /**
     * Attach a message sender to this reporter.
     *
     * @param newMessageSender Message Sender.
     */
    public void attachMessageSender(MessageSender newMessageSender) {
        this.messageSender = newMessageSender;
    }

    /*
    ========================================================
    Private Helpers
    ========================================================
    */

    /**
     * Authenticate (check participant id, etc.) of a client.
     *
     * @param clientId ID of client.
     * @return All gathered client information.
     */
    private Optional<AuthenticatedClient> authenticate(ClientId clientId) {
        return chatRoomState.getParticipantIdOf(clientId).flatMap(participantId ->
                chatRoomState.getCurrentRoomIdOf(participantId).map(currentRoomId -> {
                    ServerId serverId = systemState.getServerOfRoom(currentRoomId).orElseThrow();
                    Optional<RoomId> owningRoomId = systemState.getRoomOwnedByParticipant(participantId);
                    AuthenticatedClient authenticatedClient = AuthenticatedClient.builder()
                            .clientId(clientId)
                            .participantId(participantId)
                            .serverId(serverId)
                            .currentRoomId(currentRoomId)
                            .owningRoomId(owningRoomId.orElse(null)).build();
                    return Optional.of(authenticatedClient);
                }).orElse(Optional.empty()));
    }

    private void sendToClient(ClientId clientId, String message) {
        if (messageSender != null) {
            messageSender.sendToClient(clientId, message);
        }
    }

    private void sendToRoom(RoomId roomId, String message) {
        if (messageSender != null) {
            messageSender.sendToRoom(roomId, message);
        }
    }

    private void disconnectClient(ClientId clientId) {
        if (messageSender != null) {
            messageSender.disconnect(clientId);
        }
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
            // New Identity
            NewIdentityClientRequest request = (NewIdentityClientRequest) baseRequest;
            // Process only if identity field is present.
            Optional.ofNullable(request.getIdentity()).ifPresent(participantId ->
                    processNewIdentityRequest(clientId, new ParticipantId(participantId)));
            return false;
        } else {
            return authenticate(clientId).map(authenticatedClient -> {
                if (baseRequest instanceof ListClientRequest) {
                    // List
                    processChatRoomListRequest(authenticatedClient);
                } else if (baseRequest instanceof MessageClientRequest) {
                    // Message
                    Optional.ofNullable(((MessageClientRequest) baseRequest).getContent())
                            .ifPresent(content -> processMessageRequest(authenticatedClient, content));
                } else if (baseRequest instanceof WhoClientRequest) {
                    // Who
                    processWhoRequest(authenticatedClient);
                } else if (baseRequest instanceof CreateRoomClientRequest) {
                    // Create Room
                    Optional.ofNullable(((CreateRoomClientRequest) baseRequest).getRoomId())
                            .ifPresent(roomId -> processCreateRoomRequest(authenticatedClient, new RoomId(roomId)));
                } else if (baseRequest instanceof DeleteRoomClientRequest) {
                    // Delete Room
                    Optional.ofNullable(((DeleteRoomClientRequest) baseRequest).getRoomId())
                            .ifPresent(roomId -> processDeleteRoomRequest(authenticatedClient, new RoomId(roomId)));
                } else if (baseRequest instanceof JoinRoomClientRequest) {
                    // Join Room
                    Optional.ofNullable(((JoinRoomClientRequest) baseRequest).getRoomId())
                            .ifPresent(roomId -> processJoinRoomRequest(authenticatedClient, new RoomId(roomId)));
                } else if (baseRequest instanceof QuitClientRequest) {
                    // Quit
                    processQuitRequest(authenticatedClient);
                    return true;
                } else {
                    // Unknown
                    log.warn("Unknown command from Client({}): {}", clientId, rawRequest);
                }
                return false;
            }).orElse(false);
        }
    }

    @Override
    public void clientSideDisconnect(ClientId clientId) {
        authenticate(clientId).ifPresent(this::processQuitRequest);
    }

    /*
    ========================================================
    Private Handlers for Client Events
    ========================================================
     */

    /**
     * Create new identity for user.
     *
     * @param clientId      Client requesting identity.
     * @param participantId Requested identity.
     */
    @Synchronized
    private void processNewIdentityRequest(ClientId clientId, ParticipantId participantId) {
        log.traceEntry("clientId={} participantId={}", clientId, participantId);
        // If participant id is invalid locally, REJECT
        // Add client to waiting list. If someone is already waiting, REJECT
        boolean acceptedLocally = !systemState.hasParticipant(participantId)
                && waitingList.waitForParticipantCreation(clientId, participantId);
        if (acceptedLocally) {
            String message = createParticipantCreateRejectedMsg();
            sendToClient(clientId, message);
            return;
        }
        // TODO: Send to leader via RAFT.
        // For now put a log manually.
        systemState.apply(new CreateIdentityLog(currentServerId.getValue(), participantId.getValue()));
    }

    /**
     * Lists all the chat rooms.
     * TODO: Integrate gossip state.
     *
     * @param authenticatedClient Authenticated client.
     */
    private void processChatRoomListRequest(AuthenticatedClient authenticatedClient) {
        log.traceEntry("authenticatedClient={}", authenticatedClient);
        // TODO: Integrate Gossip state
        Collection<RoomId> roomIds = systemState.getRoomsInServer(currentServerId);
        String message = createRoomListMsg(roomIds);
        sendToClient(authenticatedClient.getClientId(), message);
    }

    /**
     * Broadcast message to the room.
     * Sends message to everyone (except originator) in room.
     *
     * @param authenticatedClient Authenticated client.
     * @param content             Content to broadcast.
     */
    private void processMessageRequest(AuthenticatedClient authenticatedClient, String content) {
        log.traceEntry("authenticatedClient={} content={}", authenticatedClient, content);
        String message = createMessageBroadcastMsg(authenticatedClient.getParticipantId(), content);
        sendToClient(authenticatedClient.getClientId(), message);
    }

    /**
     * Show room information.
     *
     * @param authenticatedClient Authenticated client.
     */
    private void processWhoRequest(AuthenticatedClient authenticatedClient) {
        log.traceEntry("authenticatedClient={}", authenticatedClient);
        // Find information for the response.
        RoomId currentRoomId = authenticatedClient.getCurrentRoomId();
        systemState.getOwnerOfRoom(currentRoomId).ifPresent(roomOwnerId -> {
            Collection<ParticipantId> friendIds = new ArrayList<>();
            for (ClientId friendClientId : chatRoomState.getClientIdsOf(currentRoomId)) {
                chatRoomState.getParticipantIdOf(friendClientId).ifPresent(friendIds::add);
            }
            String message = createWhoMsg(roomOwnerId, friendIds, currentRoomId);
            sendToClient(authenticatedClient.getClientId(), message);
        });
    }

    /**
     * Create room.
     *
     * @param authenticatedClient Authenticated client.
     * @param roomId              Room to create.
     */
    @Synchronized
    private void processCreateRoomRequest(AuthenticatedClient authenticatedClient, RoomId roomId) {
        log.traceEntry("authenticatedClient={} roomId={}", authenticatedClient, roomId);
        ClientId clientId = authenticatedClient.getClientId();
        // If room id is invalid locally, REJECT
        // Add client to waiting list. If someone is already waiting, REJECT
        boolean acceptedLocally = !systemState.hasRoom(roomId)
                && waitingList.waitForRoomCreation(clientId, roomId);
        if (!acceptedLocally) {
            String message = createRoomCreateRejectedMsg(roomId);
            sendToClient(clientId, message);
            return;
        }
        // TODO: Send to leader via RAFT.
        // For now put a log manually.
        systemState.apply(new CreateRoomLog(roomId.getValue(),
                authenticatedClient.getParticipantId().getValue()));
    }

    /**
     * Delete room.
     *
     * @param authenticatedClient Authenticated client.
     * @param roomId              Room to delete.
     */
    @Synchronized
    private void processDeleteRoomRequest(AuthenticatedClient authenticatedClient, RoomId roomId) {
        log.traceEntry("authenticatedClient={} roomId={}", authenticatedClient, roomId);
        ClientId clientId = authenticatedClient.getClientId();
        // If the room does not exist, REJECT
        // If client is not the owner of the room, REJECT
        // Add client to waiting list. If someone is already waiting, REJECT
        boolean isSameOwner = systemState.getOwnerOfRoom(roomId)
                .map(authenticatedClient.getParticipantId()::equals).orElse(false);
        boolean acceptedLocally = !systemState.hasRoom(roomId)
                && isSameOwner
                && waitingList.waitForRoomDeletion(clientId, roomId);
        if (acceptedLocally) {
            String message = createRoomDeleteRejectedMsg(roomId);
            sendToClient(clientId, message);
            return;
        }
        // TODO: Send to leader via RAFT.
        // For now put a log manually.
        systemState.apply(new DeleteRoomLog(roomId.getValue()));
    }

    /**
     * Join room.
     * TODO: Remove condition to move only in the same server
     *
     * @param authenticatedClient Authenticated client.
     * @param roomId              Room to join.
     */
    @Synchronized
    private void processJoinRoomRequest(AuthenticatedClient authenticatedClient, RoomId roomId) {
        log.traceEntry("authenticatedClient={} roomId={}", authenticatedClient, roomId);
        // Cant change if owns a room or room id is invalid.
        RoomId formerRoomId = authenticatedClient.getCurrentRoomId();
        ParticipantId participantId = authenticatedClient.getParticipantId();
        ClientId clientId = authenticatedClient.getClientId();
        // If the room does not exist, REJECT
        // If client owns a room, REJECT
        // If the room not in same server, REJECT
        boolean isSameServer = systemState.getServerOfRoom(roomId)
                .map(currentServerId::equals).orElse(false);
        boolean acceptedLocally = systemState.hasRoom(roomId)
                && systemState.getRoomOwnedByParticipant(participantId).isPresent()
                && isSameServer;
        if (acceptedLocally) {
            String message = createRoomChangeBroadcastMsg(participantId, formerRoomId, formerRoomId);
            sendToClient(clientId, message);
            return;
        }
        // Update chat room maps.
        chatRoomState.roomJoinInternal(clientId, roomId);
        // Send room change to all in new/old room.
        String message = createRoomChangeBroadcastMsg(participantId, formerRoomId, roomId);
        sendToRoom(formerRoomId, message);
        sendToRoom(roomId, message);
    }

    /**
     * Quit.
     *
     * @param authenticatedClient Authenticated client.
     */
    @Synchronized
    private void processQuitRequest(AuthenticatedClient authenticatedClient) {
        log.traceEntry("authenticatedClient={}", authenticatedClient);
        ClientId clientId = authenticatedClient.getClientId();
        RoomId currentRoomId = authenticatedClient.getCurrentRoomId();
        ParticipantId participantId = authenticatedClient.getParticipantId();
        // Update chat room maps.
        chatRoomState.participantQuit(clientId);
        // Send room change to all in old room and to client.
        String message = createRoomChangeBroadcastMsg(participantId, currentRoomId, RoomId.NULL);
        sendToRoom(currentRoomId, message);
        sendToClient(clientId, message);
        disconnectClient(clientId);
        // TODO: Send to leader via RAFT.
        // For now put a log manually.
        systemState.apply(new DeleteIdentityLog(participantId.getValue()));
    }

    /*
    ========================================================
    Response message creators
    ========================================================
    */

    private String createRoomCreateRejectedMsg(RoomId createdRoomId) {
        CreateRoomClientResponse response = CreateRoomClientResponse.builder()
                .roomId(createdRoomId).approved(false).build();
        return serializer.toJson(response);
    }

    private String createRoomDeleteRejectedMsg(RoomId deletedRoomId) {
        DeleteRoomClientResponse response = DeleteRoomClientResponse.builder()
                .roomId(deletedRoomId).approved(false).build();
        return serializer.toJson(response);
    }

    private String createParticipantCreateRejectedMsg() {
        NewIdentityClientResponse response = NewIdentityClientResponse.builder()
                .approved(false).build();
        return serializer.toJson(response);
    }

    private String createRoomChangeBroadcastMsg(ParticipantId participantId, RoomId fromRoomId, RoomId toRoomId) {
        RoomChangeBroadcastResponse response = RoomChangeBroadcastResponse.builder()
                .participantId(participantId)
                .formerRoomId(fromRoomId)
                .currentRoomId(toRoomId).build();
        return serializer.toJson(response);
    }

    private String createRoomListMsg(Collection<RoomId> roomIds) {
        ListClientResponse response = ListClientResponse.builder()
                .rooms(roomIds).build();
        return serializer.toJson(response);
    }

    private String createMessageBroadcastMsg(ParticipantId participantId, String content) {
        MessageBroadcastResponse response = MessageBroadcastResponse.builder()
                .participantId(participantId).content(content).build();
        return serializer.toJson(response);
    }

    private String createWhoMsg(ParticipantId ownerId, Collection<ParticipantId> friendIds, RoomId roomId) {
        WhoClientResponse response = WhoClientResponse.builder()
                .ownerId(ownerId).roomId(roomId).participantIds(friendIds).build();
        return serializer.toJson(response);
    }
}
