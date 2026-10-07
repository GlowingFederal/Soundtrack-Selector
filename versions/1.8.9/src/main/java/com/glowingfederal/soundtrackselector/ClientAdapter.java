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
    private volatile boolean reload = true;
    private SelectorMusic music;
    @Override public void init() {
        Minecraft mc = Minecraft.getMinecraft();
        library = new Library(new File(mc.mcDataDir, "config"), new Library.Log() {
            public void info(String message) { LOG.info(message); }
            public void warning(String message, Throwable failure) { LOG.warn(message, failure); }
            public void debug(String message, Throwable failure) { LOG.debug(message, failure); }
        });
        ClientRegistry.registerKeyBinding(OPEN);
        ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(new IResourceManagerReloadListener() {
            @Override public void onResourceManagerReload(IResourceManager manager) { reload = true; }
        });
        FMLCommonHandler.instance().bus().register(this);
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (music == null) {
            java.lang.reflect.Field field = FieldAccess.unique(Minecraft.class, MusicTicker.class);
            Object previous = FieldAccess.get(field, mc);
            if (previous.getClass() != MusicTicker.class) {
                LOG.warn("Another mod replaced the music ticker; Soundtrack Selector cannot install its music hook");
                FMLCommonHandler.instance().bus().unregister(this);
                return;
            }
            music = new SelectorMusic(mc);
            library.playback = music;
            FieldAccess.set(field, mc, music);
        }
        if (reload) { reload = false; music.reset(); discover(mc); }
        while (OPEN.isPressed()) if (mc.currentScreen == null) mc.displayGuiScreen(new SelectorScreen(null));
    }
    private void discover(final Minecraft mc) {
        final IResourceManager manager = mc.getResourceManager();
        SoundDefinitions definitions = new SoundDefinitions();
        for (Object domain : manager.getResourceDomains()) {
            String namespace = domain.toString();
            List<IResource> resources;
            try { resources = manager.getAllResources(new ResourceLocation(namespace, "sounds.json")); }
            catch (IOException missing) { continue; }
            for (IResource resource : resources) {
                try {
                    Reader reader = new InputStreamReader(resource.getInputStream(), "UTF-8");
                    try { definitions.add(namespace, reader); } finally { reader.close(); }
                } catch (IOException failure) { LOG.debug("Cannot read sounds.json in " + namespace, failure); }
                catch (RuntimeException failure) { LOG.debug("Invalid sounds.json in " + namespace, failure); }
            }
        }
        Map<String, String> files = definitions.files();
        SoundRegistry nativeRegistry = (SoundRegistry) FieldAccess.get(FieldAccess.unique(SoundHandler.class, SoundRegistry.class), mc.getSoundHandler());
        Map<?, ?> registered = (Map<?, ?>) FieldAccess.get(FieldAccess.unique(SoundRegistry.class, Map.class), nativeRegistry);
        java.lang.reflect.Field list = FieldAccess.unique(SoundEventAccessorComposite.class, List.class);
        java.lang.reflect.Field file = FieldAccess.unique(SoundEventAccessor.class, SoundPoolEntry.class);
        for (Object event : registered.values()) {
            SoundEventAccessorComposite accessor = (SoundEventAccessorComposite) event;
            if (accessor.getSoundEventLocation().getResourceDomain().equals("soundtrackselector")) continue;
            for (Object entry : (List<?>) FieldAccess.get(list, accessor)) {
                if (entry instanceof SoundEventAccessor) {
                    SoundPoolEntry sound = (SoundPoolEntry) FieldAccess.get(file, entry);
                    String resource = sound.getSoundPoolEntryLocation().toString();
                    if (!files.containsKey(resource)) files.put(resource, accessor.getSoundEventLocation().toString());
                }
            }
        }
        library.discover(files, new Library.Resources() {
            public InputStream open(String id) throws IOException { return manager.getResource(new ResourceLocation(id)).getInputStream(); }
        });
        SoundRegistry registry = (SoundRegistry) FieldAccess.get(FieldAccess.unique(SoundHandler.class, SoundRegistry.class), mc.getSoundHandler());
        nativeTracks.clear();
        int index = 0;
        for (Track track : library.catalog.tracks()) {
            ResourceLocation id = new ResourceLocation("soundtrackselector", "track/" + index++);
            final SoundPoolEntry entry = new SoundPoolEntry(new ResourceLocation(track.id), 1.0, 1.0, true);
            SoundEventAccessorComposite accessor = new SoundEventAccessorComposite(id, 1.0, 1.0, SoundCategory.MUSIC) {
                @Override public int getWeight() { return 1; }
                @Override public SoundPoolEntry cloneEntry() { return new SoundPoolEntry(entry); }
            };
            registry.registerSound(accessor);
            nativeTracks.put(track.id, id);
        }

    }
}
