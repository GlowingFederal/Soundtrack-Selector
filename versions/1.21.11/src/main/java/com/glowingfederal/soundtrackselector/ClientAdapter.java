package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.*;
import java.io.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.Identifier;
import net.minecraft.client.KeyMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ClientAdapter {
    static final Logger LOG = LogManager.getLogger("Soundtrack Selector");
    static Library library;
    static final KeyMapping OPEN = new KeyMapping("key.soundtrackselector.open", 77, KeyMapping.Category.MISC);
    private static volatile boolean reload = true;
    private static SelectorMusic music;
    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            library = new Library(new File(mc.gameDirectory, "config"), new Library.Log() {
                public void info(String message) { LOG.info(message); }
                public void warning(String message, Throwable failure) { LOG.warn(message, failure); }
                public void debug(String message, Throwable failure) { LOG.debug(message, failure); }
            });
            
        }));
        bus.addListener((net.neoforged.neoforge.client.event.AddClientReloadListenersEvent event) ->
                event.addListener(Identifier.fromNamespaceAndPath("soundtrackselector", "catalog"),
                        (net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> reload = true));

        bus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) -> event.register(OPEN));

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ClientAdapter::tick);
    }
    private static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        
        if (library == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (music == null) {
            java.lang.reflect.Field field = FieldAccess.unique(Minecraft.class, net.minecraft.client.sounds.MusicManager.class);
            Object previous = FieldAccess.get(field, mc);
            if (previous.getClass() != net.minecraft.client.sounds.MusicManager.class) {
                LOG.warn("Another mod replaced the music manager; Soundtrack Selector cannot install its music hook");
                library = null;
                return;
            }
            music = new SelectorMusic(mc);
            library.playback = music;
            FieldAccess.set(field, mc, music);
        }
        if (reload) { reload = false; music.reset(); discover(mc); }
        while (OPEN.consumeClick()) if (mc.screen == null) mc.setScreen(new SelectorScreen(null));
    }
    private static void discover(Minecraft mc) {
        final ResourceManager manager = mc.getResourceManager();
        SoundDefinitions definitions = new SoundDefinitions();
        for (String namespace : manager.getNamespaces()) {
            List<Resource> resources;
            resources = manager.getResourceStack(Identifier.fromNamespaceAndPath(namespace, "sounds.json"));
            for (Resource resource : resources) {
                try {
                    Reader reader = new InputStreamReader(resource.open(), "UTF-8");
                    try { definitions.add(namespace, reader); } finally { reader.close(); }
                } catch (IOException failure) { LOG.debug("Cannot read sounds.json in " + namespace, failure); }
                catch (RuntimeException failure) { LOG.debug("Invalid sounds.json in " + namespace, failure); }
            }
        }
        Map<String, String> files = definitions.files();
        java.lang.reflect.Field entries = FieldAccess.unique(net.minecraft.client.sounds.WeighedSoundEvents.class, List.class);
        for (Identifier event : mc.getSoundManager().getAvailableSounds()) {
            net.minecraft.client.sounds.WeighedSoundEvents accessor = mc.getSoundManager().getSoundEvent(event);
            if (accessor == null) continue;
            for (Object entry : (List<?>) FieldAccess.get(entries, accessor)) {
                if (entry instanceof net.minecraft.client.resources.sounds.Sound) {
                    net.minecraft.client.resources.sounds.Sound sound = (net.minecraft.client.resources.sounds.Sound) entry;
                    if (sound.getType() == net.minecraft.client.resources.sounds.Sound.Type.FILE && sound.getWeight() > 0) {
                        String resource = sound.getPath().toString();
                        if (!files.containsKey(resource)) files.put(resource, event.toString());
                    }
                }
            }
        }
        library.discover(files, id -> manager.getResource(Identifier.parse(id)).orElseThrow(() -> new FileNotFoundException(id)).open());
    }
}
