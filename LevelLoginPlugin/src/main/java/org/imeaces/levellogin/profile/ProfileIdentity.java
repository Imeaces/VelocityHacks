package org.imeaces.levellogin.profile;

import java.util.Set;
import java.util.UUID;

public record ProfileIdentity(
        UUID playerUniqueId,
        String playerName,
        Set<AccountId> boundAccounts
) {
}
