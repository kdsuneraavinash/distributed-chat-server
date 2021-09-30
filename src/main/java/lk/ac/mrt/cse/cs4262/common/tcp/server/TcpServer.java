package lk.ac.mrt.cse.cs4262.common.tcp.server;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Simple TCP server that accepts a connection and responds.
 * Socket is closed after responding.
 */
@Log4j2
public class TcpServer implements Runnable {
    private final int port;
    private final TcpRequestHandler requestHandler;

    /**
     * See {@link TcpServer}.
     *
     * @param port           Port to run.
     * @param requestHandler Handler for requests.
     */
    public TcpServer(int port, TcpRequestHandler requestHandler) {
        this.port = port;
        this.requestHandler = requestHandler;
    }

    @Override
    public void run() {
        log.info("starting tcp server on port {}", port);
        try {
            @Cleanup ServerSocket serverSocket = new ServerSocket(port);
            while (!Thread.currentThread().isInterrupted()) {
                // Create a new client from each socket connection.
                Socket socket = serverSocket.accept();
                Thread thread = new Thread(new TcpSocketListener(socket, requestHandler));
                thread.start();
            }
        } catch (IOException e) {
            log.error("server socket opening failed on port {}.", port);
            log.throwing(e);
        }
    }
}
