package lk.ac.mrt.cse.cs4262.components.raft.state.logs;

import com.google.gson.annotations.JsonAdapter;
import lk.ac.mrt.cse.cs4262.components.raft.deserializers.RaftLogDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * A base class for all the logs for the system state.
 */
@Getter
@ToString
@AllArgsConstructor
@JsonAdapter(RaftLogDeserializer.class)
public abstract class BaseLog {
    /**
     * Log type for Create Identity operation.
     */
    public static final String CREATE_IDENTITY_LOG = "CREATE_IDENTITY_LOG";
    /**
     * Log type for Create Room operation.
     */
    public static final String CREATE_ROOM_LOG = "CREATE_ROOM_LOG";
    /**
     * Log type for Delete Identity operation.
     */
    public static final String DELETE_IDENTITY_LOG = "DELETE_IDENTITY_LOG";
    /**
     * Log type for Delete Room operation.
     */
    public static final String DELETE_ROOM_LOG = "DELETE_ROOM_LOG";

    /**
     * Log type for server change log.
     */
    public static final String SERVER_CHANGE_LOG = "SERVER_CHANGE_LOG";

    /**
     * Log type for no-op log.
     */
    public static final String NO_OP_LOG = "NO_OP_LOG";

    private final String type;
}
