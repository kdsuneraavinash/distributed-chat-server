package lk.ac.mrt.cse.cs4262.components.raft.messages.variants;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.MessageType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class VoteRequestMessage extends BaseRaftMessage {
    private final int term;
    private final int lastLogTerm;
    private final int lastLogIndex;

    /**
     * Create {@link AppendReplyMessage}.
     *
     * @param senderId     Sender server ID.
     * @param term         Sender's term.
     * @param lastLogTerm  Last log's term.
     * @param lastLogIndex Last log's index.
     */
    @Builder
    public VoteRequestMessage(ServerId senderId, int term, int lastLogTerm, int lastLogIndex) {
        super(MessageType.REQUEST_VOTE_REQ, senderId.getValue());
        this.term = term;
        this.lastLogTerm = lastLogTerm;
        this.lastLogIndex = lastLogIndex;
    }
}
