package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.*;
import java.io.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.settings.KeyBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ClientAdapter {
    static final Logger LOG = LogManager.getLogger("Soundtrack Selector");
    static Library library;
    static final KeyBinding OPEN = new KeyBinding("key.soundtrackselector.open", 77, "key.categories.soundtrackselector");
    private static Collection<String> vanillaEvents() {
        List<String> events = new ArrayList<String>();
        // Native constants identify categories; the game still resolves biome/dimension contexts at runtime.
        for (java.lang.reflect.Field field : net.minecraft.client.audio.BackgroundMusicTracks.class.getDeclaredFields()) {
            if (field.getType() != net.minecraft.client.audio.BackgroundMusicSelector.class || !java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            net.minecraft.client.audio.BackgroundMusicSelector type = (net.minecraft.client.audio.BackgroundMusicSelector) FieldAccess.get(field, null);
            events.add(type.getEvent().getLocation().toString());
        }
        return events;
    }
    private static volatile boolean reload = true;
    private static SelectorMusic music;
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            library = new Library(new File(mc.gameDirectory, "config"), new Library.Log() {
                public void info(String message) { LOG.info(message); }
                public void warning(String message, Throwable failure) { LOG.warn(message, failure); }
                public void debug(String message, Throwable failure) { LOG.debug(message, failure); }
            }, "1.16.5", vanillaEvents());
            net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(OPEN);
            ((net.minecraft.resources.IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(
                    (net.minecraft.resources.IResourceManagerReloadListener) manager -> reload = true);

        }));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ClientAdapter::tick);
    }
    private static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (library == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (music == null) {
            music = new SelectorMusic(mc);
            library.playback = music;
        }
        music.checkOwnership();
        if (reload) { reload = false; library.stop(); discover(mc); }
        while (OPEN.consumeClick()) if (mc.screen == null) mc.setScreen(new SelectorScreen(null));
    }
    private static void discover(Minecraft mc) {
        final IResourceManager manager = mc.getResourceManager();
        SoundDefinitions definitions = new SoundDefinitions();
        for (String namespace : manager.getNamespaces()) {
            List<IResource> resources;
            try { resources = manager.getResources(new ResourceLocation(namespace, "sounds.json")); } catch (IOException missing) { continue; }
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
        java.lang.reflect.Field entries = FieldAccess.unique(net.minecraft.client.audio.SoundEventAccessor.class, List.class);
        for (ResourceLocation event : mc.getSoundManager().getAvailableSounds()) {
            net.minecraft.client.audio.SoundEventAccessor accessor = mc.getSoundManager().getSoundEvent(event);
            if (accessor == null) continue;
            int slot = 0;
            for (Object entry : (List<?>) FieldAccess.get(entries, accessor)) {
                int index = slot++;
                if (entry instanceof net.minecraft.client.audio.Sound) {
                    net.minecraft.client.audio.Sound sound = (net.minecraft.client.audio.Sound) entry;
                    if (sound.getType() == net.minecraft.client.audio.Sound.Type.FILE && sound.getWeight() > 0) {
                        String resource = sound.getPath().toString();
                        if (!files.containsKey(resource)) {
                            files.put(resource, event.toString());
                            aliases.put(resource, event.toString() + "#" + index);
                        }
                    }
                }
            }
        }
        library.discover(files, id -> manager.getResource(new ResourceLocation(id)).getInputStream(), aliases);
    }
}
