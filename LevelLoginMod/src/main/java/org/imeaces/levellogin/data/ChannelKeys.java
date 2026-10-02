package org.imeaces.levellogin.data;

import org.imeaces.levellogin.handle.HandleChannelAttrValue;
import org.imeaces.levellogin.profile.AccountId;
import org.imeaces.levellogin.profile.PlayerTrait;

public class ChannelKeys {
    public static final HandleChannelAttrValue<AccountId> AccountIdKey =
            HandleChannelAttrValue.create("levelLogin_accountId");
    public static final HandleChannelAttrValue<Boolean> ShouldAuthenticateKey =
            HandleChannelAttrValue.create("levelLogin_shouldAuthenticate");
    public static final HandleChannelAttrValue<PlayerTrait> PlayerTraitKey =
            HandleChannelAttrValue.create("levelLogin_playerTrait");
}
