package org.imeaces.levelLogin

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import org.imeaces.levellogin.handle.HandleChannelAccountId
import org.imeaces.levellogin.profile.AccountId

class OfflineSessionAuthManager {
    @Subscribe
    fun onPreLogin(event: PreLoginEvent){
        val inbound = event.connection
        if (HandleChannelAccountId.withLoginInboundConnectionHas(inbound)) {
            return
        }

        var accountId: AccountId? = HandleChannelAccountId.withLoginInboundConnectionGet(inbound)

        // TODO: grab accountId for offline player by inbound info

        if (accountId != null) {
            HandleChannelAccountId.withLoginInboundConnectionUpdate(inbound, accountId)
        }
    }
}
