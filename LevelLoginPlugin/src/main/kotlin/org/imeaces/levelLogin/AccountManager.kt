package org.imeaces.levelLogin

import com.velocitypowered.api.util.GameProfile
import org.imeaces.levellogin.handle.HandleGrabGameProfile
import org.imeaces.levellogin.profile.AccountId

class AccountManager : HandleGrabGameProfile.HandleGrabGameProfileHandler {
    override fun grabGameProfile(accountId: AccountId): GameProfile {
        // TODO: grab game profile relate to accountId

        return GameProfile.forOfflinePlayer("unknown")
    }
}
