package lk.ac.mrt.cse.cs4262.server.state.logs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class CreateIdentityLog extends BaseLog {
    public static final String COMMAND = "CREATE_IDENTITY";

    private final String serverId;
    private final String identity;
}
