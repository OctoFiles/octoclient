package dev.octoclient.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server independent cosmetic sharing through a small HTTP API.
 *
 * <p>The custom-payload relay only works when the Minecraft server itself runs
 * OctoClient, which rules out every normal server. This talks to a tiny
 * standalone service instead: every client uploads its own loadout under its
 * UUID and asks for the loadouts of the players around it, so other OctoClient
 * users are dressed correctly no matter where you play.</p>
 *
 * <p>Configured by {@code config/octoclient_api.json}. A missing file is created
 * pointing at {@code http://127.0.0.1:8765}, which is only reachable when the
 * service actually runs on this machine.</p>
 */
public final class CosmeticApi {
    /** Official public relay used when the user has not configured an endpoint. */
    private static final String DEFAULT_API_URL = "https://octoclient1.pythonanywhere.com";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir()
            .resolve("octoclient_api.json");

    /** Loadouts fetched from the API, per player. */
    private static final Map<UUID, CosmeticLoadout> CACHE = new ConcurrentHashMap<>();
    /** When each UUID was last requested, in milliseconds. */
    private static final Map<UUID, Long> FETCHED_AT = new ConcurrentHashMap<>();
    private static final AtomicBoolean POLLING = new AtomicBoolean();

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static boolean loaded;
    private static boolean enabled;
    private static String baseUrl = "";
    private static String token = "";
    private static int pollSeconds = 1;

    private static CosmeticLoadout lastPosted;
    private static long lastPostAt;
    private static long lastPollAt;
    private static long lastWarnAt;

    private CosmeticApi() {
    }

    /** Loadout another player published, or {@code null} when none is known. */
    public static CosmeticLoadout cached(UUID playerId) {
        return CACHE.get(playerId);
    }

    /** Drops every fetched loadout, e.g. after leaving a world. */
    public static void reset() {
        CACHE.clear();
        FETCHED_AT.clear();
        lastPosted = null;
        lastPollAt = 0;
    }

    /** Called once per client tick from {@link CosmeticSyncClient}. */
    public static void tick(Minecraft client) {
        load();
        if (!enabled || baseUrl.isEmpty()) {
            return;
        }
        if (client.player == null || client.level == null) {
            lastPosted = null;
            return;
        }
        long now = System.currentTimeMillis();

        CosmeticLoadout local = CosmeticSyncClient.localLoadout();
        if (!local.equals(lastPosted) || now - lastPostAt > 60_000L) {
            lastPosted = local;
            lastPostAt = now;
            post(client, local);
        }

        long interval = Math.max(2, pollSeconds) * 1000L;
        if (now - lastPollAt < interval) {
            return;
        }
        lastPollAt = now;

        List<UUID> missing = new ArrayList<>();
        long staleAfter = Math.max(interval, 10_000L);
        for (AbstractClientPlayer player : client.level.players()) {
            UUID id = player.getUUID();
            if (id.equals(client.player.getUUID())) {
                continue;
            }
            Long fetched = FETCHED_AT.get(id);
            if (fetched == null || now - fetched > staleAfter) {
                missing.add(id);
            }
        }
        if (!missing.isEmpty()) {
            fetch(missing);
        }
    }

    // ------------------------------------------------------------------ http

    private static void post(Minecraft client, CosmeticLoadout loadout) {
        String playerId = client.player.getUUID().toString();
        JsonObject body = new JsonObject();
        body.addProperty("uuid", playerId);
        body.addProperty("name", client.player.getName().getString());
        String encoded = loadout.encodeLegacy();
        body.addProperty("loadout", encoded);

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/loadouts"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        authorize(request);

        HTTP.sendAsync(request.build(), HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        warn("upload failed: " + error.getMessage());
                    } else if (response.statusCode() / 100 != 2) {
                        warn("upload rejected with HTTP " + response.statusCode());
                    } else {
                        System.out.println("[OctoClient] Cosmetics API: uploaded " + encoded
                                + " for " + playerId);
                    }
                });
    }

    private static void fetch(List<UUID> ids) {
        if (!POLLING.compareAndSet(false, true)) {
            return;
        }
        StringBuilder query = new StringBuilder();
        for (UUID id : ids) {
            query.append(query.isEmpty() ? "" : "&").append("uuid=")
                    .append(URLEncoder.encode(id.toString(), StandardCharsets.UTF_8));
        }
        HttpRequest request;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(
                            URI.create(baseUrl + "/v1/loadouts?" + query))
                    .timeout(Duration.ofSeconds(8))
                    .GET();
            authorize(builder);
            request = builder.build();
        } catch (IllegalArgumentException exception) {
            POLLING.set(false);
            warn("invalid API url: " + baseUrl);
            return;
        }

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .whenComplete((response, error) -> {
                    POLLING.set(false);
                    if (error != null) {
                        warn("lookup failed: " + error.getMessage());
                        return;
                    }
                    if (response.statusCode() / 100 != 2) {
                        warn("lookup rejected with HTTP " + response.statusCode());
                        return;
                    }
                    apply(response.body(), ids);
                });
    }

    private static void apply(String body, List<UUID> requested) {
        Map<UUID, CosmeticLoadout> found = new LinkedHashMap<>();
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonObject loadouts = root.getAsJsonObject("loadouts");
            if (loadouts != null) {
                for (Map.Entry<String, JsonElement> entry : loadouts.entrySet()) {
                    UUID id;
                    try {
                        id = UUID.fromString(entry.getKey());
                    } catch (IllegalArgumentException exception) {
                        continue;
                    }
                    CosmeticLoadout loadout = CosmeticLoadout.decode(entry.getValue().getAsString());
                    if (!loadout.isEmpty()) {
                        found.put(id, loadout);
                    }
                }
            }
        } catch (RuntimeException exception) {
            warn("bad API response: " + exception.getMessage());
            return;
        }
        long now = System.currentTimeMillis();
        for (UUID id : requested) {
            // Mark as asked either way: an unknown player would otherwise be
            // requested again on every poll.
            FETCHED_AT.put(id, now);
            CosmeticLoadout loadout = found.get(id);
            if (loadout == null) {
                CACHE.remove(id);
            } else {
                CACHE.put(id, loadout);
            }
        }
        if (!found.isEmpty()) {
            System.out.println("[OctoClient] Cosmetics API: fetched " + found.size()
                    + " of " + requested.size() + " player loadout(s)");
        }
    }

    private static void authorize(HttpRequest.Builder request) {
        if (!token.isEmpty()) {
            request.header("Authorization", "Bearer " + token);
        }
    }

    private static void warn(String message) {
        long now = System.currentTimeMillis();
        if (now - lastWarnAt < 60_000L) {
            return;
        }
        lastWarnAt = now;
        System.err.println("[OctoClient] Cosmetics API: " + message);
    }

    // ---------------------------------------------------------------- config

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            if (!Files.exists(CONFIG)) {
                JsonObject defaults = new JsonObject();
                defaults.addProperty("enabled", true);
                defaults.addProperty("url", DEFAULT_API_URL);
                defaults.addProperty("token", "");
                defaults.addProperty("pollSeconds", 1);
                Files.createDirectories(CONFIG.getParent());
                Files.writeString(CONFIG, GSON.toJson(defaults));
                enabled = true;
                baseUrl = DEFAULT_API_URL;
                System.out.println("[OctoClient] Cosmetics API: using default public relay " + baseUrl);
                return;
            }
            JsonObject root = GSON.fromJson(Files.readString(CONFIG), JsonObject.class);
            enabled = !root.has("enabled") || root.get("enabled").getAsBoolean();
            baseUrl = normalize(root.has("url") ? root.get("url").getAsString() : "");
            token = root.has("token") ? root.get("token").getAsString().trim() : "";
            pollSeconds = root.has("pollSeconds") ? root.get("pollSeconds").getAsInt() : 1;
            // Earlier builds generated localhost as a placeholder. Upgrade that
            // untouched default automatically, so users do not need to edit JSON.
            if ("http://127.0.0.1:8765".equals(baseUrl) && token.isEmpty()) {
                baseUrl = DEFAULT_API_URL;
                root.addProperty("url", DEFAULT_API_URL);
                Files.writeString(CONFIG, GSON.toJson(root));
            }
            if (pollSeconds == 5) {
                pollSeconds = 1;
                root.addProperty("pollSeconds", 1);
                Files.writeString(CONFIG, GSON.toJson(root));
            }
            if (enabled) {
                System.out.println("[OctoClient] Cosmetics API: " + baseUrl
                        + " (config: " + CONFIG.toAbsolutePath() + ")");
            }
        } catch (IOException | RuntimeException exception) {
            enabled = false;
            System.err.println("[OctoClient] Cosmetics API config unreadable: " + exception.getMessage());
        }
    }

    private static String normalize(String url) {
        String value = url == null ? "" : url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
