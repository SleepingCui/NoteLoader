package net.noteloader;

import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.noteloader.command.NoteLoaderCommand;
import net.noteloader.manager.ChunkLoadManager;
import net.noteloader.manager.ConfigManager;

@Log4j2
public class Noteloader implements ModInitializer {

    @Override
    public void onInitialize() {
        ConfigManager.load();
        ServerTickEvents.END_SERVER_TICK.register(ChunkLoadManager::tick);
        NoteLoaderCommand.register();
        log.info("Noteloader {} loaded", FabricLoader.getInstance()
                .getModContainer("noteloader")
                .map(ModContainer::getMetadata)
                .map(meta -> meta.getVersion().getFriendlyString())
                .orElse("unknown"));
    }
}