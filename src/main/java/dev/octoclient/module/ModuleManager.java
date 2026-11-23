package dev.octoclient.module;

import dev.octoclient.module.hud.ArmorStatusHud;
import dev.octoclient.module.hud.ArraylistHud;
import dev.octoclient.module.hud.ClockHud;
import dev.octoclient.module.hud.ComboCounter;
import dev.octoclient.module.hud.CreatorCodeDashboard;
import dev.octoclient.module.hud.Friends;
import dev.octoclient.module.hud.CompassHud;
import dev.octoclient.module.hud.CoordsHud;
import dev.octoclient.module.hud.CpsHud;
import dev.octoclient.module.hud.DayCounter;
import dev.octoclient.module.hud.JumpResetHud;
import dev.octoclient.module.hud.DirectionHud;
import dev.octoclient.module.hud.FpsHud;
import dev.octoclient.module.hud.HudModule;
import dev.octoclient.module.hud.KeystrokesHud;
import dev.octoclient.module.hud.ItemCounterHud;
import dev.octoclient.module.hud.MusaArmorHud;
import dev.octoclient.module.hud.LightLevelHud;
import dev.octoclient.module.hud.MemoryHud;
import dev.octoclient.module.hud.PingHud;
import dev.octoclient.module.hud.PotionHud;
import dev.octoclient.module.hud.ResourcePackDisplay;
import dev.octoclient.module.hud.MouseTracker;
import dev.octoclient.module.hud.SpotifyOverlay;
import dev.octoclient.module.hud.ReachDisplay;
import dev.octoclient.module.hud.ServerInfoHud;
import dev.octoclient.module.hud.SpeedHud;
import dev.octoclient.module.hud.SaturationHud;
import dev.octoclient.module.hud.SessionHud;
import dev.octoclient.module.hud.ShieldStatusHud;
import dev.octoclient.module.hud.ShieldHud;
import dev.octoclient.module.hud.ShulkerPreview;
import dev.octoclient.module.hud.Skin3DHud;
import dev.octoclient.module.hud.StatsHud;
import dev.octoclient.module.hud.TargetHud;
import dev.octoclient.module.hud.TiersHud;
import dev.octoclient.module.hud.TpsHud;
import dev.octoclient.module.hud.TntTimerHud;
import dev.octoclient.module.hud.UptimeHud;
import dev.octoclient.module.hud.Watermark;
import dev.octoclient.module.hud.Waypoints;
import dev.octoclient.module.cosmetics.Aura;
import dev.octoclient.module.cosmetics.AuraDesigns;
import dev.octoclient.module.cosmetics.Accessory;
import dev.octoclient.module.cosmetics.AccessoryDesigns;
import dev.octoclient.module.cosmetics.Bandana;
import dev.octoclient.module.cosmetics.BandanaDesigns;
import dev.octoclient.module.cosmetics.CatalogCosmetic;
import dev.octoclient.module.cosmetics.Cape;
import dev.octoclient.module.cosmetics.ColoredName;
import dev.octoclient.module.cosmetics.CosmeticCatalog;
import dev.octoclient.module.cosmetics.Halo;
import dev.octoclient.module.cosmetics.Hat;
import dev.octoclient.module.cosmetics.HatDesign;
import dev.octoclient.module.cosmetics.HatDesigns;
import dev.octoclient.module.cosmetics.NameText;
import dev.octoclient.module.cosmetics.WingDesigns;
import dev.octoclient.module.cosmetics.Wings;
import dev.octoclient.module.render.Fullbright;
import dev.octoclient.module.render.ColorSaturation;
import dev.octoclient.module.render.FovChanger;
import dev.octoclient.module.render.HealthIndicator;
import dev.octoclient.module.render.Nametags;
import dev.octoclient.module.render.NoFog;
import dev.octoclient.module.render.NoHurtCam;
import dev.octoclient.module.render.BadOptimization;
import dev.octoclient.module.render.GlintColorizer;
import dev.octoclient.module.render.HitColor;
import dev.octoclient.module.render.Hitbox;
import dev.octoclient.module.render.ItemModel;
import dev.octoclient.module.render.ItemHighlighter;
import dev.octoclient.module.render.CustomCrosshair;
import dev.octoclient.module.render.ActionBar;
import dev.octoclient.module.render.ArrowTrail;
import dev.octoclient.module.render.BlockOutlines;
import dev.octoclient.module.render.ChatAnimations;
import dev.octoclient.module.render.ClearBackground;
import dev.octoclient.module.render.MotionBlur;
import dev.octoclient.module.render.NameProtect;
import dev.octoclient.module.render.NoBobbing;
import dev.octoclient.module.render.NoFov;
import dev.octoclient.module.render.OldAnimations;
import dev.octoclient.module.render.Particles;
import dev.octoclient.module.render.NoWeather;
import dev.octoclient.module.render.TimeChanger;
import dev.octoclient.module.render.LowFire;
import dev.octoclient.module.render.NoAdvancement;
import dev.octoclient.module.render.PackTweaks;
import dev.octoclient.module.render.ShinyPots;
import dev.octoclient.module.render.Scoreboard;
import dev.octoclient.module.render.SideShield;
import dev.octoclient.module.render.SwingSpeed;
import dev.octoclient.module.render.Titles;
import dev.octoclient.module.render.Tips;
import dev.octoclient.module.render.ToggleAnimations;
import dev.octoclient.module.render.Zoom;
import dev.octoclient.module.settings.ClientSettings;
import dev.octoclient.module.utility.AutoGG;
import dev.octoclient.module.utility.DropStack;
import dev.octoclient.module.utility.Emotes;
import dev.octoclient.module.utility.Autotext;
import dev.octoclient.module.utility.McReal;
import dev.octoclient.module.utility.ScreenshotModule;
import dev.octoclient.module.utility.AutoReconnect;
import dev.octoclient.module.utility.AutoRespawn;
import dev.octoclient.module.utility.ChatSuffix;
import dev.octoclient.module.utility.FreeLook;
import dev.octoclient.module.utility.Panic;
import dev.octoclient.module.utility.Sprint;
import dev.octoclient.module.utility.ServerQuickConnect;
import dev.octoclient.module.utility.ResourcepackOrganizer;
import dev.octoclient.module.utility.WheelModule;
import dev.octoclient.module.utility.SkyLyricsModule;
import dev.octoclient.module.utility.StreamerMode;
import dev.octoclient.module.secret.AutoEat;
import dev.octoclient.module.secret.AutoTool;
import dev.octoclient.module.secret.FastPlace;
import dev.octoclient.module.secret.NoBreakDelay;
import dev.octoclient.module.secret.AutoWalk;
import dev.octoclient.module.secret.AutoJump;
import dev.octoclient.module.secret.AntiAfk;
import dev.octoclient.module.secret.KillAura;
import dev.octoclient.module.secret.Velocity;
import dev.octoclient.module.secret.Reach;
import dev.octoclient.module.secret.Fly;
import dev.octoclient.module.secret.Speed;
import dev.octoclient.module.secret.Step;
import dev.octoclient.module.secret.NoFall;
import dev.octoclient.module.secret.Scaffold;
import dev.octoclient.module.secret.Tracers;
import dev.octoclient.module.secret.StorageESP;
import dev.octoclient.module.secret.AutoTotem;
import dev.octoclient.module.secret.AutoArmor;
import dev.octoclient.module.secret.AutoClicker;
import dev.octoclient.module.secret.NoSlow;
import dev.octoclient.module.secret.Nuker;
import dev.octoclient.module.secret.HoleESP;
import dev.octoclient.module.secret.AntiVoid;
import dev.octoclient.module.secret.Criticals;
import dev.octoclient.module.secret.AirJump;
import dev.octoclient.module.secret.HighJump;
import dev.octoclient.module.secret.LongJump;
import dev.octoclient.module.secret.Spider;
import dev.octoclient.module.secret.Jesus;
import dev.octoclient.module.secret.SafeWalk;
import dev.octoclient.module.secret.AntiHunger;
import dev.octoclient.module.secret.ChestSwap;
import dev.octoclient.module.secret.MaceAura;
import dev.octoclient.module.secret.PlayerESP;
import dev.octoclient.module.secret.AnchorAura;
import dev.octoclient.module.secret.AutoAnchor;
import dev.octoclient.module.secret.AnchorPlace;
import dev.octoclient.module.secret.AnchorCharge;
import dev.octoclient.module.secret.AnchorExplode;
import dev.octoclient.module.secret.AnchorPrediction;
import dev.octoclient.module.secret.AnchorDamage;
import dev.octoclient.module.secret.AnchorTarget;
import dev.octoclient.module.secret.AnchorSwitch;
import dev.octoclient.module.secret.AnchorMacro;
import dev.octoclient.module.secret.CrystalAura;
import dev.octoclient.module.secret.AutoCrystal;
import dev.octoclient.module.secret.CrystalPlace;
import dev.octoclient.module.secret.CrystalBreak;
import dev.octoclient.module.secret.CrystalOptimizer;
import dev.octoclient.module.secret.CrystalPrediction;
import dev.octoclient.module.secret.CrystalDamage;
import dev.octoclient.module.secret.CrystalTarget;
import dev.octoclient.module.secret.CrystalRange;
import dev.octoclient.module.secret.CrystalRotation;
import dev.octoclient.module.secret.CrystalSwitch;
import dev.octoclient.module.secret.CrystalSurround;
import dev.octoclient.module.secret.CrystalTrap;
import dev.octoclient.module.secret.CrystalAntiTotem;
import dev.octoclient.module.secret.CrystalMacro;
import dev.octoclient.module.secret.AimAssist;
import dev.octoclient.module.secret.TriggerBot;
import dev.octoclient.module.secret.AntiKnockback;
import dev.octoclient.module.secret.AutoBlock;
import dev.octoclient.module.secret.TargetStrafe;
import dev.octoclient.module.secret.Backtrack;
import dev.octoclient.module.secret.ProjectileAura;
import dev.octoclient.module.secret.AutoWeapon;
import dev.octoclient.module.secret.FastBow;
import dev.octoclient.module.secret.MultiAura;
import dev.octoclient.module.secret.SpearAura;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Holds every registered module, ordered for the dashboard grid. */
public final class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();

    private ModuleManager() {
        // HUD (icons are the real Minecraft item textures shown on cards)
        register(withIcon(new Watermark(), Items.DIAMOND));
        register(withIcon(new StatsHud(), Items.EMERALD));
        register(withIcon(new KeystrokesHud(), Items.REDSTONE));
        register(withIcon(new ArmorStatusHud(), Items.DIAMOND_CHESTPLATE));
        register(withIcon(new TargetHud(), Items.DIAMOND_SWORD));
        register(withIcon(new ArraylistHud(), Items.PAPER));
        register(withIcon(new CompassHud(), Items.COMPASS));
        register(withIcon(new PotionHud(), Items.POTION));
        register(withIcon(new ResourcePackDisplay(), Items.BOOK));
        register(withIcon(new MouseTracker(), Items.SPYGLASS));
        register(withIcon(new SpotifyOverlay(), Items.MUSIC_DISC_CAT));
        register(withIcon(new CoordsHud(), Items.MAP));
        register(withIcon(new ClockHud(), Items.CLOCK));
        register(withIcon(new MemoryHud(), Items.BOOKSHELF));
        register(withIcon(new SpeedHud(), Items.SUGAR));
        register(withIcon(new CpsHud(), Items.FEATHER));
        register(withIcon(new PingHud(), Items.ENDER_PEARL));
        register(withIcon(new ServerInfoHud(), Items.OAK_SIGN));
        register(withIcon(new DirectionHud(), Items.COMPASS));
        register(withIcon(new FpsHud(), Items.CLOCK));
        register(withIcon(new SaturationHud(), Items.COOKED_BEEF));
        register(withIcon(new SessionHud(), Items.CLOCK));
        register(withIcon(new UptimeHud(), Items.CLOCK));
        register(withIcon(new TpsHud(), Items.CLOCK));
        register(withIcon(new JumpResetHud(), Items.RABBIT_FOOT));
        register(withIcon(new DayCounter(), Items.BELL));
        register(withIcon(new LightLevelHud(), Items.TORCH));
        register(withIcon(new ComboCounter(), Items.IRON_SWORD));
        register(withIcon(new ReachDisplay(), Items.FISHING_ROD));
        register(withIcon(new ShieldHud(), Items.SHIELD));
        register(withIcon(new ShieldStatusHud(), Items.SHIELD));
        register(withIcon(new TntTimerHud(), Items.TNT));
        register(withIcon(new ShulkerPreview(), Items.SHULKER_BOX));
        register(withIcon(new Skin3DHud(), Items.PLAYER_HEAD));
        register(withIcon(new TiersHud(), Items.NAME_TAG));
        register(withIcon(new CreatorCodeDashboard(), Items.EMERALD));
        register(withIcon(new Friends(), Items.PLAYER_HEAD));
        register(withIcon(new ItemCounterHud(), Items.CHEST));
        register(withIcon(new MusaArmorHud(), Items.DIAMOND_CHESTPLATE));
        register(withIcon(new Waypoints(), Items.COMPASS));

        // Render
        register(withIcon(new Fullbright(), Items.GLOWSTONE_DUST));
        register(withIcon(new CustomCrosshair(), Items.TARGET));
        register(withIcon(new OldAnimations(), Items.IRON_SWORD));
        register(withIcon(new SwingSpeed(), Items.IRON_SWORD));
        register(withIcon(new HitColor(), Items.REDSTONE));
        register(withIcon(new Hitbox(), Items.GLASS));
        register(withIcon(new ClearBackground(), Items.GLASS));
        register(withIcon(new NoWeather(), Items.WATER_BUCKET));
        register(withIcon(new TimeChanger(), Items.CLOCK));
        register(withIcon(new Scoreboard(), Items.PAPER));
        register(withIcon(new Titles(), Items.NAME_TAG));
        register(withIcon(new ActionBar(), Items.OAK_SIGN));
        register(withIcon(new ArrowTrail(), Items.ARROW));
        register(withIcon(new BlockOutlines(), Items.STONE));
        register(withIcon(new Particles(), Items.GLOWSTONE_DUST));
        register(withIcon(new GlintColorizer(), Items.ENCHANTED_BOOK));
        register(withIcon(new ItemModel(), Items.DIAMOND_SWORD));
        register(withIcon(new PackTweaks(), Items.PAINTING));
        register(withIcon(new LowFire(), Items.FLINT_AND_STEEL));
        register(withIcon(new SideShield(), Items.SHIELD));
        register(withIcon(new NoAdvancement(), Items.TOTEM_OF_UNDYING));
        register(withIcon(new HealthIndicator(), Items.GOLDEN_APPLE));
        register(withIcon(new Nametags(), Items.NAME_TAG));
        register(withIcon(new NoHurtCam(), Items.LEATHER_CHESTPLATE));
        register(withIcon(new BadOptimization(), Items.REDSTONE));
        register(withIcon(new ItemHighlighter(), Items.COMPASS));
        register(withIcon(new ShinyPots(), Items.SPLASH_POTION));
        register(withIcon(new Tips(), Items.BOOK));
        register(withIcon(new NameProtect(), Items.NAME_TAG));
        register(withIcon(new ToggleAnimations(), Items.CLOCK));
        register(withIcon(new FovChanger(), Items.SPYGLASS));
        register(withIcon(new Zoom(), Items.SPYGLASS));
        register(withIcon(new NoWeather(), Items.WATER_BUCKET));
        register(withIcon(new NoBobbing(), Items.LEATHER_BOOTS));
        register(withIcon(new NoFov(), Items.SUGAR));
        register(withIcon(new ChatAnimations(), Items.WRITABLE_BOOK));
        register(withIcon(new NoFog(), Items.GLASS));
        register(withIcon(new ColorSaturation(), Items.POTION));
        register(withIcon(new MotionBlur(), Items.CLOCK));

        // Utility
        register(withIcon(new Sprint(), Items.LEATHER_BOOTS));
        register(withIcon(new DropStack(), Items.DIAMOND));
        register(withIcon(new Autotext(), Items.OAK_SIGN));
        register(withIcon(new ScreenshotModule(), Items.PAINTING));
        register(withIcon(new FreeLook(), Items.BOW));
        register(withIcon(new ChatSuffix(), Items.OAK_SIGN));
        register(withIcon(new AutoGG(), Items.GOLDEN_APPLE));
        register(withIcon(new AutoRespawn(), Items.TOTEM_OF_UNDYING));
        register(withIcon(new Panic(), Items.ARROW));
        register(withIcon(new StreamerMode(), Items.TINTED_GLASS));
        register(withIcon(new AutoReconnect(), Items.RECOVERY_COMPASS));
        register(withIcon(new ServerQuickConnect(), Items.COMPASS));
        register(withIcon(new ResourcepackOrganizer(), Items.BOOKSHELF));
        register(withIcon(new Emotes(), Items.NOTE_BLOCK));
        register(withIcon(new McReal(), Items.PAINTING));
        register(withIcon(new WheelModule(), Items.COMPASS));
        register(withIcon(new SkyLyricsModule(), Items.MUSIC_DISC_CAT));

        // Extra client-side quality-of-life modules, only shown after the hidden tab is unlocked.
        register(withIcon(new AutoEat(), Items.COOKED_BEEF));
        register(withIcon(new AutoTool(), Items.IRON_PICKAXE));
        register(withIcon(new FastPlace(), Items.BRICKS));
        register(withIcon(new NoBreakDelay(), Items.DIAMOND_PICKAXE));
        register(withIcon(new AutoWalk(), Items.LEATHER_BOOTS));
        register(withIcon(new AutoJump(), Items.RABBIT_FOOT));
        register(withIcon(new AntiAfk(), Items.CLOCK));

        // Combat / movement / world modules ported from LiquidBounce and Meteor.
        register(withIcon(new KillAura(), Items.NETHERITE_SWORD));
        register(withIcon(new Velocity(), Items.SHIELD));
        register(withIcon(new Reach(), Items.SPYGLASS));
        register(withIcon(new Fly(), Items.ELYTRA));
        register(withIcon(new Speed(), Items.SUGAR));
        register(withIcon(new Step(), Items.LEATHER_BOOTS));
        register(withIcon(new NoFall(), Items.FEATHER));
        register(withIcon(new Scaffold(), Items.SCAFFOLDING));
        register(withIcon(new Tracers(), Items.ARROW));
        register(withIcon(new StorageESP(), Items.CHEST));
        register(withIcon(new AutoTotem(), Items.TOTEM_OF_UNDYING));
        register(withIcon(new AutoArmor(), Items.DIAMOND_CHESTPLATE));
        register(withIcon(new AutoClicker(), Items.LEVER));
        register(withIcon(new NoSlow(), Items.SWEET_BERRIES));
        register(withIcon(new Nuker(), Items.NETHERITE_PICKAXE));
        register(withIcon(new HoleESP(), Items.OBSIDIAN));
        register(withIcon(new AntiVoid(), Items.ENDER_PEARL));
        register(withIcon(new Criticals(), Items.IRON_SWORD));
        register(withIcon(new AirJump(), Items.FEATHER));
        register(withIcon(new HighJump(), Items.RABBIT_FOOT));
        register(withIcon(new LongJump(), Items.RABBIT_HIDE));
        register(withIcon(new Spider(), Items.COBWEB));
        register(withIcon(new Jesus(), Items.WATER_BUCKET));
        register(withIcon(new SafeWalk(), Items.SCAFFOLDING));
        register(withIcon(new AntiHunger(), Items.ROTTEN_FLESH));
        register(withIcon(new ChestSwap(), Items.ELYTRA));
        register(withIcon(new MaceAura(), Items.MACE));
        register(withIcon(new SpearAura(), Items.TRIDENT));
        register(withIcon(new PlayerESP(), Items.PLAYER_HEAD));

        // Respawn-anchor auras and their single-step feature modules.
        register(withIcon(new AnchorAura(), Items.RESPAWN_ANCHOR));
        register(withIcon(new AutoAnchor(), Items.RESPAWN_ANCHOR));
        register(withIcon(new AnchorPlace(), Items.RESPAWN_ANCHOR));
        register(withIcon(new AnchorCharge(), Items.GLOWSTONE));
        register(withIcon(new AnchorExplode(), Items.FLINT_AND_STEEL));
        register(withIcon(new AnchorPrediction(), Items.COMPASS));
        register(withIcon(new AnchorDamage(), Items.TNT));
        register(withIcon(new AnchorTarget(), Items.TARGET));
        register(withIcon(new AnchorSwitch(), Items.GLOWSTONE_DUST));
        register(withIcon(new AnchorMacro(), Items.LEVER));

        // End-crystal auras and their single-step feature modules.
        register(withIcon(new CrystalAura(), Items.END_CRYSTAL));
        register(withIcon(new AutoCrystal(), Items.END_CRYSTAL));
        register(withIcon(new CrystalPlace(), Items.END_CRYSTAL));
        register(withIcon(new CrystalBreak(), Items.IRON_PICKAXE));
        register(withIcon(new CrystalOptimizer(), Items.ENDER_EYE));
        register(withIcon(new CrystalPrediction(), Items.ENDER_EYE));
        register(withIcon(new CrystalDamage(), Items.TNT));
        register(withIcon(new CrystalTarget(), Items.TARGET));
        register(withIcon(new CrystalRange(), Items.SPYGLASS));
        register(withIcon(new CrystalRotation(), Items.COMPASS));
        register(withIcon(new CrystalSwitch(), Items.END_CRYSTAL));
        register(withIcon(new CrystalSurround(), Items.OBSIDIAN));
        register(withIcon(new CrystalTrap(), Items.CRYING_OBSIDIAN));
        register(withIcon(new CrystalAntiTotem(), Items.TOTEM_OF_UNDYING));
        register(withIcon(new CrystalMacro(), Items.LEVER));

        // Extra combat helpers.
        register(withIcon(new AimAssist(), Items.SPECTRAL_ARROW));
        register(withIcon(new TriggerBot(), Items.CROSSBOW));
        register(withIcon(new AntiKnockback(), Items.SHIELD));
        register(withIcon(new AutoBlock(), Items.SHIELD));
        register(withIcon(new TargetStrafe(), Items.FEATHER));
        register(withIcon(new Backtrack(), Items.CLOCK));
        register(withIcon(new ProjectileAura(), Items.SNOWBALL));
        register(withIcon(new AutoWeapon(), Items.NETHERITE_SWORD));
        register(withIcon(new FastBow(), Items.BOW));
        register(withIcon(new MultiAura(), Items.NETHERITE_SWORD));

        // Existing custom cosmetics and the bundled searchable catalog.
        register(withIcon(new Cape("Cloud", "68f25dab817305fac760fc8e.gif"), Items.ELYTRA));
        register(withIcon(new Cape("Galaxy", "68f25e6a817305fac760fed9.gif"), Items.ELYTRA));
        register(withIcon(new Cape("Shadow", "68f2800084c544a757d23ea1.gif"), Items.ELYTRA));
        register(withIcon(new Cape("Aldi", "68f30a718ac4a645dc51ccaf.png"), Items.ELYTRA));
        register(withIcon(new Cape("Lightning", "68fe6fbe8ac4a645dc529ee7.gif"), Items.ELYTRA));
        register(withIcon(new Cape("Orbit", "6a236be691b47a1fbef95266.gif"), Items.ELYTRA));
        register(withIcon(new Cape("Rainbow", "skinmc-cape.png"), Items.ELYTRA));
        for (String design : BandanaDesigns.names()) {
            register(withIcon(new Bandana(design), Items.LEATHER_HELMET));
        }
        for (String design : WingDesigns.names()) {
            register(withIcon(new Wings(design), Items.PHANTOM_MEMBRANE));
        }
        for (String design : AccessoryDesigns.names()) {
            register(withIcon(new Accessory(design), Items.LEAD));
        }
        register(withIcon(new Hat(), Items.LEATHER_HELMET));
        for (String design : HatDesigns.names()) {
            register(withIcon(new HatDesign(design), Items.LEATHER_HELMET));
        }
        register(withIcon(new ColoredName(), Items.NAME_TAG));
        register(withIcon(new NameText(), Items.OAK_SIGN));
        register(withIcon(new Halo(), Items.GOLD_NUGGET));
        for (String design : AuraDesigns.OPTIONS) {
            register(withIcon(new Aura(design), Items.END_ROD));
        }
        for (CosmeticCatalog.Entry cosmetic : CosmeticCatalog.entries()) {
            register(withIcon(new CatalogCosmetic(cosmetic), CatalogCosmetic.iconFor(cosmetic)));
        }

        // Settings
        register(withIcon(new ClientSettings(), Items.AMETHYST_SHARD));

        modules.sort(Comparator.comparingInt(m -> m.getCategory().ordinal()));
        // Registration order changed under the sort; drop any snapshot taken
        // before it so the first read sees the catalog order.
        moduleSnapshot = null;
        hudSnapshot = null;
        categorySnapshots.clear();
    }

    private static Module withIcon(Module module, Item item) {
        module.withIconItem(item);
        return module;
    }

    public static ModuleManager get() {
        return INSTANCE;
    }

    /** Snapshots are rebuilt after a registration, not on every frame. */
    private List<Module> moduleSnapshot;
    private List<HudModule> hudSnapshot;
    private final Map<ModuleCategory, List<Module>> categorySnapshots = new EnumMap<>(ModuleCategory.class);

    public void register(Module module) {
        modules.add(module);
        moduleSnapshot = null;
        hudSnapshot = null;
        categorySnapshots.clear();
    }

    public List<Module> getModules() {
        List<Module> snapshot = moduleSnapshot;
        if (snapshot == null) {
            snapshot = List.copyOf(modules);
            moduleSnapshot = snapshot;
        }
        return snapshot;
    }

    public List<Module> getByCategory(ModuleCategory category) {
        List<Module> snapshot = categorySnapshots.get(category);
        if (snapshot == null) {
            snapshot = modules.stream().filter(m -> m.getCategory() == category).toList();
            categorySnapshots.put(category, snapshot);
        }
        return snapshot;
    }

    public List<HudModule> getHudModules() {
        List<HudModule> snapshot = hudSnapshot;
        if (snapshot == null) {
            List<HudModule> result = new ArrayList<>();
            for (Module m : modules) {
                if (m instanceof HudModule hud) {
                    result.add(hud);
                }
            }
            snapshot = List.copyOf(result);
            hudSnapshot = snapshot;
        }
        return snapshot;
    }

    public Optional<Module> byName(String name) {
        Optional<Module> exact = modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        if ("Sprint".equalsIgnoreCase(name)) {
            return modules.stream().filter(m -> m.getName().equalsIgnoreCase("ToggleSprint")).findFirst();
        }
        if ("No Weather".equalsIgnoreCase(name)) {
            return modules.stream().filter(m -> m.getName().equalsIgnoreCase("Weather Changer")).findFirst();
        }
        return Optional.empty();
    }

    public <T extends Module> Optional<T> byName(String name, Class<T> type) {
        return byName(name).filter(type::isInstance).map(type::cast);
    }
}
