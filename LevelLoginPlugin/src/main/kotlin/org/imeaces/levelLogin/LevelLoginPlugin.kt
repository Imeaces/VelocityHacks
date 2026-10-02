package org.imeaces.levelLogin

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.imeaces.levelLogin.profile.AccountManager
import org.imeaces.levelLogin.profile.SessionServerAuthManager
import org.slf4j.Logger
import java.nio.file.Path

class LevelLoginPlugin @Inject constructor(
    val logger: Logger,
    @DataDirectory val dataDirectory: Path,
    val server: ProxyServer,
) {
    val sessionServerAuthManager = SessionServerAuthManager(this)
    val accountManager = AccountManager(this)

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("datadir: {}", dataDirectory)
        logger.info("server: {}", server)

        server.eventManager.register(this, sessionServerAuthManager.YggdrasilSessionAuthListener())
        server.eventManager.register(this, accountManager.OfflineSessionAuthListener())
        server.eventManager.register(this, accountManager.AccountIdMappingListener())
    }
}
