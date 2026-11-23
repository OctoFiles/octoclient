# OctoClient

An initial Forge 1.8.9 port is maintained separately in `forge-1.8.9`.

Dark / glassmorphism utility client for **Minecraft 26.2** (Fabric, Java 25).

## Features

- **Dashboard (Right Shift)** — full-screen glass overlay with:
  - category tabs (HUD / Render / Utility / Settings) and live search
  - animated module cards (hover glow, animated toggle switches, and per-module settings)
  - sliders, toggle switches, dropdowns, editable text, key capture, and RGB(A) color pickers;
    module settings can use the default inline accordion or an optional popup; inherited
    background and panel-corner controls are omitted from module menus
- **HUD Editor (Insert)** — drag & drop HUD elements with screen and module-to-module
  edge/center snapping guides, so widgets can be aligned or docked together,
  dashed selection frames, scroll over an element or drag one of its corner
  handles to resize it, and ESC saves positions and HUD settings
- **HUD appearance** — each HUD widget has an adjustable scale and can be
  repositioned or resized in the editor; shared GUI-background and panel-frame
  controls are omitted from module settings
- **HUD modules** (all rendered with real item textures via the vanilla GUI pipeline):
  - Watermark, Stats (FPS / CPS / Ping), Keystrokes (animated WASD/space/LMB/RMB),
    Armor Status (durability %), Target HUD (animated HP bar, distance, held item),
    Arraylist (slide-in animation)
- **Utility modules** — Free Look (hold Y or toggle with M), Chat Suffix,
  Auto GG, Panic (Delete disables everything)
- **First gameplay modules** — latency with configurable text format, color mode,
  dynamic/fixed dimensions, HUD visibility and tab-list display; a crosshair editor
  with presets, undo/copy/paste, stroke controls and outline; ToggleSprint with
  status HUD and configurable flight-speed boost; and Drop Stack with keybind and
  inventory-use controls
- **Additional HUD/render modules** — Potion Status with compact/card layouts,
  effect sorting, visibility and sizing controls; Scoreboard number, text-shadow,
  corner and sizing controls; title placement, scale, animation and editor preview;
  Action Bar positioning, HUD scale, dynamic/fixed dimensions and text color; and
  individually configurable legacy animation controls
- **Visual and input modules** — custom hit tint for recently damaged entities,
  category-specific world-space hitboxes and facing vectors, chat/inventory/
  scoreboard/nametag/tab-list backdrop toggles, client-side weather and time,
  addable and persistent Autotext entries with individual keybinds, and an active
  resource-pack display with optional icon and description. Keystrokes and
  Mousetracker expose their layout, color, trail, size and CPS controls; screenshot
  capture uses a configurable subfolder, and the enchantment glint has a color picker.
  Block Outlines supports outline/filled overlays, RGB and alpha controls; the
  searchable Particles selector now includes All On/All Off actions
- **Additional render modules** — first-person item model size/position/rotation
  controls, an independently offset shield, lower and more transparent fire,
  optional sky/cloud hiding, and an advancement-toast toggle
- **TNT Timer HUD** — lists nearby primed TNT fuse countdowns with configurable
  distance and entry limits; position and size are adjustable in the HUD editor
- **Server and survival helpers** — three configurable server quick-connect slots,
  the existing delayed Auto Reconnect, a configurable Server Info HUD with server
  icon support, and a saturation HUD with optional exhaustion and food-value data.
  Quick Connect uses
  F6/F7/F8 by default and is available from menus.
- **Inventory and player HUDs** — Shulker Preview shows the 27-slot contents when
  hovering a shulker in an inventory, with an optional preview key; 3D Skin
  displays your current skin as a draggable HUD widget with overlay, voxel,
  torso, head, and skull-model controls. Tiers reads rank/tier prefixes from
  scoreboard teams, so the server must provide those prefixes for ranked labels
  to appear. Waypoints can be created by key or button, persisted per dimension,
  selected as a HUD target, and managed from the waypoint list; deathpoints and
  reached-deathpoint cleanup are configurable.
- **Loading Tips** — rotates gameplay tips while resource packs and assets load,
  with random/cycle order and an adjustable interval.
- **Health Indicator / NoHurtCam** — add configurable numeric or bar health
  labels above nearby living entities, and independently disable damage camera
  shake. Health labels can be filtered to players or other mobs.
- **Bad Optimization** — experimental switches for hiding clouds/weather and
  entity shadows, a configurable per-tick cap for non-important particles, and
  opt-in shared player-skin and nametag-icon atlases plus redundant pipeline
  bind skipping within a render pass. The icon atlas adds small player and
  health icons to configured nametags.
- **Resourcepack Organizer** — stores three named pack-ID presets and applies
  them with rebindable keys. These are virtual in-game groups (not disk folders),
  so pack files remain where Minecraft expects them; incompatible-pack handling,
  selector backdrop transparency and pack previews are configurable.
- **Creator Code Dashboard** — stores three creator codes and local view/click/
  redemption counts, with CTR and conversion-rate display. Counts can be edited
  in settings or incremented with keybinds; there is no platform analytics
  connection or automatic third-party tracking.
- **Wavey Capes and Cosmetics** — individually selectable capes use a direct
  port of tr7zw's WaveyCapes simulation and expose the same settings with the
  same names, values, ranges and defaults: `Cape Style` (`Blocky`/`Smooth`,
  default `Smooth`), `Wind Mode` (`No Wind`/`Waves`, default `No Wind`),
  `Cape Movement` (`Classic`/`Simulation`/`Simulation V2`/`Dungeons`, default
  `Simulation V2`), `Gravity` (5–32, default 25), `Height Multiplier` (4–16,
  default 6) and `Compute Gravity Vector` (off). There are no extra cape modes
  or swing controls. Every visible wearer's cape is simulated locally with the
  wearer's own settings, including players found through the HTTP relay. Catalog
  capes use the same simulation and WaveyCapes defaults, so a catalog cape no
  longer renders as a rigid sheet. Catalog capes are flat, two-sided cloth anchored
  at the back surface, so they lie against the back without thickness-corner
  artifacts. Catalog bandanas sway their hanging knot and tails while the band
  stays on the head. The existing cosmetics tab also
  provides bundled wings, bandanas, hats, name tags and selectable aura designs.
  The Forge 1.8.9 target has no cape renderer, so these cape animation settings
  apply to the 26.2 Fabric client only.
- **Emotes, Friends and Wheel** — select a local Dance/Wave/Cheer pose, maintain
  a persistent friend list with current-world online status, and right-click a
  player name in chat for Add/Remove Friend, Invite Text, Emote and Copy Name
  actions. Emote poses are client-side; copied invitations are not sent
  automatically and no friends/social server is assumed.
- **McReal gallery** — opens a local browser for screenshots saved under the
  configured screenshots folder and launches a selected image in the system
  viewer. It does not upload content or provide an online feed.
- **Item Counter / CPS / Clock** — Item Counter counts a configurable item
  selected by registry ID or name across inventory and optionally equipment;
  the existing CPS and Clock HUDs include layout, display and time-format
  settings.
- **Item Highlighter / Shiny Pots / Musa's Armor HUD** — Search inventories by
  item name or registry ID and outline matches with a configurable color; add
  glint to healing potions (with separate splash/lingering toggles); and use an
  alternative armor/held-item HUD with visibility, sticky slots, layout,
  durability style, and color settings. Combo Counter includes configurable
  text, color, panel size, and active time. Speed and Reach are also available
  as configurable HUD modules.
- **Uptime / Day Counter / TPS / Jump Reset** — Show game-process uptime and
  world day/time; estimate server TPS from authoritative time-sync packets; and
  show a configurable manual jump-timing cue after damage. Coordinates, Zoom,
  and Fullbright are also configurable modules. Jump Reset never presses jump
  automatically.
- **Free Look / Nametags / FOV Changer / Color Saturation / NoFog** — Free Look
  separates camera look from player aim in third person with a configurable
  hold/toggle key and an R reset key;
  Nametags offers range, color, health and distance options; FOV Changer sets a
  configurable base FOV; Color Saturation adjusts hue, saturation, brightness,
  and contrast across the full screen;
  and NoFog sets a configurable fog fade distance with optional fluid-fog removal.
- **Particle Selector / Chat Animations / Shield Status** — The Particles module
  has a searchable list of individual particle types with persistent per-type
  toggles and bulk on/off controls. Chat Animations is a native eased fade-in implementation (the
  supplied ChatAnimationshield repository URL was unavailable) with a
  configurable duration. Shield Status shows the local shield cooldown and
  applies its configurable ready/disabled color transition to the first-person
  shield base. Side Shield is enabled by default and supports a
  configurable side offset, side angle and reduced blocking size; these are
  native model transforms rather than bundled resource-pack files.
- **Settings and editing** — Color settings include a quick palette as well as
  the existing RGBA sliders. Modules expose an optional per-module Toggle Key;
  the existing gear button continues to open inline or popup settings according
  to Client Settings. Custom Crosshair has a button-driven live designer, and
  Titles can override title and subtitle text.
- **Streamer Mode** — independently hides the server and HUD coordinates, hides
  or replaces your local name in the nametag and player list, and hides or
  randomizes the position shown by F3. It also exposes NoTextureRotations'
  rotation, offset, and mode controls.
- **Spotify Overlay** — displays the current track, artist, album and progress,
  album artwork, time labels and playback hotkeys, with configurable card layout,
  dimensions, colors and artwork resolution. An optional Track/Lyrics navigation
  page shows the current synced Sky Lyrics line; its page-switch key is configurable.
  If Windows does not provide the playing track's artwork, OctoClient looks it up
  using the iTunes Search and Deezer APIs.
  Playback time uses a monotonic clock and unknown track lengths are shown explicitly.
  Windows media sessions work without a token; the optional Spotify Web API
  integration needs a user access token with playback permissions, stored unencrypted in
  `config/octoclient.json`, so remove it from settings before sharing that file.
  Other listed music services do not yet have playback providers; selecting them
  without Spotify displays an explicit unsupported-source status.
- **Sky Lyrics** — integrated from the MIT-licensed Sky Lyrics project. Enable the
  module in the dashboard's Settings tab to show synced lyrics in-world; all
  appearance, timing and source options are available in the regular module
  settings, alongside song editing, recording and AI actions. Optional AI
  components install only when you press the explicit Install AI Lyrics action.
- **Bundled performance mods** — Sodium, Sodium Extra, Lithium, FerriteCore,
  Krypton, ImmediatelyFast and Dynamic FPS are included for Minecraft 26.2 and
  load automatically with OctoClient. Starlight is not bundled because it has no
  compatible 26.2 release.
- **Motion Blur** — bundles Natural Motion Blur 1.4.5 for Minecraft 26.2 with
  controls for its algorithm, profile, strength, and refresh-rate scaling.
  Upstream notes a conflict with VulkanMod.
- **Cosmetics** — seven individually toggleable capes in the Capes tab, including
  animated GIF capes with card previews (Cloud, Galaxy, Shadow, Aldi, Lightning,
  Orbit and Rainbow), plus 13 individually toggleable animated bandanas in the
  Bandanas tab. The Wings tab has 57 individually selectable textured wing designs
  and variants; enabling a wing automatically disables the previous design, just
  like capes. Feather and Bee styles are also available as separate choices, and
  textured wings use their bundled per-design bone animations. Hats, name tags and
  aura effects are also included. The Hats tab includes 90 selectable textured
  hat designs and variants. The Accessories tab includes 28 individually
  selectable models with their bundled textures and animations. The searchable
  catalog also exposes all 3,397 entries from `cosmetics`, including OBJ and MD5
  meshes; MD5 wing and cape clips interpolate between neighbouring frames.
  Bundled textures are capped at 256 pixels per edge; an unreadable source AVIF
  gets a visible fallback texture. Bandanas, wings and accessories
  are client-side only. Cape and accessory choices are shared with other
  OctoClient players through the server relay when the server also has
  OctoClient, and otherwise through the public HTTP endpoint configured in
  `config/octoclient_api.json`. That endpoint accepts the original ten-field
  loadout; cape movement settings ride after `~` in its cape field. MiniMe
  accessories let you enter a Minecraft username to use that player's skin.
- **Config** — `config/octoclient.json` (module states + settings),
  `config/client_hud.json` (HUD positions), saved on screen close

### UI styles

Every module card, HUD box and overlay is drawn in one of four styles. The
global default lives in **Settings → Module Style**; each module can override it
with its own **Card Style** (`Default` follows the global choice):

| Style | Look |
| --- | --- |
| `Glass` | Smoked liquid-glass sheet with a real backdrop refraction and a 1px rim |
| `Clear` | Outline only, no fill, so the scene stays fully visible |
| `Dark Frosted Glass` | Matte, semi-transparent graphite pane over a heavy backdrop blur, rounded corners and one hairline light rim |
| `Solid` | Opaque dark panel with a hairline rim |

`Dark Frosted Glass` (also called Dark Glassmorphism) is deliberately calmer
than `Glass`: it blurs harder, keeps the body dark and milky instead of glossy,
and has no specular sheen or refraction rings.

The Forge 1.8.9 target exposes the same four values in Module Style / Card Style
and applies them to its dashboard cards and HUD chips. Its legacy GUI has no
frame-copy shader pipeline, so `Dark Frosted Glass` there is the layered matte
graphite pane with the hairline rim (`GlassRenderer.frostedPanel`) — it keeps
the look but cannot blur the scene behind it.

The style is reusable for new surfaces:

- `Draw.frostedGlass(...)` — queues the real frosted sheet and falls back to
  `Draw.frostedPanel(...)` when the render pipeline is unavailable.
- `dev.octoclient.render.LiquidGlass.drawFrosted(...)` — the renderer entry
  point behind it (`assets/octoclient/shaders/core/liquid_glass_frosted.fsh`).
- `Theme.FROST_BODY_TOP` / `FROST_BODY_BOTTOM` / `FROST_TINT` / `FROST_RIM` —
  palette, and `ClientSettings.STYLE_FROSTED` — the stored style value.

### Dashboard shape and background

The **Settings → Client Settings** page also exposes four dashboard controls:

| Setting | Default | Effect |
| --- | --- | --- |
| `Dashboard Style` | `Glass` | `Glass`, `Clear`, `Dark Frosted Glass` or `Solid` sheet behind the dashboard |
| `Background Color` | `#9AA1BC` | Tints the glass/frosted sheet behind the module list, sidebar and inspector |
| `Module Corners` | `Rounded` | `Square` makes every module card and HUD panel square |
| `Client Corners` | `Rounded` | `Square` makes the dashboard window and its side panels square |

`Dashboard Background` still hides the sheet entirely. `Dark Frosted Glass`
uses the real frosted shader on Fabric; Forge 1.8.9 has no frame-copy shader, so
it falls back to the matte graphite pane with a hairline rim, just like the card
style. The shape modes override the effective radius only, so a module's saved
`Corner Radius` (default `10`) is restored when you switch back to `Rounded`.
On Fabric the background color is passed to the glass shader; on Forge 1.8.9 it
is mixed into the matte pane.

Long settings lists — including **Client Settings** and a module's inspector —
scroll with the mouse wheel, by dragging the scrollbar at the panel's right
edge, or with `Page Up` / `Page Down`. The draggable scrollbar makes every
appearance control reachable even without a wheel.

## Build & run

Requires **JDK 25**.

```powershell
./gradlew build        # produces build/libs/octoclient-1.0.0.jar
./gradlew runClient    # launches a dev client with the mod installed
```

On Windows: `.\gradlew.bat build`. If `java` is not on PATH, point `JAVA_HOME` at a JDK 25
before running.

Install into a real client by dropping `octoclient-1.0.0.jar` plus
[Fabric API](https://modrinth.com/mod/fabric-api) (built for 26.2) into `mods/`;
use Fabric Loader 0.20.1 or later.
Sky Lyrics and the supported client-side performance mods, along with Natural
Motion Blur, NoTextureRotations, and YACL, are bundled in the OctoClient jar; do
not install duplicate copies separately.
For the highest-fidelity shared capes, install the same OctoClient jar and
compatible Fabric API on the server as well. A server-side install is optional:
without it, the client shares loadouts through the public HTTP endpoint in
`config/octoclient_api.json`.

### Launcher cosmetic sync

The Frost Client launcher at `E:\der beste launcher der welt` offers the same
cosmetics as the game: all 3,397 bundled catalog entries plus the 201
hand-written cosmetic modules (Cloud/Galaxy capes, Fire Aura, Royal Crown,
Halo, nametag styles, …) — 3,598 items total. Its old placeholder cosmetics
(Aurora Cape, Frost-Rucksack, Night-Beanie, …) were removed. Both sides share
two files in the instance's `config/` directory:

- `octoclient.json` — the launcher reads enabled cosmetic modules from the
  game's config, so cosmetics equipped in Minecraft appear in the launcher.
- `octoclient_launcher_loadout.json` — equipping a cosmetic in the launcher
  writes `TYPE:id` keys for catalog items and `BUILTIN:<group>:<module name>`
  for hand-written modules; the mod applies them on the next start and clears
  cosmetics that are no longer selected.

Only cosmetics that really exist in the mod can be selected. One item per
mutual-exclusion group (cape, bandana, wings, hat, aura, …) can be equipped,
matching the game's own rules. The launcher's searchable Cosmetics window also
has an "Aus Spiel laden" button for a manual refresh. To regenerate the launcher
catalog after changing the mod catalog or cosmetic modules:

```powershell
node tools/generate-launcher-cosmetics.mjs
```

### Regenerating the cosmetic catalog

The generated catalog, referenced models and converted textures are bundled
under `src/main/resources/assets/octoclient`. Textures are downscaled to a
maximum edge of 256 pixels to keep the bundle manageable. To rebuild them from
the `cosmetics` directory, run `npm ci` and `npm run generate:cosmetics` from
`tools`. The generator uses `sharp` for AVIF conversion and the Windows imaging
decoder for files `sharp` cannot read.

## Bundled third-party mods

These upstream mods are bundled with OctoClient. License files are retained in
the nested jars or, for the integrated Sky Lyrics sources and its fonts, included
in the OctoClient jar under `licenses/sky-lyrics`.

| Mod | Version | Source |
| --- | --- | --- |
| Natural Motion Blur | 1.4.5+mc26.2 | [ItsPasi/natural-motionblur-fabric](https://github.com/ItsPasi/natural-motionblur-fabric) |
| NoTextureRotations | 1.0.5+26.x | [rfresh2/NoTextureRotations](https://github.com/rfresh2/NoTextureRotations) |
| YetAnotherConfigLib (YACL) | 3.9.7+26.2-fabric | [isXander/YetAnotherConfigLib](https://github.com/isXander/YetAnotherConfigLib) |
| Sky Lyrics (integrated sources) | 1.8.1 | [tigerbunplayz-star/sky-lyrics](https://github.com/tigerbunplayz-star/sky-lyrics) |
| Sodium | 0.9.2 | [CaffeineMC/sodium-fabric](https://github.com/CaffeineMC/sodium-fabric) |
| Sodium Extra | 0.9.5 | [FlashyReese/sodium-extra-fabric](https://github.com/FlashyReese/sodium-extra-fabric) |
| Lithium | 0.25.3 | [CaffeineMC/lithium-fabric](https://github.com/CaffeineMC/lithium-fabric) |
| FerriteCore | 9.0.0 | [malte0811/FerriteCore](https://github.com/malte0811/FerriteCore) |
| Krypton | 0.3.1 | [astei/krypton](https://github.com/astei/krypton) |
| ImmediatelyFast | 1.16.5 | [RaphiMC/ImmediatelyFast](https://github.com/RaphiMC/ImmediatelyFast) |
| Dynamic FPS | 3.11.9 | [juliand665/Dynamic-FPS](https://github.com/juliand665/Dynamic-FPS) |

## Default keybinds

| Key | Action |
| --- | --- |
| Right Shift | Open / close the dashboard |
| Insert | Open the HUD editor |
| Y | Hold Free Look |
| M | Toggle Free Look |
| R | Reset the Free Look view |
| Delete | Panic — disable all modules |
| F12 | Capture a screenshot (Screenshot module) |
| F9 / F10 / F11 | Spotify previous / play-pause / next (Spotify Overlay) |
| F6 / F7 / F8 | Server Quick Connect slots 1–3 |
| K | Toggle Sky Lyrics |
| H | Start the selected local emote (Emotes module) |
| M | Open the local screenshot gallery (McReal module) |

All keybinds are rebindable in the vanilla Controls screen under the *Miscellaneous* category.

## Project layout

```
src/main/java/dev/octoclient/
├── OctoClient.java          entrypoint: keybinds, HUD element, tick handler
├── config/ConfigManager.java   Gson persistence (octoclient.json + client_hud.json)
├── module/
│   ├── Module.java, ModuleCategory.java, ModuleManager.java
│   ├── setting/             Boolean, Number, Mode, Text, Color settings
│   ├── hud/                 HudModule base + 6 HUD modules + HudDrawContext
│   ├── render/              Fullbright, MotionBlur, NameProtect, ToggleAnimations, GlowClickGui
│   ├── utility/             Sprint, FreeLook, ChatSuffix, AutoGG, Panic
│   └── settings/            ClientSettings (accent color, animation speed, blur…)
├── ui/                      DashboardScreen, EditHudScreen, Theme, ValueAnim
└── util/                    Anim, Draw (rounded rects), Click/FpsCounter, TargetTracker
```

Built against Minecraft **26.2** (unobfuscated Mojang mappings), Fabric Loader 0.20.1,
Loom 1.19.3, Fabric API 0.161.0+26.2.

## Multi-version targets

`versions/` holds one standalone Gradle target per Minecraft release (own `gradle.properties`,
`fabric.mod.json` and pinned bundled-mod versions, shared client sources). Only the 26.2 root
target builds today; `versions/README.md` records the measured state of 26.1, 26.1.1, 26.1.2
and 26.3 and what each one still needs.
