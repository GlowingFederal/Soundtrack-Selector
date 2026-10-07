package com.glowingfederal.soundtrackselector;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
@Mod(value = "soundtrackselector", dist = Dist.CLIENT)
public final class SoundtrackSelector {
    public SoundtrackSelector(net.neoforged.bus.api.IEventBus bus) {
        ClientAdapter.register(bus);
    }
}
