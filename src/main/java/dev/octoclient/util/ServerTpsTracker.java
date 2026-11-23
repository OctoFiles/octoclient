package dev.octoclient.util;

/** Estimates server tick rate from authoritative world-time synchronization packets. */
public final class ServerTpsTracker {
    private static long lastGameTime = -1L;
    private static long lastPacketNanos;
    private static double smoothedTps = 20.0;
    private static int samples;

    private ServerTpsTracker() {
    }

    public static synchronized void onTimeUpdate(long gameTime) {
        long now = System.nanoTime();
        if (lastGameTime >= 0L && lastPacketNanos > 0L) {
            long tickDelta = gameTime - lastGameTime;
            double elapsedSeconds = (now - lastPacketNanos) / 1_000_000_000.0;
            if (tickDelta > 0L && elapsedSeconds >= 0.1 && elapsedSeconds <= 10.0) {
                double measured = Math.min(20.0, tickDelta / elapsedSeconds);
                smoothedTps = samples == 0 ? measured : smoothedTps * 0.65 + measured * 0.35;
                samples++;
            } else if (tickDelta <= 0L || elapsedSeconds > 10.0) {
                samples = 0;
            }
        }
        lastGameTime = gameTime;
        lastPacketNanos = now;
    }

    public static synchronized double getTps() {
        if (samples == 0 || System.nanoTime() - lastPacketNanos > 5_000_000_000L) {
            return -1.0;
        }
        return smoothedTps;
    }
}
