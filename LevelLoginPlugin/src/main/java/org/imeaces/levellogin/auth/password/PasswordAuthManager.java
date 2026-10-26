package org.imeaces.levellogin.auth.password;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.imeaces.levellogin.LevelLoginMain;
import org.imeaces.levellogin.config.LevelLoginConfig;
import org.imeaces.levellogin.data.ChannelKeys;
import org.imeaces.levellogin.profile.AccountId;
import org.imeaces.levellogin.profile.storage.PlayerProfile;
import org.imeaces.levellogin.profile.storage.ProfileStore;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 密码认证管理器
 *
 * 与现有架构的整合方式：
 * 1. PreLoginEvent 阶段：AccountManager.OfflineSessionAuthListener 检测到用户名匹配已有密码档案
 *    → 设置 PendingPasswordAuthKey=true 到 Channel
 * 2. 登录完成后（LoginEvent）：PostLoginAuthListener 检查 PendingPasswordAuthKey
 *    → 调用 requireAuth() 开始密码挑战
 * 3. 玩家发送聊天消息 → ChatListener 拦截 → 验证密码
 * 4. 认证成功 → 更新档案，解除 pending 状态
 *    认证失败 → 重试或踢出
 */
public class PasswordAuthManager {

    private final ProfileStore profileStore;
    private final LevelLoginConfig config;

    /** 待认证玩家 -> 重试次数 */
    private final Map<String, Integer> pendingAuth;

    /** 待认证玩家 -> 用户名（可能与 player.getUsername() 不同） */
    private final Map<String, String> pendingUsernames;

    public PasswordAuthManager(ProfileStore profileStore, LevelLoginConfig config) {
        this.profileStore = profileStore;
        this.config = config;
        this.pendingAuth = new ConcurrentHashMap<>();
        this.pendingUsernames = new ConcurrentHashMap<>();
    }

    /**
     * 标记玩家需要密码认证
     */
    public void requireAuth(Player player, String username) {
        pendingAuth.put(player.getUsername(), 0);
        pendingUsernames.put(player.getUsername(), username);

        player.sendMessage(Component.text(config.getPasswordPromptMessage())
                .color(NamedTextColor.YELLOW));

        LevelLoginMain.getLogger().info("Password auth required for player {}", username);
    }

    /**
     * 检查玩家是否需要密码认证
     */
    public boolean needsAuth(String username) {
        return pendingAuth.containsKey(username) && pendingUsernames.containsKey(username);
    }

    /**
     * 移除玩家的待认证状态（用于清理，例如玩家断开连接时）
     */
    public void clearPending(String username) {
        pendingAuth.remove(username);
        pendingUsernames.remove(username);
    }

    /**
     * 处理密码输入
     */
    private void handlePassword(Player player, String password) {
        String username = pendingUsernames.get(player.getUsername());
        int retries = pendingAuth.getOrDefault(player.getUsername(), 0);

        AccountId link = new AccountId("password", username.toLowerCase());
        PlayerProfile profile = profileStore.findByLink(link).orElse(null);

        if (profile == null) {
            if (config.isPasswordAutoRegister()) {
                // 新用户，创建"账户关联档案"
                LevelLoginMain.getLogger().info("Auto-registering new profile for {}", username);
                profile = new PlayerProfile();
                profile.setPreferredUsername(username);
                profile.addLink(link);

                String passwordHash = PasswordHashUtil.hashPassword(
                        password, username.toLowerCase(), config.getServerSecret());
                profile.setProperty("passwordHash", passwordHash);
                profile.addGroup("default");
                profileStore.save(profile);

                successAuth(player, profile, true);
            } else {
                failAuth(player, "该用户名未注册");
            }
            return;
        }

        // 验证密码
        String storedHash = profile.getProperty("passwordHash").orElse("");
        if (PasswordHashUtil.verifyPassword(password, username.toLowerCase(), config.getServerSecret(), storedHash)) {
            LevelLoginMain.getLogger().info("Password auth success for {}", username);
            successAuth(player, profile, false);
        } else {
            retries++;
            pendingAuth.put(player.getUsername(), retries);

            if (retries >= config.getPasswordMaxRetries()) {
                failAuth(player, "密码错误次数过多");
            } else {
                player.sendMessage(Component.text(
                        "密码错误，请重试（" + retries + "/" + config.getPasswordMaxRetries() + "）")
                        .color(NamedTextColor.RED));
            }
        }
    }

    private void successAuth(Player player, PlayerProfile profile, boolean isNew) {
        profile.markActive();
        profileStore.save(profile);

        pendingAuth.remove(player.getUsername());
        pendingUsernames.remove(player.getUsername());

        // 清除 Channel 上的 pending 标记
        // 注意：LoginInboundConnection 在 LoginEvent 之后可能已经变了
        // 但 pending 状态已经通过内存 Map 管理，Channel 标记只是给初始化阶段用的

        if (isNew) {
            player.sendMessage(Component.text("注册成功！欢迎，" + profile.getPreferredUsername())
                    .color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("认证成功！欢迎回来，" + profile.getPreferredUsername())
                    .color(NamedTextColor.GREEN));
        }
    }

    private void failAuth(Player player, String reason) {
        pendingAuth.remove(player.getUsername());
        pendingUsernames.remove(player.getUsername());

        player.sendMessage(Component.text("认证失败：" + reason)
                .color(NamedTextColor.RED));
        player.disconnect(Component.text("认证失败：" + reason)
                .color(NamedTextColor.RED));
    }

    /**
     * 聊天事件监听器 - 拦截待认证玩家的消息
     */
    public class ChatListener {
        @Subscribe
        public void onPlayerChat(PlayerChatEvent event) {
            Player player = event.getPlayer();

            if (!needsAuth(player.getUsername())) {
                return;
            }

            // 取消聊天事件（不让消息发到游戏里）
            event.setResult(PlayerChatEvent.ChatResult.denied());

            String password = event.getMessage();
            handlePassword(player, password);
        }
    }

    /**
     * 服务器连接前检查 - 阻止未认证玩家切换服务器
     */
    public class ServerConnectListener {
        @Subscribe
        public void onServerPreConnect(ServerPreConnectEvent event) {
            if (needsAuth(event.getPlayer().getUsername())) {
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
                event.getPlayer().sendMessage(Component.text("请先完成密码认证")
                        .color(NamedTextColor.RED));
            }
        }
    }
}
