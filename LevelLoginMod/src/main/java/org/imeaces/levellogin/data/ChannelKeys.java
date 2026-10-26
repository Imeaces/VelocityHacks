package org.imeaces.levellogin.data;

import org.imeaces.levellogin.handle.HandleChannelAttrValue;
import org.imeaces.levellogin.profile.AccountId;
import org.imeaces.levellogin.profile.PlayerTrait;

import java.util.UUID;

public class ChannelKeys {
    public static final HandleChannelAttrValue<AccountId> AccountIdKey =
            HandleChannelAttrValue.create("levelLogin_accountId");
    public static final HandleChannelAttrValue<Boolean> ShouldAuthenticateKey =
            HandleChannelAttrValue.create("levelLogin_shouldAuthenticate");
    public static final HandleChannelAttrValue<PlayerTrait> PlayerTraitKey =
            HandleChannelAttrValue.create("levelLogin_playerTrait");

    /** Floodgate 玩家的 correctUniqueId（由 FgVelocityListenerMixin 写入） */
    public static final HandleChannelAttrValue<UUID> FloodgateCorrectUuidKey =
            HandleChannelAttrValue.create("levelLogin_floodgateCorrectUuid");
    /** Floodgate 玩家已关联的 Java UUID（来自 FloodgatePlayer.getLinkedPlayer） */
    public static final HandleChannelAttrValue<UUID> FloodgateLinkedJavaUuidKey =
            HandleChannelAttrValue.create("levelLogin_floodgateLinkedJavaUuid");
    /** 标记连接处于密码认证待定状态 */
    public static final HandleChannelAttrValue<Boolean> PendingPasswordAuthKey =
            HandleChannelAttrValue.create("levelLogin_pendingPasswordAuth");
    /** 密码认证关联的目标用户名 */
    public static final HandleChannelAttrValue<String> PendingPasswordUsernameKey =
            HandleChannelAttrValue.create("levelLogin_pendingPasswordUsername");
}
