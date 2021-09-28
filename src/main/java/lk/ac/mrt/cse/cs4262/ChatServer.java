package lk.ac.mrt.cse.cs4262;

import lombok.extern.log4j.Log4j2;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lk.ac.mrt.cse.cs4262.components.client.ClientComponent;
import lk.ac.mrt.cse.cs4262.components.gossip.GossipComponent;
import lk.ac.mrt.cse.cs4262.components.raft.RaftComponent;

/**
 * Chat lk.ac.mrt.cse.cs4262.server main entry class. Contains of three primary components;
 * {@link ClientComponent}, {@link GossipComponent} and {@link RaftComponent}.
 */
@Log4j2
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
        // Threads
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
        // Interrupt all threads
        clientComponentThread.interrupt();
        gossipComponentThread.interrupt();
        raftComponentThread.interrupt();
        // Wait until threads exit
        clientComponentThread.join();
        gossipComponentThread.join();
        raftComponentThread.join();
        // Close each component resources
        raftComponent.close();
        gossipComponent.close();
        clientComponent.close();
    }
}
