package org.imeaces.levelLogin

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.player.GameProfileRequestEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.util.GameProfile
import com.velocitypowered.proxy.connection.client.LoginInboundConnection
import org.imeaces.levellogin.data.ChannelKeys
import org.imeaces.levellogin.handle.HandleGrabGameProfile
import org.imeaces.levellogin.profile.AccountId
import org.slf4j.Logger
import java.nio.file.Path

class LevelLoginPlugin @Inject constructor(
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path,
    private val server: ProxyServer,
) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("datadir: {}", dataDirectory)
        logger.info("server: {}", server)

        HandleGrabGameProfile.setHandler { it ->
            // TODO: grab game profile relate to accountId

            GameProfile.forOfflinePlayer("unknown")
        }
    }

    @Subscribe
    fun onPreLogin(event: PreLoginEvent){
        val inbound = event.connection
        if (inbound is LoginInboundConnection){
            val channel = inbound.delegatedConnection().channel
            val accountId: AccountId? = channel.attr(ChannelKeys.AccountIdKey).get()

            if (accountId == null){
                // TODO: grab accountId for offline player by inbound info

                if (accountId != null) {
                    channel.attr(ChannelKeys.AccountIdKey).setIfAbsent(accountId)
                }
            }
        }
    }
}
