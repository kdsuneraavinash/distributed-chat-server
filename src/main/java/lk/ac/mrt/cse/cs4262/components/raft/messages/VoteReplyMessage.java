package lk.ac.mrt.cse.cs4262.components.raft.messages;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class VoteReplyMessage extends BaseRaftMessage {
    private final int term;
    private final String vote;

    /**
     * Create {@link AppendReplyMessage}.
     *
     * @param senderId Sender server ID.
     * @param term     Sender's term.
     * @param vote     Voted server ID.
     */
    @Builder
    public VoteReplyMessage(ServerId senderId, int term, ServerId vote) {
        super(REQUEST_VOTE_REP, senderId.getValue());
        this.term = term;
        this.vote = vote.getValue();
    }

    /**
     * @return Voted server ID.
     */
    public ServerId getVote() {
        return new ServerId(vote);
    }
}
