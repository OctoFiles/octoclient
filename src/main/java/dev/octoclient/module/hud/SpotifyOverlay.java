package dev.octoclient.module.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.TextSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.WindowsMediaSession;
import dev.skylyrics.SkyLyricsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.glfw.GLFW;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/** Displays Spotify's current track and offers optional playback hotkeys via the Spotify Web API. */
public class SpotifyOverlay extends HudModule implements Module.Tickable {
    private static SpotifyOverlay instance;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final TextSetting accessToken =
            new TextSetting("Access Token", "Spotify Web API user access token (stored in client config)", "", 512);
    private final NumberSetting updateInterval =
            new NumberSetting("Update Interval", "Track refresh interval in seconds", 10, 5, 60, 1);
    private final NumberSetting corners = new NumberSetting("Corners", "Overlay corner radius", 10, 0, 24, 1);
    private final ModeSetting layout = new ModeSetting("Layout", "Choose the track card arrangement",
            "Compact", List.of("Compact", "Cover", "Centered"));
    private final BooleanSetting showCover =
            new BooleanSetting("Show Album Cover",
                    "Show artwork from Windows media controls or iTunes/Deezer lookup", true);
    private final BooleanSetting showText = new BooleanSetting("Show Text", "Show track, artist and time text", true);
    private final BooleanSetting dynamicPadding = new BooleanSetting("Dynamic Padding", "Size to the displayed track content", true);
    private final NumberSetting fixedWidth = new NumberSetting("Width", "Fixed overlay width when dynamic padding is off", 220, 100, 500, 5);
    private final NumberSetting fixedHeight = new NumberSetting("Height", "Fixed overlay height when dynamic padding is off", 64, 30, 240, 2);
    private final ColorSetting albumColor = new ColorSetting("Album Color", "Overlay tint color", 0xFF43506A);
    private final NumberSetting coverResolution = new NumberSetting("Cover Resolution", "Preferred album artwork source resolution", 300, 64, 640, 16);
    private final BooleanSetting autoHide = new BooleanSetting("Auto Hide", "Hide the overlay when nothing is playing", false);
    private final BooleanSetting marquee = new BooleanSetting("Marquee", "Scroll long track names inside the card", true);
    private final BooleanSetting nametagVisibility = new BooleanSetting("Nametag Visibility", "Show the Spotify source label", false);
    private final BooleanSetting worldTrackText =
            new BooleanSetting("World Song Text", "Show the current song above your player in the world", false);
    private final BooleanSetting showTimeLabels = new BooleanSetting("Show Time Labels", "Show elapsed and total track time", true);
    private final BooleanSetting showLyricsNavigation =
            new BooleanSetting("Lyrics Navigation", "Show Track and Lyrics pages in the overlay", false);
    private final ModeSetting overlayPage = new ModeSetting("Overlay Page", "Choose the visible Spotify overlay page",
            "Track", List.of("Track", "Lyrics"));
    private final KeybindSetting pageToggleKey =
            new KeybindSetting("Page Toggle Key", "Switch between the Track and Lyrics pages", 0);
    private final BooleanSetting sourceSpotify = new BooleanSetting("Spotify", "Show Spotify playback", true);
    private final BooleanSetting sourceAppleMusic = new BooleanSetting("Apple Music", "Allow Apple Music playback source", false);
    private final BooleanSetting sourceYouTube = new BooleanSetting("YouTube", "Allow YouTube playback source", false);
    private final BooleanSetting sourceAmazonMusic = new BooleanSetting("Amazon Music", "Allow Amazon Music playback source", false);
    private final BooleanSetting sourceSoundCloud = new BooleanSetting("SoundCloud", "Allow SoundCloud playback source", false);
    private final BooleanSetting sourceTidal = new BooleanSetting("Tidal", "Allow Tidal playback source", false);
    private final BooleanSetting sourceCider = new BooleanSetting("Cider", "Allow Cider playback source", false);
    private final BooleanSetting sourceDeezer = new BooleanSetting("Deezer", "Allow Deezer playback source", false);
    private final BooleanSetting allowOther = new BooleanSetting("Allow Other", "Allow unrecognized playback sources", false);
    private final BooleanSetting showArtist = new BooleanSetting("Artist", "Show artist name", true);
    private final BooleanSetting showAlbum = new BooleanSetting("Album", "Show album name", false);
    private final BooleanSetting showProgress = new BooleanSetting("Progress", "Show playback progress bar", true);
    private final BooleanSetting controls = new BooleanSetting("Playback Controls", "Enable the playback hotkeys", true);
    private final KeybindSetting previousKey =
            new KeybindSetting("Previous Track Key", "Skip to previous track", GLFW.GLFW_KEY_F9);
    private final KeybindSetting playPauseKey =
            new KeybindSetting("Play/Pause Key", "Toggle playback", GLFW.GLFW_KEY_F10);
    private final KeybindSetting nextKey =
            new KeybindSetting("Next Track Key", "Skip to next track", GLFW.GLFW_KEY_F11);
    private final KeybindSetting seekBackKey =
            new KeybindSetting("Seek Back Key", "Rewind the current track", GLFW.GLFW_KEY_F7);
    private final KeybindSetting seekForwardKey =
            new KeybindSetting("Seek Forward Key", "Fast-forward the current track", GLFW.GLFW_KEY_F8);
    private final NumberSetting seekStep =
            new NumberSetting("Seek Step", "Seconds skipped by the seek keys", 10, 1, 60, 1);
    private final boolean[] wasDown = new boolean[5];
    private final AtomicBoolean requestInFlight = new AtomicBoolean();

    private volatile String track = "";
    private volatile String artist = "";
    private volatile String album = "";
    private volatile String mediaSource = "";
    private volatile String status = "Nothing playing";
    private volatile long progressMillis;
    private volatile long durationMillis;
    private volatile long progressSyncedAtNanos;
    private long lastProgressSampleNanos = Long.MIN_VALUE;
    private volatile boolean playing;
    private volatile boolean hasPlayback;
    private volatile String coverUrl = "";
    private volatile byte[] coverBytes;
    private volatile String coverBytesUrl = "";
    private volatile String rejectedCoverUrl = "";
    private volatile String requestedCoverUrl = "";
    private final AtomicBoolean coverRequestInFlight = new AtomicBoolean();
    private Identifier coverTexture;
    private String registeredCoverUrl = "";
    private volatile String lastLoggedError = "";
    private volatile String requestedCoverTrack = "";
    private long nextPollTime;
    private boolean pageKeyWasDown;

    public SpotifyOverlay() {
        super("Spotify Overlay", "Show and control your current Spotify playback", 'S');
        addSetting(accessToken);
        addSetting(updateInterval);
        addSetting(corners);
        addSetting(layout);
        addSetting(showCover);
        addSetting(showText);
        addSetting(dynamicPadding);
        addSetting(fixedWidth);
        addSetting(fixedHeight);
        addSetting(albumColor);
        addSetting(coverResolution);
        addSetting(autoHide);
        addSetting(marquee);
        addSetting(nametagVisibility);
        addSetting(worldTrackText);
        addSetting(showTimeLabels);
        addSetting(showLyricsNavigation);
        addSetting(overlayPage);
        addSetting(pageToggleKey);
        addSetting(sourceSpotify);
        addSetting(sourceAppleMusic);
        addSetting(sourceYouTube);
        addSetting(sourceAmazonMusic);
        addSetting(sourceSoundCloud);
        addSetting(sourceTidal);
        addSetting(sourceCider);
        addSetting(sourceDeezer);
        addSetting(allowOther);
        addSetting(showArtist);
        addSetting(showAlbum);
        addSetting(showProgress);
        addSetting(controls);
        addSetting(previousKey);
        addSetting(playPauseKey);
        addSetting(nextKey);
        addSetting(seekBackKey);
        addSetting(seekForwardKey);
        addSetting(seekStep);
        x = 10;
        y = 210;
        instance = this;
    }

    public static boolean hasWorldTrackText(LivingEntity entity) {
        return instance != null && instance.isEnabled() && instance.worldTrackText.isEnabled()
                && entity == Minecraft.getInstance().player && instance.hasPlayback;
    }

    public static Component worldTrackText(Player player) {
        if (!hasWorldTrackText(player)) {
            return null;
        }
        String title = instance.track;
        if (!instance.artist.isBlank()) {
            title += " - " + instance.artist;
        }
        return Component.literal(title);
    }

    @Override
    public void onTick(Minecraft client) {
        String token = accessToken.getValue().trim();

        if (showLyricsNavigation.isEnabled() && client.gui.screen() == null) {
            triggerPageToggle(client.getWindow().handle());
        } else {
            pageKeyWasDown = false;
        }

        if (controls.isEnabled() && client.gui.screen() == null) {
            long window = client.getWindow().handle();
            triggerControl(window, 0, previousKey, "previous");
            triggerControl(window, 1, playPauseKey, "play-pause");
            triggerControl(window, 2, nextKey, "next");
            triggerSeek(window, 3, seekBackKey, -1);
            triggerSeek(window, 4, seekForwardKey, 1);
        } else {
            wasDown[0] = wasDown[1] = wasDown[2] = wasDown[3] = wasDown[4] = false;
        }

        if (token.isEmpty()) {
            // No token needed: read the Windows media session instead.
            pollWindowsSession();
            return;
        }

        long now = System.currentTimeMillis();
        if (now >= nextPollTime && requestInFlight.compareAndSet(false, true)) {
            nextPollTime = now + updateInterval.getInt() * 1000L;
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
                    .timeout(Duration.ofSeconds(8))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((response, error) -> {
                        try {
                            if (error != null) {
                                reportError("Spotify request failed: " + error.getClass().getSimpleName());
                                status = "Spotify connection failed";
                                return;
                            }
                            handlePlaybackResponse(response);
                        } finally {
                            requestInFlight.set(false);
                        }
                    });
        }
    }

    /**
     * Maps the Windows media session onto the overlay fields, so Spotify works
     * without entering an access token (it is read from the OS media controls).
     */
    private void pollWindowsSession() {
        WindowsMediaSession.Info info = WindowsMediaSession.info();
        if (info == null) {
            return;
        }
        if (info.title().isBlank()) {
            hasPlayback = false;
            requestedCoverTrack = "";
            status = "Nothing playing";
            return;
        }
        String source = info.source() == null ? "" : info.source().toLowerCase(java.util.Locale.ROOT);
        boolean spotify = source.contains("spotify");
        boolean allowed = sourceEnabled(source);
        if (!allowed) {
            hasPlayback = false;
            status = spotify ? "Spotify source disabled" : "Selected source is not integrated";
            return;
        }
        String nextTrack = info.title();
        String nextArtist = info.artist() == null ? "" : info.artist();
        String nextAlbum = info.album() == null ? "" : info.album();
        boolean trackChanged = !track.equals(nextTrack) || !artist.equals(nextArtist) || !album.equals(nextAlbum);
        if (trackChanged && info.coverKey().isBlank()) {
            coverBytes = null;
            coverBytesUrl = "";
            coverUrl = "";
        }
        syncProgress(nextTrack, info.positionMillis(), info.durationMillis(), info.playing(),
                info.polledAtNanos());
        track = nextTrack;
        artist = nextArtist;
        album = nextAlbum;
        mediaSource = sourceName(info.source());
        if (!info.coverKey().isBlank()) {
            syncWindowsCover(info.coverKey());
        }
        if (showCover.isEnabled() && coverUrl.isBlank()) {
            requestAlbumArtwork(nextTrack, artist, album);
        }
        hasPlayback = true;
        status = "";
    }

    private void requestAlbumArtwork(String title, String artistName, String albumName) {
        String trackKey = title + '\u0001' + artistName + '\u0001' + albumName;
        if (title.isBlank() || artistName.isBlank() || trackKey.equals(requestedCoverTrack)
                || !coverRequestInFlight.compareAndSet(false, true)) {
            return;
        }
        requestedCoverTrack = trackKey;

        String term = String.join(" ", title, artistName, albumName).strip();
        URI searchUri = URI.create("https://itunes.apple.com/search?term="
                + URLEncoder.encode(term, StandardCharsets.UTF_8) + "&entity=song&limit=10");
        HttpRequest searchRequest = HttpRequest.newBuilder(searchUri)
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .GET()
                .build();
        HTTP.sendAsync(searchRequest, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        requestDeezerArtwork(title, artistName, albumName);
                        return;
                    }
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        requestDeezerArtwork(title, artistName, albumName);
                        return;
                    }

                    String artworkUrl;
                    try {
                        artworkUrl = findAlbumArtwork(response.body(), title, artistName);
                    } catch (JsonParseException | IllegalArgumentException | IllegalStateException
                             | UnsupportedOperationException errorParsing) {
                        requestDeezerArtwork(title, artistName, albumName);
                        return;
                    }
                    if (!track.equals(title) || !artist.equals(artistName)
                            || !album.equals(albumName) || !coverUrl.isBlank()) {
                        coverRequestInFlight.set(false);
                        return;
                    }
                    if (artworkUrl == null) {
                        requestDeezerArtwork(title, artistName, albumName);
                        return;
                    }
                    downloadAlbumArtwork(artworkUrl, title, artistName, albumName);
                });
    }

    private void requestDeezerArtwork(String title, String artistName, String albumName) {
        String term = title + " " + artistName;
        URI searchUri = URI.create("https://api.deezer.com/search/track?q="
                + URLEncoder.encode(term, StandardCharsets.UTF_8));
        HttpRequest searchRequest = HttpRequest.newBuilder(searchUri)
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .GET()
                .build();
        HTTP.sendAsync(searchRequest, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        coverRequestInFlight.set(false);
                        reportError("Album artwork lookup failed: " + error.getClass().getSimpleName());
                        return;
                    }
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        coverRequestInFlight.set(false);
                        reportError("Album artwork lookup returned HTTP " + response.statusCode());
                        return;
                    }

                    String artworkUrl;
                    try {
                        artworkUrl = findDeezerArtwork(response.body(), title, artistName);
                    } catch (JsonParseException | IllegalArgumentException | IllegalStateException
                             | UnsupportedOperationException errorParsing) {
                        coverRequestInFlight.set(false);
                        reportError("Album artwork lookup response could not be parsed: "
                                + errorParsing.getClass().getSimpleName());
                        return;
                    }
                    if (!track.equals(title) || !artist.equals(artistName)
                            || !album.equals(albumName) || !coverUrl.isBlank()) {
                        coverRequestInFlight.set(false);
                        return;
                    }
                    if (artworkUrl == null) {
                        coverRequestInFlight.set(false);
                        reportError("Album artwork lookup found no matching track");
                        return;
                    }
                    downloadAlbumArtwork(artworkUrl, title, artistName, albumName);
                });
    }

    private void downloadAlbumArtwork(String artworkUrl, String title, String artistName, String albumName) {
        URI artworkUri;
        try {
            artworkUri = URI.create(artworkUrl.replaceFirst("\\d+x\\d+bb", "600x600bb"));
        } catch (IllegalArgumentException invalidUri) {
            coverRequestInFlight.set(false);
            reportError("Album artwork URL is invalid");
            return;
        }
        HttpRequest artworkRequest = HttpRequest.newBuilder(artworkUri)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HTTP.sendAsync(artworkRequest, HttpResponse.BodyHandlers.ofByteArray())
                .whenComplete((imageResponse, imageError) -> {
                    try {
                        if (imageError != null) {
                            reportError("Album artwork download failed: "
                                    + imageError.getClass().getSimpleName());
                            return;
                        }
                        if (imageResponse.statusCode() < 200 || imageResponse.statusCode() >= 300) {
                            reportError("Album artwork download returned HTTP "
                                    + imageResponse.statusCode());
                            return;
                        }
                        if (imageResponse.body().length == 0) {
                            reportError("Album artwork download returned an empty image");
                            return;
                        }
                        if (track.equals(title) && artist.equals(artistName) && album.equals(albumName)
                                && coverUrl.isBlank()) {
                            coverBytes = imageResponse.body();
                            coverBytesUrl = artworkUrl;
                            coverUrl = artworkUrl;
                            rejectedCoverUrl = "";
                        }
                    } finally {
                        coverRequestInFlight.set(false);
                    }
                });
    }

    private static String findAlbumArtwork(String responseBody, String title, String artistName) {
        JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
        var results = response.getAsJsonArray("results");
        if (results == null) {
            return null;
        }
        String wantedTitle = normalizeArtworkText(title);
        String wantedArtist = normalizeArtworkText(artistName);
        for (var result : results) {
            if (!result.isJsonObject()) {
                continue;
            }
            JsonObject item = result.getAsJsonObject();
            if (!item.has("trackName") || !item.has("artistName") || !item.has("artworkUrl100")) {
                continue;
            }
            String resultTitle = normalizeArtworkText(item.get("trackName").getAsString());
            String resultArtist = normalizeArtworkText(item.get("artistName").getAsString());
            if (wantedTitle.equals(resultTitle)
                    && (wantedArtist.equals(resultArtist) || wantedArtist.contains(resultArtist)
                    || resultArtist.contains(wantedArtist))) {
                return item.get("artworkUrl100").getAsString();
            }
        }
        return null;
    }

    private static String normalizeArtworkText(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private static String findDeezerArtwork(String responseBody, String title, String artistName) {
        JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
        var results = response.getAsJsonArray("data");
        if (results == null) {
            return null;
        }
        String wantedTitle = normalizeArtworkText(title);
        String wantedArtist = normalizeArtworkText(artistName);
        for (var result : results) {
            if (!result.isJsonObject()) {
                continue;
            }
            JsonObject item = result.getAsJsonObject();
            if (!item.has("title") || !item.has("artist") || !item.get("artist").isJsonObject()
                    || !item.has("album") || !item.get("album").isJsonObject()) {
                continue;
            }
            JsonObject albumInfo = item.getAsJsonObject("album");
            if (!albumInfo.has("cover_xl")) {
                continue;
            }
            String resultTitle = normalizeArtworkText(item.get("title").getAsString());
            String resultArtist = normalizeArtworkText(item.getAsJsonObject("artist").get("name").getAsString());
            if (wantedTitle.equals(resultTitle)
                    && (wantedArtist.equals(resultArtist) || wantedArtist.contains(resultArtist)
                    || resultArtist.contains(wantedArtist))) {
                return albumInfo.get("cover_xl").getAsString();
            }
        }
        return null;
    }

    /** Loads the exact album artwork extracted from the active Windows media session. */
    private void syncWindowsCover(String coverKey) {
        if (!showCover.isEnabled()) {
            return;
        }
        if (coverKey == null || coverKey.isBlank()) {
            coverBytes = null;
            coverBytesUrl = "";
            coverUrl = "";
            return;
        }
        if (coverKey.equals(rejectedCoverUrl)) {
            return;
        }
        if (coverKey.equals(coverBytesUrl) && coverBytes != null && coverBytes.length > 0) {
            return;
        }
        try {
            var coverFile = WindowsMediaSession.coverFile();
            if (coverFile == null || !Files.isRegularFile(coverFile)) {
                coverBytes = null;
                coverBytesUrl = "";
                coverUrl = "";
                return;
            }
            byte[] image = Files.readAllBytes(coverFile);
            if (image.length == 0) {
                throw new IOException("Windows media artwork file is empty");
            }
            coverBytes = image;
            coverBytesUrl = coverKey;
            coverUrl = coverKey;
            rejectedCoverUrl = "";
        } catch (IOException error) {
            reportError("Windows media artwork could not be read: " + error.getMessage());
        }
    }

    private boolean sourceEnabled(String source) {
        if (source.contains("spotify")) return sourceSpotify.isEnabled();
        if (source.contains("apple")) return sourceAppleMusic.isEnabled();
        if (source.contains("youtube")) return sourceYouTube.isEnabled();
        if (source.contains("amazon")) return sourceAmazonMusic.isEnabled();
        if (source.contains("soundcloud")) return sourceSoundCloud.isEnabled();
        if (source.contains("tidal")) return sourceTidal.isEnabled();
        if (source.contains("cider")) return sourceCider.isEnabled();
        if (source.contains("deezer")) return sourceDeezer.isEnabled();
        return allowOther.isEnabled();
    }

    private static String sourceName(String source) {
        if (source == null || source.isBlank()) return "Media";
        String normalized = source.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("spotify")) return "Spotify";
        if (normalized.contains("apple")) return "Apple Music";
        if (normalized.contains("youtube")) return "YouTube";
        if (normalized.contains("amazon")) return "Amazon Music";
        if (normalized.contains("soundcloud")) return "SoundCloud";
        if (normalized.contains("tidal")) return "Tidal";
        if (normalized.contains("cider")) return "Cider";
        if (normalized.contains("deezer")) return "Deezer";
        return source;
    }

    private void handlePlaybackResponse(HttpResponse<String> response) {
        if (response.statusCode() == 204) {
            hasPlayback = false;
            status = "Nothing playing";
            lastLoggedError = "";
            return;
        }
        if (response.statusCode() != 200) {
            String message = response.statusCode() == 401
                    ? "Spotify token expired or unauthorized"
                    : "Spotify API returned HTTP " + response.statusCode();
            reportError(message);
            status = response.statusCode() == 401 ? "Access token expired" : "Spotify API error";
            hasPlayback = false;
            return;
        }

        try {
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject item = root.getAsJsonObject("item");
            if (item == null || item.isJsonNull()) {
                hasPlayback = false;
                status = "Nothing playing";
                return;
            }
            String nextTrack = item.get("name").getAsString();
            mediaSource = "Spotify";
            JsonObject albumObject = item.getAsJsonObject("album");
            album = albumObject.get("name").getAsString();
            updateCoverUrl(albumObject);
            var artists = item.getAsJsonArray("artists");
            artist = artists.isEmpty() ? "" : artists.get(0).getAsJsonObject().get("name").getAsString();
            long nextProgress = root.has("progress_ms") && !root.get("progress_ms").isJsonNull()
                    ? root.get("progress_ms").getAsLong() : 0L;
            long nextDuration = item.get("duration_ms").getAsLong();
            boolean nextPlaying = root.get("is_playing").getAsBoolean();
            syncProgress(nextTrack, nextProgress, nextDuration, nextPlaying, System.nanoTime());
            track = nextTrack;
            hasPlayback = true;
            status = "";
            lastLoggedError = "";
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException error) {
            reportError("Spotify response could not be parsed: " + error.getClass().getSimpleName());
            status = "Invalid Spotify response";
            hasPlayback = false;
        }
    }

    private void updateCoverUrl(JsonObject albumObject) {
        var images = albumObject.getAsJsonArray("images");
        if (images == null || images.isEmpty()) {
            coverUrl = "";
            coverBytes = null;
            return;
        }
        JsonObject selected = null;
        int bestDifference = Integer.MAX_VALUE;
        for (var imageElement : images) {
            JsonObject image = imageElement.getAsJsonObject();
            if (!image.has("url")) {
                continue;
            }
            int width = image.has("width") && !image.get("width").isJsonNull()
                    ? image.get("width").getAsInt() : coverResolution.getInt();
            int difference = Math.abs(width - coverResolution.getInt());
            if (difference < bestDifference) {
                bestDifference = difference;
                selected = image;
            }
        }
        if (selected == null) {
            coverUrl = "";
            return;
        }
        String url = selected.get("url").getAsString();
        coverUrl = url;
        if (url.equals(requestedCoverUrl) || !coverRequestInFlight.compareAndSet(false, true)) {
            return;
        }
        requestedCoverUrl = url;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .whenComplete((response, error) -> {
                    try {
                        if (error != null) {
                            reportError("Spotify cover request failed: " + error.getClass().getSimpleName());
                            return;
                        }
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            reportError("Spotify cover request returned HTTP " + response.statusCode());
                            return;
                        }
                        if (url.equals(requestedCoverUrl)) {
                            coverBytes = response.body();
                            coverBytesUrl = url;
                        }
                    } finally {
                        coverRequestInFlight.set(false);
                    }
                });
    }

    private void triggerControl(long window, int index, KeybindSetting key, String action) {
        int code = key.getKeyCode();
        boolean down = code != 0 && GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
        if (down && !wasDown[index]) {
            sendControl(action);
        }
        wasDown[index] = down;
    }

    private void triggerSeek(long window, int index, KeybindSetting key, int direction) {
        int code = key.getKeyCode();
        boolean down = code != 0 && GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
        if (down && !wasDown[index]) {
            sendSeek(direction);
        }
        wasDown[index] = down;
    }

    private void triggerPageToggle(long window) {
        int code = pageToggleKey.getKeyCode();
        boolean down = code != 0 && GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
        if (down && !pageKeyWasDown) {
            overlayPage.select(overlayPage.getValue().equals("Track") ? "Lyrics" : "Track");
        }
        pageKeyWasDown = down;
    }

    private void sendSeek(int direction) {
        long step = (long) (seekStep.getFloat() * 1000f);
        long target = Math.max(0L, currentProgress() + direction * step);
        if (durationMillis > 0) {
            target = Math.min(target, durationMillis);
        }
        String token = accessToken.getValue().trim();
        if (token.isEmpty()) {
            WindowsMediaSession.seekTo(target);
        } else {
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("https://api.spotify.com/v1/me/player/seek?position_ms=" + target))
                    .timeout(Duration.ofSeconds(8))
                    .header("Authorization", "Bearer " + token)
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();
            HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(error -> {
                        reportError("Spotify seek failed: " + error.getClass().getSimpleName());
                        return null;
                    });
        }
        progressMillis = target;
        progressSyncedAtNanos = System.nanoTime();
    }

    private void syncProgress(String nextTrack, long nextProgress, long nextDuration,
                              boolean nextPlaying, long sampledAtNanos) {
        long nowNanos = System.nanoTime();
        if (sampledAtNanos > 0 && sampledAtNanos == lastProgressSampleNanos) {
            return;
        }
        if (sampledAtNanos > 0) {
            lastProgressSampleNanos = sampledAtNanos;
        }
        long syncAtNanos = sampledAtNanos > 0 ? sampledAtNanos : nowNanos;
        if (hasPlayback && playing && nextPlaying && track.equals(nextTrack)) {
            long expected = currentProgress();
            long lag = expected - nextProgress;
            long staleSampleLimitNanos = Math.max(2_000L, updateInterval.getInt() * 1_000L) * 1_000_000L;
            if (lag > 0 && lag <= staleSampleLimitNanos / 1_000_000L) {
                nextProgress = expected;
                syncAtNanos = nowNanos;
            }
        }
        durationMillis = Math.max(0L, nextDuration);
        progressMillis = Math.max(0L, Math.min(nextProgress, durationMillis > 0 ? durationMillis : Long.MAX_VALUE));
        playing = nextPlaying;
        progressSyncedAtNanos = syncAtNanos;
    }

    /** Progress including the time elapsed since the last media-session poll. */
    private long currentProgress() {
        long base = progressMillis;
        if (playing && progressSyncedAtNanos > 0) {
            base += Math.max(0L, System.nanoTime() - progressSyncedAtNanos) / 1_000_000L;
        }
        if (durationMillis > 0) {
            base = Math.min(base, durationMillis);
        }
        return base;
    }

    private void sendControl(String action) {
        String token = accessToken.getValue().trim();
        if (token.isEmpty()) {
            WindowsMediaSession.control(action.equals("play-pause") || action.equals("playpause")
                    ? "playpause" : action);
            if (action.equals("play-pause")) {
                playing = !playing;
            }
            return;
        }
        String method;
        String endpoint;
        switch (action) {
            case "previous" -> {
                method = "POST";
                endpoint = "previous";
            }
            case "next" -> {
                method = "POST";
                endpoint = "next";
            }
            default -> {
                method = "PUT";
                endpoint = playing ? "pause" : "play";
            }
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create("https://api.spotify.com/v1/me/player/" + endpoint))
                .timeout(Duration.ofSeconds(8))
                .header("Authorization", "Bearer " + token);
        HttpRequest request = method.equals("POST") ? builder.POST(HttpRequest.BodyPublishers.noBody()).build()
                : builder.PUT(HttpRequest.BodyPublishers.noBody()).build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        reportError("Spotify playback control returned HTTP " + response.statusCode());
                        status = "Spotify control unavailable";
                    } else {
                        playing = endpoint.equals("play") || endpoint.equals("next") || endpoint.equals("previous");
                    }
                })
                .exceptionally(error -> {
                    reportError("Spotify control failed: " + error.getClass().getSimpleName());
                    status = "Spotify control failed";
                    return null;
                });
    }

    private void reportError(String message) {
        if (!message.equals(lastLoggedError)) {
            System.err.println("[OctoClient] " + message);
            lastLoggedError = message;
        }
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        boolean activePlayback = hasPlayback;
        if (autoHide.isEnabled() && !activePlayback) {
            return;
        }
        String title = activePlayback ? track : (sourceSpotify.isEnabled() ? status
                : hasExternalSourceEnabled() ? "Selected source is not integrated" : "Spotify source disabled");
        boolean textVisible = showText.isEnabled();
        String artistLine = textVisible && activePlayback && showArtist.isEnabled() && !artist.isBlank() ? artist : null;
        String albumLine = textVisible && activePlayback && showAlbum.isEnabled() && !album.isBlank() ? album : null;
        String sourceLine = textVisible && nametagVisibility.isEnabled() ? mediaSource : null;
        String elapsedTime = formatTime(currentProgress());
        String trackLength = durationMillis > 0 ? formatTime(durationMillis) : "--:--";
        String timeLine = textVisible && activePlayback && showTimeLabels.isEnabled()
                ? elapsedTime + " / " + trackLength : null;
        boolean navigationVisible = showLyricsNavigation.isEnabled();
        boolean lyricsPage = navigationVisible && overlayPage.getValue().equals("Lyrics");
        String lyricLine = textVisible && lyricsPage && activePlayback
                ? SkyLyricsClient.currentLyricLine(track) : null;
        if (textVisible && lyricsPage && activePlayback && lyricLine == null) {
            lyricLine = "No lyric line right now";
        }
        int pad = 7;
        int lineHeight = ctx.textHeight() + 2;
        int navigationHeight = navigationVisible ? lineHeight + 4 : 0;
        boolean coverAvailable = showCover.isEnabled() && !coverUrl.isBlank() && activePlayback;
        int imageSize = coverAvailable ? (layout.getValue().equals("Cover") ? 52 : 36) : 0;
        String renderedTitle = textVisible
                ? (marquee.isEnabled()
                ? marqueeText(title, Math.max(4, (dynamicPadding.isEnabled() ? 220 : fixedWidth.getInt())
                        / Math.max(1, ctx.textWidth("M")) - 4))
                : title)
                : "";
        int textWidth = ctx.textWidth(renderedTitle);
        if (artistLine != null) {
            textWidth = Math.max(textWidth, ctx.textWidth(artistLine));
        }
        if (albumLine != null) {
            textWidth = Math.max(textWidth, ctx.textWidth(albumLine));
        }
        if (sourceLine != null) {
            textWidth = Math.max(textWidth, ctx.textWidth(sourceLine));
        }
        if (timeLine != null) {
            textWidth = Math.max(textWidth, ctx.textWidth(timeLine));
        }
        if (lyricLine != null) {
            textWidth = Math.max(textWidth, ctx.textWidth(lyricLine));
        }
        int lines = (renderedTitle.isEmpty() ? 0 : 1) + (sourceLine == null ? 0 : 1) + (artistLine == null ? 0 : 1)
                + (albumLine == null ? 0 : 1) + (timeLine == null ? 0 : 1) + (lyricLine == null ? 0 : 1);
        lines = Math.max(1, lines);
        int naturalWidth = Math.max(imageSize > 0 ? imageSize + pad * 2 : 110,
                textWidth + pad * 2 + (imageSize > 0 ? imageSize + pad : 0));
        int naturalHeight = pad * 2 + navigationHeight + Math.max(lines * lineHeight, imageSize)
                + (activePlayback && showProgress.isEnabled() ? 4 : 0);
        int panelWidth = dynamicPadding.isEnabled() ? naturalWidth : fixedWidth.getInt();
        int panelHeight = dynamicPadding.isEnabled() ? naturalHeight : fixedHeight.getInt();
        width = panelWidth;
        height = panelHeight;
        drawPanel(ctx, (int) x, (int) y, panelWidth, panelHeight, corners.getInt(),
                albumColor.getValue(), 0.94f, 0f, 70);
        ensureCoverTexture(ctx);

        float contentY = y + pad;
        if (navigationVisible) {
            float navY = y + pad;
            float tabWidth = (panelWidth - pad * 2f) / 2f;
            boolean lyricsSelected = lyricsPage;
            ctx.fill(x + pad, navY, x + pad + tabWidth, navY + lineHeight + 2,
                    Theme.withAlpha(lyricsSelected ? Theme.TEXT_FAINT : Theme.ACCENT_START, 110));
            ctx.fill(x + pad + tabWidth, navY, x + panelWidth - pad, navY + lineHeight + 2,
                    Theme.withAlpha(lyricsSelected ? Theme.ACCENT_START : Theme.TEXT_FAINT, 110));
            ctx.drawText("TRACK", x + pad + (tabWidth - ctx.textWidth("TRACK")) / 2f, navY + 1,
                    lyricsSelected ? Theme.TEXT_SECONDARY : Theme.TEXT_PRIMARY, false);
            ctx.drawText("LYRICS", x + pad + tabWidth + (tabWidth - ctx.textWidth("LYRICS")) / 2f, navY + 1,
                    lyricsSelected ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);
            contentY += navigationHeight;
        }

        int line = 0;
        float textX = x + pad + imageSize + (imageSize > 0 ? pad : 0);
        if (layout.getValue().equals("Centered")) {
            int textAreaWidth = panelWidth - pad * 2 - (imageSize > 0 ? imageSize + pad : 0);
            textX += Math.max(0, (textAreaWidth - ctx.textWidth(renderedTitle)) / 2f);
        }
        int textColor = activePlayback ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
        if (imageSize > 0 && coverTexture != null && registeredCoverUrl.equals(coverUrl)) {
            ctx.gfx.blit(coverTexture, (int) x + pad, (int) contentY, imageSize, imageSize, 0, 0, 1, 1);
        }
        if (textVisible) {
            ctx.enableScissor(textX, contentY, x + panelWidth - pad, y + panelHeight - pad);
            try {
                if (!renderedTitle.isEmpty()) {
                    ctx.drawText(renderedTitle, textX, contentY + line * lineHeight, textColor, false);
                    line++;
                }
                if (sourceLine != null) {
                    ctx.drawText(sourceLine, textX, contentY + line++ * lineHeight, Theme.TEXT_FAINT, false);
                }
                if (artistLine != null) {
                    ctx.drawText(artistLine, textX, contentY + line++ * lineHeight, Theme.TEXT_SECONDARY, false);
                }
                if (albumLine != null) {
                    ctx.drawText(albumLine, textX, contentY + line++ * lineHeight, Theme.TEXT_FAINT, false);
                }
                if (lyricLine != null) {
                    ctx.drawText(lyricLine, textX, contentY + line++ * lineHeight, Theme.TEXT_PRIMARY, false);
                }
                if (timeLine != null) {
                    ctx.drawText(timeLine, textX, contentY + line * lineHeight, Theme.TEXT_FAINT, false);
                }
            } finally {
                ctx.disableScissor();
            }
        }
        if (activePlayback && showProgress.isEnabled() && durationMillis > 0) {
            int barY = (int) y + panelHeight - 4;
            int barW = (int) Math.round((panelWidth - pad * 2)
                    * Math.min(1, Math.max(0, currentProgress() / (double) durationMillis)));
            ctx.fill(x + pad, barY, x + panelWidth - pad, barY + 2, Theme.withAlpha(Theme.TEXT_FAINT, 100));
            ctx.fill(x + pad, barY, x + pad + barW, barY + 2, Theme.ACCENT_START);
        }
    }

    private String marqueeText(String value, int maxCharacters) {
        if (value.length() <= maxCharacters) {
            return value;
        }
        String scrolling = value + "     " + value;
        int offset = (int) ((System.currentTimeMillis() / 250L) % (value.length() + 5));
        return scrolling.substring(offset, Math.min(scrolling.length(), offset + maxCharacters));
    }

    private String formatTime(long millis) {
        long seconds = Math.max(0, millis / 1000);
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private boolean hasExternalSourceEnabled() {
        return sourceAppleMusic.isEnabled() || sourceYouTube.isEnabled() || sourceAmazonMusic.isEnabled()
                || sourceSoundCloud.isEnabled() || sourceTidal.isEnabled() || sourceCider.isEnabled()
                || sourceDeezer.isEnabled() || allowOther.isEnabled();
    }

    private void ensureCoverTexture(HudDrawContext ctx) {
        byte[] imageBytes = coverBytes;
        String url = coverUrl;
        if (imageBytes == null || imageBytes.length == 0 || url.isEmpty()
                || !url.equals(coverBytesUrl) || url.equals(registeredCoverUrl)) {
            return;
        }
        try {
            byte[] pngBytes = normalizeCoverImage(imageBytes);
            try (ByteArrayInputStream input = new ByteArrayInputStream(pngBytes)) {
                NativeImage image = NativeImage.read(input);
                coverTexture = Identifier.fromNamespaceAndPath("octoclient", "spotify_cover");
                ctx.mc.getTextureManager().register(coverTexture,
                        new DynamicTexture(() -> "OctoClient Spotify cover", image));
                registeredCoverUrl = url;
            }
        } catch (IOException error) {
            rejectedCoverUrl = url;
            coverBytes = null;
            coverBytesUrl = "";
            coverUrl = "";
            coverTexture = null;
            registeredCoverUrl = "";
            reportError("Album artwork could not be decoded: " + error.getMessage());
        }
    }

    private static byte[] normalizeCoverImage(byte[] bytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new IOException("Unsupported image format");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", output)) {
                throw new IOException("PNG encoder is unavailable");
            }
            return output.toByteArray();
        } finally {
            image.flush();
        }
    }
}
