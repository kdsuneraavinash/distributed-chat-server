package server.component.client;

import java.io.IOException;
import java.util.HashMap;

public class ClientManager implements AutoCloseable {
    private final HashMap<Client, Thread> clients;

    public ClientManager() {
        clients = new HashMap<>();
    }

    public void connect(Client client) {
        ClientListener listener = new ClientListener(client);
        Thread listenerThread = new Thread(listener);
        listenerThread.start();
        clients.put(client, listenerThread);
        System.err.println("Connected client: " + client.toString());
    }

    public void disconnect(Client client) throws InterruptedException {
        clients.get(client).interrupt();
        clients.get(client).join();
        clients.remove(client);
    }

    @Override
    public void close() throws IOException, InterruptedException {
        for (Client client : clients.keySet()) {
            disconnect(client);
        }
    }
}
