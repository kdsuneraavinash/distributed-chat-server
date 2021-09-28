package lk.ac.mrt.cse.cs4262.server.state;

import lombok.NonNull;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import lk.ac.mrt.cse.cs4262.server.core.ParticipantId;
import lk.ac.mrt.cse.cs4262.server.core.RoomId;
import lk.ac.mrt.cse.cs4262.server.core.ServerId;
import lk.ac.mrt.cse.cs4262.server.state.logs.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

@ToString
@Log4j2
public class ServerState {
    public static final RoomId MAIN_HALL = new RoomId("MainHall");
    public static final ServerId SERVER = new ServerId("SERVER");
    /**
     * Map for each lk.ac.mrt.cse.cs4262.server containing its participants.
     * If the participant owns a room, it will also be recorded.
     * This is the primary state object.
     * Other objects are simply for performance-sake.
     */
    private final HashMap<@NonNull ServerId, HashMap<@NonNull ParticipantId, RoomId>> state;

    /**
     * Map for each participant to record the lk.ac.mrt.cse.cs4262.server that they are part of.
     * This is a derived value of {@code state}.
     * Used for reverse relations.
     */
    private final HashMap<@NonNull ParticipantId, @NonNull ServerId> participantServerMap;

    /**
     * Map for each room to record their owner.
     * This is a derived value of {@code state}.
     * Used for reverse relations.
     */
    private final HashMap<@NonNull RoomId, @NonNull ParticipantId> roomOwnerMap;

    public ServerState() {
        this.state = new HashMap<>();
        // TODO: Add empty hash map for every lk.ac.mrt.cse.cs4262.server id possible.
        this.state.put(SERVER, new HashMap<>());
        this.participantServerMap = new HashMap<>();
        this.roomOwnerMap = new HashMap<>();
    }

    public void addLog(@NonNull BaseLog logEntry) {
        if (logEntry instanceof CreateIdentityLog) {
            createIdentity((CreateIdentityLog) logEntry);
        } else if (logEntry instanceof CreateRoomLog) {
            createRoom((CreateRoomLog) logEntry);
        } else if (logEntry instanceof DeleteIdentityLog) {
            deleteIdentity((DeleteIdentityLog) logEntry);
        } else if (logEntry instanceof DeleteRoomLog) {
            deleteRoom((DeleteRoomLog) logEntry);
        }
        log.info(this);
    }

    private void createIdentity(@NonNull CreateIdentityLog logEntry) {
        ParticipantId participantId = new ParticipantId(logEntry.getIdentity());
        ServerId serverId = new ServerId(logEntry.getServerId());
        state.get(serverId).put(participantId, null);
        participantServerMap.put(participantId, serverId);
    }

    private void createRoom(@NonNull CreateRoomLog logEntry) {
        ParticipantId participantId = new ParticipantId(logEntry.getParticipantId());
        RoomId roomId = new RoomId(logEntry.getRoomId());
        ServerId serverId = participantServerMap.get(participantId);
        state.get(serverId).put(participantId, roomId);
        roomOwnerMap.put(roomId, participantId);
    }

    private void deleteIdentity(@NonNull DeleteIdentityLog logEntry) {
        ParticipantId participantId = new ParticipantId(logEntry.getIdentity());
        ServerId serverId = participantServerMap.remove(participantId);
        RoomId ownedRoomId = state.get(serverId).remove(participantId);
        if (ownedRoomId != null) {
            roomOwnerMap.remove(ownedRoomId);
        }
    }

    private void deleteRoom(@NonNull DeleteRoomLog logEntry) {
        RoomId roomId = new RoomId(logEntry.getRoomId());
        ParticipantId ownerId = roomOwnerMap.remove(roomId);
        ServerId serverId = participantServerMap.get(ownerId);
        state.get(serverId).put(ownerId, null);
    }

    public boolean hasParticipant(ParticipantId participantId) {
        return participantServerMap.containsKey(participantId);
    }

    public boolean hasRoom(RoomId roomId) {
        if (MAIN_HALL.equals(roomId)) {
            return true;
        }
        return roomOwnerMap.containsKey(roomId);
    }

    public Collection<RoomId> serverRoomIds() {
        HashSet<RoomId> roomIds = new HashSet<>();
        roomIds.add(ServerState.MAIN_HALL);
        state.get(SERVER).forEach((key, value) -> {
            if (value != null) roomIds.add(value);
        });
        return roomIds;
    }

    public ParticipantId getOwnerId(RoomId roomId) {
        return roomOwnerMap.get(roomId);
    }
}
