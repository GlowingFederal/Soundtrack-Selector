package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.*;
import java.io.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.client.resources.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

public final class ClientAdapter extends CommonProxy {
    static final Logger LOG = LogManager.getLogger("Soundtrack Selector");
    static Library library;
    static final KeyBinding OPEN = new KeyBinding("key.soundtrackselector.open", Keyboard.KEY_M, "key.categories.soundtrackselector");
    static final Map<String, ResourceLocation> nativeTracks = new HashMap<String, ResourceLocation>();
    private static Collection<String> vanillaEvents() {
        List<String> events = new ArrayList<String>();
        for (MusicTicker.MusicType type : MusicTicker.MusicType.values()) events.add(type.getMusicLocation().getSoundName().toString());
        return events;
    }
    private final java.util.concurrent.atomic.AtomicBoolean reload = new java.util.concurrent.atomic.AtomicBoolean(true);
    private SelectorMusic music;
    @Override public void init() {
        Minecraft mc = Minecraft.getMinecraft();
        library = new Library(new File(mc.gameDir, "config"), new Library.Log() {
            public void info(String message) { LOG.info(message); }
            public void warning(String message, Throwable failure) { LOG.warn(message, failure); }
            public void debug(String message, Throwable failure) { LOG.debug(message, failure); }
        }, "1.12.2", vanillaEvents());
        ClientRegistry.registerKeyBinding(OPEN);
        ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(new IResourceManagerReloadListener() {
            @Override public void onResourceManagerReload(IResourceManager manager) { if (library != null) library.invalidateDiscovery(); reload.set(true); }
        });
        FMLCommonHandler.instance().bus().register(this);
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (music == null) {
            music = new SelectorMusic(mc);
            library.playback = music;
        }
        music.checkOwnership();
        if (reload.getAndSet(false) || library.takeRefreshRequest()) discover(mc);
        library.pollDiscovery();
        while (OPEN.isPressed()) if (mc.currentScreen == null) mc.displayGuiScreen(new SelectorScreen(null));
    }
    private void discover(final Minecraft mc) {
        long start = System.nanoTime();
        final ResourceFiles resources = snapshot(mc.getResourceManager());
        final Map<String, String> registeredFiles = new TreeMap<String, String>();
        final Map<String, String> registeredAliases = new HashMap<String, String>();
        SoundRegistry nativeRegistry = (SoundRegistry) FieldAccess.get(FieldAccess.unique(SoundHandler.class, SoundRegistry.class), mc.getSoundHandler());
        Map<?, ?> registered = (Map<?, ?>) FieldAccess.get(FieldAccess.unique(SoundRegistry.class, Map.class), nativeRegistry);
        java.lang.reflect.Field list = FieldAccess.unique(SoundEventAccessor.class, List.class);
        for (Object event : registered.values()) {
            SoundEventAccessor accessor = (SoundEventAccessor) event;
            int slot = 0;
            for (Object entry : (List<?>) FieldAccess.get(list, accessor)) {
                int index = slot++;
                if (entry instanceof Sound) {
                    Sound sound = (Sound) entry;
                    if (sound.getType() == Sound.Type.FILE && sound.getWeight() > 0) {
                        String resource = sound.getSoundAsOggLocation().toString();
                        if (!registeredFiles.containsKey(resource)) {
                        registeredFiles.put(resource, accessor.getLocation().toString());
                        registeredAliases.put(resource, accessor.getLocation().toString() + "#" + index);
                    }
                    }
                }
            }
        }
        library.submit(new Discovery.Scan() {
            public Library.Result run() throws Exception {
                try {
                    SoundDefinitions definitions = resources.definitions(library.log());
                    Map<String, String> files = definitions.files();
                    Map<String, String> aliases = definitions.aliasIds();
                    for (Map.Entry<String, String> entry : registeredFiles.entrySet()) if (!files.containsKey(entry.getKey())) {
                        files.put(entry.getKey(), entry.getValue()); aliases.put(entry.getKey(), registeredAliases.get(entry.getKey()));
                    }
                    return library.inspect(files, resources, aliases);
                } finally { resources.close(); }
            }
            public void published() {

            }
        });
        LOG.info("Soundtrack Selector client snapshot/submission: " + (System.nanoTime() - start) / 1000000.0 + " ms");
    }
    /** Copy pack descriptors only: the worker never calls a live Minecraft resource manager. */
    @SuppressWarnings("unchecked")
    private ResourceFiles snapshot(IResourceManager manager) {
        ResourceFiles result = new ResourceFiles();
        Map<?, ?> domains = (Map<?, ?>) FieldAccess.get(FieldAccess.unique(SimpleReloadableResourceManager.class, Map.class), manager);
        java.lang.reflect.Field list = FieldAccess.unique(FallbackResourceManager.class, List.class);
        for (Map.Entry<?, ?> domain : domains.entrySet()) for (IResourcePack pack : (List<IResourcePack>) FieldAccess.get(list, domain.getValue())) {
            // This native wrapper changes language paths only, not sounds.json or audio.
            while (pack instanceof LegacyV2Adapter)
                pack = (IResourcePack) FieldAccess.get(FieldAccess.unique(LegacyV2Adapter.class, IResourcePack.class), pack);
            if (pack instanceof DefaultResourcePack) {
                ResourceIndex index = (ResourceIndex) FieldAccess.get(FieldAccess.unique(DefaultResourcePack.class, ResourceIndex.class), pack);
                if (index instanceof ResourceIndexFolder)
                    result.addAssetDirectory(domain.getKey().toString(), (File) FieldAccess.get(FieldAccess.unique(ResourceIndexFolder.class, File.class), index), DefaultResourcePack.class);
                else result.add(domain.getKey().toString(), (Map<?, ?>) FieldAccess.get(FieldAccess.unique(ResourceIndex.class, Map.class), index), DefaultResourcePack.class);
            } else if (pack instanceof AbstractResourcePack) {
                result.add(domain.getKey().toString(), (File) FieldAccess.get(FieldAccess.unique(AbstractResourcePack.class, File.class), pack));
            } else {
                result.add(domain.getKey().toString(), Collections.emptyMap(), pack.getClass());
                LOG.warn("Soundtrack Selector can only inspect classpath audio for opaque resource pack " + pack.getClass().getName());
            }
        }
        return result;
    }
}
