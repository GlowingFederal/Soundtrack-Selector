package com.glowingfederal.soundtrackselector.domain;

import java.util.concurrent.*;

/** One worker and one replaceable pending request. No client state is touched here. */
public final class Discovery implements AutoCloseable {
    public interface Scan {
        Library.Result run() throws Exception;
        void published(); // Called only by the client tick, after the catalog changes.
    }
    public static final class Completion {
        final long generation;
        final Scan scan;
        final Library.Result result;
        final Exception failure;
        Completion(long generation, Scan scan, Library.Result result, Exception failure) {
            this.generation = generation; this.scan = scan; this.result = result; this.failure = failure;
        }
    }
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(1), new ThreadFactory() {
                public Thread newThread(Runnable task) {
                    Thread thread = new Thread(task, "Soundtrack Selector discovery");
                    thread.setDaemon(true);
                    return thread;
                }
            });
    private Future<?> running;
    private long generation;
    private Completion completed;
    private boolean busy, closed;
    public Discovery() {
        Runtime.getRuntime().addShutdownHook(new Thread("Soundtrack Selector shutdown") {
            @Override public void run() { close(); }
        });
    }
    public synchronized void invalidate() {
        generation++;
        completed = null;
        if (running != null) running.cancel(true);
        worker.getQueue().clear();
        busy = false;
    }
    public synchronized void submit(final Scan scan) {
        if (closed) return;
        invalidate();
        busy = true;
        final long epoch = generation;
        running = worker.submit(new Runnable() {
            public void run() {
                Library.Result result = null;
                Exception failure = null;
                try { result = scan.run(); }
                catch (Exception problem) { failure = problem; }
                synchronized (Discovery.this) {
                    if (!closed && generation == epoch) completed = new Completion(epoch, scan, result, failure);
                }
            }
        });
    }
    public synchronized boolean busy() { return busy; }
    public synchronized Completion poll() {
        Completion value = completed;
        completed = null;
        if (value == null || value.generation != generation) return null;
        busy = false;
        running = null;
        return value;
    }
    @Override public synchronized void close() {
        closed = true;
        invalidate();
        worker.shutdownNow();
    }
    public static void checkCancelled() throws java.io.InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException("Discovery cancelled");
    }
}
