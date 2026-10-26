package org.imeaces.levellogin.profile;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家身份（{@link ProfileIdentity}）与外部账户（{@link AccountId}）之间的绑定关系。
 *
 * <p>这一层只回答"这个账户是谁""这个名字是谁"，<b>不回答"凭什么相信他是谁"</b>：
 * 信任判定留在调用方，存储层不参与认证决策。
 *
 * <p>下面三条不变量由实现负责维持，违反时抛出 {@link IllegalStateException}：
 * <ul>
 *   <li>一个 {@link AccountId} 至多绑定到一个身份；</li>
 *   <li>一个 {@code playerUniqueId} 至多对应一个身份；</li>
 *   <li>一个玩家名至多被一个身份占用，比较时不区分大小写。</li>
 * </ul>
 *
 * <p>{@link ProfileIdentity} 本身是不变值；只有 {@link #findBoundAccounts(UUID)} 返回的集合
 * 是某一刻的关系快照。
 */
public interface ProfileIdentityStore {

    /**
     * 按外部账户查找绑定到它的身份。
     */
    @NotNull Optional<ProfileIdentity> findIdentity(@NotNull AccountId accountId);

    /**
     * 按玩家名查找身份，比较时不区分大小写。
     */
    @NotNull Optional<ProfileIdentity> findIdentityByPlayerName(@NotNull String playerName);

    /**
     * 按玩家 UUID 查找身份。
     */
    @NotNull Optional<ProfileIdentity> findIdentityByUniqueId(@NotNull UUID playerUniqueId);

    /**
     * 当前已知的全部身份快照。
     */
    @NotNull Collection<ProfileIdentity> identities();

    /**
     * 反查一个身份绑定过的全部账户。
     *
     * <p>这是"绑定关系"唯一的方向性查询：身份不持有账户集合，需要时来问这里。
     */
    @NotNull Set<AccountId> findBoundAccounts(@NotNull UUID playerUniqueId);

    /**
     * 建立一个新的身份，并立即把 {@code initialAccount} 绑定到它。
     *
     * @param playerUniqueId 进游戏后使用的 UUID。对离线玩家而言这是本存储存在的全部理由：
     *                       没有外部权威能给出它，只能在这里固定下来，否则每次登录都会换一个身份。
     * @throws IllegalStateException 当该 UUID、该玩家名或该账户已经被占用时
     */
    @NotNull ProfileIdentity createIdentity(
            @NotNull UUID playerUniqueId,
            @NotNull String playerName,
            @NotNull AccountId initialAccount
    );

    /**
     * 建立一个使用随机 UUID 的新身份，并立即把 {@code initialAccount} 绑定到它。
     *
     * <p>只应当在确实没有任何外部权威可以提供 UUID 时使用（例如后续自行注册的离线账户）。
     * {@link #createIdentity(UUID, String, AccountId)} 则用于 Mojang、Floodgate 这类
     * UUID 由外部给出的场景。
     */
    @NotNull ProfileIdentity createIdentity(
            @NotNull String playerName,
            @NotNull AccountId initialAccount
    );

    /**
     * 把一个账户绑定到已有身份。
     *
     * <p>若该账户已经绑定到同一身份，则为幂等操作并直接返回当前身份。
     *
     * @throws IllegalStateException 当身份不存在，或该账户已绑定到另一个身份时
     */
    @NotNull ProfileIdentity bindAccount(@NotNull UUID playerUniqueId, @NotNull AccountId accountId);

    /**
     * 解除一个账户的绑定。
     *
     * <p>不会因此删除身份：身份仍可通过玩家名或 UUID 找到。进游戏用的是 UUID，
     * 删掉一个暂时没有账户的身份只会让它对应的世界数据变成孤儿。
     *
     * @return 该账户原本存在绑定并已解除时返回 {@code true}
     */
    boolean releaseAccount(@NotNull AccountId accountId);
}
