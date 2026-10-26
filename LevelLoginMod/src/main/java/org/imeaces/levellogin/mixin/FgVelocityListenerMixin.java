package org.imeaces.levellogin.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.imeaces.levellogin.LevelLoginMain;
import org.imeaces.levellogin.data.ChannelKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(org.geysermc.floodgate.listener.VelocityListener.class)
public class FgVelocityListenerMixin {

    /**
     * 在 Floodgate 处理 GameProfileRequest 时，把 FloodgatePlayer 的关键信息
     * 写入 Netty Channel 属性，供后续 AccountManager 使用。
     *
     * 捕获的字段：
     * - correctUniqueId：Floodgate 计算出的最终 UUID（可能是关联 Java 的 UUID）
     * - linkedPlayer：如果基岩账号已关联 Java 账号，拿到其 UUID
     */
    @Inject(
            method = "onGameProfileRequest",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/geysermc/floodgate/api/player/FloodgatePlayer;getCorrectUniqueId()Ljava/util/UUID;"
            )
    )
    private void levelLogin$inject$fetchInfo(
            GameProfileRequestEvent event,
            Continuation continuation,
            CallbackInfo ci,
            @Local(name = "player") FloodgatePlayer player
    ) {
        LevelLoginMain.getLogger()
                .info("Floodgate profile request: xuid={}, javaUuid={}, correctUuid={}, linked={}",
                        player.getXuid(),
                        player.getJavaUniqueId(),
                        player.getCorrectUniqueId(),
                        player.getLinkedPlayer() != null
                );

        // 把 Floodgate 的关键 UUID 写入 Channel，供 AccountManager.grabGameProfile() 使用
        if (event.getConnection() instanceof LoginInboundConnection inbound) {
            ChannelKeys.FloodgateCorrectUuidKey.claim(inbound, player.getCorrectUniqueId());

            if (player.getLinkedPlayer() != null) {
                ChannelKeys.FloodgateLinkedJavaUuidKey.claim(inbound,
                        player.getLinkedPlayer().getUniqueId());
                LevelLoginMain.getLogger().info("Floodgate player {} is linked to Java UUID: {}",
                        player.getUsername(), player.getLinkedPlayer().getUniqueId());
            }
        }
    }
}
