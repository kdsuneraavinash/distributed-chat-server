package lk.ac.mrt.cse.cs4262.components.raft.messages;

import com.google.gson.annotations.JsonAdapter;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.deserializers.RaftMessageDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
@JsonAdapter(RaftMessageDeserializer.class)
public class BaseRaftMessage {
    /**
     * Message type of RequestVote RPC (Request).
     */
    public static final String REQUEST_VOTE_REQ = "REQUEST_VOTE_REQ";
    /**
     * Message type of RequestVote RPC (Response).
     */
    public static final String REQUEST_VOTE_REP = "REQUEST_VOTE_REP";
    /**
     * Message type of Command RPC (Request).
     */
    public static final String COMMAND_REQ = "COMMAND_REQ";
    /**
     * Message type of AppendEntries RPC (Request).
     */
    public static final String APPEND_ENTRIES_REQ = "APPEND_ENTRIES_REQ";
    /**
     * Message type of AppendEntries RPC (Response).
     */
    public static final String APPEND_ENTRIES_REP = "APPEND_ENTRIES_REP";

    private final String action;
    private final String senderId;

    /**
     * @return Sender ID.
     */
    public ServerId getSenderId() {
        return new ServerId(senderId);
    }
}
