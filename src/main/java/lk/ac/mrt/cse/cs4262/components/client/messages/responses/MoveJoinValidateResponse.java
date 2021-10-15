package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.utils.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MoveJoinValidateResponse {
    @JsonAdapter(ToStringSerializer.class)
    @SerializedName("movejoinvalid")
    private boolean validated;
}
