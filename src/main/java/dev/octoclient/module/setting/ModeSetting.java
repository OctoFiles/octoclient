package dev.octoclient.module.setting;

import java.util.List;

/** Dropdown setting cycling through fixed options. */
public class ModeSetting extends Setting<String> {
    private final List<String> options;

    public ModeSetting(String name, String description, String defaultValue, List<String> options) {
        super(name, description, defaultValue);
        this.options = List.copyOf(options);
    }

    public List<String> getOptions() {
        return options;
    }

    public void cycle() {
        int idx = options.indexOf(getValue());
        setValue(options.get((idx + 1) % options.size()));
    }

    public void select(String option) {
        if (options.contains(option)) {
            setValue(option);
        }
    }
}
