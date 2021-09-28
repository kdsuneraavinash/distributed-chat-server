package lk.ac.mrt.cse.cs4262.common.state.logs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * The log for deleting a room.
 */
@Getter
@ToString
@AllArgsConstructor
public class DeleteRoomLog implements BaseLog {
    private final String roomId;
}
