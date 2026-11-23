package dev.octoclient.util;

/** Enhanced animation helpers with smooth easing functions. */
public final class Anim {
    private Anim() {
    }

    /** Frame-rate independent exponential smoothing towards a target. */
    public static float approach(float current, float target, float speed, float partialTicks) {
        // speed = fraction of the remaining distance covered per 1/20s tick
        float clampedSpeed = clamp01(speed);
        float factor = 1.0f - (float) Math.pow(1.0f - clampedSpeed, Math.max(partialTicks, 0.0001f) * 20.0f);
        return current + (target - current) * factor;
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    public static int lerp(int from, int to, float t) {
        return (int) Math.round(from + (to - from) * t);
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public static float clamp01(float v) {
        return clamp(v, 0f, 1f);
    }

    /** Ease-out cubic. */
    public static float easeOut(float t) {
        float p = clamp01(t);
        return 1.0f - (1.0f - p) * (1.0f - p) * (1.0f - p);
    }

    /** Ease-out quad. */
    public static float easeOutQuad(float t) {
        float p = clamp01(t);
        return 1.0f - (1.0f - p) * (1.0f - p);
    }

    /** Ease-out quart. */
    public static float easeOutQuart(float t) {
        float p = clamp01(t);
        return 1.0f - (float) Math.pow(1.0f - p, 4.0f);
    }

    /** Ease-out quint. */
    public static float easeOutQuint(float t) {
        float p = clamp01(t);
        return 1.0f - (float) Math.pow(1.0f - p, 5.0f);
    }

    /** Ease-in-out cubic. */
    public static float easeInOut(float t) {
        float p = clamp01(t);
        return p < 0.5f ? 4 * p * p * p : 1 - (float) Math.pow(-2 * p + 2, 3) / 2f;
    }

    /** Ease-in-out back. */
    public static float easeInOutBack(float t) {
        float p = clamp01(t);
        float c1 = 1.70158f;
        float c2 = c1 * 1.525f;
        return p < 0.5f
                ? (float) (Math.pow(2 * p, 2) * ((c2 + 1) * 2 * p - c2)) / 2
                : (float) (Math.pow(2 * p - 2, 2) * ((c2 + 1) * (p * 2 - 2) + c2) + 2) / 2;
    }

    /**
     * Back-out: eases out past the target then settles back. Gives toggles and
     * cards a physical "snap" instead of a flat slide.
     */
    public static float easeOutBack(float t) {
        float p = clamp01(t);
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float q = p - 1f;
        return 1f + c3 * q * q * q + c1 * q * q;
    }

    /** Elastic ease-out for bouncy animations. */
    public static float easeOutElastic(float t) {
        float p = clamp01(t);
        if (p == 0f) return 0f;
        if (p == 1f) return 1f;
        float c4 = (2f * (float) Math.PI) / 3f;
        return (float) Math.pow(2f, -10f * p) * (float) Math.sin((p * 10f - 0.75f) * c4) + 1f;
    }

    /**
     * Exponential ease-out: very fast start, long tail. Good for entrances that
     * should feel instant but still settle softly.
     */
    public static float easeOutExpo(float t) {
        float p = clamp01(t);
        return p >= 1f ? 1f : 1f - (float) Math.pow(2f, -10f * p);
    }

    /** Smoothstep interpolation. */
    public static float smoothstep(float t) {
        float p = clamp01(t);
        return p * p * (3f - 2f * p);
    }

    /** Smootherstep interpolation. */
    public static float smootherstep(float t) {
        float p = clamp01(t);
        return p * p * p * (p * (6f * p - 15f) + 10f);
    }
}
