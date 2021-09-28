package lk.ac.mrt.cse.cs4262.server.state.logs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class DeleteIdentityLog extends BaseLog {
    public static final String COMMAND = "DELETE_IDENTITY";

    private final String identity;
}
