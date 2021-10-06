package lk.ac.mrt.cse.cs4262.components.raft.controller;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.messages.variants.BaseRaftMessage;

public interface RaftMessageSender {
    void sendToServer(ServerId serverId, BaseRaftMessage message);
}
