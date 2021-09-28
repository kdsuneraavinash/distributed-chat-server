package lk.ac.mrt.cse.cs4262.server.state.logs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class DeleteRoomLog extends BaseLog {
    public static final String COMMAND = "DELETE_ROOM";

    private final String roomId;
}
