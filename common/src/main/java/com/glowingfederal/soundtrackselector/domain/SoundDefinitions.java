package com.glowingfederal.soundtrackselector.domain;

import com.google.gson.*;
import java.io.Reader;
import java.util.*;

/** Mirrors sounds.json pack stacking and event references without game types. */
public final class SoundDefinitions {
    private final Map<String, List<Reference>> events = new TreeMap<String, List<Reference>>();

    public void add(String namespace, Reader reader) {
        JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
        // Validate the whole document before changing the accumulated pack stack.
        Map<String, List<Reference>> parsed = new LinkedHashMap<String, List<Reference>>();
        Set<String> replacements = new HashSet<String>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String event = qualify(namespace, entry.getKey());
            JsonObject definition = entry.getValue().getAsJsonObject();
            if (definition.has("replace") && definition.get("replace").getAsBoolean()) replacements.add(event);
            List<Reference> refs = new ArrayList<Reference>();
            if (definition.has("sounds")) for (JsonElement sound : definition.getAsJsonArray("sounds")) {
                String name;
                boolean link = false;
                if (sound.isJsonPrimitive()) name = sound.getAsString();
                else {
                    JsonObject object = sound.getAsJsonObject();
                    name = object.get("name").getAsString();
                    String type = object.has("type") ? object.get("type").getAsString() : "file";
                    if (!type.equals("file") && !type.equals("event")) throw new IllegalArgumentException("Unknown sound type " + type);
                    link = type.equals("event");
                    if (object.has("weight") && object.get("weight").getAsInt() <= 0) continue;
                }
                // Minecraft resolves unqualified sound names in the minecraft namespace.
                refs.add(new Reference(qualify("minecraft", name), link));
            }
            parsed.put(event, refs);
        }
        for (Map.Entry<String, List<Reference>> entry : parsed.entrySet()) {
            List<Reference> previous = events.get(entry.getKey());
            if (previous == null || replacements.contains(entry.getKey())) events.put(entry.getKey(), entry.getValue());
            else previous.addAll(entry.getValue());
        }
    }

    public Map<String, String> files() {
        Map<String, String> result = new TreeMap<String, String>();
        for (String event : events.keySet()) collect(event, event, new HashSet<String>(), result);
        return result;
    }
    private void collect(String event, String origin, Set<String> visiting, Map<String, String> result) {
        if (visiting.size() >= 128 || !visiting.add(event)) return;
        List<Reference> refs = events.get(event);
        if (refs != null) for (Reference ref : refs) {
            if (ref.event) collect(ref.id, origin, visiting, result);
            else {
                int colon = ref.id.indexOf(':');
                String resource = ref.id.substring(0, colon + 1) + "sounds/" + ref.id.substring(colon + 1) + ".ogg";
                if (!result.containsKey(resource) || Track.musicHint(origin)) result.put(resource, origin);
            }
        }
        visiting.remove(event);
    }
    public static String qualify(String namespace, String id) { return id.indexOf(':') < 0 ? namespace + ":" + id : id; }
    private static final class Reference {
        final String id;
        final boolean event;
        Reference(String id, boolean event) { this.id = id; this.event = event; }
    }
}
