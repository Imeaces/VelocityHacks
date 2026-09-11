package org.imeaces.levellogin.auth;

import java.net.URI;

public record MojangSessionServerHasJoinedServerInfo(
        boolean preventProxy
) implements AuthServerInfo {
    @Override
    public URI createRequestUri(String username, String serverId, String clientIp) {
        var uri = "https://sessionserver.mojang.com/session/minecraft/hasJoined"
                + "?username=" + username
                + "&serverId=" + serverId;

        if (preventProxy) {
            uri += "&ip=" + clientIp;
        }

        return URI.create(uri);
    }
}
