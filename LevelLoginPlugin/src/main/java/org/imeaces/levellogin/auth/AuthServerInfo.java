package org.imeaces.levellogin.auth;

import java.net.URI;
import java.time.Duration;

public interface AuthServerInfo {
    URI createRequestUri(String username, String serverId, String clientIp);

    String authType();

    default Duration requestTimeoutDuration() {
        return Duration.ofSeconds(15);
    }
}
