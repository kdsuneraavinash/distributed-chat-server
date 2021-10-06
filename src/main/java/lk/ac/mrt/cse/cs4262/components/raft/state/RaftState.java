package lk.ac.mrt.cse.cs4262.components.raft.state;

import lk.ac.mrt.cse.cs4262.ServerConfiguration;


/**
 * The State containing the primary system state.
 * Will contain all the servers, participants and rooms.
 * This is only updated via log entries and will be persisted.
 */
public interface RaftState extends RaftStateReadView {
    /**
     * Initializes state by applying all persisted logs.
     *
     * @param serverConfiguration System configuration.
     */
    void initialize(ServerConfiguration serverConfiguration);

    /**
     * Adds a log to the state. This can alter the state.
     * Any changes done to the state will be persisted.
     *
     * @param logEntry Log entry to apply to the system.
     */
    void commit(RaftLog logEntry);


    /*
    ========================================================
    Leader Election
    ========================================================
     */

    /**
     * Status of Leader Election.
     */
    enum LeaderElectionStatus {
        ON_GOING,
        CANCELLED,
        COMPLETED;
    }

    /**
     * Get current leader election status.
     *
     * @return current leaderElectionStatus
     */
    LeaderElectionStatus getLeaderElectionStatus();

    /**
     * Set LeaderElectionStatus to "ON_GOING".
     */
    void startLeaderElection();

    /**
     * Set LeaderElectionStatus to "CANCELLED".
     */
    void cancelLeaderElection();

    /**
     * Set LeaderElectionStatus to "COMPLETED".
     */
    void completeLeaderElection();

    /**
     * Get current term number.
     *
     * @return currentTermNumber
     */
    int getTerm();

    /**
     * Increment current term number by 1.
     *
     * @return currentTermNumber+1
     */
    int incrementTerm();

    /**
     * Node Type of the servers.
     */
    enum NodeType {
        CANDIDATE,
        FOLLOWER,
        LEADER,
    }

    /**
     * Get current server NodeType.
     *
     * @return NodeType
     */
    NodeType getNodeType();

    /**
     * set current server to Leader NodeType.
     */
    void setToLeader();

    /**
     * set current server to Follower NodeType.
     */
    void setToFollower();

    /**
     * set current server to Candidate NodeType.
     */
    void setToCandidate();

    /**
     * Get last log index.
     *
     * @return lastLogIndex
     */
    int getLastLogIndex();

    /**
     * Get last log term.
     *
     * @return lastLogIndex
     */
    int getLastLogTerm();
}
