package net.noteloader.manager;

import lombok.extern.log4j.Log4j2;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ChunkLevels;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Log4j2
public class ChunkLoadManager {
    public record DimensionChunkPos(RegistryKey<World> dimension, ChunkPos pos) {}
    private record ActiveChunk(long expireTime, ConfigManager.LoadMode mode, boolean tickEntities, boolean owned) {
        private ActiveChunk withExpireTime(long newExpireTime) {
            return new ActiveChunk(newExpireTime, mode, tickEntities, owned);
        }
    }

    private static final ChunkTicketType<ChunkPos> NOTE_TICKET = ChunkTicketType.create("noteloader", Comparator.comparingLong(ChunkPos::toLong));
    private static final Map<DimensionChunkPos, ActiveChunk> activeChunks = new HashMap<>();
    private static final Map<BlockPos, Long> cooldownMap = new HashMap<>();
    private static MinecraftServer server;

    public static void init(MinecraftServer mcServer) {
        server = mcServer;
        log.info("ChunkLoadManager initialized successfully");
    }

    public static int cleanupAll() {
        if (server == null) return 0;

        int released = 0;
        for (Map.Entry<DimensionChunkPos, ActiveChunk> entry : activeChunks.entrySet()) {
            ServerWorld world = server.getWorld(entry.getKey().dimension());
            if (world != null) {
                unload(world, entry.getKey().pos(), entry.getValue());
                if (entry.getValue().owned()) released++;
            }
        }
        activeChunks.clear();
        cooldownMap.clear();

        log.info("Released {} forced chunks", released);
        return released;
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
        ConfigManager.LoadMode mode = ConfigManager.get().loadMode();
        boolean tickEntities = effectiveTickEntities(mode, ConfigManager.get().tickEntities());
        ChunkPos center = new ChunkPos(pos);
        RegistryKey<World> dimension = world.getRegistryKey();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkPos targetChunk = new ChunkPos(center.x + x, center.z + z);
                DimensionChunkPos target = new DimensionChunkPos(dimension, targetChunk);

                ActiveChunk existing = activeChunks.get(target);
                if (existing != null && existing.mode() == mode && existing.tickEntities() == tickEntities) {
                    activeChunks.put(target, existing.withExpireTime(expireTime));
                    continue;
                }
                if (existing != null) {
                    unload(world, targetChunk, existing);
                }
                boolean owned = load(world, targetChunk, mode, tickEntities);
                activeChunks.put(target, new ActiveChunk(expireTime, mode, tickEntities, owned));
            }
        }
    }

    public static int reapplyActiveChunks() {
        if (server == null) return 0;

        ConfigManager.LoadMode mode = ConfigManager.get().loadMode();
        boolean tickEntities = effectiveTickEntities(mode, ConfigManager.get().tickEntities());
        int changed = 0;

        for (Map.Entry<DimensionChunkPos, ActiveChunk> entry : activeChunks.entrySet()) {
            ActiveChunk data = entry.getValue();
            if (data.mode() == mode && data.tickEntities() == tickEntities) continue;

            ServerWorld world = server.getWorld(entry.getKey().dimension());
            if (world == null) continue;

            ChunkPos pos = entry.getKey().pos();
            unload(world, pos, data);
            boolean owned = load(world, pos, mode, tickEntities);
            entry.setValue(new ActiveChunk(data.expireTime(), mode, tickEntities, owned));
            changed++;
        }
        return changed;
    }

    private static boolean load(ServerWorld world, ChunkPos pos, ConfigManager.LoadMode mode, boolean tickEntities) {
        if (mode == ConfigManager.LoadMode.FORCED) {
            return world.setChunkForced(pos.x, pos.z, true);
        }
        world.getChunkManager().addTicket(NOTE_TICKET, pos, ticketRadius(tickEntities), pos);
        return true;
    }

    private static void unload(ServerWorld world, ChunkPos pos, ActiveChunk data) {
        if (!data.owned()) return;

        if (data.mode() == ConfigManager.LoadMode.FORCED) {
            world.setChunkForced(pos.x, pos.z, false);
        } else {
            world.getChunkManager().removeTicket(NOTE_TICKET, pos, ticketRadius(data.tickEntities()), pos);
        }
    }

    private static boolean effectiveTickEntities(ConfigManager.LoadMode mode, boolean configured) {
        return mode == ConfigManager.LoadMode.FORCED || configured;
    }

    private static int ticketRadius(boolean tickEntities) {
        ChunkLevelType type = tickEntities ? ChunkLevelType.ENTITY_TICKING : ChunkLevelType.BLOCK_TICKING;
        return ChunkLevels.getLevelFromType(ChunkLevelType.FULL) - ChunkLevels.getLevelFromType(type);
    }

    public static void tick(MinecraftServer server) {
        if (ChunkLoadManager.server == null) ChunkLoadManager.server = server;
        long now = server.getOverworld().getTime();
        Iterator<Map.Entry<DimensionChunkPos, ActiveChunk>> iterator = activeChunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DimensionChunkPos, ActiveChunk> entry = iterator.next();
            if (now >= entry.getValue().expireTime()) {
                DimensionChunkPos dimPos = entry.getKey();
                ServerWorld world = server.getWorld(dimPos.dimension());
                if (world != null) {
                    unload(world, dimPos.pos(), entry.getValue());
                }
                iterator.remove();
            }
        }
        cooldownMap.entrySet().removeIf(e -> now - e.getValue() > ConfigManager.get().cooldownTicks() * 2);
    }

    public static Set<DimensionChunkPos> getActiveChunks() { return activeChunks.keySet(); }

    public static long getExpireTime(DimensionChunkPos pos) {
        ActiveChunk data = activeChunks.get(pos);
        return data != null ? data.expireTime() : 0L;
    }

    public static ConfigManager.LoadMode getLoadMode(DimensionChunkPos pos) {
        ActiveChunk data = activeChunks.get(pos);
        return data != null ? data.mode() : null;
    }

    public static long getServerTime() {
        if (server == null) return 0;
        return server.getOverworld().getTime();
    }
}
