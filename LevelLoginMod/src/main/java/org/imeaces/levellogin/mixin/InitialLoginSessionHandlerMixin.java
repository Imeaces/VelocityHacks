package org.imeaces.levellogin.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequestPacket;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.imeaces.levellogin.LevelLoginMain;
import org.imeaces.levellogin.data.ChannelKeys;
import org.imeaces.levellogin.data.LevelLoginConfigurations;
import org.imeaces.levellogin.handle.HandleChannelAccountId;
import org.imeaces.levellogin.handle.HandleGrabGameProfile;
import org.imeaces.levellogin.handle.HandleHasJoined;
import org.imeaces.levellogin.profile.AccountId;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(InitialLoginSessionHandler.class)
public abstract class InitialLoginSessionHandlerMixin {

    @Shadow
    private byte[] verify;
    @Shadow
    private MinecraftConnection mcConnection;
    @Shadow
    private VelocityServer server;
    @Shadow
    private LoginInboundConnection inbound;
    @Shadow
    private ServerLoginPacket login;

    @Shadow
    private native EncryptionRequestPacket generateEncryptionRequest();
    @Shadow
    private InitialLoginSessionHandler.LoginState currentState;

    @Redirect(
            method = "lambda$handle$2(Lcom/velocitypowered/api/event/connection/PreLoginEvent$PreLoginComponentResult;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/velocitypowered/api/event/connection/PreLoginEvent$PreLoginComponentResult;isForceOfflineMode()Z"
            )
    )
    private boolean levelLogin$forceOfflineMode(
            PreLoginEvent.PreLoginComponentResult result
    ) {
        // 总是使用离线模式，以便自行处理逻辑
        LevelLoginMain.getLogger().trace("force offline mode condition branch");

        return true;
    }

    @Inject(
            method = "lambda$handle$2(Lcom/velocitypowered/api/event/connection/PreLoginEvent$PreLoginComponentResult;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/velocitypowered/proxy/connection/MinecraftConnection;setActiveSessionHandler(Lcom/velocitypowered/proxy/protocol/StateRegistry;Lcom/velocitypowered/proxy/connection/MinecraftSessionHandler;)V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void levelLogin$inject$tryHandleLogin(PreLoginEvent.PreLoginComponentResult result, CallbackInfo ci) {
        ci.cancel(); // always cancel remaining codes under inject point

        var accountId = mcConnection.getChannel().attr(ChannelKeys.AccountIdKey).get();

        // 只有在"没有已确定的账户"且"运行配置要求使用 Yggdrasil 认证"时，才向会话服务器认证。
        // 原版 Velocity 的判定逻辑已被覆盖
        boolean shouldAuthenticate = accountId == null && LevelLoginConfigurations.isYggdrasilLoginEnabled();

        // EncryptionRequest 的 shouldAuthenticate 字段自 1.20.5 起才存在于协议中（见 EncryptionRequestPacket），
        // 更早的客户端收不到它，无法表达"加密但不认证"。
        // 因此只有 1.20.5+ 才可以做到启用加密但不使用认证。
        // 在协议允许且运行配置已启用相关功能时才启用加密，其余情况直接建立离线会话。
        boolean needsEncryption = shouldAuthenticate;
        if (LevelLoginConfigurations.isOfflineAuthWithEncryptionEnabled()
                && mcConnection.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_5))
            needsEncryption = true;

        if (needsEncryption) {
            // Request encryption (may authenticate).

            levelLogin$requestEncryption(shouldAuthenticate);
        } else {
            levelLogin$enterOfflineMode(accountId);
        }
    }

    private void levelLogin$enterOfflineMode(@Nullable AccountId accountId) {
        // TODO: 实现基于连接特征的凭据登录

        if (accountId != null) {
                /*
                  assume offline because found accountId before authenticate.

                  it might come from floodgate, but not important for us. see also FgVelocityListenerMixin
                 */

            LevelLoginMain.getLogger().info("enter offline mode for player {} because found an accountId: {}",
                    login.getUsername(), accountId);

            levelLogin$startSession(accountId, false, null);
        } else {
            // 既没有解析出账户，也不要求在线认证：完全离线放行，
            // 让玩家先进入游戏，再在游戏内完成认证（例如传统 /login，或设想中的 WebAuth）。

            LevelLoginMain.getLogger().info("enter offline mode for player {}", login.getUsername());

            levelLogin$startSession(null);
        }
    }

    private void levelLogin$requestEncryption(boolean shouldAuthenticate){
        LevelLoginMain.getLogger().info(
                "request encryption for player {}, shouldAuthenticate: {}",
                login.getUsername(), shouldAuthenticate
        );

        mcConnection.getChannel().attr(ChannelKeys.ShouldAuthenticateKey).set(shouldAuthenticate);
        EncryptionRequestPacket request = generateEncryptionRequest();
        request.shouldAuthenticate = shouldAuthenticate;
        verify = Arrays.copyOf(request.getVerifyToken(), 4);
        mcConnection.write(request);
        currentState = InitialLoginSessionHandler.LoginState.ENCRYPTION_REQUEST_SENT;
    }

    @Inject(
            method = "handle(Lcom/velocitypowered/proxy/protocol/packet/EncryptionResponsePacket;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/net/http/HttpRequest;newBuilder()Ljava/net/http/HttpRequest$Builder;",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void levelLogin$inject$handleEncryption(
            EncryptionResponsePacket pk,
            CallbackInfoReturnable<Boolean> ci,
            @Local( name = "serverId" ) String serverId,
            @Local( name = "playerIp" ) String clientIp
    ) {
        // 从这里接管 Velocity 原本的会话认证逻辑。
        // 先取消原实现，指示之后要让 handle() 立即返回，连接停留在 ENCRYPTION_RESPONSE_RECEIVED 状态；
        // 认证结果回来后，再在我们自己的方法中回到 event loop 上完成状态切换。
        ci.setReturnValue(true);
        ci.cancel();

        final String username = login.getUsername();
        var accountId = HandleChannelAccountId.get(mcConnection);
        Boolean shouldAuthenticateAttr =
                mcConnection.getChannel().attr(ChannelKeys.ShouldAuthenticateKey).get();
        // 属性缺失时按"需要认证"处理：这是更安全的一侧，也与 Velocity 自身的默认行为一致。
        boolean shouldAuthenticate = shouldAuthenticateAttr == null || shouldAuthenticateAttr;

        if (accountId != null) {
            LevelLoginMain.getLogger().info("ship to online check logic due to exist accountId: {}", accountId);
        } else if (!shouldAuthenticate) {
            LevelLoginMain.getLogger().info("ship to online check logic due to require skip authenticate for {}", username);
        } else {
            // 没有预先确定的账户，且要求使用 Yggdrasil 在线认证，开始认证过程。
            levelLogin$requestOnlineAuthHasJoined(username, serverId, clientIp);
            return;
        }

        // 注意：下面两个分支都以 isOnlineMode = false 建立会话，却仍然把本次加密握手中的真实 serverId 传下去。
        // 这是刻意的——本项目会发送 shouldAuthenticate = false 的 EncryptionRequest（原版 Velocity 从不这样做），
        // 于是"离线登录"与"确实发生过加密握手"可以同时成立，serverId 因此是真实存在的值。
        // 代价是 LoginEvent#getServerIdHash()（com.velocitypowered.api.event.connection.LoginEvent）会对一个
        // 未经 Mojang 认证的连接返回非 null，不再吻合上游 "offline-mode 时返回 null" 的 javadoc：
        // 上游那条结论的前提是"离线就不会发加密请求"，而该前提在本项目中不成立。
        // 请勿把它"修正"成 null：那只会丢掉真实信息，且不会有任何编译期或运行期报错。
        // （Velocity 自身并不消费这个值：AuthSessionHandler 中 serverIdHash 的唯一用途就是构造 LoginEvent。）
        if (accountId == null) {
            // 使用了离线模式加密，但未能通过凭据登录，进入完全离线的登录模式，并自动构造离线模式玩家配置
            // 以允许在进入游戏后准备进一步认证（例如传统 /login 插件，或者设想中的 WebAuth）
            // TODO: 实现基于连接特征的凭据登录

            levelLogin$startSession(serverId);
        } else {
            // 加密之前就确定了账户（例如 Floodgate），不需要再向会话服务器认证，直接进入下一步。
            levelLogin$startSession(accountId, false, serverId);
        }
    }

    private void levelLogin$requestOnlineAuthHasJoined(
            String username,
            String serverId,
            String clientIp
    ){
        HandleHasJoined.requestHasJoined(username, serverId, clientIp, inbound)
                // 这里用 handleAsync 而不是 whenCompleteAsync：认证请求本身的失败已经由
                // levelLogin$completeOnlineAuthHasJoined 就地处理，若再让它传播下去，下面的兜底会
                // 对同一次失败重复记录日志并重复断开连接。
                .<Void>handleAsync(
                        (authResult, throwable) -> {
                            levelLogin$completeOnlineAuthHasJoined(
                                    authResult,
                                    throwable,
                                    username,
                                    serverId
                            );
                            return null;
                        },
                        mcConnection.eventLoop()
                )
                .exceptionally(throwable -> {
                    // 仅在 levelLogin$completeOnlineAuthHasJoined 自身抛出时才会到达，避免玩家一直卡在登录阶段。
                    LevelLoginMain.getLogger()
                            .error("Unable to complete login for player {}", username, throwable);

                    if (!mcConnection.isClosed()) {
                        inbound.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
                    }

                    return null;
                });
    }

    /**
     * 在 Netty event loop 上处理在线认证的结果，并推进登录状态。
     */
    private void levelLogin$completeOnlineAuthHasJoined(
            HandleHasJoined.HasJoinedOnlineResponseResult authResult,
            Throwable throwable,
            String username,
            String serverId
    ) {
        if (mcConnection.isClosed()) {
            // 认证期间玩家已经断开连接。
            return;
        }

        if (throwable != null) {
            LevelLoginMain.getLogger().error("Unable to authenticate player {}", username, throwable);
            inbound.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
            return;
        }

        switch (authResult){
            case HandleHasJoined.HasJoinedOnlineResponseFailed failedResult -> {
                // 会话服务器返回 204：该玩家并没有以此代理的身份登录。
                inbound.disconnect(
                        Component.translatable("velocity.error.online-mode-only", NamedTextColor.RED)
                );
            }

            case HandleHasJoined.HasJoinedOnlineResponseSuccess successResult -> {
                // 与 Velocity 原实现保持一致：1.19.1+ 的 LINKED_V2 公钥需要绑定到认证得到的玩家 UUID 上。
                var identifiedKey = inbound.getIdentifiedKey();

                if (identifiedKey != null
                        && identifiedKey.getKeyRevision() == IdentifiedKey.Revision.LINKED_V2
                        && identifiedKey instanceof IdentifiedKeyImpl key
                        && !key.internalAddHolder(successResult.verifiedUniqueId())) {
                    inbound.disconnect(
                            Component.translatable("multiplayer.disconnect.invalid_public_key")
                    );
                    return;
                }

                levelLogin$startSession(successResult.accountId(), true, serverId);
            }
        }
    }

    private void levelLogin$startSession(@Nullable String serverId){
        mcConnection.setActiveSessionHandler(
                StateRegistry.LOGIN,
                new AuthSessionHandler(server, inbound,
                        GameProfile.forOfflinePlayer(login.getUsername()),
                        false, serverId
                )
        );
    }

    /**
     * 以解析出的 {@link AccountId} 建立会话，并切换到 {@link AuthSessionHandler}。
     */
    private void levelLogin$startSession(AccountId accountId, boolean isOnlineMode, String serverId) {
        HandleChannelAccountId.update(mcConnection, accountId);

        var gameProfile = HandleGrabGameProfile.grabGameProfile(accountId);

        // All went well, initialize the session.
        mcConnection.setActiveSessionHandler(
                StateRegistry.LOGIN,
                new AuthSessionHandler(
                        server,
                        inbound,
                        gameProfile,
                        isOnlineMode,
                        serverId
                )
        );
    }

    @Inject(
            method = "handle(Lcom/velocitypowered/proxy/protocol/packet/ServerLoginPacket;)Z",
            at = @At( "HEAD" )
    )
    private void levelLogin$inject$captureServerLoginPacket(ServerLoginPacket pk, CallbackInfo ci){
        LevelLoginMain.getLogger().info("login packet: {}", pk);
    }
}
