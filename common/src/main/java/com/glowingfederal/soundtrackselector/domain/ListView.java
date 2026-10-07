package com.glowingfederal.soundtrackselector.domain;

import java.util.*;

/** Cached library views: music, enabled soundtrack, or all long audio. */
public final class ListView {
    private final Catalog catalog;
    private final List<Track> filtered = new ArrayList<Track>();
    private String query = "";
    private int revision = -1;
    private int filter, sort;
    public int offset;
    public Track selected;
    public ListView(Catalog catalog) { this.catalog = catalog; }
    public String filterLabel() { return filter == 0 ? "Music" : filter == 1 ? "Soundtrack" : "Long audio"; }
    public String sortLabel() { return sort == 0 ? "Name" : sort == 1 ? "Source" : "Length"; }
    public void cycleFilter() { filter = (filter + 1) % 3; revision = -1; offset = 0; }
    public void cycleSort() { sort = (sort + 1) % 3; revision = -1; offset = 0; }
    public void filter(int value) { filter = value; revision = -1; offset = 0; }
    public List<Track> rows(String search) {
        String next = search.toLowerCase(Locale.ROOT).trim();
        if (revision != catalog.revision() || !next.equals(query)) {
            if (!next.equals(query)) offset = 0;
            query = next;
            revision = catalog.revision();
            filtered.clear();
            for (Track track : catalog.tracks()) {
                if (filter == 0 && !track.music) continue;
                if (filter == 1 && !catalog.preferences.enabled(track)) continue;
                if (track.searchText.contains(query)) filtered.add(track);
            }
            Collections.sort(filtered, new Comparator<Track>() {
                public int compare(Track a, Track b) {
                    int value = sort == 2 ? Double.compare(b.seconds, a.seconds)
                            : sort == 1 ? a.namespace.compareTo(b.namespace) : a.name.compareTo(b.name);
                    return value != 0 ? value : a.id.compareTo(b.id);
                }
            });
            if (selected != null) {
                Track replacement = null;
                for (Track track : catalog.tracks()) if (track.id.equals(selected.id)) { replacement = track; break; }
                selected = replacement;
            }
        }
        return filtered;
    }
    public void clamp(int visible) { offset = Math.max(0, Math.min(offset, Math.max(0, filtered.size() - visible))); }
    public void scroll(int amount, int visible) { offset += amount; clamp(visible); }
}
