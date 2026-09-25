package org.imeaces.levellogin.handle;

import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import org.imeaces.levellogin.profile.AccountId;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HandleHasJoined {
    private static HandleHasJoinedHandler HANDLER;

    public static @NotNull CompletableFuture<@NotNull HasJoinedOnlineResponseResult> requestHasJoined(
            String username,
            String serverId,
            String clientIp,
            LoginInboundConnection connection
    ) {
        return HANDLER.requestHasJoined(username, serverId, clientIp, connection);
    }

    @FunctionalInterface
    public interface HandleHasJoinedHandler {
        /**
         * 使用 Yggdrasil 的形式完成服务器登录认证，并根据认证结果确定 AccountId
         *
         * @param username 登录玩家名
         * @param serverId 服务器ID
         * @param clientIp 客户端IP
         * @param inbound 连接对象
         * @return 认证结果的 future：
         *         <ul>
         *             <li>认证通过：以 {@link HasJoinedOnlineResponseSuccess} 完成，其中同时携带解析出的
         *                 AccountId 与会话服务器为该玩家认证的 UUID；</li>
         *             <li>认证服务器明确拒绝了该玩家（例如 Mojang 返回 204）：以 {@link HasJoinedOnlineResponseFailed} 完成；</li>
         *             <li>请求失败、无法完成认证：以异常完成。</li>
         *         </ul>
         * @implSpec
         * 该方法在 Netty event loop 上被调用，必须立即返回，不得阻塞调用线程。
         * 实现负责异步地请求 Session Server 并把认证结果映射为 {@link HasJoinedOnlineResponseResult}；
         * 把 AccountId 记录到连接上由调用方负责。
         * <p>
         * {@link HasJoinedOnlineResponseSuccess#verifiedUniqueId()} 必须是会话服务器认证的那个 UUID，
         * 它与 {@link HasJoinedOnlineResponseSuccess#accountId()} 的 subject 不一定相同：前者用于客户端公钥绑定，
         * 后者是账户的标识。
         */
        CompletableFuture<@NotNull HasJoinedOnlineResponseResult> requestHasJoined(
                String username,
                String serverId,
                String clientIp,
                LoginInboundConnection inbound
        );
    }

    public sealed interface HasJoinedOnlineResponseResult
            permits HasJoinedOnlineResponseSuccess, HasJoinedOnlineResponseFailed {
    }

    public record HasJoinedOnlineResponseSuccess(
            @NotNull AccountId accountId,
            @NotNull UUID verifiedUniqueId
    ) implements HasJoinedOnlineResponseResult {
    }

    public record HasJoinedOnlineResponseFailed() implements HasJoinedOnlineResponseResult {

    }

    public static void setHandler( HandleHasJoinedHandler handler ) {
        HANDLER = handler;
    }
}
