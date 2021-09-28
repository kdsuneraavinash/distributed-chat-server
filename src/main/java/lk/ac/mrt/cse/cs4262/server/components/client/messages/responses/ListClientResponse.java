package lk.ac.mrt.cse.cs4262.server.components.client.messages.responses;

import lombok.NonNull;
import lombok.ToString;
import lk.ac.mrt.cse.cs4262.server.core.RoomId;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@ToString
public class ListClientResponse {
    public static final String TYPE = "roomlist";

    private final String type;
    private final List<String> rooms;

    public ListClientResponse(@NonNull Collection<RoomId> roomsIds) {
        this.type = TYPE;
        this.rooms = roomsIds.stream().map(RoomId::getValue).collect(Collectors.toList());
    }
}
