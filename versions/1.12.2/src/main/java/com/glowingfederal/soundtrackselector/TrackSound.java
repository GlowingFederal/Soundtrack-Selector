package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.Track;
import net.minecraft.client.audio.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
final class TrackSound extends PositionedSound {
    TrackSound(ResourceLocation context, Track track) {
        super(context, SoundCategory.MUSIC);
        volume = 1.0F; pitch = 1.0F; attenuationType = ISound.AttenuationType.NONE;
        sound = new Sound(track.id.replace(":sounds/", ":").replaceFirst("\\.ogg$", ""), 1.0F, 1.0F, 1, Sound.Type.FILE, true);
    }
    @Override public SoundEventAccessor createAccessor(SoundHandler handler) {
        SoundEventAccessor accessor = new SoundEventAccessor(getSoundLocation(), null);
        accessor.addSound(sound);
        return accessor;
    }
}
