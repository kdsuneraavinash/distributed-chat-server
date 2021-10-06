package lk.ac.mrt.cse.cs4262.components.raft.messages;

public final class MessageType {
    public static final String REQUEST_VOTE_REQ = "REQUEST_VOTE_REQ";
    public static final String REQUEST_VOTE_REP = "REQUEST_VOTE_REP";
    public static final String COMMAND_REQ = "COMMAND_REQ";
    public static final String APPEND_ENTRIES_REQ = "APPEND_ENTRIES_REQ";
    public static final String APPEND_ENTRIES_REP = "APPEND_ENTRIES_REP";

    private MessageType() {
    }
}
