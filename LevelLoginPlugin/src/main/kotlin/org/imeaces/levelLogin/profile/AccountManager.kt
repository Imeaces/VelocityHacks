package org.imeaces.levelLogin.profile

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.util.GameProfile
import com.velocitypowered.proxy.connection.client.LoginInboundConnection
import org.imeaces.levelLogin.LevelLoginPlugin
import org.imeaces.levellogin.data.ChannelKeys
import org.imeaces.levellogin.event.AccountIdMappingEvent
import org.imeaces.levellogin.auth.LoginCredentialStore
import org.imeaces.levellogin.profile.AccountId
import org.imeaces.levellogin.profile.PlayerTrait
import org.imeaces.levellogin.profile.ProfileIdentityStore

class AccountManager(
    private val plugin: LevelLoginPlugin,
    private val identityStore: ProfileIdentityStore,
    private val loginCredentialStore: LoginCredentialStore,
) {
    fun grabGameProfile(accountId: AccountId): GameProfile {
        val identity = identityStore.findIdentity(accountId).orElse(null)

        if (identity == null) {
            throw IllegalStateException("no profile identity is bound to account $accountId")
        }

        return GameProfile(identity.playerUniqueId, identity.playerName, emptyList())
    }

    fun grabRelatedAccountId(playerTrait: PlayerTrait): AccountId? {
        // 连接特征整体作为指纹去查登录凭据。命中即代表这个连接"之前完成过一次认证"，
        // 因此它关联的会话凭据可以直接采信，不需要再看 authType，也不需要反查身份存储。
        return loginCredentialStore.findSessionCredential(playerTrait).orElse(null)
    }

    inner class AccountIdMappingListener {
        @Subscribe
        fun onGameProfileMapping(event: AccountIdMappingEvent) {
            event.gameProfile = grabGameProfile(event.accountId)
        }
    }

    inner class OfflineSessionAuthListener {

        @Subscribe(priority = 10000)
        fun onPreLogin(event: PreLoginEvent) {
            val inbound = event.connection
            if (inbound !is LoginInboundConnection) {
                throw RuntimeException(
                    "inbound connection is not of type LoginInboundConnection, typeof inbound = "
                    + inbound::class.java.name)
            }

            val playerTrait: PlayerTrait? = ChannelKeys.PlayerTraitKey.get(inbound)

            if (playerTrait == null) {
                throw IllegalStateException("Player trait not found")
            }

            val accountId: AccountId? = grabRelatedAccountId(playerTrait)

            if (accountId != null) {
                plugin.logger.info("claiming accountId for player {}: {}", event.username, accountId)
                ChannelKeys.AccountIdKey.claim(inbound, accountId)
            }
        }
    }

    /**
     * 登录后检查是否需要密码认证
     */
    inner class PostLoginAuthListener {
        @Subscribe
        fun onLogin(event: LoginEvent) {
            val player = event.player

            // 检查是否有 AccountId
            if (event.connection is LoginInboundConnection) {
                val inbound = event.connection as LoginInboundConnection
                val accountId = ChannelKeys.AccountIdKey.get(inbound)

                if (accountId == null && plugin.config.isEnablePassword) {
                    // 没有认证，需要密码认证
                    plugin.passwordAuthManager.requireAuth(player, player.username)
                }
            }
        }
    }
}
