package com.glowingfederal.soundtrackselector.domain;

import java.util.List;

/** Platform-neutral layout and actions; adapters provide native text, buttons and input. */
public final class PlayerView {
    public interface Canvas {
        void rectangle(int left, int top, int right, int bottom, int color);
        void text(String value, int x, int y, int availableWidth, int color);
    }
    public final ListView list;
    private final Library library;
    public int width, height;
    public String query = "";
    public String playlistName = "";
    public boolean editingNickname;
    private String previousQuery;
    private Track nicknameTrack;
    private boolean deleteArmed;
    private String clickedTrack;
    private long clickedAt;
    public PlayerView(Library library) { this.library = library; list = new ListView(library.catalog); }
    public int left() { return width >= 460 ? 144 : 16; }
    private boolean compact() { return height < 300; }
    public int visible() { return Math.max(1, (height - (compact() ? 180 : 212)) / 26); }
    public List<Track> rows() { List<Track> rows = list.rows(editingNickname ? previousQuery : query); list.clamp(visible()); return rows; }
    public void action(int id) {
        clickedTrack = null;
        if (id == 18) {
            if (editingNickname) {
                library.rename(nicknameTrack, query); query = previousQuery; editingNickname = false;
            } else if (list.selected != null) {
                nicknameTrack = list.selected; previousQuery = query;
                query = library.catalog.preferences.nickname(nicknameTrack); editingNickname = true;
            }
            return;
        }
        if (id != 15) deleteArmed = false;
        if (id == 13) library.nextPlaylist();
        if (id == 14) library.createPlaylist(playlistName);
        if (id == 15) { if (deleteArmed) { library.deletePlaylist(); deleteArmed = false; } else deleteArmed = true; }
        if (id == 1) library.setTracks(rows(), true);
        if (id == 2) library.setTracks(rows(), false);
        if (id == 4) list.cycleFilter();
        if (id == 5) list.cycleSort();
        if (id == 6) library.play(list.selected);
        if (id == 7) library.togglePause();
        if (id == 8) library.stop();
        if (id == 9) library.next();
        if (id == 16) library.toggleLoop();
        if (id == 17) library.toggleRandomOrder();
        if (id >= 10 && id <= 12) list.filter(id - 10);
    }
    public boolean click(double x, double y) {
        if (editingNickname) return false;
        if (x < left() || x >= width - 16 || y < 118 || y >= 118 + visible() * 26) { clickedTrack = null; return false; }
        List<Track> rows = rows();
        int index = list.offset + (int) ((y - 118) / 26);
        if (index >= rows.size()) { clickedTrack = null; return false; }
        Track track = rows.get(index);
        list.selected = track;
        if (x < left() + 28) { clickedTrack = null; library.toggle(track); }
        else {
            long now = System.nanoTime();
            if (track.id.equals(clickedTrack) && now - clickedAt <= 250000000L) {
                clickedTrack = null;
                library.play(track);
            } else { clickedTrack = track.id; clickedAt = now; }
        }
        return true;
    }
    public void scroll(int delta) { clickedTrack = null; rows(); int step = Math.min(Math.abs(delta), visible()); list.scroll(delta < 0 ? -step : step, visible()); }
    public String label(int id) {
        if (id == 18) return editingNickname ? "Save name" : "Nickname";
        if (id == 9) return library.custom() ? "Next" : "Start";
        if (id == 13) return library.catalog.preferences.activePlaylist();
        if (id == 15) return deleteArmed ? "Sure?" : "Delete";
        if (id == 16) return "Loop: " + (library.catalog.preferences.loop() ? "On" : "Off");
        if (id == 17) return "Random: " + (library.catalog.preferences.randomOrder() ? "On" : "Off");
        if (id == 4) return list.filterLabel();
        if (id == 5) return "Sort: " + list.sortLabel();
        if (id == 7) return library.playback != null && library.playback.paused() ? "Resume" : "Pause";
        return "";
    }
    public boolean available(int id) {
        if (id == 18) return editingNickname || list.selected != null;
        if (editingNickname && id != 3) return false;
        if (id == 1 || id == 2) return !library.catalog.preferences.vanilla();
        if (id == 15) return !library.catalog.preferences.vanilla() && !library.catalog.preferences.activePlaylist().equals("Default");
        if (id == 6) return list.selected != null && library.playback != null;
        if (id == 8) return library.custom();
        if (id == 7) return library.playback != null && library.playback.current() != null;
        if (id == 9) return library.playback != null && library.catalog.hasEnabled();
        return true;
    }
    public void draw(Canvas c, int mouseX, int mouseY) {
        c.rectangle(0, 0, width, height, 0xff0b1118);
        int left = left(), right = width - 16;
        if (width >= 460) {
            c.rectangle(0, 0, 128, height - 80, 0xff111d29);
            c.text("SOUNDTRACK", 14, 17, 104, 0xff7ce6b5);
            c.text("SELECTOR", 14, 30, 104, 0xffffffff);
            c.text("LIBRARY VIEWS", 14, 54, 104, 0xff8094a8);
            if (!compact()) c.text(library.catalog.enabledCount() + " enabled", 14, height - 111, 104, 0xff7ce6b5);
            if (!compact()) c.text("Music volume applies", 14, height - 99, 104, 0xff8094a8);
        }
        c.text("Soundtrack Selector", left, 13, width - 168 - left, 0xffffffff);
        c.text(library.catalog.preferences.playlistCount() + " playlists / " + library.status, left, 26, right - left, 0xff8094a8);
        List<Track> rows = rows();
        for (int i = 0; i < visible() && list.offset + i < rows.size(); i++) {
            Track track = rows.get(list.offset + i);
            int top = 118 + i * 26;
            boolean selected = list.selected != null && list.selected.id.equals(track.id);
            boolean hovered = mouseX >= left && mouseX < right && mouseY >= top && mouseY < top + 24;
            boolean enabled = library.catalog.preferences.enabled(track);
            c.rectangle(left, top, right, top + 24, selected ? 0xff243c4d : hovered ? 0xff1b2b3b : 0xff131f2c);
            if (selected) c.rectangle(left, top, left + 2, top + 24, 0xff7ce6b5);
            c.rectangle(left + 8, top + 8, left + 20, top + 20, enabled ? 0xff7ce6b5 : 0xff425568);
            c.text(enabled ? "+" : "-", left + 11, top + 10, 9, enabled ? 0xff10231d : 0xffc6d4e0);
            c.text(library.catalog.preferences.displayName(track), left + 30, top + 4, right - left - 76, 0xffeef4fa);
            c.text(track.event, left + 30, top + 16, right - left - 76, 0xff8094a8);
            c.text(track.durationText(), right - 42, top + 10, 38, 0xffc6d4e0);
        }
        if (rows.isEmpty()) c.text("No matching tracks. Try Long audio or another search.", left, 122, right - left, 0xff8094a8);
        int trackHeight = visible() * 26;
        if (rows.size() > visible()) {
            int thumb = Math.max(8, trackHeight * visible() / rows.size());
            int top = 118 + (trackHeight - thumb) * list.offset / Math.max(1, rows.size() - visible());
            c.rectangle(right + 5, 118, right + 7, 92 + trackHeight, 0xff243747);
            c.rectangle(right + 5, top, right + 7, top + thumb, 0xff7ce6b5);
        }
        if (!compact()) c.text(rows.size() + " in view / " + library.catalog.enabledCount() + " enabled | All on/off: this view", left, height - 92, right - left, 0xff8094a8);
        c.rectangle(0, height - (compact() ? 60 : 80), width, height, 0xff111d29);
        c.rectangle(0, height - (compact() ? 60 : 80), width, height - (compact() ? 59 : 79), 0xff2c4254);
        Track playing = library.playback == null ? null : library.playback.current();
        Track details = list.selected;
        if (!compact()) c.text(details == null ? "Select a track, then Play, or double-click to listen" : "Selected: " + library.catalog.preferences.displayName(details) + " / " + details.id, 16, height - 71, width - 32, 0xffc6d4e0);
        c.text(playing == null ? "VANILLA   Minecraft controls music" : (library.playback.paused() ? "PAUSED   " : "NOW PLAYING   ") + library.catalog.preferences.displayName(playing) + " / " + playing.namespace,
                16, height - 56, width - 32, 0xff7ce6b5);
        c.rectangle(16, height - 40, width - 16, height - 37, 0xff2c4254);
        if (playing != null) {
            int progress = (int) ((width - 32) * Math.min(1.0, library.elapsed() / playing.seconds));
            c.rectangle(16, height - 40, 16 + progress, height - 37, 0xff7ce6b5);
        }
    }
}
