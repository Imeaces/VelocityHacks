package org.imeaces.levellogin.profile;

import java.util.UUID;

/**
 * 玩家身份本身。
 *
 * <p>{@code playerUniqueId} 才是身份：进游戏用的是它，世界数据也都挂在它上面。
 * {@code playerName} 只是它当前的属性——正版玩家可以改名，将来这个名字会搬到
 * 独立的"名字占用"关系里，届时本类型只剩 UUID 一个字段。
 *
 * <p>这里不包含"绑定了哪些账户"：账户绑定是身份与外部账户之间的**关系**，
 * 不是身份自身的属性。需要反查时用 {@link ProfileIdentityStore#findBoundAccounts(UUID)}。
 */
public record ProfileIdentity(
        UUID playerUniqueId,
        String playerName
) {
}
