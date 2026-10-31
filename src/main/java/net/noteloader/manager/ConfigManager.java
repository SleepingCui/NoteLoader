package net.noteloader.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.*;

public class ConfigManager {
    private static Config config;

    public enum LoadMode {
        @SerializedName("forced") FORCED,
        @SerializedName("ticket") TICKET
    }

    public static class Config {
        private int radius = 1;
        private long durationTicks = 300;
        private long cooldownTicks = 20;
        private LoadMode loadMode = LoadMode.FORCED;
        private boolean tickEntities = true;

        public int radius() { return radius; }
        public long durationTicks() { return durationTicks; }
        public long cooldownTicks() { return cooldownTicks; }
        public LoadMode loadMode() { return loadMode; }
        public boolean tickEntities() { return tickEntities; }
    }

    public static Config get() {
        if (config == null) load();
        return config;
    }
    public static void load() {
        File file = new File("config/noteloader.json");

        if (!file.exists()) {
            config = new Config();
            save();
            return;
        }
        boolean parsed = false;
        try (Reader reader = new FileReader(file)) {
            Gson gson = new Gson();
            Config loaded = gson.fromJson(reader, Config.class);
            config = loaded != null ? loaded : new Config();
            parsed = true;
        } catch (Exception e) {
            config = new Config();
        }
        normalize();
        if (parsed) save();
    }

    private static void normalize() {
        config.radius = Math.max(0, config.radius);
        config.durationTicks = Math.max(1, config.durationTicks);
        config.cooldownTicks = Math.max(0, config.cooldownTicks);
        if (config.loadMode == null) config.loadMode = LoadMode.FORCED;
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
        config.radius = radius;
        save();
    }
    public static void setDuration(long ticks) {
        config.durationTicks = ticks;
        save();
    }
    public static void setLoadMode(LoadMode mode) {
        config.loadMode = mode;
        save();
    }
    public static void setTickEntities(boolean tickEntities) {
        config.tickEntities = tickEntities;
        save();
    }
}
