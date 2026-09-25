package org.imeaces.levellogin.handle;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import io.netty.channel.Channel;
import org.imeaces.levellogin.data.ChannelKeys;
import org.imeaces.levellogin.profile.AccountId;

import java.util.Objects;

public class HandleChannelAccountId {
    public static void claim(Channel ch, AccountId accountId){
        Objects.requireNonNull(accountId, "accountId");
        ch.attr(ChannelKeys.AccountIdKey).setIfAbsent(accountId);
    }

    public static void claim(MinecraftConnection minecraftConnection, AccountId accountId){
        claim(minecraftConnection.getChannel(), accountId);
    }

    public static void claim(LoginInboundConnection loginInboundConnection, AccountId accountId){
        claim(loginInboundConnection.delegatedConnection(), accountId);
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
}
