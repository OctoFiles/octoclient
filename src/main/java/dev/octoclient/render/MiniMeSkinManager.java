package dev.octoclient.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Resolves Minecraft usernames to cached skin textures for MiniMe cosmetics. */
public final class MiniMeSkinManager {
    private static final ConcurrentMap<String, Identifier> SKINS = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private MiniMeSkinManager() {
    }

    public static Identifier textureFor(String username) {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            return null;
        }
        String key = name.toLowerCase(Locale.ROOT);
        if (!name.matches("[A-Za-z0-9_]{1,16}")) {
            if (FAILED.add(key)) {
                System.err.println("[OctoClient] Invalid MiniMe Minecraft username: " + name);
            }
            return null;
        }

        Identifier skin = SKINS.get(key);
        if (skin != null || FAILED.contains(key) || !LOADING.add(key)) {
            return skin;
        }

        Minecraft client = Minecraft.getInstance();
        CompletableFuture.supplyAsync(() -> client.services().profileResolver().fetchByName(name))
                .thenCompose(profile -> profile
                        .map(client.getSkinManager()::get)
                        .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())))
                .whenComplete((result, error) -> {
                    LOADING.remove(key);
                    if (error != null) {
                        fail(key, name, "could not load profile skin", error);
                    } else if (result == null || result.isEmpty() || result.get().body() == null) {
                        fail(key, name, "no skin found for Minecraft username", null);
                    } else {
                        PlayerSkin playerSkin = result.get();
                        SKINS.put(key, playerSkin.body().texturePath());
                    }
                });
        return null;
    }

    private static void fail(String key, String username, String reason, Throwable error) {
        FAILED.add(key);
        System.err.println("[OctoClient] MiniMe skin " + reason + " for " + username
                + (error == null ? "" : ": " + error));
    }
}
