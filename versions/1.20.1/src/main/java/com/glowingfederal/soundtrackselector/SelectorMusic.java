package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.*;
import java.lang.reflect.Field;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.*;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.Music;
import net.minecraft.resources.ResourceLocation;

final class SelectorMusic extends MusicManager implements Library.Playback {
    private static final Field CURRENT = FieldAccess.unique(MusicManager.class, SoundInstance.class);
    private static final Field DELAY = FieldAccess.unique(MusicManager.class, int.class);
    private final Minecraft mc;
    private final Random random = new Random();
    private Track selected;
    private boolean preview, paused;
    private Object world;
    SelectorMusic(Minecraft mc) { super(mc); this.mc = mc; world = mc.level; }
    void reset() { stopPlaying(); selected = null; preview = false; paused = false; FieldAccess.set(DELAY, this, 100); }
    @Override public void tick() {
        if (world != mc.level) { world = mc.level; reset(); }
        if (selected != null && !preview && !ClientAdapter.library.catalog.preferences.enabled(selected)) reset();
        if ((preview || paused) && Library.specialMusic(mc.getSituationalMusic().getEvent().value().getLocation().toString())) reset();
        if (preview || paused) {
            if (current() == null) {
                if (preview && !paused) ClientAdapter.library.previewFinished(); else reset();
            }
            return;
        }
        super.tick();
    }
    @Override public void startPlaying(Music music) {
        ResourceLocation context = music.getEvent().value().getLocation();
        if (Library.specialMusic(context.toString())) { selected = null; super.startPlaying(music); return; }
        Track track = ClientAdapter.library.catalog.next(random);
        if (track == null) {
            selected = null;
            FieldAccess.set(DELAY, this, music.getMinDelay() == music.getMaxDelay() ? music.getMinDelay() : music.getMinDelay() + random.nextInt(music.getMaxDelay() - music.getMinDelay() + 1));
            return;
        }
        SoundInstance previous = (SoundInstance) FieldAccess.get(CURRENT, this);
        if (previous != null) mc.getSoundManager().stop(previous);
        TrackSound sound = new TrackSound(context, track);
        FieldAccess.set(CURRENT, this, sound);
        selected = track;
        ClientAdapter.library.played(track);
        mc.getSoundManager().play(sound);
        FieldAccess.set(DELAY, this, Integer.MAX_VALUE);
    }

    @Override public void play(Track track) {
        if (Library.specialMusic(mc.getSituationalMusic().getEvent().value().getLocation().toString())) { ClientAdapter.library.status = "Playback unavailable during dragon/credits music"; return; }
        reset(); preview = true;
        TrackSound sound = new TrackSound(new ResourceLocation("minecraft:music.menu"), track);
        FieldAccess.set(CURRENT, this, sound);
        selected = track;
        mc.getSoundManager().play(sound);
        ClientAdapter.library.played(track);
        FieldAccess.set(DELAY, this, Integer.MAX_VALUE);
    }
    @Override public void stop() { if (selected != null) reset(); }
    @Override public Track current() {
        SoundInstance sound = (SoundInstance) FieldAccess.get(CURRENT, this);
        return selected != null && sound != null && mc.getSoundManager().isActive(sound) ? selected : null;
    }
    @Override public boolean paused() { return paused; }
    @Override public void togglePause() {
        SoundInstance sound = (SoundInstance) FieldAccess.get(CURRENT, this);
        if (sound != null && TrackPlayback.pause(mc.getSoundManager(), sound, !paused)) paused = !paused;
    }
    @Override public void release() { if (preview) reset(); }
}
