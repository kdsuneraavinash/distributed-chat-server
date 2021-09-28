package lk.ac.mrt.cse.cs4262.components.client.messages.requests;

import com.google.gson.annotations.JsonAdapter;
import lk.ac.mrt.cse.cs4262.components.client.messages.ClientMessageDeserializer;
import lombok.Getter;

@Getter
@JsonAdapter(ClientMessageDeserializer.class)
public class BaseClientRequest {
    private String type;
}
