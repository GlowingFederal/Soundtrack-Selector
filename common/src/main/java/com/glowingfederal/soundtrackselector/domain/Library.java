package com.glowingfederal.soundtrackselector.domain;

import java.io.*;
import java.util.*;

/** One cached catalog per client; native adapters own the resource lifecycle. */
public final class Library {
    public interface Resources { InputStream open(String id) throws IOException; }
    public interface Log {
        void info(String message);
        void warning(String message, Throwable failure);
        void debug(String message, Throwable failure);
    }
    public interface Playback {
        void play(Track track);
        void stop();
        Track current();
        void togglePause();
        boolean paused();
        void release();
    }
    public Playback playback;
    private long started, pausedAt;
    public void played(Track track) { started = System.nanoTime(); pausedAt = 0; }
    public void play(Track track) {
        if (playback == null || track == null) return;
        catalog.begin(track, playerRandom);
        playback.play(track);
    }
    public void stop() { if (playback != null) playback.stop(); }
    public void next() {
        if (playback == null) return;
        Track track = catalog.next(playerRandom);
        // An explicit Next restarts a finished non-looping playlist.
        if (track == null && catalog.hasEnabled()) { catalog.restart(); track = catalog.next(playerRandom); }
        if (track != null) playback.play(track);
    }
    public void previewFinished() {
        if (playback == null) return;
        // Failed/muted audio must not turn loop into a per-tick retry cycle.
        if (elapsed() < 5.0) { stop(); status = "Playback stopped: sound unavailable or music muted"; return; }
        Track track = catalog.next(playerRandom);
        if (track == null) stop(); else playback.play(track);
    }
    private final Random playerRandom = new Random();
    public void togglePause() {
        if (playback == null || playback.current() == null) return;
        playback.togglePause();
        if (playback.paused()) pausedAt = System.nanoTime();
        else if (pausedAt != 0) { started += System.nanoTime() - pausedAt; pausedAt = 0; }
    }
    public void release() {
        if (playback == null) return;
        if (playback.paused()) togglePause();
        playback.release();
    }
    public double elapsed() { return Math.max(0, ((pausedAt == 0 ? System.nanoTime() : pausedAt) - started) / 1000000000.0); }
    public final Catalog catalog;
    private final Log log;
    public String status = "Waiting for sound resources";
    private boolean writable = true;

    public Library(File directory, Log log) {
        this.log = log;
        File file = new File(directory, "soundtrackselector.json");
        Preferences preferences;
        try { preferences = new Preferences(file); }
        catch (IOException failure) {
            log.warning("Cannot read " + file + "; preserving the file and using enabled defaults for this session", failure);
            writable = false;
            try { preferences = new Preferences(null); }
            catch (IOException impossible) { throw new IllegalStateException(impossible); }
        }
        catalog = new Catalog(preferences);
    }

    public void discover(Map<String, String> files, Resources resources) {
        List<Track> tracks = new ArrayList<Track>();
        Set<String> namespaces = new HashSet<String>();
        int musicCount = 0;
        int shortCount = 0;
        int failures = 0;
        for (Map.Entry<String, String> file : files.entrySet()) {
            try {
                InputStream input = resources.open(file.getKey());
                double duration;
                try { duration = OggDuration.read(input); } finally { input.close(); }
                if (duration > 10.0) {
                    Track track = new Track(file.getKey(), file.getValue(), duration);
                    tracks.add(track);
                    if (track.music) musicCount++;
                    namespaces.add(track.namespace);
                } else shortCount++;
            } catch (IOException failure) {
                failures++;
                log.debug("Skipping unreadable soundtrack resource " + file.getKey(), failure);
            } catch (RuntimeException failure) {
                failures++;
                log.debug("Skipping invalid soundtrack resource " + file.getKey(), failure);
            }
        }
        catalog.replace(tracks);
        status = musicCount + " music tracks; " + (tracks.size() - musicCount) + " other long audio";
        log.info("Soundtrack Selector discovered " + tracks.size() + " resources longer than 10s (" + musicCount + " music candidates) from " + namespaces.size()
                + " namespaces; " + shortCount + " short sounds ignored; " + failures + " unreadable resources skipped.");
    }

    public void save() {
        if (!writable) { status = "Preferences file unreadable; changes are session-only (see log)"; return; }
        try { catalog.preferences.save(); }
        catch (IOException failure) {
            status = "Could not save preferences (see log)";
            log.warning("Cannot save soundtrackselector.json", failure);
        }
    }
    public void nextPlaylist() { catalog.preferences.nextPlaylist(); catalog.refresh(); stop(); save(); }
    public void toggleLoop() { catalog.preferences.toggleLoop(); save(); }
    public void toggleRandomOrder() { catalog.preferences.toggleRandomOrder(); catalog.restart(); save(); }
    public boolean createPlaylist(String name) {
        if (!catalog.preferences.createPlaylist(name)) { status = "Use a unique playlist name (1-48 characters)"; return false; }
        catalog.refresh(); stop(); save(); return true;
    }
    public void deletePlaylist() { catalog.preferences.deletePlaylist(); catalog.refresh(); stop(); save(); }
    public void toggle(Track track) { catalog.toggle(track); save(); }
    public void setTracks(Collection<Track> tracks, boolean enabled) { catalog.set(tracks, enabled); save(); }
    public void setAll(boolean enabled) { catalog.setAll(enabled); save(); }

    public static boolean specialMusic(String event) {
        return event.equals("minecraft:music.game.end.dragon") || event.equals("minecraft:music.dragon")
                || event.equals("minecraft:music.game.end.credits") || event.equals("minecraft:music.credits");
    }
}
