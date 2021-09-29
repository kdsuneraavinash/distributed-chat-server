package lk.ac.mrt.cse.cs4262.components.client.messages.requests;

import com.google.gson.annotations.JsonAdapter;
import lk.ac.mrt.cse.cs4262.components.client.messages.ClientMessageDeserializer;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
@JsonAdapter(ClientMessageDeserializer.class)
public class BaseClientRequest {
    @Nullable
    private String type;
}
