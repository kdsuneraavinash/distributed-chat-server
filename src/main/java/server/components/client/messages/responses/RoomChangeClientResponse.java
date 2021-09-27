package server.components.client.messages.responses;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RoomChangeClientResponse {
    private final String type = "roomchange";
    private final String identity;
    private final String former;
    @SerializedName("roomid")
    private final String roomId;
}
