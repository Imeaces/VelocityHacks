package org.imeaces.levellogin.handle;

import com.velocitypowered.api.util.GameProfile;
import org.imeaces.levellogin.profile.AccountId;
import org.jetbrains.annotations.NotNull;

public class HandleGrabGameProfile {
    private static HandleGrabGameProfileHandler HANDLER;

    public static @NotNull GameProfile grabGameProfile(AccountId accountId){
        return HANDLER.grabGameProfile(accountId);
    }

    @FunctionalInterface
    public interface HandleGrabGameProfileHandler {
        @NotNull GameProfile grabGameProfile(AccountId accountId);
    }

    public static void setHandler( HandleGrabGameProfileHandler handler ) {
        HANDLER = handler;
    }
}
