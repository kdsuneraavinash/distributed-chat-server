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
     * Helper method to add to a waiting list.
     * Only the first for a given value is added.
     *
     * @param waitingList Waiting List to add.
     * @param clientId    Client ID.
     * @param value       Value to add.
     * @param <T>         Type of value (waiting list).
     * @return Whether the client was added.
     */
    private <T> boolean addToMapIfNotExists(Map<T, ClientId> waitingList, ClientId clientId, T value) {
        if (value == null || waitingList.containsKey(value)) {
            return false;
        }
        waitingList.put(value, clientId);
        return true;
    }

    /**
     * Add to a waiting list to wait until a participant id is created.
     *
     * @param clientId      Client ID.
     * @param participantId Participant ID.
     * @return Whether the client was added.
     */
    @Synchronized
    public boolean waitForParticipantCreation(ClientId clientId, ParticipantId participantId) {
        return addToMapIfNotExists(waitingForParticipantIdCreation, clientId, participantId);
    }

    /**
     * Add to a waiting list to wait until a room id is created.
     *
     * @param clientId Client ID.
     * @param roomId   Room ID.
     * @return Whether the client was added.
     */
    @Synchronized
    public boolean waitForRoomCreation(ClientId clientId, RoomId roomId) {
        return addToMapIfNotExists(waitingForRoomIdCreation, clientId, roomId);
    }

    /**
     * Add to a waiting list to wait until a room id is deleted.
     *
     * @param clientId Client ID.
     * @param roomId   Room ID.
     * @return Whether the client was added.
     */
    @Synchronized
    public boolean waitForRoomDeletion(ClientId clientId, RoomId roomId) {
        return addToMapIfNotExists(waitingForRoomIdDeletion, clientId, roomId);
    }

    /**
     * Add to a watching list to wait until the destination server validation occurs.
     *
     * @param participantId - Participant ID
     * @param roomId - Room ID
     * @return Whether the entry is correctly added or not.
     */
    @Synchronized
    public boolean waitForServerChange(ParticipantId participantId, RoomId roomId) {
        if (participantId == null || roomId == null) {
            return false;
        }
        // If previous value exist, provided value will overwrite previous value.
        waitingForServerChange.put(participantId, roomId);
        return true;
    }

    /**
     * Get the client that is waiting for this participant id creation.
     *
     * @param participantId Participant ID.
     * @return ID of waiting Client (if any)
     */
    @Synchronized
    public Optional<ClientId> getWaitingForParticipantCreation(ParticipantId participantId) {
        return Optional.ofNullable(waitingForParticipantIdCreation.remove(participantId));
    }

    /**
     * Get the client that is waiting for this room id creation.
     *
     * @param roomId Room ID.
     * @return ID of waiting Client (if any)
     */
    @Synchronized
    public Optional<ClientId> getWaitingForRoomCreation(RoomId roomId) {
        return Optional.ofNullable(waitingForRoomIdCreation.remove(roomId));
    }

    /**
     * Get the client that is waiting for this room id deletion.
     *
     * @param roomId Room ID.
     * @return ID of waiting Client (if any)
     */
    @Synchronized
    public Optional<ClientId> getWaitingForDeletion(RoomId roomId) {
        return Optional.ofNullable(waitingForRoomIdDeletion.remove(roomId));
    }

    /**
     * Get the room id for a specific participant id in a server change process.
     * Used by the destination server to validate the server change source server.
     *
     * @param participantId Participant ID.
     * @param remove Remove the entry or not. If true remove.
     * @return Related room id involved in the server change if any.
     */
    @Synchronized
    public Optional<RoomId> getWaitingForServerChange(ParticipantId participantId, boolean remove) {
        if (remove) {
            return Optional.ofNullable(waitingForServerChange.remove(participantId));
        }
        return Optional.ofNullable(waitingForServerChange.get(participantId));
    }

    /**
     * Helper method to check for participants waiting for server change.
     * @param participantId - Participant ID
     * @return waiting for server change or not
     */
    @Synchronized
    public boolean isWaitingForServerChange(ParticipantId participantId) {
        return waitingForServerChange.containsKey(participantId);
    }

    /**
     * Adding former room involved in Server change.
     * @param participantId - Participant ID
     * @param roomId - Room ID
     */
    public void addServerChangeFormerRoom(ParticipantId participantId, RoomId roomId) {
        serverChangeFormerRoom.put(participantId, roomId);
    }

    /**
     * Getter for former room involved in server change.
     * @param participantId - Participant ID
     * @return - Room ID
     */
    @Synchronized
    public Optional<RoomId> getServerChangeFormerRoom(ParticipantId participantId) {
        return Optional.ofNullable(serverChangeFormerRoom.remove(participantId));
    }
}
