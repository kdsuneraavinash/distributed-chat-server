package lk.ac.mrt.cse.cs4262.components.raft.controller;

import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.AppendReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.AppendRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.BaseRaftMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.CommandRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.VoteReplyMessage;
import lk.ac.mrt.cse.cs4262.components.raft.messages.VoteRequestMessage;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftState;
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.BaseLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.protocol.NodeState;
import lk.ac.mrt.cse.cs4262.components.raft.timeouts.ElectionTimeoutInvoker;
import lk.ac.mrt.cse.cs4262.components.raft.timeouts.RpcTimeoutInvoker;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Raft controller implementation. See {@link RaftController}.
 */
@Log4j2
public class RaftControllerImpl implements RaftController {
    private final ServerId currentServerId;
    private final RaftState raftState;
    private final ServerConfiguration serverConfiguration;
    private final ElectionTimeoutInvoker electionTimeoutInvoker;
    private final RpcTimeoutInvoker rpcTimeoutInvoker;
    @Nullable
    private RaftMessageSender raftMessageSender;

    private final List<ServerId> votes;

    /**
     * Create a raft controller. See {@link RaftControllerImpl}.
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

        this.votes = new ArrayList<>();
    }

    @Override
    public void attachMessageSender(RaftMessageSender messageSender) {
        this.raftMessageSender = messageSender;
    }

    @Override
    public void initialize() {
        log.info("raft controller initialized");
        this.electionTimeoutInvoker.attachController(this);
        this.rpcTimeoutInvoker.attachController(this);
        // Start election timeout
        this.electionTimeoutInvoker.setTimeout(0);
    }

    @Override
    @Synchronized
    public void handleElectionTimeout() {
        // Slide 26
        if (NodeState.FOLLOWER.equals(raftState.getState()) || NodeState.CANDIDATE.equals(raftState.getState())) {
            log.info("handleElectionTimeout");

            int t = (int) (Math.random() + 1) * T_DELTA_ELECTION_MS;
            electionTimeoutInvoker.setTimeout(t);

            raftState.setCurrentTerm(raftState.getCurrentTerm() + 1);
            raftState.setState(NodeState.CANDIDATE);
            raftState.setVotedFor(currentServerId);

            votes.clear();
            votes.add(currentServerId);

            serverConfiguration.allServerIds().forEach(serverId -> {
                if (!serverId.equals(currentServerId)) {
                    rpcTimeoutInvoker.cancelTimeout(serverId);
                    rpcTimeoutInvoker.setTimeout(serverId, 0);
                }
            });
        }
    }

    @Override
    @Synchronized
    public void handleRpcTimeout(ServerId serverId) {
        // Slide 44
        log.info("handleRpcTimeout for serverId={}", serverId);

        NodeState state = raftState.getState();
        if (NodeState.CANDIDATE.equals(state)) {
            rpcTimeoutInvoker.setTimeout(serverId, T_DELTA_VOTE_MS);
            sendVoteRequest(
                    serverId,
                    raftState.getCurrentTerm(),
                    raftState.getLastLogTerm(),
                    raftState.getLastLogIndex());

        } else if (NodeState.LEADER.equals(state)) {
            rpcTimeoutInvoker.setTimeout(serverId, T_DELTA_ELECTION_MS / 2);
            sendAppendEntries(serverId);
        }

    }

    @Override
    @Synchronized
    public void handleVoteRequest(VoteRequestMessage request) {
        // Slide 45
        log.info("handleVoteRequest request={}", request);

        ServerId q = request.getSenderId();
        int term = request.getTerm();
        int currentTerm = raftState.getCurrentTerm();

        if (term > currentTerm) {
            stepDown(term);
        }

        Optional<ServerId> votedFor = raftState.getVotedFor();

        if (term == currentTerm && (votedFor.isEmpty() || votedFor.get().equals(q))) {
            int lastLogTerm = request.getLastLogTerm();
            int lastLogIndex = request.getLastLogIndex();

            if (lastLogTerm > raftState.getLastLogTerm()
                    || (lastLogTerm == raftState.getLastLogTerm() && lastLogIndex >= raftState.getLastLogIndex())) {
                raftState.setVotedFor(q);

                int t = (int) (Math.random() + 1) * T_DELTA_ELECTION_MS;
                electionTimeoutInvoker.setTimeout(t);

                sendVoteReply(q, term, raftState.getVotedFor().orElseThrow());
            }
        }
    }

    @Override
    @Synchronized
    public void handleVoteReply(VoteReplyMessage request) {
        // Slide 29
        log.info("handleVoteReply request={}", request);

        ServerId q = request.getSenderId();
        int term = request.getTerm();
        int currentTerm = raftState.getCurrentTerm();

        if (term > currentTerm) {
            stepDown(term);

        } else if (term == currentTerm && NodeState.CANDIDATE.equals(raftState.getState())) {
            if (request.getVote().equals(currentServerId)) {
                votes.add(q);
            }

            rpcTimeoutInvoker.cancelTimeout(q);

            int minVotes = serverConfiguration.allServerIds().size() / 2;
            log.info("votes {}/{}", votes.size(), minVotes + 1);
            if (votes.size() > minVotes) {
                raftState.setState(NodeState.LEADER);
                raftState.setLeaderId(currentServerId);
                log.info("set Leader={}", currentServerId);

                serverConfiguration.allServerIds().forEach(serverId -> {
                    if (!serverId.equals(currentServerId)) {
                        sendAppendEntries(serverId);
                    }
                });
            }
        }

    }

    @Override
    @Synchronized
    public void handleCommandRequest(CommandRequestMessage request) {
        // slide 34
        log.info("handleCommandRequest request={}", request);
        log.info(request);

        if (NodeState.LEADER.equals(raftState.getState())) {
            raftState.addLogEntry(new RaftLog(request.getCommand(), raftState.getCurrentTerm()));
            serverConfiguration.allServerIds().forEach(serverId -> {
                if (serverId != currentServerId) {
                    sendAppendEntries(serverId);
                }
            });
        }
    }

    @Override
    @Synchronized
    public void handleAppendRequest(AppendRequestMessage request) {
        // Slide 40
        log.info("handleAppendRequest request={}", request);

        ServerId q = request.getSenderId();
        int term = request.getTerm();
        int currentTerm = raftState.getCurrentTerm();

        if (term > currentTerm) {
            stepDown(term);

        }
        // TODO: Implement slide 40

    }

    @Override
    @Synchronized
    public void handleAppendReply(AppendReplyMessage request) {
        log.info("handleAppendReply request={}", request);
        // TODO: Implement (Slide 51)
    }

    @Override
    @Synchronized
    public void stepDown(int term) {
        // Slide 30
        log.info("stepDown term={}", term);

        raftState.setCurrentTerm(term);
        raftState.setState(NodeState.FOLLOWER);
        raftState.setVotedFor(null);

        int t = (int) (Math.random() + 1) * T_DELTA_ELECTION_MS;
        electionTimeoutInvoker.setTimeout(t);
    }

    @Override
    public void sendAppendEntries(ServerId serverId) {
        log.info("sendAppendEntries serverId={}", serverId);
        // TODO: Implement (Slide 37)
        rpcTimeoutInvoker.setTimeout(serverId, T_DELTA_ELECTION_MS / 2);
        // TODO: ?
        int lastLogIndex = Math.min(raftState.getNextIndex(serverId), raftState.getLastLogIndex());
        raftState.setNextIndex(serverId, lastLogIndex);

        sendAppendRequest(
                serverId,
                raftState.getCurrentTerm(),
                0,
                0,
                List.of(),
                raftState.getCommitIndex());
    }

    @Override
    public void storeEntries(int prevIndex, List<RaftLog> entries, int minCommittedIndex) {
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

    private void sendAppendReply(ServerId toServerId, int term, boolean success, int index) {
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
