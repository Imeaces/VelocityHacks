package org.imeaces.levelLogin

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.util.GameProfile
import com.velocitypowered.proxy.connection.client.LoginInboundConnection
import org.imeaces.levellogin.handle.HandleChannelAccountId
import org.imeaces.levellogin.handle.HandleGrabGameProfile
import org.imeaces.levellogin.profile.AccountId
import org.imeaces.levellogin.profile.OfflinePlayerIdentity

class AccountManager(plugin: LevelLoginPlugin) : HandleGrabGameProfile.HandleGrabGameProfileHandler {
    override fun grabGameProfile(accountId: AccountId): GameProfile {
        TODO("grab game profile relate to accountId")
    }

    fun grabAccountId(maybeOfflineIdentity: OfflinePlayerIdentity): AccountId? {
        // TODO: grab accountId for offline player by inbound info

        return null
    }

    inner class OfflineSessionAuthListener {
        @Subscribe
        fun onPreLogin(event: PreLoginEvent){
            val inbound = event.connection
            if (HandleChannelAccountId.withLoginInboundConnectionHas(inbound)) {
                return
            }

            val loginInbound: LoginInboundConnection = inbound as LoginInboundConnection

            val maybeOfflineIdentity = OfflinePlayerIdentity(
                loginInbound.remoteAddress.address.hostAddress,
                event.username,
                event.uniqueId
            )

            val accountId: AccountId? = grabAccountId(maybeOfflineIdentity)

            if (accountId != null) {
                HandleChannelAccountId.withLoginInboundConnectionUpdate(inbound, accountId)
            }
        }
    }
}
