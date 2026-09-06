package org.imeaces.levellogin.transformMod;

import com.velocitypowered.proxy.VelocityServer;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import org.slf4j.LoggerFactory;

@CTransformer(VelocityServer.class)
public class VelocityServerTransform {
    @CInject(method = "<init>", target = @CTarget(
            value = "NEW",
            target = "com.velocitypowered.proxy.plugin.VelocityPluginManager"
    ))
    public void inject$init() {
        LoggerFactory.getLogger("Silvigarabis").info("I mixed some code here via modTransform! ");
    }
}
