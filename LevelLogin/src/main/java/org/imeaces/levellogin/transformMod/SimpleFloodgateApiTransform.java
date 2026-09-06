package org.imeaces.levellogin.transformMod;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.slf4j.LoggerFactory;

@CTransformer(SimpleFloodgateApi.class)
public class SimpleFloodgateApiTransform {
    @CInject(method = "<init>", target = @CTarget("RETURN"))
    public void inject$init() {
        LoggerFactory.getLogger("Silvigarabis").info("I mixed some code into Floodgate via modTransform!");
        LoggerFactory.getLogger("Silvigarabis").info("I mixed some code into Floodgate via modTransform!");
        throw new RuntimeException("but I would like to inject this!");
    }
}
