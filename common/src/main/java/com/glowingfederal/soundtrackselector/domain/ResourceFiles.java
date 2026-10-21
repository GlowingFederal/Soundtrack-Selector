package com.glowingfederal.soundtrackselector.domain;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.*;

/** Detached legacy pack descriptors. All filesystem work and archive ownership is worker-local. */
public final class ResourceFiles implements Library.Resources, Closeable {
    private static final class Pack {
        File root;
        File assets;
        Map<String, File> indexed;
        Class<?> anchor;
        ZipFile zip;
        boolean opened;
        InputStream open(String id) throws IOException {
            String path = "assets/" + id.replace(':', '/');
            if (root != null) {
                if (root.isDirectory()) {
                    Path base = root.toPath().toAbsolutePath().normalize();
                    Path file = base.resolve(path).normalize();
                    if (!file.startsWith(base) || !Files.isRegularFile(file)) return null;
                    return Files.newInputStream(file);
                }
                if (!opened) { opened = true; zip = new ZipFile(root); }
                if (zip == null) return null;
                ZipEntry entry = zip.getEntry(path);
                return entry == null ? null : zip.getInputStream(entry);
            }
            // The default pack gives classpath resources precedence over its asset index.
            InputStream stream = anchor.getResourceAsStream("/" + path);
            if (stream != null) return stream;
            File file = indexed.get(id);
            if (file == null && assets != null) {
                Path base = assets.toPath().toAbsolutePath().normalize();
                Path resolved = base.resolve(id.replace(':', '/')).normalize();
                if (!resolved.startsWith(base)) return null;
                file = resolved.toFile();
            }
            return file == null ? null : new FileInputStream(file);
        }
    }
    private final Map<String, List<Pack>> domains = new LinkedHashMap<String, List<Pack>>();
    private final List<Pack> packs = new ArrayList<Pack>();
    public void add(String namespace, File root) {
        Pack pack = new Pack(); pack.root = root; add(namespace, pack);
    }
    public void add(String namespace, Map<?, ?> index, Class<?> anchor) {
        Pack pack = new Pack(); pack.anchor = anchor;
        pack.indexed = new HashMap<String, File>();
        for (Map.Entry<?, ?> entry : index.entrySet()) if (entry.getValue() instanceof File)
            pack.indexed.put(entry.getKey().toString(), (File) entry.getValue());
        add(namespace, pack);
    }
    public void addAssetDirectory(String namespace, File assets, Class<?> anchor) {
        Pack pack = new Pack(); pack.assets = assets; pack.anchor = anchor;
        pack.indexed = Collections.emptyMap();
        add(namespace, pack);
    }
    private void add(String namespace, Pack pack) {
        List<Pack> stack = domains.get(namespace);
        if (stack == null) { stack = new ArrayList<Pack>(); domains.put(namespace, stack); }
        stack.add(pack); packs.add(pack);
    }
    @Override public InputStream open(String id) throws IOException {
        Discovery.checkCancelled();
        List<Pack> stack = domains.get(id.substring(0, id.indexOf(':')));
        if (stack != null) for (int i = stack.size() - 1; i >= 0; i--) {
            InputStream input = stack.get(i).open(id);
            if (input != null) return input;
        }
        throw new FileNotFoundException(id);
    }
    public SoundDefinitions definitions(Library.Log log) throws IOException {
        SoundDefinitions definitions = new SoundDefinitions();
        for (Map.Entry<String, List<Pack>> domain : domains.entrySet()) for (Pack pack : domain.getValue()) {
            Discovery.checkCancelled();
            try {
                InputStream input = pack.open(domain.getKey() + ":sounds.json");
                if (input == null) continue;
                Reader reader = new InputStreamReader(new BufferedInputStream(input), "UTF-8");
                try { definitions.add(domain.getKey(), reader); } finally { reader.close(); }
            } catch (IOException failure) { Discovery.checkCancelled(); log.debug("Cannot read sounds.json in " + domain.getKey(), failure); }
            catch (RuntimeException failure) { log.debug("Invalid sounds.json in " + domain.getKey(), failure); }
        }
        return definitions;
    }
    /** 1.6.4 has no definitions: walk files iteratively without following directory links. */
    public Map<String, String> enumerate(final Library.Log log) throws IOException {
        final Map<String, String> files = new TreeMap<String, String>();
        Set<File> visited = new HashSet<File>();
        for (Pack pack : packs) {
            Discovery.checkCancelled();
            if (pack.root == null) {
                for (String id : pack.indexed.keySet()) if (id.endsWith(".ogg")) files.put(id, id);
            } else if (visited.add(pack.root)) {
                try {
                    if (pack.root.isDirectory()) {
                        final Path root = pack.root.toPath();
                        Path assets = root.resolve("assets");
                        if (!Files.isDirectory(assets)) continue;
                        Files.walkFileTree(assets, new SimpleFileVisitor<Path>() {
                            @Override public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
                                Discovery.checkCancelled(); return FileVisitResult.CONTINUE;
                            }
                            @Override public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                                Discovery.checkCancelled(); addAsset(files, root.relativize(path).toString().replace('\\', '/'));
                                return FileVisitResult.CONTINUE;
                            }
                            @Override public FileVisitResult visitFileFailed(Path path, IOException failure) {
                                log.debug("Cannot enumerate " + path, failure); return FileVisitResult.CONTINUE;
                            }
                        });
                    } else {
                        if (!pack.opened) { pack.opened = true; pack.zip = new ZipFile(pack.root); }
                        if (pack.zip == null) continue;
                        Enumeration<? extends ZipEntry> entries = pack.zip.entries();
                        while (entries.hasMoreElements()) { Discovery.checkCancelled(); addAsset(files, entries.nextElement().getName()); }
                    }
                } catch (IOException failure) { Discovery.checkCancelled(); log.debug("Cannot enumerate " + pack.root, failure); }
                catch (SecurityException failure) { log.debug("Cannot enumerate " + pack.root, failure); }
            }
        }
        return files;
    }
    private static void addAsset(Map<String, String> files, String path) {
        if (!path.startsWith("assets/") || !path.endsWith(".ogg")) return;
        int slash = path.indexOf('/', 7);
        if (slash > 7) { String id = path.substring(7, slash) + ":" + path.substring(slash + 1); files.put(id, id); }
    }
    /** Used by the old sound system on its audio thread, with no Minecraft reference. */
    public URL url(final String id) throws java.net.MalformedURLException {
        return new URL(null, "soundtrackresource:" + id, new java.net.URLStreamHandler() {
            @Override protected java.net.URLConnection openConnection(URL url) {
                return new java.net.URLConnection(url) {
                    @Override public void connect() {}
                    @Override public InputStream getInputStream() throws IOException {
                        // Playback owns its stream/ZIP, independent of the completed discovery scan.
                        final ResourceFiles copy = copy();
                        try {
                            return new FilterInputStream(copy.open(id)) {
                                @Override public void close() throws IOException { try { super.close(); } finally { copy.close(); } }
                            };
                        } catch (IOException failure) { copy.close(); throw failure; }
                    }
                };
            }
        });
    }
    private ResourceFiles copy() {
        ResourceFiles result = new ResourceFiles();
        for (Map.Entry<String, List<Pack>> domain : domains.entrySet()) for (Pack pack : domain.getValue()) {
            if (pack.root != null) result.add(domain.getKey(), pack.root);
            else if (pack.assets != null) result.addAssetDirectory(domain.getKey(), pack.assets, pack.anchor);
            else result.add(domain.getKey(), pack.indexed, pack.anchor);
        }
        return result;
    }
    @Override public void close() {
        for (Pack pack : packs) if (pack.zip != null) {
            try { pack.zip.close(); } catch (IOException ignored) { }
            pack.zip = null;
        }
    }
}
