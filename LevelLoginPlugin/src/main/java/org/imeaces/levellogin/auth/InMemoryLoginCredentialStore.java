package org.imeaces.levellogin.auth;

import org.imeaces.levellogin.profile.AccountId;
import org.imeaces.levellogin.profile.PlayerTrait;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link LoginCredentialStore} 的纯内存实现。
 *
 * <p>进程重启后所有登录凭据都会丢失。短期凭据本来就该如此——丢了只是让玩家重新走一次认证，
 * 而不是丢失任何长期数据。
 *
 * <p>过期条目采用惰性清理：查找命中时顺带丢弃该条，写入时顺带扫一遍全表。
 * 凭据数量等于"刚刚完成认证、还没重新连上"的玩家数，规模极小，不值得引入定时任务。
 */
public final class InMemoryLoginCredentialStore implements LoginCredentialStore {

    private final Object lock = new Object();

    /** 完整连接特征 → 会话凭据。{@link PlayerTrait} 是 record，可以直接作为结构化键。 */
    private final Map<PlayerTrait, Credential> credentials = new HashMap<>();

    private final Clock clock;

    public InMemoryLoginCredentialStore() {
        this(Clock.systemUTC());
    }

    /** 注入时钟以便测试控制过期行为。 */
    public InMemoryLoginCredentialStore(@NotNull Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public @NotNull Optional<AccountId> findSessionCredential(@NotNull PlayerTrait trait) {
        Objects.requireNonNull(trait, "trait");

        synchronized (lock) {
            var credential = credentials.get(trait);
            if (credential == null) {
                return Optional.empty();
            }

            if (credential.isExpiredAt(clock.instant())) {
                credentials.remove(trait);
                return Optional.empty();
            }

            return Optional.of(credential.sessionCredential);
        }
    }

    @Override
    public void issue(
            @NotNull PlayerTrait trait,
            @NotNull AccountId sessionCredential,
            @NotNull Duration lifetime
    ) {
        Objects.requireNonNull(trait, "trait");
        requireAccount(sessionCredential);
        requirePositive(lifetime);

        var now = clock.instant();
        var issued = new Credential(sessionCredential, now.plus(lifetime));

        synchronized (lock) {
            credentials.values().removeIf(credential -> credential.isExpiredAt(now));
            credentials.put(trait, issued);
        }
    }

    @Override
    public boolean revoke(@NotNull PlayerTrait trait) {
        Objects.requireNonNull(trait, "trait");

        synchronized (lock) {
            return credentials.remove(trait) != null;
        }
    }

    private static void requireAccount(@NotNull AccountId accountId) {
        Objects.requireNonNull(accountId, "sessionCredential");
        Objects.requireNonNull(accountId.authType(), "sessionCredential.authType");
        Objects.requireNonNull(accountId.subject(), "sessionCredential.subject");
    }

    private static void requirePositive(@NotNull Duration lifetime) {
        Objects.requireNonNull(lifetime, "lifetime");
        if (lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("lifetime must be positive, but was " + lifetime);
        }
    }

    private record Credential(AccountId sessionCredential, Instant expiresAt) {

        private boolean isExpiredAt(Instant now) {
            return !now.isBefore(expiresAt);
        }
    }
}
