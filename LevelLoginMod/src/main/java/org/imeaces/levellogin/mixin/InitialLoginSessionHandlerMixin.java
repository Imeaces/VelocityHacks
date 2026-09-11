package org.imeaces.levellogin.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequestPacket;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import org.imeaces.levellogin.LevelLoginMain;
import org.imeaces.levellogin.data.ChannelKeys;
import org.imeaces.levellogin.handle.HandleGrabGameProfile;
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
        LevelLoginMain.getLogger().info("force offline mode condition branch");

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

        boolean shouldAuthenticate = true;

        if (accountId != null) {
            // assume offline because found accountId before online authenticate

            if (mcConnection.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
                // enable encryption on above 1.20.5
                // but do not use authenticate
                shouldAuthenticate = false;

                // skip to last

            } else {
                // normal offline auth

                var gameProfile = HandleGrabGameProfile.grabGameProfile(accountId);

                mcConnection.setActiveSessionHandler(StateRegistry.LOGIN,
                        new AuthSessionHandler(
                                server,
                                inbound,
                                gameProfile,
                                false,
                                null
                        )
                );

                // no encryption require, exit
                return;
            }
        }

        // Request encryption (may authenticate).
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
            @Local( name = "serverId" ) String serverId
    ){
        var accountId = mcConnection.getChannel().attr(ChannelKeys.AccountIdKey).get();
        boolean isOnlineMode = false;

        if (accountId == null) {
            // assume online auth because no accountId set before

            LevelLoginMain.getLogger().info("ship to online check logic");

            // TODO: online auth check
            isOnlineMode = true;

            // after online auth get accountId again
            accountId = mcConnection.getChannel().attr(ChannelKeys.AccountIdKey).get();
        }

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

        ci.setReturnValue(true);
        ci.cancel();
    }

    @Inject(
            method = "handle(Lcom/velocitypowered/proxy/protocol/packet/ServerLoginPacket;)Z",
            at = @At( "HEAD" )
    )
    private void levelLogin$inject$captureServerLoginPacket(ServerLoginPacket pk, CallbackInfo ci){
        LevelLoginMain.getLogger().info("login packet: {}", pk);
    }
}
