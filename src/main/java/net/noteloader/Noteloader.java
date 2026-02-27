package net.noteloader;

import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import net.noteloader.manager.ChunkLoadManager;
import net.noteloader.manager.ConfigManager;
import net.noteloader.command.NoteLoaderCommand;

@Log4j2
public class Noteloader implements ModInitializer {

    @Override
    public void onInitialize() {
        ConfigManager.load();
        ServerLifecycleEvents.SERVER_STARTED.register(ChunkLoadManager::init);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ChunkLoadManager.cleanupAll());
        ServerTickEvents.END_SERVER_TICK.register(ChunkLoadManager::tick);
        NoteLoaderCommand.register();
        log.info("NoteLoader {} loaded", FabricLoader.getInstance().getModContainer("noteloader").map(ModContainer::getMetadata).map(meta -> meta.getVersion().getFriendlyString()).orElse("unknown"));
    }
}