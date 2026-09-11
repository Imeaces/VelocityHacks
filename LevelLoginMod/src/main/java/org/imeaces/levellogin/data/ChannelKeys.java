package org.imeaces.levellogin.data;

import io.netty.util.AttributeKey;
import org.imeaces.levellogin.profile.AccountId;

public class ChannelKeys {
    public static final AttributeKey<AccountId> AccountIdKey = AttributeKey.valueOf("levelLogin_accountId");
}
