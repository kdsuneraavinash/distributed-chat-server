package lk.ac.mrt.cse.cs4262.components.raft.messages;

import com.google.gson.JsonParseException;
import lk.ac.mrt.cse.cs4262.common.utils.TypedJsonDeserializer;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.AppendReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.AppendRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.BaseRaftMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.CommandRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.VoteReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.VoteRequestMessage;

/**
 * A serializer for client messages.
 */
public class RaftMessageDeserializer extends TypedJsonDeserializer<BaseRaftMessage> {
    protected RaftMessageDeserializer() {
        super("action");
    }

    @Override
    protected Class<?> mapTypeToClass(String action) throws JsonParseException {
        // TODO: Register other request types
        switch (action) {
            case MessageType.REQUEST_VOTE_REQ:
                return VoteRequestMessage.class;
            case MessageType.REQUEST_VOTE_REP:
                return VoteReplyMessage.class;
            case MessageType.COMMAND_REQ:
                return CommandRequestMessage.class;
            case MessageType.APPEND_ENTRIES_REQ:
                return AppendRequestMessage.class;
            case MessageType.APPEND_ENTRIES_REP:
                return AppendReplyMessage.class;
            default:
                throw new JsonParseException("unknown type: " + action);
        }
    }
}
