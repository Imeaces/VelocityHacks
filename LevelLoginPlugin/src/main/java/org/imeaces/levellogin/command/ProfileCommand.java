package org.imeaces.levellogin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.imeaces.levellogin.auth.password.PasswordHashUtil;
import org.imeaces.levellogin.config.LevelLoginConfig;
import org.imeaces.levellogin.profile.AccountId;
import org.imeaces.levellogin.profile.storage.PlayerProfile;
import org.imeaces.levellogin.profile.storage.ProfileStore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * /profile 命令 - 管理玩家档案
 *
 * 子命令：
 * - /profile view [username]               查看档案
 * - /profile setname <name>                设置首选用户名
 * - /profile changepass <old> <new>        修改密码
 * - /profile link <authType> <subject>     把当前登录指向另一个档案
 * - /profile unlink <authType>             解除某个认证关联
 */
public class ProfileCommand {

    private final ProfileStore profileStore;
    private final LevelLoginConfig config;

    public ProfileCommand(ProfileStore profileStore, LevelLoginConfig config) {
        this.profileStore = profileStore;
        this.config = config;
    }

    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("profile")
                .requires(source -> source instanceof Player)

                // /profile view [target]
                .then(LiteralArgumentBuilder.<CommandSource>literal("view")
                        .executes(ctx -> viewProfile((Player) ctx.getSource(), ((Player) ctx.getSource()).getUsername()))
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("target", StringArgumentType.word())
                                .requires(source -> source.hasPermission("levellogin.profile.view.others"))
                                .executes(ctx -> viewProfile((Player) ctx.getSource(),
                                        StringArgumentType.getString(ctx, "target")))))

                // /profile setname <name>
                .then(LiteralArgumentBuilder.<CommandSource>literal("setname")
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("name", StringArgumentType.string())
                                .executes(ctx -> setPreferredName((Player) ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))

                // /profile changepass <oldpass> <newpass>
                .then(LiteralArgumentBuilder.<CommandSource>literal("changepass")
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("oldpass", StringArgumentType.word())
                                .then(RequiredArgumentBuilder
                                        .<CommandSource, String>argument("newpass", StringArgumentType.word())
                                        .executes(ctx -> changePassword((Player) ctx.getSource(),
                                                StringArgumentType.getString(ctx, "oldpass"),
                                                StringArgumentType.getString(ctx, "newpass"))))))

                // /profile link <authType> <subject>
                // 把当前玩家的认证指向另一个档案：
                // 找到目标档案后，将 authType:subject 关联添加到当前玩家的档案上
                // 如果目标档案不存在则创建新的
                .then(LiteralArgumentBuilder.<CommandSource>literal("link")
                        .requires(source -> source.hasPermission("levellogin.profile.link"))
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("authType", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("yggdrasil-mojang");
                                    builder.suggest("floodgate");
                                    builder.suggest("password");
                                    builder.suggest("discord");
                                    builder.suggest("github");
                                    return builder.buildFuture();
                                })
                                .then(RequiredArgumentBuilder
                                        .<CommandSource, String>argument("subject", StringArgumentType.greedyString())
                                        .executes(ctx -> linkAccount((Player) ctx.getSource(),
                                                StringArgumentType.getString(ctx, "authType"),
                                                StringArgumentType.getString(ctx, "subject"))))))

                // /profile unlink <authType>
                .then(LiteralArgumentBuilder.<CommandSource>literal("unlink")
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("authType", StringArgumentType.word())
                                .executes(ctx -> unlinkAccount((Player) ctx.getSource(),
                                        StringArgumentType.getString(ctx, "authType")))))

                // /profile pointto <targetUsername>
                // 核心功能：把当前登录认证指向另一个玩家的档案
                // 例如：基岩版登录时自动使用 Java 版账号的档案
                .then(LiteralArgumentBuilder.<CommandSource>literal("pointto")
                        .requires(source -> source.hasPermission("levellogin.profile.pointto"))
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("target", StringArgumentType.word())
                                .executes(ctx -> pointToProfile((Player) ctx.getSource(),
                                        StringArgumentType.getString(ctx, "target")))))

                .build();

        return new BrigadierCommand(node);
    }

    /**
     * 递归检测是否会产生循环引用
     * 从 targetProfile 开始沿 profileLink 链走，如果最终指回 sourceProfileId 则有循环
     */
    private boolean wouldCreateCycle(String sourceProfileId, PlayerProfile targetProfile) {
        Set<String> visited = new HashSet<>();
        visited.add(sourceProfileId);

        PlayerProfile current = targetProfile;
        int depth = 0;
        while (current != null && depth < 10) {
            if (visited.contains(current.getProfileId())) {
                return true; // 指回了源头，循环
            }
            visited.add(current.getProfileId());
            String nextLink = current.getProperty("profileLink").orElse(null);
            current = nextLink != null ? profileStore.findById(nextLink).orElse(null) : null;
            depth++;
        }
        return false;
    }

    // ==================== 子命令实现 ====================

    private int viewProfile(Player player, String targetUsername) {
        List<PlayerProfile> profiles = profileStore.findByUsername(targetUsername);

        if (profiles.isEmpty()) {
            // 也尝试按 AccountId subject 查找
            profiles = profileStore.getAll().stream()
                    .filter(p -> p.getAccountLinks().stream()
                            .anyMatch(l -> l.subject().equalsIgnoreCase(targetUsername)))
                    .toList();
        }

        if (profiles.isEmpty()) {
            player.sendMessage(Component.text("未找到玩家 '" + targetUsername + "' 的档案")
                    .color(NamedTextColor.RED));
            return 0;
        }

        PlayerProfile profile = profiles.get(0);

        player.sendMessage(Component.text("=== 玩家档案 ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("档案ID: ").color(NamedTextColor.GRAY)
                .append(Component.text(profile.getProfileId()).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("用户名: ").color(NamedTextColor.GRAY)
                .append(Component.text(profile.getPreferredUsername()).color(NamedTextColor.GREEN)));
        player.sendMessage(Component.text("关联账户: ").color(NamedTextColor.GRAY));

        for (AccountId link : profile.getAccountLinks()) {
            player.sendMessage(Component.text("  - " + link.authType() + ": " + link.subject())
                    .color(NamedTextColor.AQUA));
        }

        // 显示档案指向
        String profileLink = profile.getProperty("profileLink").orElse(null);
        if (profileLink != null) {
            player.sendMessage(Component.text("指向档案: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(profileLink).color(NamedTextColor.WHITE)));

            PlayerProfile target = profileStore.findById(profileLink).orElse(null);
            if (target != null) {
                player.sendMessage(Component.text("  → 用户名: " + target.getPreferredUsername())
                        .color(NamedTextColor.GRAY));
            }
        }

        if (!profile.getGroups().isEmpty()) {
            player.sendMessage(Component.text("权限组: ").color(NamedTextColor.GRAY)
                    .append(Component.text(String.join(", ", profile.getGroups())).color(NamedTextColor.YELLOW)));
        }

        player.sendMessage(Component.text("创建时间: ").color(NamedTextColor.GRAY)
                .append(Component.text(profile.getCreatedAt().toString()).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("最后活跃: ").color(NamedTextColor.GRAY)
                .append(Component.text(profile.getLastActiveAt().toString()).color(NamedTextColor.WHITE)));

        return 1;
    }

    private int setPreferredName(Player player, String newName) {
        // 通过原始 Minecraft 用户名查找档案（不是 preferredUsername）
        List<PlayerProfile> profiles = profileStore.findByMinecraftUsername(player.getUsername());

        if (profiles.isEmpty()) {
            player.sendMessage(Component.text("未找到你的档案").color(NamedTextColor.RED));
            return 0;
        }

        PlayerProfile profile = profiles.get(0);
        String oldName = profile.getPreferredUsername();
        profile.setPreferredUsername(newName);
        profileStore.save(profile);

        player.sendMessage(Component.text("显示名已更改: " + oldName + " → " + newName)
                .color(NamedTextColor.GREEN));
        return 1;
    }

    private int changePassword(Player player, String oldPass, String newPass) {
        // 通过原始 Minecraft 用户名查找档案
        List<PlayerProfile> profiles = profileStore.findByMinecraftUsername(player.getUsername());

        if (profiles.isEmpty()) {
            player.sendMessage(Component.text("未找到你的档案").color(NamedTextColor.RED));
            return 0;
        }

        PlayerProfile profile = profiles.get(0);
        String storedHash = profile.getProperty("passwordHash").orElse(null);

        if (storedHash == null) {
            player.sendMessage(Component.text("你的档案没有密码认证").color(NamedTextColor.RED));
            return 0;
        }

        // 验证旧密码
        String salt = player.getUsername().toLowerCase();
        if (!PasswordHashUtil.verifyPassword(oldPass, salt, config.getServerSecret(), storedHash)) {
            player.sendMessage(Component.text("旧密码错误").color(NamedTextColor.RED));
            return 0;
        }

        // 更新密码
        String newHash = PasswordHashUtil.hashPassword(newPass, salt, config.getServerSecret());
        profile.setProperty("passwordHash", newHash);
        profileStore.save(profile);

        player.sendMessage(Component.text("密码已更新").color(NamedTextColor.GREEN));
        return 1;
    }

    /**
     * 关联账户到当前档案
     * 用法：/profile link floodgate 25354734765
     * 效果：在当前档案上添加 floodgate:XUID 关联，以后基岩版登录自动用这个档案
     */
    private int linkAccount(Player player, String authType, String subject) {
        // 通过原始 Minecraft 用户名查找档案
        List<PlayerProfile> profiles = profileStore.findByMinecraftUsername(player.getUsername());

        if (profiles.isEmpty()) {
            player.sendMessage(Component.text("未找到你的档案").color(NamedTextColor.RED));
            return 0;
        }

        PlayerProfile profile = profiles.get(0);
        AccountId newLink = new AccountId(authType, subject);

        // 检查是否已被其他档案关联
        java.util.Optional<PlayerProfile> existing = profileStore.findByLink(newLink);
        if (existing.isPresent() && !existing.get().getProfileId().equals(profile.getProfileId())) {
            player.sendMessage(Component.text("该账户已被其他档案关联，请先解除关联")
                    .color(NamedTextColor.RED));
            return 0;
        }

        profile.addLink(newLink);
        profileStore.save(profile);

        player.sendMessage(Component.text("已关联 " + authType + ":" + subject + " 到你的档案")
                .color(NamedTextColor.GREEN));
        return 1;
    }

    /**
     * 解除认证关联
     */
    private int unlinkAccount(Player player, String authType) {
        // 通过原始 Minecraft 用户名查找档案
        List<PlayerProfile> profiles = profileStore.findByMinecraftUsername(player.getUsername());

        if (profiles.isEmpty()) {
            player.sendMessage(Component.text("未找到你的档案").color(NamedTextColor.RED));
            return 0;
        }

        PlayerProfile profile = profiles.get(0);
        java.util.Optional<AccountId> link = profile.findLink(authType);

        if (link.isEmpty()) {
            player.sendMessage(Component.text("你的档案没有 " + authType + " 关联").color(NamedTextColor.RED));
            return 0;
        }

        profile.removeLink(link.get());
        profileStore.save(profile);

        player.sendMessage(Component.text("已解除 " + authType + " 关联").color(NamedTextColor.GREEN));
        return 1;
    }

    /**
     * 核心功能：把当前登录认证指向另一个玩家的档案
     *
     * 例如：/profile pointto Steve
     * 效果：你（基岩版）下次登录时，会使用 Steve 的档案（UUID、权限组等）
     *
     * 实现方式：在当前档案的 properties 里设置 profileLink 指向目标档案 ID
     * 当 AccountManager.grabGameProfile() 处理时，会解析这个指向
     */
    private int pointToProfile(Player player, String targetUsername) {
        // 通过原始 Minecraft 用户名查找档案
        List<PlayerProfile> myProfiles = profileStore.findByMinecraftUsername(player.getUsername());
        List<PlayerProfile> targetProfiles = profileStore.findByUsername(targetUsername);

        if (myProfiles.isEmpty()) {
            player.sendMessage(Component.text("未找到你的档案").color(NamedTextColor.RED));
            return 0;
        }

        if (targetProfiles.isEmpty()) {
            player.sendMessage(Component.text("未找到目标玩家 '" + targetUsername + "' 的档案")
                    .color(NamedTextColor.RED));
            return 0;
        }

        PlayerProfile myProfile = myProfiles.get(0);
        PlayerProfile targetProfile = targetProfiles.get(0);

        if (myProfile.getProfileId().equals(targetProfile.getProfileId())) {
            player.sendMessage(Component.text("不能指向自己的档案").color(NamedTextColor.RED));
            return 0;
        }

        // 防止循环指向：递归检查目标档案的指向链是否包含自己的档案
        if (wouldCreateCycle(myProfile.getProfileId(), targetProfile)) {
            player.sendMessage(Component.text("会产生循环引用，操作已拒绝")
                    .color(NamedTextColor.RED));
            return 0;
        }

        myProfile.setProperty("profileLink", targetProfile.getProfileId());
        profileStore.save(myProfile);

        player.sendMessage(Component.text("你的登录现在指向档案: " + targetProfile.getPreferredUsername())
                .color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("下次登录时将使用该档案的 UUID 和权限")
                .color(NamedTextColor.GRAY));
        return 1;
    }
}
