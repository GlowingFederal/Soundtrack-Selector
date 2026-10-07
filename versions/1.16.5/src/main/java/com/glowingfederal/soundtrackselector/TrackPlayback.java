package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.FieldAccess;
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.audio.*;

/** Controls one native channel through its owning audio executor. */
final class TrackPlayback {
    private static final Field ENGINE = FieldAccess.unique(SoundHandler.class, SoundEngine.class);
    private static final Field CHANNELS = FieldAccess.mapValue(SoundEngine.class, ChannelManager.Entry.class);
    static boolean pause(SoundHandler manager, ISound sound, final boolean paused) {
        SoundEngine engine = (SoundEngine) FieldAccess.get(ENGINE, manager);
        Object entry = ((Map<?, ?>) FieldAccess.get(CHANNELS, engine)).get(sound);
        if (entry == null) return false;
        ((ChannelManager.Entry) entry).execute(channel -> { if (paused) channel.pause(); else channel.unpause(); });
        return true;
    }
}
