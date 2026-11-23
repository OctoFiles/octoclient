package dev.octoclient.module.setting;

/** RGBA color setting stored as a packed 0xAARRGGBB integer. */
public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, String description, int defaultArgb) {
        super(name, description, defaultArgb);
    }

    public int getRed() {
        return (getValue() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getValue() >> 8) & 0xFF;
    }

    public int getBlue() {
        return getValue() & 0xFF;
    }

    public int getAlpha() {
        return (getValue() >>> 24) & 0xFF;
    }

    public void setRgba(int r, int g, int b, int a) {
        setValue(((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
    }

    /** Opaque rgb packed for drawing helpers. */
    public int getRgb() {
        return getValue() & 0xFFFFFF;
    }
}
