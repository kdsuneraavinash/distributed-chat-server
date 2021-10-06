package lk.ac.mrt.cse.cs4262.components.raft.messages.requests;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.ToString;

@ToString
@Builder
public class RequestVoteResRequest extends BaseRaftRequest {
    private final String action = "REQUEST_VOTE_REQ";

    private final int term;

    @SerializedName("vote_granted")
    private final boolean voteGranted;
}
