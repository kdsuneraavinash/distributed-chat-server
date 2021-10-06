package lk.ac.mrt.cse.cs4262.components.raft.messages.responses;

import com.google.gson.annotations.JsonAdapter;
import lk.ac.mrt.cse.cs4262.components.raft.messages.RaftMessageDeserializer;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
@JsonAdapter(RaftMessageDeserializer.class)
public class BaseRaftResponse {
    @Nullable
    private String action;

    private int term;
}
