package org.imeaces.levellogin.mixin;

import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequestPacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.imeaces.levellogin.LevelLoginMain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.security.SecureRandom;
import java.util.Arrays;

@Mixin(InitialLoginSessionHandler.class)
public abstract class InitialLoginSessionHandlerMixin {

    @Shadow
    private static SecureRandom SECURE_RANDOM;

    @Shadow
    private LoginInboundConnection inbound;

    @Shadow
    private byte[] verify;

    @Shadow
    private MinecraftConnection mcConnection;

    @Shadow
    private VelocityServer server;

    @Shadow
    private @MonotonicNonNull ServerLoginPacket login;

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
            method = "lambda$handle$2",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/velocitypowered/proxy/connection/MinecraftConnection;setActiveSessionHandler(Lcom/velocitypowered/proxy/protocol/StateRegistry;Lcom/velocitypowered/proxy/connection/MinecraftSessionHandler;)V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void levelLogin$inject$tryHandleLogin(PreLoginEvent.PreLoginComponentResult result, CallbackInfo ci) {
        // Request encryption.
        EncryptionRequestPacket request = generateEncryptionRequest();
        request.shouldAuthenticate = true;
        verify = Arrays.copyOf(request.getVerifyToken(), 4);
        mcConnection.write(request);
        currentState = InitialLoginSessionHandler.LoginState.ENCRYPTION_REQUEST_SENT;

        LevelLoginMain.getLogger().info("force online mode request (just for test)");

        ci.cancel();
    }
}
