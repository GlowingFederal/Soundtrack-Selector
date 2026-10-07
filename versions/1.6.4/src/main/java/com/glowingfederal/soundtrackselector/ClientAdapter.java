package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.*;
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
    private volatile boolean reload = true;
    private Track selected;
    private boolean preview, paused;
    private int previewGrace;
    private Object world;
    private final Random random = new Random();
    private final Map<String, URL> urls = new HashMap<String, URL>();
    private SoundManager manager;
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
        });
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
            @Override public void onResourceManagerReload(ResourceManager resources) { reload = true; }
        });
        manager = mc.sndManager;
        world = mc.theWorld;
        library.playback = this;
    }
    @ForgeSubscribe public void soundLoaded(SoundLoadEvent event) { manager = event.manager; reload = true; }
    @ForgeSubscribe public void background(PlayBackgroundMusicEvent event) {
        if (preview || paused) { event.result = null; FieldAccess.set(MUSIC_DELAY, manager, 12000); return; }
        Track track = library.catalog.next(random);
        selected = track;
        library.played(track);
        if (track == null) {
            event.result = null;
            // Vanilla only resets its delay after a non-null selection. Avoid retrying every tick.
            FieldAccess.set(MUSIC_DELAY, manager, 12000);
        } else event.result = new SoundPoolEntry(track.id, urls.get(track.id));
    }
    @Override public void tickStart(EnumSet<TickType> types, Object... data) {}
    @Override public void tickEnd(EnumSet<TickType> types, Object... data) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (world != mc.theWorld || reload || (selected != null && !preview && !library.catalog.preferences.enabled(selected))) {
            world = mc.theWorld;
            if (manager != null && manager.sndSystem != null) manager.sndSystem.stop("BgMusic");
            selected = null; preview = false; paused = false; previewGrace = 0;
        }
        if (reload) { reload = false; discover(mc); }
        if (previewGrace > 0) previewGrace--;
        if (preview && !paused && current() == null) library.previewFinished();
    }
    @Override public EnumSet<TickType> ticks() { return EnumSet.of(TickType.CLIENT); }
    @Override public String getLabel() { return "Soundtrack Selector"; }

    @Override public void play(Track track) {
        if (manager == null || manager.sndSystem == null || !urls.containsKey(track.id)) return;
        stop(); preview = true; selected = track; previewGrace = 40;
        manager.sndSystem.backgroundMusic("BgMusic", urls.get(track.id), track.id, false);
        manager.sndSystem.setVolume("BgMusic", Minecraft.getMinecraft().gameSettings.musicVolume);
        manager.sndSystem.play("BgMusic");
        FieldAccess.set(MUSIC_DELAY, manager, 12000);
        library.played(track);
    }
    @Override public void stop() {
        if (manager != null && manager.sndSystem != null) manager.sndSystem.stop("BgMusic");
        selected = null; preview = false; paused = false; previewGrace = 0;
        if (manager != null) FieldAccess.set(MUSIC_DELAY, manager, 100);
    }
    @Override public Track current() {
        return selected != null && manager != null && manager.sndSystem != null
                && (previewGrace > 0 || paused || manager.sndSystem.playing("BgMusic")) ? selected : null;
    }
    @Override public void togglePause() {
        if (current() == null) return;
        if (paused) manager.sndSystem.play("BgMusic"); else manager.sndSystem.pause("BgMusic");
        paused = !paused;
    }
    @Override public boolean paused() { return paused; }
    @Override public void release() { if (preview) stop(); }

    @SuppressWarnings("unchecked")
    private void discover(final Minecraft mc) {
        final ResourceManager resources = mc.getResourceManager();
        Map<String, String> files = new TreeMap<String, String>();
        urls.clear();
        Map<?, ?> domains = (Map<?, ?>) FieldAccess.get(FieldAccess.unique(SimpleReloadableResourceManager.class, Map.class), resources);
        Set<ResourcePack> packs = new HashSet<ResourcePack>();
        Field packList = FieldAccess.unique(FallbackResourceManager.class, List.class);
        for (Object domain : domains.values()) packs.addAll((List<ResourcePack>) FieldAccess.get(packList, domain));
        for (ResourcePack pack : packs) {
            try {
                if (pack instanceof DefaultResourcePack) {
                    Map<?, ?> defaults = (Map<?, ?>) FieldAccess.get(FieldAccess.unique(DefaultResourcePack.class, Map.class), pack);
                    for (Object id : defaults.keySet()) add(files, id.toString());
                } else if (pack instanceof AbstractResourcePack) {
                    File source = (File) FieldAccess.get(FieldAccess.unique(AbstractResourcePack.class, File.class), pack);
                    if (source.isDirectory()) collectFolder(source, source, files);
                    else {
                        ZipFile archive = new ZipFile(source);
                        try {
                            Enumeration<? extends ZipEntry> entries = archive.entries();
                            while (entries.hasMoreElements()) addAsset(files, entries.nextElement().getName());
                        } finally { archive.close(); }
                    }
                }
            } catch (IOException failure) { FMLLog.log(Level.FINE, failure, "Cannot enumerate audio in %s", pack.getPackName()); }
        }
        // Also include programmatic pool registrations that expose a concrete resource URL.
        for (SoundPool pool : new SoundPool[] {manager.soundPoolSounds, manager.soundPoolStreaming, manager.soundPoolMusic}) {
            Map<?, ?> entries = (Map<?, ?>) FieldAccess.get(FieldAccess.unique(SoundPool.class, Map.class), pool);
            for (Object group : entries.values()) for (Object entryObject : (List<?>) group) {
                SoundPoolEntry entry = (SoundPoolEntry) entryObject;
                URL url = entry.getSoundUrl();
                String id = url.getPath();
                if (id.indexOf(':') < 0) id = "minecraft:" + id;
                if (files.containsKey(id + ".ogg")) continue;
                if (!files.containsKey(id)) { files.put(id, entry.getSoundName()); urls.put(id, url); }
            }
        }
        for (final String id : files.keySet()) if (!urls.containsKey(id)) {
            try {
                urls.put(id, new URL(null, "soundtrackresource:" + id, new URLStreamHandler() {
                    @Override protected URLConnection openConnection(URL url) {
                        return new URLConnection(url) {
                            @Override public void connect() {}
                            @Override public InputStream getInputStream() throws IOException {
                                return mc.getResourceManager().getResource(new ResourceLocation(id)).getInputStream();
                            }
                        };
                    }
                }));
            } catch (MalformedURLException failure) { throw new IllegalStateException(failure); }
        }
        library.discover(files, new Library.Resources() {
            public InputStream open(String id) throws IOException { return urls.get(id).openStream(); }
        });
    }
    private void collectFolder(File root, File directory, Map<String, String> files) {
        File[] children = directory.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) collectFolder(root, child, files);
            else addAsset(files, root.toURI().relativize(child.toURI()).getPath());
        }
    }
    private void addAsset(Map<String, String> files, String path) {
        if (!path.startsWith("assets/")) return;
        int slash = path.indexOf('/', 7);
        if (slash > 7) add(files, path.substring(7, slash) + ":" + path.substring(slash + 1));
    }
    private void add(Map<String, String> files, String id) {
        if (id.endsWith(".ogg")) files.put(id, id);
    }
}
