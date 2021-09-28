package lk.ac.mrt.cse.cs4262.common.state;

import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.NonNull;

import java.util.Collection;

/**
 * The State containing the primary system state read view.
 * No edits are permitted via this view.
 * TODO: Change Client component to accept SystemStateReadView.
 */
public interface SystemStateReadView {
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

    /**
     * Listener for System state change events.
     */
    interface Listener {
        /**
         * @param serverId      Owning server of the created identity.
         * @param participantId Created identity.
         */
        void createdParticipantId(@NonNull ServerId serverId, @NonNull ParticipantId participantId);

        /**
         * @param serverId Owning server of the created room.
         * @param ownerId  Owner id of created room.
         * @param roomId   Created room id.
         */
        void createdRoom(@NonNull ServerId serverId, @NonNull ParticipantId ownerId, @NonNull RoomId roomId);


        /**
         * @param serverId      Owning server of the deleted identity.
         * @param participantId Deleted identity.
         * @param deletedRoomId Room id owned by deleted participant. (if any)
         */
        void deletedIdentity(@NonNull ServerId serverId, @NonNull ParticipantId participantId, RoomId deletedRoomId);

        /**
         * @param serverId      Owning server of the deleted room.
         * @param deletedRoomId Deleted room id.
         */
        void deletedRoom(@NonNull ServerId serverId, RoomId deletedRoomId);
    }
}
