package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.utils.ToStringSerializer;
import lombok.Builder;
import lombok.ToString;

@ToString
@Builder
public class RouteServerClientResponse {
    private final String type = "route";

    @SerializedName("roomid")
    private final RoomId roomId;

    private final String host;

    @JsonAdapter(ToStringSerializer.class)
    private final Integer port;

}
