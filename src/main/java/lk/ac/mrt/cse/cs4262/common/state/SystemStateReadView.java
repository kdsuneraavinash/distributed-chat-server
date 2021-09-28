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
     * @param participantId ID of the participant.
     * @return Room ID if the participant owns a room. Otherwise null.
     */
    RoomId owningRoom(@NonNull ParticipantId participantId);

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
     * @param roomId ID of the room.
     * @return The ID of the server with the room.
     */
    @NonNull ServerId getRoomServerId(@NonNull RoomId roomId);

    /**
     * Attaches a listener to listen state events.
     *
     * @param newReporter Listener to attach.
     */
    void attachListener(@NonNull SystemStateReadView.Reporter newReporter);

    /**
     * Listener for System state change events.
     */
    interface Reporter {
        /**
         * @param createParticipantId Created identity.
         */
        void participantIdCreated(@NonNull ParticipantId createParticipantId);

        /**
         * @param ownerId       Owner id of created room.
         * @param createdRoomId Created room id.
         */
        void roomIdCreated(@NonNull ParticipantId ownerId, @NonNull RoomId createdRoomId);


        /**
         * @param deletedId     Deleted identity.
         * @param deletedRoomId Room id owned by deleted participant. (if any)
         */
        void participantIdDeleted(@NonNull ParticipantId deletedId,
                                  RoomId deletedRoomId);

        /**
         * @param deletedRoomId Deleted room id.
         */
        void roomIdDeleted(RoomId deletedRoomId);
    }
}
