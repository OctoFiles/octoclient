package dev.octoclient.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.octoclient.module.cosmetics.CosmeticCatalog;
import dev.octoclient.network.CosmeticLoadout;
import dev.octoclient.network.CosmeticSyncClient;
import dev.octoclient.module.cosmetics.CosmeticPreviewState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared helper for catalog shield skins replacing the vanilla shield model. */
public final class ShieldCosmetics {
    private static final ThreadLocal<FirstPersonShield> FIRST_PERSON = new ThreadLocal<>();
    private static final ThreadLocal<CosmeticCatalog.Entry> THIRD_PERSON = new ThreadLocal<>();
    private static final boolean DEBUG_SHIELDS = Boolean.getBoolean("octoclient.debugShields");
    private static boolean debugLogged;

    private ShieldCosmetics() {
    }

    /** True when the loadout has a selected SHIELD catalog cosmetic. */
    public static boolean hasShieldSkin(CosmeticLoadout loadout) {
        if (loadout == null || loadout.catalogSelection().isEmpty()) {
            return false;
        }
        return CosmeticCatalog.fromSelection(loadout.catalogSelection()).stream()
                .anyMatch(entry -> entry.type().equals("SHIELD"));
    }

    /** The selected catalog shield for an entity, or {@code null} when none is equipped. */
    public static CosmeticCatalog.Entry selectedShield(LivingEntity entity) {
        Minecraft client = Minecraft.getInstance();
        CosmeticLoadout loadout = entity == client.player ? CosmeticSyncClient.localLoadout()
                : entity instanceof Player player ? CosmeticSyncClient.remoteLoadout(player.getUUID())
                : CosmeticLoadout.EMPTY;
        return CosmeticCatalog.fromSelection(loadout.catalogSelection()).stream()
                .filter(entry -> entry.type().equals("SHIELD"))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remembers the shield skin of the player currently being rendered, so the
     * vanilla shield layer can be cancelled while the catalog layer draws the
     * replacement on the same arm.
     */
    public static void beginEntityRender(AvatarRenderState state) {
        CosmeticLoadout loadout;
        if (state instanceof CosmeticPreviewState preview && preview.octoclient$isCosmeticPreview()) {
            loadout = preview.octoclient$getCosmeticPreviewLoadout();
        } else {
            loadout = loadoutFor(state.id);
        }
        CosmeticCatalog.Entry entry = loadout == null ? null : CosmeticCatalog.fromSelection(loadout.catalogSelection())
                .stream()
                .filter(candidate -> candidate.type().equals("SHIELD"))
                .findFirst()
                .orElse(null);
        if (entry == null) {
            THIRD_PERSON.remove();
        } else {
            THIRD_PERSON.set(entry);
        }
    }

    public static void endEntityRender() {
        THIRD_PERSON.remove();
    }

    private static CosmeticLoadout loadoutFor(int entityId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && entityId == client.player.getId()) {
            return CosmeticSyncClient.localLoadout();
        }
        if (client.level == null) {
            return CosmeticLoadout.EMPTY;
        }
        Entity entity = client.level.getEntity(entityId);
        return entity instanceof Player player ? CosmeticSyncClient.remoteLoadout(player.getUUID())
                : CosmeticLoadout.EMPTY;
    }

    /**
     * Suppresses the vanilla third-person shield item layer when a catalog shield
     * is selected. The replacement is drawn directly on the arm by the catalog
     * cosmetic layer so it sits flush with the arm.
     */
    public static boolean submitThirdPersonShield(SpecialModelRenderer renderer, PoseStack pose,
                                                  SubmitNodeCollector collector, int light) {
        CosmeticCatalog.Entry entry = THIRD_PERSON.get();
        return renderer instanceof ShieldSpecialRenderer && entry != null;
    }

    /**
     * Legacy helper: third-person shields are now replaced in the item render
     * layer so the mesh stays aligned with the held-item pose.
     */
    public static boolean replacesThirdPersonShield(LivingEntity entity, ItemStack stack,
                                                    ItemDisplayContext context) {
        return false;
    }

    /**
     * Remembers the first-person shield skin for the current item render. The
     * item transform is applied later, while the render state submits its layer,
     * so the mesh is drawn from a mixin on that layer instead of here.
     */
    public static void beginItemRender(LivingEntity entity, ItemStack stack, ItemDisplayContext context) {
        if (DevCapture.hideShield()) {
            FIRST_PERSON.remove();
            return;
        }
        if (stack.is(Items.SHIELD) && isFirstPerson(context)) {
            CosmeticCatalog.Entry entry = selectedShield(entity);
            if (entry != null) {
                FIRST_PERSON.set(new FirstPersonShield(entry));
                if (DEBUG_SHIELDS && !debugLogged) {
                    debugLogged = true;
                    System.out.println("[OctoClient] first-person shield selected: " + entry.key()
                            + " context=" + context);
                }
                return;
            }
        }
        FIRST_PERSON.remove();
    }

    public static void endItemRender() {
        FIRST_PERSON.remove();
    }

    /**
     * Round catalog shields need the opposite side-angle sign in first person so
     * the authored design faces the player instead of showing the mesh edge-on.
     */
    public static boolean shouldMirrorFirstPersonSideAngle() {
        FirstPersonShield render = FIRST_PERSON.get();
        return render != null && CatalogCosmeticRenderer.isRoundShield(render.entry);
    }

    /**
     * Submits the remembered catalog shield in the already transformed item pose.
     * Returns {@code true} when the vanilla layer must be cancelled. Later layers
     * are cancelled too, but the mesh is only submitted once.
     */
    public static boolean submitFirstPersonShield(PoseStack pose, SubmitNodeCollector collector, int light) {
        FirstPersonShield render = FIRST_PERSON.get();
        if (render == null) {
            return false;
        }
        if (render.submitted) {
            return render.handled;
        }
        render.submitted = true;
        render.handled = CatalogCosmeticRenderer.renderItemShield(render.entry, pose, collector, light, true);
        if (DEBUG_SHIELDS) {
            System.out.println("[OctoClient] first-person shield submit: " + render.entry.key()
                    + " handled=" + render.handled);
        }
        return render.handled;
    }

    private static boolean isFirstPerson(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    private static final class FirstPersonShield {
        private final CosmeticCatalog.Entry entry;
        private boolean submitted;
        private boolean handled;

        private FirstPersonShield(CosmeticCatalog.Entry entry) {
            this.entry = entry;
        }
    }
}
