package lk.ac.mrt.cse.cs4262.components.raft.controller;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.AppendReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.AppendRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.CommandRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.VoteReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.VoteRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftLog;

import java.util.List;

public interface RaftController extends AutoCloseable {
    void initialize();

    void attachMessageSender(RaftMessageSender messageSender);

    void handleElectionTimeout();

    void handleRpcTimeout(ServerId serverId);

    void handleVoteRequest(VoteRequestMessage request);

    void handleVoteReply(VoteReplyMessage request);

    void handleCommandRequest(CommandRequestMessage request);

    void handleAppendRequest(AppendRequestMessage request);

    void handleAppendReply(AppendReplyMessage request);

    void stepDown(int term);

    void sendAppendEntries(ServerId serverId);

    void storeEntries(int prevIndex, List<RaftLog> entries, int c);
}
