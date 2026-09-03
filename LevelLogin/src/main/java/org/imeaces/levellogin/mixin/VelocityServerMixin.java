package org.imeaces.levellogin.mixin;

import com.velocitypowered.proxy.VelocityServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.logging.LogManager;

@Mixin(VelocityServer.class)
public class VelocityServerMixin {

    @Unique
    private static final Logger logger = LoggerFactory.getLogger("Silvigarabis");

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private static void init(CallbackInfo info) {
        logger.info("I mixed some code here! ");
    }
}
