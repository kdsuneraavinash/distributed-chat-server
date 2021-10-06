package lk.ac.mrt.cse.cs4262.components.raft.messages.variants;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.MessageType;
import lombok.Builder;
import lombok.Getter;

@Getter
public class VoteReplyMessage extends BaseRaftMessage {
    private final int term;
    private final String vote;

    @Builder
    public VoteReplyMessage(ServerId senderId, int term, ServerId vote) {
        super(MessageType.REQUEST_VOTE_REP, senderId.getValue());
        this.term = term;
        this.vote = vote.getValue();
    }

    public ServerId getVote() {
        return new ServerId(vote);
    }
}
