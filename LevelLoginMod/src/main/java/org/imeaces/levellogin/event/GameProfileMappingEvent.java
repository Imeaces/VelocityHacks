package org.imeaces.levellogin.event;

import com.velocitypowered.api.util.GameProfile;
import lombok.Getter;
import org.imeaces.levellogin.profile.AccountId;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class GameProfileMappingEvent {
    @Getter
    private final @NotNull AccountId accountId;

    private GameProfile gameProfile;

    public GameProfileMappingEvent(@NonNull AccountId accountId) {
        this.accountId = accountId;
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
