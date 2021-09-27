package server.components.client.messages;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class NewIdentityMessage extends BaseMessage {
    @SerializedName("identity")
    private String identity;
}
