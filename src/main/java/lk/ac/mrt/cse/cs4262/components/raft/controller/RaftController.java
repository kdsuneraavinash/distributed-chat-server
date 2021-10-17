package lk.ac.mrt.cse.cs4262.components.raft.controller;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.AppendReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.AppendRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.CommandRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.VoteReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.VoteRequestMessage;

/**
 * Controller for Raft logic.
 */
public interface RaftController extends AutoCloseable {
    /**
     * Time between elections.
     */
    int T_DELTA_ELECTION_MS = 500;
    /**
     * Vote round duration.
     */
    int T_DELTA_VOTE_MS = 250;

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
     * <p>
     * Implementation: Slide 26
     */
    void handleElectionTimeout();

    /**
     * Some RPC call has timed out without acton coming to a close.
     * <p>
     * Implementation: Slide 44
     *
     * @param serverId Server ID for which the RPC call was done.
     */
    void handleRpcTimeout(ServerId serverId);

    /**
     * Handles Request Vote REQ call.
     * <p>
     * Implementation: Slide 45
     *
     * @param request Request.
     */
    void handleVoteRequest(VoteRequestMessage request);

    /**
     * Handles Request Vote REP call.
     * <p>
     * Implementation: Slide 29
     *
     * @param request Request.
     */
    void handleVoteReply(VoteReplyMessage request);

    /**
     * Handles Command call.
     * Typically, done to the leader.
     * <p>
     * Implementation: Slide 34
     *
     * @param request Request.
     * @return Whether command was accepted.
     */
    boolean handleCommandRequest(CommandRequestMessage request);

    /**
     * Handles Append Entries REQ call.
     * <p>
     * Implementation: Slide 40
     *
     * @param request Request.
     */
    void handleAppendRequest(AppendRequestMessage request);

    /**
     * Handles Append Entries REP call.
     * <p>
     * Implementation: Slide 51
     *
     * @param request Request.
     */
    void handleAppendReply(AppendReplyMessage request);
}
