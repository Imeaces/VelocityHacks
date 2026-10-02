package org.imeaces.levellogin.data;

import org.imeaces.levellogin.profile.AccountId;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public sealed interface HasJoinedOnlineResponseResult {

    record Success(
            @NotNull AccountId accountId,
            @NotNull UUID verifiedHolderId
    ) implements HasJoinedOnlineResponseResult {
    }

    record Invalid() implements HasJoinedOnlineResponseResult {

    }

    record Failed() implements HasJoinedOnlineResponseResult {

    }
}
