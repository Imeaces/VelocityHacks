package org.imeaces.levellogin.mixin;

import com.velocitypowered.proxy.network.ServerChannelInitializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.logging.Logger;

@Mixin(ServerChannelInitializer.class)
public class ServerChannelInitializerMixin {
	@Unique
	private static final Logger logger = Logger.getLogger("mix! ");

	@Inject(
			method = "initChannel",
			at = @At(
                    value = "INVOKE",
					target = "Lcom/velocitypowered/proxy/connection/MinecraftConnection;setActiveSessionHandler(Lcom/velocitypowered/proxy/protocol/StateRegistry;Lcom/velocitypowered/proxy/connection/MinecraftSessionHandler;)V",
					shift = At.Shift.AFTER
			)
	)
	private void init(CallbackInfo info) {
		// This code is injected into the start of MinecraftServer.loadLevel()V
	}
}