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
    public final Appearance appearance;
    private final Deque<Track> history = new ArrayDeque<Track>();
    private Track lastPlayed;
    private boolean goingBack;
    private long started, pausedAt;
    private boolean custom;
    public boolean custom() { return custom; }
    public void played(Track track) {
        if (!goingBack && lastPlayed != null && !lastPlayed.id.equals(track.id)) {
            if (history.size() == 64) history.removeFirst();
            history.addLast(lastPlayed);
        }
        lastPlayed = track;
        custom = true; started = System.nanoTime(); pausedAt = 0;
    }
    public boolean hasPrevious() { return custom && !history.isEmpty(); }
    public void previous() {
        if (playback == null || !hasPrevious()) return;
        Track track = history.removeLast();
        goingBack = true;
        try { play(track); } finally { goingBack = false; }
    }
    public void play(Track track) {
        if (playback == null || track == null) return;
        catalog.begin(track, playerRandom);
        playback.play(track);
    }
    public void relinquished() { custom = false; history.clear(); lastPlayed = null; }
    public void stop() { if (playback != null) playback.stop(); relinquished(); catalog.preferences.returnToVanilla(); catalog.refresh(); save(); }
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
    public Log log() { return log; }
    public final Catalog catalog;
    private final Log log;
    public String status = "Waiting for sound resources";
    private boolean writable = true;

    public Library(File directory, Log log, String target, Collection<String> vanillaEvents) {
        this.log = log;
        appearance = new Appearance(directory, log);
        File file = new File(directory, "soundtrackselector.json");
        Preferences preferences;
        try { preferences = new Preferences(file); }
        catch (IOException failure) {
            log.warning("Cannot read " + file + "; preserving the file and using Vanilla for this session", failure);
            writable = false;
            try { preferences = new Preferences(null); }
            catch (IOException impossible) { throw new IllegalStateException(impossible); }
        }
        catalog = new Catalog(preferences);
        try {
            // Recovery is independent of whether the user's configuration is readable.
            preferences.maintainVanilla(directory, target, vanillaEvents);
            if (writable) preferences.save();
        } catch (IOException failure) { log.warning("Cannot maintain Vanilla recovery configuration", failure); }
    }

    private final Discovery discovery = new Discovery();
    private boolean refreshRequested;
    public void requestRefresh() { refreshRequested = true; }
    public boolean takeRefreshRequest() { if (discovery.busy()) return false; boolean value = refreshRequested; refreshRequested = false; return value; }
    public void invalidateDiscovery() { discovery.invalidate(); }
    public boolean discovering() { return discovery.busy(); }
    public void submit(Discovery.Scan scan) {
        refreshRequested = false;
        status = "Discovering audio...";
        discovery.submit(scan);
    }
    /** Client-thread publication uses the latest preferences, never a worker copy. */
    public void pollDiscovery() {
        synchronized (discovery) {
            Discovery.Completion completion = discovery.poll();
            if (completion == null) return;
            if (completion.failure != null) {
                status = "Discovery failed; refresh to retry (see log)";
                log.warning("Cannot discover soundtrack resources", completion.failure);
                return;
            }
            long start = System.nanoTime();
            catalog.replace(completion.result.tracks);
            completion.scan.published();
            status = completion.result.status;
            log.info("Soundtrack Selector client publication: " + (System.nanoTime() - start) / 1000000.0 + " ms");
        }
    }

    public static final class Result {
        public final List<Track> tracks;
        public final String status;
        Result(List<Track> tracks, String status) {
            this.tracks = Collections.unmodifiableList(new ArrayList<Track>(tracks));
            this.status = status;
        }
    }
    public Result inspect(Map<String, String> files, Resources resources) throws IOException {
        return inspect(files, resources, Collections.<String, String>emptyMap());
    }
    public Result inspect(Map<String, String> files, Resources resources, Map<String, String> aliases) throws IOException {
        long start = System.nanoTime();
        List<Track> tracks = new ArrayList<Track>();
        Set<String> namespaces = new HashSet<String>();
        int musicCount = 0;
        int shortCount = 0;
        int failures = 0;
        for (Map.Entry<String, String> file : files.entrySet()) {
            Discovery.checkCancelled();
            try {
                InputStream input = new BufferedInputStream(resources.open(file.getKey()));
                double duration;
                try { duration = OggDuration.read(input); } finally { input.close(); }
                if (duration > 10.0) {
                    String alias = aliases.get(file.getKey());
                    Track track = alias == null ? new Track(file.getKey(), file.getValue(), duration)
                            : new Track(file.getKey(), file.getValue(), duration, alias);
                    tracks.add(track);
                    if (track.music) musicCount++;
                    namespaces.add(track.namespace);
                } else shortCount++;
            } catch (IOException failure) {
                Discovery.checkCancelled();
                failures++;
                log.debug("Skipping unreadable soundtrack resource " + file.getKey(), failure);
            } catch (RuntimeException failure) {
                failures++;
                log.debug("Skipping invalid soundtrack resource " + file.getKey(), failure);
            }
        }
        Collections.sort(tracks, new Comparator<Track>() {
            public int compare(Track a, Track b) { return a.id.compareTo(b.id); }
        });
        String summary = musicCount + " music tracks; " + (tracks.size() - musicCount) + " other long audio";
        log.info("Soundtrack Selector discovered " + tracks.size() + " resources longer than 10s (" + musicCount + " music candidates) from " + namespaces.size()
                + " namespaces; " + shortCount + " short sounds ignored; " + failures + " unreadable resources skipped; metadata " + (System.nanoTime() - start) / 1000000.0 + " ms.");
        return new Result(tracks, summary);
    }

    public void save() {
        if (!writable) { status = "Preferences file unreadable; changes are session-only (see log)"; return; }
        try { catalog.preferences.save(); }
        catch (IOException failure) {
            status = "Could not save preferences (see log)";
            log.warning("Cannot save soundtrackselector.json", failure);
        }
    }
    public void nextPlaylist() { if (playback != null) playback.stop(); custom = false; catalog.preferences.nextPlaylist(); catalog.refresh(); save(); }
    public void toggleLoop() { catalog.preferences.toggleLoop(); save(); }
    public void toggleRandomOrder() { catalog.preferences.toggleRandomOrder(); catalog.restart(); save(); }
    public boolean createPlaylist(String name) {
        if (!catalog.preferences.createPlaylist(name)) { status = "Use a unique playlist name (1-48 characters)"; return false; }
        if (playback != null) playback.stop(); custom = false; catalog.refresh(); save(); return true;
    }
    public void deletePlaylist() { if (playback != null) playback.stop(); custom = false; catalog.preferences.deletePlaylist(); catalog.refresh(); save(); }
    public void rename(Track track, String name) {
        if (track == null) return;
        if (!catalog.preferences.rename(track, name)) { status = "Nickname must be at most 128 characters without control characters"; return; }
        catalog.refresh(); save();
    }
    public void toggle(Track track) { catalog.toggle(track); save(); }
    public void setTracks(Collection<Track> tracks, boolean enabled) { catalog.set(tracks, enabled); save(); }
    public void setAll(boolean enabled) { catalog.setAll(enabled); save(); }

    public static boolean specialMusic(String event) {
        return event.equals("minecraft:music.game.end.dragon") || event.equals("minecraft:music.dragon")
                || event.equals("minecraft:music.game.end.credits") || event.equals("minecraft:music.credits");
    }
}
