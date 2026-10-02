package org.imeaces.levellogin.auth;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import org.imeaces.levellogin.LevelLoginMain;
import org.imeaces.levellogin.data.LevelLoginConfiguration;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RequestHasJoinedExecutor {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(LevelLoginConfiguration.getHttpConnectionTimeout())
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public @NotNull CompletableFuture<@NotNull SessionValidateResult> requestSessionValidate(
            List<AuthServerInfo> authServerList,
            String username,
            String serverId,
            String clientIp
    ) {
        var requestedValidateFutures = authServerList.stream().map(authServer -> this.startSessionValidateAsync(authServer, username, serverId, clientIp)).toList();

        return CompletableFuture.allOf( requestedValidateFutures.toArray(CompletableFuture[]::new) )
        .thenApply(_ -> {
            var validateResultList = requestedValidateFutures.stream().map(CompletableFuture::join).toList();

            var succeedResults = new ArrayList<SessionValidateResult>();
            var invalidResults = new ArrayList<>();
            var failedResults = new ArrayList<>();
            for (var result : validateResultList) {
                switch (result){
                    case SessionValidateResult.Success result1 -> succeedResults.add(result1);
                    case SessionValidateResult.Failed result2 -> failedResults.add(result2);
                    case SessionValidateResult.Invalid result3 -> invalidResults.add(result3);
                }
            }

            LevelLoginMain.getLogger().debug(
                    "session validate result for player {} ip {}: {} succeed, {} invalid, {} failed to query",
                    username, clientIp, succeedResults.size(), failedResults.size(), invalidResults.size());

            if (invalidResults.size() == validateResultList.size()){
                return SessionValidateResult.INVALID;
            }

            if (failedResults.size() == validateResultList.size()){
                return SessionValidateResult.FAILED;
            }

            if (succeedResults.size() == 1){
                var result = succeedResults.getFirst();

                LevelLoginMain.getLogger().debug(
                        "session validate result for player {} with ip {} is {}",
                        username, clientIp, result);

                return result;
            } else if (succeedResults.size() > 1) {
                LevelLoginMain.getLogger().warn(
                        "invalid session validate results: more than 1 results returned: {}", succeedResults);
                return SessionValidateResult.FAILED;
            }

            return SessionValidateResult.FAILED;
        });

    }

    private @NotNull CompletableFuture<@NotNull SessionValidateResult> startSessionValidateAsync(
            AuthServerInfo authServer,
            String username,
            String serverId,
            String clientIp
    ) {
        URI uri = authServer.createRequestUri(username, serverId, clientIp);
        HttpRequest request =
                HttpRequest.newBuilder(uri)
                .timeout(authServer.requestTimeoutDuration())
                .GET()
                .setHeader("User-Agent", LevelLoginConfiguration.getHttpUserAgent())
                .build();

        return
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())

        .thenApply(response -> {
            switch (response.statusCode()) {
                case 200 -> {
                    var profile = VelocityServer.GENERAL_GSON.fromJson(
                            response.body(),
                            GameProfile.class
                    );
                    return SessionValidateResult.Success.with(authServer, profile);
                }
                case 204 -> {
                    return SessionValidateResult.INVALID;
                }
                default -> {
                    LevelLoginMain.getLogger().warn("Unexpected response status: {}", response.statusCode());
                    return SessionValidateResult.FAILED;
                }
            }
        })

        .exceptionally(ex -> {
            LevelLoginMain.getLogger().error("Failed to request to {} for player {}:",
                    authServer.authType(),
                    username,
                    ex
            );
            return SessionValidateResult.FAILED;
        });
    }
}
