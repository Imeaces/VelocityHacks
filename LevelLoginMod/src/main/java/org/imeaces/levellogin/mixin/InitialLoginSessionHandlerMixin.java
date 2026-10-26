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
import org.imeaces.levellogin.data.HasJoinedOnlineResponseResult;
import org.imeaces.levellogin.data.LevelLoginConfiguration;
import org.imeaces.levellogin.event.AccountIdMappingEvent;
import org.imeaces.levellogin.event.YggdrasilHasJoinedRequestEvent;
import org.imeaces.levellogin.profile.AccountId;
import org.imeaces.levellogin.profile.PlayerTrait;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.function.Function;

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

    @Inject(
            method = "handle(Lcom/velocitypowered/proxy/protocol/packet/ServerLoginPacket;)Z",
            at = @At( "HEAD" )
    )
    private void levelLogin$inject$captureServerLoginPacket(ServerLoginPacket pk, CallbackInfo ci){
        LevelLoginMain.getLogger().info("login packet: {}", pk);
    }

    @Inject(
            method = "handle(Lcom/velocitypowered/proxy/protocol/packet/ServerLoginPacket;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/velocitypowered/proxy/event/VelocityEventManager;fire(Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;",
                    shift = At.Shift.BEFORE
            )
    )
    private void levelLogin$inject$playerTrait(ServerLoginPacket pk, CallbackInfo ci){
        var playerTrait = new PlayerTrait(
                inbound.getRemoteAddress().getAddress().getHostAddress(),
                login.getUsername(),
                login.getHolderUuid(),
                inbound.getRawVirtualHost().orElse(null),
                inbound.getProtocolVersion().getProtocol()
        );

        ChannelKeys.PlayerTraitKey.set(mcConnection, playerTrait);
    }

    @Inject(
            method = "lambda$handle$2(Lcom/velocitypowered/api/event/connection/PreLoginEvent$PreLoginComponentResult;)V",
            at = @At( "HEAD" ),
            cancellable = true
    )
    private void levelLogin$inject$handlePreLogin(PreLoginEvent.PreLoginComponentResult result, CallbackInfo ci) {
        ci.cancel(); // always cancel remaining codes under inject point

        var accountId = ChannelKeys.AccountIdKey.get(mcConnection);

        // 只有在"没有已确定的账户"且"运行配置要求使用 Yggdrasil 认证"时，才向会话服务器认证。
        // 原版 Velocity 的判定逻辑已被覆盖
        boolean shouldAuthenticate = accountId == null && LevelLoginConfiguration.isYggdrasilLoginEnabled();

        // EncryptionRequest 的 shouldAuthenticate 字段自 1.20.5 起才存在于协议中（见 EncryptionRequestPacket），
        // 更早的客户端收不到它，无法表达"加密但不认证"。
        // 因此只有 1.20.5+ 才可以做到启用加密但不使用认证。
        // 在协议允许且运行配置已启用相关功能时才启用加密，其余情况直接建立离线会话。
        boolean needsEncryption = false;
        calculateNeedsEncryption: {
            if (shouldAuthenticate) {

                needsEncryption = true;

                break calculateNeedsEncryption;
            }

            if (!LevelLoginConfiguration.isOfflineAuthWithEncryptionEnabled()) {
                break calculateNeedsEncryption;
            }

            if (mcConnection.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_20_5)){
                break calculateNeedsEncryption;
            }

            needsEncryption = true;
        }

        if (needsEncryption) {
            // Request encryption (may authenticate).
            levelLogin$requestEncryption(shouldAuthenticate);
        } else {
            levelLogin$handleUnencryptionLogin(accountId);
        }
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
        var accountId = ChannelKeys.AccountIdKey.get(mcConnection);

        // 属性缺失时按"需要认证"处理：这是更安全的一侧，也与 Velocity 自身的默认行为一致。
        Boolean shouldAuthenticateAttr = ChannelKeys.ShouldAuthenticateKey.get(mcConnection);
        boolean shouldAuthenticate = shouldAuthenticateAttr == null || shouldAuthenticateAttr;

        processAuthentication: {
            if (accountId != null) {
                LevelLoginMain.getLogger().info(
                        "shipped to LOGIN stage due to existed accountId: {}", accountId);

                break processAuthentication;
            }
            if (!shouldAuthenticate) {
                LevelLoginMain.getLogger().info(
                        "shipped to LOGIN stage because authentication was skipped for player {}", username);

                break processAuthentication;
            }

            // 没有预先确定的账户，且要求使用 Yggdrasil 在线认证，开始认证过程。在稍后完成认证后才开始会话
            levelLogin$requestHasJoined$start(username, serverId, clientIp);

            return;  // 返回，对于需要认证的连接已经处理完成
        }

        // 在此处我们需要处理 isOnlineMode = false 时的加密请求

        // 本项目可以发送会发送 shouldAuthenticate = false 的 EncryptionRequestPacket，这使得离线模式也能够使用加密传输。
        // 为了保持数据的一致性，我们选择传递 serverId。
        // 这可能违反了 com.velocitypowered.api.event.connection.LoginEvent#serverIdHash 所描述的事情，在此记录一下
        if (accountId == null) {
            // 使用了离线模式加密，但未能通过凭据登录，进入完全离线的登录模式，并自动构造离线模式玩家配置
            // 以允许在进入游戏后准备进一步认证（例如传统 /login 插件，或者设想中的 WebAuth）
            levelLogin$startSession(serverId);
        } else {
            // 加密之前就确定了账户（例如 Floodgate），不需要再向会话服务器认证，直接进入下一步。
            levelLogin$startSession(accountId, false, serverId);
        }
    }

    /**
     * 向远端要求启用加密
     * <br />
     * 在客户端收到 EncryptionRequestPacket 后，将会启用加密，同时选择向认证服务器发送请求。
     * 最后服务器将会受到 EncryptionResponsePacket，并经由我们的 {@link levelLogin$inject$handleEncryption} 处理
     * @param shouldAuthenticate 是否启用在线登录验证
     */
    private void levelLogin$requestEncryption(boolean shouldAuthenticate){
        LevelLoginMain.getLogger().info(
                "request encryption for player {}, shouldAuthenticate: {}",
                login.getUsername(), shouldAuthenticate
        );

        ChannelKeys.ShouldAuthenticateKey.set(mcConnection, shouldAuthenticate);
        EncryptionRequestPacket request = generateEncryptionRequest();
        // 此处使用 Access Widener 扩展访问以修改该字段
        request.shouldAuthenticate = shouldAuthenticate;
        verify = Arrays.copyOf(request.getVerifyToken(), 4);
        mcConnection.write(request);
        currentState = InitialLoginSessionHandler.LoginState.ENCRYPTION_REQUEST_SENT;
    }

    /**
     * 处理未加密的任何登录请求
     * @param accountId 可能的 accountId
     */
    private void levelLogin$handleUnencryptionLogin(@Nullable AccountId accountId) {
        if (accountId != null) {
            // 我们在 Yggdrasil 认证前就获取到了 AccountId，我们可以直接开始会话
            // 这可能来源于 floodgate （见 FgVelocityListenerMixin），也可能来源于连接特征
            LevelLoginMain.getLogger().info("enter offline mode for player {} because found an accountId: {}",
                    login.getUsername(), accountId);

            levelLogin$startSession(accountId, false, null);
        } else {
            // 没有解析出账户，也不要求在线认证：完全离线放行，
            // 让玩家先进入游戏，再在游戏内完成认证（例如传统 /login，或设想中的 WebAuth）。
            LevelLoginMain.getLogger().info("enter offline mode for player {}", login.getUsername());

            levelLogin$startSession(null);
        }
    }

    private void levelLogin$requestHasJoined$start(
            String username,
            String serverId,
            String clientIp
    ){
        LevelLoginMain.getLogger().info(
                "requesting yggdrasil auth for player {} clientIp {}",
                username, clientIp
        );

        var requestEvent = new YggdrasilHasJoinedRequestEvent(username, serverId, clientIp, inbound);

        Runnable responseHandle = () -> {
            if (mcConnection.isClosed()) return;

            var authResult = requestEvent.getResult();
            levelLogin$requestHasJoined$complete(authResult, serverId);
        };

        server.getEventManager().fire(requestEvent)
            .thenRunAsync(responseHandle, mcConnection.eventLoop())
            .exceptionallyAsync(levelLogin$createGenericErrHandle(), mcConnection.eventLoop());
    }

    private void levelLogin$requestHasJoined$complete(
            HasJoinedOnlineResponseResult authResult,
            String serverId
    ) {
        switch (authResult){
            case HasJoinedOnlineResponseResult.Failed _ -> {
                // 会话服务器出错，检查是否启用密码认证作为降级方案
                if (LevelLoginConfiguration.isPasswordEnabled()) {
                    LevelLoginMain.getLogger().warn(
                            "Yggdrasil auth failed for {}, falling back to password authentication",
                            login.getUsername()
                    );
                    ChannelKeys.PendingPasswordAuthKey.set(mcConnection, true);
                    ChannelKeys.PendingPasswordUsernameKey.set(mcConnection, login.getUsername());
                    levelLogin$startSession(serverId);
                } else {
                    inbound.disconnect(
                            Component.translatable("multiplayer.disconnect.authservers_down")
                    );
                }
            }

            case HasJoinedOnlineResponseResult.Invalid _ -> {
                // 会话服务器返回 204：该玩家并没有以此代理的身份登录
                // 检查是否启用密码认证作为降级方案
                if (LevelLoginConfiguration.isPasswordEnabled()) {
                    LevelLoginMain.getLogger().info(
                            "Yggdrasil auth invalid for {}, falling back to password authentication",
                            login.getUsername()
                    );
                    ChannelKeys.PendingPasswordAuthKey.set(mcConnection, true);
                    ChannelKeys.PendingPasswordUsernameKey.set(mcConnection, login.getUsername());
                    levelLogin$startSession(serverId);
                } else {
                    inbound.disconnect(
                            Component.translatable("velocity.error.online-mode-only", NamedTextColor.RED)
                    );
                }
            }

            case HasJoinedOnlineResponseResult.Success successResult -> {
                // 与 Velocity 原实现保持一致：1.19.1+ 的 LINKED_V2 公钥需要绑定到认证得到的玩家 UUID 上。
                var identifiedKey = inbound.getIdentifiedKey();

                if (identifiedKey != null
                        && identifiedKey.getKeyRevision() == IdentifiedKey.Revision.LINKED_V2
                        && identifiedKey instanceof IdentifiedKeyImpl key
                        && !key.internalAddHolder(successResult.verifiedHolderId())) {
                    inbound.disconnect(
                            Component.translatable("multiplayer.disconnect.invalid_public_key")
                    );
                    return;
                }

                // 开始会话
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

    private void levelLogin$startSession(@NotNull AccountId accountId, boolean isOnlineMode, @Nullable String serverId) {
        ChannelKeys.AccountIdKey.claim(mcConnection, accountId);

        LevelLoginMain.getLogger().info(
                "mapping accountId {} under player {} clientIp {} to gameProfile",
                accountId, login.getUsername(), inbound.getRemoteAddress()
        );

        // 使用 AccountIdMappingEvent 获取玩家档案
        var reqProfileEvent = new AccountIdMappingEvent(accountId, inbound);
        Runnable gameProfileHandle = () -> {
            var gameProfile = reqProfileEvent.getGameProfile();

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
        };

        server.getEventManager().fire(reqProfileEvent)
                .thenRunAsync(gameProfileHandle, mcConnection.eventLoop())
                .exceptionallyAsync(levelLogin$createGenericErrHandle(), mcConnection.eventLoop());
    }

    private Function<Throwable, Void> levelLogin$createGenericErrHandle(){
        return (err) -> {
            levelLogin$genericErrHandle(err);
            return null;
        };
    }

    private void levelLogin$genericErrHandle(Throwable err){
        LevelLoginMain.getLogger().error(
                "unexpected error under player {} connect from {}, disconnecting...",
                login.getUsername(), inbound.getRemoteAddress(), err
        );
        if (!mcConnection.isClosed()){
            inbound.disconnect(Component.translatable("multiplayer.disconnect.generic"));
        }
    }
}
