package net.noteloader.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.noteloader.manager.ChunkLoadManager;
import net.noteloader.manager.ConfigManager;

import java.util.Locale;

public class NoteLoaderCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("noteloader")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("info").executes(ctx -> executeInfo(ctx.getSource())))
                        .then(CommandManager.literal("reload").executes(ctx -> executeReload(ctx.getSource())))
                        .then(CommandManager.literal("cleanall").executes(ctx -> executeCleanAll(ctx.getSource())))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.literal("radius")
                                        .then(CommandManager.argument("value", IntegerArgumentType.integer(0))
                                                .executes(ctx -> executeSetRadius(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "value")))))
                                .then(CommandManager.literal("duration")
                                        .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeSetDuration(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "ticks")))))
                                .then(CommandManager.literal("mode")
                                        .then(CommandManager.literal("forced")
                                                .executes(ctx -> executeSetMode(ctx.getSource(), ConfigManager.LoadMode.FORCED)))
                                        .then(CommandManager.literal("ticket")
                                                .executes(ctx -> executeSetMode(ctx.getSource(), ConfigManager.LoadMode.TICKET))))
                                .then(CommandManager.literal("entityticking")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> executeSetEntityTicking(ctx.getSource(), BoolArgumentType.getBool(ctx, "value"))))))
                )
        );
    }
    private static int executeInfo(ServerCommandSource source) {
        java.util.Set<ChunkLoadManager.DimensionChunkPos> activeChunks = ChunkLoadManager.getActiveChunks();
        ConfigManager.Config config = ConfigManager.get();
        boolean entityTicking = config.loadMode() == ConfigManager.LoadMode.FORCED || config.tickEntities();

        if (activeChunks.isEmpty()) {
            source.sendFeedback(() -> Text.literal(String.format(
                    "[NoteLoader] No forced chunks currently active. Mode: %s | Entity ticking: %s",
                    formatMode(config.loadMode()), entityTicking)), false);
            return 1;
        }
        source.sendFeedback(() -> Text.literal(String.format(
                "[NoteLoader] Active Forced Chunks: %d | Mode: %s | Entity ticking: %s",
                activeChunks.size(), formatMode(config.loadMode()), entityTicking)), false);
        long currentTime = ChunkLoadManager.getServerTime();

        for (ChunkLoadManager.DimensionChunkPos dimChunk : activeChunks) {
            long remaining = Math.max(0, ChunkLoadManager.getExpireTime(dimChunk) - currentTime);
            String info = String.format("[NoteLoader] Chunk [%d,%d] in %s | Remaining %d tick(s) | %s",
                    dimChunk.pos().x, dimChunk.pos().z, dimChunk.dimension().getValue().getPath(), remaining,
                    formatMode(ChunkLoadManager.getLoadMode(dimChunk)));
            source.sendFeedback(() -> Text.literal(info), false);
        }
        return 1;
    }
    private static int executeReload(ServerCommandSource source) {
        ConfigManager.load();
        int changed = ChunkLoadManager.reapplyActiveChunks();
        ConfigManager.Config config = ConfigManager.get();
        String msg = String.format("[NoteLoader] Config reloaded. Radius: %d, Duration: %d tick(s), Mode: %s, Entity ticking: %s%s",
                config.radius(), config.durationTicks(), formatMode(config.loadMode()),
                config.loadMode() == ConfigManager.LoadMode.FORCED || config.tickEntities(),
                changed > 0 ? " (reapplied to " + changed + " active chunk(s))" : "");
        source.sendFeedback(() -> Text.literal(msg), false);
        return 1;
    }
    private static int executeSetRadius(ServerCommandSource source, int value) {
        ConfigManager.setRadius(value);
        source.sendFeedback(() -> Text.literal("[NoteLoader] Radius set to " + value), false);
        return 1;
    }
    private static int executeSetDuration(ServerCommandSource source, int ticks) {
        ConfigManager.setDuration(ticks);
        source.sendFeedback(() -> Text.literal("[NoteLoader] Duration set to " + ticks + " ticks"), false);
        return 1;
    }
    private static int executeSetMode(ServerCommandSource source, ConfigManager.LoadMode mode) {
        ConfigManager.setLoadMode(mode);
        int changed = ChunkLoadManager.reapplyActiveChunks();
        String msg = String.format("[NoteLoader] Load mode set to %s%s", formatMode(mode),
                changed > 0 ? " (reapplied to " + changed + " active chunk(s))" : "");
        source.sendFeedback(() -> Text.literal(msg), true);
        return 1;
    }
    private static int executeSetEntityTicking(ServerCommandSource source, boolean value) {
        ConfigManager.setTickEntities(value);
        int changed = ChunkLoadManager.reapplyActiveChunks();
        String msg = String.format("[NoteLoader] Entity ticking set to %s%s", value,
                changed > 0 ? " (reapplied to " + changed + " active chunk(s))" : "");
        if (ConfigManager.get().loadMode() == ConfigManager.LoadMode.FORCED) {
            msg += " (ignored in forced mode; forced chunks always tick entities; use /noteloader set mode ticket)";
        }
        String finalMsg = msg;
        source.sendFeedback(() -> Text.literal(finalMsg), true);
        return 1;
    }
    private static int executeCleanAll(ServerCommandSource source) {
        String msg = String.format("[NoteLoader] Released %d forced chunks", ChunkLoadManager.cleanupAll());
        source.sendFeedback(() -> Text.literal(msg), true);
        return 1;
    }
    private static String formatMode(ConfigManager.LoadMode mode) {
        return mode == null ? "unknown" : mode.name().toLowerCase(Locale.ROOT);
    }
}
