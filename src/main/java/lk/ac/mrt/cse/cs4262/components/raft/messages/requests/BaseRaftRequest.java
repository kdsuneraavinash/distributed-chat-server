package lk.ac.mrt.cse.cs4262.components.raft.messages.requests;

import lombok.Getter;

@Getter
public abstract class BaseRaftRequest {
    private final String type;

    protected BaseRaftRequest(String type) {
        this.type = type;
    }
}
