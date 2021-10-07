package lk.ac.mrt.cse.cs4262.common.tcp.server.shared;

import lk.ac.mrt.cse.cs4262.common.tcp.server.TcpRequestHandler;
import lk.ac.mrt.cse.cs4262.common.tcp.server.TcpServer;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Simple TCP server that accepts a connection and responds.
 * Socket is closed after responding.
 * Many listeners can connect to this server.
 */
public class SharedTcpServer implements Runnable {
    private final TcpServer tcpServer;
    private final RequestHandler requestHandler;

    /**
     * See {@link TcpServer}.
     *
     * @param port    Port to run.
     * @param timeout Timeout for the socket server. (milliseconds)
     */
    public SharedTcpServer(int port, int timeout) {
        this.requestHandler = new RequestHandler();
        this.tcpServer = new TcpServer(port, timeout, requestHandler);
    }

    /**
     * Attach a request handler.
     *
     * @param sharedTcpRequestHandler Request Handler.
     */
    public void attachRequestHandler(SharedTcpRequestHandler sharedTcpRequestHandler) {
        requestHandler.requestHandlers.add(sharedTcpRequestHandler);
    }

    @Override
    public void run() {
        tcpServer.run();
    }

    /**
     * The inner class to handle requests.
     * Will call each request handler until one responds.
     * If no one responds, empty response will be sent.
     */
    @Log4j2
    private static final class RequestHandler implements TcpRequestHandler {
        private final List<SharedTcpRequestHandler> requestHandlers;

        private RequestHandler() {
            this.requestHandlers = new ArrayList<>();
        }

        @Override
        public String handleRequest(String request) {
            for (SharedTcpRequestHandler handler : requestHandlers) {
                try {
                    Optional<String> response = handler.handleRequest(request);
                    if (response.isPresent()) {
                        return response.get();
                    }
                } catch (Exception e) {
                    // On any kind of exception that is not handled,
                    // Log it and respond with error message
                    log.throwing(e);
                    return "error: unhandled exception";
                }
            }
            return "error: not handled";
        }
    }
}
