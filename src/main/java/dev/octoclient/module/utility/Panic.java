package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;

/** Panic key: disables every module at once. */
public class Panic extends Module {
    public Panic() {
        super("Panic", "Disables all modules (DELETE key)", 'P');
    }

    /** Invoked when the panic key is pressed. */
    public void trigger() {
        for (Module module : ModuleManager.get().getModules()) {
            if (module != this) {
                module.setEnabled(false);
            }
        }
    }
}
