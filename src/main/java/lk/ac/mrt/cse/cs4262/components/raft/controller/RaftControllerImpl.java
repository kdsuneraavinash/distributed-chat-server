package lk.ac.mrt.cse.cs4262.components.raft.controller;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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
import lk.ac.mrt.cse.cs4262.components.raft.state.logs.NoOpLog;
import lk.ac.mrt.cse.cs4262.components.raft.state.protocol.NodeState;
import lk.ac.mrt.cse.cs4262.components.raft.timeouts.ElectionTimeoutInvoker;
import lk.ac.mrt.cse.cs4262.components.raft.timeouts.RpcTimeoutInvoker;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

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
    private final Random randomGenerator;

    // List holding votes that this server received
    private final Set<ServerId> votes;
    private final int raftElectionDelayMs;
    @Nullable
    private RaftMessageSender raftMessageSender;

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
        this.randomGenerator = new Random();
        this.votes = new HashSet<>();

        Config configuration = ConfigFactory.load();
        this.raftElectionDelayMs = configuration.getInt("raft.election.delay");
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
        this.electionTimeoutInvoker.setTimeout(raftElectionDelayMs);
    }

    /*
    ========================================================
    TIMEOUTS
    ========================================================
    */

    @Override
    @Synchronized
    public void handleElectionTimeout() {
        // Elections are not held by leaders
        NodeState nodeState = raftState.getState();
        if (NodeState.FOLLOWER.equals(nodeState)
                || NodeState.CANDIDATE.equals(nodeState)) {
            // Decide on next election timeout
            restartElectionTimeout();

            // Change term and appoint as candidate
            raftState.setCurrentTerm(raftState.getCurrentTerm() + 1);
            raftState.setState(NodeState.CANDIDATE);
            log.debug("starting election round and becoming candidate for term {}",
                    raftState.getCurrentTerm());

            // Add my own vote for the election
            votes.clear();
            votes.add(currentServerId);
            raftState.setVotedFor(currentServerId);

            // Start rpc timeout for every server to start voting round
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
        NodeState nodeState = raftState.getState();
        if (NodeState.CANDIDATE.equals(nodeState)) {
            log.trace("sending vote request to {}", serverId);
            // If candidate, start voting round with the server.
            // Timeout for next voting round if current one does not end.
            rpcTimeoutInvoker.setTimeout(serverId, T_DELTA_VOTE_MS);

            // Send vote request asking for the vote.
            int lastLogIndex = raftState.getLogSize();
            int lastLogTerm = raftState.getLogTermOf(lastLogIndex);
            sendVoteRequest(serverId, raftState.getCurrentTerm(), lastLogTerm, lastLogIndex);

        } else if (NodeState.LEADER.equals(nodeState)) {
            log.trace("sending append entries to {}", serverId);
            // Send append entries RPC continuously to maintain leadership.
            rpcTimeoutInvoker.setTimeout(serverId, T_DELTA_ELECTION_MS / 2);
            sendAppendEntries(serverId);
        }
    }

    /*
    ========================================================
    REQUEST HANDLERS
    ========================================================
    */

    @Override
    @Synchronized
    public void handleVoteRequest(VoteRequestMessage request) {
        log.traceEntry("request={}", request);
        ServerId senderId = request.getSenderId();
        int senderTerm = request.getTerm();

        // If sender has a higher term, step down immediately.
        int currentTerm = raftState.getCurrentTerm();
        if (senderTerm > currentTerm) {
            log.trace("stepping down on vote request from {}", senderId);
            stepDown(senderTerm);

        } else if (senderTerm == currentTerm) {
            // If the sender and server has same term, check if
            // vote can be given to the sender.
            Optional<ServerId> myVote = raftState.getVotedFor();
            if (myVote.isEmpty() || myVote.get().equals(senderId)) {
                log.trace("can vote for {}", senderId);
                // If sender has logs that are same or newer, vote.
                int senderLastLogTerm = request.getLastLogTerm();
                int senderLastLogIndex = request.getLastLogIndex();
                int lastLogTerm = raftState.getLogTermOf(raftState.getLogSize());
                if (senderLastLogTerm > lastLogTerm
                        || (senderLastLogTerm == lastLogTerm
                        && senderLastLogIndex >= raftState.getLogSize())) {
                    log.debug("voted for {}", senderId);

                    // Vote for the server.
                    raftState.setVotedFor(senderId);
                    // Restart election timeout.
                    restartElectionTimeout();
                    // Send the vote reply. Here voted for should be set
                    // already by above to be sender id.
                    sendVoteReply(senderId, senderTerm, senderId);
                }
            }
        }
    }

    @Override
    @Synchronized
    public void handleVoteReply(VoteReplyMessage request) {
        log.traceEntry("request={}", request);
        ServerId senderId = request.getSenderId();
        int senderTerm = request.getTerm();

        // If sender has a higher term, step down immediately.
        int currentTerm = raftState.getCurrentTerm();
        if (senderTerm > currentTerm) {
            stepDown(senderTerm);

        } else if (senderTerm == currentTerm) {
            // If the sender and server has same term, check if
            // I am still the leader.
            if (NodeState.CANDIDATE.equals(raftState.getState())) {
                // If the sender voted for me, add their votes to my votes.
                // Also, cancel any RPC call that is due to sender.
                if (request.getVote().equals(currentServerId)) {
                    log.debug("{} voted for me. current votes={}", senderId, votes);
                    votes.add(senderId);
                }
                rpcTimeoutInvoker.cancelTimeout(senderId);

                // Check if minimum required votes is reached.
                int minimumRequiredVotes = serverConfiguration.allServerIds().size() / 2 + 1;
                log.debug("received vote reply. current votes {}/{} for term {}",
                        votes.size(), minimumRequiredVotes, currentTerm);
                if (votes.size() >= minimumRequiredVotes) {
                    // Appoint myself as leader.
                    raftState.setState(NodeState.LEADER);
                    raftState.setLeaderId(currentServerId);

                    // Send append entries to announce my leadership.
                    serverConfiguration.allServerIds().forEach(serverId -> {
                        if (!serverId.equals(currentServerId)) {
                            sendAppendEntries(serverId);
                        }
                    });

                    // Add no-op log
                    // See: https://groups.google.com/g/raft-dev/c/KIozjYuq5m0?pli=1
                    handleCommandRequest(CommandRequestMessage.builder()
                            .command(new NoOpLog()).senderId(currentServerId).build());
                }
            }
        }
    }

    @Override
    @Synchronized
    public boolean handleCommandRequest(CommandRequestMessage request) {
        log.traceEntry("request={}", request);

        if (NodeState.LEADER.equals(raftState.getState())) {
            BaseLog baseLog = request.getCommand();
            if (raftState.isAcceptable(baseLog)) {
                // Add the log in uncommitted state.
                RaftLog uncommittedLogEntry = new RaftLog(request.getCommand(), raftState.getCurrentTerm());
                raftState.appendLogEntry(uncommittedLogEntry);
                raftState.setMatchIndex(currentServerId, raftState.getLogSize());

                // Send append entries to announce the new log.
                serverConfiguration.allServerIds().forEach(serverId -> {
                    if (!serverId.equals(currentServerId)) {
                        sendAppendEntries(serverId);
                    }
                });

                return true;
            }
        }
        return false;
    }

    @Override
    @Synchronized
    public void handleAppendRequest(AppendRequestMessage request) {
        log.traceEntry("request={}", request);
        ServerId senderId = request.getSenderId();
        int senderTerm = request.getTerm();

        // If sender has a higher term, step down immediately.
        int currentTerm = raftState.getCurrentTerm();
        if (senderTerm > currentTerm) {
            stepDown(senderTerm);

        } else if (senderTerm < currentTerm) {
            // If sender has a lower term, inform them of my term.
            // Here index does not matter. So sending -1.
            sendAppendReply(senderId, currentTerm, false, -1);

        } else {
            // Sender is confirmed leader.
            // Logic to set leader. Log for first time.
            if (raftState.getLeaderId().isEmpty()
                    || !senderId.equals(raftState.getLeaderId().get())) {
                raftState.setLeaderId(senderId);
            }

            restartElectionTimeout();

            int prevIndex = request.getPrevIndex();
            int prevTerm = request.getPrevTerm();

            int index = 0;
            // Success check if logs are consistent.
            // Term of the last log should match.
            boolean success = (prevIndex == 0)
                    || (prevIndex <= raftState.getLogSize()
                    && raftState.getLogTermOf(prevIndex) == prevTerm);
            // If logs are consistent, store them
            if (success) {
                index = storeEntries(prevIndex, request.getEntries(), request.getCommitIndex());
            }

            sendAppendReply(senderId, currentTerm, success, index);
        }
    }

    @Override
    @Synchronized
    public void handleAppendReply(AppendReplyMessage request) {
        log.traceEntry("request={}", request);
        ServerId senderId = request.getSenderId();
        int senderTerm = request.getTerm();

        // If sender has a higher term, step down immediately.
        int currentTerm = raftState.getCurrentTerm();
        if (senderTerm > currentTerm) {
            stepDown(senderTerm);

        } else if (senderTerm == currentTerm) {
            // If the term is same and I am the leader, handle.
            NodeState currentState = raftState.getState();
            if (NodeState.LEADER.equals(currentState)) {
                int index = request.getIndex();
                int currentNextIndex = raftState.getNextIndex(senderId);
                if (request.isSuccess()) {
                    // Successfully updated.
                    raftState.setNextIndex(senderId, index + 1);
                    if (index != raftState.getMatchIndex(senderId)) {
                        raftState.setMatchIndex(senderId, index);
                        raftState.performCommitIfNecessary();
                    }
                } else {
                    // If update was not successful, go back one index.
                    // This is to find last successful log eventually.
                    int reducedNextIndex = Math.max(1, currentNextIndex - 1);
                    raftState.setNextIndex(senderId, reducedNextIndex);
                }
                // If there are still entries to send, send them.
                if (currentNextIndex <= raftState.getLogSize()) {
                    sendAppendEntries(senderId);
                }
            }
        }
    }

    /*
    ========================================================
    HELPERS
    ========================================================
    */

    /**
     * Steps down to being a follower.
     * <p>
     * Implementation: Slide 30
     *
     * @param term Term sent by the server forcing step down.
     *             This is newer than the current term.
     */
    private void stepDown(int term) {
        log.debug("stepping down and becoming follower for term={}", term);

        // Update term and set self as follower.
        raftState.setCurrentTerm(term);
        raftState.setState(NodeState.FOLLOWER);
        raftState.setVotedFor(null);
        // Start election timeout for leader.
        restartElectionTimeout();
    }

    /**
     * Leader appending entries to the followers.
     * <p>
     * Implementation: Slide 37
     *
     * @param serverId Server to append entries to.
     */
    private void sendAppendEntries(ServerId serverId) {
        log.traceEntry("serverId={}", serverId);
        // Reset time to send next append entries message.
        rpcTimeoutInvoker.setTimeout(serverId, T_DELTA_ELECTION_MS / 2);

        // TODO: Modify to send more than one log at a time.
        int sendingLogIndex = raftState.getNextIndex(serverId);
        int prevLogIndex = sendingLogIndex - 1;
        int prevLogTerm = raftState.getLogTermOf(prevLogIndex);

        // Sending log is empty if index is too large. (Already replicated remaining)
        List<RaftLog> sendingLogs = sendingLogIndex <= raftState.getLogSize()
                ? List.of(raftState.getLogEntry(sendingLogIndex)) : List.of();

        // Send append request upto specified point.
        sendAppendRequest(serverId, raftState.getCurrentTerm(),
                prevLogIndex, prevLogTerm, sendingLogs, raftState.getCommitIndex());
    }

    /**
     * Stores log senderEntries.
     * <p>
     * Implementation: Slide 53
     *
     * @param prevIndex            Index to start writing logs.
     * @param senderEntries        Log senderEntries to add.
     * @param senderCommittedIndex Committed entry index.
     * @return Added last log index.
     */
    private int storeEntries(int prevIndex, List<RaftLog> senderEntries, int senderCommittedIndex) {
        log.traceEntry("prevIndex={} senderEntries={} senderCommittedIndex={}",
                prevIndex, senderEntries, senderCommittedIndex);

        int index = prevIndex;
        for (RaftLog entry : senderEntries) {
            index = index + 1;
            // If the log is totally new or, even if it is already there but
            // it has a different term, insert one from the senderEntries.
            if (index >= raftState.getLogSize()
                    || raftState.getLogEntry(index).getTerm() != entry.getTerm()) {
                raftState.insertLogEntry(entry, index);
            }
        }
        // Commit
        raftState.setCommitIndex(Math.min(senderCommittedIndex, index));
        return index;
    }

    /**
     * Set next election timeout to be between T to 2T.
     * This is done after receiving append entries message,
     * or vote request from a valid candidate.
     */
    private void restartElectionTimeout() {
        int nextElectionTimeout = randomGenerator.nextInt(T_DELTA_ELECTION_MS) + T_DELTA_ELECTION_MS;
        electionTimeoutInvoker.setTimeout(nextElectionTimeout);
    }

    /*
    ========================================================
    CLOSE METHOD
    ========================================================
    */

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
