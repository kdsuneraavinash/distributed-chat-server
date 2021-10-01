package lk.ac.mrt.cse.cs4262.components.raft.state.logs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * The log for creating an identity.
 */
@Getter
@ToString
@AllArgsConstructor
public class CreateIdentityLog implements BaseLog {
    private final String serverId;
    private final String identity;
}
