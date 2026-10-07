package com.glowingfederal.soundtrackselector;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
@Mod(modid = "soundtrackselector", name = "Soundtrack Selector", version = BuildVersion.VERSION)
@NetworkMod(clientSideRequired = false, serverSideRequired = false)
public final class SoundtrackSelector {
    @SidedProxy(clientSide = "com.glowingfederal.soundtrackselector.ClientAdapter", serverSide = "com.glowingfederal.soundtrackselector.CommonProxy")
    public static CommonProxy proxy;
    @Mod.EventHandler public void init(FMLInitializationEvent event) { proxy.init(); }
}
