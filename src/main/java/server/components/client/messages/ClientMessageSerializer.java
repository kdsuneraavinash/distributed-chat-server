package server.components.client.messages;

import com.google.gson.*;
import server.components.client.messages.requests.BaseClientRequest;
import server.components.client.messages.requests.ListClientRequest;
import server.components.client.messages.requests.MessageClientRequest;
import server.components.client.messages.requests.NewIdentityClientRequest;

import java.lang.reflect.Type;

public class ClientMessageSerializer implements JsonDeserializer<BaseClientRequest> {
    @Override
    public BaseClientRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonElement jsonType = jsonObject.get("type");
        String type = jsonType.getAsString();
        return context.deserialize(json, mapTypeToClass(type));
    }

    private Class<?> mapTypeToClass(String type) {
        // TODO: Register other request types
        switch (type) {
            case NewIdentityClientRequest.TYPE:
                return NewIdentityClientRequest.class;
            case ListClientRequest.TYPE:
                return ListClientRequest.class;
            case MessageClientRequest.TYPE:
                return MessageClientRequest.class;
            default:
                throw new JsonParseException("Unknown type: " + type);
        }
    }
}