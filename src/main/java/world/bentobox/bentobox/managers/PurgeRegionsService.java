package world.bentobox.bentobox.managers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import world.bentobox.bentobox.util.TaskScheduler;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.island.IslandGrid;
import world.bentobox.bentobox.managers.island.IslandGrid.IslandData;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.level.Level;

/**
 * Core implementation of the "purge region files" operation shared by the
 * {@code /bbox admin purge regions} command and the periodic
 * {@link HousekeepingManager} auto-purge task.
 *
 * <p>Threading requirements are method-specific. Methods that scan, read, or
 * delete region data perform blocking disk I/O and should be called from an
 * async thread. Methods that interact with Bukkit world or chunk APIs must be
 * called from the main server thread.
 *
 * <p>The service does not interact with players or issue confirmations — the
 * caller is responsible for any user-facing UX.
 *
 * <p>Shared code path for scanning, filtering, and deleting region files
 * across the overworld + optional nether/end dimensions. Used by
 * {@code /bbox admin purge} (the top-level command), {@code purge deleted},
 * and the periodic {@link HousekeepingManager}.
 *
 * @since 3.15.0
 */
public class PurgeRegionsService {

    private static final String REGION = "region";
    private static final String ENTITIES = "entities";
    private static final String POI = "poi";
    private static final String DIM_1 = "DIM-1";
    private static final String DIM1 = "DIM1";
    private static final String PLAYERS = "players";
    private static final String PLAYERDATA = "playerdata";
    private static final String EXISTS_PREFIX = " (exists=";
    private static final String PURGE_FOUND = "Purge found ";

    private final BentoBox plugin;
    private final File deferredFile;
    private final Map<String, RecoveredDeletion> recoveredDeletions = new HashMap<>();

    private record RecoveredDeletion(int days, List<File> dimensions) {}

    /**
     * Island IDs whose region files were deleted by a deleted-sweep
     * ({@code days == 0}) but whose DB rows are deferred until plugin
     * shutdown. Paper's internal chunk cache may still serve stale block
     * data even after the {@code .mca} file is gone from disk; only a
     * clean shutdown guarantees the cache is cleared.
     */
    private final Set<String> pendingDeletions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, List<File>> pendingFiles = new ConcurrentHashMap<>();

    public PurgeRegionsService(BentoBox plugin) {
        this.plugin = plugin;
        this.deferredFile = new File(new File(plugin.getDataFolder(), "database"), "deferred-purge.yml");
        recoverDeferredRegionFiles();
    }

    /** Records an online Folia purge for recovery before the next world load. */
    public synchronized boolean defer(PurgeScanResult scan) {
        if (scan.isEmpty()) return true;
        YamlConfiguration yaml = loadDeferred();
        if (yaml == null) return false;
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Map<?, ?> raw : yaml.getMapList("purges")) {
            Map<String, Object> entry = new HashMap<>();
            raw.forEach((key, value) -> entry.put(String.valueOf(key), value));
            entries.add(entry);
        }
        String world;
        List<Map<String, Object>> dimensions = new ArrayList<>();
        try {
            world = scan.world().getWorldFolder().getCanonicalPath();
            dimensions.add(dimensionManifest(scan.world()));
            if (scan.isNether()) dimensions.add(dimensionManifest(plugin.getIWM().getNetherWorld(scan.world())));
            if (scan.isEnd()) dimensions.add(dimensionManifest(plugin.getIWM().getEndWorld(scan.world())));
        } catch (IOException | RuntimeException error) {
            plugin.logWarning("Purge not deferred: world identity/path could not be verified: " + error.getMessage());
            return false;
        }
        List<String> regions = scan.deletableRegions().keySet().stream()
                .map(pair -> pair.x() + ":" + pair.z()).toList();
        boolean duplicate = entries.stream().anyMatch(entry -> Integer.valueOf(2).equals(entry.get("version"))
                && world.equals(entry.get("world")) && regions.equals(entry.get("regions"))
                && dimensions.equals(entry.get("dimensions"))
                && Integer.valueOf(scan.days()).equals(entry.get("days")));
        if (!duplicate) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("world", world);
            entry.put("world-name", scan.world().getName());
            entry.put("days", scan.days());
            entry.put("nether", scan.isNether());
            entry.put("end", scan.isEnd());
            entry.put("version", 2);
            entry.put("dimensions", dimensions);
            entry.put("straddling", new ArrayList<>(scan.straddlingIslandIds()));
            entry.put("regions", regions);
            entry.put("islands", scan.deletableRegions().values().stream().flatMap(Set::stream).distinct().toList());
            entries.add(entry);
        }
        yaml.set("purges", entries);
        boolean saved = saveDeferred(yaml);
        if (saved) plugin.logWarning("Purge deferred for next startup: " + scan.deletableRegions().size()
                + " region(s) in " + scan.world().getName());
        return saved;
    }

    /** Completes database cleanup for files recovered at startup. Call after islands load. */
    public synchronized void completeRecoveredDeletions() {
        List<Island> aged = new ArrayList<>();
        recoveredDeletions.forEach((id, recovery) -> plugin.getIslands().getIslandById(id).ifPresent(island -> {
            if (hasResidualFiles(island, recovery.dimensions())) {
                plugin.logWarning("Recovered island row retained: residual files could not be ruled out for " + id);
                return;
            }
            if (recovery.days() == 0) {
                if (island.isDeletable()) {
                    pendingFiles.put(id, recovery.dimensions());
                    pendingDeletions.add(id);
                }
            } else if (!cannotDeleteIsland(island, recovery.days())) {
                aged.add(island);
            }
        }));
        if (!aged.isEmpty()) finalizeIslands(aged);
        recoveredDeletions.clear();
    }

    private Map<String, Object> dimensionManifest(World world) throws IOException {
        if (world == null) throw new IOException("Dimension is not loaded");
        return Map.of("world", world.getWorldFolder().getCanonicalPath(),
                "uuid", world.getUID().toString(), "environment", world.getEnvironment().name(),
                "data", resolveDataFolder(world).getCanonicalPath());
    }

    private YamlConfiguration loadDeferred() {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            if (deferredFile.exists()) yaml.load(deferredFile);
            if (yaml.contains("purges") && (!(yaml.get("purges") instanceof List<?> entries)
                    || entries.stream().anyMatch(entry -> !(entry instanceof Map<?, ?>)))) {
                throw new InvalidConfigurationException("Invalid purge list");
            }
            return yaml;
        } catch (IOException | InvalidConfigurationException error) {
            plugin.logWarning("Deferred purge manifest retained unread: " + error.getMessage());
            return null;
        }
    }

    private void recoverDeferredRegionFiles() {
        if (!deferredFile.isFile()) return;
        YamlConfiguration yaml = loadDeferred();
        if (yaml == null) return;
        List<Map<?, ?>> remaining = new ArrayList<>();
        for (Map<?, ?> raw : yaml.getMapList("purges")) {
            File worldFolder = new File(String.valueOf(raw.get("world")));
            // Old manifests lack the data needed to revalidate safely; retain them for manual recovery.
            if (!Integer.valueOf(2).equals(raw.get("version")) || !safeDeferredPurge(raw, worldFolder)) {
                remaining.add(raw);
                plugin.logWarning("Deferred purge retained: current island/file state could not be verified for " + worldFolder);
                continue;
            }
            boolean ok = true;
            Object regionsValue = raw.get("regions");
            if (regionsValue instanceof List<?> regions) {
                for (Object value : regions) {
                    String[] parts = String.valueOf(value).split(":", 2);
                    if (parts.length != 2 || !parts[0].matches("-?\\d+") || !parts[1].matches("-?\\d+")) { ok = false; continue; }
                    String name = "r." + parts[0] + "." + parts[1] + ".mca";
                    for (File dimension : deferredDimensions(raw, worldFolder)) {
                        ok &= deleteDimensionFiles(name, new DimFolders(new File(dimension, REGION),
                                new File(dimension, ENTITIES), new File(dimension, POI)));
                    }
                }
            }
            if (ok) {
                Object islands = raw.get("islands");
                if (islands instanceof List<?> ids) ids.stream()
                        .filter(id -> !(raw.get("straddling") instanceof List<?> retained) || !retained.contains(id))
                         .forEach(id -> recoveredDeletions.put(String.valueOf(id), new RecoveredDeletion(
                                 ((Number) raw.get("days")).intValue(), deferredDimensions(raw, worldFolder))));
            } else {
                remaining.add(raw);
            }
        }
        yaml.set("purges", remaining);
        saveDeferred(yaml);
    }

    private List<File> deferredDimensions(Map<?, ?> raw, File worldFolder) {
        return ((List<?>) raw.get("dimensions")).stream()
                .map(entry -> new File((String) ((Map<?, ?>) entry).get("data"))).toList();
    }

    /** Read persisted rows without Bukkit deserialization: worlds must still be unloaded. */
    private boolean safeDeferredPurge(Map<?, ?> raw, File worldFolder) {
        // shortcut: JSON only; add a strict raw-row database API before enabling other backends.
        if (plugin.getSettings().getDatabaseType() != world.bentobox.bentobox.database.DatabaseSetup.DatabaseType.JSON) {
            plugin.logWarning("Deferred purge requires a complete, strict database read; backend is not supported");
            return false;
        }
        if (!(raw.get("world-name") instanceof String worldName)
                || !(raw.get("regions") instanceof List<?> regions) || regions.isEmpty()
                || !(raw.get("islands") instanceof List<?> ids) || ids.stream().anyMatch(id -> !(id instanceof String))
                || !(raw.get("straddling") instanceof List<?> retained)
                || retained.stream().anyMatch(id -> !(id instanceof String))
                || !(raw.get("days") instanceof Integer days) || days < 0
                || !(raw.get("nether") instanceof Boolean) || !(raw.get("end") instanceof Boolean)) return false;
        File table = new File(plugin.getDataFolder(), "database/Island");
        File[] files = table.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return false;
        Set<String> found = new HashSet<>();
        try {
            if (!validDeferredDimensions(raw, worldFolder)) return false;
            Set<Pair<Integer, Integer>> coordinates = new HashSet<>();
            for (Object region : regions) {
                String[] parts = String.valueOf(region).split(":", -1);
                if (parts.length != 2) return false;
                int x = Integer.parseInt(parts[0]), z = Integer.parseInt(parts[1]);
                if (x < (Integer.MIN_VALUE >> 9) || x > (Integer.MAX_VALUE >> 9)
                        || z < (Integer.MIN_VALUE >> 9) || z > (Integer.MAX_VALUE >> 9)) return false;
                coordinates.add(new Pair<>(x, z));
            }
            if (coordinates.size() != regions.size()) return false;
            Set<String> allIds = new HashSet<>();
            for (File file : files) {
                JsonObject row = JsonParser.parseString(Files.readString(file.toPath())).getAsJsonObject();
                String id = row.get("uniqueId").getAsString();
                if (!allIds.add(id)) return false;
                String rowWorld = row.get("world").getAsString();
                if (!worldName.equals(rowWorld)
                        && !(Boolean.TRUE.equals(raw.get("nether")) && (worldName + "_nether").equals(rowWorld))
                        && !(Boolean.TRUE.equals(raw.get("end")) && (worldName + "_the_end").equals(rowWorld))) continue;
                var center = row.getAsJsonArray("center");
                if (!rowWorld.equals(center.get(0).getAsString())) return false;
                if (!row.getAsJsonPrimitive("range").isNumber()
                        || !center.get(1).getAsJsonPrimitive().isNumber()
                        || !center.get(3).getAsJsonPrimitive().isNumber()
                        || row.get("range").getAsDouble() != row.get("range").getAsInt()
                        || center.get(1).getAsDouble() < Integer.MIN_VALUE
                        || center.get(1).getAsDouble() > Integer.MAX_VALUE
                        || center.get(3).getAsDouble() < Integer.MIN_VALUE
                        || center.get(3).getAsDouble() > Integer.MAX_VALUE) return false;
                int x = (int) Math.floor(center.get(1).getAsDouble()), z = (int) Math.floor(center.get(3).getAsDouble());
                int range = row.get("range").getAsInt();
                if (range <= 0 || !Double.isFinite(center.get(1).getAsDouble())
                        || !Double.isFinite(center.get(3).getAsDouble())) return false;
                boolean overlaps = coordinates.stream().anyMatch(region ->
                        ((long) x - range) <= (long) region.x() * 512 + 511
                        && ((long) x + range) > (long) region.x() * 512
                        && ((long) z - range) <= (long) region.z() * 512 + 511
                        && ((long) z + range) > (long) region.z() * 512);
                if (!overlaps) continue;
                if (!ids.contains(id) || !eligibleDeferredIsland(row, days)) return false;
                found.add(id);
            }
            if (!found.containsAll(ids)) return false;
            // A loaded world may still have dirty chunks, even when its files look unchanged.
            if (days > 0) {
                long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
                for (File dimension : deferredDimensions(raw, worldFolder)) for (var region : coordinates)
                    for (String kind : List.of(REGION, ENTITIES, POI)) {
                        File file = new File(new File(dimension, kind), "r." + region.x() + "." + region.z() + ".mca");
                        if (file.exists() && (!file.isFile() || file.length() < 8192
                                || file.lastModified() >= cutoff || strictRegionTimestamp(file) >= cutoff)) return false;
                    }
            }
            return true;
        } catch (IOException | RuntimeException error) {
            plugin.logWarning("Deferred purge validation failed: " + error.getMessage());
            return false;
        }
    }

    private boolean saveDeferred(YamlConfiguration yaml) {
        java.nio.file.Path temporary = null;
        try {
            Files.createDirectories(deferredFile.getParentFile().toPath());
            temporary = Files.createTempFile(deferredFile.getParentFile().toPath(), "deferred-purge-", ".tmp");
            Files.writeString(temporary, yaml.saveToString(), java.nio.charset.StandardCharsets.UTF_8);
            try {
                Files.move(temporary, deferredFile.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException error) {
                Files.move(temporary, deferredFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            plugin.logError("Failed to save deferred purge state: " + e.getMessage());
            return false;
        } finally {
            if (temporary != null) try {
                Files.deleteIfExists(temporary);
            } catch (IOException error) {
                plugin.logWarning("Could not remove temporary purge manifest: " + temporary);
            }
        }
    }

    private boolean validDeferredDimensions(Map<?, ?> raw, File worldFolder) throws IOException {
        String worldName = (String) raw.get("world-name");
        File container = Bukkit.getWorldContainer();
        if (container == null || worldName.isBlank() || worldName.contains("/") || worldName.contains("\\")) return false;
        File base = worldFolder.getCanonicalFile();
        if (!base.getPath().equals(raw.get("world")) || !validWorldFolder(container, base, worldName)) return false;
        if (!(raw.get("dimensions") instanceof List<?> dimensions)
                || dimensions.size() != 1 + (Boolean.TRUE.equals(raw.get("nether")) ? 1 : 0)
                        + (Boolean.TRUE.equals(raw.get("end")) ? 1 : 0)) return false;
        Set<File> seen = new HashSet<>();
        for (int i = 0; i < dimensions.size(); i++) {
            if (!(dimensions.get(i) instanceof Map<?, ?> dimension)
                    || !(dimension.get("world") instanceof String folderPath)
                    || !(dimension.get("data") instanceof String dataPath)
                    || !(dimension.get("uuid") instanceof String uuid)) return false;
            String environment = i == 0 ? "NORMAL"
                    : i == 1 && Boolean.TRUE.equals(raw.get("nether")) ? "NETHER" : "THE_END";
            if (!environment.equals(dimension.get("environment"))) return false;
            File folder = new File(folderPath).getCanonicalFile();
            File data = new File(dataPath).getCanonicalFile();
            File allowedWorld = i == 0 ? base : new File(base.getParentFile(),
                    worldName + (environment.equals("NETHER") ? "_nether" : "_the_end")).getCanonicalFile();
            if ((!folder.equals(allowedWorld) && !(i > 0 && folder.equals(base))) || !folder.getPath().equals(folderPath)
                    || !data.getPath().equals(dataPath) || !seen.add(data)) return false;
            File nested = new File(folder, environment.equals("NETHER") ? DIM_1 : DIM1);
            if (!data.equals(folder) && (i == 0 || !data.equals(nested))) return false;
            if (i > 0 && folder.equals(base) && !data.equals(nested)) return false;
            if (!UUID.fromString(uuid).equals(worldUuid(folder))) return false;
            for (World loaded : Bukkit.getWorlds()) {
                File loadedFolder = loaded.getWorldFolder().getCanonicalFile();
                if (loaded.getUID().toString().equals(uuid) || loadedFolder.equals(folder)
                        || resolveDataFolder(loaded).getCanonicalFile().equals(data)) return false;
            }
            for (String kind : List.of(REGION, ENTITIES, POI)) {
                File directory = new File(data, kind);
                if (!directory.getCanonicalFile().equals(directory.getAbsoluteFile())
                        || directory.exists() && !directory.isDirectory()) return false;
                for (Object coordinate : (List<?>) raw.get("regions")) {
                    String[] parts = String.valueOf(coordinate).split(":", -1);
                    if (parts.length != 2) return false;
                    File target = new File(directory, "r." + Integer.parseInt(parts[0]) + "."
                            + Integer.parseInt(parts[1]) + ".mca");
                    if (!target.getCanonicalFile().equals(target.getAbsoluteFile())
                            || target.exists() && !target.isFile()) return false;
                }
            }
        }
        return true;
    }

    /**
     * Accepts the legacy layout ({@code <container>/<world>}) and the Paper 26.1.1+
     * dimension layout ({@code <container>/<level>/dimensions/<namespace>/<world>}).
     * The folder leaf must equal the recorded world name so a manifest cannot
     * redirect deletion to another world.
     */
    private boolean validWorldFolder(File container, File folder, String worldName) throws IOException {
        if (!folder.getName().equals(worldName)) return false;
        File parent = folder.getParentFile();
        if (parent == null) return false;
        if (parent.equals(container.getCanonicalFile())) return true;
        File dimensions = parent.getParentFile();
        return dimensions != null && "dimensions".equals(dimensions.getName())
                && dimensions.getParentFile() != null
                && container.getCanonicalFile().equals(dimensions.getParentFile().getParentFile())
                && parent.getName().matches("[A-Za-z0-9._-]+");
    }

    /** Level UUID from uid.dat, or from Paper's NBT metadata when that file is absent. */
    private UUID worldUuid(File folder) throws IOException {
        File uid = new File(folder, "uid.dat");
        if (uid.isFile()) try (DataInputStream input = new DataInputStream(new FileInputStream(uid))) {
            return new UUID(input.readLong(), input.readLong());
        }
        File metadata = new File(folder, "data/paper/metadata.dat");
        if (!metadata.isFile()) throw new IOException("No world identity file in " + folder);
        return paperMetadataUuid(metadata);
    }

    private UUID paperMetadataUuid(File file) throws IOException {
        try (DataInputStream input = new DataInputStream(
                new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))))) {
            if (input.readUnsignedByte() != 10) throw new IOException("World metadata is not an NBT compound");
            readNbtString(input);
            UUID uuid = null;
            for (int type; (type = input.readUnsignedByte()) != 0; ) {
                String name = readNbtString(input);
                if (type == 10 && "data".equals(name)) {
                    for (int inner; (inner = input.readUnsignedByte()) != 0; ) {
                        String innerName = readNbtString(input);
                        if (inner == 11 && "uuid".equals(innerName)) {
                            if (uuid != null || input.readInt() != 4) throw new IOException("Invalid world UUID");
                            uuid = new UUID(
                                    ((long) input.readInt() << 32) | (input.readInt() & 0xFFFFFFFFL),
                                    ((long) input.readInt() << 32) | (input.readInt() & 0xFFFFFFFFL));
                        } else {
                            skipNbt(input, inner);
                        }
                    }
                } else {
                    skipNbt(input, type);
                }
            }
            if (uuid == null || input.read() != -1) throw new IOException("World metadata has no single UUID");
            return uuid;
        }
    }

    private String readNbtString(DataInputStream input) throws IOException {
        byte[] bytes = new byte[input.readUnsignedShort()];
        input.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void skipNbt(DataInputStream input, int type) throws IOException {
        switch (type) {
            case 1 -> input.skipNBytes(1);
            case 2 -> input.skipNBytes(2);
            case 3, 5 -> input.skipNBytes(4);
            case 4, 6 -> input.skipNBytes(8);
            case 7 -> skipNbtBytes(input, input.readInt());
            case 8 -> readNbtString(input);
            case 9 -> {
                int listType = input.readUnsignedByte();
                int size = input.readInt();
                if (size < 0 || size > 1_000_000) throw new IOException("Invalid NBT list");
                for (int i = 0; i < size; i++) skipNbt(input, listType);
            }
            case 10 -> {
                for (int inner; (inner = input.readUnsignedByte()) != 0; ) {
                    readNbtString(input);
                    skipNbt(input, inner);
                }
            }
            case 11 -> skipNbtBytes(input, (long) input.readInt() * 4);
            case 12 -> skipNbtBytes(input, (long) input.readInt() * 8);
            default -> throw new IOException("Unsupported NBT tag " + type);
        }
    }

    private void skipNbtBytes(DataInputStream input, long length) throws IOException {
        if (length < 0 || length > 100_000_000L) throw new IOException("Invalid NBT length");
        input.skipNBytes(length);
    }

    private boolean eligibleDeferredIsland(JsonObject row, int days) throws IOException {
        for (String flag : List.of("deletable", "spawn", "purgeProtected")) {
            if (!row.has(flag) || !row.get(flag).isJsonPrimitive()
                    || !row.getAsJsonPrimitive(flag).isBoolean()) return false;
        }
        boolean deletable = row.get("deletable").getAsBoolean();
        if (row.get("spawn").getAsBoolean() || row.get("purgeProtected").getAsBoolean()) return false;
        boolean owned = row.has("owner") && !row.get("owner").isJsonNull();
        if (days == 0) return deletable && !owned;
        if (deletable) return !owned;
        if (!owned) return false;
        // Level is not loaded before worlds; inspect descriptors, never infer addon identity from filenames.
        if (deferredLevelProtectionUnavailable() && !safeDeferredLevel(row.get("uniqueId").getAsString())) {
            plugin.logWarning("Deferred age purge retained: Level record is protected, uninitialized, pending or unverifiable for "
                    + row.get("uniqueId").getAsString());
            return false;
        }
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        Set<UUID> members = new HashSet<>();
        members.add(UUID.fromString(row.get("owner").getAsString()));
        for (var member : row.getAsJsonObject("members").entrySet()) {
            if (member.getValue().getAsInt() >= RanksManager.MEMBER_RANK) members.add(UUID.fromString(member.getKey()));
        }
        for (UUID member : members) {
            File playerFile = new File(plugin.getDataFolder(), "database/Players/" + member + ".json");
            if (!playerFile.isFile()) return false;
            JsonObject player = JsonParser.parseString(Files.readString(playerFile.toPath())).getAsJsonObject();
            if (!member.toString().equals(player.get("uniqueId").getAsString())
                    || !player.has("lastLogin") || player.get("lastLogin").isJsonNull()
                    || player.get("lastLogin").getAsLong() <= 0
                    || player.get("lastLogin").getAsLong() >= cutoff
                    || Bukkit.getOfflinePlayer(member).getLastSeen() >= cutoff) return false;
        }
        return true;
    }

    private boolean safeDeferredLevel(String islandId) {
        try {
            File table = new File(plugin.getDataFolder(), "database/IslandLevels");
            File file = new File(table, islandId.endsWith(".json") ? islandId : islandId + ".json");
            if (!file.getCanonicalFile().getParentFile().equals(table.getCanonicalFile()) || !file.isFile()) return false;
            JsonObject data = new JsonObject();
            try (var reader = new com.google.gson.stream.JsonReader(Files.newBufferedReader(file.toPath()))) {
                reader.setStrictness(com.google.gson.Strictness.STRICT);
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (data.has(name)) return false;
                    data.add(name, JsonParser.parseReader(reader));
                }
                reader.endObject();
                if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) return false;
            }
            Set<String> numbers = Set.of("level", "initialLevel", "initialCount", "pointsToNextLevel", "maxLevel",
                    "totalPoints", "donatedPoints", "anonymousDeaths");
            Set<String> counts = Set.of("uwCount", "mdCount", "donatedBlocks", "memberDeaths");
            for (var field : data.entrySet()) {
                String name = field.getKey();
                var value = field.getValue();
                if (numbers.contains(name)) {
                    if (value.isJsonNull() && !Set.of("initialLevel", "initialCount", "donatedPoints").contains(name)) return false;
                    if (!value.isJsonNull()) strictLong(value);
                } else if (counts.contains(name)) {
                    if (!value.isJsonNull()) for (var count : value.getAsJsonObject().entrySet()) {
                        long amount = strictLong(count.getValue());
                        if (amount < 0 || amount > Integer.MAX_VALUE) return false;
                    }
                } else if (name.equals("uniqueId")) {
                    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                            || !islandId.equals(value.getAsString())) return false;
                } else if (name.equals("pendingInitialCountOwner")) {
                    if (!value.isJsonNull()) return false;
                } else if (name.equals("deathsMigrated")) {
                    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) return false;
                } else if (name.equals("donationLog")) {
                    if (!value.isJsonNull()) for (var donation : value.getAsJsonArray()) {
                        JsonObject record = donation.getAsJsonObject();
                        if (!record.keySet().equals(Set.of("timestamp", "donorUUID", "material", "count", "points"))) return false;
                        strictLong(record.get("timestamp"));
                        strictLong(record.get("count"));
                        strictLong(record.get("points"));
                        UUID.fromString(record.get("donorUUID").getAsString());
                        if (!record.getAsJsonPrimitive("material").isString()) return false;
                    }
                } else return false;
            }
            // getIslandLevel returns the stored current level; missing/default or pending baselines are not evidence.
            return data.has("uniqueId") && data.has("initialCount") && !data.get("initialCount").isJsonNull()
                    && data.has("level") && !data.get("level").isJsonNull()
                    && strictLong(data.get("level")) < plugin.getSettings().getIslandPurgeLevel();
        } catch (IOException | RuntimeException error) {
            plugin.logWarning("Deferred Level record retained for " + islandId + ": " + error.getMessage());
            return false;
        }
    }

    private long strictLong(com.google.gson.JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Expected an integer Level field");
        return value.getAsBigDecimal().longValueExact();
    }

    private boolean deferredLevelProtectionUnavailable() throws IOException {
        if (plugin.getAddonsManager() != null)
            return plugin.getAddonsManager().getAddonByName("Level").isPresent();
        File directory = new File(plugin.getDataFolder(), "addons");
        if (!directory.exists()) return false;
        File[] jars = directory.listFiles((dir, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".jar"));
        if (jars == null) throw new IOException("Cannot enumerate addons");
        boolean levelPresent = false;
        for (File file : jars) {
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(file)) {
                var descriptor = jar.getJarEntry("addon.yml");
                if (descriptor == null) throw new IOException("Missing addon descriptor: " + file.getName());
                YamlConfiguration yaml = new YamlConfiguration();
                try (var reader = new java.io.InputStreamReader(jar.getInputStream(descriptor),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    yaml.load(reader);
                } catch (InvalidConfigurationException error) {
                    throw new IOException("Invalid addon descriptor: " + file.getName(), error);
                }
                String name = yaml.getString("name");
                if (name == null) throw new IOException("Missing addon name: " + file.getName());
                if ("Level".equalsIgnoreCase(name)) levelPresent = true;
            }
        }
        return levelPresent;
    }

    private boolean hasResidualFiles(Island island, List<File> dimensions) {
        // Use the whole island space, not its possibly-shrunk current protection box.
        if (island.getMaxX() <= island.getMinX() || island.getMaxZ() <= island.getMinZ()) return true;
        int minX = island.getMinX() >> 9, maxX = (island.getMaxX() - 1) >> 9;
        int minZ = island.getMinZ() >> 9, maxZ = (island.getMaxZ() - 1) >> 9;
        for (File dimension : dimensions) for (String kind : List.of(REGION, ENTITIES, POI)) {
            File directory = new File(dimension, kind);
            if (!directory.exists()) continue;
            File[] files = directory.listFiles();
            if (files == null) return true;
            for (File file : files) {
                if (!file.getName().matches("r\\.-?\\d+\\.-?\\d+\\.mca")) continue;
                Pair<Integer, Integer> coords = parseRegionCoords(file.getName());
                if (coords == null || coords.x() >= minX && coords.x() <= maxX
                        && coords.z() >= minZ && coords.z() <= maxZ) return true;
            }
        }
        return false;
    }

    /**
     * Result of a purge scan — a map of deletable region coordinates to the
     * set of island IDs in each region, plus filtering statistics.
     *
     * @param world the world scanned
     * @param days  the age cutoff (days) used
     * @param deletableRegions regions considered deletable keyed by region
     *                          coordinate {@code (regionX, regionZ)}
     * @param isNether whether the nether dimension was included
     * @param isEnd    whether the end dimension was included
     * @param stats    filter statistics for logging/reporting
     */
    public record PurgeScanResult(
            World world,
            int days,
            Map<Pair<Integer, Integer>, Set<String>> deletableRegions,
            boolean isNether,
            boolean isEnd,
            FilterStats stats,
            Set<String> straddlingIslandIds) {
        public boolean isEmpty() {
            return deletableRegions.isEmpty();
        }

        public int uniqueIslandCount() {
            Set<String> ids = new HashSet<>();
            deletableRegions.values().forEach(ids::addAll);
            return ids.size();
        }
    }

    /** Tracks island-level and region-level block counts during filtering. */
    public record FilterStats(int islandsOverLevel, int islandsPurgeProtected,
            int regionsBlockedByLevel, int regionsBlockedByProtection) {}

    /** Groups the three folder types (region, entities, poi) for one world dimension. */
    private record DimFolders(File region, File entities, File poi) {}

    /** Return type for {@link #filterForDeletedSweep}: filter statistics plus the set of
     *  deletable island IDs whose chunks lie in a region that was blocked by a live neighbour. */
    private record DeleteSweepFilter(FilterStats stats, Set<String> straddlingIds) {}

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Scans the given world for islands flagged as {@code deletable} and
     * returns the set of region files that can be reaped immediately,
     * ignoring region-file age.
     *
     * <p>Unlike {@link #scan(World, int)} this does not look at region
     * timestamps at all: the {@code deletable} flag is the sole source of
     * truth. A region is only returned if <em>every</em> island that
     * overlaps it is deletable — a lone active neighbour blocks the whole
     * region.
     *
     * <p>The returned {@link PurgeScanResult} uses {@code days = 0} as a
     * sentinel meaning "no age filter" so that {@link #delete(PurgeScanResult)}
     * and {@code deleteRegionFiles} skip their freshness re-check.
     *
     * <p>Runs synchronously on the calling thread and performs disk I/O.
     * Callers must invoke this from an async task.
     *
     * @param world the gamemode overworld to scan
     * @return scan result, never {@code null}
     * @since 3.15.0
     */
    public PurgeScanResult scanDeleted(World world) {
        boolean isNether = plugin.getIWM().isNetherGenerate(world) && plugin.getIWM().isNetherIslands(world);
        boolean isEnd = plugin.getIWM().isEndGenerate(world) && plugin.getIWM().isEndIslands(world);

        IslandGrid islandGrid = plugin.getIslands().getIslandCache().getIslandGrid(world);
        if (islandGrid == null) {
            plugin.logWarning("Purge deleted-sweep: no island grid for world " + world.getName()
                    + " — skipping scan");
            return new PurgeScanResult(world, 0, new HashMap<>(), isNether, isEnd,
                    new FilterStats(0, 0, 0, 0), Set.of());
        }

        // Collect candidate region coords from every deletable island's
        // protection bounds. A single island may straddle multiple regions.
        Set<Pair<Integer, Integer>> candidateRegions = new HashSet<>();
        for (Island island : plugin.getIslands().getIslandCache().getIslands(world)) {
            if (!island.isDeletable()) continue;
            int minRX = island.getMinProtectedX() >> 9;
            int maxRX = (island.getMaxProtectedX() - 1) >> 9;
            int minRZ = island.getMinProtectedZ() >> 9;
            int maxRZ = (island.getMaxProtectedZ() - 1) >> 9;
            for (int rx = minRX; rx <= maxRX; rx++) {
                for (int rz = minRZ; rz <= maxRZ; rz++) {
                    candidateRegions.add(new Pair<>(rx, rz));
                }
            }
        }
        plugin.log("Purge deleted-sweep: " + candidateRegions.size()
                + " candidate region(s) from deletable islands in world " + world.getName());

        Map<Pair<Integer, Integer>, Set<String>> deletableRegions =
                mapIslandsToRegions(new ArrayList<>(candidateRegions), islandGrid);
        DeleteSweepFilter filterResult = filterForDeletedSweep(deletableRegions);
        logFilterStats(filterResult.stats());
        if (!filterResult.straddlingIds().isEmpty()) {
            plugin.log("Purge deleted-sweep: " + filterResult.straddlingIds().size()
                    + " island(s) straddle a blocked region — DB row(s) retained for next sweep");
        }
        return new PurgeScanResult(world, 0, deletableRegions, isNether, isEnd,
                filterResult.stats(), filterResult.straddlingIds());
    }

    /**
     * Scans the given world (and its nether/end if the gamemode owns them)
     * for region files older than {@code days} and returns the set of
     * regions whose overlapping islands may all be safely deleted.
     *
     * <p>Runs synchronously on the calling thread and performs disk I/O.
     * Callers must invoke this from an async task.
     *
     * @param world the gamemode overworld to scan
     * @param days  minimum age in days for region files to be candidates
     * @return scan result, never {@code null}
     */
    public PurgeScanResult scan(World world, int days) {
        boolean isNether = plugin.getIWM().isNetherGenerate(world) && plugin.getIWM().isNetherIslands(world);
        boolean isEnd = plugin.getIWM().isEndGenerate(world) && plugin.getIWM().isEndIslands(world);

        IslandGrid islandGrid = plugin.getIslands().getIslandCache().getIslandGrid(world);
        if (islandGrid == null) {
            plugin.logWarning("Purge age-sweep: no island grid for world " + world.getName()
                    + " — skipping scan");
            return new PurgeScanResult(world, days, new HashMap<>(), isNether, isEnd,
                    new FilterStats(0, 0, 0, 0), Set.of());
        }

        List<Pair<Integer, Integer>> oldRegions = findOldRegions(world, days, isNether, isEnd);
        Map<Pair<Integer, Integer>, Set<String>> deletableRegions = mapIslandsToRegions(oldRegions, islandGrid);
        FilterStats stats = filterNonDeletableRegions(deletableRegions, days);
        logFilterStats(stats);
        return new PurgeScanResult(world, days, deletableRegions, isNether, isEnd, stats, Set.of());
    }

    /**
     * Deletes the region files identified by a prior {@link #scan(World, int)}
     * along with any island database entries, island cache entries, and
     * orphaned player data files that correspond to them.
     *
     * <p>Performs disk I/O on the calling thread and should be invoked from
     * an async task. The final bookkeeping — firing {@link IslandEvent}s and
     * removing islands from the cache and database — is hopped onto the main
     * thread and awaited before this method returns, because Paper only
     * allows {@link IslandEvent} to be fired synchronously. Callers are also
     * responsible for flushing in-memory chunk state by calling
     * {@code World.save()} on the main thread <b>before</b> dispatching
     * this method — {@code World.save()} is not safe to invoke from an
     * async thread.
     *
     * @param scan the prior scan result
     * @return {@code true} if all file deletions succeeded; {@code false} if
     *         any file was unexpectedly fresh or could not be deleted
     */
    public boolean delete(PurgeScanResult scan) {
        if (scan.deletableRegions().isEmpty()) {
            return false;
        }
        if (TaskScheduler.isFolia()) {
            // Region scheduling cannot close server-owned region-file handles safely.
            return defer(scan);
        }
        plugin.log("Now deleting region files for world " + scan.world().getName());
        boolean ok = deleteRegionFiles(scan);
        if (!ok) {
            plugin.logError("Not all region files could be deleted");
            // A partial disk cleanup must never remove the island row or add it
            // to the shutdown queue. Retaining the row keeps the next sweep
            // able to find and retry the remaining region file.
            return false;
        }

        // Collect unique island IDs across all reaped regions. An island
        // that spans multiple regions will only be considered once here.
        Set<String> affectedIds = new HashSet<>();
        for (Set<String> islandIDs : scan.deletableRegions().values()) {
            affectedIds.addAll(islandIDs);
        }

        // Phase 1 (calling thread, disk I/O): decide which islands are fully
        // reaped and delete their orphaned player data. No Bukkit events or
        // cache/DB mutations happen here — this method is normally invoked
        // from an async task and IslandEvent may only be fired on the main
        // thread (Paper enforces this and throws IllegalStateException).
        int islandsDeferred = 0;
        List<Island> toFinalize = new ArrayList<>();
        for (String islandID : affectedIds) {
            Optional<Island> opt = plugin.getIslands().getIslandById(islandID);
            if (opt.isEmpty()) {
                continue;
            }
            Island island = opt.get();

            if (scan.days() == 0) {
                if (scan.straddlingIslandIds().contains(islandID)) {
                    // This island has chunks in a region that was blocked by an
                    // active neighbour island — those blocks are still on disk.
                    // Retaining the DB row (deletable=true) lets the next
                    // deleted-sweep retry once the blocker is itself gone.
                    islandsDeferred++;
                    plugin.log("Island ID " + islandID
                            + " straddles a blocked region \u2014 DB row retained for next purge sweep");
                    continue;
                }
                // Deleted sweep: region files are gone from disk but Paper
                // may still serve stale chunk data from its internal memory
                // cache. Defer DB row removal to plugin shutdown when the
                // cache is guaranteed clear.
                pendingDeletions.add(islandID);
                pendingFiles.put(islandID, scanDimensions(scan));
                islandsDeferred++;
                plugin.log("Island ID " + islandID
                        + " region files deleted \u2014 DB row deferred to shutdown");
            } else {
                // Age sweep: regions are old enough that Paper won't have
                // them cached. Gate on residual-region completeness check
                // to avoid orphaning blocks when the strict filter blocked
                // some of the island's regions.
                List<Pair<Integer, Integer>> residual = findResidualRegions(island, scan.world());
                if (!residual.isEmpty()) {
                    islandsDeferred++;
                    plugin.log("Island ID " + islandID + " has " + residual.size()
                            + " residual region(s) still on disk: " + residual
                            + " \u2014 DB row retained for a future purge");
                    continue;
                }
                deletePlayerFromWorldFolder(scan.world(), islandID, scan.deletableRegions(), scan.days());
                toFinalize.add(island);
            }
        }

        // Phase 2 (main thread): fire events and remove the islands from the
        // cache and database. Blocks the calling thread until done so the
        // return value and the summary log below reflect the final state.
        int islandsRemoved = toFinalize.isEmpty() ? 0 : runOnMainThread(() -> finalizeIslands(toFinalize));
        plugin.log("Purge complete for world " + scan.world().getName()
                + ": " + scan.deletableRegions().size() + " region(s), "
                + islandsRemoved + " island(s) removed, "
                + islandsDeferred + " island(s) deferred"
                + (scan.days() == 0 ? " (to shutdown)" : " (partial cleanup)"));
        return ok;
    }

    /**
     * Processes all island IDs whose region files were deleted by a prior
     * deleted-sweep but whose DB rows were deferred because Paper's internal
     * memory cache may still serve stale chunk data. Call this on plugin
     * shutdown when the cache is guaranteed to be cleared.
     *
     * <p>If the server crashes before a clean shutdown, the pending set is
     * lost — the islands stay {@code deletable=true} in the database and the
     * next purge cycle will pick them up again (safe failure mode).
     */
    public void flushPendingDeletions() {
        if (pendingDeletions.isEmpty()) {
            return;
        }
        plugin.log("Flushing " + pendingDeletions.size() + " deferred island deletion(s)...");
        int count = 0;
        Set<String> flushed = new HashSet<>();
        for (String islandID : pendingDeletions) {
            // Capture island before cache eviction so we can build the event.
            // DELETED was already fired at soft-delete time; PURGED signals that
            // the DB row is now physically removed.
            Optional<Island> opt = plugin.getIslands().getIslandById(islandID);
            if (opt.isEmpty() || !opt.get().isDeletable() || opt.get().isOwned()
                    || opt.get().isSpawn() || opt.get().isPurgeProtected()) {
                flushed.add(islandID);
                continue;
            }
            List<File> dimensions = pendingFiles.get(islandID);
            if (dimensions == null || hasResidualFiles(opt.get(), dimensions)) {
                plugin.logWarning("Deferred island row retained: residual files could not be ruled out for " + islandID);
                continue;
            }
            if (plugin.getIslands().deleteIslandId(islandID)) {
                plugin.getIslands().getIslandCache().deleteIslandFromCache(islandID);
                count++;
                flushed.add(islandID);
                opt.ifPresent(island -> IslandEvent.builder()
                        .island(island)
                        .reason(Reason.PURGED)
                        .build());
            }
        }
        pendingDeletions.removeAll(flushed);
        flushed.forEach(pendingFiles::remove);
        plugin.log("Flushed " + count + " island(s) from cache and database");
    }

    /**
     * Returns an unmodifiable view of the island IDs currently pending
     * DB deletion (deferred to shutdown). Primarily for testing.
     */
    public Set<String> getPendingDeletions() {
        return Collections.unmodifiableSet(pendingDeletions);
    }

    /**
     * Removes each fully-reaped island from the cache and database, firing
     * {@link Reason#DELETED} first if the island was never soft-deleted and
     * {@link Reason#PURGED} once the row is gone. <b>Main thread only</b>:
     * {@link IslandEvent} is a synchronous event.
     *
     * @param islands islands whose region files and player data are already gone
     * @return the number of islands actually removed from the database
     */
    private int finalizeIslands(List<Island> islands) {
        int removed = 0;
        for (Island island : islands) {
            String islandID = island.getUniqueId();
            // For the age sweep, DELETED may never have fired (the island was
            // pruned by age, not by an explicit /is reset). Fire it now so addons
            // (Level, OneBlock, etc.) can clean up their per-island data.
            if (!island.isDeletable()) {
                IslandEvent.builder()
                        .island(island)
                        .reason(Reason.DELETED)
                        .build();
            }
            if (plugin.getIslands().deleteIslandId(islandID)) {
                plugin.getIslands().getIslandCache().deleteIslandFromCache(islandID);
                plugin.log("Island ID " + islandID + " deleted from cache and database");
                removed++;
                // Fire PURGED so addons that track physical storage state know
                // the region files and DB row are both gone.
                IslandEvent.builder()
                        .island(island)
                        .reason(Reason.PURGED)
                        .build();
            }
        }
        return removed;
    }

    /**
     * Runs {@code task} on the main server thread and waits for its result.
     * If already on the main thread the task runs inline.
     *
     * @param task work that must run on the main thread
     * @return the task's result
     * @throws IllegalStateException if the task could not be scheduled or failed
     */
    private <T> T runOnMainThread(Supplier<T> task) {
        if (TaskScheduler.isFolia() ? Bukkit.isGlobalTickThread() : Bukkit.isPrimaryThread()) {
            return task.get();
        }
        CompletableFuture<T> done = new CompletableFuture<>();
        try {
            TaskScheduler.global(plugin, () -> {
                try {
                    done.complete(task.get());
                } catch (Exception e) {
                    done.completeExceptionally(e);
                }
            });
        } catch (RuntimeException e) {
            // Typically IllegalPluginAccessException: the plugin is disabling and
            // the scheduler will not accept new tasks.
            throw new IllegalStateException("Could not schedule purge finalization on the main thread", e);
        }
        try {
            return done.join();
        } catch (CompletionException e) {
            throw new IllegalStateException("Purge finalization failed on the main thread", e.getCause());
        }
    }

    /**
     * Returns the region coordinates for every {@code r.X.Z.mca} file still
     * present on disk that overlaps the island's protection box, across the
     * overworld and (if the gamemode owns them) the nether and end
     * dimensions. An empty list means every region file the island touches
     * is gone from disk and the island DB row can safely be reaped.
     *
     * <p>The protection box is converted to region coordinates with
     * {@code blockX >> 9} (each .mca covers a 512×512 block square). The
     * maximum bound is inclusive at the block level so we shift
     * {@code max - 1} to avoid picking up a neighbour region when the
     * protection ends exactly on a region boundary.
     */
    private List<Pair<Integer, Integer>> findResidualRegions(Island island, World overworld) {
        int rxMin = island.getMinProtectedX() >> 9;
        int rxMax = (island.getMaxProtectedX() - 1) >> 9;
        int rzMin = island.getMinProtectedZ() >> 9;
        int rzMax = (island.getMaxProtectedZ() - 1) >> 9;

        File base = overworld.getWorldFolder();
        File overworldRegionDir = new File(base, REGION);

        World netherWorld = plugin.getIWM().getNetherWorld(overworld);
        File netherRegionDir = plugin.getIWM().isNetherIslands(overworld)
                ? new File(netherWorld != null ? resolveDataFolder(netherWorld) : resolveNetherFallback(base), REGION)
                : null;

        World endWorld = plugin.getIWM().getEndWorld(overworld);
        File endRegionDir = plugin.getIWM().isEndIslands(overworld)
                ? new File(endWorld != null ? resolveDataFolder(endWorld) : resolveEndFallback(base), REGION)
                : null;

        List<Pair<Integer, Integer>> residual = new ArrayList<>();
        for (int rx = rxMin; rx <= rxMax; rx++) {
            for (int rz = rzMin; rz <= rzMax; rz++) {
                String name = "r." + rx + "." + rz + ".mca";
                if (regionFileExists(overworldRegionDir, name)
                        || regionFileExists(netherRegionDir, name)
                        || regionFileExists(endRegionDir, name)) {
                    residual.add(new Pair<>(rx, rz));
                }
            }
        }
        return residual;
    }

    private static boolean regionFileExists(File dir, String name) {
        return dir != null && new File(dir, name).exists();
    }

    // ---------------------------------------------------------------
    // Chunk eviction
    // ---------------------------------------------------------------

    /**
     * Unloads every loaded chunk that falls inside any region in
     * {@code scan.deletableRegions()} with {@code save = false}, so the
     * in-memory chunk copy is thrown away rather than flushed back over the
     * region files we are about to delete.
     *
     * <p>Each {@code r.X.Z.mca} covers a 32×32 chunk square. For every target
     * region this iterates {@code (rX*32 .. rX*32+31, rZ*32 .. rZ*32+31)} and
     * unloads any chunk currently loaded. Chunks that cannot be unloaded
     * (e.g. a player is inside, or the chunk is force-loaded) are silently
     * skipped — reaping a chunk out from under a present player would be
     * worse than waiting for the next sweep.
     *
     * <p>The deleted-sweep callers (manual command + housekeeping) must
     * invoke this on the main thread <b>before</b> dispatching the async
     * {@link #delete(PurgeScanResult)}; otherwise Paper's autosave or shutdown
     * will rewrite the region file with the stale in-memory chunks immediately
     * after we delete it on disk.
     *
     * <p>Nether and end dimensions are evicted only when the gamemode owns
     * them, mirroring the dimension gating in {@link #deleteRegionFiles}.
     *
     * @param scan a prior scan result whose regions should be evicted
     */
    public void evictChunks(PurgeScanResult scan) {
        if (TaskScheduler.isFolia()) {
            throw new IllegalStateException("Use evictChunksAsync on region-threaded servers");
        }
        if (scan.deletableRegions().isEmpty()) {
            return;
        }
        World overworld = scan.world();
        if (overworld == null) {
            return;
        }
        World netherWorld = scan.isNether() ? plugin.getIWM().getNetherWorld(overworld) : null;
        World endWorld = scan.isEnd() ? plugin.getIWM().getEndWorld(overworld) : null;

        int evicted = 0;
        for (Pair<Integer, Integer> coords : scan.deletableRegions().keySet()) {
            int baseCx = coords.x() << 5;   // rX * 32
            int baseCz = coords.z() << 5;
            evicted += evictRegion(overworld, baseCx, baseCz);
            if (netherWorld != null) {
                evicted += evictRegion(netherWorld, baseCx, baseCz);
            }
            if (endWorld != null) {
                evicted += evictRegion(endWorld, baseCx, baseCz);
            }
        }
        plugin.log("Purge deleted: evicted " + evicted + " loaded chunk(s) from "
                + scan.deletableRegions().size() + " target region(s)");
    }

    private int evictRegion(World world, int baseCx, int baseCz) {
        int count = 0;
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                int cx = baseCx + dx;
                int cz = baseCz + dz;
                if (world.isChunkLoaded(cx, cz) && world.unloadChunk(cx, cz, false)) {
                    count++;
                }
            }
        }
        return count;
    }

    private List<File> scanDimensions(PurgeScanResult scan) {
        List<File> dimensions = new ArrayList<>();
        File base = scan.world().getWorldFolder();
        dimensions.add(base);
        if (scan.isNether()) {
            World nether = plugin.getIWM().getNetherWorld(scan.world());
            dimensions.add(nether == null ? resolveNetherFallback(base) : resolveDataFolder(nether));
        }
        if (scan.isEnd()) {
            World end = plugin.getIWM().getEndWorld(scan.world());
            dimensions.add(end == null ? resolveEndFallback(base) : resolveDataFolder(end));
        }
        return dimensions;
    }

    /** Await only from an async task; failed eviction must prevent region-file deletion. */
    public CompletableFuture<Void> evictChunksAsync(PurgeScanResult scan) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        List<World> worlds = new ArrayList<>();
        worlds.add(scan.world());
        if (scan.isNether()) worlds.add(plugin.getIWM().getNetherWorld(scan.world()));
        if (scan.isEnd()) worlds.add(plugin.getIWM().getEndWorld(scan.world()));
        for (World dimension : worlds) {
            if (dimension == null) continue;
            for (Pair<Integer, Integer> region : scan.deletableRegions().keySet()) {
                for (int dx = 0; dx < 32; dx++) {
                    for (int dz = 0; dz < 32; dz++) {
                        int cx = (region.x() << 5) + dx;
                        int cz = (region.z() << 5) + dz;
                        if (!dimension.isChunkLoaded(cx, cz)) continue;
                        tasks.add(TaskScheduler.regionFuture(plugin,
                                new Location(dimension, cx << 4, 0, cz << 4), () -> {
                                    if (dimension.isChunkLoaded(cx, cz) && !dimension.unloadChunk(cx, cz, false)) {
                                        throw new IllegalStateException("Chunk is still in use: " + dimension.getName() + " " + cx + "," + cz);
                                    }
                                }));
                    }
                }
            }
        }
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    // ---------------------------------------------------------------
    // Debug / testing: artificially age region files
    // ---------------------------------------------------------------

    /**
     * Debug/testing utility. Rewrites the per-chunk timestamp table of every
     * {@code .mca} region file in the given world's overworld (and nether/end
     * if the gamemode owns those dimensions) so that every chunk entry looks
     * like it was last written {@code days} days ago.
     *
     * <p>The purge scanner reads per-chunk timestamps from the second 4KB
     * block of each region file's header (not file mtime), so {@code touch}
     * cannot fake ageing. This rewrites that 4KB timestamp table in place,
     * setting all 1024 slots to {@code now - days*86400} seconds. File mtime
     * is not modified.
     *
     * <p>Runs synchronously and performs disk I/O. Callers must invoke
     * this from an async task, and should call {@code World.save()} on the
     * main thread first to flush in-memory chunk state.
     *
     * @param world the gamemode overworld whose regions should be aged
     * @param days  how many days in the past to pretend the regions were
     *              last written
     * @return number of {@code .mca} files successfully rewritten across
     *         all dimensions
     */
    public int ageRegions(World world, int days) {
        boolean isNether = plugin.getIWM().isNetherGenerate(world) && plugin.getIWM().isNetherIslands(world);
        boolean isEnd = plugin.getIWM().isEndGenerate(world) && plugin.getIWM().isEndIslands(world);

        File worldDir = world.getWorldFolder();
        File overworldRegion = new File(worldDir, REGION);

        World netherWorld = plugin.getIWM().getNetherWorld(world);
        File netherRegion = new File(
                netherWorld != null ? resolveDataFolder(netherWorld) : resolveNetherFallback(worldDir), REGION);

        World endWorld = plugin.getIWM().getEndWorld(world);
        File endRegion = new File(
                endWorld != null ? resolveDataFolder(endWorld) : resolveEndFallback(worldDir), REGION);

        long targetSeconds = (System.currentTimeMillis() / 1000L) - (days * 86400L);
        int total = 0;
        total += ageRegionsInFolder(overworldRegion, "overworld", targetSeconds);
        if (isNether) {
            total += ageRegionsInFolder(netherRegion, "nether", targetSeconds);
        }
        if (isEnd) {
            total += ageRegionsInFolder(endRegion, "end", targetSeconds);
        }
        return total;
    }

    private int ageRegionsInFolder(File folder, String dimension, long targetSeconds) {
        if (!folder.isDirectory()) {
            plugin.log("Age-regions: " + dimension + " folder does not exist, skipping: "
                    + folder.getAbsolutePath());
            return 0;
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".mca"));
        if (files == null || files.length == 0) {
            plugin.log("Age-regions: no .mca files in " + dimension + " folder " + folder.getAbsolutePath());
            return 0;
        }
        int count = 0;
        for (File file : files) {
            if (writeTimestampTable(file, targetSeconds)) {
                count++;
            }
        }
        plugin.log("Age-regions: rewrote " + count + "/" + files.length + " " + dimension + " region file(s)");
        return count;
    }

    /**
     * Overwrites the 4KB timestamp table (bytes 4096..8191) of a Minecraft
     * {@code .mca} file with a single repeating big-endian int timestamp.
     *
     * @param regionFile    the file to rewrite
     * @param targetSeconds the Unix timestamp (seconds) to write into every slot
     * @return {@code true} if the table was rewritten
     */
    private boolean writeTimestampTable(File regionFile, long targetSeconds) {
        if (!regionFile.exists() || regionFile.length() < 8192) {
            plugin.log("Age-regions: skipping " + regionFile.getName()
                    + " (missing or smaller than 8192 bytes)");
            return false;
        }
        byte[] table = new byte[4096];
        int ts = (int) targetSeconds;
        for (int i = 0; i < 1024; i++) {
            int offset = i * 4;
            table[offset]     = (byte) (ts >> 24);
            table[offset + 1] = (byte) (ts >> 16);
            table[offset + 2] = (byte) (ts >> 8);
            table[offset + 3] = (byte)  ts;
        }
        try (RandomAccessFile raf = new RandomAccessFile(regionFile, "rw")) {
            raf.seek(4096);
            raf.write(table);
            return true;
        } catch (IOException e) {
            plugin.logError("Age-regions: failed to rewrite timestamp table of "
                    + regionFile.getAbsolutePath() + ": " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------
    // Filtering
    // ---------------------------------------------------------------

    /**
     * Removes regions whose island-set contains at least one island that
     * cannot be deleted, returning blocking statistics.
     */
    private FilterStats filterNonDeletableRegions(
            Map<Pair<Integer, Integer>, Set<String>> deletableRegions, int days) {
        int islandsOverLevel = 0;
        int islandsPurgeProtected = 0;
        int regionsBlockedByLevel = 0;
        int regionsBlockedByProtection = 0;

        var iter = deletableRegions.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            int[] regionCounts = evaluateRegionIslands(entry.getValue(), days);
            if (regionCounts[0] > 0) { // shouldRemove
                iter.remove();
                islandsOverLevel += regionCounts[1];
                islandsPurgeProtected += regionCounts[2];
                if (regionCounts[1] > 0) regionsBlockedByLevel++;
                if (regionCounts[2] > 0) regionsBlockedByProtection++;
            }
        }
        return new FilterStats(islandsOverLevel, islandsPurgeProtected,
                regionsBlockedByLevel, regionsBlockedByProtection);
    }

    /**
     * Strict filter for the deleted sweep: any non-deletable island in a
     * region blocks the whole region. Unlike {@link #filterNonDeletableRegions}
     * this has no age/login/level logic — only the {@code deletable} flag
     * matters.
     *
     * <p>When a region is blocked, any <em>deletable</em> islands in that region
     * are recorded as "straddling" — they have chunks in a blocked region that
     * cannot be reaped this sweep. Their DB rows must be retained so the next
     * sweep can retry once the blocking island is itself deleted.
     */
    private DeleteSweepFilter filterForDeletedSweep(
            Map<Pair<Integer, Integer>, Set<String>> deletableRegions) {
        int regionsBlockedByProtection = 0;
        Set<String> straddling = new HashSet<>();
        var iter = deletableRegions.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            Set<String> ids = entry.getValue();
            boolean block = false;
            for (String id : ids) {
                Optional<Island> opt = plugin.getIslands().getIslandById(id);
                if (opt.isEmpty()) {
                    // Missing rows don't block — they're already gone.
                    continue;
                }
                if (!opt.get().isDeletable()) {
                    block = true;
                    break;
                }
            }
            if (block) {
                // Collect the deletable islands whose chunks lie in this blocked
                // region — they straddle the boundary between a reaped region and
                // this blocked one, so their blocks remain on disk here.
                for (String id : ids) {
                    plugin.getIslands().getIslandById(id)
                            .filter(Island::isDeletable)
                            .ifPresent(i -> straddling.add(i.getUniqueId()));
                }
                iter.remove();
                regionsBlockedByProtection++;
            }
        }
        return new DeleteSweepFilter(new FilterStats(0, 0, 0, regionsBlockedByProtection), straddling);
    }

    private int[] evaluateRegionIslands(Set<String> islandIds, int days) {
        int shouldRemove = 0;
        int levelBlocked = 0;
        int purgeBlocked = 0;
        for (String id : islandIds) {
            Optional<Island> opt = plugin.getIslands().getIslandById(id);
            if (opt.isEmpty()) {
                shouldRemove = 1;
                continue;
            }
            Island isl = opt.get();
            if (cannotDeleteIsland(isl, days)) {
                shouldRemove = 1;
                if (isl.isPurgeProtected()) purgeBlocked++;
                if (isLevelTooHigh(isl)) levelBlocked++;
            }
        }
        return new int[] { shouldRemove, levelBlocked, purgeBlocked };
    }

    private void logFilterStats(FilterStats stats) {
        if (stats.islandsOverLevel() > 0) {
            plugin.log("Purge: " + stats.islandsOverLevel() + " island(s) exceed the level threshold of "
                    + plugin.getSettings().getIslandPurgeLevel()
                    + " - preventing " + stats.regionsBlockedByLevel() + " region(s) from being purged");
        }
        if (stats.islandsPurgeProtected() > 0) {
            plugin.log("Purge: " + stats.islandsPurgeProtected() + " island(s) are purge-protected"
                    + " - preventing " + stats.regionsBlockedByProtection() + " region(s) from being purged");
        }
    }

    /**
     * Check if an island cannot be deleted. Purge protected, spawn, or
     * unowned (non-deletable) islands cannot be deleted. Islands whose members
     * recently logged in, or that exceed the level threshold, cannot be
     * deleted.
     *
     * @param island island
     * @param days   the age cutoff
     * @return {@code true} if the island cannot be deleted
     */
    public boolean cannotDeleteIsland(Island island, int days) {
        if (island.isDeletable()) {
            return false;
        }
        long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        boolean recentLogin = island.getMemberSet().stream().anyMatch(uuid -> {
            Long lastLogin = plugin.getPlayers().getLastLoginTimestamp(uuid);
            if (lastLogin == null) {
                lastLogin = Bukkit.getOfflinePlayer(uuid).getLastSeen();
            }
            return lastLogin >= cutoffMillis;
        });
        if (recentLogin) {
            return true;
        }
        if (isLevelTooHigh(island)) {
            return true;
        }
        return island.isPurgeProtected() || island.isSpawn() || !island.isOwned();
    }

    private boolean isLevelTooHigh(Island island) {
        return plugin.getAddonsManager().getAddonByName("Level")
                .map(l -> ((Level) l).getIslandLevel(island.getWorld(), island.getOwner())
                        >= plugin.getSettings().getIslandPurgeLevel())
                .orElse(false);
    }

    // ---------------------------------------------------------------
    // Scan
    // ---------------------------------------------------------------

    /**
     * Finds all region files in the overworld (and optionally nether/end)
     * that have not been modified in the last {@code days} days.
     */
    private List<Pair<Integer, Integer>> findOldRegions(World world, int days, boolean isNether, boolean isEnd) {
        File worldDir = world.getWorldFolder();
        File overworldRegion = new File(worldDir, REGION);

        World netherWorld = plugin.getIWM().getNetherWorld(world);
        File netherBase = netherWorld != null ? resolveDataFolder(netherWorld) : resolveNetherFallback(worldDir);
        File netherRegion = new File(netherBase, REGION);

        World endWorld = plugin.getIWM().getEndWorld(world);
        File endBase = endWorld != null ? resolveDataFolder(endWorld) : resolveEndFallback(worldDir);
        File endRegion = new File(endBase, REGION);

        long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);

        logRegionFolderPaths(overworldRegion, netherRegion, endRegion, world, isNether, isEnd);

        Set<String> candidateNames = collectCandidateNames(overworldRegion, netherRegion, endRegion, isNether, isEnd);
        plugin.log("Purge total candidate region coordinates: " + candidateNames.size());
        plugin.log("Purge checking candidate region(s) against island data, please wait...");

        List<Pair<Integer, Integer>> regions = new ArrayList<>();
        for (String name : candidateNames) {
            Pair<Integer, Integer> coords = parseRegionCoords(name);
            if (coords == null) continue;
            if (!isAnyDimensionFresh(name, overworldRegion, netherRegion, endRegion, cutoffMillis, isNether, isEnd)) {
                regions.add(coords);
            }
        }
        return regions;
    }

    private void logRegionFolderPaths(File overworldRegion, File netherRegion, File endRegion,
            World world, boolean isNether, boolean isEnd) {
        plugin.log("Purge region folders - Overworld: " + overworldRegion.getAbsolutePath()
                + EXISTS_PREFIX + overworldRegion.isDirectory() + ")");
        if (isNether) {
            plugin.log("Purge region folders - Nether: " + netherRegion.getAbsolutePath()
                    + EXISTS_PREFIX + netherRegion.isDirectory() + ")");
        } else {
            plugin.log("Purge region folders - Nether: disabled (isNetherGenerate="
                    + plugin.getIWM().isNetherGenerate(world) + ", isNetherIslands="
                    + plugin.getIWM().isNetherIslands(world) + ")");
        }
        if (isEnd) {
            plugin.log("Purge region folders - End: " + endRegion.getAbsolutePath()
                    + EXISTS_PREFIX + endRegion.isDirectory() + ")");
        } else {
            plugin.log("Purge region folders - End: disabled (isEndGenerate="
                    + plugin.getIWM().isEndGenerate(world) + ", isEndIslands="
                    + plugin.getIWM().isEndIslands(world) + ")");
        }
    }

    private Set<String> collectCandidateNames(File overworldRegion, File netherRegion, File endRegion,
            boolean isNether, boolean isEnd) {
        Set<String> names = new HashSet<>();
        addFileNames(names, overworldRegion.listFiles((dir, name) -> name.endsWith(".mca")), "overworld");
        if (isNether) {
            addFileNames(names, netherRegion.listFiles((dir, name) -> name.endsWith(".mca")), "nether");
        }
        if (isEnd) {
            addFileNames(names, endRegion.listFiles((dir, name) -> name.endsWith(".mca")), "end");
        }
        return names;
    }

    private void addFileNames(Set<String> names, File[] files, String dimension) {
        if (files != null) {
            for (File f : files) names.add(f.getName());
        }
        plugin.log(PURGE_FOUND + (files != null ? files.length : 0) + " " + dimension + " region files");
    }

    private Pair<Integer, Integer> parseRegionCoords(String name) {
        String coordsPart = name.substring(2, name.length() - 4);
        String[] parts = coordsPart.split("\\.");
        if (parts.length != 2) return null;
        try {
            return new Pair<>(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Maps each old region to the set of island IDs whose island-squares
     * overlap it. Each region covers a 512x512 block square.
     */
    private Map<Pair<Integer, Integer>, Set<String>> mapIslandsToRegions(
            List<Pair<Integer, Integer>> oldRegions, IslandGrid islandGrid) {
        final int blocksPerRegion = 512;
        Map<Pair<Integer, Integer>, Set<String>> regionToIslands = new HashMap<>();

        for (Pair<Integer, Integer> region : oldRegions) {
            int regionMinX = region.x() * blocksPerRegion;
            int regionMinZ = region.z() * blocksPerRegion;
            int regionMaxX = regionMinX + blocksPerRegion - 1;
            int regionMaxZ = regionMinZ + blocksPerRegion - 1;

            Set<String> ids = new HashSet<>();
            for (IslandData data : islandGrid.getIslandsInBounds(regionMinX, regionMinZ, regionMaxX, regionMaxZ)) {
                ids.add(data.id());
            }
            regionToIslands.put(region, ids);
        }
        return regionToIslands;
    }

    // ---------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------

    private boolean deleteRegionFiles(PurgeScanResult scan) {
        int days = scan.days();
        if (days < 0) {
            plugin.logError("Days is somehow negative!");
            return false;
        }
        // days == 0 is the "deleted sweep" sentinel — no age filter and no
        // freshness recheck. days > 0 is the age-based sweep.
        boolean ageGated = days > 0;
        long cutoffMillis = ageGated ? System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days) : 0L;

        World world = scan.world();
        File base = world.getWorldFolder();
        File overworldRegion   = new File(base, REGION);
        File overworldEntities = new File(base, ENTITIES);
        File overworldPoi      = new File(base, POI);

        World netherWorld = plugin.getIWM().getNetherWorld(world);
        File netherBase     = netherWorld != null ? resolveDataFolder(netherWorld) : resolveNetherFallback(base);
        File netherRegion   = new File(netherBase, REGION);
        File netherEntities = new File(netherBase, ENTITIES);
        File netherPoi      = new File(netherBase, POI);

        World endWorld = plugin.getIWM().getEndWorld(world);
        File endBase     = endWorld != null ? resolveDataFolder(endWorld) : resolveEndFallback(base);
        File endRegion   = new File(endBase, REGION);
        File endEntities = new File(endBase, ENTITIES);
        File endPoi      = new File(endBase, POI);

        // Verify none of the files have been updated since the cutoff.
        // Skipped for the deleted sweep (ageGated == false) — the deletable
        // flag on the island row is the sole authority there.
        if (ageGated) {
            for (Pair<Integer, Integer> coords : scan.deletableRegions().keySet()) {
                String name = "r." + coords.x() + "." + coords.z() + ".mca";
                if (isAnyDimensionFresh(name, overworldRegion, netherRegion, endRegion, cutoffMillis,
                        scan.isNether(), scan.isEnd())) {
                    return false;
                }
            }
        }

        DimFolders ow     = new DimFolders(overworldRegion, overworldEntities, overworldPoi);
        DimFolders nether = new DimFolders(netherRegion,    netherEntities,    netherPoi);
        DimFolders end    = new DimFolders(endRegion,       endEntities,       endPoi);
        boolean allOk = true;
        for (Pair<Integer, Integer> coords : scan.deletableRegions().keySet()) {
            String name = "r." + coords.x() + "." + coords.z() + ".mca";
            if (!deleteOneRegion(name, ow, nether, end, scan.isNether(), scan.isEnd())) {
                plugin.logError("Could not delete all the region/entity/poi files for some reason");
                allOk = false;
            }
        }
        return allOk;
    }

    private boolean deleteOneRegion(String name, DimFolders overworld, DimFolders nether, DimFolders end,
            boolean isNether, boolean isEnd) {
        // Remove sidecars first. If one cannot be removed, leave the region
        // file in place so a later scan can discover and retry the cleanup.
        boolean ok = deleteDimensionFiles(name, overworld);
        if (isNether) {
            ok = ok && deleteDimensionFiles(name, nether);
        }
        if (isEnd) {
            ok = ok && deleteDimensionFiles(name, end);
        }
        return ok;
    }

    private boolean deleteDimensionFiles(String name, DimFolders folders) {
        return deleteIfExists(new File(folders.entities(), name))
                && deleteIfExists(new File(folders.poi(), name))
                && deleteIfExists(new File(folders.region(), name));
    }

    private boolean deleteIfExists(File file) {
        if (!file.getParentFile().exists()) {
            return true;
        }
        try {
            Files.deleteIfExists(file.toPath());
            return true;
        } catch (IOException e) {
            plugin.logError("Failed to delete file: " + file.getAbsolutePath());
            return false;
        }
    }

    // ---------------------------------------------------------------
    // Player data cleanup
    // ---------------------------------------------------------------

    private void deletePlayerFromWorldFolder(World world, String islandID,
            Map<Pair<Integer, Integer>, Set<String>> deletableRegions, int days) {
        File playerData = resolvePlayerDataFolder(world);
        plugin.getIslands().getIslandById(islandID)
                .ifPresent(island -> island.getMemberSet()
                        .forEach(uuid -> maybeDeletePlayerData(world, uuid, playerData, deletableRegions, days)));
    }

    private void maybeDeletePlayerData(World world, UUID uuid, File playerData,
            Map<Pair<Integer, Integer>, Set<String>> deletableRegions, int days) {
        // Deleted sweep (days == 0) skips player-data cleanup entirely —
        // the player might still be active, and the age-based sweep will
        // reap orphaned .dat files later.
        if (days <= 0) {
            return;
        }
        List<Island> memberOf = new ArrayList<>(plugin.getIslands().getIslands(world, uuid));
        deletableRegions.values().forEach(ids -> memberOf.removeIf(i -> ids.contains(i.getUniqueId())));
        if (!memberOf.isEmpty()) {
            return;
        }
        if (Bukkit.getOfflinePlayer(uuid).isOp()) {
            return;
        }
        long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        Long lastLogin = plugin.getPlayers().getLastLoginTimestamp(uuid);
        long actualLast = lastLogin != null ? lastLogin : Bukkit.getOfflinePlayer(uuid).getLastSeen();
        if (actualLast >= cutoffMillis) {
            return;
        }
        deletePlayerFiles(uuid, playerData);
    }

    private void deletePlayerFiles(UUID uuid, File playerData) {
        if (!playerData.exists()) {
            return;
        }
        deletePlayerFile(new File(playerData, uuid + ".dat"), "player data file");
        deletePlayerFile(new File(playerData, uuid + ".dat_old"), "player data backup file");
    }

    private void deletePlayerFile(File file, String description) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ex) {
            plugin.logError("Failed to delete " + description + ": " + file.getAbsolutePath());
        }
    }

    // ---------------------------------------------------------------
    // Dimension path resolution (pre-26.1 vs 26.1.1+)
    // ---------------------------------------------------------------

    private File resolveDataFolder(World world) {
        File worldFolder = world.getWorldFolder();
        return switch (world.getEnvironment()) {
            case NETHER -> {
                File dim = new File(worldFolder, DIM_1);
                yield dim.isDirectory() ? dim : worldFolder;
            }
            case THE_END -> {
                File dim = new File(worldFolder, DIM1);
                yield dim.isDirectory() ? dim : worldFolder;
            }
            default -> worldFolder;
        };
    }

    private File resolvePlayerDataFolder(World world) {
        File worldFolder = world.getWorldFolder();
        File oldPath = new File(worldFolder, PLAYERDATA);
        if (oldPath.isDirectory()) {
            return oldPath;
        }
        File root = worldFolder.getParentFile();
        if (root != null) root = root.getParentFile();
        if (root != null) root = root.getParentFile();
        if (root != null) {
            File newPath = new File(root, PLAYERS + File.separator + "data");
            if (newPath.isDirectory()) {
                return newPath;
            }
        }
        return oldPath;
    }

    private File resolveNetherFallback(File overworldFolder) {
        File dim = new File(overworldFolder, DIM_1);
        if (dim.isDirectory()) {
            return dim;
        }
        File parent = overworldFolder.getParentFile();
        if (parent != null) {
            File sibling = new File(parent, overworldFolder.getName() + "_nether");
            if (sibling.isDirectory()) {
                return sibling;
            }
        }
        return dim;
    }

    private File resolveEndFallback(File overworldFolder) {
        File dim = new File(overworldFolder, DIM1);
        if (dim.isDirectory()) {
            return dim;
        }
        File parent = overworldFolder.getParentFile();
        if (parent != null) {
            File sibling = new File(parent, overworldFolder.getName() + "_the_end");
            if (sibling.isDirectory()) {
                return sibling;
            }
        }
        return dim;
    }

    // ---------------------------------------------------------------
    // Freshness checks + region timestamp reader
    // ---------------------------------------------------------------

    private boolean isFileFresh(File file, long cutoffMillis) {
        return file.exists() && getRegionTimestamp(file) >= cutoffMillis;
    }

    private boolean isAnyDimensionFresh(String name, File overworldRegion, File netherRegion,
            File endRegion, long cutoffMillis, boolean isNether, boolean isEnd) {
        if (isFileFresh(new File(overworldRegion, name), cutoffMillis)) return true;
        if (isNether && isFileFresh(new File(netherRegion, name), cutoffMillis)) return true;
        return isEnd && isFileFresh(new File(endRegion, name), cutoffMillis);
    }

    /**
     * Reads the most recent per-chunk timestamp in a Minecraft .mca file
     * header, in milliseconds since epoch.
     */
    private long getRegionTimestamp(File regionFile) {
        if (!regionFile.exists() || regionFile.length() < 8192) {
            return 0L;
        }
        try (FileInputStream fis = new FileInputStream(regionFile)) {
            byte[] buffer = new byte[4096];
            if (fis.skip(4096) != 4096) {
                return 0L;
            }
            if (fis.read(buffer) != 4096) {
                return 0L;
            }
            ByteBuffer bb = ByteBuffer.wrap(buffer);
            bb.order(ByteOrder.BIG_ENDIAN);
            long maxTimestampSeconds = 0;
            for (int i = 0; i < 1024; i++) {
                long timestamp = Integer.toUnsignedLong(bb.getInt());
                if (timestamp > maxTimestampSeconds) {
                    maxTimestampSeconds = timestamp;
                }
            }
            return maxTimestampSeconds * 1000L;
        } catch (IOException e) {
            plugin.logError("Failed to read region file timestamps: " + regionFile.getAbsolutePath()
                    + " " + e.getMessage());
            return 0L;
        }
    }

    private long strictRegionTimestamp(File file) throws IOException {
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            input.seek(4096);
            long latest = 0;
            for (int i = 0; i < 1024; i++) latest = Math.max(latest, Integer.toUnsignedLong(input.readInt()));
            return latest * 1000L;
        }
    }
}
