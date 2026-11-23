package dev.octoclient.ui;

import dev.octoclient.util.Anim;

/** Per-property smoothed animation value (frame-rate independent). */
public final class ValueAnim {
    private final float speed;
    private float value;

    public ValueAnim(float initial, float speed) {
        this.value = initial;
        this.speed = speed;
    }

    /** Advances toward the target and returns the new value. */
    public float to(float target, float tickDelta) {
        value = Anim.approach(value, target, speed, tickDelta);
        return value;
    }

    public float get() {
        return value;
    }

    public void snap(float v) {
        value = v;
    }
}
