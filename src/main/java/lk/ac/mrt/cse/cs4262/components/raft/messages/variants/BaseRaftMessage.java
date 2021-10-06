package lk.ac.mrt.cse.cs4262.components.raft.messages.variants;

import com.google.gson.annotations.JsonAdapter;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.RaftMessageDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
@JsonAdapter(RaftMessageDeserializer.class)
public class BaseRaftMessage {
    private final String action;
    private final String senderId;

    /**
     * @return Sender ID.
     */
    public ServerId getSenderId() {
        return new ServerId(senderId);
    }
}
