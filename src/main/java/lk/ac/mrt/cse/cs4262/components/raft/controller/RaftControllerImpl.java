package lk.ac.mrt.cse.cs4262.components.raft.controller;

import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.RaftComponent;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.AppendReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.AppendRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.BaseRaftMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.CommandRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.VoteReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.VoteRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftState;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.BaseLog;
import lk.ac.mrt.cse.cs4262.components.raft.timeouts.ElectionTimeoutInvoker;
import lk.ac.mrt.cse.cs4262.components.raft.timeouts.RpcTimeoutInvoker;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

@Log4j2
public class RaftControllerImpl implements RaftController {
    private final ServerId currentServerId;
    private final RaftState raftState;
    private final ServerConfiguration serverConfiguration;
    private final ElectionTimeoutInvoker electionTimeoutInvoker;
    private final RpcTimeoutInvoker rpcTimeoutInvoker;
    @Nullable
    private RaftMessageSender raftMessageSender;

    /**
     * Create a raft component. See {@link RaftComponent}.
     *
     * @param currentServerId     Current server id.
     * @param raftState           System global state (write view).
     * @param serverConfiguration All server configuration.
     */
    public RaftControllerImpl(ServerId currentServerId, RaftState raftState,
                              ServerConfiguration serverConfiguration) {
        this.currentServerId = currentServerId;
        this.raftState = raftState;
        this.serverConfiguration = serverConfiguration;
        this.electionTimeoutInvoker = new ElectionTimeoutInvoker();
        this.rpcTimeoutInvoker = new RpcTimeoutInvoker();
    }

    /**
     * Attach a message sender to this controller.
     *
     * @param messageSender Message Sender.
     */
    public void attachMessageSender(RaftMessageSender messageSender) {
        this.raftMessageSender = messageSender;
    }

    @Override
    public void initialize() {
        log.info("raft controller initialized");
        this.electionTimeoutInvoker.initialize(this);
        this.rpcTimeoutInvoker.initialize(this);
        // Start election timeout
        this.electionTimeoutInvoker.setTimeout(0);
    }

    @Override
    public void handleElectionTimeout() {
        log.info("election timeout");
        electionTimeoutInvoker.setTimeout(1000);
        // TODO: Implement (Slide 26)
    }

    @Override
    public void handleRpcTimeout(ServerId serverId) {
        log.info("rpc timeout");
        rpcTimeoutInvoker.setTimeout(serverId, 1000);
        // TODO: Implement (Slide 44)
    }

    @Override
    public void handleVoteRequest(VoteRequestMessage request) {
        // TODO: Implement (Slide 45)
    }

    @Override
    public void handleVoteReply(VoteReplyMessage request) {
        // TODO: Implement (Slide 29)
    }

    @Override
    public void handleCommandRequest(CommandRequestMessage request) {
        // TODO: Implement (Slide 34)
    }

    @Override
    public void handleAppendRequest(AppendRequestMessage request) {
        // TODO: Implement (Slide 40)
    }

    @Override
    public void handleAppendReply(AppendReplyMessage request) {
        // TODO: Implement (Slide 51)
    }

    @Override
    public void stepDown(int term) {
        // TODO: Implement (Slide 30)
    }

    @Override
    public void sendAppendEntries(ServerId serverId) {
        // TODO: Implement (Slide 37)
    }

    @Override
    public void storeEntries(int prevIndex, List<RaftLog> entries, int c) {
        // TODO: Implement (Slide 53)
    }

    @Override
    public void close() throws Exception {
        electionTimeoutInvoker.close();
        rpcTimeoutInvoker.close();
    }

    /*
    ========================================================
    MESSAGE SENDERS
    ========================================================
    */

    private void sendVoteRequest(ServerId toServerId, int term, int lastLogTerm, int lastLogIndex) {
        sendToServer(toServerId, VoteRequestMessage.builder()
                .senderId(currentServerId)
                .term(term)
                .lastLogTerm(lastLogTerm)
                .lastLogIndex(lastLogIndex).build());
    }

    private void sendVoteReply(ServerId toServerId, int term, ServerId vote) {
        sendToServer(toServerId, VoteReplyMessage.builder()
                .senderId(currentServerId)
                .term(term)
                .vote(vote).build());
    }

    private void sendCommandRequest(ServerId toServerId, BaseLog command) {
        sendToServer(toServerId, CommandRequestMessage.builder()
                .senderId(currentServerId)
                .command(command).build());
    }

    private void sendAppendRequest(ServerId toServerId, int term, int prevIndex, int prevTerm,
                                   List<RaftLog> entries, int commitIndex) {
        sendToServer(toServerId, AppendRequestMessage.builder()
                .senderId(currentServerId)
                .term(term)
                .prevIndex(prevIndex)
                .prevTerm(prevTerm)
                .entries(entries)
                .commitIndex(commitIndex).build());
    }

    private void sendAppendRequest(ServerId toServerId, int term, boolean success, int index) {
        sendToServer(toServerId, AppendReplyMessage.builder()
                .senderId(currentServerId)
                .term(term)
                .success(success)
                .index(index).build());
    }

    private void sendToServer(ServerId toServerId, BaseRaftMessage message) {
        if (raftMessageSender != null) {
            raftMessageSender.sendToServer(toServerId, message);
        }
    }
}
