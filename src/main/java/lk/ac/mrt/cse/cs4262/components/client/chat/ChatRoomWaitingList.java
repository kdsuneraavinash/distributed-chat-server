package lk.ac.mrt.cse.cs4262.components.client.chat;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lombok.Data;
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
    private final BiMap<ParticipantId, ClientId> waitingForParticipantEvent;

    /**
     * Clients that are waiting for a new room id to be accepted/deleted.
     */
    private final BiMap<RoomId, ClientId> waitingForRoomEvent;

    /**
     * Clients that are waiting for a server change to connect to a chat room in another server.
     */
    private final Map<ParticipantId, ServerChangeRecord> waitingForServerChangeEvent;

    /**
     * See {@link ChatRoomWaitingList}.
     */
    public ChatRoomWaitingList() {
        this.waitingForParticipantEvent = HashBiMap.create();
        this.waitingForRoomEvent = HashBiMap.create();
        this.waitingForServerChangeEvent = new HashMap<>();
    }

    /**
     * Add to a waiting list to wait until a participant id is created.
     *
     * @param participantId Participant ID.
     * @param clientId      Client ID.
     */
    @Synchronized
    public void waitForParticipantEvent(ParticipantId participantId, ClientId clientId) {
        waitingForParticipantEvent.put(participantId, clientId);
    }

    /**
     * Add to a waiting list to wait until a room id is created.
     *
     * @param roomId   Room ID.
     * @param clientId Client ID.
     */
    @Synchronized
    public void waitForRoomEvent(RoomId roomId, ClientId clientId) {
        waitingForRoomEvent.put(roomId, clientId);
    }

    /**
     * Add to a watching list to wait until the destination server validation occurs.
     * If previous value exist, provided value will overwrite previous value.
     *
     * @param participantId Participant ID
     * @param clientId      Client ID.
     * @param formerRoomId  Former Room ID.
     * @param newRoomId     New Room ID.
     */
    @Synchronized
    public void waitForServerChangeEvent(ParticipantId participantId,
                                         ClientId clientId, RoomId formerRoomId, RoomId newRoomId) {
        waitingForServerChangeEvent.put(participantId, new ServerChangeRecord(clientId, formerRoomId, newRoomId));
    }

    /**
     * Get if there are no clients that is waiting for this participant id creation.
     *
     * @param participantId Participant ID.
     * @return Whether there are no clients waiting.
     */
    @Synchronized
    public boolean noOneWaitingForParticipantEvent(ParticipantId participantId) {
        return !waitingForParticipantEvent.containsKey(participantId);
    }

    /**
     * Get if there are no clients that is waiting for this room id creation/deletion.
     *
     * @param roomId Room ID.
     * @return Whether there are no clients waiting.
     */
    @Synchronized
    public boolean noOneWaitingForRoomEvent(RoomId roomId) {
        return !waitingForRoomEvent.containsKey(roomId);
    }

    /**
     * Get if there are no data for a specific participant id in a server change process.
     * Used by the destination server to validate the server change source server.
     *
     * @param participantId Participant ID.
     * @return Whether there are no clients waiting.
     */
    @Synchronized
    public boolean noOneWaitingForServerChangeEvent(ParticipantId participantId) {
        return !waitingForServerChangeEvent.containsKey(participantId);
    }

    /**
     * Get the client that is waiting for this participant id creation.
     * If there is no client, this will create a fake client.
     * Removes the entity.
     *
     * @param participantId Participant ID.
     * @return ID of waiting Client (if any)
     */
    @Synchronized
    public Optional<ClientId> removeWaitingForParticipantEvent(ParticipantId participantId) {
        return Optional.ofNullable(waitingForParticipantEvent.remove(participantId));
    }

    /**
     * Remove the client that is waiting for this room id creation.
     * Ignore if there isn't a client waiting.
     *
     * @param roomId Room ID.
     */
    @Synchronized
    public void removeWaitingForRoomEvent(RoomId roomId) {
        waitingForRoomEvent.remove(roomId);
    }

    /**
     * Get the data for a specific participant id in a server change process.
     * Used by the destination server to validate the server change source server.
     * Removes the entity.
     *
     * @param participantId Participant ID.
     * @return Related data involved in the server change if any.
     */
    @Synchronized
    public Optional<ServerChangeRecord> removeWaitingForServerChangeEvent(ParticipantId participantId) {
        return Optional.ofNullable(waitingForServerChangeEvent.remove(participantId));
    }

    /**
     * Removes a client from all the waiting lists in the system.
     * Called after a client is disconnected.
     *
     * @param clientId ID of client.
     */
    @Synchronized
    public void removeClientFromAllWaitingLists(ClientId clientId) {
        waitingForParticipantEvent.values().remove(clientId);
        waitingForRoomEvent.values().remove(clientId);
        waitingForServerChangeEvent.values().removeIf(record -> clientId.equals(record.getClientId()));
    }

    /**
     * Record of a server change with former, newer room id and client id.
     */
    @Data
    public static final class ServerChangeRecord {
        private final ClientId clientId;
        private final RoomId formerRoomId;
        private final RoomId newRoomId;
    }
}
