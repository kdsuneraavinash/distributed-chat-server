package lk.ac.mrt.cse.cs4262;

import lk.ac.mrt.cse.cs4262.common.state.SystemState;
import lk.ac.mrt.cse.cs4262.common.state.SystemStateImpl;
import lk.ac.mrt.cse.cs4262.components.client.ClientComponent;
import lk.ac.mrt.cse.cs4262.components.gossip.GossipComponent;
import lk.ac.mrt.cse.cs4262.components.raft.RaftComponent;
import lombok.extern.log4j.Log4j2;

/**
 * Chat server main entry class. Contains of three primary components;
 * {@link ClientComponent}, {@link GossipComponent} and {@link RaftComponent}.
 */
@Log4j2
public class ChatServer implements AutoCloseable {
    // Components
    private final ClientComponent clientComponent;
    private final GossipComponent gossipComponent;
    private final RaftComponent raftComponent;
    // Threads
    private final Thread clientComponentThread;
    private final Thread gossipComponentThread;
    private final Thread raftComponentThread;

    /**
     * Creates a chat server. See {@link ChatServer}.
     *
     * @param port Port to operate.
     */
    public ChatServer(int port) {
        SystemState systemState = new SystemStateImpl();
        // Components
        this.clientComponent = new ClientComponent(port, systemState);
        this.gossipComponent = new GossipComponent();
        this.raftComponent = new RaftComponent();
        // Threads
        this.clientComponentThread = new Thread(clientComponent);
        this.gossipComponentThread = new Thread(gossipComponent);
        this.raftComponentThread = new Thread(raftComponent);
    }

    /**
     * Start all the components and listen for the clients.
     *
     * @throws InterruptedException If listening is interrupted.
     */
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
