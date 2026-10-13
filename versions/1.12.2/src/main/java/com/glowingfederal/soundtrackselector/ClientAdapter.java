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
    private volatile boolean reload = true;
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
            @Override public void onResourceManagerReload(IResourceManager manager) { reload = true; }
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
        if (reload) { reload = false; library.stop(); discover(mc); }
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
        Map<String, String> aliases = definitions.aliasIds();
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
                        if (!files.containsKey(resource)) {
                        files.put(resource, accessor.getLocation().toString());
                        aliases.put(resource, accessor.getLocation().toString() + "#" + index);
                    }
                    }
                }
            }
        }
        library.discover(files, new Library.Resources() {
            public InputStream open(String id) throws IOException { return manager.getResource(new ResourceLocation(id)).getInputStream(); }
        }, aliases);
    }
}
