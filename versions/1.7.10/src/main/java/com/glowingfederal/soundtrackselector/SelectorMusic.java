package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.*;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.util.ResourceLocation;

/** Owns only background music; native sound effects and record playback are untouched. */
final class SelectorMusic extends MusicTicker implements Library.Playback {
    private final Minecraft mc;
    private final Random random = new Random();
    private ISound current;
    private ResourceLocation context;
    private Track selected;
    private boolean preview, paused;
    private Object world;
    private int delay = 100;
    SelectorMusic(Minecraft mc) { super(mc); this.mc = mc; world = mc.theWorld; }
    void reset() {
        if (current != null) mc.getSoundHandler().stopSound(current);
        current = null; selected = null; context = null; delay = 100; preview = false; paused = false;
    }
    @Override public void update() {
        MusicType type = mc.func_147109_W();
        ResourceLocation location = type.getMusicTickerLocation();
        int min = type.func_148634_b(), max = type.func_148633_c();
        if (world != mc.theWorld) { world = mc.theWorld; reset(); }
        if (selected != null && !preview && !ClientAdapter.library.catalog.preferences.enabled(selected)) reset();
        if ((preview || paused) && Library.specialMusic(location.toString())) reset();
        if (preview || paused) {
            if (current() == null) {
                if (preview && !paused) ClientAdapter.library.previewFinished(); else reset();
            }
            return;
        }
        if (current != null) {
            if (!location.equals(context) || (selected != null && !ClientAdapter.library.catalog.preferences.enabled(selected))) {
                mc.getSoundHandler().stopSound(current);
                delay = random.nextInt(min / 2 + 1);
            }
            if (!mc.getSoundHandler().isSoundPlaying(current)) {
                current = null; selected = null;
                delay = Math.min(delay, between(min, max));
            }
        }
        delay = Math.min(delay, max);
        if (current == null && delay-- <= 0) start(type);
    }
    private int between(int min, int max) { return min == max ? min : min + random.nextInt(max - min + 1); }
    private void start(MusicType type) {
        if (current != null) mc.getSoundHandler().stopSound(current);
        ResourceLocation location = type.getMusicTickerLocation();
        Track track = Library.specialMusic(location.toString()) ? null : ClientAdapter.library.catalog.next(random);
        if (track == null && !Library.specialMusic(location.toString())) { delay = between(type.func_148634_b(), type.func_148633_c()); return; }
        current = track == null ? PositionedSoundRecord.func_147673_a(location) : PositionedSoundRecord.func_147673_a(ClientAdapter.nativeTracks.get(track.id));
        selected = track; context = location;
        ClientAdapter.library.played(track);
        mc.getSoundHandler().playSound(current);
        delay = Integer.MAX_VALUE;
    }

    @Override public void play(Track track) {
        if (Library.specialMusic(mc.func_147109_W().getMusicTickerLocation().toString())) { ClientAdapter.library.status = "Playback unavailable during dragon/credits music"; return; }
        reset(); preview = true;
        context = new ResourceLocation("minecraft:music.menu");
        current = PositionedSoundRecord.func_147673_a(ClientAdapter.nativeTracks.get(track.id));
        selected = track;
        mc.getSoundHandler().playSound(current);
        ClientAdapter.library.played(track);
        delay = Integer.MAX_VALUE;
    }
    @Override public void stop() { if (selected != null) reset(); }
    @Override public Track current() { return selected != null && current != null && mc.getSoundHandler().isSoundPlaying(current) ? selected : null; }
    @Override public boolean paused() { return paused; }
    @Override public void togglePause() {
        if (current != null && TrackPlayback.pause(mc.getSoundHandler(), current, !paused)) paused = !paused;
    }
    @Override public void release() { if (preview) reset(); }
}
