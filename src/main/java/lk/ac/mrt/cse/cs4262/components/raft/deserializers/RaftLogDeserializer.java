package lk.ac.mrt.cse.cs4262.components.raft.deserializers;

import com.google.gson.JsonParseException;
import lk.ac.mrt.cse.cs4262.common.utils.TypedJsonDeserializer;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.BaseLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.DeleteIdentityLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.DeleteRoomLog;

/**
 * A serializer for raft messages.
 */
public class RaftLogDeserializer extends TypedJsonDeserializer<BaseLog> {
    protected RaftLogDeserializer() {
        super("type");
    }

    @Override
    protected Class<?> mapTypeToClass(String action) throws JsonParseException {
        switch (action) {
            case BaseLog.CREATE_IDENTITY_LOG:
                return CreateIdentityLog.class;
            case BaseLog.CREATE_ROOM_LOG:
                return CreateRoomLog.class;
            case BaseLog.DELETE_IDENTITY_LOG:
                return DeleteIdentityLog.class;
            case BaseLog.DELETE_ROOM_LOG:
                return DeleteRoomLog.class;
            default:
                throw new JsonParseException("unknown type: " + action);
        }
    }
}
