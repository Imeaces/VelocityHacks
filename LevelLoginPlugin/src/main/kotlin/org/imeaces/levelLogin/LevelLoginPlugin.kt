package org.imeaces.levelLogin

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.imeaces.levelLogin.profile.AccountManager
import org.imeaces.levelLogin.profile.SessionServerAuthManager
import org.imeaces.levellogin.auth.InMemoryLoginCredentialStore
import org.imeaces.levellogin.auth.LoginCredentialStore
import org.imeaces.levellogin.auth.password.PasswordAuthManager
import org.imeaces.levellogin.command.ProfileCommand
import org.imeaces.levellogin.config.LevelLoginConfig
import org.imeaces.levellogin.profile.InMemoryProfileIdentityStore
import org.imeaces.levellogin.profile.ProfileIdentityStore
import org.imeaces.levellogin.profile.storage.ProfileStore
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

    // 配置
    val config: LevelLoginConfig = LevelLoginConfig.load(dataDirectory)

    // 档案存储（持久化）
    val profileStore: ProfileStore = ProfileStore(dataDirectory)

    // 密码认证管理器
    val passwordAuthManager: PasswordAuthManager = PasswordAuthManager(profileStore, config)

    val sessionServerAuthManager = SessionServerAuthManager(this)
    val accountManager = AccountManager(this, profileIdentityStore, loginCredentialStore)

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("LevelLogin Plugin initializing...")
        logger.info("Data directory: {}", dataDirectory)
        logger.info("Config loaded: Yggdrasil={}, Floodgate={}, Password={}",
            config.isEnableYggdrasil, config.isEnableFloodgate, config.isEnablePassword)
        logger.info("ProfileStore initialized: {} profiles loaded", profileStore.count())

        // 注册原有事件监听器
        server.eventManager.register(this, sessionServerAuthManager.YggdrasilSessionAuthListener())
        server.eventManager.register(this, accountManager.OfflineSessionAuthListener())
        server.eventManager.register(this, accountManager.AccountIdMappingListener())

        // 注册密码认证相关监听器
        server.eventManager.register(this, passwordAuthManager.ChatListener())
        server.eventManager.register(this, passwordAuthManager.ServerConnectListener())
        server.eventManager.register(this, accountManager.PostLoginAuthListener())

        // 注册 /profile 命令
        val profileCmd = ProfileCommand(profileStore).createCommand()
        server.commandManager.register(
            server.commandManager.metaBuilder("profile").plugin(this).build(),
            profileCmd
        )
        server.commandManager.register(
            server.commandManager.metaBuilder("pl").plugin(this).build(),
            profileCmd
        )

        logger.info("LevelLogin Plugin initialized successfully!")
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        logger.info("LevelLogin Plugin shutting down...")
    }
}
