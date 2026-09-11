package org.imeaces.levelLogin

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.util.GameProfile
import org.imeaces.levellogin.handle.HandleChannelAccountId
import org.imeaces.levellogin.handle.HandleGrabGameProfile
import org.imeaces.levellogin.handle.HandleHasJoined
import org.imeaces.levellogin.profile.AccountId
import org.slf4j.Logger
import java.nio.file.Path

class LevelLoginPlugin @Inject constructor(
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path,
    private val server: ProxyServer,
) {
    val offlineSessionAuthManager = OfflineSessionAuthManager()
    val sessionServerAuthManager = SessionServerAuthManager()
    val accountManager = AccountManager()

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("datadir: {}", dataDirectory)
        logger.info("server: {}", server)

        HandleHasJoined.setHandler(sessionServerAuthManager)
        HandleGrabGameProfile.setHandler(accountManager)
        server.eventManager.register(this, offlineSessionAuthManager)
    }
}
