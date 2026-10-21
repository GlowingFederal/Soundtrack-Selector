package com.glowingfederal.soundtrackselector.domain;

import java.util.Locale;

/** A resolved audio resource, independent of a loader's sound-event registry. */
public final class Track {
    public final String id;
    public final String event;
    public final String namespace;
    public final String name;
    public final String searchText;
    public final double seconds;
    public final boolean music;
    public final int artwork;
    public final String disc;
    private final String alias;

    public Track(String id, String event, double seconds) {
        this(id, event, seconds, event.indexOf(':') >= 0 && !event.endsWith(".ogg") ? event : id);
    }
    public Track(String id, String event, double seconds, String alias) {
        this.alias = alias;
        this.id = id;
        this.event = event;
        this.seconds = seconds;
        this.music = musicHint(id) || musicHint(event);
        String hint = (id + " " + event).toLowerCase(Locale.ROOT);
        String record = null;
        if (id.startsWith("minecraft:") && (hint.contains("record") || hint.contains("disc"))) {
            String leaf = id.substring(id.lastIndexOf('/') + 1).replace(".ogg", "");
            for (String known : new String[]{"13","cat","blocks","chirp","far","mall","mellohi","stal","strad","ward","11","wait","pigstep","otherside","5","relic","creator","creator_music_box","precipice","tears","lava_chicken"})
                if (leaf.equals(known)) { record = known; break; }
        }
        disc = record;
        artwork = record != null ? 5 : hint.contains("nether") ? 2 : hint.contains("end/") || hint.contains(".end") ? 3
                : hint.contains("menu") ? 4 : id.startsWith("minecraft:") && music ? 1 : music ? 0 : 6;
        this.namespace = id.substring(0, id.indexOf(':'));
        String path = id.substring(id.indexOf(':') + 1).replaceFirst("^(sounds|sound|music|records)/", "")
                .replaceFirst("\\.ogg$", "");
        StringBuilder readable = new StringBuilder();
        for (String word : path.split("[/._-]+")) {
            if (word.length() == 0) continue;
            if (readable.length() > 0) readable.append(' ');
            readable.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        name = readable.toString();
        searchText = (name + " " + id + " " + event).toLowerCase(Locale.ROOT);
    }

    public static boolean musicHint(String value) {
        int colon = value.indexOf(':');
        if (colon >= 0) value = value.substring(colon + 1);
        for (String token : value.toLowerCase(Locale.ROOT).split("[:/._-]+"))
            if (token.equals("music") || token.equals("soundtrack") || token.equals("song")
                    || token.equals("songs") || token.equals("record") || token.equals("records")
                    || token.equals("disc") || token.equals("discs")) return true;
        return false;
    }

    public String durationText() {
        int total = (int) seconds;
        return (total / 60) + ":" + (total % 60 < 10 ? "0" : "") + (total % 60);
    }

    /** Event identity survives a pack replacing the file behind the sound. Legacy pools fall back to the resource. */
    public String aliasId() { return alias; }
}
