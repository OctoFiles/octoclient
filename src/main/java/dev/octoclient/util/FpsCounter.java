package dev.octoclient.util;

/** Frame-rate counter measured from HUD render calls. */
public final class FpsCounter {
    private static final FpsCounter INSTANCE = new FpsCounter();

    private long windowStart = System.currentTimeMillis();
    private int frames;
    private int fps;

    private FpsCounter() {
    }

    public static FpsCounter get() {
        return INSTANCE;
    }

    public void onFrame() {
        frames++;
        long now = System.currentTimeMillis();
        long elapsed = now - windowStart;
        if (elapsed >= 500L) {
            fps = (int) (frames * 1000L / elapsed);
            frames = 0;
            windowStart = now;
        }
    }

    public int getFps() {
        return fps;
    }
}
