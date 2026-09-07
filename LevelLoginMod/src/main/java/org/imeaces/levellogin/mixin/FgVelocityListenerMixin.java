package org.imeaces.levellogin.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.imeaces.levellogin.LevelLoginMain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(org.geysermc.floodgate.listener.VelocityListener.class)
public class FgVelocityListenerMixin {
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
                .info("hi! I see your xuid: {}, java uuid: {}, corr uuid: {}, linked: {}",
                        player.getXuid(),
                        player.getJavaUniqueId(),
                        player.getCorrectUniqueId(),
                        player.getLinkedPlayer() != null
                );
    }
}
