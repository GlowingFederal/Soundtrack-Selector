package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.*;
import java.io.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.KeyMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ClientAdapter {
    static final Logger LOG = LogManager.getLogger("Soundtrack Selector");
    static Library library;
    static final KeyMapping OPEN = new KeyMapping("key.soundtrackselector.open", 77, "key.categories.soundtrackselector");
    private static Collection<String> vanillaEvents() {
        List<String> events = new ArrayList<String>();
        // Native constants identify categories; the game still resolves biome/dimension contexts at runtime.
        for (java.lang.reflect.Field field : net.minecraft.sounds.Musics.class.getDeclaredFields()) {
            if (field.getType() != net.minecraft.sounds.Music.class || !java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            net.minecraft.sounds.Music type = (net.minecraft.sounds.Music) FieldAccess.get(field, null);
            events.add(type.getEvent().getLocation().toString());
        }
        return events;
    }
    private static final java.util.concurrent.atomic.AtomicBoolean reload = new java.util.concurrent.atomic.AtomicBoolean(true);
    private static SelectorMusic music;
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            library = new Library(new File(mc.gameDirectory, "config"), new Library.Log() {
                public void info(String message) { LOG.info(message); }
                public void warning(String message, Throwable failure) { LOG.warn(message, failure); }
                public void debug(String message, Throwable failure) { LOG.debug(message, failure); }
            }, "1.18.2", vanillaEvents());
            net.minecraftforge.client.ClientRegistry.registerKeyBinding(OPEN);
        }));
        bus.addListener((net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> { if (library != null) library.invalidateDiscovery(); reload.set(true); }));

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
        if (reload.getAndSet(false) || library.takeRefreshRequest()) discover(mc);
        library.pollDiscovery();
        while (OPEN.consumeClick()) if (mc.screen == null) mc.setScreen(new SelectorScreen(null));
    }
    private static void discover(final Minecraft mc) {
        long start = System.nanoTime();
        final List<net.minecraft.server.packs.repository.Pack> packs = new ArrayList<net.minecraft.server.packs.repository.Pack>(mc.getResourcePackRepository().getSelectedPacks());
        final Map<String, String> registeredFiles = new TreeMap<String, String>();
        final Map<String, String> registeredAliases = new HashMap<String, String>();
        java.lang.reflect.Field entries = FieldAccess.unique(net.minecraft.client.sounds.WeighedSoundEvents.class, List.class);
        for (ResourceLocation event : mc.getSoundManager().getAvailableSounds()) {
            net.minecraft.client.sounds.WeighedSoundEvents accessor = mc.getSoundManager().getSoundEvent(event);
            if (accessor == null) continue;
            int slot = 0;
            for (Object entry : (List<?>) FieldAccess.get(entries, accessor)) {
                int index = slot++;
                if (entry instanceof net.minecraft.client.resources.sounds.Sound) {
                    net.minecraft.client.resources.sounds.Sound sound = (net.minecraft.client.resources.sounds.Sound) entry;
                    if (sound.getType() == net.minecraft.client.resources.sounds.Sound.Type.FILE && sound.getWeight() > 0) {
                        String resource = sound.getPath().toString();
                        if (!registeredFiles.containsKey(resource)) {
                            registeredFiles.put(resource, event.toString());
                            registeredAliases.put(resource, event.toString() + "#" + index);
                        }
                    }
                }
            }
        }
        library.submit(new Discovery.Scan() {
            public Library.Result run() throws Exception {
                List<net.minecraft.server.packs.PackResources> opened = new ArrayList<net.minecraft.server.packs.PackResources>();
                net.minecraft.server.packs.resources.MultiPackResourceManager manager = null;
                try {
                    for (net.minecraft.server.packs.repository.Pack pack : packs) { Discovery.checkCancelled(); opened.add(pack.open()); }
                    manager = new net.minecraft.server.packs.resources.MultiPackResourceManager(net.minecraft.server.packs.PackType.CLIENT_RESOURCES, opened);
                    final ResourceManager scanResources = manager;
                    SoundDefinitions definitions = new SoundDefinitions();
                    for (String namespace : manager.getNamespaces()) {
                        Discovery.checkCancelled();
                        List<Resource> resources;
                        try { resources = manager.getResources(new ResourceLocation(namespace, "sounds.json")); } catch (IOException missing) { continue; }
                        try {
                            for (Resource resource : resources) {
                                Discovery.checkCancelled();
                                try {
                                    Reader reader = new InputStreamReader(new BufferedInputStream(resource.getInputStream()), "UTF-8");
                                    try { definitions.add(namespace, reader); } finally { reader.close(); }
                                } catch (IOException failure) { LOG.debug("Cannot read sounds.json in " + namespace, failure); }
                                catch (RuntimeException failure) { LOG.debug("Invalid sounds.json in " + namespace, failure); }
                            }
                        } finally {
                            for (Resource resource : resources) try { resource.close(); }
                            catch (IOException failure) { LOG.debug("Cannot close sound definition resource", failure); }
                        }
                    }
                    Map<String, String> files = definitions.files();
                    Map<String, String> aliases = definitions.aliasIds();
                    for (Map.Entry<String, String> entry : registeredFiles.entrySet()) if (!files.containsKey(entry.getKey())) {
                        files.put(entry.getKey(), entry.getValue()); aliases.put(entry.getKey(), registeredAliases.get(entry.getKey()));
                    }
                    return library.inspect(files, id -> stream(scanResources.getResource(new ResourceLocation(id))), aliases);
                } finally { if (manager != null) manager.close(); else for (net.minecraft.server.packs.PackResources pack : opened) pack.close(); }
            }
            public void published() {

            }
        });
        LOG.info("Soundtrack Selector client snapshot/submission: " + (System.nanoTime() - start) / 1000000.0 + " ms");
    }
    private static InputStream stream(final Resource resource) {
        return new FilterInputStream(resource.getInputStream()) {
            @Override public void close() throws IOException { resource.close(); }
        };
    }

}
