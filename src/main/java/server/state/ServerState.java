package server.state;

import lombok.NonNull;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.Collection;
import java.util.HashMap;

@ToString
@Log4j2
public class ServerState {
    private final HashMap<String, String> participants;
    private final HashMap<String, String> roomOwners;

    public ServerState() {
        participants = new HashMap<>();
        roomOwners = new HashMap<>();
    }

    public boolean createIdentity(@NonNull String identity) {
        if (participants.containsKey(identity)) {
            log.info("Identity {} already exists.", identity);
            return false;
        }
        participants.put(identity, null);
        log.info("Created identity {}.", identity);
        return true;
    }

    public boolean createRoom(@NonNull String identity, @NonNull String roomId) {
        if (roomOwners.containsKey(roomId)) {
            log.info("Room {} already exists.", roomId);
            return false;
        }
        if (participants.get(roomId) != null) {
            log.info("{} is already owner of {}.", roomId, participants.get(roomId));
            return false;
        }
        roomOwners.put(roomId, identity);
        participants.put(identity, roomId);
        log.info("Created room {} for {}.", roomId, identity);
        return true;
    }

    public void deleteIdentity(@NonNull String identity) {
        if (roomOwners.containsKey(identity)) {
            // If identity has a room, delete it first
            deleteRoom(roomOwners.get(identity));
        }
        participants.remove(identity);
        log.info("Deleted identity {}.", identity);
    }

    public void deleteRoom(@NonNull String roomId) {
        participants.put(roomOwners.get(roomId), null);
        roomOwners.remove(roomId);
        log.info("Deleted room {}.", roomId);
    }

    public Collection<String> getAllIdentities() {
        return participants.keySet();
    }
}
