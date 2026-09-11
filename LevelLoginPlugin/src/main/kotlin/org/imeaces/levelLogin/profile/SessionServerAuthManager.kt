package org.imeaces.levelLogin

import com.velocitypowered.proxy.connection.client.LoginInboundConnection
import net.kyori.adventure.text.Component
import org.imeaces.levellogin.handle.HandleHasJoined

class SessionServerAuthManager(plugin: LevelLoginPlugin) : HandleHasJoined.HandleHasJoinedHandler {
    override fun requestHasJoined(
        username: String,
        serverId: String,
        clientIp: String,
        inbound: LoginInboundConnection
    ) {
        // TODO: online auth check

        inbound.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"))
    }
}
