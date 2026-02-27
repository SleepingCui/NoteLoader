package net.noteloader.manager;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Log4j2
public class ChunkLoadManager {
    private static final Map<ChunkPos, Long> activeChunks = new HashMap<>();
    private static final Map<BlockPos, Long> cooldownMap = new HashMap<>();
    private static MinecraftServer server;

    public static void init(MinecraftServer mcServer) {
        server = mcServer;
        log.info("ChunkLoadManager initialized.");
    }
    public static void cleanupAll() {
        if (server == null) {
            log.warn("Cleanup skipped: server is null.");
            return;
        }
        log.info("Cleaning up {} forced chunks before shutdown...", activeChunks.size());
        for (ChunkPos pos : activeChunks.keySet()) {
            for (ServerWorld world : server.getWorlds()) {
                world.setChunkForced(pos.x, pos.z, false);
            }
        }
        activeChunks.clear();
        cooldownMap.clear();
        log.info("All forced chunks successfully released.");
    }
    public static void trigger(ServerWorld world, BlockPos pos) {
        long now = world.getTime();
        long cooldown = ConfigManager.get().cooldownTicks();

        if (cooldownMap.containsKey(pos) && now - cooldownMap.get(pos) < cooldown) {
            log.debug("Trigger skipped due to cooldown at {}", pos);
            return;
        }
        cooldownMap.put(pos, now);

        int radius = ConfigManager.get().radius();
        long expireTime = now + ConfigManager.get().durationTicks();
        ChunkPos center = new ChunkPos(pos);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkPos target = new ChunkPos(center.x + x, center.z + z);
                if (!activeChunks.containsKey(target)) {
                    world.setChunkForced(target.x, target.z, true);
                    log.debug("Forced chunk {} loaded.", target);
                } else {
                    log.debug("Refreshed chunk {} expiration.", target);
                }

                activeChunks.put(target, expireTime);
            }
        }
    }
    public static void tick(MinecraftServer server) {
        long now = server.getOverworld().getTime();
        Iterator<Map.Entry<ChunkPos, Long>> iterator = activeChunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ChunkPos, Long> entry = iterator.next();
            if (now >= entry.getValue()) {
                for (ServerWorld world : server.getWorlds()) {
                    world.setChunkForced(entry.getKey().x, entry.getKey().z, false);
                }
                log.debug("Chunk {} expired and unloaded.", entry.getKey());
                iterator.remove();
            }
        }
        cooldownMap.entrySet().removeIf(e -> now - e.getValue() > ConfigManager.get().cooldownTicks() * 2
        );
    }
    public static Set<ChunkPos> getActiveChunks() {
        return activeChunks.keySet();
    }
    public static long getExpireTime(ChunkPos pos) {
        return activeChunks.getOrDefault(pos, 0L);
    }
    public static long getServerTime() {
        if (server == null) return 0;
        return server.getOverworld().getTime();
    }
}