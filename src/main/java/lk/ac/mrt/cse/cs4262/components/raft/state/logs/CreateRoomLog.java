package lk.ac.mrt.cse.cs4262.components.raft.state.logs;

import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lombok.Builder;
import lombok.ToString;

/**
 * The log for creating a room.
 */
@ToString
public class CreateRoomLog extends BaseLog {
    private final String roomId;
    private final String participantId;

    /**
     * See {@link CreateRoomLog}.
     *
     * @param roomId        Room to create.
     * @param participantId Owner of the room.
     */
    @Builder
    public CreateRoomLog(RoomId roomId, ParticipantId participantId) {
        super(CREATE_ROOM_LOG);
        this.roomId = roomId.getValue();
        this.participantId = participantId.getValue();
    }

    /**
     * @return Room to create.
     */
    public RoomId getRoomId() {
        return new RoomId(roomId);
    }

    /**
     * @return Owner of the room.
     */
    public ParticipantId getParticipantId() {
        return new ParticipantId(participantId);
    }
}
