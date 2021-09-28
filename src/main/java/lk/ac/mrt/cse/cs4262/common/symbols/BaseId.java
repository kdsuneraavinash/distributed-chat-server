package lk.ac.mrt.cse.cs4262.common.symbols;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A base ID for all the IDs in the system.
 * The ID only has a string value which is used for all the
 * equality/hash checks.
 */
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class BaseId {
    private final String value;

    @Override
    public final String toString() {
        return value;
    }
}
