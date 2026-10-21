package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.client.resources.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.client.event.sound.*;
import org.lwjgl.input.Keyboard;

public final class ClientAdapter extends CommonProxy implements ITickHandler, Library.Playback {
    static Library library;
    private static Collection<String> vanillaEvents() {
        // This generation has a native background pool rather than sound events.
        return Collections.singletonList("minecraft:music");
    }
    private final java.util.concurrent.atomic.AtomicBoolean reload = new java.util.concurrent.atomic.AtomicBoolean(true);
    private Track selected;
    private boolean preview, paused;
    private int previewGrace;
    private Object world;
    private int savedDelay;
    private float customVolume;
    private static final String CUSTOM_SOURCE = "SoundtrackSelectorMusic";
    private final Map<String, URL> urls = new HashMap<String, URL>();
    private SoundManager manager;
    private volatile SoundManager pendingManager;
    private static final Field MUSIC_DELAY = musicDelayField();
    private static Field musicDelayField() {
        for (String name : new String[] {"ticksBeforeMusic", "field_77383_i"}) {
            try { Field field = SoundManager.class.getDeclaredField(name); field.setAccessible(true); return field; }
            catch (NoSuchFieldException ignored) {}
        }
        throw new IllegalStateException("Missing background music delay in Forge 1.6.4");
    }
    @Override public void init() {
        final Minecraft mc = Minecraft.getMinecraft();
        library = new Library(new File(mc.mcDataDir, "config"), new Library.Log() {
            public void info(String message) { FMLLog.log(Level.INFO, "%s", message); }
            public void warning(String message, Throwable failure) { FMLLog.log(Level.WARNING, failure, "%s", message); }
            public void debug(String message, Throwable failure) { FMLLog.log(Level.FINE, failure, "%s", message); }
        }, "1.6.4", vanillaEvents());
        MinecraftForge.EVENT_BUS.register(this);
        TickRegistry.registerTickHandler(this, Side.CLIENT);
        KeyBindingRegistry.registerKeyBinding(new KeyBindingRegistry.KeyHandler(
                new KeyBinding[] {new KeyBinding("key.soundtrackselector.open", Keyboard.KEY_M)}, new boolean[] {false}) {
            @Override public void keyDown(EnumSet<TickType> types, KeyBinding key, boolean end, boolean repeats) {
                if (end && mc.currentScreen == null) mc.displayGuiScreen(new SelectorScreen(null));
            }
            @Override public void keyUp(EnumSet<TickType> types, KeyBinding key, boolean end) {}
            @Override public EnumSet<TickType> ticks() { return EnumSet.of(TickType.CLIENT); }
            @Override public String getLabel() { return "Soundtrack Selector key"; }
        });
        ((ReloadableResourceManager) mc.getResourceManager()).registerReloadListener(new ResourceManagerReloadListener() {
            @Override public void onResourceManagerReload(ResourceManager resources) { library.invalidateDiscovery(); reload.set(true); }
        });
        manager = mc.sndManager;
        world = mc.theWorld;
        library.playback = this;
    }
    @ForgeSubscribe public void soundLoaded(SoundLoadEvent event) { pendingManager = event.manager; if (library != null) library.invalidateDiscovery(); reload.set(true); }
    @ForgeSubscribe public void background(PlayBackgroundMusicEvent event) {
        // Observation/indexing never changes a normal pool selection.
        if (preview) event.result = null;
    }
    @Override public void tickStart(EnumSet<TickType> types, Object... data) {}
    @Override public void tickEnd(EnumSet<TickType> types, Object... data) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (pendingManager != null) { manager = pendingManager; pendingManager = null; }
        if (world != mc.theWorld) { world = mc.theWorld; library.stop(); }
        if (reload.getAndSet(false) || library.takeRefreshRequest()) discover(mc);
        library.pollDiscovery();
        if (preview && manager != null && manager.sndSystem != null && customVolume != mc.gameSettings.musicVolume) {
            customVolume = mc.gameSettings.musicVolume;
            manager.sndSystem.setVolume(CUSTOM_SOURCE, customVolume);
        }
        if (previewGrace > 0) previewGrace--;
        if (preview && !paused && current() == null) library.previewFinished();
    }
    @Override public EnumSet<TickType> ticks() { return EnumSet.of(TickType.CLIENT); }
    @Override public String getLabel() { return "Soundtrack Selector"; }

    @Override public void play(Track track) {
        if (manager == null || manager.sndSystem == null || !urls.containsKey(track.id)) return;
        if (!preview) {
            savedDelay = ((Integer) FieldAccess.get(MUSIC_DELAY, manager)).intValue();
            manager.sndSystem.stop("BgMusic");
        }
        manager.sndSystem.stop(CUSTOM_SOURCE);
        preview = true; paused = false; selected = track; previewGrace = 40;
        manager.sndSystem.backgroundMusic(CUSTOM_SOURCE, urls.get(track.id), track.id, false);
        customVolume = Minecraft.getMinecraft().gameSettings.musicVolume;
        manager.sndSystem.setVolume(CUSTOM_SOURCE, customVolume);
        manager.sndSystem.play(CUSTOM_SOURCE);
        FieldAccess.set(MUSIC_DELAY, manager, Integer.MAX_VALUE);
        library.played(track);
    }
    @Override public void stop() {
        if (preview && manager != null) {
            if (manager.sndSystem != null) manager.sndSystem.stop(CUSTOM_SOURCE);
            // Restore the native countdown captured at acquisition, never force an early song.
            FieldAccess.set(MUSIC_DELAY, manager, savedDelay);
        }
        selected = null; preview = false; paused = false; previewGrace = 0;
        library.relinquished();
    }
    @Override public Track current() {
        return selected != null && manager != null && manager.sndSystem != null
                && (previewGrace > 0 || paused || manager.sndSystem.playing(CUSTOM_SOURCE)) ? selected : null;
    }
    @Override public void togglePause() {
        if (current() == null) return;
        if (paused) manager.sndSystem.play(CUSTOM_SOURCE); else manager.sndSystem.pause(CUSTOM_SOURCE);
        paused = !paused;
    }
    @Override public boolean paused() { return paused; }
    @Override public void release() { library.stop(); }

    @SuppressWarnings("unchecked")
    private void discover(final Minecraft mc) {
        long start = System.nanoTime();
        final ResourceFiles resources = new ResourceFiles();
        Map<?, ?> domains = (Map<?, ?>) FieldAccess.get(FieldAccess.unique(SimpleReloadableResourceManager.class, Map.class), mc.getResourceManager());
        Field packList = FieldAccess.unique(FallbackResourceManager.class, List.class);
        for (Map.Entry<?, ?> domain : domains.entrySet()) for (ResourcePack pack : (List<ResourcePack>) FieldAccess.get(packList, domain.getValue())) {
            if (pack instanceof DefaultResourcePack) {
                resources.add(domain.getKey().toString(), (Map<?, ?>) FieldAccess.get(FieldAccess.unique(DefaultResourcePack.class, Map.class), pack), DefaultResourcePack.class);
            } else if (pack instanceof AbstractResourcePack) {
                resources.add(domain.getKey().toString(), (File) FieldAccess.get(FieldAccess.unique(AbstractResourcePack.class, File.class), pack));
            }
        }
        final Map<String, String> registered = new TreeMap<String, String>();
        final Map<String, URL> registeredUrls = new HashMap<String, URL>();
        if (manager != null) for (SoundPool pool : new SoundPool[] {manager.soundPoolSounds, manager.soundPoolStreaming, manager.soundPoolMusic}) {
            Map<?, ?> entries = (Map<?, ?>) FieldAccess.get(FieldAccess.unique(SoundPool.class, Map.class), pool);
            for (Object group : entries.values()) for (Object entryObject : (List<?>) group) {
                SoundPoolEntry entry = (SoundPoolEntry) entryObject;
                URL url = entry.getSoundUrl();
                // Native pool URLs are file/jar URLs. Never invoke a custom game URL handler on the worker.
                if (!url.getProtocol().equals("file") && !url.getProtocol().equals("jar")) continue;
                String id = url.getPath();
                if (id.indexOf(':') < 0) id = "minecraft:" + id;
                if (!registered.containsKey(id)) { registered.put(id, entry.getSoundName()); registeredUrls.put(id, url); }
            }
        }
        library.submit(new Discovery.Scan() {
            private final Map<String, URL> discoveredUrls = new HashMap<String, URL>();
            public Library.Result run() throws Exception {
                try {
                    Map<String, String> files = resources.enumerate(library.log());
                    for (Map.Entry<String, String> entry : registered.entrySet()) {
                        if (!files.containsKey(entry.getKey() + ".ogg") && !files.containsKey(entry.getKey())) {
                            files.put(entry.getKey(), entry.getValue()); discoveredUrls.put(entry.getKey(), registeredUrls.get(entry.getKey()));
                        }
                    }
                    for (String id : files.keySet()) if (!discoveredUrls.containsKey(id)) discoveredUrls.put(id, resources.url(id));
                    return library.inspect(files, new Library.Resources() {
                        public InputStream open(String id) throws IOException {
                            URL registered = registeredUrls.get(id);
                            return registered != null ? registered.openStream() : resources.open(id);
                        }
                    });
                } finally { resources.close(); }
            }
            public void published() { urls.clear(); urls.putAll(discoveredUrls); }
        });
        FMLLog.log(Level.INFO, "Soundtrack Selector client snapshot/submission: %s ms", (System.nanoTime() - start) / 1000000.0);
    }
}
