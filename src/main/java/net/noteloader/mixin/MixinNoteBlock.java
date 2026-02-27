package net.noteloader.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.noteloader.manager.ChunkLoadManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoteBlock.class)
public class MixinNoteBlock {
    @Inject(method = "playNote", at = @At("HEAD"))
    private void noteloader$onPlayNote(@Nullable Entity entity, BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        if (world instanceof ServerWorld serverWorld) {
            ChunkLoadManager.trigger(serverWorld, pos);
        }
    }
}