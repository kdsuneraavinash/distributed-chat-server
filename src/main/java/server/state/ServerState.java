package server.state;

import lombok.NonNull;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import server.state.logs.CreateIdentityLog;
import server.state.logs.CreateRoomLog;
import server.state.logs.DeleteIdentityLog;
import server.state.logs.DeleteRoomLog;

import java.util.HashMap;

@ToString
@Log4j2
public class ServerState {
    public static final String SERVER = "SERVER";
    // Servers -> Participants -> Room
    private final HashMap<String, HashMap<String, String>> state;
    // Participants -> Server
    private final HashMap<String, String> participantLocations;
    // Rooms -> Server
    private final HashMap<String, String> roomLocations;
    // Rooms -> Participant
    private final HashMap<String, String> roomOwners;

    public ServerState() {
        this.state = new HashMap<>();
        this.state.put(SERVER, new HashMap<>());
        this.participantLocations = new HashMap<>();
        this.roomLocations = new HashMap<>();
        this.roomOwners = new HashMap<>();
    }

    public void createIdentity(@NonNull CreateIdentityLog stateLog) {
        String identity = stateLog.getIdentity();
        String serverId = stateLog.getServerId();
        state.get(serverId).put(identity, null);
        participantLocations.put(identity, serverId);
        log.info(this);
    }

    public void createRoom(@NonNull CreateRoomLog stateLog) {
        String identity = stateLog.getIdentity();
        String serverId = stateLog.getServerId();
        String roomId = stateLog.getRoomId();
        state.get(serverId).put(identity, roomId);
        roomOwners.put(roomId, identity);
        roomLocations.put(roomId, serverId);
        log.info(this);
    }

    public void deleteIdentity(@NonNull DeleteIdentityLog stateLog) {
        String identity = stateLog.getIdentity();
        String serverId = participantLocations.remove(identity);
        String ownedRoomId = state.get(serverId).remove(identity);
        if (ownedRoomId != null) {
            roomLocations.remove(ownedRoomId);
            roomOwners.remove(ownedRoomId);
        }
        log.info(this);
    }

    public void deleteRoom(@NonNull DeleteRoomLog stateLog) {
        String roomId = stateLog.getRoomId();
        String serverId = roomLocations.remove(roomId);
        String ownerId = roomOwners.remove(roomId);
        state.get(serverId).put(ownerId, null);
        log.info(this);
    }

    public boolean hasParticipant(String identity) {
        return participantLocations.containsKey(identity);
    }
}
