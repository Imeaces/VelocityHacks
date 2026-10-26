package org.imeaces.levellogin.auth;

import org.imeaces.levellogin.profile.AccountId;
import org.imeaces.levellogin.profile.PlayerTrait;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;

/**
 * 登录凭据：把一次传入连接的"仅与连接相关的信息"（{@link PlayerTrait}）关联到一个
 * 已经验证过的会话凭据（{@link AccountId}）。
 *
 * <p>见 {@code docs/基本原理.md} 的"基于连接特征的凭据登录"。它不是身份存储，两者的差别是本质的：
 * <ul>
 *   <li>身份存储里的绑定是长期的，来源是外部权威（Yggdrasil、Floodgate）；</li>
 *   <li>这里的每一条都<b>只活一小段时间</b>，而且必须由一次<b>已经完成的认证</b>换来——
 *       玩家在游戏内完成认证后被踢出，再次连接时连接特征命中，我们才采信它关联的会话凭据。</li>
 * </ul>
 *
 * <p>因此条目本身就是授权证据：<b>凭据存在 == 之前确实有一次成功认证</b>。
 * 取用时不需要再对 {@link AccountId#authType()} 做甄别，也不需要反查身份存储。
 */
public interface LoginCredentialStore {

    /**
     * 按完整连接特征查找关联的会话凭据。
     *
     * <p><b>整体匹配，不使用 {@link PlayerTrait} 的任意单个字段。</b>
     * 这份特征之所以能被当作指纹，正是因为它由若干互不相关的观察值拼成；
     * 单独取其中一个字段既会误伤（同名的正常玩家），也会被轻易伪造。
     *
     * <p>已过期的条目视同不存在，并会在此时被丢弃。
     */
    @NotNull Optional<AccountId> findSessionCredential(@NotNull PlayerTrait trait);

    /**
     * 记录一份登录凭据。
     *
     * <p>只应当在玩家完成认证之后调用；这是本存储唯一的写入入口，
     * 因此它的调用点就是"什么算认证通过"的定义所在。
     *
     * @param lifetime 有效期。登录凭据是短期凭据，不接受无限期，必须由调用方给出明确的时间
     */
    void issue(
            @NotNull PlayerTrait trait,
            @NotNull AccountId sessionCredential,
            @NotNull Duration lifetime
    );

    /**
     * 撤销一份登录凭据。
     *
     * @return 原本存在并已撤销时返回 {@code true}
     */
    boolean revoke(@NotNull PlayerTrait trait);
}
