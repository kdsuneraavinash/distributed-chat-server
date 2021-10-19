package lk.ac.mrt.cse.cs4262.components.client.chat;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lombok.Synchronized;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class tracking the state of chat rooms.
 */
@Log4j2
@ToString
public class ChatRoomState {
    private final RoomId mainRoomId;
    private final ParticipantId systemUserId;

    /**
     * Data structure to track client id of participants.
     * All authenticated clients are guaranteed to be in this map.
     */
    private final BiMap<ClientId, ParticipantId> clientParticipantMap;

    /**
     * Data structure to track the current room of a given participant.
     */
    private final Multimap<RoomId, ParticipantId> roomParticipantMap;
    private final Map<ParticipantId, RoomId> participantRoomMap;

    /**
     * See {@link ChatRoomState}.
     *
     * @param mainRoomId   ID of the main room in the server.
     * @param systemUserId ID of the system user.
     */
    public ChatRoomState(RoomId mainRoomId, ParticipantId systemUserId) {
        this.mainRoomId = mainRoomId;
        this.systemUserId = systemUserId;
        this.clientParticipantMap = HashBiMap.create();
        this.roomParticipantMap = HashMultimap.create();
        this.participantRoomMap = new HashMap<>();
        // Add system user and rooms
        this.clientParticipantMap.put(ClientId.real(), systemUserId);
        this.roomParticipantMap.put(mainRoomId, systemUserId);
        this.participantRoomMap.put(systemUserId, mainRoomId);
    }

    /*
    ========================================================
    State Read Methods
    ========================================================
     */

    /**
     * @param clientId Client ID.
     * @return Whether client has not participant mapped.
     */
    public boolean cannotFindParticipant(ClientId clientId) {
        return !clientParticipantMap.containsKey(clientId);
    }

    /**
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
     * @param participantId ID of the participant.
     * @return ID of the corresponding client if any.
     */
    public ClientId getClientIdOf(ParticipantId participantId) {
        ClientId clientId = clientParticipantMap.inverse().get(participantId);
        if (clientId != null) {
            return clientId;
        }
        throw new IllegalStateException("client does not exist");
    }

    /**
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
     * @param roomId ID of the room.
     * @return Participant IDs of participants in the room.
     */
    public Collection<ParticipantId> getParticipantIdsOf(RoomId roomId) {
        return roomParticipantMap.get(roomId).stream()
                .filter(participantId -> !systemUserId.equals(participantId))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * @param roomId ID of the room.
     * @return Client IDs of participants.
     */
    public Collection<ClientId> getClientIdsOf(RoomId roomId) {
        return getParticipantIdsOf(roomId).stream()
                .map(this::getClientIdOf)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * @return All current participant ids.
     */
    public Collection<ParticipantId> getAllActiveParticipantIds() {
        return clientParticipantMap.entrySet().stream()
                .filter(entry -> !entry.getKey().isFakeClient())
                .map(Map.Entry::getValue)
                .collect(Collectors.toUnmodifiableList());
    }

    /*
    ========================================================
    State Write Methods
    ========================================================
     */

    /**
     * @param participantId New participant ID.
     * @param clientId      ID of the client.
     * @param roomId        Room to add the participant.
     */
    @Synchronized
    public void createParticipant(ParticipantId participantId, ClientId clientId, RoomId roomId) {
        Preconditions.checkArgument(!hasParticipant(participantId), "participant already present");
        Preconditions.checkArgument(cannotFindParticipant(clientId), "client already present");
        Preconditions.checkArgument(hasRoom(roomId), "room does not exist");
        clientParticipantMap.put(clientId, participantId);
        roomParticipantMap.put(roomId, participantId);
        participantRoomMap.put(participantId, roomId);
        log.debug("createParticipant={}", this);
    }

    /**
     * @param participantId ID of the participant.
     *                      The participant is assumed not to own a room.
     */
    @Synchronized
    public void deleteParticipant(ParticipantId participantId) {
        Preconditions.checkArgument(hasParticipant(participantId), "participant does not exist");
        clientParticipantMap.values().remove(participantId);
        roomParticipantMap.values().remove(participantId);
        participantRoomMap.remove(participantId);
        log.debug("deleteParticipant={}", this);
    }

    /**
     * @param ownerId Owner ID.
     * @param roomId  Room to create.
     */
    @Synchronized
    public void createRoom(ParticipantId ownerId, RoomId roomId) {
        Preconditions.checkArgument(hasParticipant(ownerId), "participant does not exist");
        Preconditions.checkArgument(!hasRoom(roomId), "room already present");
        RoomId currentRoomId = getCurrentRoomIdOf(ownerId);
        roomParticipantMap.remove(currentRoomId, ownerId);
        roomParticipantMap.put(roomId, ownerId);
        participantRoomMap.put(ownerId, roomId);
        log.debug("createRoom={}", this);
    }

    /**
     * @param roomId Room to delete.
     * @return Deleted participants.
     */
    @Synchronized
    public Collection<ParticipantId> deleteRoom(RoomId roomId) {
        Preconditions.checkArgument(hasRoom(roomId), "room does not exist");
        Collection<ParticipantId> participantIds = roomParticipantMap.removeAll(roomId);
        participantRoomMap.values().remove(roomId);
        roomParticipantMap.putAll(mainRoomId, participantIds);
        participantIds.forEach(participantId -> participantRoomMap.put(participantId, mainRoomId));
        log.debug("deleteRoom={}", this);
        return participantIds;
    }

    /*
    ========================================================
    Private Helpers
    ========================================================
     */

    /**
     * @param participantId Participant ID.
     * @return Whether participant exists.
     */
    private boolean hasParticipant(ParticipantId participantId) {
        return clientParticipantMap.containsValue(participantId);
    }

    /**
     * @param roomId room ID.
     * @return Whether participant exists.
     */
    private boolean hasRoom(RoomId roomId) {
        return roomParticipantMap.containsKey(roomId);
    }
}
