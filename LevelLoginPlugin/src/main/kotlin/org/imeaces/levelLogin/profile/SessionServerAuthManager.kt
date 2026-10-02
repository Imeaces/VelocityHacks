package org.imeaces.levelLogin.profile

import com.velocitypowered.api.event.Subscribe

import org.imeaces.levelLogin.LevelLoginPlugin
import org.imeaces.levellogin.auth.MojangSessionServerHasJoinedServerInfo
import org.imeaces.levellogin.auth.RequestHasJoinedExecutor
import org.imeaces.levellogin.auth.SessionValidateResult
import org.imeaces.levellogin.data.HasJoinedOnlineResponseResult
import org.imeaces.levellogin.data.LevelLoginConfiguration
import org.imeaces.levellogin.event.YggdrasilHasJoinedRequestEvent
import org.imeaces.levellogin.profile.AccountId

class SessionServerAuthManager(
    private val plugin: LevelLoginPlugin,
) {

    companion object {
        const val SESSION_SERVER_AUTH_TYPE_PREFIX: String = "yggdrasil"
    }

    private val executor = RequestHasJoinedExecutor()

    private val authServerList = listOf(
        MojangSessionServerHasJoinedServerInfo(
            LevelLoginConfiguration.shouldPreventClientProxyConnections()
        )
    )

    fun requestHasJoinedSync(
        username: String,
        serverId: String,
        clientIp: String,
    ): HasJoinedOnlineResponseResult {

        val result = executor.requestSessionValidate(authServerList, username, serverId, clientIp).get()

        return when (result) {
            is SessionValidateResult.Success -> {
                plugin.logger.info("authenticated {} from {} as {}", username, clientIp, result.profile)

                HasJoinedOnlineResponseResult.Success(
                    AccountId(
                        "${SESSION_SERVER_AUTH_TYPE_PREFIX}-${result.authServerInfo.authType()}",
                        result.profile.id.toString(),
                    ),
                    result.profile.id
                )
            }
            is SessionValidateResult.Failed -> {
                plugin.logger.info("Authentication incomplete for {} from {}", username, clientIp)

                HasJoinedOnlineResponseResult.Failed()
            }
            is SessionValidateResult.Invalid -> {
                plugin.logger.info("Invalid session of {} from {}", username, clientIp)

                HasJoinedOnlineResponseResult.Invalid()
            }
        }
    }

    inner class YggdrasilSessionAuthListener {
        @Subscribe
        fun onRequest(event: YggdrasilHasJoinedRequestEvent) {
            event.result = requestHasJoinedSync(event.username, event.serverId, event.clientIp)
        }
    }
}
