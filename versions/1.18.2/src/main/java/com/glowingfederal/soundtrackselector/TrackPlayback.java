package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.FieldAccess;
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.sounds.*;
import net.minecraft.client.resources.sounds.SoundInstance;

/** Controls one native channel through its owning audio executor. */
final class TrackPlayback {
    private static final Field ENGINE = FieldAccess.unique(SoundManager.class, SoundEngine.class);
    private static final Field CHANNELS = FieldAccess.mapValue(SoundEngine.class, ChannelAccess.ChannelHandle.class);
    static boolean pause(SoundManager manager, SoundInstance sound, final boolean paused) {
        SoundEngine engine = (SoundEngine) FieldAccess.get(ENGINE, manager);
        Object entry = ((Map<?, ?>) FieldAccess.get(CHANNELS, engine)).get(sound);
        if (entry == null) return false;
        ((ChannelAccess.ChannelHandle) entry).execute(channel -> { if (paused) channel.pause(); else channel.unpause(); });
        return true;
    }
}
