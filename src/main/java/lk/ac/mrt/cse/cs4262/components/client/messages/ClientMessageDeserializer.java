package lk.ac.mrt.cse.cs4262.components.client.messages;

import com.google.gson.JsonParseException;
import lk.ac.mrt.cse.cs4262.common.utils.TypedJsonDeserializer;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.BaseClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.CreateRoomClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.ListClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.MessageClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.NewIdentityClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.WhoClientRequest;

/**
 * A serializer for client messages.
 */
public class ClientMessageDeserializer extends TypedJsonDeserializer<BaseClientRequest> {
    private static final String NEW_IDENTITY_TYPE = "newidentity";
    private static final String LIST_TYPE = "list";
    private static final String MESSAGE_TYPE = "message";
    private static final String WHO_TYPE = "who";
    private static final String CREATE_ROOM_TYPE = "createroom";

    protected ClientMessageDeserializer() {
        // Use field `type` to deserialization.
        super("type");
    }

    @Override
    protected Class<?> mapTypeToClass(String type) throws JsonParseException {
        // TODO: Register other request types
        switch (type) {
            case NEW_IDENTITY_TYPE:
                return NewIdentityClientRequest.class;
            case LIST_TYPE:
                return ListClientRequest.class;
            case MESSAGE_TYPE:
                return MessageClientRequest.class;
            case WHO_TYPE:
                return WhoClientRequest.class;
            case CREATE_ROOM_TYPE:
                return CreateRoomClientRequest.class;
            default:
                throw new JsonParseException("Unknown type: " + type);
        }
    }
}
