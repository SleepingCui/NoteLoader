package net.noteloader.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

public class ConfigManager {
    private static Config config;
    public record Config(int radius, long durationTicks, long cooldownTicks) {}

    public static Config get() {
        if (config == null) load();
        return config;
    }
    public static void load() {
        File file = new File("config/noteloader.json");

        if (!file.exists()) {
            config = new Config(1, 300, 20);
            save();
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Gson gson = new Gson();
            config = gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            config = new Config(1, 300, 20);
        }
    }
    public static void save() {
        try {
            File file = new File("config/noteloader.json");
            file.getParentFile().mkdirs();

            try (Writer writer = new FileWriter(file)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(config, writer);
            }
        } catch (Exception ignored) {}
    }
    public static void setRadius(int radius) {
        config = new Config(radius, config.durationTicks, config.cooldownTicks);
        save();
    }
    public static void setDuration(long ticks) {
        config = new Config(config.radius, ticks, config.cooldownTicks);
        save();
    }
}