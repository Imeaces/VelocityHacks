package org.imeaces.levellogin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.imeaces.levellogin.LevelLoginMain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 配置管理
 */
public class LevelLoginConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 服务器密钥（用于密码哈希加盐） */
    private String serverSecret = null;

    /** 是否启用 Yggdrasil 认证 */
    private boolean enableYggdrasil = true;

    /** 是否启用 Floodgate 认证 */
    private boolean enableFloodgate = true;

    /** 是否启用密码认证 */
    private boolean enablePassword = true;

    /** 密码认证：是否自动注册新用户 */
    private boolean passwordAutoRegister = true;

    /** 认证超时（秒） */
    private int authTimeoutSeconds = 30;

    /** 密码认证提示消息 */
    private String passwordPromptMessage = "请输入密码完成登录（直接发送消息即可）";

    /** 密码认证失败最大重试次数 */
    private int passwordMaxRetries = 3;

    public String getServerSecret() {
        if (serverSecret == null) {
            serverSecret = java.util.UUID.randomUUID().toString();
            LevelLoginMain.getLogger().warn("Generated new serverSecret, please save it to config!");
        }
        return serverSecret;
    }
    public boolean isEnableYggdrasil() { return enableYggdrasil; }
    public boolean isEnableFloodgate() { return enableFloodgate; }
    public boolean isEnablePassword() { return enablePassword; }
    public boolean isPasswordAutoRegister() { return passwordAutoRegister; }
    public int getAuthTimeoutSeconds() { return authTimeoutSeconds; }
    public String getPasswordPromptMessage() { return passwordPromptMessage; }
    public int getPasswordMaxRetries() { return passwordMaxRetries; }

    public static LevelLoginConfig load(Path dataDirectory) {
        Path configFile = dataDirectory.resolve("level-login.json");

        try {
            Files.createDirectories(dataDirectory);

            if (!Files.exists(configFile)) {
                LevelLoginConfig config = new LevelLoginConfig();
                config.save(dataDirectory);
                return config;
            }

            String json = Files.readString(configFile);
            LevelLoginConfig config = GSON.fromJson(json, LevelLoginConfig.class);
            
            // 验证配置完整性
            if (config == null) {
                LevelLoginMain.getLogger().error("Failed to parse config, using defaults");
                return new LevelLoginConfig();
            }
            
            // 确保 serverSecret 存在且持久化
            if (config.serverSecret == null) {
                config.serverSecret = java.util.UUID.randomUUID().toString();
                config.save(dataDirectory);
                LevelLoginMain.getLogger().warn("Generated and saved new serverSecret");
            }
            
            LevelLoginMain.getLogger().info("Loaded config from {}", configFile);
            return config;

        } catch (Exception e) {
            LevelLoginMain.getLogger().error("Failed to load config, using defaults", e);
            return new LevelLoginConfig();
        }
    }

    public void save(Path dataDirectory) {
        Path configFile = dataDirectory.resolve("level-login.json");
        try {
            Files.createDirectories(dataDirectory);
            String json = GSON.toJson(this);
            Files.writeString(configFile, json);
        } catch (IOException e) {
            LevelLoginMain.getLogger().error("Failed to save config", e);
        }
    }
}
