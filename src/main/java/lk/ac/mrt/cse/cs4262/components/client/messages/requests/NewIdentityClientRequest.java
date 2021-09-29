package lk.ac.mrt.cse.cs4262.components.client.messages.requests;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
public class NewIdentityClientRequest extends BaseClientRequest {
    @Nullable
    private String identity;
}
