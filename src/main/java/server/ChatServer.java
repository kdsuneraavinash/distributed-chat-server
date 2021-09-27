package server;

import lombok.extern.java.Log;
import server.components.ServerComponent;
import server.components.client.ClientComponent;
import server.components.gossip.GossipComponent;
import server.components.raft.RaftComponent;

@Log
public class ChatServer implements AutoCloseable {
    // Components
    private final ServerComponent clientComponent;
    private final ServerComponent gossipComponent;
    private final ServerComponent raftComponent;
    // Threads
    private final Thread clientComponentThread;
    private final Thread gossipComponentThread;
    private final Thread raftComponentThread;

    public ChatServer(int port) {
        // Components
        this.clientComponent = new ClientComponent(port);
        this.gossipComponent = new GossipComponent(port);
        this.raftComponent = new RaftComponent(port);
        this.clientComponentThread = new Thread(clientComponent);
        this.gossipComponentThread = new Thread(gossipComponent);
        this.raftComponentThread = new Thread(raftComponent);
    }

    public void startListening() throws InterruptedException {
        // Start component threads
        raftComponentThread.start();
        gossipComponentThread.start();
        clientComponentThread.start();
        // Wait until all components exit
        clientComponentThread.join();
        gossipComponentThread.join();
        raftComponentThread.join();
    }

    @Override
    public void close() throws Exception {
        clientComponentThread.interrupt();
        clientComponentThread.join();
        gossipComponentThread.interrupt();
        gossipComponentThread.join();
        raftComponentThread.interrupt();
        raftComponentThread.join();
        raftComponent.close();
        gossipComponent.close();
        clientComponent.close();
    }
}
