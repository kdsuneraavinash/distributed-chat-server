package lk.ac.mrt.cse.cs4262.components.raft.messages;

import com.google.gson.JsonParseException;
import lk.ac.mrt.cse.cs4262.common.utils.TypedJsonDeserializer;
import lk.ac.mrt.cse.cs4262.components.raft.messages.responses.BaseRaftResponse;
import lk.ac.mrt.cse.cs4262.components.raft.messages.responses.RequestVoteReqResponse;
import lk.ac.mrt.cse.cs4262.components.raft.messages.responses.RequestVoteResResponse;

/**
 * A serializer for client messages.
 */
public class RaftMessageDeserializer extends TypedJsonDeserializer<BaseRaftResponse> {
    private static final String REQUEST_VOTE_REQ = "REQUEST_VOTE_REQ";
    private static final String REQUEST_VOTE_REP = "REQUEST_VOTE_REP";

    protected RaftMessageDeserializer() {
        super("action");
    }

    @Override
    protected Class<?> mapTypeToClass(String action) throws JsonParseException {
        // TODO: Register other request types
        switch (action) {
            case REQUEST_VOTE_REQ:
                return RequestVoteReqResponse.class;
            case REQUEST_VOTE_REP:
                return RequestVoteResResponse.class;
            default:
                throw new JsonParseException("unknown type: " + action);
        }
    }
}
