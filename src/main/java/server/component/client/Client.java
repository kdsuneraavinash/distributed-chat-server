package server.component.client;

import java.net.Socket;

public class Client {
    private final Socket socket;

    public Client(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public String toString() {
        return "Client{" + socket.getPort() + "}";
    }
}
