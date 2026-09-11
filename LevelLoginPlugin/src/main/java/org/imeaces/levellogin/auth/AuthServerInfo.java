package org.imeaces.levellogin.auth;

import java.net.URI;

public interface AuthServerInfo {
    URI createRequestUri(String username, String serverId, String clientIp);
}
