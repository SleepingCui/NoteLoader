package net.noteloader.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.noteloader.manager.ChunkLoadManager;
import net.noteloader.manager.ConfigManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class NoteLoaderCommand {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public static void register() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("noteloader")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("info")
                        .executes(ctx -> {
                            Set<ChunkPos> activeChunks = ChunkLoadManager.getActiveChunks();
                            long durationTicks = ConfigManager.get().durationTicks();

                            if (activeChunks.isEmpty()) {
                                ctx.getSource().sendFeedback(
                                        () -> Text.literal("No forced chunks currently active."), false);
                                return 1;
                            }

                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("Active forced chunks: " + activeChunks.size()), false);

                            for (ChunkPos chunk : activeChunks) {
                                long expireTick = ChunkLoadManager.getExpireTime(chunk);
                                var ref = new Object() {
                                    long remainingTicks = expireTick - ChunkLoadManager.getServerTime();
                                };
                                if (ref.remainingTicks < 0) ref.remainingTicks = 0;
                                String expireTimeStr = TIME_FORMATTER.format(Instant.now().plusMillis(ref.remainingTicks * 50L));

                                ctx.getSource().sendFeedback(
                                        () -> Text.literal(String.format(
                                                "Chunk (%d, %d) | Remaining: %d ticks (~%d s) | Expires at: %s",
                                                chunk.x, chunk.z,
                                                ref.remainingTicks,
                                                ref.remainingTicks / 20,
                                                expireTimeStr
                                        )),
                                        false
                                );
                            }
                            return 1;
                        })
                )
                .then(CommandManager.literal("reload")
                        .executes(ctx -> {
                            ConfigManager.load();
                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("Config reloaded. Duration: " + ConfigManager.get().durationTicks() + " ticks, Radius: " + ConfigManager.get().radius()), false);
                            return 1;
                        })
                )
                .then(CommandManager.literal("set")
                        .then(CommandManager.literal("radius")
                                .then(CommandManager.argument("value", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            ConfigManager.setRadius(value);
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("Radius set to " + value), false);
                                            return 1;
                                        })
                                )
                        )
                        .then(CommandManager.literal("duration")
                                .then(CommandManager.argument("ticks", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                            ConfigManager.setDuration(ticks);
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("Duration set to " + ticks + " ticks"), false);
                                            return 1;
                                        })
                                )
                        )
                )
        ));
    }
}