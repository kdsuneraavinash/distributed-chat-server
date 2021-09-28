package lk.ac.mrt.cse.cs4262.common.state.logs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * The log for deleting an identity.
 */
@Getter
@ToString
@AllArgsConstructor
public class DeleteIdentityLog implements BaseLog {
    private final String identity;
}
