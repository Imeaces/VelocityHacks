package org.imeaces.levelLogin

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.util.GameProfile
import com.velocitypowered.proxy.connection.client.LoginInboundConnection
import org.imeaces.levellogin.handle.HandleChannelAccountId
import org.imeaces.levellogin.handle.HandleGrabGameProfile
import org.imeaces.levellogin.profile.AccountId
import org.imeaces.levellogin.profile.PlayerTrait
import kotlin.jvm.optionals.getOrNull

class AccountManager(plugin: LevelLoginPlugin) : HandleGrabGameProfile.HandleGrabGameProfileHandler {
    override fun grabGameProfile(accountId: AccountId): GameProfile {
        TODO("grab game profile relate to accountId")
    }

    fun grabAccountId(maybeOfflineIdentity: PlayerTrait): AccountId? {
        // TODO: grab accountId for offline player by inbound info

        return null
    }

    inner class OfflineSessionAuthListener {
        @Subscribe
        fun onPreLogin(event: PreLoginEvent){
            val inbound = event.connection
            if (inbound !is LoginInboundConnection) {
                return
            }

            val maybeOfflineIdentity = PlayerTrait(
                inbound.remoteAddress.address.hostAddress,
                event.username,
                event.uniqueId,
                inbound.rawVirtualHost.getOrNull(),
                inbound.protocolVersion.protocol
            )

            val accountId: AccountId? = grabAccountId(maybeOfflineIdentity)

            if (accountId != null) {
                HandleChannelAccountId.claim(inbound, accountId)
            }
        }
    }
}
