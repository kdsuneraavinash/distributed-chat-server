package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

import java.util.Collection;

@ToString
@AllArgsConstructor
public class ListClientResponse {
    private final String type = "roomlist";

    @NonNull
    private final Collection<RoomId> rooms;
}
