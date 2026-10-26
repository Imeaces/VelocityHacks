package org.imeaces.levellogin.profile;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * {@link ProfileIdentityStore} 的纯内存实现。
 *
 * <p>进程退出后所有绑定关系都会丢失，因此它只适合开发阶段；存储格式一旦确定，
 * 应当由 SQLite 之类的实现接管，本类退化为测试替身。
 *
 * <p>所有公开方法都在同一把锁下执行。登录路径上的查询频率远低于网络往返，
 * 用一把锁换"复合操作天然原子"是划算的——绑定时需要同时检查并更新三张索引，
 * 用并发容器拼出这个原子性反而更容易出错。
 */
public final class InMemoryProfileIdentityStore implements ProfileIdentityStore {

    private final Object lock = new Object();

    /** 账户 → 身份 UUID。 */
    private final Map<AccountId, UUID> ownerByAccount = new HashMap<>();

    /** 小写玩家名 → 身份 UUID。 */
    private final Map<String, UUID> ownerByPlayerName = new HashMap<>();

    /** 身份 UUID → 身份内容。 */
    private final Map<UUID, IdentityEntry> identities = new HashMap<>();

    @Override
    public @NotNull Optional<ProfileIdentity> findIdentity(@NotNull AccountId accountId) {
        requireAccount(accountId);

        synchronized (lock) {
            return identityOrEmpty(ownerByAccount.get(accountId));
        }
    }

    @Override
    public @NotNull Optional<ProfileIdentity> findIdentityByPlayerName(@NotNull String playerName) {
        requirePlayerName(playerName);

        synchronized (lock) {
            return identityOrEmpty(ownerByPlayerName.get(normalize(playerName)));
        }
    }

    @Override
    public @NotNull Optional<ProfileIdentity> findIdentityByUniqueId(@NotNull UUID playerUniqueId) {
        Objects.requireNonNull(playerUniqueId, "playerUniqueId");

        synchronized (lock) {
            return identityOrEmpty(playerUniqueId);
        }
    }

    @Override
    public @NotNull Collection<ProfileIdentity> identities() {
        synchronized (lock) {
            return identities.values().stream()
                    .map(InMemoryProfileIdentityStore::toIdentity)
                    .toList();
        }
    }

    @Override
    public @NotNull Set<AccountId> findBoundAccounts(@NotNull UUID playerUniqueId) {
        Objects.requireNonNull(playerUniqueId, "playerUniqueId");

        synchronized (lock) {
            var entry = identities.get(playerUniqueId);
            return entry == null ? Set.of() : Set.copyOf(entry.accounts);
        }
    }

    @Override
    public @NotNull ProfileIdentity createIdentity(
            @NotNull UUID playerUniqueId,
            @NotNull String playerName,
            @NotNull AccountId initialAccount
    ) {
        Objects.requireNonNull(playerUniqueId, "playerUniqueId");
        requirePlayerName(playerName);
        requireAccount(initialAccount);

        synchronized (lock) {
            if (identities.containsKey(playerUniqueId)) {
                throw new IllegalStateException(
                        "player unique id " + playerUniqueId + " is already registered"
                );
            }

            var nameKey = normalize(playerName);
            if (ownerByPlayerName.containsKey(nameKey)) {
                throw new IllegalStateException(
                        "player name " + playerName + " is already claimed by "
                                + ownerByPlayerName.get(nameKey)
                );
            }

            var accountOwner = ownerByAccount.get(initialAccount);
            if (accountOwner != null) {
                throw new IllegalStateException(
                        "account " + initialAccount + " is already bound to " + accountOwner
                );
            }

            var entry = new IdentityEntry(playerUniqueId, playerName);
            entry.accounts.add(initialAccount);

            identities.put(playerUniqueId, entry);
            ownerByPlayerName.put(nameKey, playerUniqueId);
            ownerByAccount.put(initialAccount, playerUniqueId);

            return toIdentity(entry);
        }
    }

    @Override
    public @NotNull ProfileIdentity createIdentity(
            @NotNull String playerName,
            @NotNull AccountId initialAccount
    ) {
        return createIdentity(UUID.randomUUID(), playerName, initialAccount);
    }

    @Override
    public @NotNull ProfileIdentity bindAccount(
            @NotNull UUID playerUniqueId,
            @NotNull AccountId accountId
    ) {
        Objects.requireNonNull(playerUniqueId, "playerUniqueId");
        requireAccount(accountId);

        synchronized (lock) {
            var entry = identities.get(playerUniqueId);
            if (entry == null) {
                throw new IllegalStateException("no identity registered for " + playerUniqueId);
            }

            var accountOwner = ownerByAccount.get(accountId);
            if (playerUniqueId.equals(accountOwner)) {
                // 已经绑在同一个身份上：幂等。
                return toIdentity(entry);
            }

            if (accountOwner != null) {
                throw new IllegalStateException(
                        "account " + accountId + " is already bound to " + accountOwner
                );
            }

            entry.accounts.add(accountId);
            ownerByAccount.put(accountId, playerUniqueId);

            return toIdentity(entry);
        }
    }

    @Override
    public boolean releaseAccount(@NotNull AccountId accountId) {
        requireAccount(accountId);

        synchronized (lock) {
            var playerUniqueId = ownerByAccount.remove(accountId);
            if (playerUniqueId == null) {
                return false;
            }

            var entry = identities.get(playerUniqueId);
            if (entry != null) {
                entry.accounts.remove(accountId);
            }

            return true;
        }
    }

    private @NotNull Optional<ProfileIdentity> identityOrEmpty(UUID playerUniqueId) {
        if (playerUniqueId == null) {
            return Optional.empty();
        }

        var entry = identities.get(playerUniqueId);
        return entry == null ? Optional.empty() : Optional.of(toIdentity(entry));
    }

    private static @NotNull ProfileIdentity toIdentity(IdentityEntry entry) {
        return new ProfileIdentity(entry.playerUniqueId, entry.playerName);
    }

    private static @NotNull String normalize(@NotNull String playerName) {
        // Minecraft 的名字只可能是 ASCII，用 ROOT 避免默认 Locale 参与大小写折叠。
        return playerName.toLowerCase(Locale.ROOT);
    }

    private static void requirePlayerName(@NotNull String playerName) {
        Objects.requireNonNull(playerName, "playerName");
        if (playerName.isBlank()) {
            throw new IllegalArgumentException("playerName must not be blank");
        }
    }

    private static void requireAccount(@NotNull AccountId accountId) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(accountId.authType(), "accountId.authType");
        Objects.requireNonNull(accountId.subject(), "accountId.subject");
    }

    /**
     * 存储内部的实体。
     *
     * <p>它比 {@link ProfileIdentity} 多持有一份账户集合，因为绑定关系需要这份反查索引；
     * 但集合只会经 {@link #findBoundAccounts(UUID)} 以拷贝形式离开本类，
     * 不会被塞回 {@link ProfileIdentity}。
     */
    private static final class IdentityEntry {

        private final UUID playerUniqueId;
        private final String playerName;
        private final Set<AccountId> accounts = new LinkedHashSet<>();

        private IdentityEntry(UUID playerUniqueId, String playerName) {
            this.playerUniqueId = playerUniqueId;
            this.playerName = playerName;
        }
    }
}
