package org.imeaces.levellogin.mixin;

import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tinylog.Logger;

@Mixin(SimpleFloodgateApi.class)
public class FloodgateBootMixin {

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private static void init(CallbackInfo info) {
        Logger.info("I mixed some code to floodgate!");
    }
}
