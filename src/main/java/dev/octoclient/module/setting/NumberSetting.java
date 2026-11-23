package dev.octoclient.module.setting;

/** Numeric slider setting with min/max/step. */
public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    /** Value clamped to [min, max] and snapped to the step grid. */
    public double getSnapped() {
        double v = Math.max(min, Math.min(max, getValue()));
        if (step > 0) {
            v = min + Math.round((v - min) / step) * step;
            v = Math.max(min, Math.min(max, v));
        }
        return v;
    }

    public int getInt() {
        return (int) Math.round(getSnapped());
    }

    public float getFloat() {
        return (float) getSnapped();
    }

    public void setRaw(double v) {
        setValue(Math.max(min, Math.min(max, v)));
    }

    /** Slider position 0..1. */
    public float getFraction() {
        return (float) ((getSnapped() - min) / (max - min));
    }

    public void setFraction(float f) {
        setRaw(min + (max - min) * f);
    }
}
