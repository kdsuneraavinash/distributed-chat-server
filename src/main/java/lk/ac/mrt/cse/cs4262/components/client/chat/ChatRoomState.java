package lk.ac.mrt.cse.cs4262.components.client.chat;

import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lombok.Synchronized;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class tracking the state of chat rooms.
 */
@Log4j2
@ToString
public class ChatRoomState {
    /**
     * ID of the main room.
     */
    private final RoomId mainRoomId;

    /**
     * Data structure to track participant id of clients.
     */
    private final Map<ClientId, ParticipantId> clientParticipantMap;

    /**
     * Data structure to track client id of participants.
     */
    private final Map<ParticipantId, ClientId> participantClientMap;

    /**
     * Data structure to track all the participants in a given room.
     * The clients in the map should have a participant id.
     */
    private final Map<RoomId, List<ClientId>> roomClientListMap;

    /**
     * Data structure to track the current room of a given participant.
     */
    private final Map<ParticipantId, RoomId> participantRoomMap;

    /**
     * See {@link ChatRoomState}.
     *
     * @param mainRoomId ID of the main room in the server.
     */
    public ChatRoomState(RoomId mainRoomId) {
        this.mainRoomId = mainRoomId;

        this.clientParticipantMap = new HashMap<>();
        this.participantClientMap = new HashMap<>();
        this.roomClientListMap = new HashMap<>();
        this.roomClientListMap.put(this.mainRoomId, new ArrayList<>());
        this.participantRoomMap = new HashMap<>();
    }

    /*
    ========================================================
    State Write Methods
    ========================================================
     */

    /**
     * Participant ID created.
     *
     * @param clientId      ID of the client.
     * @param participantId New participant ID.
     */
    @Synchronized
    public void participantCreate(ClientId clientId, ParticipantId participantId) {
        log.traceEntry("clientId={} participantId={}", clientId, participantId);
        if (!roomClientListMap.containsKey(mainRoomId)) {
            throw new IllegalStateException("main room does not exist");
        }
        clientParticipantMap.put(clientId, participantId);
        participantClientMap.put(participantId, clientId);
        roomClientListMap.get(mainRoomId).add(clientId);
        participantRoomMap.put(participantId, mainRoomId);
        log.traceExit("state modified: {}", this);
    }

    /**
     * Delete participant from the server.
     *
     * @param participantId ID of the participant.
     */
    @Synchronized
    public void participantDelete(ParticipantId participantId) {
        log.traceEntry("participantId={}", participantId);
        ClientId clientId = participantClientMap.remove(participantId);
        if (clientId == null) {
            throw new IllegalStateException("participant unknown");
        }
        RoomId formerRoomId = getCurrentRoomIdOf(participantId);
        clientParticipantMap.remove(clientId);
        participantRoomMap.remove(participantId);
        if (roomClientListMap.containsKey(formerRoomId)) {
            roomClientListMap.get(formerRoomId).remove(clientId);
        }
        log.traceExit("state modified: {}", this);
    }

    /**
     * Participants joins a room that is also in the same server.
     * The room join must happen inside the same server.
     *
     * @param clientId ID of the client.
     * @param roomId   Moving room ID.
     */
    @Synchronized
    public void roomJoinInternal(ClientId clientId, RoomId roomId) {
        log.traceEntry("clientId={} roomId={}", clientId, roomId);
        if (!roomClientListMap.containsKey(roomId)) {
            throw new IllegalArgumentException("next room does not exist");
        }
        ParticipantId participantId = getParticipantIdOf(clientId);
        RoomId formerRoomId = getCurrentRoomIdOf(participantId);
        participantRoomMap.put(participantId, roomId);
        // Ignore if former room does not exist
        if (roomClientListMap.containsKey(formerRoomId)) {
            roomClientListMap.get(formerRoomId).remove(clientId);
        }
        roomClientListMap.get(roomId).add(clientId);
        log.traceExit("state modified: {}", this);
    }

    /**
     * Participants join from an external server.
     *
     * @param clientId      Client ID
     * @param participantId Participant ID
     * @param roomId        Room ID
     */
    @Synchronized
    public void roomJoinExternal(ClientId clientId, ParticipantId participantId, RoomId roomId) {
        log.traceEntry("clientId={} participantId={} roomId={}", clientId, participantId, roomId);
        if (!roomClientListMap.containsKey(roomId)) {
            throw new IllegalArgumentException("next room does not exist");
        }
        clientParticipantMap.put(clientId, participantId);
        participantClientMap.put(participantId, clientId);
        roomClientListMap.get(roomId).add(clientId);
        participantRoomMap.put(participantId, roomId);
    }

    /**
     * Delete participants who have moved to another server. Participants
     * get deleted after moving the destination server successfully.
     *
     * @param clientId Client ID
     * @param roomId   Room ID
     */
    @Synchronized
    public void deleteMovedParticipant(ClientId clientId, RoomId roomId) {
        log.traceEntry("clientId={} roomId={}", clientId, roomId);
        if (!roomClientListMap.containsKey(roomId)) {
            throw new IllegalArgumentException("previous room does not exist");
        }
        ParticipantId participantId = getParticipantIdOf(clientId);
        participantRoomMap.remove(participantId);
        roomClientListMap.get(roomId).remove(clientId);
    }

    /**
     * Create a room.
     *
     * @param clientId ID of the client.
     * @param roomId   Newly created room.
     */
    @Synchronized
    public void roomCreate(ClientId clientId, RoomId roomId) {
        log.traceEntry("clientId={} roomId={}", clientId, roomId);
        ParticipantId participantId = getParticipantIdOf(clientId);
        RoomId formerRoomId = getCurrentRoomIdOf(participantId);
        List<ClientId> newRoomClients = new ArrayList<>();
        newRoomClients.add(clientId);
        // Ignore if former room does not exist
        if (roomClientListMap.containsKey(formerRoomId)) {
            roomClientListMap.get(formerRoomId).remove(clientId);
        }
        roomClientListMap.put(roomId, newRoomClients);
        participantRoomMap.put(participantId, roomId);
        log.traceExit("state modified: {}", this);
    }

    /**
     * Delete a room from server.
     * Participants are moved to main room.
     *
     * @param deletedRoomId Deleted room id.
     * @return Clients of the deleted chat room.
     */
    @Synchronized
    public Collection<ClientId> roomDelete(RoomId deletedRoomId) {
        log.traceEntry("deletedRoomId={}", deletedRoomId);
        if (!roomClientListMap.containsKey(mainRoomId)) {
            throw new IllegalStateException("main room does not exist");
        }
        if (!roomClientListMap.containsKey(deletedRoomId)) {
            throw new IllegalArgumentException("deleted room does not exist");
        }
        List<ClientId> prevClientIds = roomClientListMap.remove(deletedRoomId);
        // Ignore if prevClientIds is null for some reason
        if (prevClientIds == null) {
            return List.of();
        }
        roomClientListMap.get(mainRoomId).addAll(prevClientIds);
        for (ClientId prevClientId : prevClientIds) {
            ParticipantId prevParticipantId = getParticipantIdOf(prevClientId);
            participantRoomMap.put(prevParticipantId, mainRoomId);
        }
        log.traceExit("state modified: {}", this);
        return prevClientIds;
    }

    /*
    ========================================================
    State Read Methods
    ========================================================
     */

    /**
     * Get the participant id of a client.
     *
     * @param clientId ID of the client.
     * @return ID of the corresponding participant if any.
     */
    public ParticipantId getParticipantIdOf(ClientId clientId) {
        if (clientParticipantMap.containsKey(clientId)) {
            return clientParticipantMap.get(clientId);
        }
        throw new IllegalStateException("client does not exist");
    }

    /**
     * Get the client id of a participant.
     *
     * @param participantId ID of the participant.
     * @return ID of the corresponding client if any.
     */
    public ClientId getClientIdOf(ParticipantId participantId) {
        if (participantClientMap.containsKey(participantId)) {
            return participantClientMap.get(participantId);
        }
        throw new IllegalStateException("participant does not exist");
    }

    /**
     * Get the clients of a room.
     * If the room does not exist, an empty collection will be returned.
     *
     * @param roomId ID of the room.
     * @return Client IDs of participants.
     */
    public Collection<ClientId> getClientIdsOf(RoomId roomId) {
        if (roomClientListMap.containsKey(roomId)) {
            return roomClientListMap.get(roomId);
        }
        return List.of();
    }

    /**
     * Get the current room id of a participant.
     *
     * @param participantId ID of the participant.
     * @return Current room id of the participant.
     */
    public RoomId getCurrentRoomIdOf(ParticipantId participantId) {
        if (participantRoomMap.containsKey(participantId)) {
            return participantRoomMap.get(participantId);
        }
        throw new IllegalStateException("participant does not exist");
    }

    /**
     * Get all participant Ids.
     *
     * @return All current participant ids.
     */
    public Collection<ParticipantId> getAllActiveParticipantIds() {
        ArrayList<ParticipantId> participantIds = new ArrayList<>();
        clientParticipantMap.forEach((clientId, participantId) -> {
            if (!clientId.isFakeClient()) {
                participantIds.add(participantId);
            }
        });
        return participantIds;
    }
}
