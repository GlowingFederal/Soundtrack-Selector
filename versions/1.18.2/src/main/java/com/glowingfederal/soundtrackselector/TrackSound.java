package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.Track;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

final class TrackSound extends AbstractSoundInstance {
    private final Sound chosen;
    private final WeighedSoundEvents accessor;
    TrackSound(ResourceLocation context, Track track) {
        super(context, SoundSource.MUSIC);
        volume = 1.0F; pitch = 1.0F; relative = true;
        attenuation = SoundInstance.Attenuation.NONE;
        String file = track.id.replace(":sounds/", ":").replaceFirst("\\.ogg$", "");
        chosen = new Sound(file, 1.0F, 1.0F, 1, Sound.Type.FILE, true, false, 16);
        sound = chosen;
        accessor = new WeighedSoundEvents(context, null);
        accessor.addSound(chosen);
    }
    @Override public WeighedSoundEvents resolve(SoundManager manager) { sound = chosen; return accessor; }
}
