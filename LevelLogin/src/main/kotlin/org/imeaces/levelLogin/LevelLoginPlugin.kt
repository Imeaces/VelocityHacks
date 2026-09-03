package org.imeaces.levelLogin

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
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
//        server.eventManager.register(this, EventLogListener(logger))
    }

    companion object {
        @JvmStatic
        var jarPath: Path = Path.of( LevelLoginPlugin::class.java.protectionDomain.codeSource.location.toURI() )
    }
}
