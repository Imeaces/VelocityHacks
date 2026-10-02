package org.imeaces.levelLogin.profile

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.util.GameProfile
import com.velocitypowered.proxy.connection.client.LoginInboundConnection
import org.imeaces.levelLogin.LevelLoginPlugin
import org.imeaces.levellogin.data.ChannelKeys
import org.imeaces.levellogin.profile.AccountId
import org.imeaces.levellogin.profile.PlayerTrait

class AccountManager(private val plugin: LevelLoginPlugin) {
    fun grabGameProfile(accountId: AccountId): GameProfile {
        TODO("grab game profile relate to accountId")
    }

    fun grabAccountId(maybeOfflineIdentity: PlayerTrait): AccountId? {
        TODO("grab accountId for offline player by inbound info")
    }

    inner class OfflineSessionAuthListener {
        @Subscribe(priority = 10000)
        fun onPreLogin(event: PreLoginEvent){
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

            val accountId: AccountId? = grabAccountId(playerTrait)

            if (accountId != null) {
                plugin.logger.info( "claiming accountId for player {}: {}", event.username, accountId )
                ChannelKeys.AccountIdKey.claim(inbound, accountId)
            }
        }
    }
}
