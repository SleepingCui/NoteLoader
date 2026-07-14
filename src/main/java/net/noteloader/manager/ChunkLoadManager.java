package net.noteloader.manager;

import lombok.extern.log4j.Log4j2;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Log4j2
public class ChunkLoadManager {
    public record DimensionChunkPos(RegistryKey<World> dimension, ChunkPos pos) {}

    private static final Map<DimensionChunkPos, Long> activeChunks = new HashMap<>();
    private static final Map<BlockPos, Long> cooldownMap = new HashMap<>();
    private static MinecraftServer server;

    public static void init(MinecraftServer mcServer) {
        server = mcServer;
        log.info("ChunkLoadManager initialized successfully");
    }

    public static int cleanupAll() {
        if (server == null) return 0;

        int count = activeChunks.size();
        for (DimensionChunkPos dimPos : activeChunks.keySet()) {
            ServerWorld world = server.getWorld(dimPos.dimension());
            if (world != null) {
                world.setChunkForced(dimPos.pos().x, dimPos.pos().z, false);
            }
        }
        activeChunks.clear();
        cooldownMap.clear();

        log.info("Released {} forced chunks", count);
        return count;
    }

    public static void trigger(ServerWorld world, BlockPos pos) {
        long now = world.getTime();
        long cooldown = ConfigManager.get().cooldownTicks();

        if (cooldownMap.containsKey(pos) && now - cooldownMap.get(pos) < cooldown) {
            return;
        }
        cooldownMap.put(pos, now);

        int radius = ConfigManager.get().radius();
        long expireTime = now + ConfigManager.get().durationTicks();
        ChunkPos center = new ChunkPos(pos);
        RegistryKey<World> dimension = world.getRegistryKey();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkPos targetChunk = new ChunkPos(center.x + x, center.z + z);
                DimensionChunkPos target = new DimensionChunkPos(dimension, targetChunk);

                if (!activeChunks.containsKey(target)) {
                    world.setChunkForced(targetChunk.x, targetChunk.z, true);
                }
                activeChunks.put(target, expireTime);
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (ChunkLoadManager.server == null) ChunkLoadManager.server = server;
        long now = server.getOverworld().getTime();
        Iterator<Map.Entry<DimensionChunkPos, Long>> iterator = activeChunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DimensionChunkPos, Long> entry = iterator.next();
            if (now >= entry.getValue()) {
                DimensionChunkPos dimPos = entry.getKey();
                ServerWorld world = server.getWorld(dimPos.dimension());
                if (world != null) {
                    world.setChunkForced(dimPos.pos().x, dimPos.pos().z, false);
                }
                iterator.remove();
            }
        }
        cooldownMap.entrySet().removeIf(e -> now - e.getValue() > ConfigManager.get().cooldownTicks() * 2);
    }

    public static Set<DimensionChunkPos> getActiveChunks() { return activeChunks.keySet(); }
    public static long getExpireTime(DimensionChunkPos pos) { return activeChunks.getOrDefault(pos, 0L); }

    public static long getServerTime() {
        if (server == null) return 0;
        return server.getOverworld().getTime();
    }
}