package lk.ac.mrt.cse.cs4262.components.raft.state.logs;

import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.Builder;
import lombok.ToString;

/**
 * The log for creating an identity.
 */
@ToString
public class CreateIdentityLog extends BaseLog {
    private final String serverId;
    private final String identity;

    /**
     * See {@link CreateIdentityLog}.
     *
     * @param serverId Server to create identity.
     * @param identity Identity to create.
     */
    @Builder
    public CreateIdentityLog(ServerId serverId, ParticipantId identity) {
        super(CREATE_IDENTITY_LOG);
        this.serverId = serverId.getValue();
        this.identity = identity.getValue();
    }

    /**
     * @return Identity to create.
     */
    public ParticipantId getIdentity() {
        return new ParticipantId(identity);
    }

    /**
     * @return Server to create identity.
     */
    public ServerId getServerId() {
        return new ServerId(serverId);
    }
}
