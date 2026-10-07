package com.glowingfederal.soundtrackselector;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
@Mod(modid = "soundtrackselector", name = "Soundtrack Selector", version = BuildVersion.VERSION,
        acceptableRemoteVersions = "*")
public final class SoundtrackSelector {
    @SidedProxy(clientSide = "com.glowingfederal.soundtrackselector.ClientAdapter", serverSide = "com.glowingfederal.soundtrackselector.CommonProxy")
    public static CommonProxy proxy;
    @Mod.EventHandler public void init(FMLInitializationEvent event) { proxy.init(); }
}
