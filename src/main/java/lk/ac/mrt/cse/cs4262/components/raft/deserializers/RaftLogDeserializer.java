package lk.ac.mrt.cse.cs4262.components.raft.deserializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lk.ac.mrt.cse.cs4262.common.utils.TypedJsonDeserializer;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.BaseLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.CreateIdentityLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.CreateRoomLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.DeleteIdentityLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.DeleteRoomLog;

import java.lang.reflect.Type;

/**
 * A serializer for raft messages.
 * The serializer part is required because of an GSON issue causing collections
 * of abstract types to not get serialized correctly.
 */
public class RaftLogDeserializer extends TypedJsonDeserializer<BaseLog> implements JsonSerializer<BaseLog> {
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

    @Override
    public JsonElement serialize(BaseLog src, Type typeOfSrc, JsonSerializationContext context) {
        if (src instanceof CreateIdentityLog) {
            return context.serialize(src, CreateIdentityLog.class);
        } else if (src instanceof CreateRoomLog) {
            return context.serialize(src, CreateRoomLog.class);
        } else if (src instanceof DeleteIdentityLog) {
            return context.serialize(src, DeleteIdentityLog.class);
        } else if (src instanceof DeleteRoomLog) {
            return context.serialize(src, DeleteRoomLog.class);
        } else {
            return context.serialize(src);
        }
    }
}
