package lk.ac.mrt.cse.cs4262.components.raft.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class RaftAckMessage {
    private final boolean isAccepted;
}
