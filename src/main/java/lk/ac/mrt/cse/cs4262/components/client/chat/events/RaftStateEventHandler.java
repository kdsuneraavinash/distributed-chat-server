package lk.ac.mrt.cse.cs4262.components.client.chat.events;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.client.chat.ChatRoomState;
import lk.ac.mrt.cse.cs4262.components.client.chat.ChatRoomWaitingList;
import lk.ac.mrt.cse.cs4262.components.client.chat.MessageSender;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.CreateRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.DeleteRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.MoveJoinClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.NewIdentityClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.RoomChangeBroadcastResponse;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftStateReadView;
import lombok.Builder;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * An event handler that will listen to updates from system state.
 * This will deque waiting list and send messages to them.
 */
@Log4j2
public class RaftStateEventHandler extends AbstractEventHandler implements RaftStateReadView.EventHandler {
    private final RoomId mainRoomId;
    private final ChatRoomState chatRoomState;
    private final ChatRoomWaitingList waitingList;
    private final Gson serializer;

    /**
     * Create a Event Handler for System State. See {@link RaftStateEventHandler}.
     *
     * @param mainRoomId    ID of main room
     * @param chatRoomState Chat room state object
     * @param waitingList   Waiting list
     * @param serializer    Serializer
     * @param messageSender Message Sender
     */
    @Builder
    public RaftStateEventHandler(RoomId mainRoomId,
                                 ChatRoomState chatRoomState, ChatRoomWaitingList waitingList,
                                 Gson serializer, @Nullable MessageSender messageSender) {
        super(messageSender);
        this.mainRoomId = mainRoomId;
        this.chatRoomState = chatRoomState;
        this.waitingList = waitingList;
        this.serializer = serializer;
    }

    /*
    ========================================================
    State Machine Event Handling
    TODO: Handle errors/corner cases
    ========================================================
    */

    @Synchronized
    @Override
    public void participantIdCreated(ParticipantId createdParticipantId) {
        log.traceEntry("createdParticipantId={}", createdParticipantId);
        // Get client from waiting list. If no one waiting, fake the client.
        ClientId clientId = waitingList.removeWaitingForParticipantEvent(createdParticipantId)
                .orElseGet(ClientId::fake);

        // Update chat room maps.
        chatRoomState.createParticipant(createdParticipantId, clientId, mainRoomId);
        // Send APPROVED message to client.
        String message1 = createParticipantCreateAcceptedMsg();
        sendToClient(clientId, message1);
        // Send room change to all in main room.
        String message2 = createRoomChangeBroadcastMsg(createdParticipantId, RoomId.NULL, mainRoomId);
        sendToRoom(mainRoomId, message2);
    }

    @Synchronized
    @Override
    public void roomIdCreated(ParticipantId ownerParticipantId, RoomId createdRoomId) {
        log.traceEntry("ownerId={} createdRoomId={}", ownerParticipantId, createdRoomId);
        // Get owners client id and former chat room (this must exist)
        ClientId ownerClientId = chatRoomState.getClientIdOf(ownerParticipantId);
        RoomId formerRoomId = chatRoomState.getCurrentRoomIdOf(ownerParticipantId);
        waitingList.removeWaitingForRoomEvent(createdRoomId);

        // Update chat room maps.
        chatRoomState.createRoom(ownerParticipantId, createdRoomId);
        // Send APPROVED message to client.
        String message1 = createRoomCreateAcceptedMsg(createdRoomId);
        sendToClient(ownerClientId, message1);
        // Send room change to all in former room and client.
        String message2 = createRoomChangeBroadcastMsg(ownerParticipantId, formerRoomId, createdRoomId);
        sendToClient(ownerClientId, message2);
        sendToRoom(formerRoomId, message2);
    }

    @Synchronized
    @Override
    public void participantIdDeleted(ParticipantId deletedParticipantId, @Nullable RoomId deletedRoomId) {
        log.traceEntry("deletedId={} deletedRoomId={}", deletedParticipantId, Optional.ofNullable(deletedRoomId));
        if (deletedRoomId == null) {
            // The participant is not the owner of any room.
            // We only have to delete the participant.
            chatRoomState.deleteParticipant(deletedParticipantId);
            return;
        }
        // Update chat room maps.
        Collection<ParticipantId> prevParticipantIds = chatRoomState.deleteRoom(deletedRoomId);
        chatRoomState.deleteParticipant(deletedParticipantId);
        // Send room change to all old users. (do not send for the deleted user)
        for (ParticipantId prevParticipantId : prevParticipantIds) {
            if (!deletedParticipantId.equals(prevParticipantId)) {
                String message = createRoomChangeBroadcastMsg(prevParticipantId, deletedRoomId, mainRoomId);
                sendToRoom(mainRoomId, message);
            }
        }
    }

    @Synchronized
    @Override
    public void roomIdDeleted(RoomId deletedRoomId, ParticipantId ownerId) {
        log.traceEntry("deletedRoomId={}", deletedRoomId);
        ClientId ownerClientId = chatRoomState.getClientIdOf(ownerId);
        waitingList.removeWaitingForRoomEvent(deletedRoomId);

        // Update chat room maps.
        Collection<ParticipantId> prevParticipantIds = chatRoomState.deleteRoom(deletedRoomId);
        // Send APPROVED message.
        String message1 = createRoomDeleteAcceptedMsg(deletedRoomId);
        sendToClient(ownerClientId, message1);
        // Send room change to all old users.
        for (ParticipantId prevParticipantId : prevParticipantIds) {
            String message2 = createRoomChangeBroadcastMsg(prevParticipantId, deletedRoomId, mainRoomId);
            sendToRoom(mainRoomId, message2);
        }
    }

    @Synchronized
    @Override
    public void participantMoved(ParticipantId movedParticipant) {
        log.traceEntry("participantMoved={}", movedParticipant);
        chatRoomState.deleteParticipant(movedParticipant);
    }

    @Synchronized
    @Override
    public void participantJoined(ParticipantId joinedParticipant, ServerId serverId) {
        log.traceEntry("participantJoined={}", joinedParticipant);
        // If not stored, assume moving from main room
        ChatRoomWaitingList.ServerChangeRecord record = waitingList
                .removeWaitingForServerChangeEvent(joinedParticipant)
                .orElseGet(() -> new ChatRoomWaitingList.ServerChangeRecord(ClientId.fake(), mainRoomId, mainRoomId));
        ClientId clientId = record.getClientId();
        RoomId formerRoomId = record.getFormerRoomId();
        RoomId newRoomId = record.getNewRoomId();

        chatRoomState.createParticipant(joinedParticipant, clientId, newRoomId);

        // Send messages to client and new group.
        String broadcastMsg = createRoomChangeBroadcastMsg(joinedParticipant, formerRoomId, newRoomId);
        String message = createMoveJoinAcceptedClientMsg(serverId);
        sendToClient(clientId, message);
        sendToRoom(newRoomId, broadcastMsg);
    }

    /*
    ========================================================
    Response message creators
    ========================================================
    */

    private String createRoomCreateAcceptedMsg(RoomId createdRoomId) {
        CreateRoomClientResponse response = CreateRoomClientResponse.builder()
                .roomId(createdRoomId).approved(true).build();
        return serializer.toJson(response);
    }

    private String createRoomDeleteAcceptedMsg(RoomId deletedRoomId) {
        DeleteRoomClientResponse response = DeleteRoomClientResponse.builder()
                .roomId(deletedRoomId).approved(true).build();
        return serializer.toJson(response);
    }

    private String createParticipantCreateAcceptedMsg() {
        NewIdentityClientResponse response = NewIdentityClientResponse.builder()
                .approved(true).build();
        return serializer.toJson(response);
    }

    private String createRoomChangeBroadcastMsg(ParticipantId participantId, RoomId fromRoomId, RoomId toRoomId) {
        RoomChangeBroadcastResponse response = RoomChangeBroadcastResponse.builder()
                .participantId(participantId)
                .formerRoomId(fromRoomId)
                .currentRoomId(toRoomId).build();
        return serializer.toJson(response);
    }

    private String createMoveJoinAcceptedClientMsg(ServerId serverId) {
        MoveJoinClientResponse response = MoveJoinClientResponse.builder()
                .approved(true).serverId(serverId.getValue()).build();
        return serializer.toJson(response);
    }
}
