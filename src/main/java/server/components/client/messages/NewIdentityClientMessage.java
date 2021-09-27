package server.components.client.messages;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class NewIdentityClientMessage extends BaseClientMessage {
    @SerializedName("identity")
    private String identity;
}
