package org.imeaces.levellogin.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.velocitypowered.proxy.plugin.VelocityPluginManager;
import org.imeaces.levelLogin.LevelLoginPlugin;
import org.imeaces.levellogin.util.AdditionalPathDirectoryStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.tinylog.Logger;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;

@Mixin(VelocityPluginManager.class)
public class VelocityPluginManagerMixin {
    @ModifyExpressionValue(
            method = "loadPlugins",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/nio/file/Files;newDirectoryStream(Ljava/nio/file/Path;Ljava/nio/file/DirectoryStream$Filter;)Ljava/nio/file/DirectoryStream;"
            )
    )
    private DirectoryStream<Path> modifyPluginStream(
            DirectoryStream<Path> original
    ) {
        Logger.info("this -> classloader: {}", this.getClass().getClassLoader());
        Logger.info("LevelLoginPlugin -> classloader: {}", LevelLoginPlugin.class.getClassLoader());
        return new AdditionalPathDirectoryStream(
                original,
                LevelLoginPlugin.getJarPath()
        );
    }
}
