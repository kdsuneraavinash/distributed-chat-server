package server.state.logs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class CreateRoomLog extends BaseLog {
    public static final String COMMAND = "CREATE_ROOM";

    private final String roomId;
    private final String participantId;
}
