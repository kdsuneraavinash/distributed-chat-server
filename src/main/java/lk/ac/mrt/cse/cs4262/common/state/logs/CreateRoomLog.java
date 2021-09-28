package lk.ac.mrt.cse.cs4262.common.state.logs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * The log for creating a room.
 */
@Getter
@ToString
@AllArgsConstructor
public class CreateRoomLog implements BaseLog {
    private final String roomId;
    private final String participantId;
}
