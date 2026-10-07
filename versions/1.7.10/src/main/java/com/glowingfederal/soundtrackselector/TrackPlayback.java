package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.FieldAccess;
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.audio.*;
import paulscode.sound.SoundSystem;

/** Paulscode exposes per-source pause; resolve only the current track's channel. */
final class TrackPlayback {
    private static final Field MANAGER = FieldAccess.unique(SoundHandler.class, SoundManager.class);
    private static final Field SYSTEM = FieldAccess.assignable(SoundManager.class, SoundSystem.class);
    static boolean pause(SoundHandler handler, ISound sound, boolean paused) {
        SoundManager manager = (SoundManager) FieldAccess.get(MANAGER, handler);
        SoundSystem system = (SoundSystem) FieldAccess.get(SYSTEM, manager);
        if (system == null) return false;
        for (Field field : SoundManager.class.getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(field.getType())) continue;
            field.setAccessible(true);
            Object channel = ((Map<?, ?>) FieldAccess.get(field, manager)).get(sound);
            if (channel instanceof String) {
                if (paused) system.pause((String) channel); else system.play((String) channel);
                return true;
            }
        }
        return false;
    }
}
