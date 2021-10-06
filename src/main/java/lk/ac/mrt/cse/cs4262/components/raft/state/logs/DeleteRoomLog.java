package lk.ac.mrt.cse.cs4262.components.raft.state.logs;

import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lombok.Builder;
import lombok.ToString;

/**
 * The log for deleting a room.
 */
@ToString
public class DeleteRoomLog extends BaseLog {
    private final String roomId;

    /**
     * See {@link DeleteRoomLog}.
     *
     * @param roomId Room to delete.
     */
    @Builder
    public DeleteRoomLog(RoomId roomId) {
        super(DELETE_ROOM_LOG);
        this.roomId = roomId.getValue();
    }

    /**
     * @return Room to delete.
     */
    public RoomId getRoomId() {
        return new RoomId(roomId);
    }
}
