package lk.ac.mrt.cse.cs4262.components.raft.messages.responses;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class RequestVoteResResponse extends BaseRaftResponse {
    @SerializedName("vote_granted")
    private boolean voteGranted;
}
