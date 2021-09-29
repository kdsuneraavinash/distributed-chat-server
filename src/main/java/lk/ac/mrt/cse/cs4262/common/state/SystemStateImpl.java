package lk.ac.mrt.cse.cs4262.common.state;

import lk.ac.mrt.cse.cs4262.common.state.logs.BaseLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.DeleteIdentityLog;
import lk.ac.mrt.cse.cs4262.common.state.logs.DeleteRoomLog;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation for the {@link SystemState}.
 */
@ToString(onlyExplicitlyIncluded = true)
@Log4j2
public class SystemStateImpl implements SystemState {
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
    @ToString.Include
    private final Map<ServerId, HashMap<ParticipantId, @Nullable RoomId>> state;

    /**
     * Map for each participant to record the server that they are part of.
     * This is a derived value of {@code state}. Used for reverse relations.
     */
    @ToString.Include
    private final Map<ParticipantId, ServerId> participantServerMap;
    /**
     * Map for each room to record their owner.
     * This is a derived value of {@code state}. Used for reverse relations.
     */
    @ToString.Include
    private final Map<RoomId, ParticipantId> roomOwnerMap;
    /**
     * Listener to attach for the changes in the system.
     */
    @Nullable
    private EventHandler eventHandler;

    /**
     * Create a system state. See {@link SystemStateImpl}.
     */
    public SystemStateImpl() {
        this.state = new HashMap<>();
        this.participantServerMap = new HashMap<>();
        this.roomOwnerMap = new HashMap<>();
    }

    @Override
    public void initialize() {
        ServerId serverId = getCurrentServerId();
        // TODO: Do this for all the servers.
        RoomId mainRoomId = getMainRoomId(serverId);
        ParticipantId systemUserId = getSystemUserId(serverId);
        this.state.put(serverId, new HashMap<>());
        this.state.get(serverId).put(systemUserId, mainRoomId);
        this.participantServerMap.put(systemUserId, serverId);
        this.roomOwnerMap.put(mainRoomId, systemUserId);

        // TODO: Restore persisted state.
    }

    /*
    ========================================================
    Write Methods
    ========================================================
     */

    @Override
    public void apply(BaseLog logEntry) {
        log.info("log: {}", logEntry);
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
        log.debug("state after: {}", this);
    }

    /*
    ========================================================
    Read Methods
    ========================================================
     */

    @Override
    public boolean hasParticipant(ParticipantId participantId) {
        return participantServerMap.containsKey(participantId);
    }

    @Override
    public boolean hasRoom(RoomId roomId) {
        if (RoomId.NULL.equals(roomId)) {
            return true;
        }
        return roomOwnerMap.containsKey(roomId);
    }

    @Override
    public Optional<RoomId> getRoomOwnedByParticipant(ParticipantId participantId) {
        if (participantServerMap.containsKey(participantId)) {
            ServerId serverId = participantServerMap.get(participantId);
            if (state.containsKey(serverId)) {
                Map<ParticipantId, @Nullable RoomId> prMap = state.get(serverId);
                if (prMap.containsKey(participantId)) {
                    return Optional.ofNullable(prMap.get(participantId));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Collection<RoomId> getRoomsInServer(ServerId serverId) {
        if (state.containsKey(serverId)) {
            return state.get(serverId).values().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public Optional<ParticipantId> getOwnerOfRoom(RoomId roomId) {
        return Optional.ofNullable(roomOwnerMap.get(roomId));
    }

    @Override
    public Optional<ServerId> getServerOfRoom(RoomId roomId) {
        return getOwnerOfRoom(roomId).map(participantServerMap::get);
    }

    @Override
    public Optional<ServerId> getServerOfParticipant(ParticipantId participantId) {
        return Optional.ofNullable(participantServerMap.get(participantId));
    }

    @Override
    public ServerId getCurrentServerId() {
        // TODO: Implement this using configurations.
        return new ServerId("SERVER");
    }

    @Override
    public ParticipantId getSystemUserId(ServerId serverId) {
        return new ParticipantId(SYSTEM_USER_PREFIX + serverId.getValue());
    }

    @Override
    public RoomId getMainRoomId(ServerId serverId) {
        return new RoomId(MAIN_ROOM_PREFIX + serverId.getValue());
    }

    @Override
    public void attachListener(EventHandler newEventHandler) {
        this.eventHandler = newEventHandler;
    }

    /*
    ========================================================
    Write Helper Methods
    ========================================================
     */

    private void applyCreateIdentityLog(CreateIdentityLog logEntry) {
        ParticipantId participantId = new ParticipantId(logEntry.getIdentity());
        ServerId serverId = new ServerId(logEntry.getServerId());
        if (!state.containsKey(serverId)) {
            throw new IllegalStateException("unknown server id");
        }
        state.get(serverId).put(participantId, null);
        participantServerMap.put(participantId, serverId);
        if (getCurrentServerId().equals(serverId) && eventHandler != null) {
            eventHandler.participantIdCreated(participantId);
        }
    }

    private void applyCreateRoomLog(CreateRoomLog logEntry) {
        ParticipantId participantId = new ParticipantId(logEntry.getParticipantId());
        RoomId roomId = new RoomId(logEntry.getRoomId());
        ServerId serverId = participantServerMap.get(participantId);
        if (serverId == null || !state.containsKey(serverId)) {
            throw new IllegalStateException("unknown server id");
        }
        state.get(serverId).put(participantId, roomId);
        roomOwnerMap.put(roomId, participantId);
        if (getCurrentServerId().equals(serverId) && eventHandler != null) {
            eventHandler.roomIdCreated(participantId, roomId);
        }
    }

    private void applyDeleteIdentityLog(DeleteIdentityLog logEntry) {
        ParticipantId participantId = new ParticipantId(logEntry.getIdentity());
        ServerId serverId = participantServerMap.remove(participantId);
        if (serverId == null || !state.containsKey(serverId)) {
            throw new IllegalStateException("unknown server id");
        }
        RoomId ownedRoomId = state.get(serverId).remove(participantId);
        if (ownedRoomId != null) {
            roomOwnerMap.remove(ownedRoomId);
        }
        if (getCurrentServerId().equals(serverId) && eventHandler != null) {
            eventHandler.participantIdDeleted(participantId, ownedRoomId);
        }
    }

    private void applyDeleteRoomLog(DeleteRoomLog logEntry) {
        RoomId roomId = new RoomId(logEntry.getRoomId());
        ParticipantId ownerId = roomOwnerMap.remove(roomId);
        if (ownerId == null) {
            throw new IllegalStateException("owner cannot be null");
        }
        ServerId serverId = participantServerMap.get(ownerId);
        if (serverId == null || !state.containsKey(serverId)) {
            throw new IllegalStateException("unknown server id");
        }
        state.get(serverId).put(ownerId, null);
        if (getCurrentServerId().equals(serverId) && eventHandler != null) {
            eventHandler.roomIdDeleted(roomId);
        }
    }
}
