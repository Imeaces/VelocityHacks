package org.imeaces.levellogin.mixin;

import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import org.imeaces.levellogin.LevelLoginMain;
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
        LevelLoginMain.getLogger().info("Intercepted online mode decision!");

        if (result.isForceOfflineMode()) {
            // 强制设置为离线登录，可能是 Floodgate 做的
            // 但我们不管这个，只是记录一下
        }

        // TODO: process login by ourselves
        // 从这里开始我们可以注入自定义逻辑用于实现在线模式认证，或者跳过此处，认为是离线模式用户
        // 但我认为此处实际上也可以用于处理离线用户的基于连接特征的认证

//        this.inbound.disconnect(Component.translatable(
//                "multiplayer.disconnect.outdated_client",
//                Component.text(ProtocolVersion.SUPPORTED_VERSION_STRING)
//        ));
//
//        ci.cancel();
    }
}
