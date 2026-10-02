package org.imeaces.levellogin.event;

import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import lombok.Data;
import org.imeaces.levellogin.data.HasJoinedOnlineResponseResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Data
public final class YggdrasilHasJoinedRequestEvent {
    private final String username;
    private final String serverId;
    private final String clientIp;
    private final LoginInboundConnection inbound;

    private HasJoinedOnlineResponseResult result;

    public @NotNull HasJoinedOnlineResponseResult getResult() {
        if (result == null) throw new IllegalStateException("no result was given");
        return result;
    }

    public void setResult(@NotNull HasJoinedOnlineResponseResult result) {
        Objects.requireNonNull(result, "result");
        this.result = result;
    }
}
