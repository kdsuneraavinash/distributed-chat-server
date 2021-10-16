package lk.ac.mrt.cse.cs4262.components.client.chat.events;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.tcp.TcpClient;
import lk.ac.mrt.cse.cs4262.components.client.chat.AuthenticatedClient;
import lk.ac.mrt.cse.cs4262.components.client.chat.ChatRoomState;
import lk.ac.mrt.cse.cs4262.components.client.chat.ChatRoomWaitingList;
import lk.ac.mrt.cse.cs4262.components.client.chat.MessageSender;
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
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.MoveJoinClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.MoveJoinValidateRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.CreateRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.DeleteRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.ListClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.MessageBroadcastResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.NewIdentityClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.RoomChangeBroadcastResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.WhoClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.RouteServerClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.MoveJoinValidateResponse;
import lk.ac.mrt.cse.cs4262.components.gossip.state.GossipStateReadView;
import lk.ac.mrt.cse.cs4262.components.raft.messages.CommandRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftStateReadView;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.BaseLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.DeleteIdentityLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.DeleteRoomLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.ServerChangeLog;
import lombok.Builder;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Log4j2
public class SocketEventHandler extends AbstractEventHandler implements ClientSocketListener.EventHandler {
    private final ServerId currentServerId;
    private final GossipStateReadView gossipState;
    private final RaftStateReadView raftState;
    private final ChatRoomState chatRoomState;
    private final ChatRoomWaitingList waitingList;
    private final Gson serializer;
    private final ServerConfiguration serverConfiguration;

    /**
     * Create a Event Handler for client socket. See {@link SocketEventHandler}.
     *
     * @param currentServerId ID of current server
     * @param gossipState     Gossip state
     * @param raftState       System state
     * @param chatRoomState   Chat room state object
     * @param waitingList     Waiting list
     * @param serializer      Serializer
     * @param messageSender   Message Sender
     * @param serverConfiguration ServerConfiguration
     */
    @Builder
    public SocketEventHandler(ServerId currentServerId,
                              GossipStateReadView gossipState, RaftStateReadView raftState,
                              ChatRoomState chatRoomState, ChatRoomWaitingList waitingList,
                              Gson serializer, @Nullable MessageSender messageSender,
                              ServerConfiguration serverConfiguration) {
        super(messageSender);
        this.currentServerId = currentServerId;
        this.gossipState = gossipState;
        this.raftState = raftState;
        this.chatRoomState = chatRoomState;
        this.waitingList = waitingList;
        this.serializer = serializer;
        this.serverConfiguration = serverConfiguration;
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
                    ServerId serverId = raftState.getServerOfRoom(currentRoomId).orElseThrow();
                    Optional<RoomId> owningRoomId = raftState.getRoomOwnedByParticipant(participantId);
                    AuthenticatedClient authenticatedClient = AuthenticatedClient.builder()
                            .clientId(clientId)
                            .participantId(participantId)
                            .serverId(serverId)
                            .currentRoomId(currentRoomId)
                            .owningRoomId(owningRoomId.orElse(null)).build();
                    return Optional.of(authenticatedClient);
                }).orElse(Optional.empty()));
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
        } else if (baseRequest instanceof MoveJoinClientRequest) {
            log.debug("MoveJoinClientRequest: {}", baseRequest);
            MoveJoinClientRequest request = (MoveJoinClientRequest) baseRequest;
            if (Optional.ofNullable(request.getIdentity()).isPresent()
                    || Optional.ofNullable(request.getFormer()).isPresent()
                    || Optional.ofNullable(request.getRoomId()).isPresent()) {
                processMoveJoinRequest(request, clientId);
                return false;
            }
            // TODO: Handle this case appropriately. Protocol doesn't specify any return type.
            return true;
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
        BaseLog baseLog = CreateIdentityLog.builder()
                .serverId(currentServerId).identity(participantId).build();
        // If participant id is invalid locally, REJECT
        // Add client to waiting list. If someone is already waiting, REJECT
        boolean acceptedLocally = raftState.isAcceptable(baseLog)
                && waitingList.waitForParticipantCreation(clientId, participantId);
        if (acceptedLocally) {
            sendCommandRequest(baseLog);
            return;
        }
        // Send REJECT message
        String message = createParticipantCreateRejectedMsg();
        sendToClient(clientId, message);
    }

    /**
     * Lists all the chat rooms.
     * TODO: Integrate gossip state.
     *
     * @param authenticatedClient Authenticated client.
     */
    private void processChatRoomListRequest(AuthenticatedClient authenticatedClient) {
        log.traceEntry("authenticatedClient={}", authenticatedClient);
        log.info("failedKnownServerIds={}", gossipState.failedServerIds());
        // TODO: Integrate Gossip state
        Collection<RoomId> roomIds = raftState.getRoomsInSystem();
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
        sendToRoom(authenticatedClient.getCurrentRoomId(), message, authenticatedClient.getClientId());
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
        raftState.getOwnerOfRoom(currentRoomId).ifPresent(roomOwnerId -> {
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
        ParticipantId participantId = authenticatedClient.getParticipantId();
        BaseLog baseLog = CreateRoomLog.builder()
                .roomId(roomId).participantId(participantId).build();
        // If room id is invalid locally, REJECT
        // Add client to waiting list. If someone is already waiting, REJECT
        boolean acceptedLocally = raftState.isAcceptable(baseLog)
                && waitingList.waitForRoomCreation(clientId, roomId);
        if (acceptedLocally) {
            sendCommandRequest(baseLog);
            return;
        }
        // Send REJECT message
        String message = createRoomCreateRejectedMsg(roomId);
        sendToClient(clientId, message);
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
        BaseLog baseLog = DeleteRoomLog.builder()
                .roomId(roomId).build();
        // If the room does not exist, REJECT
        // If client is not the owner of the room, REJECT
        // Add client to waiting list. If someone is already waiting, REJECT
        boolean isSameOwner = raftState.getOwnerOfRoom(roomId)
                .map(authenticatedClient.getParticipantId()::equals).orElse(false);
        boolean acceptedLocally = raftState.isAcceptable(baseLog)
                && isSameOwner
                && waitingList.waitForRoomDeletion(clientId, roomId);
        if (acceptedLocally) {
            sendCommandRequest(baseLog);
            return;
        }
        // Send REJECT message
        String message = createRoomDeleteRejectedMsg(roomId);
        sendToClient(clientId, message);
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
        boolean accepted = raftState.hasRoom(roomId) && raftState.getRoomOwnedByParticipant(participantId).isEmpty();
        if (!accepted) {
            String message = createRoomChangeBroadcastMsg(participantId, formerRoomId, formerRoomId);
            sendToClient(clientId, message);
            return;
        }
        boolean isSameServer = raftState.getServerOfRoom(roomId)
                .map(currentServerId::equals).orElse(false);
        if (isSameServer) {
            // Update chat room maps.
            chatRoomState.roomJoinInternal(clientId, roomId);
            // Send room change to all in new/old room.
            String message = createRoomChangeBroadcastMsg(participantId, formerRoomId, roomId);
            sendToRoom(formerRoomId, message);
            sendToRoom(roomId, message);
        } else {
            waitingList.waitForServerChange(participantId, roomId);
            // Safe to directly unwrap optional due to previous check and synchronized methods
            ServerId serverId = raftState.getServerOfRoom(roomId).get();
            String serverAddress = serverConfiguration.getServerAddress(serverId).orElse(null);
            Integer serverPort = serverConfiguration.getClientPort(serverId).orElse(null);
            if (serverAddress == null || serverPort == null) {
                String message = createRoomChangeBroadcastMsg(participantId, formerRoomId, formerRoomId);
                sendToClient(clientId, message);
                return;
            }
            String broadcastMsg = createRoomChangeBroadcastMsg(participantId, formerRoomId, roomId);
            String message = createRouteMsg(roomId, serverAddress, serverPort);
            sendToRoom(formerRoomId, broadcastMsg, clientId);
            sendToClient(clientId, message);
        }

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
        // Send room change to all in old room and to client.
        String message = createRoomChangeBroadcastMsg(participantId, currentRoomId, RoomId.NULL);
        sendToRoom(currentRoomId, message);
        sendToClient(clientId, message);
        disconnectClient(clientId);
        // TODO: Send to leader via RAFT.
        // For now put a log manually.
        BaseLog baseLog = DeleteIdentityLog.builder()
                .identity(participantId).build();
        sendCommandRequest(baseLog);
    }

    /**
     * Process requests of MoveJoin type sent by the client. Request is locally validated
     * by checking for required fields and then validated against the source server of the
     * movejoin process and if validated command request is sent to the leader for log replication.
     * @param request - MoveJoinClientRequest
     * @param clientId - Client ID
     */
    @Synchronized
    private void processMoveJoinRequest(MoveJoinClientRequest request, ClientId clientId) {
        log.debug("MoveJoinRequest by={}", request);
        ParticipantId participantId = new ParticipantId(Optional.ofNullable(request.getIdentity()).orElseThrow());
        RoomId formerRoomId = new RoomId(Optional.ofNullable(request.getFormer()).orElseThrow());
        RoomId newRoomId = new RoomId(Optional.ofNullable(request.getRoomId()).orElseThrow());
        MoveJoinValidateRequest validateRequest = new MoveJoinValidateRequest(participantId.getValue(),
                newRoomId.getValue());
        // TODO: Handle the invalid cases properly. Protocol doesn't specify.
        chatRoomState.participantCreate(clientId, participantId);
        raftState.getServerOfRoom(formerRoomId).ifPresent(serverId -> {
            String formerServerAddress = serverConfiguration.getServerAddress(serverId).orElse(null);
            Integer formerServerPort = serverConfiguration.getCoordinationPort(serverId).orElse(null);
            if (formerServerAddress == null || formerServerPort == null) {
                return;
            }
            try {
                String response = TcpClient.request(formerServerAddress, formerServerPort,
                        serializer.toJson(validateRequest),
                        serverConfiguration.getTcpTimeout());
                log.traceEntry("MoveJoin validation response: {} ", response);
                MoveJoinValidateResponse validateResponse = serializer.fromJson(response,
                        MoveJoinValidateResponse.class);
                if (validateResponse.isValidated()) {
                    // Add to waitinglist and send command to leader
                    waitingList.waitForServerChange(participantId, newRoomId);
                    BaseLog baselog = ServerChangeLog.builder().formerServerId(serverId)
                            .newServerId(currentServerId).participantId(participantId).build();
                    log.traceEntry("ServerChangeLog: {}", baselog);
                    sendCommandRequest(baselog);
                }
            } catch (IOException e) {
                log.error("Error: {}", e.toString());
                e.printStackTrace();
            }
        });
    }

    /**
     * Validates a MoveJoin request by comparing the room id of a participant stored
     * on the source server. Destination server sends a MoveJoinValidateRequest to the
     * source server and the source server uses this method to validate against room id.
     * @param participantId - Participant ID
     * @param roomId - Room ID
     * @return - Validated or not
     */
    @Synchronized
    public boolean validateMoveJoinRequest(ParticipantId participantId, RoomId roomId) {
        return waitingList.getWaitingForServerChange(participantId).map(roomIdSaved ->
                roomIdSaved.equals(roomId)).orElse(false);
    }

    /**
     * Send a message to the leader.
     *
     * @param command Command to send.
     */
    private void sendCommandRequest(BaseLog command) {
        CommandRequestMessage message = CommandRequestMessage.builder()
                .senderId(currentServerId)
                .command(command).build();
        raftState.getLeaderId()
                .ifPresent(leaderId -> sendToServer(leaderId, serializer.toJson(message)));
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

    private String createRouteMsg(RoomId roomId, String host, Integer port) {
        RouteServerClientResponse response = RouteServerClientResponse.builder()
                .roomId(roomId).host(host).port(port).build();
        return serializer.toJson(response);
    }
}
