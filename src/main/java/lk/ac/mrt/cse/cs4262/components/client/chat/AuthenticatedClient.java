package lk.ac.mrt.cse.cs4262.components.client.chat;

import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Client that is authenticated (has participant id).
 */
@Builder
@Getter
@AllArgsConstructor
public class AuthenticatedClient {
    private final ClientId clientId;
    private final ParticipantId participantId;
    private final ServerId serverId;
    private final RoomId currentRoomId;
    @Nullable
    private final RoomId owningRoomId;
}
