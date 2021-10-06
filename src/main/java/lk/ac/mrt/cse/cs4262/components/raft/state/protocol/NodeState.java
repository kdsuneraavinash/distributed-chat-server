package lk.ac.mrt.cse.cs4262.components.raft.state.protocol;


/**
 * Node Type of the servers.
 */
public enum NodeState {
    CANDIDATE,
    FOLLOWER,
    LEADER,
}
