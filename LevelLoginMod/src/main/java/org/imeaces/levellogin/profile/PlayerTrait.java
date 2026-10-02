package org.imeaces.levellogin.profile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record PlayerTrait(
        @NotNull String clientIp,
        @NotNull String playerName,
        @Nullable UUID holderUuid,
        @Nullable String inboundVirtualHost,
        int inboundProtocolVersion
) {
}