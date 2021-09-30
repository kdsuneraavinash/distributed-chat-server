package lk.ac.mrt.cse.cs4262.common.tcp.server.shared;

import java.util.Optional;

/**
 * Generic interface to handle requests from {@link SharedTcpServer}.
 * Many request handlers will be connected to the tcp server.
 * The first responder will be sent.
 * The responders are considered in first-come-first-serve basis.
 */
public interface SharedTcpRequestHandler {
    /**
     * Handle a request and respond.
     * Return empty response if not handled.
     * If not handled, the next handler will get the request.
     * Otherwise, the response will be sent.
     *
     * @param request Request
     * @return Response, this should be empty is not handled.
     */
    Optional<String> handleRequest(String request);
}
