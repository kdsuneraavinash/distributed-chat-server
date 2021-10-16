package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.utils.ToStringSerializer;
import lombok.Builder;
import lombok.ToString;

@ToString
@Builder
public class MoveJoinAcceptedResponse {
    private final String type = "serverchange";

    @JsonAdapter(ToStringSerializer.class)
    private final Boolean approved;

    @SerializedName("serverid")
    private final String serverId;
}
