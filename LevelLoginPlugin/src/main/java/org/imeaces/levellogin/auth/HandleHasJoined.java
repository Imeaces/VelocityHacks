package org.imeaces.levellogin.auth;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class HandleHasJoined {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public CompletableFuture<@Nullable GameProfile> requestHasJoined(
            AuthServerInfo authServerInfo,
            String username,
            String serverId,
            String clientIp
    ) {
        URI uri = authServerInfo.createRequestUri(username, serverId, clientIp);

        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 204) {
                        return null;
                    }

                    if (response.statusCode() != 200) {
                        throw new CompletionException(
                                new IOException(
                                        "HTTP " + response.statusCode()
                                )
                        );
                    }

                    return VelocityServer.GENERAL_GSON.fromJson(
                            response.body(),
                            GameProfile.class
                    );
                });
    }
}
