package com.example.real;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("real.json");

    public static Config INSTANCE = load();

    public boolean enabled = true;
    public boolean debug = true; // подсказки над хотбаром
    public int delay = 1; // тиков между перлом и зарядом ветра (0 = в конце того же тика)

    public void fix() {
        delay = Math.max(0, Math.min(20, delay));
    }

    public static Config load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) { c.fix(); return c; }
            }
        } catch (Exception ignored) {}
        return new Config();
    }

    public void save() {
        try {
            fix();
            Files.writeString(FILE, GSON.toJson(this));
        } catch (Exception ignored) {}
    }
}
