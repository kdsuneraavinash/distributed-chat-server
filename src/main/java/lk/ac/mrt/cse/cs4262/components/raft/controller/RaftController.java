package lk.ac.mrt.cse.cs4262.components.raft.controller;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.AppendReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.AppendRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.CommandRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.VoteReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.VoteRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftLog;

import java.util.List;

/**
 * Controller for Raft logic.
 */
public interface RaftController extends AutoCloseable {
    /**
     * Time between elections.
     */
    int T_DELTA_ELECTION_MS = 5000;
    /**
     * Vote round duration.
     */
    int T_DELTA_VOTE_MS = 2000;

    /**
     * Initialize controller and timers.
     */
    void initialize();

    /**
     * Attach a message sender to this controller.
     *
     * @param messageSender Message Sender.
     */
    void attachMessageSender(RaftMessageSender messageSender);

    /**
     * Runs every {@code T_DELTA_ELECTION_MS} milliseconds.
     * If elapses without no RPCs, follower assumes leader has crashed.
     */
    void handleElectionTimeout();

    /**
     * Some RPC call has timed out without acton coming to a close.
     *
     * @param serverId Server ID for which the RPC call was done.
     */
    void handleRpcTimeout(ServerId serverId);

    /**
     * Handles Request Vote REQ call.
     *
     * @param request Request.
     */
    void handleVoteRequest(VoteRequestMessage request);

    /**
     * Handles Request Vote REP call.
     *
     * @param request Request.
     */
    void handleVoteReply(VoteReplyMessage request);

    /**
     * Handles Command call.
     * Typically, done to the leader.
     *
     * @param request Request.
     */
    void handleCommandRequest(CommandRequestMessage request);

    /**
     * Handles Append Entries REQ call.
     *
     * @param request Request.
     */
    void handleAppendRequest(AppendRequestMessage request);

    /**
     * Handles Append Entries REP call.
     *
     * @param request Request.
     */
    void handleAppendReply(AppendReplyMessage request);

    /**
     * Steps down to being a follower.
     *
     * @param term Term sent by the server forcing step down.
     *             This is newer than the current term.
     */
    void stepDown(int term);

    /**
     * Leader appending entries to the followers.
     *
     * @param serverId Server to append entries to.
     */
    void sendAppendEntries(ServerId serverId);

    /**
     * Stores log entries.
     *
     * @param prevIndex         Index to start writing logs.
     * @param entries           Log entries to add.
     * @param minCommittedIndex Committed entry index.
     */
    void storeEntries(int prevIndex, List<RaftLog> entries, int minCommittedIndex);
}
