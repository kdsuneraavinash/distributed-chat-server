package lk.ac.mrt.cse.cs4262.components.client.chat.events;

import com.google.gson.Gson;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftStateReadView;
import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.components.client.chat.ChatRoomState;
import lk.ac.mrt.cse.cs4262.components.client.chat.ChatRoomWaitingList;
import lk.ac.mrt.cse.cs4262.components.client.chat.MessageSender;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.CreateRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.DeleteRoomClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.NewIdentityClientResponse;
import lk.ac.mrt.cse.cs4262.components.client.messages.responses.RoomChangeBroadcastResponse;
import lombok.Builder;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;

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
        // Get client from waiting list.
        waitingList.getWaitingForParticipantCreation(createdParticipantId).ifPresent(clientId -> {
            log.traceEntry("createdParticipantId={}", createdParticipantId);
            // Update chat room maps.
            chatRoomState.participantCreate(clientId, createdParticipantId);
            // Send APPROVED message to client.
            String message1 = createParticipantCreateAcceptedMsg();
            sendToClient(clientId, message1);
            // Send room change to all in main room.
            String message2 = createRoomChangeBroadcastMsg(createdParticipantId, RoomId.NULL, mainRoomId);
            sendToRoom(mainRoomId, message2);
        });
    }

    @Synchronized
    @Override
    public void roomIdCreated(ParticipantId ownerParticipantId, RoomId createdRoomId) {
        waitingList.getWaitingForRoomCreation(createdRoomId).ifPresent(ownerClientId ->
                chatRoomState.getCurrentRoomIdOf(ownerParticipantId).ifPresent(formerRoomId -> {
                    log.traceEntry("ownerId={} createdRoomId={}", ownerParticipantId, createdRoomId);
                    // Update chat room maps.
                    chatRoomState.roomCreate(ownerClientId, createdRoomId);
                    // Send APPROVED message to client.
                    String message1 = createRoomCreateAcceptedMsg(createdRoomId);
                    sendToClient(ownerClientId, message1);
                    // Send room change to all in former room and client.
                    String message2 = createRoomChangeBroadcastMsg(ownerParticipantId, formerRoomId, createdRoomId);
                    sendToClient(ownerClientId, message2);
                    sendToRoom(formerRoomId, message2);
                }));
    }

    @Synchronized
    @Override
    public void participantIdDeleted(ParticipantId deletedParticipantId, @Nullable RoomId deletedRoomId) {
        if (deletedRoomId == null) {
            // Nothing more to do
            return;
        }
        log.traceEntry("deletedId={} deletedRoomId={}", deletedParticipantId, deletedRoomId);
        // Update chat room maps.
        Collection<ClientId> prevClientIds = chatRoomState.roomDelete(deletedRoomId);
        // Send room change to all old users.
        for (ClientId prevClientId : prevClientIds) {
            chatRoomState.getParticipantIdOf(prevClientId).ifPresent((prevParticipantId) -> {
                String message = createRoomChangeBroadcastMsg(prevParticipantId, deletedRoomId, mainRoomId);
                sendToRoom(mainRoomId, message);
            });
        }
    }

    @Synchronized
    @Override
    public void roomIdDeleted(RoomId deletedRoomId) {
        log.traceEntry("deletedRoomId={}", deletedRoomId);
        // Remove owner from waiting list
        waitingList.getWaitingForDeletion(deletedRoomId).ifPresent(ownerClientId -> {
            // Update chat room maps.
            Collection<ClientId> prevClientIds = chatRoomState.roomDelete(deletedRoomId);
            // Send APPROVED message.
            String message1 = createRoomDeleteAcceptedMsg(deletedRoomId);
            sendToClient(ownerClientId, message1);
            // Send room change to all old users.
            for (ClientId prevClientId : prevClientIds) {
                chatRoomState.getParticipantIdOf(prevClientId).ifPresent((prevParticipantId) -> {
                    String message2 = createRoomChangeBroadcastMsg(prevParticipantId, deletedRoomId, mainRoomId);
                    sendToRoom(mainRoomId, message2);
                });
            }
        });
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
}
