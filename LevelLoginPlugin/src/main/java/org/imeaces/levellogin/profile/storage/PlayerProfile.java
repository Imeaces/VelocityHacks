package org.imeaces.levellogin.profile.storage;

import org.imeaces.levellogin.profile.AccountId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * 玩家档案 - 核心数据模型
 * 
 * 一个档案可以关联多个认证系统（AccountLink）
 * 例如：同一个档案可以同时关联 Yggdrasil UUID 和 Floodgate XUID
 */
public class PlayerProfile {

    /** 档案唯一 ID */
    private final String profileId;

    /** 首选用户名（显示用） */
    private String preferredUsername;

    /** 所有关联的账户 */
    private final List<AccountId> accountLinks;

    /** 自定义属性（扩展用） */
    private final Map<String, String> properties;

    /** 创建时间 */
    private final Instant createdAt;

    /** 最后活动时间 */
    private Instant lastActiveAt;

    /** 权限组（可选） */
    private final Set<String> groups;

    public PlayerProfile() {
        this.profileId = UUID.randomUUID().toString();
        this.preferredUsername = "";
        this.accountLinks = new ArrayList<>();
        this.properties = new HashMap<>();
        this.createdAt = Instant.now();
        this.lastActiveAt = Instant.now();
        this.groups = new HashSet<>();
    }

    public PlayerProfile(String profileId, String preferredUsername, List<AccountId> accountLinks,
                        Map<String, String> properties, Instant createdAt, Instant lastActiveAt,
                        Set<String> groups) {
        this.profileId = profileId;
        this.preferredUsername = preferredUsername;
        this.accountLinks = new ArrayList<>(accountLinks);
        this.properties = new HashMap<>(properties);
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
        this.groups = new HashSet<>(groups);
    }

    public String getProfileId() {
        return profileId;
    }

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }

    public List<AccountId> getAccountLinks() {
        return Collections.unmodifiableList(accountLinks);
    }

    public void addLink(AccountId link) {
        accountLinks.removeIf(l -> l.authType().equals(link.authType()) && l.subject().equals(link.subject()));
        accountLinks.add(link);
    }

    public void removeLink(AccountId link) {
        accountLinks.removeIf(l -> l.authType().equals(link.authType()) && l.subject().equals(link.subject()));
    }

    public boolean hasLink(AccountId link) {
        return accountLinks.stream().anyMatch(l -> l.authType().equals(link.authType()) && l.subject().equals(link.subject()));
    }

    public Optional<AccountId> findLink(String authType) {
        return accountLinks.stream().filter(l -> l.authType().equals(authType)).findFirst();
    }

    public Optional<String> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public void removeProperty(String key) {
        properties.remove(key);
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void markActive() {
        this.lastActiveAt = Instant.now();
    }

    public Set<String> getGroups() {
        return Collections.unmodifiableSet(groups);
    }

    public void addGroup(String group) {
        groups.add(group);
    }

    public void removeGroup(String group) {
        groups.remove(group);
    }

    public boolean hasGroup(String group) {
        return groups.contains(group);
    }

    @Override
    public String toString() {
        return String.format("PlayerProfile{id=%s, username=%s, links=%d}",
                profileId, preferredUsername, accountLinks.size());
    }
}
