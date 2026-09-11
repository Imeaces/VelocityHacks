package org.imeaces.levellogin.handle;

import com.velocitypowered.proxy.connection.client.LoginInboundConnection;

public class HandleHasJoined {
    private static HandleHasJoinedHandler HANDLER;

    public static void requestHasJoined(String username, String serverId, String clientIp, LoginInboundConnection connection){
        HANDLER.requestHasJoined(username, serverId, clientIp, connection);
    }

    @FunctionalInterface
    public interface HandleHasJoinedHandler {
        /**
         * 使用 Yggdrasil 的形式完成服务器登录认证，并根据认证结果确定 AccountId
         * @param username 登录玩家名
         * @param serverId 服务器ID
         * @param clientIp 客户端IP
         * @param inbound 连接对象
         * @implSpec
         * 应在此方法中异步地请求 Session Server 以获取玩家档案，随后转换为对应的 AccountId，设置到连接的 Channel 上。
         */
        void requestHasJoined(String username, String serverId, String clientIp, LoginInboundConnection inbound);
    }

    public static void setHandler( HandleHasJoinedHandler handler ) {
        HANDLER = handler;
    }
}
