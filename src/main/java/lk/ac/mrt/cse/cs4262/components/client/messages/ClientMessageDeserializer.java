package lk.ac.mrt.cse.cs4262.components.client.messages;

import com.google.gson.JsonParseException;
import lk.ac.mrt.cse.cs4262.common.utils.TypedJsonDeserializer;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.BaseClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.CreateRoomClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.DeleteRoomClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.JoinRoomClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.ListClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.MessageClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.NewIdentityClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.QuitClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.WhoClientRequest;
import lk.ac.mrt.cse.cs4262.components.client.messages.requests.MoveJoinClientRequest;


/**
 * A serializer for client messages.
 */
public class ClientMessageDeserializer extends TypedJsonDeserializer<BaseClientRequest> {
    private static final String NEW_IDENTITY_TYPE = "newidentity";
    private static final String LIST_TYPE = "list";
    private static final String MESSAGE_TYPE = "message";
    private static final String WHO_TYPE = "who";
    private static final String CREATE_ROOM_TYPE = "createroom";
    private static final String DELETE_ROOM_TYPE = "deleteroom";
    private static final String JOIN_ROOM_TYPE = "joinroom";
    private static final String QUIT_ROOM_TYPE = "quit";
    private static final String MOVE_JOIN_TYPE = "movejoin";

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
            case DELETE_ROOM_TYPE:
                return DeleteRoomClientRequest.class;
            case JOIN_ROOM_TYPE:
                return JoinRoomClientRequest.class;
            case QUIT_ROOM_TYPE:
                return QuitClientRequest.class;
            case MOVE_JOIN_TYPE:
                return MoveJoinClientRequest.class;
            default:
                throw new JsonParseException("unknown type: " + type);
        }
    }
}
