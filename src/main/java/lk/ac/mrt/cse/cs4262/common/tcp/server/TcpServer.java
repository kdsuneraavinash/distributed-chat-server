package lk.ac.mrt.cse.cs4262.common.tcp.server;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple TCP server that accepts a connection and responds.
 * Socket is closed after responding.
 */
@Log4j2
public class TcpServer implements Runnable {
    private final int port;
    private final int timeout;
    private final TcpRequestHandler requestHandler;
    private final ExecutorService executorService;

    /**
     * See {@link TcpServer}.
     *
     * @param port           Port to run.
     * @param timeout        Timeout for the socket server. (milliseconds)
     * @param requestHandler Handler for requests.
     */
    public TcpServer(int port, int timeout, TcpRequestHandler requestHandler) {
        this.port = port;
        this.timeout = timeout;
        this.requestHandler = requestHandler;
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        log.info("starting tcp server on port {}", port);
        try {
            @Cleanup ServerSocket serverSocket = new ServerSocket(port);
            while (!Thread.currentThread().isInterrupted()) {
                // Create a new client from each socket connection.
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(timeout);
                this.executorService.submit(new TcpSocketListener(socket, requestHandler));
            }
        } catch (IOException e) {
            log.error("server socket opening failed on port {}.", port);
            log.throwing(e);
        } finally {
            this.executorService.shutdownNow();
        }
    }
}
