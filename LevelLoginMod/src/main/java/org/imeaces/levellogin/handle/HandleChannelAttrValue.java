package org.imeaces.levellogin.handle;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.Objects;

public record HandleChannelAttrValue<T>(AttributeKey<T> attrKey) {

    public static <T> HandleChannelAttrValue<T> create(String attrId) {
        return new HandleChannelAttrValue<>(AttributeKey.valueOf(attrId));
    }

    public void set(Channel ch, T value) {
        Objects.requireNonNull(value, "value");
        ch.attr(attrKey).set(value);
    }

    public void set(MinecraftConnection minecraftConnection, T value) {
        set(minecraftConnection.getChannel(), value);
    }

    public void set(LoginInboundConnection loginInboundConnection, T value) {
        set(loginInboundConnection.delegatedConnection(), value);
    }

    public void claim(Channel ch, T value) {
        Objects.requireNonNull(value, "value");
        ch.attr(attrKey).setIfAbsent(value);
    }

    public void claim(MinecraftConnection minecraftConnection, T value) {
        claim(minecraftConnection.getChannel(), value);
    }

    public void claim(LoginInboundConnection loginInboundConnection, T value) {
        claim(loginInboundConnection.delegatedConnection(), value);
    }

    public T get(Channel ch) {
        return ch.attr(attrKey).get();
    }

    public T get(MinecraftConnection minecraftConnection) {
        return get(minecraftConnection.getChannel());
    }

    public T get(LoginInboundConnection loginInboundConnection) {
        return get(loginInboundConnection.delegatedConnection());
    }

    public boolean has(Channel ch) {
        return get(ch) != null;
    }

    public boolean has(MinecraftConnection minecraftConnection) {
        return get(minecraftConnection) != null;
    }

    public boolean has(LoginInboundConnection loginInboundConnection) {
        return get(loginInboundConnection) != null;
    }
}
