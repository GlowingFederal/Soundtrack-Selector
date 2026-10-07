package com.glowingfederal.soundtrackselector;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
@Mod("soundtrackselector")
public final class SoundtrackSelector {
    public SoundtrackSelector() {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientAdapter.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
    }
}
