package lk.ac.mrt.cse.cs4262.components.gossip;

import lk.ac.mrt.cse.cs4262.common.tcp.server.shared.SharedTcpRequestHandler;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;

import java.util.Optional;

public class GossipComponent implements ServerComponent, SharedTcpRequestHandler {
    @Override
    public Optional<String> handleRequest(String request) {
        if (request.startsWith("G")) {
            return Optional.of("GOSSIP EH?");
        }
        return Optional.empty();
    }
}
