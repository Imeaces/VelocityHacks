package org.imeaces.levellogin.data;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

// TODO: 读取、加载用户配置
public class LevelLoginConfiguration {
    public static boolean isYggdrasilLoginEnabled(){
        return true;
    }
    public static boolean isOfflineAuthWithEncryptionEnabled(){
        return true;
    }
    public static boolean shouldPreventClientProxyConnections(){
        return false;
    }

    public static @NotNull String getHttpUserAgent() {
        return "LevelLogin/dev";
    }

    public static Duration getHttpConnectionTimeout() {
        return Duration.ofSeconds(8);
    }

    public static boolean isPasswordEnabled() {
        return true;
    }
}
