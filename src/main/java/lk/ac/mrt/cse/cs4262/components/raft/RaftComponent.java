package lk.ac.mrt.cse.cs4262.components.raft;

import lk.ac.mrt.cse.cs4262.common.tcp.server.shared.SharedTcpRequestHandler;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

@Log4j2
public class RaftComponent implements ServerComponent, SharedTcpRequestHandler {
    @Override
    public Optional<String> handleRequest(String request) {
        if (request.startsWith("R")) {
            return Optional.of("WAHHH!!!!");
        }
        return Optional.empty();
    }
}
