package lk.ac.mrt.cse.cs4262.components.raft.messages.variants;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.MessageType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class AppendReplyMessage extends BaseRaftMessage {
    private final int term;
    private final boolean success;
    private final int index;

    /**
     * Create {@link AppendReplyMessage}.
     *
     * @param senderId Sender server ID.
     * @param term     Sender's term.
     * @param success  Whether append was success.
     * @param index    Appended index.
     */
    @Builder
    public AppendReplyMessage(ServerId senderId, int term, boolean success, int index) {
        super(MessageType.APPEND_ENTRIES_REP, senderId.getValue());
        this.term = term;
        this.success = success;
        this.index = index;
    }
}
