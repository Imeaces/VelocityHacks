package org.imeaces.levellogin.auth;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record MojangSessionServerHasJoinedServerInfo(
        boolean preventProxy
) implements AuthServerInfo {
    @Override
    public URI createRequestUri(String username, String serverId, String clientIp) {
        var uri = "https://sessionserver.mojang.com/session/minecraft/hasJoined"
                + "?username=" + encode(username)
                + "&serverId=" + encode(serverId);

        if (preventProxy) {
            uri += "&ip=" + encode(clientIp);
        }

        return URI.create(uri);
    }

    @Override
    public String authType() {
        return "mojang";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
