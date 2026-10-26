package org.imeaces.levellogin.profile.storage;

import com.google.gson.*;
import org.imeaces.levellogin.LevelLoginMain;
import org.imeaces.levellogin.profile.AccountId;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 玩家档案存储 - JSON 文件持久化
 * 
 * 存储结构：
 * profiles/
 *   profiles.json          <- 主存储文件
 *   profiles.json.bak      <- 自动备份
 */
public class ProfileStore {

    private final Path storageDir;
    private final Path storageFile;
    private final Path backupFile;
    private final Gson gson;

    /** 内存缓存 */
    private final Map<String, PlayerProfile> profilesById;
    private final Map<String, String> linkIndex; // "authType:subject" -> profileId
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ProfileStore(Path dataDirectory) {
        this.storageDir = dataDirectory.resolve("profiles");
        this.storageFile = storageDir.resolve("profiles.json");
        this.backupFile = storageDir.resolve("profiles.json.bak");
        this.profilesById = new ConcurrentHashMap<>();
        this.linkIndex = new ConcurrentHashMap<>();

        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .registerTypeAdapter(AccountId.class, new AccountIdAdapter())
                .setPrettyPrinting()
                .create();

        load();
    }

    /**
     * 根据 AccountId 查找档案
     */
    public Optional<PlayerProfile> findByLink(@NotNull AccountId link) {
        lock.readLock().lock();
        try {
            String key = link.authType() + ":" + link.subject();
            String profileId = linkIndex.get(key);
            if (profileId != null) {
                return Optional.ofNullable(profilesById.get(profileId));
            }
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 根据档案 ID 查找
     */
    public Optional<PlayerProfile> findById(String profileId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(profilesById.get(profileId));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 根据 preferredUsername 查找
     */
    public List<PlayerProfile> findByUsername(String username) {
        lock.readLock().lock();
        try {
            return profilesById.values().stream()
                    .filter(p -> p.getPreferredUsername().equalsIgnoreCase(username))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 根据原始 Minecraft 用户名查找档案
     * 通过遍历所有档案的 password:username 链接匹配
     */
    public List<PlayerProfile> findByMinecraftUsername(String minecraftUsername) {
        lock.readLock().lock();
        try {
            String lowerUsername = minecraftUsername.toLowerCase();
            return profilesById.values().stream()
                    .filter(p -> p.getAccountLinks().stream()
                            .anyMatch(link -> link.authType().equals("password") 
                                    && link.subject().equals(lowerUsername)))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 保存档案
     */
    public void save(PlayerProfile profile) {
        lock.writeLock().lock();
        try {
            profilesById.put(profile.getProfileId(), profile);

            for (AccountId link : profile.getAccountLinks()) {
                String key = link.authType() + ":" + link.subject();
                linkIndex.put(key, profile.getProfileId());
            }

            persist();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 删除档案
     */
    public void delete(PlayerProfile profile) {
        lock.writeLock().lock();
        try {
            profilesById.remove(profile.getProfileId());

            for (AccountId link : profile.getAccountLinks()) {
                String key = link.authType() + ":" + link.subject();
                linkIndex.remove(key);
            }

            persist();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取所有档案
     */
    public Collection<PlayerProfile> getAll() {
        return Collections.unmodifiableCollection(profilesById.values());
    }

    /**
     * 档案总数
     */
    public int count() {
        return profilesById.size();
    }

    private void load() {
        try {
            Files.createDirectories(storageDir);

            if (!Files.exists(storageFile)) {
                LevelLoginMain.getLogger().info("No profile data found, starting fresh");
                return;
            }

            String json = Files.readString(storageFile);
            ProfileData[] data = gson.fromJson(json, ProfileData[].class);

            if (data != null) {
                for (ProfileData pd : data) {
                    PlayerProfile profile = pd.toProfile();
                    profilesById.put(profile.getProfileId(), profile);

                    for (AccountId link : profile.getAccountLinks()) {
                        String key = link.authType() + ":" + link.subject();
                        linkIndex.put(key, profile.getProfileId());
                    }
                }
            }

            LevelLoginMain.getLogger().info("Loaded {} profiles", profilesById.size());
        } catch (Exception e) {
            LevelLoginMain.getLogger().error("Failed to load profiles", e);
        }
    }

    private void persist() {
        try {
            if (Files.exists(storageFile)) {
                Files.copy(storageFile, backupFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            ProfileData[] data = profilesById.values().stream()
                    .map(ProfileData::fromProfile)
                    .toArray(ProfileData[]::new);

            String json = gson.toJson(data);
            Files.writeString(storageFile, json);
        } catch (IOException e) {
            LevelLoginMain.getLogger().error("Failed to save profiles", e);
        }
    }

    private record ProfileData(
        String profileId,
        String preferredUsername,
        AccountId[] accountLinks,
        Map<String, String> properties,
        String createdAt,
        String lastActiveAt,
        String[] groups
    ) {
        public PlayerProfile toProfile() {
            return new PlayerProfile(
                profileId,
                preferredUsername,
                Arrays.asList(accountLinks),
                properties != null ? properties : new HashMap<>(),
                Instant.parse(createdAt),
                Instant.parse(lastActiveAt),
                groups != null ? new HashSet<>(Arrays.asList(groups)) : new HashSet<>()
            );
        }

        public static ProfileData fromProfile(PlayerProfile profile) {
            return new ProfileData(
                profile.getProfileId(),
                profile.getPreferredUsername(),
                profile.getAccountLinks().toArray(new AccountId[0]),
                new HashMap<>(profile.getProperties()),
                profile.getCreatedAt().toString(),
                profile.getLastActiveAt().toString(),
                profile.getGroups().toArray(new String[0])
            );
        }
    }

    private static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return Instant.parse(json.getAsString());
        }
    }

    private static class AccountIdAdapter implements JsonSerializer<AccountId>, JsonDeserializer<AccountId> {
        @Override
        public JsonElement serialize(AccountId src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("authType", src.authType());
            obj.addProperty("subject", src.subject());
            return obj;
        }

        @Override
        public AccountId deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonObject obj = json.getAsJsonObject();
            return new AccountId(
                obj.get("authType").getAsString(),
                obj.get("subject").getAsString()
            );
        }
    }
}
