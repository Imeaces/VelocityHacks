package org.imeaces.levellogin.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.netty.channel.Channel;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.player.FloodgateHandshakeHandler;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.LinkedPlayer;
import org.imeaces.levellogin.LevelLoginMain;
import org.imeaces.levellogin.data.ChannelKeys;
import org.imeaces.levellogin.profile.AccountId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FloodgateHandshakeHandler.class)
public class FgHandshakeHandlerMixin {
    @Inject(
            method = "handlePart2",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/netty/util/Attribute;set(Ljava/lang/Object;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void levelLogin$inject$captureAccountId(
            Channel channel,
            String hostname,
            BedrockData bedrockData,
            LinkedPlayer linkedPlayer,
            CallbackInfo ci,
            @Local(name = "player") FloodgatePlayer fgPlayer
            ){

        // 此处处于 Floodgate 处理 HandshakePacket 的处理方法中

        var accountId = new AccountId("floodgate", fgPlayer.getXuid());

        LevelLoginMain.getLogger().info("claiming accountId for floodgate player {}: {}", fgPlayer.getJavaUsername(), accountId);

        ChannelKeys.AccountIdKey.claim(channel, accountId);
    }
}
