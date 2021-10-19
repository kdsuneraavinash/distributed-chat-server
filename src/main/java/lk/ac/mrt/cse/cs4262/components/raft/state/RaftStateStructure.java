package lk.ac.mrt.cse.cs4262.components.raft.state;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.Synchronized;
import lombok.ToString;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The data structure managed by Raft.
 * Reflects system state.
 */
@ToString
public class RaftStateStructure {
    /**
     * A reserved participant name prefix for SYSTEM user.
     * This is the owner main rooms for each server.
     */
    private static final String SYSTEM_USER_PREFIX = "SYSTEM-";
    /**
     * A reserved room name prefix for Main Rooms.
     * This is the default room for each server.
     */
    private static final String MAIN_ROOM_PREFIX = "MainHall-";

    /**
     * Maps with all participants and their respective servers.
     * All the servers and participants are guaranteed to be in the map.
     */
    private final Multimap<ServerId, ParticipantId> serverParticipantMap;
    private final Map<ParticipantId, ServerId> participantServerMap;

    /**
     * Map with rooms.
     * Only the participants with rooms are tracked.
     * All rooms are guaranteed to be in this map.
     * But participants may or may not be in the map.
     */
    private final BiMap<RoomId, ParticipantId> roomParticipantMap;

    /**
     * See {@link RaftStateStructure}.
     */
    public RaftStateStructure() {
        this.serverParticipantMap = HashMultimap.create();
        this.participantServerMap = new HashMap<>();
        this.roomParticipantMap = HashBiMap.create();
    }

    /**
     * @param serverId ID of the server.
     * @return Whether the server is active in the system.
     */
    public boolean hasServer(ServerId serverId) {
        return serverParticipantMap.containsKey(serverId);
    }

    /**
     * @param participantId ID of the participant.
     * @return Whether the participant is active in the system.
     */
    public boolean hasParticipant(ParticipantId participantId) {
        return participantServerMap.containsKey(participantId);
    }

    /**
     * @param roomId ID of the room.
     * @return Whether the room is active in the system.
     */
    public boolean hasRoom(RoomId roomId) {
        return roomParticipantMap.containsKey(roomId);
    }

    /**
     * @param participantId ID of the participant.
     * @return Room ID if the participant owns a room. Otherwise null.
     */
    public Optional<RoomId> getRoomOwnedByParticipant(ParticipantId participantId) {
        return Optional.ofNullable(roomParticipantMap.inverse().get(participantId));
    }

    /**
     * @param serverId ID of the server.
     * @return A list of all the active participant IDs in the specified server.
     * The system user is filtered out. (The system user is always in the server)
     */
    public Collection<ParticipantId> getParticipantsInServer(ServerId serverId) {
        ParticipantId systemParticipantId = getSystemUserId(serverId);
        if (serverParticipantMap.containsKey(serverId)) {
            return serverParticipantMap.get(serverId).stream()
                    .filter(pid -> !systemParticipantId.equals(pid))
                    .collect(Collectors.toUnmodifiableList());
        }
        throw new IllegalStateException("server does not exist.");
    }

    /**
     * @return A list of all the active room IDs in the system.
     */
    public Collection<RoomId> getRoomsInSystem() {
        return Collections.unmodifiableCollection(roomParticipantMap.keySet());
    }

    /**
     * @param roomId ID of the room.
     * @return Participant ID of the owner of the room.
     */
    public ParticipantId getOwnerOfRoom(RoomId roomId) {
        if (roomParticipantMap.containsKey(roomId)) {
            return roomParticipantMap.get(roomId);
        }
        throw new IllegalStateException("room does not exist.");
    }

    /**
     * @param roomId ID of the room.
     * @return The ID of the server with the room.
     */
    public ServerId getServerOfRoom(RoomId roomId) {
        ParticipantId ownerId = getOwnerOfRoom(roomId);
        return getServerOfParticipant(ownerId);
    }

    /**
     * @param participantId ID of the participant.
     * @return The ID of the server with the participant.
     */
    public ServerId getServerOfParticipant(ParticipantId participantId) {
        if (participantServerMap.containsKey(participantId)) {
            return participantServerMap.get(participantId);
        }
        throw new IllegalStateException("participant does not exist");
    }

    /**
     * @param serverId ID of the server.
     * @return The ID of the system user of specified server.
     */
    public ParticipantId getSystemUserId(ServerId serverId) {
        return new ParticipantId(SYSTEM_USER_PREFIX + serverId.getValue());
    }

    /**
     * @param serverId ID of the server.
     * @return The ID of the main room of specified server.
     */
    public RoomId getMainRoomId(ServerId serverId) {
        return new RoomId(MAIN_ROOM_PREFIX + serverId.getValue());
    }

    /*
    ========================================================
    Unsupported
    ========================================================
     */

    /**
     * @param serverId Server to create.
     */
    @Synchronized
    public void createServer(ServerId serverId) {
        Preconditions.checkArgument(!hasServer(serverId), "server already created");
        ParticipantId systemUserId = getSystemUserId(serverId);
        serverParticipantMap.put(serverId, systemUserId);
        participantServerMap.put(systemUserId, serverId);
    }

    /**
     * @param serverId      Server of participant.
     * @param participantId Participant to create.
     */
    @Synchronized
    public void createParticipant(ServerId serverId, ParticipantId participantId) {
        Preconditions.checkArgument(hasServer(serverId), "server does not exist");
        Preconditions.checkArgument(!hasParticipant(participantId), "participant already created");
        serverParticipantMap.put(serverId, participantId);
        participantServerMap.put(participantId, serverId);
    }

    /**
     * @param ownerId Owner of room.
     * @param roomId  Room to create.
     */
    @Synchronized
    public void createRoom(ParticipantId ownerId, RoomId roomId) {
        Preconditions.checkArgument(hasParticipant(ownerId), "participant does not exist");
        Preconditions.checkArgument(!hasRoom(roomId), "room already created");
        Preconditions.checkArgument(getRoomOwnedByParticipant(ownerId).isEmpty(), "participant already owns room");
        roomParticipantMap.put(roomId, ownerId);
    }

    /**
     * @param participantId Participant ot delete.
     */
    @Synchronized
    public void deleteParticipant(ParticipantId participantId) {
        Preconditions.checkArgument(hasParticipant(participantId), "participant does not exist");
        serverParticipantMap.values().remove(participantId);
        participantServerMap.remove(participantId);
        roomParticipantMap.values().remove(participantId);
    }

    /**
     * @param roomId Room to delete.
     */
    @Synchronized
    public void deleteRoom(RoomId roomId) {
        Preconditions.checkArgument(hasRoom(roomId), "room does not exist");
        roomParticipantMap.remove(roomId);
    }

    /**
     * @param participantId Participant to move.
     * @param toServerId    Server to move.
     */
    @Synchronized
    public void moveParticipant(ParticipantId participantId, ServerId toServerId) {
        Preconditions.checkArgument(hasParticipant(participantId), "participant does not exist");
        Preconditions.checkArgument(hasServer(toServerId), "to server does not exist");
        deleteParticipant(participantId);
        createParticipant(toServerId, participantId);
    }
}
