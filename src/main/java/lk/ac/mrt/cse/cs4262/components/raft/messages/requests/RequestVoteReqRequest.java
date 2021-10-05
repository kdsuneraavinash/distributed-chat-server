package lk.ac.mrt.cse.cs4262.components.raft.messages.requests;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.utils.ToStringSerializer;
import lombok.Builder;
import lombok.ToString;

@ToString
@Builder
public class RequestVoteReqRequest extends BaseRaftRequest {
    private final String action = "REQUEST_VOTE_REQ";

    private final int term;

    @SerializedName("candidate_id")
    @JsonAdapter(ToStringSerializer.class)
    private final ServerId candidateId;

    @SerializedName("last_log_index")
    private final int lastLogIndex;

    @SerializedName("last_log_term")
    private final int lastLogTerm;
}
