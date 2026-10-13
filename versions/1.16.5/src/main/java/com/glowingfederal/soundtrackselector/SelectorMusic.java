package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.*;
import java.lang.reflect.Field;
import net.minecraft.client.audio.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/** Installed only during explicit custom playback; the original scheduler retains its state. */
final class SelectorMusic extends MusicTicker implements Library.Playback {
    private static final Field CONTROLLER = FieldAccess.unique(Minecraft.class, MusicTicker.class);
    private static final Field NATIVE_CURRENT = FieldAccess.unique(MusicTicker.class, ISound.class);
    private final Minecraft mc;
    private MusicTicker original;
    private ISound sound;
    private Track selected;
    private boolean paused;
    private int grace;
    private Object world;
    SelectorMusic(Minecraft mc) { super(mc); this.mc = mc; world = mc.level; }
    void reset() {
        if (sound != null) mc.getSoundManager().stop(sound);
        sound = null; selected = null; paused = false; grace = 0;
        if (original != null && FieldAccess.get(CONTROLLER, mc) == this) FieldAccess.set(CONTROLLER, mc, original);
        original = null;
        ClientAdapter.library.relinquished();
    }
    @Override public void tick() {
        if (world != mc.level || FieldAccess.get(CONTROLLER, mc) != this) {
            world = mc.level; ClientAdapter.library.stop(); return;
        }
        if (mc.getSituationalMusic() != null && Library.specialMusic(mc.getSituationalMusic().getEvent().getLocation().toString())) { ClientAdapter.library.stop(); return; }
        if (grace > 0) grace--;
        if (!paused && selected != null && current() == null) ClientAdapter.library.previewFinished();
    }
    @Override public void play(Track track) {
        if (mc.getSituationalMusic() != null && Library.specialMusic(mc.getSituationalMusic().getEvent().getLocation().toString())) { ClientAdapter.library.status = "Playback unavailable during dragon/credits music"; return; }
        if (original == null) {
            original = (MusicTicker) FieldAccess.get(CONTROLLER, mc);
            ISound previous = (ISound) FieldAccess.get(NATIVE_CURRENT, original);
            // Stop only the controller's own background instance. Records and independent mod sounds are untouched.
            if (previous != null) mc.getSoundManager().stop(previous);
            FieldAccess.set(CONTROLLER, mc, this);
        }
        if (sound != null) mc.getSoundManager().stop(sound);
        world = mc.level; paused = false; grace = 40;
        sound = new TrackSound(new ResourceLocation("minecraft:music.menu"), track); selected = track;
        mc.getSoundManager().play(sound);
        ClientAdapter.library.played(track);
    }
    void checkOwnership() { if (original != null && FieldAccess.get(CONTROLLER, mc) != this) ClientAdapter.library.stop(); }
    @Override public void stop() { reset(); }
    @Override public Track current() {
        return selected != null && sound != null && (paused || grace > 0 || mc.getSoundManager().isActive(sound)) ? selected : null;
    }
    @Override public boolean paused() { return paused; }
    @Override public void togglePause() {
        if (sound != null && TrackPlayback.pause(mc.getSoundManager(), sound, !paused)) paused = !paused;
    }
    @Override public void release() { ClientAdapter.library.stop(); }
    // An external request belongs to the native controller, never to the catalog.
    @Override public void startPlaying(BackgroundMusicSelector music) {
        MusicTicker controller = original;
        reset();
        if (controller != null) controller.startPlaying(music);
    }
    @Override public void stopPlaying() { reset(); }
}
