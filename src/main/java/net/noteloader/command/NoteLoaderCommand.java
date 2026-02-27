package net.noteloader.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.noteloader.manager.ChunkLoadManager;
import net.noteloader.manager.ConfigManager;
import java.util.Set;

public class NoteLoaderCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("noteloader")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("info").executes(ctx -> executeInfo(ctx.getSource())))
                        .then(CommandManager.literal("reload").executes(ctx -> executeReload(ctx.getSource())))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.literal("radius")
                                        .then(CommandManager.argument("value", IntegerArgumentType.integer(0))
                                                .executes(ctx -> executeSetRadius(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "value")))))
                                .then(CommandManager.literal("duration")
                                        .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeSetDuration(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "ticks"))))))
                )
        );
    }
    private static int executeInfo(ServerCommandSource source) {
        Set<ChunkPos> activeChunks = ChunkLoadManager.getActiveChunks();
        if (activeChunks.isEmpty()) {
            source.sendFeedback(() -> Text.literal("[NoteLoader] No forced chunks currently active."), false);
            return 1;
        }
        source.sendFeedback(() -> Text.literal("[NoteLoader] Active Forced Chunks: " + activeChunks.size()), false);
        long currentTime = ChunkLoadManager.getServerTime();
        for (ChunkPos chunk : activeChunks) {
            long remaining = Math.max(0, ChunkLoadManager.getExpireTime(chunk) - currentTime);
            String info = String.format("[NoteLoader] Chunk [%d,%d] | Remaining %dticks", chunk.x, chunk.z, remaining);
            source.sendFeedback(() -> Text.literal(info), false);
        }
        return 1;
    }
    private static int executeReload(ServerCommandSource source) {
        ConfigManager.load();
        String msg = String.format("[NoteLoader] Config reloaded. Radius: %d, Duration: %d ticks", ConfigManager.get().radius(), ConfigManager.get().durationTicks());
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
}