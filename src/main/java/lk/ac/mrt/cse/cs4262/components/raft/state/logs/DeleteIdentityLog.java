package lk.ac.mrt.cse.cs4262.components.raft.state.logs;

import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lombok.Builder;
import lombok.ToString;

/**
 * The log for deleting an identity.
 */
@ToString
public class DeleteIdentityLog extends BaseLog {
    private final String identity;

    /**
     * See {@link DeleteIdentityLog}.
     *
     * @param identity Identity to delete.
     */
    @Builder
    public DeleteIdentityLog(ParticipantId identity) {
        super(DELETE_IDENTITY_LOG);
        this.identity = identity.getValue();
    }

    /**
     * @return Identity to delete.
     */
    public ParticipantId getIdentity() {
        return new ParticipantId(identity);
    }
}
