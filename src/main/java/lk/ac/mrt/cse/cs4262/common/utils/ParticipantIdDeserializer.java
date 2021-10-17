package lk.ac.mrt.cse.cs4262.common.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;

import java.lang.reflect.Type;

public class ParticipantIdDeserializer implements JsonDeserializer<ParticipantId> {
    @Override
    public ParticipantId deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return new ParticipantId(json.getAsString());
    }
}
