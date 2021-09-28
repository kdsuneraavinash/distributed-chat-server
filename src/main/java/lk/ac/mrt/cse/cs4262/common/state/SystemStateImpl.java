package lk.ac.mrt.cse.cs4262.common.state;

import lk.ac.mrt.cse.cs4262.common.state.logs.BaseLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.DeleteIdentityLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.DeleteRoomLog;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation for the {@link SystemState}.
 */
@ToString
@Log4j2
public final class SystemStateImpl implements SystemState {
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
     * Map for each server containing its participants.
     * If the participant owns a room, it will also be recorded.
     * This is the primary state object.
     * Other objects are simply for performance-sake.
     */
    private final Map<@NonNull ServerId, HashMap<@NonNull ParticipantId, RoomId>> state;

    /**
     * Map for each participant to record the server that they are part of.
     * This is a derived value of {@code state}. Used for reverse relations.
     */
    private final Map<@NonNull ParticipantId, @NonNull ServerId> participantServerMap;
    /**
     * Map for each room to record their owner.
     * This is a derived value of {@code state}. Used for reverse relations.
     */
    private final Map<@NonNull RoomId, @NonNull ParticipantId> roomOwnerMap;

    /**
     * Create a system state. See {@link SystemStateImpl}.
     */
    public SystemStateImpl() {
        this.state = new HashMap<>();
        this.participantServerMap = new HashMap<>();
        this.roomOwnerMap = new HashMap<>();

        // TODO: Do this for all the servers.
        createReservedIdsForServer(getCurrentServerId());

        // TODO: Restore persisted state.
    }

    @Override
    public void apply(@NonNull BaseLog logEntry) {
        if (logEntry instanceof CreateIdentityLog) {
            applyCreateIdentityLog((CreateIdentityLog) logEntry);
        } else if (logEntry instanceof CreateRoomLog) {
            applyCreateRoomLog((CreateRoomLog) logEntry);
        } else if (logEntry instanceof DeleteIdentityLog) {
            applyDeleteIdentityLog((DeleteIdentityLog) logEntry);
        } else if (logEntry instanceof DeleteRoomLog) {
            applyDeleteRoomLog((DeleteRoomLog) logEntry);
        } else {
            throw new UnsupportedOperationException();
        }
        // TODO: Persist state.
        log.info(this);
    }

    @Override
    public boolean hasParticipant(@NonNull ParticipantId participantId) {
        return false;
    }

    @Override
    public boolean hasRoom(@NonNull RoomId roomId) {
        return roomOwnerMap.containsKey(roomId);
    }

    @Override
    public @NonNull Collection<RoomId> serverRoomIds(@NonNull ServerId serverId) {
        Set<RoomId> roomIds = new HashSet<>();
        // TODO: Increase performance by maintaining a data structure?
        state.get(serverId).forEach((key, value) -> {
            if (value != null) {
                roomIds.add(value);
            }
        });
        return roomIds;
    }

    @Override
    public @NonNull ParticipantId getOwnerId(@NonNull RoomId roomId) {
        return roomOwnerMap.get(roomId);
    }

    @Override
    public @NonNull ServerId getCurrentServerId() {
        // TODO: Implement this using configurations.
        return new ServerId("SERVER");
    }

    @Override
    public @NonNull ParticipantId getSystemUserId(@NonNull ServerId serverId) {
        return new ParticipantId(SYSTEM_USER_PREFIX + serverId.getValue());
    }

    @Override
    public @NonNull RoomId getMainRoomId(@NonNull ServerId serverId) {
        return new RoomId(MAIN_ROOM_PREFIX + serverId.getValue());
    }

    private void createReservedIdsForServer(@NonNull ServerId serverId) {
        RoomId mainRoomId = getMainRoomId(serverId);
        ParticipantId systemUserId = getSystemUserId(serverId);
        this.state.put(serverId, new HashMap<>());
        this.state.get(serverId).put(systemUserId, mainRoomId);
        this.participantServerMap.put(systemUserId, serverId);
        this.roomOwnerMap.put(mainRoomId, systemUserId);
    }

    private void applyCreateIdentityLog(@NonNull CreateIdentityLog logEntry) {
        ParticipantId participantId = new ParticipantId(logEntry.getIdentity());
        ServerId serverId = new ServerId(logEntry.getServerId());
        state.get(serverId).put(participantId, null);
        participantServerMap.put(participantId, serverId);
    }

    private void applyCreateRoomLog(@NonNull CreateRoomLog logEntry) {
        ParticipantId participantId = new ParticipantId(logEntry.getParticipantId());
        RoomId roomId = new RoomId(logEntry.getRoomId());
        ServerId serverId = participantServerMap.get(participantId);
        state.get(serverId).put(participantId, roomId);
        roomOwnerMap.put(roomId, participantId);
    }

    private void applyDeleteIdentityLog(@NonNull DeleteIdentityLog logEntry) {
        ParticipantId participantId = new ParticipantId(logEntry.getIdentity());
        ServerId serverId = participantServerMap.remove(participantId);
        RoomId ownedRoomId = state.get(serverId).remove(participantId);
        if (ownedRoomId != null) {
            roomOwnerMap.remove(ownedRoomId);
        }
    }

    private void applyDeleteRoomLog(@NonNull DeleteRoomLog logEntry) {
        RoomId roomId = new RoomId(logEntry.getRoomId());
        ParticipantId ownerId = roomOwnerMap.remove(roomId);
        ServerId serverId = participantServerMap.get(ownerId);
        state.get(serverId).put(ownerId, null);
    }
}
