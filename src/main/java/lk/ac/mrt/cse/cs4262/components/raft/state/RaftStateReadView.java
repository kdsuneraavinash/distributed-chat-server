package lk.ac.mrt.cse.cs4262.components.raft.state;

import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.BaseLog;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * The State containing the primary system state read view.
 * No edits are permitted via this view.
 */
public interface RaftStateReadView {
    /**
     * @param roomId ID of the room.
     * @return Whether the room is active in the system.
     */
    boolean hasRoom(RoomId roomId);

    /**
     * @param participantId ID of the participant.
     * @return Room ID if the participant owns a room. Otherwise null.
     */
    Optional<RoomId> getRoomOwnedByParticipant(ParticipantId participantId);

    /**
     * @param serverId ID of the server.
     * @return A list of all the active participant IDs in the specified server.
     */
    Collection<ParticipantId> getParticipantsInServer(ServerId serverId);

    /**
     * @return A list of all the active room IDs in the system.
     */
    Collection<RoomId> getRoomsInSystem();

    /**
     * @param roomId ID of the room.
     * @return Participant ID of the owner of the room.
     */
    ParticipantId getOwnerOfRoom(RoomId roomId);

    /**
     * @param roomId ID of the room.
     * @return The ID of the server with the room.
     */
    ServerId getServerOfRoom(RoomId roomId);

    /**
     * @param serverId ID of the server.
     * @return The ID of the system user of specified server.
     */
    ParticipantId getSystemUserId(ServerId serverId);

    /**
     * @param serverId ID of the server.
     * @return The ID of the main room of specified server.
     */
    RoomId getMainRoomId(ServerId serverId);

    /**
     * @return The current leader of system.
     */
    Optional<ServerId> getLeaderId();

    /**
     * @param baseLog Log to check.
     * @return Whether the log can be safely applied.
     */
    boolean isAcceptable(BaseLog baseLog);

    /**
     * Attaches a listener to listen state events.
     *
     * @param newEventHandler Listener to attach.
     */
    void attachListener(EventHandler newEventHandler);

    /**
     * Listener for System state change events.
     */
    interface EventHandler {
        /**
         * @param createdParticipantId Created identity.
         */
        void participantIdCreated(ParticipantId createdParticipantId);

        /**
         * @param ownerParticipantId Owner id of created room.
         * @param createdRoomId      Created room id.
         */
        void roomIdCreated(ParticipantId ownerParticipantId, RoomId createdRoomId);


        /**
         * @param deletedParticipantId Deleted identity.
         * @param deletedRoomId        Room id owned by deleted participant. (if any)
         */
        void participantIdDeleted(ParticipantId deletedParticipantId, @Nullable RoomId deletedRoomId);

        /**
         * @param deletedRoomId Deleted room id.
         * @param ownerId       Owner id.
         */
        void roomIdDeleted(RoomId deletedRoomId, ParticipantId ownerId);

        /**
         * Method to notify when a participant moves to another server.
         *
         * @param movedParticipant Participant ID
         */
        void participantMoved(ParticipantId movedParticipant);

        /**
         * Method to notify when a participant from another server joins.
         *
         * @param joinedParticipant Participant ID
         * @param serverId          Server ID
         */
        void participantJoined(ParticipantId joinedParticipant, ServerId serverId);
    }
}
