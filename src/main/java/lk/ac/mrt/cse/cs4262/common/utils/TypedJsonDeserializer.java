package lk.ac.mrt.cse.cs4262.common.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Base class for json deserializers that change the type based on a type field.
 *
 * @param <T> Base class with the common fields. This should have a field named {@code typeFieldName}.
 */
public abstract class TypedJsonDeserializer<T> implements JsonDeserializer<T> {
    private final String typeFieldName;

    /**
     * See {@link TypedJsonDeserializer}.
     *
     * @param typeFieldName Name of the field to use to change deserializer.
     */
    protected TypedJsonDeserializer(String typeFieldName) {
        this.typeFieldName = typeFieldName;
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonElement jsonType = jsonObject.get(typeFieldName);
        String type = jsonType.getAsString();
        return context.deserialize(json, mapTypeToClass(type));
    }

    /**
     * @param type Type field value of the json.
     * @return The class to use for deserialization.
     * @throws JsonParseException If type is unknown or if parsing failed.
     */
    protected abstract Class<?> mapTypeToClass(String type) throws JsonParseException;
}
