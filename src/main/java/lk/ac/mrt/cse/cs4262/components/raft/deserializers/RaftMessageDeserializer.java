package lk.ac.mrt.cse.cs4262.components.raft.deserializers;

import com.google.gson.JsonParseException;
import lk.ac.mrt.cse.cs4262.common.utils.TypedJsonDeserializer;
import lk.ac.mrt.cse.cs4262.components.raft.messages.AppendReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.AppendRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.BaseRaftMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.CommandRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.VoteReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.VoteRequestMessage;

/**
 * A serializer for raft messages.
 */
public class RaftMessageDeserializer extends TypedJsonDeserializer<BaseRaftMessage> {
    protected RaftMessageDeserializer() {
        super("action");
    }

    @Override
    protected Class<?> mapTypeToClass(String action) throws JsonParseException {
        switch (action) {
            case BaseRaftMessage.REQUEST_VOTE_REQ:
                return VoteRequestMessage.class;
            case BaseRaftMessage.REQUEST_VOTE_REP:
                return VoteReplyMessage.class;
            case BaseRaftMessage.COMMAND_REQ:
                return CommandRequestMessage.class;
            case BaseRaftMessage.APPEND_ENTRIES_REQ:
                return AppendRequestMessage.class;
            case BaseRaftMessage.APPEND_ENTRIES_REP:
                return AppendReplyMessage.class;
            default:
                throw new JsonParseException("unknown action: " + action);
        }
    }
}
