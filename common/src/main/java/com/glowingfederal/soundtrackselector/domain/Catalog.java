package com.glowingfederal.soundtrackselector.domain;

import java.util.*;

/** Runtime lists are rebuilt only after discovery or a user preference change. */
public final class Catalog {
    private List<Track> tracks = Collections.emptyList();
    private Track[] enabled = new Track[0];
    private Track[] order;
    private int cursor;
    private final Comparator<Track> BY_NAME = new Comparator<Track>() {
        public int compare(Track a, Track b) {
            int result = preferences.displayName(a).compareToIgnoreCase(preferences.displayName(b));
            return result == 0 ? a.id.compareTo(b.id) : result;
        }
    };
    private int revision;
    public final Preferences preferences;

    public Catalog(Preferences preferences) { this.preferences = preferences; }
    public List<Track> tracks() { return tracks; }
    public int revision() { return revision; }
    public int enabledCount() { return enabled.length; }
    public boolean hasEnabled() { return enabled.length != 0; }
    /** Each enabled track plays once per pass; only cycle boundaries reorder the cache. */
    public Track next(Random random) {
        if (enabled.length == 0) return null;
        if (order == null) prepare(random);
        else if (cursor == order.length) {
            if (!preferences.loop()) return null;
            prepare(random);
        }
        return order[cursor++];
    }
    public void restart() { order = null; cursor = 0; }
    public void begin(Track track, Random random) {
        prepare(random);
        for (int i = 0; i < order.length; i++) if (order[i].id.equals(track.id)) {
            if (preferences.randomOrder()) {
                Track first = order[0]; order[0] = order[i]; order[i] = first;
                cursor = 1;
            } else cursor = i + 1;
            return;
        }
        // A disabled resource can still be auditioned before the enabled playlist.
    }
    private void prepare(Random random) {
        if (order == null || order.length != enabled.length) order = enabled.clone();
        if (preferences.randomOrder()) {
            for (int i = order.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                Track previous = order[i]; order[i] = order[j]; order[j] = previous;
            }
        } else Arrays.sort(order, BY_NAME);
        cursor = 0;
    }

    public void replace(Collection<Track> discovered) {
        List<Track> sorted = new ArrayList<Track>(discovered);
        Collections.sort(sorted, new Comparator<Track>() {
            public int compare(Track a, Track b) { return a.id.compareTo(b.id); }
        });
        tracks = Collections.unmodifiableList(sorted);
        rebuild();
    }

    public void refresh() { rebuild(); }
    public void toggle(Track track) { preferences.set(track.id, !preferences.enabled(track)); rebuild(); }
    public void setAll(boolean value) { set(tracks, value); }
    public void set(Collection<Track> entries, boolean value) {
        for (Track track : entries) preferences.set(track.id, value);
        rebuild();
    }
    private void rebuild() {
        List<Track> selected = new ArrayList<Track>();
        for (Track track : tracks) if (preferences.enabled(track)) selected.add(track);
        enabled = selected.toArray(new Track[selected.size()]);
        restart();
        revision++;
    }
}
