package dev.octoclient.module.secret;

/**
 * Spear aura: the same dive/hit cycle as {@link MaceAura}, locked to spears
 * and tridents. Every hit is fully charged because the local attack ticker is
 * refilled and no swing packet is sent, so the server never resets the charge
 * ("no cooldown").
 */
public class SpearAura extends MaceAura {
    public SpearAura() {
        super("SpearAura", "Dives onto targets with a spear and hits them without a cooldown.",
                'S', "Spear");
    }
}
