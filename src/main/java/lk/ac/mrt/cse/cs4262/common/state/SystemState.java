package lk.ac.mrt.cse.cs4262.common.state;

import lk.ac.mrt.cse.cs4262.common.state.logs.BaseLog;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.NonNull;

import java.util.Collection;

/**
 * The State containing the primary system state.
 * Will contain all the servers, participants and rooms.
 * This is only updated via log entries and will be persisted.
 */
public interface SystemState {
    /**
     * Adds a log to the state. This can alter the state.
     * Any changes done to the state will be persisted.
     *
     * @param logEntry Log entry to apply to the system.
     */
    void apply(@NonNull BaseLog logEntry);

    /**
     * @param participantId ID of the participant.
     * @return Whether the participant is active in the system.
     */
    boolean hasParticipant(@NonNull ParticipantId participantId);

    /**
     * @param roomId ID of the room.
     * @return Whether the room is active in the system.
     */
    boolean hasRoom(@NonNull RoomId roomId);

    /**
     * @param serverId ID of the server.
     * @return A list of all the active room IDs in the specified server.
     */
    @NonNull Collection<RoomId> serverRoomIds(@NonNull ServerId serverId);

    /**
     * @param roomId ID of the room.
     * @return Participant ID of the owner of the room.
     */
    @NonNull ParticipantId getOwnerId(@NonNull RoomId roomId);

    /**
     * @return The ID of the current active server.
     */
    @NonNull ServerId getCurrentServerId();

    /**
     * @param serverId ID of the server.
     * @return The ID of the system user of specified server.
     */
    @NonNull ParticipantId getSystemUserId(@NonNull ServerId serverId);

    /**
     * @param serverId ID of the server.
     * @return The ID of the main room of specified server.
     */
    @NonNull RoomId getMainRoomId(@NonNull ServerId serverId);
}
