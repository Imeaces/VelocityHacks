package org.imeaces.levelLogin

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.imeaces.levelLogin.profile.AccountManager
import org.imeaces.levelLogin.profile.SessionServerAuthManager
import org.imeaces.levellogin.auth.InMemoryLoginCredentialStore
import org.imeaces.levellogin.auth.LoginCredentialStore
import org.imeaces.levellogin.profile.InMemoryProfileIdentityStore
import org.imeaces.levellogin.profile.ProfileIdentityStore
import org.slf4j.Logger
import java.nio.file.Path

class LevelLoginPlugin @Inject constructor(
    val logger: Logger,
    @DataDirectory val dataDirectory: Path,
    val server: ProxyServer,
) {
    /** 目前是纯内存实现，进程重启后绑定关系全部丢失。 */
    val profileIdentityStore: ProfileIdentityStore = InMemoryProfileIdentityStore()

    /** 目前是纯内存实现，进程重启后需要重新走一次连接特征认证。 */
    val loginCredentialStore: LoginCredentialStore = InMemoryLoginCredentialStore()

    val sessionServerAuthManager = SessionServerAuthManager(this)
    val accountManager = AccountManager(this, profileIdentityStore, loginCredentialStore)

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("datadir: {}", dataDirectory)
        logger.info("server: {}", server)

        server.eventManager.register(this, sessionServerAuthManager.YggdrasilSessionAuthListener())
        server.eventManager.register(this, accountManager.OfflineSessionAuthListener())
        server.eventManager.register(this, accountManager.AccountIdMappingListener())
    }
}
