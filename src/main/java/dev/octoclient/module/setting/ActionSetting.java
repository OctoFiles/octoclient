package dev.octoclient.module.setting;

/** A non-persistent button that launches a focused editor or action. */
public class ActionSetting extends Setting<String> {
    private final Runnable action;

    public ActionSetting(String name, String description, String buttonLabel, Runnable action) {
        super(name, description, buttonLabel);
        this.action = action;
    }

    public void activate() {
        action.run();
    }
}
