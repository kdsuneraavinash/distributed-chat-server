package lk.ac.mrt.cse.cs4262.components.raft.messages.variants;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.MessageType;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftLog;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class AppendRequestMessage extends BaseRaftMessage {
    private final int term;
    private final int prevIndex;
    private final int prevTerm;
    private final List<RaftLog> entries;
    private final int commitIndex;

    /**
     * Create {@link AppendRequestMessage}.
     *
     * @param senderId    Sender server ID.
     * @param term        Sender's term.
     * @param prevIndex   Previous index sent.
     * @param prevTerm    Term of previous sent.
     * @param entries     Entries to append
     * @param commitIndex Committed messages.
     */
    @Builder
    public AppendRequestMessage(ServerId senderId, int term, int prevIndex, int prevTerm,
                                List<RaftLog> entries, int commitIndex) {
        super(MessageType.APPEND_ENTRIES_REQ, senderId.getValue());
        this.term = term;
        this.prevIndex = prevIndex;
        this.prevTerm = prevTerm;
        this.entries = entries;
        this.commitIndex = commitIndex;
    }
}
