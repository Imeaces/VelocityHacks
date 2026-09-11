package org.imeaces.levellogin.handle;

import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import io.netty.channel.Channel;
import org.imeaces.levellogin.data.ChannelKeys;
import org.imeaces.levellogin.profile.AccountId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class HandleChannelAccountId {
    public static void update(Channel ch, AccountId accountId){
        Objects.requireNonNull(accountId, "accountId");
        ch.attr(ChannelKeys.AccountIdKey).setIfAbsent(accountId);
    }

    public static void update(MinecraftConnection minecraftConnection, AccountId accountId){
        update(minecraftConnection.getChannel(), accountId);
    }

    public static void update(LoginInboundConnection loginInboundConnection, AccountId accountId){
        update(loginInboundConnection.delegatedConnection(), accountId);
    }

    public static AccountId get(Channel ch){
        return ch.attr(ChannelKeys.AccountIdKey).get();
    }

    public static AccountId get(MinecraftConnection minecraftConnection){
        return get(minecraftConnection.getChannel());
    }

    public static AccountId get(LoginInboundConnection loginInboundConnection){
        return get(loginInboundConnection.delegatedConnection());
    }

    public static boolean has(Channel ch){
        return get(ch) != null;
    }

    public static boolean has(MinecraftConnection minecraftConnection){
        return get(minecraftConnection) != null;
    }

    public static boolean has(LoginInboundConnection loginInboundConnection){
        return get(loginInboundConnection) != null;
    }

    public static boolean withLoginInboundConnectionHas(InboundConnection inbound) {
        if (inbound instanceof LoginInboundConnection loginInboundConnection) {
            return has(loginInboundConnection);
        } else {
            return true;
        }
    }

    public static @Nullable AccountId withLoginInboundConnectionGet(InboundConnection inbound) {
        if (inbound instanceof LoginInboundConnection loginInboundConnection) {
            return get(loginInboundConnection);
        } else {
            return null;
        }
    }

    public static void withLoginInboundConnectionUpdate(InboundConnection inbound, @NotNull AccountId accountId) {
        if (inbound instanceof LoginInboundConnection loginInboundConnection) {
            update(loginInboundConnection,  accountId);
        }
    }
}
