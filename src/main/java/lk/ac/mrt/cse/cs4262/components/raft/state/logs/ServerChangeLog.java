package lk.ac.mrt.cse.cs4262.components.raft.state.logs;

import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.Builder;
import lombok.ToString;

/**
 * Log for server change when a participant moves from one server
 * to another.
 */
@ToString
public class ServerChangeLog extends BaseLog {
    private final String formerServerId;
    private final String newServerId;
    private final String participantId;

    /**
     * See {@link ServerChangeLog}.
     *
     * @param formerServerId Former server id of the participant.
     * @param newServerId    New server id of the participant.
     * @param participantId  Participant ID.
     */
    @Builder
    public ServerChangeLog(ServerId formerServerId, ServerId newServerId, ParticipantId participantId) {
        super(SERVER_CHANGE_LOG);
        this.formerServerId = formerServerId.getValue();
        this.newServerId = newServerId.getValue();
        this.participantId = participantId.getValue();
    }

    /**
     * Getter for formerServerID.
     *
     * @return ServerID
     */
    public ServerId getFormerServerId() {
        return new ServerId(formerServerId);
    }

    /**
     * Getter for new server ID.
     *
     * @return Server ID
     */
    public ServerId getNewServerId() {
        return new ServerId(newServerId);
    }

    /**
     * Getter for Participant ID.
     *
     * @return Participant ID
     */
    public ParticipantId getParticipantId() {
        return new ParticipantId(participantId);
    }
}
