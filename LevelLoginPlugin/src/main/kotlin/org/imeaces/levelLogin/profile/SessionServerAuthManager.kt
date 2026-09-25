package org.imeaces.levelLogin

import com.velocitypowered.api.util.GameProfile
import com.velocitypowered.proxy.connection.client.LoginInboundConnection
import org.imeaces.levellogin.auth.HandleHasJoinedInjector
import org.imeaces.levellogin.auth.MojangSessionServerHasJoinedServerInfo
import org.imeaces.levellogin.handle.HandleHasJoined
import org.imeaces.levellogin.profile.AccountId
import java.util.concurrent.CompletableFuture

class SessionServerAuthManager(
    private val plugin: LevelLoginPlugin,
) : HandleHasJoined.HandleHasJoinedHandler {

    private val client = HandleHasJoinedInjector()

    override fun requestHasJoined(
        username: String,
        serverId: String,
        clientIp: String,
        inbound: LoginInboundConnection,
    ): CompletableFuture<HandleHasJoined.HasJoinedOnlineResponseResult> {
        val authServerInfo = MojangSessionServerHasJoinedServerInfo(
            plugin.server.configuration.shouldPreventClientProxyConnections()
        )

        return client
            .requestHasJoined(authServerInfo, username, serverId, clientIp)
            .thenApply { gameProfile: GameProfile? ->
                gameProfile?.let {
                    HandleHasJoined.HasJoinedOnlineResponseSuccess(
                        AccountId(AUTH_TYPE_YGGDRASIL, it.id.toString()),
                        it.id,
                    )
                } ?: HandleHasJoined.HasJoinedOnlineResponseFailed()
            }
    }

    private companion object {
        /** 由 Yggdrasil 会话服务器认证得到的账户的 authType。 */
        const val AUTH_TYPE_YGGDRASIL = "yggdrasil"
    }
}
