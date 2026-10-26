package org.imeaces.levellogin.event;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import lombok.Getter;
import org.imeaces.levellogin.profile.AccountId;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class AccountIdMappingEvent {
    @Getter
    private final @NotNull AccountId accountId;

    @Getter
    private final @NotNull LoginInboundConnection inbound;

    private GameProfile gameProfile;

    public AccountIdMappingEvent(@NonNull AccountId accountId, @NonNull LoginInboundConnection inbound) {
        this.accountId = accountId;
        this.inbound = inbound;
    }

    public void setGameProfile(GameProfile gameProfile) {
        Objects.requireNonNull(gameProfile, "gameProfile");
        this.gameProfile = gameProfile;
    }

    public @NotNull GameProfile getGameProfile() {
        if (gameProfile == null) throw new IllegalStateException("Game profile has not been set");
        return gameProfile;
    }
}
