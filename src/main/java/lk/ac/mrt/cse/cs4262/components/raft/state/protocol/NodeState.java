package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;


/**
 * Node Type of the servers.
 */
public enum NodeState {
    /**
     * Used to used elect to elect a new a leader.
     * Normal operation: 1 leader, N-1 followers.
     */
    CANDIDATE,
    /**
     * Completely passive (issues no RPCs, responds to incoming RPCs).
     */
    FOLLOWER,
    /**
     * Handles all client interactions, log replication.
     * At most 1 viable leader at a time.
     */
    LEADER,
}
