package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleCategory;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ActionSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.TextSetting;
import dev.skylyrics.SkyLyricsClient;
import dev.skylyrics.SkyLyricsConfig;
import dev.skylyrics.gui.SettingsModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SkyLyricsModule extends Module {
    private final ModeSetting font = new ModeSetting("Lyrics Font", "Font used for the 3D lyrics",
            "block", List.of("block", "lobster", "minecraft"));
    private final BooleanSetting uppercase = new BooleanSetting("Uppercase", "Write lyrics in capitals", true);
    private final NumberSetting size = new NumberSetting("Text Size", "Size of the 3D lyrics", 1.0, 0.3, 3.0, 0.05);
    private final NumberSetting depth = new NumberSetting("Text Depth", "Depth of the 3D letters", 1.0, 0.0, 4.0, 0.1);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Lyrics color palette",
            "original", List.of("original", "custom"));
    private final ModeSetting colorPalette = new ModeSetting("Color Palette", "Choose a ready-made lyric color set",
            SettingsModel.PALETTE_NAMES.getFirst(), paletteOptions());
    private final ColorSetting customColor = new ColorSetting("Main Color", "Custom primary lyric color", 0xFF4DD8FF);
    private final BooleanSetting customFade = new BooleanSetting("Color Fade", "Blend each group into a second color", true);
    private final ColorSetting customFadeColor = new ColorSetting("Fade Color", "Custom secondary lyric color", 0xFF9B7BFF);
    private final BooleanSetting shimmer = new BooleanSetting("Shimmer", "Animate the lyric colors", true);
    private final BooleanSetting bob = new BooleanSetting("Bob", "Let the words bob and sway", true);
    private final NumberSetting spread = new NumberSetting("Spread", "Horizontal spread around the camera",
            36, 0, 80, 1);
    private final NumberSetting distance = new NumberSetting("Distance", "Average distance of lyric words",
            30, 4, 64, 0.5);
    private final NumberSetting heightMin = new NumberSetting("Minimum Height", "Lowest lyric height above your eyes",
            -1, -20, 50, 0.5);
    private final NumberSetting heightMax = new NumberSetting("Maximum Height", "Highest lyric height above your eyes",
            8, -20, 50, 0.5);
    private final NumberSetting holdMs = new NumberSetting("Line Hold", "Time a line remains visible after the next starts",
            2200, 0, 10000, 100);
    private final NumberSetting animationSpeed = new NumberSetting("Animation Speed", "Speed of lyric animations",
            1.0, 0.1, 4.0, 0.1);
    private final NumberSetting maxWords = new NumberSetting("Words Per Group", "Maximum words in a lyric group",
            3, 1, 8, 1);
    private final NumberSetting maxChars = new NumberSetting("Characters Per Group", "Maximum characters in a lyric group",
            12, 4, 32, 1);
    private final NumberSetting offsetMs = new NumberSetting("Timing Offset", "Positive values show lyrics earlier",
            0, -5000, 5000, 100);
    private final TextSetting player = new TextSetting("Music Source", "Spotify or * for any playing app",
            "spotify", 80);
    private final BooleanSetting estimateUnsynced = new BooleanSetting("Estimate Unsynced Lyrics",
            "Estimate timing when only plain lyrics are available", true);
    private final BooleanSetting youtubeMusicLyrics = new BooleanSetting("YouTube Music Fallback",
            "Use YouTube Music when synced lyrics are unavailable", true);
    private final TextSetting youtubeRegion = new TextSetting("YouTube Region", "Two-letter country code",
            "", 2);
    private final ModeSetting aiLanguage = new ModeSetting("AI Language", "Language for manually created AI lyrics",
            "auto", List.of("auto", "en", "hu"));
    private final ModeSetting aiModel = new ModeSetting("AI Model", "Model for manually created AI lyrics",
            "small", List.of("small", "medium"));

    public SkyLyricsModule() {
        super("Sky Lyrics", "Synced in-world lyrics and their appearance", ModuleCategory.SETTINGS, 'L');
        addSetting(font);
        addSetting(uppercase);
        addSetting(size);
        addSetting(depth);
        addSetting(colorMode);
        addSetting(colorPalette);
        addSetting(customColor);
        addSetting(customFade);
        addSetting(customFadeColor);
        addSetting(shimmer);
        addSetting(bob);
        addSetting(spread);
        addSetting(distance);
        addSetting(heightMin);
        addSetting(heightMax);
        addSetting(holdMs);
        addSetting(animationSpeed);
        addSetting(maxWords);
        addSetting(maxChars);
        addSetting(offsetMs);
        addSetting(player);
        addSetting(estimateUnsynced);
        addSetting(youtubeMusicLyrics);
        addSetting(youtubeRegion);
        addSetting(aiLanguage);
        addSetting(aiModel);
        addSetting(new ActionSetting("Open Lyrics Folder", "Open the local lyrics folder",
                "Open", SkyLyricsClient::openLyricsFolder));
        addSetting(new ActionSetting("Edit Current Lyrics", "Open the current song's lyrics file",
                "Edit", SkyLyricsClient::editCurrentLyrics));
        addSetting(new ActionSetting("Refresh Lyrics", "Fetch lyrics again for the current song",
                "Refresh", SkyLyricsClient::refreshLyrics));
        addSetting(new ActionSetting("Paste YouTube Link", "Link the copied YouTube video to the current song",
                "Paste", SkyLyricsClient::pasteVideoLink));
        addSetting(new ActionSetting("Remove YouTube Link", "Remove the current song's YouTube link",
                "Remove", SkyLyricsClient::removeVideoLink));
        addSetting(new ActionSetting("Lyrics Recording", "Start or stop timing lyrics for the current song",
                "Start / Stop", SkyLyricsClient::toggleLyricsRecording));
        addSetting(new ActionSetting("Install AI Lyrics", "Install optional AI lyrics components",
                "Install", SkyLyricsClient::installAiLyrics));
        addSetting(new ActionSetting("Create AI Lyrics", "Start or stop AI lyric creation for the current song",
                "Start / Stop", SkyLyricsClient::toggleAiLyrics));
    }

    public void loadSettings(SkyLyricsConfig config) {
        font.setValue(config.font);
        uppercase.setValue(config.uppercase);
        size.setRaw(config.size);
        depth.setRaw(config.depth);
        colorMode.setValue(config.colorMode);
        colorPalette.setValue(findPalette(config.colors));
        customColor.setValue(0xFF000000 | SkyLyricsConfig.parseColor(config.customColor, 0x4DD8FF));
        customFade.setValue(config.customFade);
        customFadeColor.setValue(0xFF000000 | SkyLyricsConfig.parseColor(config.customFadeColor, 0x9B7BFF));
        shimmer.setValue(config.shimmer);
        bob.setValue(config.bob);
        spread.setRaw(config.spread);
        distance.setRaw(config.distanceCenter());
        heightMin.setRaw(config.heightMin);
        heightMax.setRaw(config.heightMax);
        holdMs.setRaw(config.holdMs);
        animationSpeed.setRaw(config.animationSpeed);
        maxWords.setRaw(config.maxWordsPerGroup);
        maxChars.setRaw(config.maxCharsPerGroup);
        offsetMs.setRaw(config.offsetMs);
        player.setValue(config.player);
        estimateUnsynced.setValue(config.estimateUnsyncedLyrics);
        youtubeMusicLyrics.setValue(config.youtubeMusicLyrics);
        youtubeRegion.setValue(config.youtubeRegion);
        aiLanguage.setValue(config.aiLanguage);
        aiModel.setValue(config.aiModel);
    }

    public boolean syncSettings(SkyLyricsConfig config) {
        boolean changed = false;
        changed |= update(config.font, font.getValue());
        config.font = font.getValue();
        changed |= update(config.uppercase, uppercase.getValue());
        config.uppercase = uppercase.getValue();
        changed |= update(config.size, size.getSnapped());
        config.size = size.getSnapped();
        changed |= update(config.depth, depth.getSnapped());
        config.depth = depth.getSnapped();
        changed |= update(config.colorMode, colorMode.getValue());
        config.colorMode = colorMode.getValue();
        int paletteIndex = SettingsModel.PALETTE_NAMES.indexOf(colorPalette.getValue());
        if (paletteIndex >= 0) {
            List<String> palette = SettingsModel.PALETTES.get(paletteIndex);
            changed |= !Objects.equals(config.colors, palette);
            config.colors = new ArrayList<>(palette);
        }
        String mainColor = formatColor(customColor.getRgb());
        changed |= update(config.customColor, mainColor);
        config.customColor = mainColor;
        changed |= update(config.customFade, customFade.getValue());
        config.customFade = customFade.getValue();
        String fadeColor = formatColor(customFadeColor.getRgb());
        changed |= update(config.customFadeColor, fadeColor);
        config.customFadeColor = fadeColor;
        changed |= update(config.shimmer, shimmer.getValue());
        config.shimmer = shimmer.getValue();
        changed |= update(config.bob, bob.getValue());
        config.bob = bob.getValue();
        changed |= update(config.spread, spread.getSnapped());
        config.spread = spread.getSnapped();

        double newDistanceMin = Math.round(distance.getSnapped() * 0.8 * 10) / 10.0;
        double newDistanceMax = Math.round(distance.getSnapped() * 1.2 * 10) / 10.0;
        changed |= update(config.distanceMin, newDistanceMin);
        changed |= update(config.distanceMax, newDistanceMax);
        config.distanceMin = newDistanceMin;
        config.distanceMax = newDistanceMax;
        changed |= update(config.heightMin, heightMin.getSnapped());
        config.heightMin = heightMin.getSnapped();
        changed |= update(config.heightMax, heightMax.getSnapped());
        config.heightMax = heightMax.getSnapped();
        long newHoldMs = Math.round(holdMs.getSnapped());
        changed |= update(config.holdMs, newHoldMs);
        config.holdMs = newHoldMs;
        changed |= update(config.animationSpeed, animationSpeed.getSnapped());
        config.animationSpeed = animationSpeed.getSnapped();
        int newMaxWords = maxWords.getInt();
        changed |= update(config.maxWordsPerGroup, newMaxWords);
        config.maxWordsPerGroup = newMaxWords;
        int newMaxChars = maxChars.getInt();
        changed |= update(config.maxCharsPerGroup, newMaxChars);
        config.maxCharsPerGroup = newMaxChars;
        long newOffset = Math.round(offsetMs.getSnapped());
        changed |= update(config.offsetMs, newOffset);
        config.offsetMs = newOffset;
        changed |= update(config.player, player.getValue());
        config.player = player.getValue();
        changed |= update(config.estimateUnsyncedLyrics, estimateUnsynced.getValue());
        config.estimateUnsyncedLyrics = estimateUnsynced.getValue();
        changed |= update(config.youtubeMusicLyrics, youtubeMusicLyrics.getValue());
        config.youtubeMusicLyrics = youtubeMusicLyrics.getValue();
        changed |= update(config.youtubeRegion, youtubeRegion.getValue());
        config.youtubeRegion = youtubeRegion.getValue();
        changed |= update(config.aiLanguage, aiLanguage.getValue());
        config.aiLanguage = aiLanguage.getValue();
        changed |= update(config.aiModel, aiModel.getValue());
        config.aiModel = aiModel.getValue();
        return changed;
    }

    private static List<String> paletteOptions() {
        List<String> options = new ArrayList<>(SettingsModel.PALETTE_NAMES);
        options.add("Custom");
        return List.copyOf(options);
    }

    private static String findPalette(List<String> colors) {
        for (int i = 0; i < SettingsModel.PALETTES.size(); i++) {
            List<String> palette = SettingsModel.PALETTES.get(i);
            if (palette.size() != colors.size()) {
                continue;
            }
            boolean matches = true;
            for (int color = 0; color < palette.size(); color++) {
                if (!palette.get(color).equalsIgnoreCase(colors.get(color).strip())) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return SettingsModel.PALETTE_NAMES.get(i);
            }
        }
        return "Custom";
    }

    private static String formatColor(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    private static boolean update(Object current, Object next) {
        return !Objects.equals(current, next);
    }

    public void syncEnabledFromLyrics(boolean enabled) {
        if (isEnabled() != enabled) {
            setEnabled(enabled);
        }
    }

    @Override
    protected void onEnable() {
        SkyLyricsClient.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        SkyLyricsClient.setEnabled(false);
    }
}
