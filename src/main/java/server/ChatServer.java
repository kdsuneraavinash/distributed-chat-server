package server;

import server.component.client.ClientComponent;
import server.component.client.ClientManager;
import server.component.gossip.GossipComponent;
import server.component.raft.RaftComponent;

import java.io.IOException;

public class ChatServer implements AutoCloseable {
    // Threads
    private final Thread clientComponentThread;
    private final Thread gossipComponentThread;
    private final Thread raftComponentThread;
    // Managers
    private final ClientManager clientManager;

    public ChatServer(int port) {
        this.clientManager = new ClientManager();
        // Components
        ClientComponent clientComponent = new ClientComponent(port, clientManager);
        GossipComponent gossipComponent = new GossipComponent(port);
        RaftComponent raftComponent = new RaftComponent(port);
        this.clientComponentThread = new Thread(clientComponent);
        this.gossipComponentThread = new Thread(gossipComponent);
        this.raftComponentThread = new Thread(raftComponent);
    }

    public void startListening() {
        try {
            // Start component threads
            raftComponentThread.start();
            gossipComponentThread.start();
            clientComponentThread.start();
            // Wait until all components exit
            clientComponentThread.join();
            gossipComponentThread.join();
            raftComponentThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws InterruptedException, IOException {
        clientComponentThread.interrupt();
        clientComponentThread.join();
        gossipComponentThread.interrupt();
        gossipComponentThread.join();
        raftComponentThread.interrupt();
        raftComponentThread.join();
        clientManager.close();
    }
}
