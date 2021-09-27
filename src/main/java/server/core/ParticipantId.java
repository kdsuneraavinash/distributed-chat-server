package server.core;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class ParticipantId {
    private final String value;

    @Override
    public String toString() {
        return value;
    }
}
