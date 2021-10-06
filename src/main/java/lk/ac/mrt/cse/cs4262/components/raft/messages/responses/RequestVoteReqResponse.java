package lk.ac.mrt.cse.cs4262.components.raft.messages.responses;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
public class RequestVoteReqResponse extends BaseRaftResponse {
    @SerializedName("candidate_id")
    @Nullable
    private String candidateId;

    @SerializedName("last_log_index")
    private int lastLogIndex;

    @SerializedName("last_log_term")
    private int lastLogTerm;
}
