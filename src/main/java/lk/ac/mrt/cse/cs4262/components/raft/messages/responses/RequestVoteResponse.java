package lk.ac.mrt.cse.cs4262.components.raft.messages.responses;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class RequestVoteResponse extends BaseRaftResponse {
    private final String action = "REQUEST_VOTE_RES";

    @SerializedName("vote_granted")
    private boolean voteGranted;
}
