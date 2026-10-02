package org.imeaces.levellogin.auth;

import com.velocitypowered.api.util.GameProfile;

public sealed interface SessionValidateResult
{
    record Success(
            AuthServerInfo authServerInfo,
            GameProfile profile
    ) implements SessionValidateResult {
        static SessionValidateResult with(AuthServerInfo authServer, GameProfile sessionProfile) {
            return new Success(authServer, sessionProfile);
        }
    }
    record Invalid() implements SessionValidateResult {}
    record Failed() implements SessionValidateResult {}

    SessionValidateResult INVALID = new Invalid();
    SessionValidateResult FAILED = new Failed();
}
