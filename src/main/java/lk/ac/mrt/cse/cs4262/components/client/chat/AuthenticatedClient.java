package lk.ac.mrt.cse.cs4262.components.client.chat;

import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lombok.Builder;
import lombok.Value;

/**
 * Client that is authenticated (has participant id).
 */
@Value
@Builder
public class AuthenticatedClient {
    ClientId clientId;
    ParticipantId participantId;
    ServerId serverId;
    RoomId currentRoomId;
    RoomId owningRoomId;
}
