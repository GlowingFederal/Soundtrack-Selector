package com.glowingfederal.soundtrackselector.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.TreeMap;

/** Absent tracks remain in this file so removed packs/mods retain their choices. */
public final class Preferences {
    private final File file;
    private final Map<String, Map<String, Boolean>> playlists = new TreeMap<String, Map<String, Boolean>>();
    private String active = "Default";
    private Map<String, Boolean> choices = new TreeMap<String, Boolean>();
    private boolean dirty;
    private boolean loop = true, randomOrder = true;

    public Preferences(File file) throws IOException {
        this.file = file;
        playlists.put(active, choices);
        if (file == null || !file.isFile()) return;
        Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
        try {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            int format = root.get("format").getAsInt();
            loop = readMode(root, "loop");
            randomOrder = readMode(root, "randomOrder");
            if (format == 1) {
                readChoices(root.getAsJsonObject("tracks"), choices);
                dirty = true;
            } else if (format == 2) {
                playlists.clear();
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("playlists").entrySet()) {
                    if (!validName(entry.getKey())) throw new IOException("Invalid playlist name");
                    Map<String, Boolean> values = new TreeMap<String, Boolean>();
                    readChoices(entry.getValue().getAsJsonObject(), values);
                    playlists.put(entry.getKey(), values);
                }
                active = root.get("activePlaylist").getAsString();
                if (!playlists.containsKey(active) || !playlists.containsKey("Default")) throw new IOException("Missing active/default playlist");
                choices = playlists.get(active);
            } else throw new IOException("Unsupported soundtrack preferences format");
        } catch (RuntimeException failure) {
            throw new IOException("Invalid soundtrack preferences", failure);
        } finally { reader.close(); }
    }

    private static void readChoices(JsonObject object, Map<String, Boolean> values) throws IOException {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isBoolean())
                throw new IOException("Invalid enabled setting for " + entry.getKey());
            values.put(entry.getKey(), entry.getValue().getAsBoolean());
        }
    }
    private static boolean readMode(JsonObject root, String key) throws IOException {
        JsonElement value = root.get(key);
        if (value == null) return true;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IOException("Invalid " + key + " setting");
        return value.getAsBoolean();
    }
    public boolean loop() { return loop; }
    public boolean randomOrder() { return randomOrder; }
    public void toggleLoop() { loop = !loop; dirty = true; }
    public void toggleRandomOrder() { randomOrder = !randomOrder; dirty = true; }
    private static boolean validName(String name) {
        if (name.length() == 0 || name.length() > 48 || !name.equals(name.trim())) return false;
        for (int i = 0; i < name.length(); i++) if (Character.isISOControl(name.charAt(i))) return false;
        return true;
    }
    public String activePlaylist() { return active; }
    public int playlistCount() { return playlists.size(); }
    public void nextPlaylist() {
        java.util.List<String> names = new java.util.ArrayList<String>(playlists.keySet());
        active = names.get((names.indexOf(active) + 1) % names.size());
        choices = playlists.get(active); dirty = true;
    }
    public boolean createPlaylist(String name) {
        name = name.trim();
        if (!validName(name) || playlists.containsKey(name)) return false;
        choices = new TreeMap<String, Boolean>(choices);
        playlists.put(name, choices); active = name; dirty = true;
        return true;
    }
    public void deletePlaylist() {
        if (active.equals("Default")) return;
        playlists.remove(active); active = "Default"; choices = playlists.get(active); dirty = true;
    }

    public boolean enabled(Track track) {
        Boolean choice = choices.get(track.id);
        return choice == null ? track.music : choice.booleanValue();
    }

    public void set(String id, boolean enabled) {
        Boolean previous = choices.put(id, enabled);
        if (previous == null || previous.booleanValue() != enabled) dirty = true;
    }

    public void save() throws IOException {
        if (!dirty || file == null) return;
        File directory = file.getAbsoluteFile().getParentFile();
        Files.createDirectories(directory.toPath());
        Path temporary = Files.createTempFile(directory.toPath(), "soundtrackselector-", ".tmp");
        try {
            JsonObject root = new JsonObject();
            root.addProperty("format", 2);
            root.addProperty("activePlaylist", active);
            root.addProperty("loop", loop);
            root.addProperty("randomOrder", randomOrder);
            JsonObject stored = new JsonObject();
            for (Map.Entry<String, Map<String, Boolean>> playlist : playlists.entrySet()) {
                JsonObject tracks = new JsonObject();
                for (Map.Entry<String, Boolean> entry : playlist.getValue().entrySet()) tracks.addProperty(entry.getKey(), entry.getValue());
                stored.add(playlist.getKey(), tracks);
            }
            root.add("playlists", stored);
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            Writer writer = new OutputStreamWriter(Files.newOutputStream(temporary), "UTF-8");
            try { gson.toJson(root, writer); writer.write('\n'); } finally { writer.close(); }
            try { Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            dirty = false;
        } finally { Files.deleteIfExists(temporary); }
    }
}
