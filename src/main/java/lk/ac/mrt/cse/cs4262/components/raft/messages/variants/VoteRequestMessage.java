package lk.ac.mrt.cse.cs4262.components.raft.messages.variants;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.MessageType;
import lombok.Builder;
import lombok.Getter;

@Getter
public class VoteRequestMessage extends BaseRaftMessage {
    private final int term;
    private final int lastLogTerm;
    private final int lastLogIndex;

    @Builder
    public VoteRequestMessage(ServerId senderId, int term, int lastLogTerm, int lastLogIndex) {
        super(MessageType.REQUEST_VOTE_REQ, senderId.getValue());
        this.term = term;
        this.lastLogTerm = lastLogTerm;
        this.lastLogIndex = lastLogIndex;
    }
}
