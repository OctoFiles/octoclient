package dev.octoclient.ui;

import dev.octoclient.config.ConfigManager;
import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleCategory;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.secret.SecretTabUnlock;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ActionSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.Setting;
import dev.octoclient.module.setting.TextSetting;
import dev.octoclient.module.cosmetics.CosmeticKind;
import dev.octoclient.module.cosmetics.CosmeticPart;
import dev.octoclient.module.cosmetics.CosmeticPreviewState;
import dev.octoclient.module.cosmetics.Accessory;
import dev.octoclient.module.cosmetics.Aura;
import dev.octoclient.module.cosmetics.Bandana;
import dev.octoclient.module.cosmetics.CatalogCosmetic;
import dev.octoclient.module.cosmetics.Cape;
import dev.octoclient.module.cosmetics.Halo;
import dev.octoclient.module.cosmetics.Hat;
import dev.octoclient.module.cosmetics.HatDesign;
import dev.octoclient.module.cosmetics.Wings;
import dev.octoclient.network.CosmeticLoadout;
import dev.octoclient.module.settings.ClientSettings;
import dev.octoclient.module.hud.HudDrawContext;
import dev.octoclient.module.hud.HudModule;
import dev.octoclient.util.Anim;
import dev.octoclient.util.Draw;
import dev.octoclient.util.UiFonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * Liquid-glass dashboard — a 1:1 port of the web preview in {@code web/app.js}.
 *
 * <p>The whole screen lives in a fixed 996x560 design space, scaled by
 * {@code min(width/996, height/560)}. The classic three-panel shell puts the
 * nav column (118..222), the module sheet (230..685) and the docked inspector
 * (696..882) side by side as separate sheets, the way the dashboard looked
 * before the single-window glass rebuild.</p>
 *
 * <p>Animations are driven by real per-frame delta seconds and the same
 * exponential approach / spring integration the web uses, so they settle in
 * the same time at any frame rate.</p>
 */
public class DashboardScreen extends Screen {
    private static final Identifier TRANSPARENT_PREVIEW_TEXTURE =
            Identifier.fromNamespaceAndPath("octoclient", "textures/misc/cosmetic_preview_transparent.png");
    private static final Identifier LOGO_TEXTURE =
            Identifier.fromNamespaceAndPath("octoclient", "textures/misc/logo.png");
    // ------------------------------------------------------------ design space
    private static final float VW = 996f;
    private static final float VH = 560f;

    // Launcher layout, 1:1: header with wordmark/version/profile, an icon rail
    // on the left, the module content floating on the backdrop and the inspector
    // as the "your account" card on the right. WIN_* is the content column.
    private static final int WIN_X = 128;
    private static final int WIN_Y = 60;
    private static final int WIN_W = 556;
    private static final int WIN_H = 440;

    private static final int SIDE_X = 16;
    private static final int SIDE_Y = 60;
    private static final int SIDE_W = 96;
    private static final int SIDE_H = WIN_H;
    /** Rail tile: 40 px icon square with the label under it. */
    private static final float SIDE_ROW_H = 52f;
    private static final float SIDE_ROW_STEP = 56f;
    private static final float RAIL_PAD = 10f;

    private static final int SET_W = 252;
    private static final int SET_X = 722;
    private static final int SET_Y = 60;
    private static final int SET_H = WIN_H;
    private static final int SET_PAD = 12;
    /** Inspector cards sit inset from the pane edge, separated like the launcher panes. */
    private static final float SET_CARD_INSET = 6f;
    /** Height of the module header card (name, arrows, description). */
    private static final float HEADER_CARD_H = 76f;
    /** Collapsible group header: label left, chevron right. */
    private static final float SEC_HEADER_H = 20f;
    private static final float SEC_PAD_X = 8f;
    private static final float SEC_PAD_Y = 5f;
    /** Gap between two inspector cards, so their rounded corners stay apart. */
    private static final float SEC_GAP = 6f;
    /** Row height of one entry in an open mode-setting option list. */
    private static final float MODE_ITEM_H = 20f;
    /** Inspector header: step through the tab's modules, left of the close box. */
    private static final float PREV_X = SET_X + SET_W - 80f;
    private static final float NEXT_X = SET_X + SET_W - 58f;

    private static final int CARD_H = 38;
    private static final int COLUMNS = 2;
    private static final int GAP = 8;
    /** Vertical gap between card rows; tighter than the column gap. */
    private static final int CARD_GAP_Y = 8;
    /** Default card rounding; each module can override it with its Radius setting. */
    private static final int CARD_R = 10;
    private static final int CARD_PAD = 12;
    private static final float CARD_LIFT = 2f;

    /** Height of the cosmetics sub-tab chips and the gap below them. */
    private static final float SUBTAB_H = 22f;
    private static final float SUBTAB_GAP = 6f;
    private static final float SUBTAB_PAD = 11f;

    private static final float PREVIEW_GAP = 12f;
    private static final float MODULE_PREVIEW_H = 86f;

    /** Module grid: under the headline, above the chip row at the bottom. */
    private static final int AX = WIN_X;                                          // 128
    private static final int AY = WIN_Y + 78;                                     // 138
    private static final int AW = WIN_W - 14;                                     // 542
    private static final int AH = WIN_Y + WIN_H - 46 - AY;                        // 356

    /** Search pill, top right of the content column like the launcher's button. */
    private static final int SEARCH_W = 200;
    private static final int SEARCH_H = 24;
    private static final int SEARCH_Y = WIN_Y + 6;                                // 66

    private static final float INTRO_CX = (SIDE_X + SET_X + SET_W) / 2f;           // 500
    private static final float INTRO_CY = WIN_Y + WIN_H / 2f;                      // 282

    // Minecraft size units: 1 = the vanilla 8 px glyphs. The UI font sheet is
    // filtered linearly, so the in-between sizes below stay readable instead of
    // stair-stepping, and the whole dashboard gains a step of text size.
    private static final float S_NAME = 1f;
    private static final float S_DESC = 0.85f;
    private static final float S_LABEL = 1f;
    /** Glyph advances are whole pixels, so the measured width is the real one. */
    private static final float WIDTH_SAFETY = 1.0f;
    private static final float S_TEXT = 1f;

    private static final int WHITE = 0xFFFFFF;
    private static final int ACCENT = 0xB9C3FF;

    // Launcher palette: pale glass panes over the blurred world, white type and
    // a lavender accent, matching the OctoClient launcher window.
    private static final int INK = 0xFFF7F8FB;
    private static final int INK_SOFT = 0xFFE4E7EF;
    private static final int INK_MUTED = 0xFFAEB4C3;
    private static final int INK_FAINT = 0xFF868C9C;
    private static final int TILE_FILL = 0x1AFFFFFF;
    private static final int TILE_RIM = 0x26FFFFFF;
    /** Pane tint pushed into the glass shader; the body stays translucent. */
    private static final int PANE_TINT = 0xE9EDF7;

    /** Cached tab icons (ItemStack creation touches the registry, so keep one set). */
    private static final EnumMap<ModuleCategory, ItemStack> CATEGORY_ICONS = new EnumMap<>(ModuleCategory.class);

    // ------------------------------------------------------------------- state
    private float ui = 1f;
    private float offX;
    private float offY;

    // Static: the category survives closing and reopening the dashboard, so
    // the menu comes back on the tab it was left on.
    private static ModuleCategory category = ModuleCategory.HUD;
    /** Cosmetics sub-tab (Capes / Wings / Hats / Aura); {@code null} = all. */
    private static CosmeticKind cosmeticFilter;
    private static CosmeticSection cosmeticSection = CosmeticSection.ALL;
    private final boolean cosmeticsOnly;
    private String search = "";
    private boolean searchFocused;
    /** Ctrl+A in the search field: the next character or Backspace replaces everything. */
    private boolean searchSelectAll;

    /** Cosmetic preview rotation, changed by dragging with the right mouse button. */
    private float previewYaw;
    private float previewPitch;
    private boolean previewDragging;
    private double previewDragX;
    private double previewDragY;
    private Module popupModule;
    /** Option list of a mode setting, open in the inspector; null = closed. */
    private ModeSetting openMode;
    private float popupProgress;
    private boolean popupClosing;
    private TextSetting editingText;
    private KeybindSetting capturingKeybind;
    /** Key presses consumed while binding also produce a char event; swallow it. */
    private long swallowCharUntil;
    private float popupScroll;
    /** Which inspector groups are expanded, per module; absent = all collapsed. */
    private final Map<Module, EnumSet<SettingSection>> openSections = new IdentityHashMap<>();
    /** Total height of the inspector's cards, filled in by {@link #popupSections()}. */
    private float popupContentH;

    private float scroll;
    private float scrollTarget;

    private float mx = -1f;
    private float my = -1f;
    private boolean inside;

    private float time;
    private float dt = 1f / 60f;
    private long lastFrameNanos;

    private float intro;
    private float catAnim;
    private float focusA;
    private float indY = -1f;
    private final float[] catLabelA = new float[ModuleCategory.values().length];

    private final Map<Module, CardAnim> cardAnims = new HashMap<>();
    private final Map<Setting<?>, SetAnim> setAnims = new IdentityHashMap<>();
    private final Map<ColorSetting, Boolean> colorOpen = new IdentityHashMap<>();
    private List<Module> visibleModulesCache;
    private List<CardRect> cardBases = List.of();
    private float gridHeight;
    private final List<CardRect> visibleCardRects = new ArrayList<>();
    private ModuleCategory cachedCategory;
    private CosmeticKind cachedCosmeticFilter;
    private String cachedSearch;
    private boolean cachedSecretUnlocked;
    private boolean cachedCosmeticsOnly;
    private CosmeticSection cachedCosmeticSection;
    private boolean cardLayoutDirty = true;
    private float cachedCardsTop = Float.NaN;
    private float cachedContentW = Float.NaN;
    private boolean cachedPopupSettings;
    private Module expandedModule;
    private int expandedIndex = -1;

    private NumberSetting draggedSlider;
    private ColorSetting draggedColor;
    private int draggedColorChannel = -1;
    private boolean draggingScrollbar;
    private float scrollbarGrab;
    /** The inspector has its own scrollbar, independent of the card grid. */
    private boolean draggingPopupScrollbar;
    private float popupScrollbarGrab;
    private boolean layoutLogged;
    /** Design-space x shift that keeps the visible shell centred. */
    private float layoutShiftX;

    public DashboardScreen() {
        this(false);
    }

    protected DashboardScreen(boolean cosmeticsOnly) {
        super(Component.literal("OctoClient"));
        this.cosmeticsOnly = cosmeticsOnly;
    }

    @Override
    public void init() {
        super.init();
        if (cosmeticsOnly) {
            category = ModuleCategory.COSMETICS;
        } else if (category == ModuleCategory.COSMETICS) {
            category = ModuleCategory.HUD;
        }
        if (minecraft != null && minecraft.gui != null) {
            minecraft.gui.toastManager().clear();
        }

        // Dev-only: -Doctoclient.demoOpen="<module>" opens that module's
        // selected settings presentation for a capture without mouse input.
        String demo = System.getProperty("octoclient.demoOpen");
        if (demo != null && !demo.isEmpty()) {
            // A category id or title ("secret", "???") selects just the tab,
            // which is what the card-grid captures need.
            for (ModuleCategory c : ModuleCategory.values()) {
                if (demo.equalsIgnoreCase(c.id) || demo.equalsIgnoreCase(c.title)) {
                    category = c;
                }
            }
            for (Module m : ModuleManager.get().getModules()) {
                if (!m.getName().equalsIgnoreCase(demo)) {
                    continue;
                }

                category = m.getCategory();
                // Set the inspector directly: init() can run more than once
                // (window resize), and openSettings() would toggle it shut.
                popupModule = m;
                popupClosing = false;
                popupScroll = 0f;
                expandedModule = null;
                expandedIndex = -1;
                for (Setting<?> s : visibleSettings(m)) {
                    if (s instanceof ColorSetting c) {
                        colorOpen.put(c, true);
                    }
                }
            }
            refreshLayout();
            // Dev-only: the capture log then proves which cards the tab shows.
            StringBuilder names = new StringBuilder();
            for (Module m : ModuleManager.get().getModules()) {
                if (m.getCategory() == category) {
                    names.append(m.getName()).append(' ');
                }
            }
            System.out.println("[OctoClient] demo category=" + category.id + " modules=" + names.toString().trim());
        }
        // Dev-only: -Doctoclient.demoMode opens the first mode setting's option
        // list of the shown module, so the dropdown can be captured.
        if (Boolean.getBoolean("octoclient.demoMode") && popupModule != null) {
            for (Setting<?> s : visibleSettings(popupModule)) {
                if (s instanceof ModeSetting mode) {
                    openMode = mode;
                    expandSection(popupModule, SettingSection.of(mode));
                    break;
                }
            }
        }
        // Dev-only: -Doctoclient.demoExpand opens every settings group of the
        // shown module, so the expanded cards can be captured.
        if (Boolean.getBoolean("octoclient.demoExpand") && popupModule != null) {
            for (SettingSection section : SettingSection.values()) {
                expandSection(popupModule, section);
            }
        }
        // Dev-only: -Doctoclient.demoScroll=<px> prescrolls the inspector so a
        // capture can show controls below the fold without synthetic input.
        int demoScroll = Integer.getInteger("octoclient.demoScroll", 0);
        if (demoScroll > 0 && popupModule != null) {
            popupScroll = Math.min(demoScroll, popupMaxScroll());
        }
        // Dev-only: preselect a cosmetic sub-tab so captures can show the
        // contents of e.g. -Doctoclient.demoCosmeticKind=SHIELD without input.
        String demoKind = System.getProperty("octoclient.demoCosmeticKind");
        if (demoKind != null && !demoKind.isEmpty()) {
            for (CosmeticKind kind : CosmeticKind.values()) {
                if (kind.name().equalsIgnoreCase(demoKind)) {
                    category = ModuleCategory.COSMETICS;
                    cosmeticFilter = kind;
                    scroll = 0f;
                    scrollTarget = 0f;
                    break;
                }
            }
            refreshLayout();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------- frame

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        ui = Math.max(0.35f, Math.min(width / VW, height / VH) * ClientSettings.dashboardScale());
        offX = (width - VW * ui) / 2f;
        offY = (height - VH * ui) / 2f;

        dt = frameDelta();
        // The inspector only takes room while it is shown, so the cluster is
        // re-centred instead of leaving the gap on the right.
        // One fixed window: the inspector floats inside it, so nothing shifts.
        layoutShiftX = 0f;

        mx = (mouseX - offX) / ui - layoutShiftX;
        my = (mouseY - offY) / ui;

        if (!layoutLogged) {
            layoutLogged = true;
            System.out.printf(Locale.ROOT,
                    "[OctoClient] dashboard: screen=%dx%d ui=%.4f space=%.0fx%.0f window=%dx%d@%d,%d cardW=%.0f%n",
                    width, height, ui, VW, VH, WIN_W, WIN_H, WIN_X, WIN_Y, cardWidth());
        }

        tick(dt);

        // Backdrop: the scene behind the menu is frosted, the way the Blur mod
        // blurs it. The frost covers the window exactly, so the corners stay
        // square instead of being rounded off by a mask. While the frame copy
        // is still missing, keep the layered gradient so the screen never goes
        // bare.
        if (!Draw.screenBlur(gfx, 0.0045f, 0.18f)) {
            gfx.fillGradient(0, 0, width, height, 0x51090911, 0x5C05050A);
            int topBand = Math.max(28, height / 4);
            int bottomBand = Math.max(36, height / 3);
            gfx.fillGradient(0, 0, width, topBand, 0x33000000, 0x00000000);
            gfx.fillGradient(0, height - bottomBand, width, height, 0x00000000, 0x4D000000);
        }

        Matrix3x2fStack pose = gfx.pose();
        pose.pushMatrix();
        pose.scale(ui, ui);
        pose.translate(offX / ui, offY / ui);
        pose.translate(layoutShiftX, 0f);
        // The whole cluster rises into place while it scales up, so opening the
        // dashboard reads as one movement instead of a pop.
        if (intro < 0.999f) {
            pose.translate(0f, (1f - intro) * 18f);
        }

        boolean scaled = intro < 0.999f;
        if (scaled) {
            float k = 0.982f + 0.018f * intro;
            pose.pushMatrix();
            pose.translate(INTRO_CX, INTRO_CY);
            pose.scale(k, k);
            pose.translate(-INTRO_CX, -INTRO_CY);
        }

        boolean radiusFocus = radiusFocusActive();
        drawShell(gfx);
        if (!radiusFocus) {
            drawHeader(gfx);
        }
        if (cosmeticsOnly) {
            drawCosmeticsSidebar(gfx);
        } else {
            drawSidebar(gfx);
        }
        if (!radiusFocus) {
            drawContentHeader(gfx);
            drawSearchBar(gfx);
            drawCosmeticTabs(gfx);
        }
        drawCards(gfx, partialTick);
        // The inspector is the launcher's account card: always docked on the right.
        if (ClientSettings.dashboardBackgroundEnabled()) {
            drawPanel(gfx, SET_X, SET_Y, SET_W, SET_H, ClientSettings.clientRadius());
        }
        drawSettingsPanel(gfx);
        if (!radiusFocus) {
            if (popupModule == null) {
                drawCosmeticPreview(gfx, partialTick);
            }
            drawScrollbar(gfx);
            drawFooterChips(gfx);
        }

        if (scaled) {
            pose.popMatrix();
        }
        pose.popMatrix();
    }

    private float frameDelta() {
        long now = System.nanoTime();
        float d = lastFrameNanos == 0L ? 1f / 60f : (now - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = now;
        return Math.max(1f / 240f, Math.min(0.05f, d));
    }

    private void tick(float rawDt) {
        float speed = Math.max(0.15f, ClientSettings.animationSpeed());
        float d = rawDt * speed;
        time += rawDt;

        if (category == ModuleCategory.SECRET && !SecretTabUnlock.isUnlocked()) {
            category = ModuleCategory.HUD;
            search = "";
            scroll = 0f;
            scrollTarget = 0f;
        }

        // One slider in Client Settings rounds every module at once.
        ClientSettings.syncGlobalRadius();
        if (popupModule != null) {
            popupScroll = Math.min(popupScroll, popupMaxScroll());
        }

        inside = mx >= 0f && my >= 0f && mx <= VW && my <= VH;

        intro = approach(intro, 1f, 11f, d);
        catAnim = approach(catAnim, 1f, 11f, d);
        focusA = approach(focusA, searchFocused ? 1f : 0f, 22f, d);
        popupProgress = approach(popupProgress, popupClosing ? 0f : popupModule == null ? 0f : 1f, 18f, d);
        if (popupClosing && popupProgress < 0.005f) {
            popupModule = null;
            popupClosing = false;
        }

        List<ModuleCategory> side = sidebarCategories();
        int sideIndex = side.indexOf(category);
        if (sideIndex >= 0) {
            float indTarget = sidebarRowY(sideIndex);
            if (indY < 0f) {
                indY = indTarget;
            }
            indY = approach(indY, indTarget, 26f, d);
        }
        for (ModuleCategory c : visibleCategories()) {
            int i = c.ordinal();
            catLabelA[i] = approach(catLabelA[i], c == category ? 1f : 0f, 22f, d);
        }

        List<CardRect> cards = cardLayout();
        CardRect hovered = inside ? hitCard(mx, my, cards) : null;
        if (ClientSettings.popupSettingsEnabled() && expandedModule != null) {
            anim(expandedModule).open = false;
            expandedModule = null;
            expandedIndex = -1;
        }

        boolean expandedVisible = false;
        for (CardRect card : cards) {
            Module m = card.module;
            CardAnim a = anim(m);
            expandedVisible |= m == expandedModule;
            boolean on = m.isEnabled();

            float hoverTarget = hovered != null && hovered.module == m ? 1f : 0f;
            a.hover = approach(a.hover, hoverTarget, 30f, d);
            float[] lift = spring(a.lift, a.liftVel, hoverTarget, d, 420f, 26f);
            a.lift = Anim.clamp(lift[0], -0.12f, 1.12f);
            a.liftVel = lift[1];

            float[] tog = spring(a.onA, a.vel, on ? 1f : 0f, d, 430f, 26f);
            a.onA = Anim.clamp(tog[0], -0.12f, 1.12f);
            a.vel = tog[1];

            a.glow = approach(a.glow, on ? 1f : 0f, 16f, d);
            a.expand = approach(a.expand, a.open ? 1f : 0f, 24f, d);
            a.spin = approach(a.spin, a.open ? 1f : 0f, 20f, d);
            a.press = approach(a.press, 0f, 22f, d);
            a.flash = approach(a.flash, 0f, 9f, d);

            for (Setting<?> s : visibleSettings(m)) {
                if (s instanceof BooleanSetting b) {
                    SetAnim sa = anim(b);
                    float[] sw = spring(sa.a, sa.va, b.isEnabled() ? 1f : 0f, d, 520f, 30f);
                    sa.a = Anim.clamp(sw[0], -0.12f, 1.12f);
                    sa.va = sw[1];
                } else if (s instanceof ModeSetting mode) {
                    SetAnim sa = anim(s);
                    float target = Math.min(96f, Math.round(Draw.uiWidth(mode.getValue(), false) * S_LABEL) + 26f);
                    sa.pw = sa.pw < 0f ? target : approach(sa.pw, target, 30f, d);
                }
            }
        }

        if (expandedModule != null && !expandedVisible) {
            CardAnim a = anim(expandedModule);
            a.expand = approach(a.expand, a.open ? 1f : 0f, 24f, d);
            a.spin = approach(a.spin, a.open ? 1f : 0f, 20f, d);
        }

        if (expandedModule != null && !anim(expandedModule).open
                && anim(expandedModule).expand < 0.001f) {
            expandedModule = null;
            expandedIndex = -1;
        }

        float max = maxScroll();
        if (scrollTarget > max) {
            scrollTarget = max;
        }
        scroll = approach(scroll, scrollTarget, 26f, d);
    }

    private static float approach(float v, float target, float k, float d) {
        return v + (target - v) * (1f - (float) Math.exp(-k * d));
    }

    /** Critically-ish damped spring (toggle overshoot), identical to the web. */
    private static float[] spring(float v, float vel, float target, float d, float k, float damp) {
        float acc = (target - v) * k - vel * damp;
        float nv = vel + acc * d;
        return new float[]{v + nv * d, nv};
    }

    // -------------------------------------------------------------- animation

    private static final class CardAnim {
        float hover;
        /** Spring-driven hover lift, so the card overshoots a hair and settles. */
        float lift;
        float liftVel;
        float onA;
        float vel;
        float glow;
        float expand;
        float spin;
        float press;
        float flash;
        boolean open;

        CardAnim(boolean on) {
            onA = on ? 1f : 0f;
            glow = on ? 1f : 0f;
        }
    }

    private static final class SetAnim {
        float a;
        float va;
        float pw = -1f;

        SetAnim(boolean on) {
            a = on ? 1f : 0f;
        }
    }

    private static final class CardRect {
        final Module module;
        final float x;
        final float y;
        final float w;
        final float h;
        final int index;

        CardRect(Module module, float x, float y, float w, float h, int index) {
            this.module = module;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.index = index;
        }
    }

    private CardAnim anim(Module m) {
        return cardAnims.computeIfAbsent(m, k -> new CardAnim(k.isEnabled()));
    }

    private SetAnim anim(Setting<?> s) {
        return setAnims.computeIfAbsent(s, k -> new SetAnim(k instanceof BooleanSetting b && b.isEnabled()));
    }

    // ------------------------------------------------------------------ layout

    private List<Module> visibleModules() {
        boolean secretUnlocked = SecretTabUnlock.isUnlocked();
        if (visibleModulesCache != null && cachedCategory == category
                && cachedCosmeticFilter == cosmeticFilter && cachedSearch.equals(search)
                && cachedSecretUnlocked == secretUnlocked && cachedCosmeticsOnly == cosmeticsOnly
                && cachedCosmeticSection == cosmeticSection) {
            return visibleModulesCache;
        }

        if (!search.isEmpty()) {
            String needle = search.toLowerCase(Locale.ROOT);
            boolean cosmeticSearch = cosmeticsOnly || category == ModuleCategory.COSMETICS;
            visibleModulesCache = ModuleManager.get().getModules().stream()
                    .filter(m -> cosmeticSearch || m.getCategory() != ModuleCategory.COSMETICS)
                    .filter(m -> !cosmeticSearch || m.getCategory() == ModuleCategory.COSMETICS)
                    .filter(m -> m.getCategory() != ModuleCategory.SECRET || secretUnlocked)
                    .filter(m -> m.getName().toLowerCase(Locale.ROOT).contains(needle)
                            || m.getDescription().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        } else if (cosmeticsOnly) {
            visibleModulesCache = ModuleManager.get().getByCategory(ModuleCategory.COSMETICS).stream()
                    .filter(this::matchesCosmeticSection)
                    .sorted(Comparator
                            .comparingInt((Module m) -> m instanceof CosmeticPart part
                                    ? part.kind().ordinal() : 99)
                            .thenComparing(Module::getName))
                    .toList();
        } else if (category == ModuleCategory.SECRET && !secretUnlocked) {
            visibleModulesCache = List.of();
        } else if (category == ModuleCategory.COSMETICS) {
            // Sorted by kind (Capes, Wings, Hats, Aura) and then by name, so the
            // grid always reads in the same order as the sub-tabs above it.
            visibleModulesCache = ModuleManager.get().getByCategory(ModuleCategory.COSMETICS).stream()
                    .filter(m -> cosmeticFilter == null
                            ? !(m instanceof CosmeticPart part && part.kind() == CosmeticKind.BLC_ICONS)
                            : (m instanceof CosmeticPart part && part.kind() == cosmeticFilter))
                    .sorted(Comparator
                            .comparingInt((Module m) -> m instanceof CosmeticPart part
                                    ? part.kind().ordinal() : 99)
                            .thenComparing(Module::getName))
                    .toList();
        } else {
            visibleModulesCache = ModuleManager.get().getByCategory(category);
        }

        cachedCategory = category;
        cachedCosmeticFilter = cosmeticFilter;
        cachedSearch = search;
        cachedSecretUnlocked = secretUnlocked;
        cachedCosmeticsOnly = cosmeticsOnly;
        cachedCosmeticSection = cosmeticSection;
        cardLayoutDirty = true;
        return visibleModulesCache;
    }

    private boolean matchesCosmeticSection(Module module) {
        if (cosmeticSection == CosmeticSection.ALL) {
            return true;
        }
        if (cosmeticSection == CosmeticSection.PREMIUM) {
            return module.isFavorited();
        }

        String type = module instanceof CatalogCosmetic catalog ? catalog.entry().type() : "";
        CosmeticKind kind = module instanceof CosmeticPart part ? part.kind() : null;
        return switch (cosmeticSection) {
            case CAPES -> type.equals("CAPE") || kind == CosmeticKind.CAPE;
            case ACCESSORIES -> kind == CosmeticKind.ACCESSORY || kind == CosmeticKind.HAT;
            case BOOTS -> type.equals("BOOTS");
            case BANDANAS -> type.equals("BANDANA") || kind == CosmeticKind.BANDANA;
            case SHIELDS -> type.equals("SHIELD");
            case WINGS -> type.equals("WINGS") || kind == CosmeticKind.WINGS;
            case WEAPONS -> type.equals("SWORD") || type.equals("BOW");
            case COMPANIONS -> type.equals("PET") || type.equals("PLAYER_PET");
            case AURAS -> type.equals("AURA") || kind == CosmeticKind.AURA;
            case ICONS -> kind == CosmeticKind.NAMETAG;
            case BLC_ICONS -> type.equals("BLC_NAMETAG_ICON") || kind == CosmeticKind.BLC_ICONS;
            case ALL -> kind != CosmeticKind.BLC_ICONS;
            case PREMIUM -> true;
        };
    }

    private static List<ModuleCategory> visibleCategories() {
        return java.util.Arrays.stream(ModuleCategory.values())
                .filter(cat -> cat != ModuleCategory.SECRET || SecretTabUnlock.isUnlocked())
                .toList();
    }

    /** Cards start below the cosmetics sub-tab row when it is shown. */
    private float cardsTop() {
        return showsCosmeticTabs() ? AY + SUBTAB_H + SUBTAB_GAP : AY;
    }

    private boolean showsCosmeticTabs() {
        return !cosmeticsOnly && category == ModuleCategory.COSMETICS && search.isEmpty();
    }

    /** The little model preview only makes sense while browsing cosmetics. */
    private boolean showsCosmeticPreview() {
        return cosmeticsOnly || (category == ModuleCategory.COSMETICS && search.isEmpty());
    }

    /**
     * The inspector is a real column, not an overlay: while it is on screen it
     * takes its room away from the grid, so cards and settings never sit on top
     * of each other.
     */
    private boolean inspectorVisible() {
        // The inspector is always docked beside the module sheet.
        return true;
    }

    private float contentW() {
        return AW;
    }

    private int columns() {
        return COLUMNS;
    }

    /** Search pill, pinned to the right edge of the grid column. */
    private float searchX() {
        return AX + contentW() - SEARCH_W;
    }

    private float columnWidth() {
        int cols = columns();
        return (contentW() - GAP * (cols - 1)) / (float) cols;
    }

    private float cardWidth() {
        float scale = Math.max(0.6f, Math.min(1f, ClientSettings.cardWidthScale()));
        return columnWidth() * scale;
    }

    /** The inspector column doubles as the cosmetics preview. */
    private float previewX() {
        return SET_X + SET_PAD;
    }

    private float previewY() {
        return SET_Y + 46f;
    }

    private float previewW() {
        return SET_W - SET_PAD * 2f;
    }

    private float previewH() {
        return SET_H - 74f;
    }

    /** All settings of a module plus its "Reset" row at the bottom. */
    private List<Setting<?>> visibleSettings(Module module) {
        List<Setting<?>> list = new ArrayList<>(module.getSettings());
        if (!list.contains(module.resetAction())) {
            list.add(module.resetAction());
        }
        return list;
    }

    private static float subTabWidth(String label) {
        return Draw.uiWidth(label, false) * S_LABEL + SUBTAB_PAD * 2f;
    }

    private float cardHeight(Module m) {
        CardAnim a = anim(m);
        if (m != expandedModule || ClientSettings.popupSettingsEnabled() || a.expand < 0.001f) {
            return CARD_H;
        }
        return CARD_H + a.expand * (settingsHeight(m) + 16f);
    }

    private float settingsHeight(Module m) {
        float h = 0f;
        for (Setting<?> s : visibleSettings(m)) {
            h += rowHeight(s);
        }
        if (previewModule(m) != null) {
            h += MODULE_PREVIEW_H;
        }
        return h;
    }

    private float rowHeight(Setting<?> s) {
        if (s instanceof BooleanSetting) {
            return 26f;
        }
        if (s instanceof ActionSetting) {
            return 30f;
        }
        if (s instanceof NumberSetting) {
            return 40f;
        }
        if (s instanceof ModeSetting) {
            return 30f;
        }
        if (s instanceof TextSetting) {
            return 36f;
        }
        if (s instanceof KeybindSetting) {
            return 30f;
        }
        if (s instanceof ColorSetting c) {
            return 30f + (isColorOpen(c) ? 14f + 72f + 4 * 22f : 0f);
        }
        return 26f;
    }

    private boolean isColorOpen(ColorSetting c) {
        return colorOpen.getOrDefault(c, false);
    }

    // ------------------------------------------------------------ module previews

    /**
     * HUD modules that show a self-contained panel and therefore can be drawn
     * inside the settings accordion without depending on the current screen.
     * Interactive HUDs (waypoints, target info, inventories) are left out.
     */
    private HudModule previewModule(Module m) {
        if (!(m instanceof HudModule hud)) {
            return null;
        }
        return switch (m.getName()) {
            case "Watermark", "Potion Status", "Keystrokes", "FPS", "CPS", "Clock", "Session",
                 "Coordinates", "Ping", "Armor Status", "Speed", "Memory", "TPS", "Uptime",
                 "Stats" -> hud;
            default -> null;
        };
    }

    private void drawModulePreview(GuiGraphicsExtractor gfx, Module m, float x, float y, float w, float alpha) {
        HudModule hud = previewModule(m);
        if (hud == null) {
            return;
        }
        float h = MODULE_PREVIEW_H - 4f;
        if (alpha < 0.02f || w < 40f) {
            return;
        }
        rrF(gfx, x, y, w, h, 8f, fade(B(72), alpha));
        roF(gfx, x, y, w, h, 8f, fade(W(16), alpha));
        Draw.ui(gfx, "PREVIEW", x + 9f, y + 6f, fade(INK_FAINT, alpha), S_TEXT, false, false);

        int sx0 = Math.round(x + 1f);
        int sy0 = Math.round(y + 15f);
        int sx1 = Math.round(x + w - 1f);
        int sy1 = Math.round(y + h - 1f);
        if (sx1 <= sx0 || sy1 <= sy0) {
            return;
        }

        float oldX = hud.x;
        float oldY = hud.y;
        float oldW = hud.width;
        float oldH = hud.height;
        gfx.enableScissor(sx0, sy0, sx1, sy1);
        try {
            hud.x = x + 12f;
            hud.y = y + 26f;
            hud.renderScaled(new HudDrawContext(gfx, Minecraft.getInstance(), 0f, (int) VW, (int) VH), 0f, true, false);
        } catch (RuntimeException ignored) {
            // A preview must never take the whole dashboard down.
        } finally {
            gfx.disableScissor();
            hud.x = oldX;
            hud.y = oldY;
            hud.width = oldW;
            hud.height = oldH;
        }
    }

    /** Uniform three-column grid; an expanded card grows its whole row. */
    private void ensureCardLayout() {
        List<Module> modules = visibleModules();
        float currentTop = cardsTop();
        boolean popupSettings = ClientSettings.popupSettingsEnabled();
        float currentContentW = contentW();
        if (!cardLayoutDirty && cachedCardsTop == currentTop && cachedPopupSettings == popupSettings
                && cachedContentW == currentContentW) {
            return;
        }

        if (expandedModule != null) {
            expandedIndex = modules.indexOf(expandedModule);
            if (expandedIndex < 0) {
                anim(expandedModule).open = false;
                expandedModule = null;
                expandedIndex = -1;
            }
        }
        float columnWidth = columnWidth();
        float currentWidth = cardWidth();
        List<CardRect> bases = new ArrayList<>();
        float y = currentTop;
        int cols = columns();
        for (int start = 0; start < modules.size(); start += cols) {
            float rowHeight = CARD_H;
            if (expandedIndex >= start && expandedIndex < start + cols && !popupSettings) {
                rowHeight = cardHeight(expandedModule);
            }
            for (int column = 0; column < cols; column++) {
                int i = start + column;
                if (i >= modules.size()) {
                    break;
                }
                bases.add(new CardRect(modules.get(i), AX + column * (columnWidth + GAP),
                        y, currentWidth, rowHeight, i));
            }
            y += rowHeight + CARD_GAP_Y;
        }
        cardBases = List.copyOf(bases);
        gridHeight = modules.isEmpty() ? 0f : y - currentTop - CARD_GAP_Y;
        cachedCardsTop = currentTop;
        cachedPopupSettings = popupSettings;
        cachedContentW = currentContentW;
        cardLayoutDirty = false;
    }

    private List<CardRect> cardLayout() {
        ensureCardLayout();
        visibleCardRects.clear();
        float offset = Math.min(scroll, maxScroll());
        float top = cardsTop();
        float bottom = AY + AH;
        for (CardRect base : cardBases) {
            float y = base.y - offset;
            if (y + base.h < top) {
                continue;
            }
            if (y > bottom) {
                break;
            }
            float height = base.module == expandedModule ? cardHeight(base.module) : CARD_H;
            visibleCardRects.add(new CardRect(base.module, base.x, y, cardWidth(), height,
                    base.index));
        }
        return visibleCardRects;
    }

    /**
     * While a corner-radius slider is dragged the dashboard chrome is hidden so
     * the user only sees the module cards (all of them, or just the edited one).
     */
    private boolean radiusFocusActive() {
        if (draggedSlider == null) {
            return false;
        }
        String name = draggedSlider.getName();
        return name.equals("Corner Radius") || name.equals("Module Radius");
    }

    private boolean globalRadiusFocus() {
        return draggedSlider != null && draggedSlider.getName().equals("Module Radius");
    }

    /** Module whose own radius is being dragged, or null for the global slider. */
    private Module radiusFocusModule() {
        if (!radiusFocusActive() || globalRadiusFocus()) {
            return null;
        }
        for (Module m : ModuleManager.get().getModules()) {
            if (m.roundness() == draggedSlider) {
                return m;
            }
        }
        return null;
    }

    private float maxScroll() {
        ensureCardLayout();
        // Cards start at cardsTop(), not at AY, so the visible height is
        // smaller whenever the cosmetics sub-tabs are shown. The trailing inset
        // keeps the last row from touching the window edge.
        float viewport = (AY + AH) - cardsTop() - GAP;
        return Math.max(0f, gridHeight - viewport);
    }

    private float appear(int i) {
        // Stagger only the first screenful; cards further down must not stay
        // invisible once the category is fully faded in (long lists such as
        // Cosmetics earlier scrolled into an empty area).
        float delay = Math.min(i, 10) * 0.035f;
        float t = Anim.clamp01((catAnim - delay) / 0.55f);
        // Ease out instead of a linear ramp: the cards arrive with a quick
        // start and settle gently, which reads as motion rather than a wipe.
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private CardRect hitCard(float x, float y) {
        return hitCard(x, y, cardLayout());
    }

    private CardRect hitCard(float x, float y, List<CardRect> cards) {
        for (CardRect c : cards) {
            if (inRect(x, y, c.x, c.y, c.w, c.h)) {
                return c;
            }
        }
        return null;
    }

    private static final class Row {
        final Setting<?> setting;
        final float x;
        final float y;
        final float w;
        final float h;

        Row(Setting<?> setting, float x, float y, float w, float h) {
            this.setting = setting;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    /** A collapsible group of the inspector's settings. */
    private enum SettingSection {
        OPTIONS("OPTIONS"),
        SLIDERS("SLIDERS"),
        COLORS("COLORS");

        final String label;

        SettingSection(String label) {
            this.label = label;
        }

        static SettingSection of(Setting<?> s) {
            if (s instanceof ColorSetting) {
                return COLORS;
            }
            if (s instanceof NumberSetting) {
                return SLIDERS;
            }
            return OPTIONS;
        }
    }

    /** One separated inspector card: a header row plus the settings it holds. */
    private static final class Section {
        final SettingSection kind;
        final float x;
        final float y;
        final float w;
        final float headerH;
        final boolean open;
        final List<Row> rows = new ArrayList<>();
        float h;

        Section(SettingSection kind, float x, float y, float w, float headerH, boolean open) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.w = w;
            this.headerH = headerH;
            this.open = open;
        }
    }

    private List<Row> settingsLayout(CardRect c) {
        List<Row> rows = new ArrayList<>();
        float y = c.y + CARD_H + 10f;
        float x = c.x + CARD_PAD + 4f;
        float w = c.w - (CARD_PAD + 4f) * 2f;
        for (Setting<?> s : visibleSettings(c.module)) {
            float h = rowHeight(s);
            rows.add(new Row(s, x, y, w, h));
            y += h;
        }
        return rows;
    }

    // ------------------------------------------------------------------- draw

    private static int W(int a) {
        return Theme.withAlpha(WHITE, a);
    }

    private static int B(int a) {
        return Theme.withAlpha(0x000000, a);
    }

    private static int fade(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 0xFF) * Anim.clamp01(a));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    /** Rounded rect at a float position (keeps the sub-pixel offset). */
    private static void rrF(GuiGraphicsExtractor gfx, float x, float y, float w, float h, float r, int argb) {
        if (w <= 0f || h <= 0f) {
            return;
        }
        Draw.pushSnap(gfx, x, y);
        Draw.roundedRect(gfx, Math.round(x), Math.round(y), Math.round(w), Math.round(h), Math.round(r), argb);
        Draw.popSnap(gfx);
    }

    private static void roF(GuiGraphicsExtractor gfx, float x, float y, float w, float h, float r, int argb) {
        if (w <= 0f || h <= 0f) {
            return;
        }
        Draw.pushSnap(gfx, x, y);
        Draw.roundedOutline(gfx, Math.round(x), Math.round(y), Math.round(w), Math.round(h), Math.round(r), argb);
        Draw.popSnap(gfx);
    }

    private void drawShell(GuiGraphicsExtractor gfx) {
        if (!ClientSettings.dashboardBackgroundEnabled()) {
            // "No background" mode: the module cards and text float directly
            // over the game instead of sitting on a glass sheet.
            return;
        }
        // The launcher puts its content straight on the backdrop, so no window
        // frame is painted: only a soft scrim keeps the type readable.
        gfx.fillGradient(0, 0, Math.round(VW), Math.round(VH), 0x5C000000, 0x8A000000);
    }

    /**
     * Dashboard background sheet. "Glass" keeps the original refractive look;
     * the other Dashboard Style values reuse the same vocabulary as module cards.
     */
    private static void drawPanel(GuiGraphicsExtractor gfx, float x, float y, float w, float h, float r) {
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(w);
        int ih = Math.round(h);
        int ir = Math.round(r);
        int tint = ClientSettings.dashboardColorRgb();
        String style = ClientSettings.dashboardStyle();

        if (ClientSettings.STYLE_FROSTED.equals(style)) {
            Draw.frostedGlass(gfx, ix, iy, iw, ih, ir, tint, 0.96f, 0f, 72);
            return;
        }
        if ("Clear".equals(style)) {
            Draw.roundedOutline(gfx, ix, iy, iw, ih, ir, Theme.withAlpha(0xFFFFFF, 52));
            return;
        }
        if ("Solid".equals(style)) {
            Draw.roundedRect(gfx, ix, iy, iw, ih, ir,
                    Theme.withAlpha(Theme.mix(0x111318, tint, 0.12f), 238));
            Draw.roundedOutline(gfx, ix, iy, iw, ih, ir, Theme.withAlpha(0xFFFFFF, 38));
            return;
        }

        // Launcher pane: a pale, low-alpha glass sheet with a fine bright rim
        // instead of the smoked body.
        Draw.liquidGlass(gfx, ix, iy, iw, ih, ir, Theme.mix(PANE_TINT, tint, 0.25f), 0.62f, 0f, 104);
        Draw.roundedOutline(gfx, ix, iy, iw, ih, ir, W(40));
        // Frosted wash across the top third and a bright specular rim line.
        Draw.shapeF(gfx, x + 1f, y + 1f, w - 2f, h * 0.3f, Math.max(0f, r - 1f), W(16));
        gfx.fill(ix + ir, iy, ix + iw - ir, iy + 1, W(120));
    }

    /** Four-point sparkle: the client mark above the nav column. */
    private static void drawSparkle(GuiGraphicsExtractor gfx, float cx, float cy, float size, int argb) {
        Draw.shapeF(gfx, cx - size * 0.09f, cy - size, size * 0.18f, size * 2f, size * 0.09f, argb);
        Draw.shapeF(gfx, cx - size, cy - size * 0.09f, size * 2f, size * 0.18f, size * 0.09f, argb);
        gfx.pose().pushMatrix();
        gfx.pose().translate(cx, cy);
        gfx.pose().rotate((float) (Math.PI / 4.0));
        int soft = Theme.withAlpha(argb & 0xFFFFFF, 130);
        Draw.shapeF(gfx, -size * 0.62f, -size * 0.06f, size * 1.24f, size * 0.12f, size * 0.06f, soft);
        Draw.shapeF(gfx, -size * 0.06f, -size * 0.62f, size * 0.12f, size * 1.24f, size * 0.06f, soft);
        gfx.pose().popMatrix();
    }

    /** Glass pill switch: smoked track, white body when on. */
    private static void switchLight(GuiGraphicsExtractor gfx, float x, float y, float w, float h, float on) {
        Draw.switchF(gfx, x, y, w, h, on);
    }

    /** Glass slider with a white knob. */
    private static void sliderLight(GuiGraphicsExtractor gfx, float x, float y, float w, float frac, float alpha) {
        Draw.sliderF(gfx, x, y, w, frac, fade(0xF2F2F7, alpha), true);
    }

    /** Glass control chip (mode pills, text fields, keybind fields). */
    private static void controlPill(GuiGraphicsExtractor gfx, float x, float y, float w, float h,
                                    boolean hot, float alpha) {
        Draw.shapeF(gfx, x, y, w, h, 7f, fade(W(hot ? 54 : 30), alpha));
        Draw.shapeF(gfx, x + 1f, y + 1f, w - 2f, h - 2f, 6f, fade(B(hot ? 100 : 72), alpha));
        // Rim: the interactive fields are the important rows, so they read as
        // outlined controls instead of flat text.
        roF(gfx, x, y, w, h, 7f, fade(W(hot ? 96 : 46), alpha));
    }

    /** Every module category is a nav tab; configs lives in the bottom row. */
    private List<ModuleCategory> sidebarCategories() {
        return visibleCategories().stream()
                .filter(cat -> cat != ModuleCategory.SETTINGS)
                .toList();
    }

    /** One representative vanilla item per category. */
    private static ItemStack categoryIcon(ModuleCategory cat) {
        ItemStack cached = CATEGORY_ICONS.get(cat);
        if (cached != null) {
            return cached;
        }
        ItemStack stack;
        try {
            stack = new ItemStack(switch (cat) {
                case HUD -> Items.CLOCK;
                case RENDER -> Items.ENDER_EYE;
                case UTILITY -> Items.CRAFTING_TABLE;
                case COSMETICS -> Items.ELYTRA;
                case SETTINGS -> Items.COMPARATOR;
                case SECRET -> Items.NETHER_STAR;
            });
        } catch (RuntimeException componentsNotBoundYet) {
            // The dashboard is drawn over the title screen too, where item
            // components may still be unbound; a tab icon must never crash it.
            return ItemStack.EMPTY;
        }
        CATEGORY_ICONS.put(cat, stack);
        return stack;
    }

    /** Item icon at a 16x16 design-space slot. */
    private static void drawCategoryIcon(GuiGraphicsExtractor gfx, ModuleCategory cat, float x, float y) {
        ItemStack stack = categoryIcon(cat);
        if (stack.isEmpty()) {
            return;
        }
        Draw.pushSnap(gfx, x, y);
        gfx.item(stack, Math.round(x), Math.round(y));
        Draw.popSnap(gfx);
    }

    /** Launcher header: wordmark, version chip and the account chip on the right. */
    private void drawHeader(GuiGraphicsExtractor gfx) {
        drawSparkle(gfx, 32f, 25f, 9f, INK);
        Draw.ui(gfx, cosmeticsOnly ? "OCTO COSMETIC" : "OCTO CLIENT", 48f, 17f,
                INK, 1.05f, true, false);
        String version = "v2.2.0";
        float vx = 48f + Draw.uiWidth(cosmeticsOnly ? "OCTO COSMETIC" : "OCTO CLIENT", true) * 1.05f + 9f;
        float vw = Draw.uiWidth(version, false) * 0.72f + 15f;
        rrF(gfx, vx, 16f, vw, 15f, 7f, W(14));
        roF(gfx, vx, 16f, vw, 15f, 7f, W(34));
        Draw.uiCentered(gfx, version, vx + vw / 2f, 19f, INK_MUTED, 0.72f, false, false);

        // Account chip, right aligned like the launcher's profile pill.
        float pw = 190f;
        float px = VW - 22f - pw;
        rrF(gfx, px, 11f, pw, 34f, 17f, W(16));
        roF(gfx, px, 11f, pw, 34f, 17f, W(26));
        String name = minecraft.player != null ? minecraft.player.getName().getString() : "Player";
        rrF(gfx, px + 6f, 14f, 28f, 28f, 9f, W(38));
        Draw.uiCentered(gfx, name.isEmpty() ? "P" : name.substring(0, 1).toUpperCase(Locale.ROOT),
                px + 20f, 22f, INK, 0.95f, true, false);
        Draw.ui(gfx, fit(name, 104f, 0.85f), px + 41f, 17f, INK, 0.85f, true, false);
        Draw.ui(gfx, fit("OCTOCLIENT · 26.2", 104f, 0.62f), px + 41f, 30f, INK_FAINT, 0.62f, false, false);
    }

    /** Rail only: no brand and no profile, those live in the header now. */
    private void drawSidebar(GuiGraphicsExtractor gfx) {
        List<ModuleCategory> cats = sidebarCategories();
        for (int i = 0; i < cats.size(); i++) {
            ModuleCategory cat = cats.get(i);
            railTile(gfx, sidebarRowY(i), cat.title, categoryGlyph(cat),
                    search.isEmpty() && cat == category);
        }
        railTile(gfx, configRowY(), "Settings", UiFonts.ICON_SLIDERS,
                category == ModuleCategory.SETTINGS && search.isEmpty());
        railTile(gfx, hudRowY(), "HUD Layout", UiFonts.ICON_GAUGE, false);
    }

    /** One rail tile: 40 px icon square, label under it, light square when active. */
    private void railTile(GuiGraphicsExtractor gfx, float y, String label, int glyph, boolean active) {
        float x = SIDE_X + RAIL_PAD;
        float w = SIDE_W - RAIL_PAD * 2f;
        boolean hovered = inside && inRect(mx, my, x, y, w, SIDE_ROW_H);
        float cx = x + w / 2f;
        if (active) {
            rrF(gfx, cx - 18f, y, 36f, 36f, 12f, W(34));
            roF(gfx, cx - 18f, y, 36f, 36f, 12f, W(42));
        } else if (hovered) {
            rrF(gfx, cx - 18f, y, 36f, 36f, 12f, W(16));
        }
        Draw.uiIcon(gfx, glyph, cx, y + 18f, active ? INK : INK_MUTED, 0.95f);
        Draw.uiCentered(gfx, fit(label, w + 6f, 0.68f), cx, y + 39f,
                active ? INK : (hovered ? INK : INK_MUTED), 0.68f, active, false);
    }

    /** Launcher heading block: small caption, big title, one muted line. */
    private void drawContentHeader(GuiGraphicsExtractor gfx) {
        Draw.ui(gfx, fit("OCTOCLIENT · " + category.title.toUpperCase(Locale.ROOT), AW - 210f, 0.7f),
                AX, WIN_Y + 2f, INK_FAINT, 0.7f, false, false);
        Draw.ui(gfx, fit(category.title, AW - 220f, 1.9f, true), AX - 1f, WIN_Y + 14f,
                INK, 1.9f, true, false);
        int enabled = 0;
        List<Module> shown = visibleModules();
        for (Module module : shown) {
            if (module.isEnabled()) {
                enabled++;
            }
        }
        String sub = shown.size() + (shown.size() == 1 ? " module" : " modules")
                + "  ·  " + enabled + " on";
        Draw.ui(gfx, fit(sub, AW - 220f, 0.8f), AX, WIN_Y + 46f, INK_MUTED, 0.8f, false, false);
    }

    /** Bottom chip row and the launcher's tagline, under the module grid. */
    private void drawFooterChips(GuiGraphicsExtractor gfx) {
        float y = WIN_Y + WIN_H + 5f;
        float x = AX;
        x = chip(gfx, x, y, "2.2.0", UiFonts.ICON_GEAR);
        x = chip(gfx, x, y, "Fabric 26.2", UiFonts.ICON_SCREWDRIVER);
        chip(gfx, x, y, "Fabric", UiFonts.ICON_GAUGE);
        String tagline = "Lightweight · built for mods and performance";
        Draw.ui(gfx, tagline, VW - 22f - Draw.uiWidth(tagline, false) * 0.68f, y + 7f,
                INK_FAINT, 0.68f, false, false);
    }

    /** One rounded chip with a leading glyph, like the launcher's status pills. */
    private float chip(GuiGraphicsExtractor gfx, float x, float y, String label, int glyph) {
        float w = Draw.uiWidth(label, false) * 0.72f + 30f;
        rrF(gfx, x, y, w, 20f, 10f, W(14));
        roF(gfx, x, y, w, 20f, 10f, W(24));
        Draw.uiIcon(gfx, glyph, x + 12f, y + 10f, INK_MUTED, 0.8f);
        Draw.ui(gfx, label, x + 21f, y + 6f, INK_SOFT, 0.72f, false, false);
        return x + w + 6f;
    }

    /** Line icon for a nav tab, so it stays legible on the light active pill. */
    private static int categoryGlyph(ModuleCategory cat) {
        return switch (cat) {
            case HUD -> UiFonts.ICON_GAUGE;
            case RENDER -> UiFonts.ICON_EYE;
            case UTILITY -> UiFonts.ICON_SCREWDRIVER;
            case COSMETICS -> UiFonts.ICON_SHIRT;
            case SECRET -> UiFonts.ICON_SHIELD;
            case SETTINGS -> UiFonts.ICON_SLIDERS;
        };
    }

    /**
     * Item glyph that survives a screen drawn before item components are bound
     * (the dashboard and the cosmetics studio also open over the title screen).
     */
    private static void drawItemSafe(GuiGraphicsExtractor gfx, Item item, float x, float y) {
        try {
            ItemStack stack = new ItemStack(item);
            Draw.pushSnap(gfx, x, y);
            gfx.item(stack, Math.round(x), Math.round(y));
            Draw.popSnap(gfx);
        } catch (RuntimeException componentsNotBoundYet) {
            // No item registry yet: skip the glyph instead of taking the screen down.
        }
    }

    /** Vertical position of a rail tile, under the header. */
    private static float sidebarRowY(int index) {
        return SIDE_Y + 4f + index * SIDE_ROW_STEP;
    }

    /** The client settings tile, right after the module tabs. */
    private float configRowY() {
        return sidebarRowY(sidebarCategories().size());
    }

    /** HUD editor tile, pinned to the bottom of the rail. */
    private float hudRowY() {
        return SIDE_Y + SIDE_H - SIDE_ROW_H - 4f;
    }

    private void drawCosmeticsSidebar(GuiGraphicsExtractor gfx) {
        float padX = SIDE_X + 12f;
        drawSparkle(gfx, padX + 9f, SIDE_Y + 25f, 10f, INK);
        Draw.ui(gfx, "Cosmetic", padX + 26f, SIDE_Y + 16f, INK, 1.26f, true, false);
        Draw.ui(gfx, "Studio", padX + 26f, SIDE_Y + 32f, INK_FAINT, 0.95f, false, false);
        Draw.ui(gfx, "COLLECTION", padX + 2f, SIDE_Y + 54f, INK_FAINT, 0.92f, false, false);

        float pillX = SIDE_X + 8f;
        float pillW = SIDE_W - 16f;
        float rowH = 25f;
        float rowStep = 27f;
        for (int i = 0; i < CosmeticSection.values().length; i++) {
            CosmeticSection section = CosmeticSection.values()[i];
            float y = SIDE_Y + 68f + i * rowStep;
            boolean active = section == cosmeticSection;
            boolean hovered = inside && inRect(mx, my, pillX, y, pillW, rowH) && !active;
            if (active) {
                rrF(gfx, pillX, y, pillW, rowH, 8f, W(26));
                roF(gfx, pillX, y, pillW, rowH, 8f, W(70));
                rrF(gfx, pillX + 1f, y + 6f, 2f, 12f, 1f, Theme.withAlpha(ACCENT, 210));
            } else if (hovered) {
                rrF(gfx, pillX, y, pillW, rowH, 8f, W(12));
            }
            int color = active ? INK : (hovered ? INK_SOFT : INK_MUTED);
            drawItemSafe(gfx, section.icon, pillX + 7f, y + 4f);
            Draw.ui(gfx, section.title, pillX + 30f, y + 8f, color, 1.06f, false, false);
        }
    }

    /** Search pill at the top right of the module sheet. */
    private void drawSearchBar(GuiGraphicsExtractor gfx) {
        float x = searchX();
        float y = SEARCH_Y;
        float w = SEARCH_W;
        float h = SEARCH_H;
        float textScale = 0.95f;
        int border = Math.round(34f + focusA * 40f);
        Draw.liquidGlass(gfx, Math.round(x), Math.round(y), Math.round(w), Math.round(h),
                Math.round(h / 2f), Theme.mix(PANE_TINT, 0xB9C0D8, 0.3f), 0.55f, 0f, border);
        Draw.uiIcon(gfx, UiFonts.ICON_SEARCH, x + 14f, y + 11f, INK_MUTED, 0.85f);

        float textX = x + 25f;
        if (!search.isEmpty()) {
            float tw = Draw.uiWidth(search, false) * textScale;
            if (searchSelectAll) {
                // Selected state: the whole query is highlighted like a text field.
                Draw.shapeF(gfx, textX - 2f, y + 7f, tw + 6f, 11f, 3f, 0x40000000);
            }
            Draw.ui(gfx, fit(search, w - 56f, textScale), textX, y + 7f, INK, textScale,
                    false, false);
            if (searchFocused && !searchSelectAll && ((int) Math.floor(time * 2f)) % 2 == 0) {
                float caretX = textX + tw + 1f;
                rrF(gfx, caretX, y + 7f, 1f, 10f, 0f, 0xCC000000);
            }
            boolean clearHover = inside && inRect(mx, my, x + w - 26f, y, 26f, h);
            Draw.uiIcon(gfx, UiFonts.ICON_XMARK, x + w - 13f, y + 11f,
                    clearHover ? INK : INK_FAINT, 0.8f);
        } else {
            Draw.ui(gfx, cosmeticsOnly ? "Search cosmetics" : "Search modules...",
                    textX, y + 7f, INK_FAINT, textScale, false, false);
        }
    }

    /** Sub-tabs of the Cosmetics tab: All + one chip per cosmetic group. */
    private void drawCosmeticTabs(GuiGraphicsExtractor gfx) {
        if (!showsCosmeticTabs()) {
            return;
        }
        float x = AX;
        x = drawSubTab(gfx, x, "All", cosmeticFilter == null);
        for (CosmeticKind kind : CosmeticKind.values()) {
            x = drawSubTab(gfx, x, kind.title, cosmeticFilter == kind);
        }
        if (Boolean.getBoolean("octoclient.debugTabs") && !octoclient$tabsLogged) {
            octoclient$tabsLogged = true;
            System.out.println("[OctoClient] cosmetic tabs end=" + (x - SUBTAB_GAP)
                    + " available=" + (AX + AW) + " overflow=" + (x - SUBTAB_GAP > AX + AW));
        }
    }

    private static boolean octoclient$tabsLogged;

    private float drawSubTab(GuiGraphicsExtractor gfx, float x, String label, boolean active) {
        float w = subTabWidth(label);
        boolean hover = inside && inRect(mx, my, x, AY, w, SUBTAB_H);
        rrF(gfx, x, AY, w, SUBTAB_H, 8f, active ? W(34) : (hover ? W(22) : W(11)));
        if (active) {
            roF(gfx, x, AY, w, SUBTAB_H, 8f, W(74));
        }
        Draw.ui(gfx, label, x + SUBTAB_PAD,
                AY + (SUBTAB_H - 8f * S_LABEL) / 2f + 1f,
                active ? Theme.TEXT_PRIMARY : (hover ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED),
                S_LABEL, false, false);
        return x + w + SUBTAB_GAP;
    }

    /** @return true when the click landed on the cosmetics sub-tab row. */
    private boolean clickCosmeticTabs(float x, float y) {
        if (!showsCosmeticTabs() || !inRect(x, y, AX, AY, AW, SUBTAB_H)) {
            return false;
        }
        float cx = AX;
        if (x < cx + subTabWidth("All")) {
            selectCosmetic(null);
            return true;
        }
        cx += subTabWidth("All") + SUBTAB_GAP;
        for (CosmeticKind kind : CosmeticKind.values()) {
            float w = subTabWidth(kind.title);
            if (x < cx + w) {
                selectCosmetic(kind);
                return true;
            }
            cx += w + SUBTAB_GAP;
        }
        return true;
    }

    private void selectCosmetic(CosmeticKind kind) {
        if (cosmeticFilter == kind) {
            return;
        }
        cosmeticFilter = kind;
        scroll = 0f;
        scrollTarget = 0f;
        refreshLayout();
    }

    private void selectCategory(ModuleCategory cat) {
        if (cat != category) {
            catAnim = 0f;
        }
        category = cat;
        search = "";
        scroll = 0f;
        scrollTarget = 0f;
        for (CardAnim a : cardAnims.values()) {
            a.open = false;
        }
        expandedModule = null;
        expandedIndex = -1;
        refreshLayout();
    }

    private void drawCards(GuiGraphicsExtractor gfx, float partialTick) {
        int top = Math.round(cardsTop());
        gfx.enableScissor(AX, top, AX + Math.round(contentW()), AY + AH);

        List<CardRect> list = cardLayout();
        Module radiusOnly = radiusFocusModule();
        for (int i = 0; i < list.size(); i++) {
            CardRect c = list.get(i);
            Module m = c.module;
            if (radiusOnly != null && m != radiusOnly) {
                continue;
            }
            CardAnim a = anim(m);
            float appear = appear(i);
            if (appear < 0.02f) {
                continue;
            }

            float lift = a.expand > 0.02f ? 0f : a.lift * CARD_LIFT;
            float x = c.x;
            float y = c.y - lift + (1f - appear) * 10f;
            float w = c.w;
            float h = c.h;
            if (y + h < cardsTop() || y > AY + AH) {
                continue;
            }
            // Per-module roundness: the slider on the card also shapes the HUD panel.
            float cardR = Math.min(m.cornerRadiusPx(), Math.min(w, h) / 2f);

            // Per-module style, resolved before the body is drawn.
            String style = m.effectiveCardStyle();

            // A soft accent halo behind an active card; "Clear" stays neutral.
            if (a.glow > 0.01f && !style.equals("Clear")) {
                Draw.shapeF(gfx, x - 3f, y - 2f, w + 6f, h + 6f, cardR + 3f,
                        Draw.withAlphaF(ACCENT, (18f * a.glow + 18f * a.flash) / 255f * appear));
            }

            int tint = Theme.mix(WHITE, 0xDCE6FF, Anim.clamp01(a.onA));
            int border = Math.round(Anim.clamp(
                    120f * a.onA + (34f + a.hover * 50f) * (1f - a.onA) + 90f * a.flash, 0f, 255f));

            Draw.pushSnap(gfx, x, y);
            int ix = Math.round(x);
            int iy = Math.round(y);
            int iw = Math.round(w);
            int ih = Math.round(h);

            // Card body: clear liquid glass by default, an outline-only "Clear"
            // look or a solid panel; the Card Style setting can override this.
            if (style.equals("Clear")) {
                roF(gfx, x, y, w, h, cardR,
                        fade(Theme.withAlpha(0xE8ECF2, Math.max(38, a.onA > 0.5f ? 92 : 48)), appear));
                roF(gfx, x + 1f, y + 1f, w - 2f, h - 2f, Math.max(0f, cardR - 1f),
                        fade(W(Math.round(10 + 14 * a.hover)), appear));
            } else if (style.equals("Solid")) {
                rrF(gfx, x, y, w, h, cardR, fade(0xEE111216, appear));
                roF(gfx, x, y, w, h, cardR, fade(Theme.withAlpha(WHITE, border), appear));
            } else if (style.equals(ClientSettings.STYLE_FROSTED)) {
                // Dark frosted glass: matte graphite pane, heavy backdrop frost,
                // one hairline rim. The rim is capped so it stays fine even when
                // the card is active or hovered.
                Draw.frostedGlass(gfx, ix, iy, iw, ih, Math.round(cardR),
                        Theme.mix(Theme.FROST_TINT, tint, 0.35f),
                        (0.94f + a.hover * 0.03f) * appear, a.hover, Math.min(96, border));
            } else {
                // Real liquid glass: smoked refractive body plus the shader rim,
                // so the cards read as glass panes instead of flat plates.
                Draw.liquidGlass(gfx, ix, iy, iw, ih, Math.round(cardR),
                        Theme.mix(PANE_TINT, tint, 0.22f),
                        (0.56f + a.hover * 0.08f) * appear, a.hover, Math.max(78, border));
            }

            // Compact card, like the reference: name and description on the
            // left, then the favourite star, the switch and the three-dot
            // settings handle. The icon tile is gone so three fit in a row.
            Draw.ui(gfx, fit(m.getName(), w - 76f, S_NAME, true), x + 12f, y + 7f,
                    INK, S_NAME, true, true);
            Draw.ui(gfx, fit(m.getDescription(), w - 78f, S_DESC), x + 12f, y + 20f,
                    INK_MUTED, S_DESC, false, true);

            float starX = x + w - 56f;
            boolean starHover = inside && inRect(mx, my, starX - 9f, y + 8f, 18f, 22f);
            Draw.uiIcon(gfx, UiFonts.ICON_STAR, starX, y + 19f,
                    m.isFavorited() ? 0xFFD9A32F : (starHover ? INK_SOFT : INK_FAINT), 0.8f);

            switchLight(gfx, x + w - 45f, y + 12f, 27f, 14f, a.onA);

            // Three dots open the inspector; they stay dim when a module has
            // nothing to configure.
            boolean hasSettings = !visibleSettings(m).isEmpty();
            boolean dotsHover = inside && inRect(mx, my, x + w - 20f, y + 4f, 18f, CARD_H - 4f);
            int dotColor = hasSettings
                    ? (dotsHover || a.open ? INK : INK_FAINT)
                    : fade(INK_FAINT, 0.35f);
            for (int dot = -1; dot <= 1; dot++) {
                Draw.shapeF(gfx, x + w - 12f, y + 19f + dot * 4f - 1f, 2f, 2f, 1f, dotColor);
            }

            if (a.press > 0.01f) {
                Draw.shapeF(gfx, x, y, w, h, cardR, B(Math.round(70f * a.press)));
            }

            if (a.expand > 0.02f) {
                drawAccordion(gfx, c, a, x, y, w, h);
            }

            Draw.popSnap(gfx);
        }

        if (list.isEmpty()) {
            String hint = search.isEmpty() ? "Nothing here" : "No utility found";
            Draw.ui(gfx, hint, AX + contentW() / 2f - Draw.uiWidth(hint, false) * S_TEXT / 2f, cardsTop() + 20f,
                    fade(Theme.TEXT_FAINT, catAnim), S_TEXT, false, false);
        }

        gfx.disableScissor();
    }

    /**
     * Side panel in the Cosmetics category: the local player rendered with all
     * currently selected cosmetics, so the design choice is visible immediately.
     */
    private void drawCosmeticPreview(GuiGraphicsExtractor gfx, float partialTick) {
        if (!showsCosmeticPreview()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        float px = previewX();
        float py = previewY();
        float pw = previewW();
        float ph = previewH();
        if (ph < 40f) {
            return;
        }

        Draw.uiIcon(gfx, UiFonts.ICON_SHIRT, px + 7f, SET_Y + 20f, INK_MUTED, 1.0f);
        Draw.ui(gfx, "Preview", px + 20f, SET_Y + 13f, INK, 1.2f, true, false);

        float top = py + 8f;
        float bottom = py + ph - 24f;
        int x0 = Math.round(offX + px * ui);
        int y0 = Math.round(offY + top * ui);
        int x1 = Math.round(offX + (px + pw) * ui);
        int y1 = Math.round(offY + bottom * ui);
        if (x1 - x0 > 8 && y1 - y0 > 8) {
            // The entity is a picture-in-picture texture placed in raw GUI
            // pixels, so use screen coordinates for the panel rect.
            int size = Math.max(10, Math.round(Math.min((x1 - x0) / 2.6f, (y1 - y0) / 2.6f)));
            renderPreviewEntity(gfx, mc, x0, y0, x1, y1, size, partialTick, null);
        }

        String name = mc.player.getName().getString();
        Draw.ui(gfx, fit(name, pw, 0.95f), px, SET_Y + SET_H - 46f, INK_SOFT, 0.95f, false, false);
        Draw.ui(gfx, "Drag to rotate", px, SET_Y + SET_H - 32f, INK_FAINT, 0.92f, false, false);
    }

    /**
     * Renders the local player into the preview panel: no nametag above the
     * model, and the rotation follows right-mouse dragging.
     */
    private void renderPreviewEntity(GuiGraphicsExtractor gfx, Minecraft mc, int x0, int y0, int x1, int y1,
                                     int size, float partialTick, CosmeticLoadout cosmeticPreview) {
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        EntityRenderState state = dispatcher.getRenderer(mc.player).createRenderState(mc.player, partialTick);
        if (state instanceof AvatarRenderState avatar) {
            avatar.headEquipment = ItemStack.EMPTY;
            avatar.chestEquipment = ItemStack.EMPTY;
            avatar.legsEquipment = ItemStack.EMPTY;
            avatar.feetEquipment = ItemStack.EMPTY;
            avatar.rightHandItemStack = ItemStack.EMPTY;
            avatar.leftHandItemStack = ItemStack.EMPTY;
            avatar.showCape = false;
            CosmeticPreviewState previewState = (CosmeticPreviewState) avatar;
            previewState.octoclient$setCosmeticPreview(cosmeticPreview != null);
            previewState.octoclient$setCosmeticPreviewLoadout(cosmeticPreview);
            if (cosmeticPreview != null && avatar.skin != null) {
                ClientAsset.Texture transparentTexture = new ClientAsset.Texture() {
                    @Override
                    public Identifier id() {
                        return TRANSPARENT_PREVIEW_TEXTURE;
                    }

                    @Override
                    public Identifier texturePath() {
                        return TRANSPARENT_PREVIEW_TEXTURE;
                    }
                };
                PlayerSkin skin = avatar.skin;
                avatar.skin = new PlayerSkin(transparentTexture, skin.cape(), skin.elytra(),
                        skin.model(), skin.secure());
            }
        }
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180f + (cosmeticPreview == null ? previewYaw : 0f);
            living.yRot = 0f;
            living.xRot = 0f;
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1f;
        }
        // The floating name tag must not appear inside the preview.
        state.nameTag = null;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        float bob = state.boundingBoxHeight / 2f + 0.0625f;
        if (cosmeticPreview == null) {
            bob += (float) Math.sin(time * 1.8f) * 0.0125f;
        }
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        float pitch = cosmeticPreview == null ? previewPitch : 0f;
        Quaternionf xRotation = new Quaternionf().rotateX(pitch * (float) (Math.PI / 180.0));
        rotation.mul(xRotation);
        gfx.entity(state, size, new Vector3f(0f, bob, 0f), rotation, xRotation, x0, y0, x1, y1);
    }

    private static CosmeticLoadout cosmeticPreviewFor(Module module) {
        if (module instanceof CatalogCosmetic catalogCosmetic) {
            return new CosmeticLoadout("", "", false, 0, false, 0, false, 0,
                    CosmeticLoadout.DEFAULT_AURA_SHAPE, "", "", "", "",
                    catalogCosmetic.entry().key());
        }
        if (!(module instanceof CosmeticPart part) || part.kind() == CosmeticKind.NAMETAG) {
            return null;
        }
        if (module instanceof Cape cape) {
            return new CosmeticLoadout("", "", false, 0, false, 0, false, 0,
                    CosmeticLoadout.DEFAULT_AURA_SHAPE, cape.fileName(), "", "", "");
        }
        if (module instanceof Bandana bandana) {
            return new CosmeticLoadout(bandana.getName(), "", false, 0, false, 0, false, 0,
                    CosmeticLoadout.DEFAULT_AURA_SHAPE, "", "", "", "");
        }
        if (module instanceof Wings wings) {
            return new CosmeticLoadout("", wings.designName(), false, 0, false, 0, false, 0,
                    CosmeticLoadout.DEFAULT_AURA_SHAPE, "", "", "", "");
        }
        if (module instanceof Accessory accessory) {
            return new CosmeticLoadout("", "", false, 0, false, 0, false, 0,
                    CosmeticLoadout.DEFAULT_AURA_SHAPE, "", accessory.designName(), "",
                    accessory.minecraftUser());
        }
        if (module instanceof HatDesign hatDesign) {
            return new CosmeticLoadout("", "", false, 0, false, 0, false, 0,
                    CosmeticLoadout.DEFAULT_AURA_SHAPE, "", "", hatDesign.designName(), "");
        }
        if (module instanceof Hat hat) {
            return new CosmeticLoadout("", "", true, hat.color.getRgb(), false, 0, false, 0,
                    CosmeticLoadout.DEFAULT_AURA_SHAPE, "", "", "", "");
        }
        if (module instanceof Halo halo) {
            return new CosmeticLoadout("", "", false, 0, true, halo.color.getRgb(), false, 0,
                    CosmeticLoadout.DEFAULT_AURA_SHAPE, "", "", "", "");
        }
        if (module instanceof Aura aura) {
            return new CosmeticLoadout("", "", false, 0, false, 0, true, aura.color.getRgb(),
                    aura.designName(), "", "", "", "");
        }
        return null;
    }

    /** True when the design-space point lies on the cosmetics preview panel. */
    private boolean inPreviewPanel(float x, float y) {
        if (!showsCosmeticPreview()) {
            return false;
        }
        return inRect(x, y, previewX(), previewY(), previewW(), previewH());
    }

    // The settings live in the right-hand inspector, like the reference: a
    // single scrolling column under the module description, split into a
    // CONTROL and a VISUAL group.
    private float popupWidth() {
        return SET_W - SET_PAD * 2f;
    }

    private float popupHeight() {
        return SET_H - 108f;
    }

    private float popupX() {
        return SET_X + SET_PAD;
    }

    private float popupY() {
        return SET_Y + 96f;
    }

    private boolean isSectionOpen(Module module, SettingSection section) {
        EnumSet<SettingSection> open = openSections.get(module);
        return open != null && open.contains(section);
    }

    private void expandSection(Module module, SettingSection section) {
        openSections.computeIfAbsent(module, k -> EnumSet.noneOf(SettingSection.class)).add(section);
    }

    private void toggleSection(Module module, SettingSection section) {
        EnumSet<SettingSection> open = openSections.computeIfAbsent(module,
                k -> EnumSet.noneOf(SettingSection.class));
        if (!open.remove(section)) {
            open.add(section);
        }
        // An option list inside a group that just closed would otherwise float
        // over the collapsed cards.
        openMode = null;
    }

    /**
     * The inspector laid out as separated cards: one header card, then one card
     * per setting group. Groups start collapsed, so a long module no longer
     * fills the whole column; a group of one keeps its row visible.
     */
    private List<Section> popupSections() {
        List<Section> sections = new ArrayList<>();
        popupContentH = 0f;
        if (popupModule == null) {
            return sections;
        }
        Map<SettingSection, List<Setting<?>>> groups = new LinkedHashMap<>();
        for (Setting<?> s : visibleSettings(popupModule)) {
            groups.computeIfAbsent(SettingSection.of(s), k -> new ArrayList<>()).add(s);
        }
        float x = SET_X + SET_CARD_INSET;
        float w = SET_W - SET_CARD_INSET * 2f;
        float y = popupY() - popupScroll;
        for (Map.Entry<SettingSection, List<Setting<?>>> entry : groups.entrySet()) {
            SettingSection kind = entry.getKey();
            List<Setting<?>> list = entry.getValue();
            float headerH = list.size() > 1 ? SEC_HEADER_H : 0f;
            Section sec = new Section(kind, x, y, w, headerH,
                    headerH == 0f || isSectionOpen(popupModule, kind));
            float rowsY = y + headerH + SEC_PAD_Y;
            float rowsH = 0f;
            if (sec.open) {
                float rowX = x + SEC_PAD_X;
                float rowW = w - SEC_PAD_X * 2f;
                float ry = rowsY;
                for (Setting<?> s : list) {
                    float rh = rowHeight(s);
                    sec.rows.add(new Row(s, rowX, ry, rowW, rh));
                    ry += rh;
                }
                rowsH = ry - rowsY;
            }
            sec.h = headerH + SEC_PAD_Y + rowsH + SEC_PAD_Y;
            sections.add(sec);
            y += sec.h + SEC_GAP;
        }
        popupContentH = Math.max(0f, y - SEC_GAP - (popupY() - popupScroll));
        return sections;
    }

    private List<Row> popupSettingsLayout() {
        List<Row> rows = new ArrayList<>();
        for (Section sec : popupSections()) {
            rows.addAll(sec.rows);
        }
        return rows;
    }

    private float popupTrackY() {
        return SET_Y + 96f;
    }

    private float popupTrackH() {
        return SET_H - 108f;
    }

    private float popupThumbH(float max) {
        float trackH = popupTrackH();
        return Math.max(24f, trackH * trackH / (trackH + max));
    }

    private float popupMaxScroll() {
        if (popupModule == null) {
            return 0f;
        }
        popupSections();
        float total = popupContentH;
        if (previewModule(popupModule) != null) {
            total += MODULE_PREVIEW_H + 10f;
        }
        return Math.max(0f, total - popupHeight());
    }

    /** Row that anchors the open option list, or null once it scrolled away. */
    private Row modeMenuRow() {
        if (openMode == null) {
            return null;
        }
        for (Row row : popupSettingsLayout()) {
            if (row.setting == openMode) {
                return row;
            }
        }
        return null;
    }

    private float modeMenuX() {
        Row row = modeMenuRow();
        return row == null ? popupX() : row.x;
    }

    private float modeMenuW() {
        Row row = modeMenuRow();
        return row == null ? popupWidth() : row.w;
    }

    private float modeMenuH() {
        return openMode == null ? 0f : openMode.getOptions().size() * MODE_ITEM_H + 8f;
    }

    /** Under its row, or above it when the panel ends too soon. */
    private float modeMenuY() {
        Row row = modeMenuRow();
        if (row == null) {
            return popupY();
        }
        float h = modeMenuH();
        float y = row.y + 26f;
        float maxY = SET_Y + SET_H - 8f - h;
        if (y > maxY) {
            y = row.y - h - 2f;
        }
        return Math.max(popupTrackY(), Math.min(y, Math.max(popupTrackY(), maxY)));
    }

    /**
     * Values of the open mode setting, listed over the settings so e.g. the
     * dashboard style is picked directly instead of cycling one click at a time.
     */
    private void drawModeMenu(GuiGraphicsExtractor gfx) {
        if (openMode == null) {
            return;
        }
        Row row = modeMenuRow();
        float h = modeMenuH();
        if (row == null || h <= 0f) {
            openMode = null;
            return;
        }
        float x = modeMenuX();
        float y = modeMenuY();
        float w = modeMenuW();
        // Opaque body: the list must hide the settings rows it covers.
        rrF(gfx, x, y, w, h, 8f, 0xFF0C0E13);
        gfx.fill(Math.round(x + 4f), Math.round(y + 4f), Math.round(x + w - 4f), Math.round(y + h - 4f),
                0xFF0C0E13);
        roF(gfx, x, y, w, h, 8f, W(64));
        List<String> options = openMode.getOptions();
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            float iy = y + 4f + i * MODE_ITEM_H;
            boolean hover = inside && inRect(mx, my, x + 3f, iy, w - 6f, MODE_ITEM_H - 2f);
            boolean selected = option.equals(openMode.getValue());
            if (hover) {
                rrF(gfx, x + 3f, iy, w - 6f, MODE_ITEM_H - 2f, 6f, W(30));
            }
            Draw.ui(gfx, fit(option, w - 34f, S_LABEL), x + 9f, iy + 6f,
                    selected ? INK : INK_SOFT, S_LABEL, selected, false);
            if (selected) {
                Draw.uiIcon(gfx, UiFonts.ICON_CHECK, x + w - 12f, iy + 10f, INK_SOFT, 0.7f);
            }
        }
    }

    /** Steps the inspector to the previous/next module of the current tab. */
    private void switchModule(int delta) {
        List<Module> list = visibleModules();
        if (list.isEmpty()) {
            return;
        }
        int index = list.indexOf(popupModule);
        index = index < 0 ? 0 : Math.floorMod(index + delta, list.size());
        popupModule = list.get(index);
        popupClosing = false;
        popupScroll = 0f;
        openMode = null;
        editingText = null;
        capturingKeybind = null;
    }

    /** Right-hand inspector: separate rounded cards over the pane, like the launcher. */
    private void drawSettingsPanel(GuiGraphicsExtractor gfx) {
        float cardX = SET_X + SET_CARD_INSET;
        float cardW = SET_W - SET_CARD_INSET * 2f;
        float headY = SET_Y + SET_CARD_INSET;
        if (popupModule == null) {
            // Docked inspector with nothing selected: its own card with a hint
            // (the cosmetics preview fills this column instead).
            inspectorCard(gfx, cardX, headY, cardW, 58f);
            Draw.uiIcon(gfx, UiFonts.ICON_GEAR, cardX + 12f, headY + 20f, INK_MUTED, 1.0f);
            Draw.ui(gfx, "Module settings", cardX + 25f, headY + 13f, INK, 1.2f, true, false);
            if (!showsCosmeticPreview()) {
                drawWrapped(gfx, "Open a module's menu to edit it here.", cardX + 12f, headY + 34f,
                        cardW - 24f, INK_FAINT, 0.9f, 2);
            }
            return;
        }

        // Header card: the open module plus arrows that step through the tab's
        // modules, so another module is one click away.
        inspectorCard(gfx, cardX, headY, cardW, HEADER_CARD_H);
        float padX = cardX + 10f;
        Draw.ui(gfx, fit(popupModule.getName(), cardW - 96f, 1.1f), padX, headY + 11f,
                INK, 1.1f, true, false);
        boolean prevHover = inside && inRect(mx, my, PREV_X, SET_Y + 8f, 20f, 24f);
        boolean nextHover = inside && inRect(mx, my, NEXT_X, SET_Y + 8f, 20f, 24f);
        Draw.uiIcon(gfx, UiFonts.ICON_CHEVRON_LEFT, PREV_X + 10f, SET_Y + 20f,
                prevHover ? INK : INK_FAINT, 0.7f);
        Draw.uiIcon(gfx, UiFonts.ICON_CHEVRON_RIGHT, NEXT_X + 10f, SET_Y + 20f,
                nextHover ? INK : INK_FAINT, 0.7f);

        boolean closeHover = inside && inRect(mx, my, SET_X + SET_W - 34f, SET_Y + 8f, 30f, 28f);
        Draw.uiIcon(gfx, UiFonts.ICON_XMARK, SET_X + SET_W - 19f, SET_Y + 22f,
                closeHover ? INK : INK_FAINT, 0.9f);

        drawWrapped(gfx, popupModule.getDescription(), padX, headY + 34f,
                cardW - 20f, INK_MUTED, 0.98f, 3);

        int top = SET_Y + 90;
        int bottom = SET_Y + SET_H - 12;
        gfx.enableScissor(Math.round(SET_X + 2f), top, Math.round(SET_X + SET_W - 4f), bottom);
        List<Section> sections = popupSections();
        // The glyph pass is flushed after the geometry, so rows under an open
        // option list would print their text straight through it; skip them.
        float menuTop = openMode == null ? Float.NaN : modeMenuY();
        float menuBottom = Float.isNaN(menuTop) ? Float.NaN : menuTop + modeMenuH();
        for (Section sec : sections) {
            if (sec.y + sec.h < top || sec.y > bottom) {
                continue;
            }
            drawSection(gfx, sec, menuTop, menuBottom);
        }
        if (previewModule(popupModule) != null) {
            float py = popupY() - popupScroll + popupContentH + 8f;
            drawModulePreview(gfx, popupModule, SET_X + SET_CARD_INSET + SEC_PAD_X, py,
                    SET_W - (SET_CARD_INSET + SEC_PAD_X) * 2f, 1f);
        }
        gfx.disableScissor();
        drawModeMenu(gfx);

        // Scrollbar for a long settings list. It is draggable, not just a
        // position indicator, so every setting can be reached without a wheel.
        float max = popupMaxScroll();
        if (max > 0f) {
            float trackX = SET_X + SET_W - 6f;
            float trackY = popupTrackY();
            float trackH = popupTrackH();
            boolean hover = inside && inRect(mx, my, trackX - 5f, trackY, 10f, trackH);
            Draw.shapeF(gfx, trackX, trackY, 2f, trackH, 1f, W(hover ? 30 : 16));
            float thumbH = popupThumbH(max);
            float thumbY = trackY + Anim.clamp01(popupScroll / max) * (trackH - thumbH);
            Draw.shapeF(gfx, trackX, thumbY, 2f, thumbH, 1f, W(hover ? 140 : 95));
        }
    }

    /** One separated inspector card: a bordered panel with rounded corners. */
    private static void inspectorCard(GuiGraphicsExtractor gfx, float x, float y, float w, float h) {
        rrF(gfx, x, y, w, h, 10f, W(14));
        roF(gfx, x, y, w, h, 10f, W(30));
    }

    /** A group card: clickable header with chevron, then its settings rows. */
    private void drawSection(GuiGraphicsExtractor gfx, Section sec, float menuTop, float menuBottom) {
        boolean headHover = sec.headerH > 0f && inside
                && inRect(mx, my, sec.x, sec.y, sec.w, sec.headerH);
        rrF(gfx, sec.x, sec.y, sec.w, sec.h, 9f, W(sec.open ? 16 : 9));
        roF(gfx, sec.x, sec.y, sec.w, sec.h, 9f, W(headHover ? 62 : 32));
        if (sec.headerH > 0f) {
            if (headHover) {
                rrF(gfx, sec.x + 1f, sec.y + 1f, sec.w - 2f, sec.headerH - 2f, 8f, W(18));
            }
            Draw.ui(gfx, sec.kind.label, sec.x + SEC_PAD_X, sec.y + 7f,
                    headHover ? INK : INK_MUTED, 0.9f, false, false);
            Draw.uiIcon(gfx, sec.open ? UiFonts.ICON_CHEVRON_UP : UiFonts.ICON_CHEVRON,
                    sec.x + sec.w - 13f, sec.y + 10f, headHover ? INK : INK_FAINT, 0.7f);
            gfx.fill(Math.round(sec.x + 1f), Math.round(sec.y + sec.headerH),
                    Math.round(sec.x + sec.w - 1f), Math.round(sec.y + sec.headerH) + 1, W(22));
        }
        for (Row row : sec.rows) {
            // Only rows that start inside the list are hidden; the row that
            // owns it stays visible with its chevron flipped up.
            if (!Float.isNaN(menuTop) && row.y > menuTop - 1f && row.y < menuBottom) {
                continue;
            }
            drawSettingRow(gfx, row, row.y, 1f);
        }
    }

    /** Word-wrapped muted paragraph, capped at {@code maxLines} lines. */
    private static void drawWrapped(GuiGraphicsExtractor gfx, String text, float x, float y,
                                    float width, int argb, float scale, int maxLines) {
        if (text == null || text.isEmpty()) {
            return;
        }
        List<String> lines = wrap(text, width, scale, maxLines);
        for (int i = 0; i < lines.size(); i++) {
            Draw.ui(gfx, lines.get(i), x, y + i * 12f, argb, scale, false, false);
        }
    }

    private static List<String> wrap(String text, float width, float scale, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() == 0 || Draw.uiWidth(candidate, false) * scale <= width) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        if (lines.size() > maxLines) {
            List<String> cut = new ArrayList<>(lines.subList(0, maxLines));
            cut.set(maxLines - 1, cut.get(maxLines - 1) + " ...");
            return cut;
        }
        return lines;
    }

    private void drawAccordion(GuiGraphicsExtractor gfx, CardRect c, CardAnim a,
                                float x, float y, float w, float h) {
        int sx0 = Math.round(x);
        int sy0 = Math.round(y + CARD_H - 2f);
        gfx.enableScissor(sx0, sy0, sx0 + Math.round(w), sy0 + Math.round(h - CARD_H + 2f));

        float fadeAmt = Anim.clamp01((a.expand - 0.02f) / 0.5f);
        float wellH = Math.max(0f, h - CARD_H - 6f);
        rrF(gfx, x + 8f, y + CARD_H + 3f, w - 16f, wellH, 9f, fade(B(86), fadeAmt));
        roF(gfx, x + 8f, y + CARD_H + 3f, w - 16f, wellH, 9f, fade(W(10), fadeAmt));
        gfx.fill(Math.round(x + 18f), Math.round(y + CARD_H + 3f), Math.round(x + w - 18f),
                Math.round(y + CARD_H + 3f) + 1, fade(W(12), fadeAmt));

        List<Row> rows = settingsLayout(c);
        float n = Math.max(1, rows.size());
        for (int ri = 0; ri < rows.size(); ri++) {
            Row row = rows.get(ri);
            float ra = Anim.clamp01((a.expand * 1.3f - (ri / n) * 0.55f) / 0.55f);
            if (ra < 0.02f) {
                continue;
            }
            drawSettingRow(gfx, row, row.y + (1f - ra) * 7f, fadeAmt * ra);
        }

        if (previewModule(c.module) != null) {
            float py = c.y + CARD_H + 10f;
            for (Row row : rows) {
                py += row.h;
            }
            drawModulePreview(gfx, c.module, x + CARD_PAD + 4f, py + 2f,
                    w - (CARD_PAD + 4f) * 2f, fadeAmt);
        }

        gfx.disableScissor();
    }

    private void drawSettingRow(GuiGraphicsExtractor gfx, Row row, float y, float alpha) {
        Setting<?> s = row.setting;
        float x = row.x;
        float w = row.w;
        boolean hoverRow = inside && inRect(mx, my, x, y, w, row.h);

        if (s instanceof BooleanSetting b) {
            Draw.ui(gfx, fit(b.getName(), w - 40f, S_LABEL), x, y + 8f,
                    fade(INK_SOFT, alpha), S_LABEL, false, false);
            SetAnim sa = anim(b);
            switchLight(gfx, x + w - 30f, y + 5.5f, 30f, 15f, sa.a);
            return;
        }

        if (s instanceof NumberSetting n) {
            Draw.ui(gfx, fit(n.getName(), w - 80f, S_LABEL), x, y + 5f,
                    fade(INK_SOFT, alpha), S_LABEL, false, false);
            String val = formatNumber(n.getSnapped());
            Draw.ui(gfx, val, x + w - Draw.uiWidth(val, false) * S_LABEL, y + 5f,
                    fade(INK, alpha), S_LABEL, false, false);
            sliderLight(gfx, x, y + 25f, w, n.getFraction(), alpha);
            return;
        }

        if (s instanceof ModeSetting mode) {
            // Label left, value pill right, like a launcher account row.
            boolean open = openMode == mode;
            Draw.ui(gfx, fit(mode.getName(), w - 112f, S_LABEL), x, y + 8f,
                    fade(INK_SOFT, alpha), S_LABEL, false, false);
            SetAnim sa = anim(mode);
            float pw = sa.pw < 0f ? Math.min(96f, Math.round(Draw.uiWidth(mode.getValue(), false)
                    * S_LABEL) + 26f) : sa.pw;
            float px = x + w - pw;
            boolean pillHover = inside && inRect(mx, my, px, y + 4f, pw, 18f);
            controlPill(gfx, px, y + 4f, pw, 18f, pillHover || open, alpha);
            if (open) {
                roF(gfx, px, y + 4f, pw, 18f, 7f, fade(W(80), alpha));
            }
            Draw.ui(gfx, fit(mode.getValue(), pw - 22f, S_LABEL), px + 7f, y + 8f,
                    fade(INK, alpha), S_LABEL, false, false);
            Draw.uiIcon(gfx, open ? UiFonts.ICON_CHEVRON_UP : UiFonts.ICON_CHEVRON,
                    px + pw - 9f, y + 13f, fade(INK_FAINT, alpha), 0.7f);
            return;
        }

        if (s instanceof TextSetting text) {
            Draw.ui(gfx, fit(text.getName(), w - 112f, S_LABEL), x, y + 13f,
                    fade(INK_SOFT, alpha), S_LABEL, false, false);
            float fieldX = x + w - 104f;
            boolean focused = editingText == text;
            boolean fieldHover = inside && inRect(mx, my, fieldX, y + 7f, 104f, 22f);
            controlPill(gfx, fieldX, y + 7f, 104f, 22f, fieldHover || focused, alpha);
            String value = fit(text.getValue(), 88f, S_LABEL);
            Draw.ui(gfx, value, fieldX + 6f, y + 14f,
                    fade(INK, alpha), S_LABEL, false, false);
            if (focused && ((int) Math.floor(time * 2f)) % 2 == 0) {
                float caretX = fieldX + 6f + Draw.uiWidth(value, false) * S_LABEL + 1f;
                rrF(gfx, Math.min(fieldX + 96f, caretX), y + 13f, 1f, 10f, 0f, fade(0xCC000000, alpha));
            }
            return;
        }

        if (s instanceof KeybindSetting keybind) {
            Draw.ui(gfx, fit(keybind.getName(), w - 112f, S_LABEL), x, y + 8f,
                    fade(INK_SOFT, alpha), S_LABEL, false, false);
            float fieldX = x + w - 104f;
            boolean capturing = capturingKeybind == keybind;
            String label = capturing ? "Press key..." : keybind.getDisplayName();
            controlPill(gfx, fieldX, y + 3f, 104f, 22f,
                    inside && inRect(mx, my, fieldX, y + 3f, 104f, 22f), alpha);
            if (capturing) {
                roF(gfx, fieldX, y + 3f, 104f, 22f, 7f, fade(0xFF6E5BD8, alpha));
            }
            Draw.ui(gfx, fit(label, 92f, S_LABEL), fieldX + 6f, y + 10f,
                    fade(capturing ? 0xFF6E5BD8 : INK, alpha), S_LABEL, false, false);
            return;
        }

        if (s instanceof ActionSetting action) {
            Draw.ui(gfx, fit(action.getName(), w - 112f, S_LABEL), x, y + 8f,
                    fade(INK_SOFT, alpha), S_LABEL, false, false);
            float fieldX = x + w - 104f;
            boolean hover = inside && inRect(mx, my, fieldX, y + 3f, 104f, 22f);
            controlPill(gfx, fieldX, y + 3f, 104f, 22f, hover, alpha);
            Draw.ui(gfx, fit(action.getValue(), 92f, S_LABEL), fieldX + 6f, y + 10f,
                    fade(hover ? INK : INK_SOFT, alpha), S_LABEL, false, false);
            return;
        }

        if (s instanceof ColorSetting col) {
            Draw.ui(gfx, fit(col.getName(), w - 56f, S_LABEL), x, y + 8f,
                    fade(INK_SOFT, alpha), S_LABEL, false, false);

            float swX = x + w - 44f;
            boolean swHover = inside && inRect(mx, my, swX, y + 4f, 44f, 18f);
            controlPill(gfx, swX, y + 4f, 44f, 18f, swHover, alpha);
            Draw.shapeF(gfx, swX + 4f, y + 8f, 36f, 10f, 3f,
                    fade(colorArgb(col), alpha));

            if (isColorOpen(col)) {
                int[] vals = {col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha()};
                String[] names = {"R", "G", "B", "A"};
                int[] cols = {0xFFFF6B6B, 0xFF6BFF8E, 0xFF6B8EFF, 0xFFE8E8E8};
                int[] palette = {
                        0xFFFFFFFF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55,
                        0xFF55FF55, 0xFF55FFFF, 0xFF5555FF, 0xFFFF55FF,
                        0xFFFF8080, 0xFF80FF80, 0xFF80BFFF, 0xFFB080FF
                };
                float paletteX = x + 18f;
                for (int i = 0; i < palette.length; i++) {
                    float px = paletteX + i * 15f;
                    rrF(gfx, px, y + 30f, 12f, 10f, 2f, fade(palette[i], alpha));
                    if ((col.getValue() & 0xFFFFFF) == (palette[i] & 0xFFFFFF)) {
                        roF(gfx, px - 1f, y + 29f, 14f, 12f, 3f, fade(W(220), alpha));
                    }
                }
                float hueX = x + 18f;
                float hueY = y + 44f;
                float pickerW = w - 48f;
                for (int i = 0; i < 24; i++) {
                    float segmentX = hueX + pickerW * i / 24f;
                    float segmentRight = hueX + pickerW * (i + 1) / 24f;
                    int hueColor = 0xFF000000 | net.minecraft.util.Mth.hsvToRgb(i / 24f, 1f, 1f);
                    gfx.fill(Math.round(segmentX), Math.round(hueY),
                            Math.max(Math.round(segmentX) + 1, Math.round(segmentRight)),
                            Math.round(hueY + 10f), fade(hueColor, alpha));
                }
                float[] hsv = rgbToHsv(col.getRed(), col.getGreen(), col.getBlue());
                float squareY = y + 58f;
                float squareH = 48f;
                int columns = 32;
                int rows = 12;
                for (int pickerRow = 0; pickerRow < rows; pickerRow++) {
                    float value = 1f - pickerRow / (float) (rows - 1);
                    for (int column = 0; column < columns; column++) {
                        float saturation = column / (float) (columns - 1);
                        int color = 0xFF000000 | net.minecraft.util.Mth.hsvToRgb(hsv[0], saturation, value);
                        int left = Math.round(hueX + pickerW * column / columns);
                        int right = Math.max(left + 1, Math.round(hueX + pickerW * (column + 1) / columns));
                        int top = Math.round(squareY + squareH * pickerRow / rows);
                        int bottom = Math.max(top + 1, Math.round(squareY + squareH * (pickerRow + 1) / rows));
                        gfx.fill(left, top, right, bottom, fade(color, alpha));
                    }
                }
                float selectorX = hueX + hsv[1] * pickerW;
                float selectorY = squareY + (1f - hsv[2]) * squareH;
                rrF(gfx, selectorX - 2f, selectorY - 2f, 5f, 5f, 2.5f, fade(0xFF101116, alpha));
                roF(gfx, selectorX - 2f, selectorY - 2f, 5f, 5f, 2.5f, fade(0xFFFFFFFF, alpha));
                float hueSelectorX = hueX + hsv[0] * pickerW;
                gfx.fill(Math.round(hueSelectorX) - 1, Math.round(hueY) - 1,
                        Math.round(hueSelectorX) + 2, Math.round(hueY + 11f),
                        fade(0xFFFFFFFF, alpha));
                for (int i = 0; i < 4; i++) {
                    float ry = y + 112f + i * 22f;
                    Draw.ui(gfx, names[i], x, ry + 4f, fade(Theme.TEXT_FAINT, alpha),
                            S_TEXT, false, false);
                    float barX = x + 18f;
                    float barW = w - 18f - 30f;
                    boolean hot = draggedColor == col && draggedColorChannel == i;
                    Draw.sliderF(gfx, barX, ry + 6f, barW, vals[i] / 255f,
                            fade(cols[i], alpha), hot);
                    String vs = String.valueOf(vals[i]);
                    Draw.ui(gfx, vs, x + w - Draw.uiWidth(vs, false) * S_TEXT, ry + 4f,
                            fade(Theme.TEXT_FAINT, alpha), S_TEXT, false, false);
                }
            }
        }
    }

    private static int colorArgb(ColorSetting c) {
        return (c.getAlpha() << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    private static float[] rgbToHsv(int red, int green, int blue) {
        float r = red / 255f;
        float g = green / 255f;
        float b = blue / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue = 0f;
        if (delta > 0f) {
            if (max == r) {
                hue = ((g - b) / delta) % 6f;
            } else if (max == g) {
                hue = (b - r) / delta + 2f;
            } else {
                hue = (r - g) / delta + 4f;
            }
            hue /= 6f;
            if (hue < 0f) {
                hue += 1f;
            }
        }
        float saturation = max == 0f ? 0f : delta / max;
        return new float[]{hue, saturation, max};
    }

    private void drawScrollbar(GuiGraphicsExtractor gfx) {
        float max = maxScroll();
        if (max <= 0f) {
            return;
        }
        float sbX = AX + contentW() + 6f;
        float thumbH = Math.max(28f, AH * AH / (AH + max));
        float t = Anim.clamp01(scroll / max);
        Draw.shapeF(gfx, sbX, AY, 2f, AH, 1f, W(26));
        Draw.shapeF(gfx, sbX, AY + t * (AH - thumbH), 2f, thumbH, 1f, W(130));
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Ignore clicks while the window is still scaling in.
        if (intro < 0.98f) {
            return true;
        }
        float x = (float) ((event.x() - offX) / ui) - layoutShiftX;
        float y = (float) ((event.y() - offY) / ui);
        mx = x;
        my = y;
        inside = x >= 0f && y >= 0f && x <= VW && y <= VH;
        if (popupClosing) {
            return true;
        }
        // The inspector is a persistent side panel: clicks inside it belong to
        // its settings, and it does not close when the module sheet is clicked.
        if (popupModule != null) {
            // An open option list swallows the click: inside it picks a value,
            // outside it only closes the list.
            if (openMode != null) {
                float menuY = modeMenuY();
                if (inRect(x, y, modeMenuX(), menuY, modeMenuW(), modeMenuH())) {
                    int index = (int) ((y - menuY - 4f) / MODE_ITEM_H);
                    List<String> options = openMode.getOptions();
                    if (index >= 0 && index < options.size()) {
                        openMode.select(options.get(index));
                    }
                }
                openMode = null;
                return true;
            }
            if (inRect(x, y, PREV_X, SET_Y + 8f, 20f, 24f)) {
                switchModule(-1);
                return true;
            }
            if (inRect(x, y, NEXT_X, SET_Y + 8f, 20f, 24f)) {
                switchModule(1);
                return true;
            }
            if (inRect(x, y, SET_X + SET_W - 34f, SET_Y + 8f, 30f, 28f)) {
                closePopup();
                return true;
            }
            for (Section sec : popupSections()) {
                if (sec.headerH > 0f && inRect(x, y, sec.x, sec.y, sec.w, sec.headerH)) {
                    toggleSection(popupModule, sec.kind);
                    return true;
                }
            }
            float popupMax = popupMaxScroll();
            if (popupMax > 0f
                    && inRect(x, y, SET_X + SET_W - 11f, popupTrackY() - 4f, 10f, popupTrackH() + 8f)) {
                float thumbH = popupThumbH(popupMax);
                float travel = Math.max(1f, popupTrackH() - thumbH);
                float thumbY = popupTrackY() + Anim.clamp01(popupScroll / popupMax) * travel;
                popupScrollbarGrab = Anim.clamp01((y - thumbY) / Math.max(1f, thumbH));
                if (y < thumbY || y > thumbY + thumbH) {
                    popupScroll = Anim.clamp((y - popupTrackY() - thumbH * 0.5f) / travel * popupMax,
                            0f, popupMax);
                }
                draggingPopupScrollbar = true;
                return true;
            }
            for (Row row : popupSettingsLayout()) {
                if (inRect(x, y, row.x, row.y, row.w, row.h)) {
                    clickSetting(row, x, y);
                    return true;
                }
            }
            if (inRect(x, y, SET_X, SET_Y, SET_W, SET_H)) {
                return true;
            }
        }
        editingText = null;
        // The nav column and the inspector sit beside the sheet, so their clicks
        // must not be filtered out by a sheet-only bounds check.
        if (!inside) {
            return true;
        }

        // Right click on a module opens its settings (and closes the others)
        // without toggling the module itself; right clicking the open card
        // again closes it.
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (inPreviewPanel(x, y)) {
                previewDragging = true;
                previewDragX = x;
                previewDragY = y;
                return true;
            }
            CardRect rc = hitCard(x, y);
            if (rc != null && !visibleSettings(rc.module).isEmpty()) {
                openSettings(rc.module);
                anim(rc.module).press = 1f;
                refreshLayout();
            }
            return true;
        }

        // Left click on the preview rotates the model too, like right click.
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && inPreviewPanel(x, y)) {
            previewDragging = true;
            previewDragX = x;
            previewDragY = y;
            return true;
        }

        // Search field (clear button takes priority when the field has text).
        if (inRect(x, y, searchX(), SEARCH_Y, SEARCH_W, SEARCH_H)) {
            if (!search.isEmpty() && inRect(x, y, searchX() + SEARCH_W - 30f, SEARCH_Y, 30f, SEARCH_H)) {
                search = "";
                refreshLayout();
                return true;
            }
            searchFocused = true;
            searchSelectAll = false;
            return true;
        }
        searchFocused = false;
        searchSelectAll = false;

        if (cosmeticsOnly) {
            float pillX = SIDE_X + 8f;
            float pillW = SIDE_W - 16f;
            float rowH = 25f;
            float rowStep = 27f;
            for (int i = 0; i < CosmeticSection.values().length; i++) {
                float ry = SIDE_Y + 68f + i * rowStep;
                if (inRect(x, y, pillX, ry, pillW, rowH)) {
                    if (cosmeticSection != CosmeticSection.values()[i]) {
                        cosmeticSection = CosmeticSection.values()[i];
                        scroll = 0f;
                        scrollTarget = 0f;
                        refreshLayout();
                    }
                    return true;
                }
            }
        }

        if (!cosmeticsOnly) {
            // Nav column: our category tabs, the HUD editor and the configs row.
            List<ModuleCategory> sideCats = sidebarCategories();
            for (int i = 0; i < sideCats.size(); i++) {
                if (inRect(x, y, SIDE_X + RAIL_PAD, sidebarRowY(i), SIDE_W - RAIL_PAD * 2f,
                        SIDE_ROW_H)) {
                    selectCategory(sideCats.get(i));
                    return true;
                }
            }

            // HUD editor tile above the profile chip.
            if (inRect(x, y, SIDE_X + RAIL_PAD, hudRowY(), SIDE_W - RAIL_PAD * 2f, SIDE_ROW_H)) {
                ConfigManager.save();
                minecraft.setScreenAndShow(new EditHudScreen());
                return true;
            }

            // The settings tile opens the client settings page.
            if (inRect(x, y, SIDE_X + RAIL_PAD, configRowY(), SIDE_W - RAIL_PAD * 2f, SIDE_ROW_H)) {
                selectCategory(ModuleCategory.SETTINGS);
                return true;
            }
        }

        // Cosmetics sub-tabs (Capes / Wings / Hats / Aura).
        if (!cosmeticsOnly && clickCosmeticTabs(x, y)) {
            return true;
        }

        // Scrollbar.
        if (maxScroll() > 0f) {
            float sbX = AX + contentW() + 6f;
            if (inRect(x, y, sbX - 4f, AY, 10f, AH)) {
                draggingScrollbar = true;
                scrollbarGrab = Anim.clamp01(scroll / Math.max(1e-3f, maxScroll()));
                return true;
            }
        }

        CardRect c = hitCard(x, y);
        if (c == null) {
            return true;
        }
        Module m = c.module;
        CardAnim a = anim(m);

        // The three dots open the inspector, the star beside the switch toggles
        // the favorite marker (the Cosmetics "Premium" section filters on it).
        if (!visibleSettings(m).isEmpty()
                && inRect(x, y, c.x + c.w - 20f, c.y + 4f, 18f, CARD_H - 4f)) {
            openSettings(m);
            a.press = 1f;
            refreshLayout();
            return true;
        }
        if (inRect(x, y, c.x + c.w - 66f, c.y + 6f, 20f, CARD_H - 6f)) {
            m.setFavorited(!m.isFavorited());
            ConfigManager.save();
            return true;
        }

        // Settings controls.
        if (a.expand > 0.3f) {
            for (Row row : settingsLayout(c)) {
                if (clickSetting(row, x, y)) {
                    return true;
                }
            }
        }

        // Anything else on the closed card body toggles the module.
        if (!inRect(x, y, c.x, c.y + CARD_H - 2f, c.w, c.h)) {
            m.toggle();
            a.flash = m.isEnabled() ? 1f : 0f;
            a.press = 1f;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        float x = (float) ((event.x() - offX) / ui) - layoutShiftX;
        float y = (float) ((event.y() - offY) / ui);
        mx = x;
        my = y;

        if (previewDragging) {
            previewYaw += (float) (x - previewDragX) * 1.2f;
            previewPitch = Anim.clamp(previewPitch + (float) (y - previewDragY) * 0.8f, -35f, 35f);
            previewDragX = x;
            previewDragY = y;
            return true;
        }

        if (draggingScrollbar) {
            float frac = (y - AY) / Math.max(1f, AH);
            scrollTarget = Anim.clamp(frac * maxScroll(), 0f, maxScroll());
            return true;
        }
        if (draggingPopupScrollbar) {
            float max = popupMaxScroll();
            float thumbH = popupThumbH(max);
            float travel = Math.max(1f, popupTrackH() - thumbH);
            float frac = (y - popupTrackY() - popupScrollbarGrab * thumbH) / travel;
            popupScroll = Anim.clamp(frac * max, 0f, max);
            return true;
        }
        if (draggedSlider != null) {
            if (popupModule != null) {
                for (Row row : popupSettingsLayout()) {
                    if (row.setting == draggedSlider) {
                        draggedSlider.setFraction(Anim.clamp01((x - row.x) / row.w));
                        return true;
                    }
                }
                return true;
            }
            for (CardRect c : cardLayout()) {
                if (!visibleSettings(c.module).contains(draggedSlider)) {
                    continue;
                }
                for (Row row : settingsLayout(c)) {
                    if (row.setting == draggedSlider) {
                        draggedSlider.setFraction(Anim.clamp01((x - row.x) / row.w));
                        return true;
                    }
                }
            }
            return true;
        }
        if (draggedColor != null) {
            if (popupModule != null) {
                for (Row row : popupSettingsLayout()) {
                    if (row.setting == draggedColor) {
                        applyColorDrag(row, x, y);
                        return true;
                    }
                }
                return true;
            }
            for (CardRect c : cardLayout()) {
                if (!visibleSettings(c.module).contains(draggedColor)) {
                    continue;
                }
                for (Row row : settingsLayout(c)) {
                    if (row.setting == draggedColor) {
                        applyColorDrag(row, x, y);
                        return true;
                    }
                }
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggedSlider = null;
        draggedColor = null;
        draggedColorChannel = -1;
        draggingScrollbar = false;
        draggingPopupScrollbar = false;
        previewDragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float x = (float) ((mouseX - offX) / ui) - layoutShiftX;
        float y = (float) ((mouseY - offY) / ui);
        // The wheel belongs to the panel under the pointer: the inspector
        // scrolls its settings, the module sheet scrolls the card grid.
        if (popupModule != null && inRect(x, y, SET_X, SET_Y, SET_W, SET_H)) {
            openMode = null;
            popupScroll = Anim.clamp(popupScroll - (float) scrollY * 35f, 0f, popupMaxScroll());
            return true;
        }
        float max = maxScroll();
        if (max <= 0f) {
            return true;
        }
        scrollTarget = Anim.clamp(scrollTarget - (float) scrollY * 35f, 0f, max);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturingKeybind != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                capturingKeybind = null;
                swallowCharUntil = System.currentTimeMillis() + 200L;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_DELETE) {
                capturingKeybind.setValue(0);
                capturingKeybind = null;
                swallowCharUntil = System.currentTimeMillis() + 200L;
                return true;
            }
            capturingKeybind.setValue(event.key());
            capturingKeybind = null;
            // Minecraft sends charTyped separately from keyPressed, so without
            // this the bound letter would also be typed into the search bar.
            swallowCharUntil = System.currentTimeMillis() + 200L;
            return true;
        }
        if (popupModule != null && openMode != null && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            openMode = null;
            return true;
        }
        if (popupModule != null && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            closePopup();
            return true;
        }
        if (popupModule != null
                && (event.key() == GLFW.GLFW_KEY_LEFT_SHIFT || event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            closePopup();
            return true;
        }
        if (popupModule != null
                && (event.key() == GLFW.GLFW_KEY_PAGE_DOWN || event.key() == GLFW.GLFW_KEY_PAGE_UP)) {
            float page = popupHeight() * 0.8f;
            popupScroll = Anim.clamp(
                    popupScroll + (event.key() == GLFW.GLFW_KEY_PAGE_DOWN ? page : -page),
                    0f, popupMaxScroll());
            return true;
        }
        if (editingText != null) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                String value = editingText.getValue();
                if (!value.isEmpty()) {
                    editingText.setValue(value.substring(0, value.length() - 1));
                }
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_ESCAPE) {
                editingText = null;
                return true;
            }
        }
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT || event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        // Ctrl+A: select the whole query (the next character replaces it).
        if (popupModule == null && capturingKeybind == null && event.isSelectAll()) {
            if (!search.isEmpty()) {
                searchFocused = true;
                searchSelectAll = true;
            }
            return true;
        }
        if (!search.isEmpty() && event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            search = searchSelectAll ? "" : search.substring(0, search.length() - 1);
            searchSelectAll = false;
            refreshLayout();
            return true;
        }
        if (searchFocused && event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER) {
            searchFocused = false;
            searchSelectAll = false;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (System.currentTimeMillis() <= swallowCharUntil) {
            return true;
        }
        if (editingText != null) {
            String typed = event.codepointAsString();
            if (typed != null && !typed.isEmpty() && !Character.isISOControl(typed.charAt(0))
                    && editingText.getValue().length() < editingText.getMaxLength()) {
                editingText.setValue(editingText.getValue() + typed);
            }
            return true;
        }
        // Any printable key types into the search field, even when it was not
        // clicked first: the dashboard is meant to be searchable right away.
        if (popupModule == null && capturingKeybind == null) {
            String typed = event.codepointAsString();
            if (typed != null && !typed.isBlank()) {
                if (searchSelectAll) {
                    // Typing over a Ctrl+A selection replaces the whole query.
                    search = "";
                    searchSelectAll = false;
                    catAnim = 0f;
                }
                if (search.length() < 24) {
                    searchFocused = true;
                    if (search.isEmpty()) {
                        catAnim = 0f;
                    }
                    search += typed;
                    scroll = 0f;
                    scrollTarget = 0f;
                    refreshLayout();
                }
                return true;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        super.onClose();
    }

    private void refreshLayout() {
        // The visible list changed, so the scroll range may have shrunk.
        visibleModulesCache = null;
        cardLayoutDirty = true;
        float max = maxScroll();
        if (scrollTarget > max) {
            scrollTarget = max;
        }
    }

    private void openSettings(Module module) {
        editingText = null;
        openMode = null;
        if (ClientSettings.popupSettingsEnabled()) {
            if (popupModule == module) {
                closePopup();
                return;
            }
            popupModule = module;
            popupClosing = false;
            popupScroll = 0f;
            return;
        }
        popupModule = null;
        if (expandedModule != null && expandedModule != module) {
            CardAnim closing = anim(expandedModule);
            closing.expand = 0f;
            closing.spin = 0f;
        }
        boolean wasOpen = anim(module).open;
        for (Map.Entry<Module, CardAnim> entry : cardAnims.entrySet()) {
            entry.getValue().open = !wasOpen && entry.getKey() == module;
        }
        expandedModule = module;
        expandedIndex = visibleModules().indexOf(module);
    }

    private void closePopup() {
        popupClosing = true;
        openMode = null;
        editingText = null;
        capturingKeybind = null;
    }

    private boolean clickSetting(Row row, float x, float y) {
        if (!inRect(x, y, row.x, row.y, row.w, row.h)) {
            return false;
        }
        Setting<?> setting = row.setting;
        if (setting instanceof BooleanSetting bool) {
            bool.toggle();
        } else if (setting instanceof NumberSetting number) {
            draggedSlider = number;
            number.setFraction(Anim.clamp01((x - row.x) / row.w));
        } else if (setting instanceof ModeSetting mode) {
            if (popupModule != null) {
                // Inspector: list every option instead of cycling blind.
                openMode = openMode == mode ? null : mode;
            } else {
                mode.cycle();
            }
        } else if (setting instanceof TextSetting text) {
            editingText = text;
        } else if (setting instanceof KeybindSetting keybind) {
            capturingKeybind = keybind;
            editingText = null;
        } else if (setting instanceof ActionSetting action) {
            action.activate();
        } else if (setting instanceof ColorSetting color) {
            if (inRect(x, y, row.x, row.y, row.w, 30f)) {
                colorOpen.put(color, !isColorOpen(color));
                refreshLayout();
                return true;
            }
            if (isColorOpen(color)) {
                int[] palette = {
                        0xFFFFFFFF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55,
                        0xFF55FF55, 0xFF55FFFF, 0xFF5555FF, 0xFFFF55FF,
                        0xFFFF8080, 0xFF80FF80, 0xFF80BFFF, 0xFFB080FF
                };
                float paletteX = row.x + 18f;
                float paletteY = row.y + 30f;
                for (int i = 0; i < palette.length; i++) {
                    float swatchX = paletteX + i * 15f;
                    if (inRect(x, y, swatchX, paletteY, 12f, 10f)) {
                        color.setValue((color.getAlpha() << 24) | (palette[i] & 0xFFFFFF));
                        return true;
                    }
                }
                float barX = row.x + 18f;
                float barW = row.w - 48f;
                float hueY = row.y + 44f;
                if (inRect(x, y, barX, hueY, barW, 10f)) {
                    draggedColor = color;
                    draggedColorChannel = 4;
                    applyColorPicker(color, row, x, y, 4);
                    return true;
                }
                float squareY = row.y + 58f;
                if (inRect(x, y, barX, squareY, barW, 48f)) {
                    draggedColor = color;
                    draggedColorChannel = 5;
                    applyColorPicker(color, row, x, y, 5);
                    return true;
                }
                for (int i = 0; i < 4; i++) {
                    float barY = row.y + 118f + i * 22f;
                    if (inRect(x, y, barX, barY, barW, 16f)) {
                        applyColorChannel(color, i,
                                Math.round(255f * Anim.clamp01((x - barX) / barW)));
                        draggedColor = color;
                        draggedColorChannel = i;
                        return true;
                    }
                }
            }
        }
        return true;
    }

    private void applyColorDrag(Row row, float x, float y) {
        if (draggedColorChannel >= 4) {
            applyColorPicker(draggedColor, row, x, y, draggedColorChannel);
            return;
        }
        float barX = row.x + 18f;
        float barW = row.w - 48f;
        applyColorChannel(draggedColor, draggedColorChannel,
                Math.round(255f * Anim.clamp01((x - barX) / barW)));
    }

    private void applyColorPicker(ColorSetting color, Row row, float x, float y, int control) {
        float hueX = row.x + 18f;
        float pickerW = row.w - 48f;
        float[] hsv = rgbToHsv(color.getRed(), color.getGreen(), color.getBlue());
        if (control == 4) {
            hsv[0] = Anim.clamp01((x - hueX) / pickerW);
        } else {
            hsv[1] = Anim.clamp01((x - hueX) / pickerW);
            hsv[2] = 1f - Anim.clamp01((y - (row.y + 58f)) / 48f);
        }
        int rgb = net.minecraft.util.Mth.hsvToRgb(hsv[0], hsv[1], hsv[2]);
        color.setRgba((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, color.getAlpha());
    }

    private void applyColorChannel(ColorSetting color, int channel, int v) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();
        switch (channel) {
            case 0 -> r = v;
            case 1 -> g = v;
            case 2 -> b = v;
            default -> a = v;
        }
        color.setRgba(r, g, b, a);
    }

    // ---------------------------------------------------------------- helpers

    private static boolean inRect(double px, double py, double x, double y, double w, double h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    /**
     * Truncates to {@code maxWidth} design pixels at the given text scale.
     * The safety margin compensates for Minecraft rounding every glyph advance
     * up to a whole pixel inside the scaled pose, which makes rendered text a
     * few percent wider than the measured width.
     */
    private static String fit(String text, float maxWidth, float scale) {
        return fit(text, maxWidth, scale, false);
    }

    private static String fit(String text, float maxWidth, float scale, boolean bold) {
        float limit = maxWidth / WIDTH_SAFETY;
        if (Draw.uiWidth(text, bold) * scale <= limit) {
            return text;
        }
        String out = text;
        while (!out.isEmpty() && Draw.uiWidth(out + "...", bold) * scale > limit) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "...";
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-6) {
            return String.valueOf((long) Math.rint(value));
        }
        String s = String.format(Locale.ROOT, "%.2f", value);
        while (s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private enum CosmeticSection {
        ALL("All Cosmetics", Items.ENDER_CHEST),
        PREMIUM("Premium", Items.NETHER_STAR),
        CAPES("Cloaks", Items.ELYTRA),
        ACCESSORIES("Accessories", Items.LEATHER_CHESTPLATE),
        BOOTS("Boots", Items.DIAMOND_BOOTS),
        BANDANAS("Bandanas", Items.LEATHER_HELMET),
        SHIELDS("Shields", Items.SHIELD),
        WINGS("Wings", Items.PHANTOM_MEMBRANE),
        WEAPONS("Weapons", Items.DIAMOND_SWORD),
        COMPANIONS("Companions", Items.LEAD),
        AURAS("Auras", Items.END_ROD),
        ICONS("Icons", Items.NAME_TAG),
        BLC_ICONS("BLC Icons", Items.NAME_TAG);

        private final String title;
        private final Item icon;

        CosmeticSection(String title, Item icon) {
            this.title = title;
            this.icon = icon;
        }
    }
}
