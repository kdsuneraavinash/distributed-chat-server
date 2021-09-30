package lk.ac.mrt.cse.cs4262.common.tcp.server;

/**
 * Generic interface to handle requests from {@link TcpServer}.
 */
public interface TcpRequestHandler {
    /**
     * Handle a request and respond.
     *
     * @param request Request
     * @return Response
     */
    String handleRequest(String request);
}
