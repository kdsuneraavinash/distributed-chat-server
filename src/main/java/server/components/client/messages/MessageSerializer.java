package server.components.client.messages;

import com.google.gson.*;

import java.lang.reflect.Type;

public class MessageSerializer implements JsonDeserializer<BaseMessage> {
    @Override
    public BaseMessage deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject jsonObject = json.getAsJsonObject();
        JsonElement jsonType = jsonObject.get("type");
        String type = jsonType.getAsString();

        if ("newidentity".equals(type)) {
            return context.deserialize(json, NewIdentityMessage.class);
        } else if ("list".equals(type)) {
            return context.deserialize(json, ListMessage.class);
        } else {
            throw new JsonParseException("Unknown type: " + type);
        }
    }
}