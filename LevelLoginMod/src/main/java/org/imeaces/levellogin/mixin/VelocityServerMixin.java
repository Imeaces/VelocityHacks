package org.imeaces.levellogin.mixin;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.plugin.VelocityPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VelocityServer.class)
public class VelocityServerMixin {

    @Shadow
    private VelocityPluginManager pluginManager;

    private static final Logger levelLogin$logger = LoggerFactory.getLogger("Silvigarabis");

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void init(CallbackInfo info) {
        levelLogin$logger.info("I mixed some code here via mixin! ");
    }

}
