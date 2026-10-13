package com.glowingfederal.soundtrackselector.domain;

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
    private boolean vanilla = true;
    private final Map<String, String> aliases = new TreeMap<String, String>();
    private JsonObject builtin;

    public Preferences(File file) throws IOException {
        this.file = file;
        playlists.put(active, choices);
        dirty = true;
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
            } else if (format == 2 || format == 3) {
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
                if (format == 3 && root.has("aliases")) {
                    for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("aliases").entrySet()) {
                        if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString())
                            throw new IOException("Invalid nickname for " + entry.getKey());
                        aliases.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
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
    public boolean vanilla() { return vanilla; }
    public void returnToVanilla() { vanilla = true; dirty = true; }
    public String activePlaylist() { return vanilla ? "Vanilla" : active.equals("Vanilla") ? "Vanilla (user)" : active; }
    public int playlistCount() { return playlists.size() + 1; }
    public void nextPlaylist() {
        java.util.List<String> names = new java.util.ArrayList<String>(playlists.keySet());
        if (vanilla) { active = names.get(0); vanilla = false; }
        else if (names.indexOf(active) == names.size() - 1) { returnToVanilla(); return; }
        else active = names.get(names.indexOf(active) + 1);
        choices = playlists.get(active); dirty = true;
    }
    public boolean createPlaylist(String name) {
        name = name.trim();
        if (!validName(name) || playlists.containsKey(name)) return false;
        choices = vanilla ? new TreeMap<String, Boolean>() : new TreeMap<String, Boolean>(choices);
        playlists.put(name, choices); active = name; vanilla = false; dirty = true;
        return true;
    }
    public void deletePlaylist() {
        if (vanilla || active.equals("Default")) return;
        playlists.remove(active); active = "Default"; choices = playlists.get(active); dirty = true;
    }

    public boolean enabled(Track track) {
        Boolean choice = choices.get(track.id);
        return !vanilla && choice != null && choice.booleanValue();
    }

    public void set(String id, boolean enabled) {
        if (vanilla) return;
        Boolean previous = choices.put(id, enabled);
        if (previous == null || previous.booleanValue() != enabled) dirty = true;
    }

    public String nickname(Track track) {
        String value = aliases.get(track.aliasId());
        return value == null ? "" : value;
    }
    public String displayName(Track track) {
        String value = nickname(track);
        return value.length() == 0 ? track.name : value;
    }
    public boolean rename(Track track, String value) {
        value = value.trim();
        if (value.length() > 128) return false;
        for (int i = 0; i < value.length(); i++) if (Character.isISOControl(value.charAt(i))) return false;
        if (value.length() == 0) aliases.remove(track.aliasId()); else aliases.put(track.aliasId(), value);
        dirty = true;
        return true;
    }

    /** Reconstruct from native constants, never from the discovered resource catalog or saved data. */
    public void maintainVanilla(File directory, String target, java.util.Collection<String> events) throws IOException {
        JsonObject definition = new JsonObject();
        definition.addProperty("id", "soundtrackselector:vanilla");
        definition.addProperty("name", "Vanilla");
        definition.addProperty("target", target);
        definition.addProperty("controller", "minecraft");
        definition.addProperty("selection", "native-context-and-scheduler");
        com.google.gson.JsonArray categories = new com.google.gson.JsonArray();
        for (String event : new java.util.TreeSet<String>(events)) categories.add(new com.google.gson.JsonPrimitive(event));
        definition.add("nativeCategories", categories);
        builtin = new JsonObject();
        builtin.add("soundtrackselector:vanilla", definition);
        dirty = true;
        if (directory != null) {
            writeJson(new File(directory, "soundtrackselector.vanilla.recovery.json"), definition);
            writeJson(new File(directory, "soundtrackselector.vanilla.json"), definition);
        }
    }

    private static void writeJson(File target, JsonObject root) throws IOException {
        Path directory = target.getAbsoluteFile().getParentFile().toPath();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "soundtrackselector-", ".tmp");
        try {
            Writer writer = new OutputStreamWriter(Files.newOutputStream(temporary), "UTF-8");
            try { new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root, writer); writer.write('\n'); }
            finally { writer.close(); }
            try { Files.move(temporary, target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, target.toPath(), StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    public void save() throws IOException {
        if (!dirty || file == null) return;
        JsonObject root = new JsonObject();
        root.addProperty("format", 3);
        root.addProperty("activePlaylist", active);
        root.addProperty("playbackMode", "vanilla"); // Playback ownership is session-only; never autoplay on restart.
        if (builtin != null) root.add("builtins", builtin);
        JsonObject names = new JsonObject();
        for (Map.Entry<String, String> entry : aliases.entrySet()) names.addProperty(entry.getKey(), entry.getValue());
        root.add("aliases", names);
        root.addProperty("loop", loop);
        root.addProperty("randomOrder", randomOrder);
        JsonObject stored = new JsonObject();
        for (Map.Entry<String, Map<String, Boolean>> playlist : playlists.entrySet()) {
            JsonObject tracks = new JsonObject();
            for (Map.Entry<String, Boolean> entry : playlist.getValue().entrySet()) tracks.addProperty(entry.getKey(), entry.getValue());
            stored.add(playlist.getKey(), tracks);
        }
        root.add("playlists", stored);
        writeJson(file, root);
        dirty = false;
    }
}
