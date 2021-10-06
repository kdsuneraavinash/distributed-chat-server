package lk.ac.mrt.cse.cs4262.components.raft.controller;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.BaseRaftMessage;

/**
 * Interface for RAFT message sender.
 */
public interface RaftMessageSender {
    /**
     * Send a message to a server.
     *
     * @param serverId Server to send message.
     * @param message  Message content.
     */
    void sendToServer(ServerId serverId, BaseRaftMessage message);
}
