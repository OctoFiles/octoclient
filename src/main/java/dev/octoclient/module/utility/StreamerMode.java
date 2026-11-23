package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.TextSetting;
import com.ntr.NoTextureRotations;
import com.ntr.config.Config;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Masks sensitive HUD info for streamers, ported from NoRisk's streamer mode.
 * The HUD modules read the flags instead of duplicating the settings.
 */
public class StreamerMode extends Module {
    private final BooleanSetting hideServer = new BooleanSetting("Hide Server", "Mask the server address", true);
    private final BooleanSetting hideCoords = new BooleanSetting("Hide Coordinates", "Mask your coordinates", false);
    private final ModeSetting playerName = new ModeSetting("Player Name", "How your local player name is displayed",
            "Hide", List.of("Hide", "Custom", "Normal"));
    private final TextSetting customPlayerName =
            new TextSetting("Custom Name", "Name shown when Player Name is set to Custom", "Streamer", 24);
    private final ModeSetting f3Coordinates = new ModeSetting("F3 Coordinates",
            "Hide or randomize coordinates shown on the debug screen", "Randomize",
            List.of("Hide", "Randomize", "Normal"));
    private final BooleanSetting disableTextureRotations =
            new BooleanSetting("Disable Texture Rotations", "Prevent block texture rotations", true);
    private final BooleanSetting disableTextureOffsets =
            new BooleanSetting("Disable Texture Offsets", "Prevent block texture offsets", true);
    private final ModeSetting textureMode = new ModeSetting("Texture Mode", "NoTextureRotations behavior",
            "No Rotations", List.of("No Rotations", "Secure Random", "Random Offset", "Repeating"));

    private final double fakeX = ThreadLocalRandom.current().nextDouble(-30_000_000, 30_000_000);
    private final double fakeY = ThreadLocalRandom.current().nextDouble(-64, 320);
    private final double fakeZ = ThreadLocalRandom.current().nextDouble(-30_000_000, 30_000_000);
    private String lastTextureConfig;
    private boolean missingTextureModLogged;

    public StreamerMode() {
        super("Streamer Mode", "Hide sensitive HUD info while streaming", 'S');
        addSetting(hideServer);
        addSetting(hideCoords);
        addSetting(playerName);
        addSetting(customPlayerName);
        addSetting(f3Coordinates);
        addSetting(disableTextureRotations);
        addSetting(disableTextureOffsets);
        addSetting(textureMode);
    }

    public boolean hidesServer() {
        return isEnabled() && hideServer.isEnabled();
    }

    public boolean hidesCoords() {
        return isEnabled() && hideCoords.isEnabled();
    }

    public boolean hidesPlayerName() {
        return isEnabled() && playerName.getValue().equals("Hide");
    }

    public String displayedPlayerName() {
        return isEnabled() && playerName.getValue().equals("Custom") ? customPlayerName.getValue() : null;
    }

    public boolean masksF3Coordinates() {
        return isEnabled() && !f3Coordinates.getValue().equals("Normal");
    }

    public boolean hidesF3Coordinates() {
        return masksF3Coordinates() && f3Coordinates.getValue().equals("Hide");
    }

    public double fakeX() {
        return fakeX;
    }

    public double fakeY() {
        return fakeY;
    }

    public double fakeZ() {
        return fakeZ;
    }

    public String maskF3CoordinateLine(String line) {
        if (!masksF3Coordinates()) {
            return line;
        }
        boolean hide = hidesF3Coordinates();
        int x = (int) Math.floor(fakeX);
        int y = (int) Math.floor(fakeY);
        int z = (int) Math.floor(fakeZ);
        if (line.startsWith("XYZ:")) {
            return hide ? "XYZ: hidden"
                    : String.format(Locale.ROOT, "XYZ: %.3f / %.5f / %.3f", fakeX, fakeY, fakeZ);
        }
        if (line.startsWith("Block:")) {
            return hide ? "Block: hidden" : String.format(Locale.ROOT, "Block: %d %d %d", x, y, z);
        }
        if (line.startsWith("Chunk:")) {
            if (hide) {
                return "Chunk: hidden";
            }
            int chunkX = Math.floorDiv(x, 16);
            int chunkZ = Math.floorDiv(z, 16);
            return String.format(Locale.ROOT, "Chunk: %d %d %d [%d %d in r.%d.%d.mca]",
                    chunkX, Math.floorDiv(y, 16), chunkZ,
                    Math.floorMod(chunkX, 32), Math.floorMod(chunkZ, 32),
                    Math.floorDiv(chunkX, 32), Math.floorDiv(chunkZ, 32));
        }
        int targetedBlock = line.indexOf("Targeted Block: ");
        int targetedFluid = line.indexOf("Targeted Fluid: ");
        int labelStart = targetedBlock >= 0 ? targetedBlock : targetedFluid;
        if (labelStart >= 0) {
            int valueStart = line.indexOf(": ", labelStart) + 2;
            return line.substring(0, valueStart)
                    + (hide ? "hidden" : String.format(Locale.ROOT, "%d, %d, %d", x, y, z));
        }
        return line;
    }

    /** Keeps NoTextureRotations aligned with the independent streamer settings. */
    public void syncTextureProtection() {
        if (!FabricLoader.getInstance().isModLoaded("notexturerotations")) {
            if (!missingTextureModLogged) {
                System.err.println("[OctoClient] NoTextureRotations is not loaded; texture protection is inactive.");
                missingTextureModLogged = true;
            }
            return;
        }
        Config config = NoTextureRotations.config.getConfig();
        boolean enabled = isEnabled();
        config.disableTextureRotations = enabled && disableTextureRotations.isEnabled();
        config.disableOffsets = enabled && disableTextureOffsets.isEnabled();
        config.mode = switch (textureMode.getValue()) {
            case "Secure Random" -> Config.Mode.SECURE_RANDOM;
            case "Random Offset" -> Config.Mode.RANDOM_OFFSET;
            case "Repeating" -> Config.Mode.REPEATING_SECTION;
            default -> Config.Mode.NO_ROTATIONS;
        };

        String state = config.disableTextureRotations + "|" + config.disableOffsets + "|" + config.mode;
        if (!state.equals(lastTextureConfig)) {
            NoTextureRotations.config.save();
            lastTextureConfig = state;
        }
    }
}
