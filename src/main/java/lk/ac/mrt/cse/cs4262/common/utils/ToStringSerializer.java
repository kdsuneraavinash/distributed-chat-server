package lk.ac.mrt.cse.cs4262.common.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * A helper serializer to convert to strings.
 */
public class ToStringSerializer implements JsonSerializer<Object> {
    @Override
    public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(String.valueOf(src), String.class);
    }
}
