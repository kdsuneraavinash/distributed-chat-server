package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lombok.Builder;
import lombok.ToString;

import java.util.Collection;

@ToString
@Builder
public class ListClientResponse {
    private final String type = "roomlist";

    private final Collection<RoomId> rooms;
}
