package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.JsonAdapter;
import lk.ac.mrt.cse.cs4262.common.utils.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class NewIdentityClientResponse {
    private final String type = "newidentity";

    @NonNull
    @JsonAdapter(ToStringSerializer.class)
    private final Boolean approved;
}
