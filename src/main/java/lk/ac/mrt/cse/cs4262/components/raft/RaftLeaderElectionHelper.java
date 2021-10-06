package lk.ac.mrt.cse.cs4262.components.raft;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lk.ac.mrt.cse.cs4262.ServerConfiguration;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.tcp.TcpClient;
import lk.ac.mrt.cse.cs4262.components.raft.messages.requests.RequestVoteReqRequest;
import lk.ac.mrt.cse.cs4262.components.raft.messages.responses.RequestVoteResResponse;
import lk.ac.mrt.cse.cs4262.components.raft.state.RaftState;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class RaftLeaderElectionHelper {
    private final ServerId currentServerId;
    private final ServerConfiguration serverConfiguration;
    private final RaftState raftState;
    private final Gson serializer = new Gson();

    RaftLeaderElectionHelper(ServerId currentServerId, ServerConfiguration serverConfiguration, RaftState raftState) {
        this.currentServerId = currentServerId;
        this.serverConfiguration = serverConfiguration;
        this.raftState = raftState;
    }

    private void startElection() {
        if (raftState.getLeaderElectionStatus() == RaftState.LeaderElectionStatus.ON_GOING) {
            return;
        }

        raftState.startLeaderElection();
        raftState.setToCandidate();
        int votes = requestVotes();
        if (raftState.getLeaderElectionStatus() == RaftState.LeaderElectionStatus.CANCELLED) {
            raftState.completeLeaderElection();
            return;
        }
        if (hasMajority(votes)) {
            raftState.setToLeader();
            raftState.cancelLeaderElection();
            return;
        }
        startElection();
    }

    private int requestVotes() {
        RequestVoteReqRequest req = RequestVoteReqRequest.builder()
                .term(raftState.incrementTerm())
                .candidateId(currentServerId)
                .lastLogIndex(0)
                .lastLogTerm(0).build();

        Map<ServerId, String> allResponses = sendToAll(req.toString());

        AtomicInteger votes = new AtomicInteger(0);
        allResponses.forEach((serverId, s) -> {
            if (!s.isBlank()) {
                try {
                    RequestVoteResResponse res = serializer.fromJson(s, RequestVoteResResponse.class);
                    if (res.isVoteGranted()) {
                        votes.getAndIncrement();
                    }
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        });
        return votes.get();
    }


    private Map<ServerId, String> sendToAll(String message) {
        Map<ServerId, String> responses = new HashMap<>();
        serverConfiguration.allServerIds().forEach((serverId -> {
            Optional<Integer> coordPort = serverConfiguration.getCoordinationPort(serverId);
            Optional<String> serverAddress = serverConfiguration.getServerAddress(serverId);

            String res = "";
            try {
                res = TcpClient.request(
                        serverAddress.orElseThrow(),
                        coordPort.orElseThrow(),
                        message,
                        RaftComponent.RAFT_REQUEST_TIMEOUT);
            } catch (IOException | NoSuchElementException e) {
                e.printStackTrace();
            }

            responses.put(serverId, res);
        }));
        return responses;
    }

    /**
     * Check if the given number exceeds the minimum threshold to have a majority.
     *
     * @param numVotes number of votes
     * @return if the votes have majority
     */
    public boolean hasMajority(int numVotes) {
        int tot = serverConfiguration.allServerIds().toArray().length;
        return numVotes > tot / 2;
    }
}
