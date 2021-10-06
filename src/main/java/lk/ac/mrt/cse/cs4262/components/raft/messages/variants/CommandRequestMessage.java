package lk.ac.mrt.cse.cs4262.components.raft.messages.variants;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.MessageType;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.BaseLog;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class CommandRequestMessage extends BaseRaftMessage {
    private final BaseLog command;

    /**
     * Create {@link CommandRequestMessage}.
     *
     * @param senderId Sender server ID.
     * @param command  Command to send.
     */
    @Builder
    public CommandRequestMessage(ServerId senderId, BaseLog command) {
        super(MessageType.COMMAND_REQ, senderId.getValue());
        this.command = command;
    }
}
