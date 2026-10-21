package com.glowingfederal.soundtrackselector.domain;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.*;

/** Presentation preferences have no dependency on playlists, discovery, or playback. */
public final class Appearance {
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable task) { Thread t = new Thread(task, "Soundtrack appearance settings"); t.setDaemon(true); return t; }
    });
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                WRITER.shutdown();
                try { WRITER.awaitTermination(2, TimeUnit.SECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
        }, "Soundtrack appearance shutdown"));
    }
    private final File file;
    private final Library.Log log;
    private boolean writable = true;
    public boolean modern, light, reducedMotion;
    public Appearance(File directory, Library.Log log) {
        this.file = directory == null ? null : new File(directory, "soundtrackselector.appearance.properties");
        this.log = log;
        if (file == null || !file.exists()) return;
        try {
            Properties p = new Properties();
            InputStream in = new FileInputStream(file);
            try { p.load(in); } finally { in.close(); }
            modern = "modern".equals(p.getProperty("style"));
            light = "light".equals(p.getProperty("mode"));
            reducedMotion = Boolean.parseBoolean(p.getProperty("reducedAnimations", "false"));
        } catch (IOException failure) { writable = false; log.warning("Cannot read appearance settings; preserving file", failure); }
        catch (IllegalArgumentException failure) { writable = false; log.warning("Invalid appearance settings; preserving file", failure); }
    }
    public void save() {
        if (file == null || !writable) return;
        final Properties snapshot = new Properties();
        snapshot.setProperty("style", modern ? "modern" : "classic");
        snapshot.setProperty("mode", light ? "light" : "dark");
        snapshot.setProperty("reducedAnimations", Boolean.toString(reducedMotion));
        WRITER.execute(new Runnable() {
            public void run() {
                Path temporary = null;
                try {
                    Files.createDirectories(file.toPath().getParent());
                    temporary = Files.createTempFile(file.toPath().getParent(), ".appearance-", ".tmp");
                    OutputStream out = Files.newOutputStream(temporary);
                    try { snapshot.store(out, "Soundtrack Selector appearance"); } finally { out.close(); }
                    try { Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                    catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING); }
                } catch (IOException failure) { log.warning("Cannot save appearance settings", failure); }
                finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { } }
            }
        });
    }
}
