package lk.ac.mrt.cse.cs4262;

import lk.ac.mrt.cse.cs4262.common.state.SystemState;
import lk.ac.mrt.cse.cs4262.common.state.SystemStateImpl;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.tcp.server.shared.SharedTcpServer;
import lk.ac.mrt.cse.cs4262.components.client.ClientComponent;
import lk.ac.mrt.cse.cs4262.components.gossip.GossipComponent;
import lk.ac.mrt.cse.cs4262.components.gossip.state.GossipState;
import lk.ac.mrt.cse.cs4262.components.gossip.state.GossipStateImpl;
import lk.ac.mrt.cse.cs4262.components.raft.RaftComponent;
import lombok.extern.log4j.Log4j2;

/**
 * Chat server main entry class. Contains of three primary components;
 * {@link ClientComponent}, {@link GossipComponent} and {@link RaftComponent}.
 */
@Log4j2
public class ChatServer implements AutoCloseable {
    // Coordination server
    private final SharedTcpServer coordinationServer;
    // Components
    private final ClientComponent clientComponent;
    private final GossipComponent gossipComponent;
    private final RaftComponent raftComponent;
    // Threads
    private final Thread coordinationServerThread;
    private final Thread clientComponentThread;

    /**
     * Creates a chat server. See {@link ChatServer}.
     *
     * @param currentServerId     Current Server ID.
     * @param serverConfiguration Server configuration obj.
     */
    public ChatServer(ServerId currentServerId, ServerConfiguration serverConfiguration) {
        log.info("starting server {}", currentServerId);
        log.trace("configuration: {}", serverConfiguration);
        int clientPort = serverConfiguration.getClientPort(currentServerId).orElseThrow();
        int coordinationPort = serverConfiguration.getCoordinationPort(currentServerId).orElseThrow();

        // System State
        SystemState systemState = new SystemStateImpl(currentServerId);
        GossipState gossipState = new GossipStateImpl(currentServerId);
        systemState.initialize(serverConfiguration);
        gossipState.initialize(serverConfiguration);
        // Coordination server
        this.coordinationServer = new SharedTcpServer(coordinationPort);
        // Components
        this.clientComponent = new ClientComponent(clientPort, currentServerId, gossipState, systemState);
        this.clientComponent.connect();
        this.gossipComponent = new GossipComponent(currentServerId, gossipState, serverConfiguration);
        this.gossipComponent.connect();
        this.raftComponent = new RaftComponent(currentServerId, systemState, serverConfiguration);
        this.raftComponent.connect();
        // Threads and Coordination server
        this.clientComponentThread = new Thread(clientComponent);
        this.coordinationServerThread = new Thread(coordinationServer);
        this.clientComponentThread.setName("client-component");
        this.coordinationServerThread.setName("coord-component");
    }

    /**
     * Start all the components and listen for the clients.
     *
     * @throws InterruptedException If listening is interrupted.
     */
    public void startListening() throws InterruptedException {
        // Attach components
        coordinationServer.attachRequestHandler(raftComponent);
        coordinationServer.attachRequestHandler(gossipComponent);
        // Start threads
        coordinationServerThread.start();
        clientComponentThread.start();
        // Wait until all threads exit
        clientComponentThread.join();
        coordinationServerThread.join();
    }

    @Override
    public void close() throws Exception {
        // Interrupt all threads
        clientComponentThread.interrupt();
        coordinationServerThread.interrupt();
        // Wait until threads exit
        clientComponentThread.join();
        coordinationServerThread.join();
        // Close components
        clientComponent.close();
        gossipComponent.close();
        raftComponent.close();
    }
}
