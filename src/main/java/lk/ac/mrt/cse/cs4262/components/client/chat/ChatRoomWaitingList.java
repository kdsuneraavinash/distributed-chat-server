package lk.ac.mrt.cse.cs4262.components.client.chat;

import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lombok.Synchronized;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Class tracking the state of chat rooms.
 */
@Log4j2
@ToString
public class ChatRoomWaitingList {
    /**
     * Clients that are waiting for a participant id to be accepted.
     */
    private final Map<ParticipantId, ClientId> waitingForParticipantIdCreation;

    /**
     * Clients that are waiting for a new room id to be accepted.
     */
    private final Map<RoomId, ClientId> waitingForRoomIdCreation;

    /**
     * Clients that are waiting for a new room id deletion to be accepted.
     */
    private final Map<RoomId, ClientId> waitingForRoomIdDeletion;

    /**
     * Clients that are waiting for a server change to connect to a chat room in another server.
     */
    private final Map<ParticipantId, RoomId> waitingForServerChange;

    /**
     * Additional Map to track the former Room IDs of participants involved in a server change.
     * Required because raftstate does not track any detail about rooms.
     */
    private final Map<ParticipantId, RoomId> serverChangeFormerRoom;

    /**
     * See {@link ChatRoomWaitingList}.
     */
    public ChatRoomWaitingList() {
        this.waitingForParticipantIdCreation = new HashMap<>();
        this.waitingForRoomIdCreation = new HashMap<>();
        this.waitingForRoomIdDeletion = new HashMap<>();
        this.waitingForServerChange = new HashMap<>();
        this.serverChangeFormerRoom = new HashMap<>();
    }

    /**
     * Add to a waiting list to wait until a participant id is created.
     *
     * @param participantId Participant ID.
     * @param clientId      Client ID.
     */
    @Synchronized
    public void waitForParticipantCreation(ParticipantId participantId, ClientId clientId) {
        waitingForParticipantIdCreation.put(participantId, clientId);
    }

    /**
     * Add to a waiting list to wait until a room id is created.
     *
     * @param roomId   Room ID.
     * @param clientId Client ID.
     */
    @Synchronized
    public void waitForRoomCreation(RoomId roomId, ClientId clientId) {
        waitingForRoomIdCreation.put(roomId, clientId);
    }

    /**
     * Add to a waiting list to wait until a room id is deleted.
     *
     * @param roomId   Room ID.
     * @param clientId Client ID.
     */
    @Synchronized
    public void waitForRoomDeletion(RoomId roomId, ClientId clientId) {
        waitingForRoomIdDeletion.put(roomId, clientId);
    }

    /**
     * Add to a watching list to wait until the destination server validation occurs.
     * If previous value exist, provided value will overwrite previous value.
     *
     * @param participantId - Participant ID
     * @param roomId        - Room ID
     */
    @Synchronized
    public void waitForServerChange(ParticipantId participantId, RoomId roomId) {
        waitingForServerChange.put(participantId, roomId);
    }

    /**
     * Get the client that is waiting for this participant id creation.
     *
     * @param participantId Participant ID.
     * @return ID of waiting Client (if any)
     */
    @Synchronized
    public Optional<ClientId> getWaitingForCreation(ParticipantId participantId) {
        return Optional.ofNullable(waitingForParticipantIdCreation.get(participantId));
    }

    /**
     * Get the client that is waiting for this room id creation.
     *
     * @param roomId Room ID.
     * @return ID of waiting Client (if any)
     */
    @Synchronized
    public Optional<ClientId> getWaitingForCreation(RoomId roomId) {
        return Optional.ofNullable(waitingForRoomIdCreation.get(roomId));
    }

    /**
     * Get the client that is waiting for this room id deletion.
     *
     * @param roomId Room ID.
     * @return ID of waiting Client (if any)
     */
    @Synchronized
    public Optional<ClientId> getWaitingForDeletion(RoomId roomId) {
        return Optional.ofNullable(waitingForRoomIdDeletion.get(roomId));
    }

    /**
     * Get the room id for a specific participant id in a server change process.
     * Used by the destination server to validate the server change source server.
     *
     * @param participantId Participant ID.
     * @return Related room id involved in the server change if any.
     */
    @Synchronized
    public Optional<RoomId> getWaitingForServerChange(ParticipantId participantId) {
        return Optional.ofNullable(waitingForServerChange.get(participantId));
    }

    /**
     * Get the client that is waiting for this participant id creation.
     * Removes the entity.
     *
     * @param participantId Participant ID.
     * @return ID of waiting Client (if any)
     */
    @Synchronized
    public Optional<ClientId> removeWaitingForCreation(ParticipantId participantId) {
        return Optional.ofNullable(waitingForParticipantIdCreation.remove(participantId));
    }

    /**
     * Get the client that is waiting for this room id creation.
     * Removes the entity.
     *
     * @param roomId Room ID.
     * @return ID of waiting Client (if any)
     */
    @Synchronized
    public Optional<ClientId> removeWaitingForCreation(RoomId roomId) {
        return Optional.ofNullable(waitingForRoomIdCreation.remove(roomId));
    }

    /**
     * Get the client that is waiting for this room id deletion.
     * Removes the entity.
     *
     * @param roomId Room ID.
     * @return ID of waiting Client (if any)
     */
    @Synchronized
    public Optional<ClientId> removeWaitingForDeletion(RoomId roomId) {
        return Optional.ofNullable(waitingForRoomIdDeletion.remove(roomId));
    }

    /**
     * Get the room id for a specific participant id in a server change process.
     * Used by the destination server to validate the server change source server.
     * Removes the entity.
     *
     * @param participantId Participant ID.
     * @return Related room id involved in the server change if any.
     */
    @Synchronized
    public Optional<RoomId> removeWaitingForServerChange(ParticipantId participantId) {
        return Optional.ofNullable(waitingForServerChange.remove(participantId));
    }

    /**
     * Adding former room involved in Server change.
     *
     * @param participantId - Participant ID
     * @param roomId        - Room ID
     */
    public void addServerChangeFormerRoom(ParticipantId participantId, RoomId roomId) {
        serverChangeFormerRoom.put(participantId, roomId);
    }

    /**
     * Getter for former room involved in server change.
     *
     * @param participantId - Participant ID
     * @return - Room ID
     */
    @Synchronized
    public Optional<RoomId> getServerChangeFormerRoom(ParticipantId participantId) {
        return Optional.ofNullable(serverChangeFormerRoom.remove(participantId));
    }
}
