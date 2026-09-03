package org.imeaces.levellogin.mixin;

import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import net.kyori.adventure.text.Component;
import org.imeaces.levellogin.LevelLoginMod;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InitialLoginSessionHandler.class)
public class InitialLoginSessionHandlerMixin {

    @Shadow
    @Final
    private LoginInboundConnection inbound;

    @Inject(
            method = "lambda$handle$2",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/velocitypowered/api/event/connection/PreLoginEvent$PreLoginComponentResult;isForceOfflineMode()Z"
            ),
            cancellable = true
    )
    private void levelLogin$inject$tryEnterOnlineMode(PreLoginEvent.PreLoginComponentResult result, CallbackInfo ci){
        // TODO: process login by ourselves
        LevelLoginMod.getLogger().info("Intercepted online mode decision!");

        this.inbound.disconnect(Component.translatable(
                "multiplayer.disconnect.outdated_client",
                Component.text(ProtocolVersion.SUPPORTED_VERSION_STRING)
        ));

        ci.cancel();
    }
}
