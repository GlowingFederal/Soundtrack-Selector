package com.glowingfederal.soundtrackselector;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
@Mod(modid = "soundtrackselector", name = "Soundtrack Selector", version = BuildVersion.VERSION,
        clientSideOnly = true, acceptableRemoteVersions = "*")
public final class SoundtrackSelector {
    @SidedProxy(clientSide = "com.glowingfederal.soundtrackselector.ClientAdapter", serverSide = "com.glowingfederal.soundtrackselector.CommonProxy")
    public static CommonProxy proxy;
    @Mod.EventHandler public void init(FMLInitializationEvent event) { proxy.init(); }
}
