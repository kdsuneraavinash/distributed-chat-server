package server.components.client.messages;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class NewIdentityClientMessage extends BaseClientMessage {
    @SerializedName("identity")
    private String identity;
}
