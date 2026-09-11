package org.imeaces.levellogin.handle;

import org.imeaces.levellogin.profile.AccountId;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class HandleHasJoined {
    private static HandleHasJoinedHandler HANDLER;

    public static CompletableFuture<@Nullable AccountId> requestHasJoined(String username, String serverId, String clientIp){
        return HANDLER.requestHasJoined(username, serverId, clientIp);
    }

    @FunctionalInterface
    public interface HandleHasJoinedHandler {
        CompletableFuture<@Nullable AccountId> requestHasJoined(String username, String serverId, String clientIp);
    }

    public static void setHandler( org.imeaces.levellogin.handle.HandleHasJoined.HandleHasJoinedHandler handler ) {
        HANDLER = handler;
    }
}
