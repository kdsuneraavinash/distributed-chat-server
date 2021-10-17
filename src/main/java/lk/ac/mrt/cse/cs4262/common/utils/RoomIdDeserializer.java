package lk.ac.mrt.cse.cs4262.common.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;

import java.lang.reflect.Type;

public class RoomIdDeserializer implements JsonDeserializer<RoomId> {
    @Override
    public RoomId deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return new RoomId(json.getAsString());
    }
}
