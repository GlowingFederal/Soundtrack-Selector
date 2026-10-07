package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.Track;
import net.minecraft.client.audio.*;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;

final class TrackSound extends LocatableSound {
    private final Sound chosen;
    private final SoundEventAccessor accessor;
    TrackSound(ResourceLocation context, Track track) {
        super(context, SoundCategory.MUSIC);
        volume = 1.0F; pitch = 1.0F; relative = true;
        attenuation = ISound.AttenuationType.NONE;
        String file = track.id.replace(":sounds/", ":").replaceFirst("\\.ogg$", "");
        chosen = new Sound(file, 1.0F, 1.0F, 1, Sound.Type.FILE, true, false, 16);
        sound = chosen;
        accessor = new SoundEventAccessor(context, null);
        accessor.addSound(chosen);
    }
    @Override public SoundEventAccessor resolve(SoundHandler manager) { sound = chosen; return accessor; }
}
