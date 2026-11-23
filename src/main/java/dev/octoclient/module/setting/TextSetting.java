package dev.octoclient.module.setting;

/** Editable single-line text setting with a maximum character count. */
public class TextSetting extends Setting<String> {
    private final int maxLength;

    public TextSetting(String name, String description, String defaultValue, int maxLength) {
        super(name, description, defaultValue);
        this.maxLength = Math.max(1, maxLength);
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public void setValue(String value) {
        super.setValue(value == null ? "" : value.substring(0, Math.min(value.length(), maxLength)));
    }
}
