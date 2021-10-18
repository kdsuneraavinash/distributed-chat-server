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
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.MoveJoinClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.MoveJoinValidateRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.NewIdentityClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.QuitClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.WhoClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.CommandAckResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.CreateRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.DeleteRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.ListClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.MessageBroadcastResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.MoveJoinClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.MoveJoinValidateResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.NewIdentityClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.RoomChangeBroadcastResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.RouteServerClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.WhoClientResponse;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Log4j2
public class SocketEventHandler extends AbstractEventHandler implements ClientSocketListener.EventHandler {
    private static final int TCP_TIMEOUT = 5000;
    private final ServerId currentServerId;
    private final GossipStateReadView gossipState;
    private final RaftStateReadView raftState;
    private final ChatRoomState chatRoomState;
    private final ChatRoomWaitingList waitingList;
    private final Gson serializer;
    private final ServerConfiguration serverConfiguration;

    /**
     * Participants that are moved from this server.
     * These are participants that sent a joinroom request.
     */
    private final Map<ParticipantId, RoomId> movedParticipants;

    /**
     * Create a Event Handler for client socket. See {@link SocketEventHandler}.
     *
     * @param currentServerId     ID of current server
     * @param gossipState         Gossip state
     * @param raftState           System state
     * @param chatRoomState       Chat room state object
     * @param waitingList         Waiting list
     * @param serializer          Serializer
     * @param messageSender       Message Sender
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
        this.movedParticipants = new HashMap<>();
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
        if (!chatRoomState.isParticipant(clientId)) {
            return Optional.empty();
        }
        ParticipantId participantId = chatRoomState.getParticipantIdOf(clientId);
        RoomId currentRoomId = chatRoomState.getCurrentRoomIdOf(participantId);
        ServerId serverId = raftState.getServerOfRoom(currentRoomId);
        Optional<RoomId> owningRoomId = raftState.getRoomOwnedByParticipant(participantId);
        AuthenticatedClient authenticatedClient = AuthenticatedClient.builder()
                .clientId(clientId)
                .participantId(participantId)
                .serverId(serverId)
                .currentRoomId(currentRoomId)
                .owningRoomId(owningRoomId.orElse(null)).build();
        return Optional.of(authenticatedClient);
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
            String identity = request.getIdentity();
            if (identity != null) {
                processNewIdentityRequest(clientId, new ParticipantId(identity));
            }
            return false;
        } else if (baseRequest instanceof MoveJoinClientRequest) {
            MoveJoinClientRequest request = (MoveJoinClientRequest) baseRequest;
            String identity = request.getIdentity();
            String formerRoomId = request.getFormer();
            String newRoomId = request.getRoomId();
            if (identity != null && formerRoomId != null && newRoomId != null) {
                processMoveJoinRequest(clientId, new ParticipantId(identity),
                        new RoomId(formerRoomId), new RoomId(newRoomId));
            }
            return false;
        } else {
            Optional<AuthenticatedClient> authenticatedClientOp = authenticate(clientId);
            if (authenticatedClientOp.isEmpty()) {
                // Ignore if the client is not authenticated
                log.warn("Client({}) tried to message is not authenticated", clientId);
                return false;
            }
            AuthenticatedClient authenticatedClient = authenticatedClientOp.get();
            if (movedParticipants.containsKey(authenticatedClient.getParticipantId())) {
                // Moved participant, IGNORE any other requests.
                return true;
            } else if (baseRequest instanceof ListClientRequest) {
                // List
                processChatRoomListRequest(authenticatedClient);
            } else if (baseRequest instanceof MessageClientRequest) {
                // Message
                String content = ((MessageClientRequest) baseRequest).getContent();
                if (content != null) {
                    processMessageRequest(authenticatedClient, content);
                }
            } else if (baseRequest instanceof WhoClientRequest) {
                // Who
                processWhoRequest(authenticatedClient);
            } else if (baseRequest instanceof CreateRoomClientRequest) {
                // Create Room
                String roomId = ((CreateRoomClientRequest) baseRequest).getRoomId();
                if (roomId != null) {
                    processCreateRoomRequest(authenticatedClient, new RoomId(roomId));
                }
            } else if (baseRequest instanceof DeleteRoomClientRequest) {
                // Delete Room
                String roomId = ((DeleteRoomClientRequest) baseRequest).getRoomId();
                if (roomId != null) {
                    processDeleteRoomRequest(authenticatedClient, new RoomId(roomId));
                }
            } else if (baseRequest instanceof JoinRoomClientRequest) {
                // Join Room
                String roomId = ((JoinRoomClientRequest) baseRequest).getRoomId();
                if (roomId != null) {
                    processJoinRoomRequest(authenticatedClient, new RoomId(roomId));
                }
            } else if (baseRequest instanceof QuitClientRequest) {
                // Quit
                processQuitRequest(authenticatedClient);
                return true;
            } else {
                // Unknown
                log.warn("Unknown command from Client({}): {}", clientId, rawRequest);
            }
            return false;
        }
    }

    @Override
    public void clientSideDisconnect(ClientId clientId) {
        Optional<AuthenticatedClient> authenticatedClientOp = authenticate(clientId);
        // Send quit message only if authenticated. (has a participant id)
        authenticatedClientOp.ifPresent(this::processQuitRequest);
        waitingList.removeClientFromAllWaitingLists(clientId);
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
                && waitingList.getWaitingForCreation(participantId).isEmpty();
        if (acceptedLocally && sendCommandRequest(baseLog)) {
            waitingList.waitForParticipantCreation(participantId, clientId);
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
        ParticipantId roomOwnerId = raftState.getOwnerOfRoom(currentRoomId);
        Collection<ParticipantId> friendParticipantIds = new ArrayList<>();
        for (ClientId friendClientId : chatRoomState.getClientIdsOf(currentRoomId)) {
            ParticipantId friendParticipantId = chatRoomState
                    .getParticipantIdOf(friendClientId);
            friendParticipantIds.add(friendParticipantId);
        }
        String message = createWhoMsg(roomOwnerId, friendParticipantIds, currentRoomId);
        sendToClient(authenticatedClient.getClientId(), message);
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
                && waitingList.getWaitingForCreation(roomId).isEmpty();
        if (acceptedLocally && sendCommandRequest(baseLog)) {
            waitingList.waitForRoomCreation(roomId, clientId);
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
        boolean acceptedLocally = raftState.isAcceptable(baseLog)
                && raftState.getOwnerOfRoom(roomId).equals(authenticatedClient.getParticipantId())
                && waitingList.getWaitingForDeletion(roomId).isEmpty();
        if (acceptedLocally && sendCommandRequest(baseLog)) {
            waitingList.waitForRoomDeletion(roomId, clientId);
            return;
        }
        // Send REJECT message
        String message = createRoomDeleteRejectedMsg(roomId);
        sendToClient(clientId, message);
    }

    /**
     * Join room.
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
        // If moving to same room, REJECT
        // If client owns a room, REJECT
        boolean accepted = raftState.hasRoom(roomId) && !formerRoomId.equals(roomId)
                && raftState.getRoomOwnedByParticipant(participantId).isEmpty();
        if (!accepted) {
            String message = createRoomChangeBroadcastMsg(participantId, formerRoomId, formerRoomId);
            sendToClient(clientId, message);
            return;
        }
        boolean isSameServer = currentServerId.equals(raftState.getServerOfRoom(roomId));
        if (isSameServer) {
            // Update chat room maps.
            chatRoomState.roomJoinInternal(clientId, roomId);
            // Send room change to all in new/old room.
            String message = createRoomChangeBroadcastMsg(participantId, formerRoomId, roomId);
            sendToRoom(formerRoomId, message);
            sendToRoom(roomId, message);
        } else {
            // Safe to directly unwrap optional due to previous check and synchronized methods
            ServerId serverId = raftState.getServerOfRoom(roomId);
            String serverAddress = serverConfiguration.getServerAddress(serverId);
            Integer serverPort = serverConfiguration.getClientPort(serverId);
            // Remember as a moved participant
            movedParticipants.put(participantId, formerRoomId);
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

        // If the client disconnect is due to server change, simply disconnect the client.
        if (movedParticipants.remove(participantId) != null) {
            disconnectClient(clientId);
            return;
        }
        // Send room change to all in old room and to client.
        String message = createRoomChangeBroadcastMsg(participantId, currentRoomId, RoomId.NULL);
        sendToRoom(currentRoomId, message);
        sendToClient(clientId, message);
        disconnectClient(clientId);
        // Send the log, and ignore whether it is accepted or not
        BaseLog baseLog = DeleteIdentityLog.builder()
                .identity(participantId).build();
        sendCommandRequest(baseLog);
    }

    /**
     * Process requests of MoveJoin type sent by the client. Request is locally validated
     * by checking for required fields and then validated against the source server of the
     * movejoin process and if validated command request is sent to the leader for log replication.
     *
     * @param clientId      Client ID
     * @param participantId Participant ID
     * @param formerRoomId  Former Room ID
     * @param newRoomId     New Joining Room ID
     */
    @Synchronized
    private void processMoveJoinRequest(ClientId clientId, ParticipantId participantId,
                                        RoomId formerRoomId, RoomId newRoomId) {
        log.info("clientId={} participantId={} formerRoomId={} newRoomId={}",
                clientId, participantId, formerRoomId, newRoomId);

        ServerId formerServerId = raftState.getServerOfRoom(formerRoomId);
        String formerServerAddress = serverConfiguration.getServerAddress(formerServerId);
        Integer formerServerPort = serverConfiguration.getCoordinationPort(formerServerId);

        // If someone already waiting for the same server change, REJECT
        if (waitingList.getWaitingForServerChange(participantId).isEmpty()) {
            try {
                // Send validation request to former server and check if the client
                // really disconnected from the said room.
                MoveJoinValidateRequest validateRequest = MoveJoinValidateRequest.builder()
                        .formerRoomId(formerRoomId.getValue())
                        .participantId(participantId.getValue()).build();
                String response = TcpClient.request(formerServerAddress, formerServerPort,
                        serializer.toJson(validateRequest), TCP_TIMEOUT);
                MoveJoinValidateResponse validateResponse = serializer.fromJson(response,
                        MoveJoinValidateResponse.class);

                // If valid, remember client and send a log command to leader of this change.
                if (validateResponse.isValidated()) {
                    // Add to waitinglist and send command to leader
                    BaseLog baselog = ServerChangeLog.builder().formerServerId(formerServerId)
                            .newServerId(currentServerId).participantId(participantId).build();
                    if (sendCommandRequest(baselog)) {
                        waitingList.waitForServerChange(participantId, clientId, formerRoomId, newRoomId);
                        return;
                    }
                }

            } catch (IOException e) {
                log.fatal("error: {}", e.toString());
                log.throwing(e);
            }
        }

        // If log sending failed or invalid, send REJECT message
        ServerId newServerId = raftState.getServerOfRoom(newRoomId);
        String rejectedMsg = createMoveJoinRejectMsg(newServerId);
        sendToClient(clientId, rejectedMsg);
    }

    /**
     * Validates a MoveJoin request by comparing the room id of a participant stored
     * on the source server. Destination server sends a MoveJoinValidateRequest to the
     * source server and the source server uses this method to validate against room id.
     *
     * @param participantId Participant ID
     * @param roomId        Room ID
     * @return Validated or not
     */
    @Synchronized
    public boolean validateMoveJoinRequest(ParticipantId participantId, RoomId roomId) {
        if (movedParticipants.containsKey(participantId)) {
            return roomId.equals(movedParticipants.get(participantId));
        }
        return false;
    }

    /**
     * Send a message to the leader.
     *
     * @param command Command to send.
     * @return Whether leader accepted message.
     */
    public boolean sendCommandRequest(BaseLog command) {
        CommandRequestMessage message = CommandRequestMessage.builder()
                .senderId(currentServerId)
                .command(command).build();
        // Command requests can be sent only if there is a known leader.
        if (raftState.getLeaderId().isPresent()) {
            ServerId leaderId = raftState.getLeaderId().get();
            String serverAddress = serverConfiguration.getServerAddress(leaderId);
            int coordinationPort = serverConfiguration.getCoordinationPort(leaderId);
            try {
                String rawResponse = TcpClient.request(serverAddress, coordinationPort,
                        serializer.toJson(message), TCP_TIMEOUT);
                CommandAckResponse response = serializer.fromJson(rawResponse, CommandAckResponse.class);
                return response.isAccepted();
            } catch (IOException ignored) {
            }
        }
        return false;
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

    private String createMoveJoinRejectMsg(ServerId serverId) {
        MoveJoinClientResponse response = MoveJoinClientResponse.builder()
                .serverId(serverId.getValue()).approved(false).build();
        return serializer.toJson(response);
    }
}
