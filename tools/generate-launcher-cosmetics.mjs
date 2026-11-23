/**
 * Generates the launcher's cosmetic catalogue from the mod's own sources.
 *
 * The bundled `catalog.json` is the source of truth for catalog cosmetics;
 * the hand-written cosmetic modules (Cloud cape, Fire Aura, Royal Crown, ...)
 * are read from their Java registrations/design arrays. Run this script from
 * the repository root after changing either source:
 *
 *   node tools/generate-launcher-cosmetics.mjs
 */

import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const launcherDir = process.env.LAUNCHER_DIR ?? "E:\\der beste launcher der welt";

const catalogSource = path.join(root, "src/main/resources/assets/octoclient/cosmetics/catalog.json");
const javaRoot = path.join(root, "src/main/java/dev/octoclient");
const moduleManagerFile = path.join(javaRoot, "module/ModuleManager.java");
const target = path.join(launcherDir, "src/data/generated-cosmetics.json");

const read = (file) => fs.readFileSync(file, "utf8");

/** Extracts quoted strings from a `List.of(...)` / array declaration. */
function stringsFromDeclaration(source, declaration, terminator = ");") {
  const start = source.indexOf(declaration);
  if (start < 0) throw new Error(`Missing declaration: ${declaration}`);
  const end = source.indexOf(terminator, start);
  if (end < 0) throw new Error(`Unterminated declaration: ${declaration}`);
  return [...source.slice(start, end).matchAll(/"([^"]*)"/g)].map((match) => match[1]);
}

/** Mirrors the `word.substring(0, 1).toUpperCase() + word.substring(1)` Java helpers. */
function titleFromPath(value) {
  const file = value.slice(value.lastIndexOf("/") + 1).replace(/\.[^.]+$/, "");
  return file
    .split("_")
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function superName(file) {
  const match = read(file).match(/super\("([^"]+)"/);
  if (!match) throw new Error(`No super(...) name in ${file}`);
  return match[1];
}

/** Matches CosmeticCatalog.variantLabel so module names stay byte-for-byte identical. */
function variantLabel(item) {
  const file = path.posix.basename(item.texture ?? "");
  const dot = file.lastIndexOf(".");
  let label = (dot > 0 ? file.slice(0, dot) : file).replaceAll("_", " ").trim();
  const lower = label.toLowerCase();
  if (lower.startsWith("wing ")) label = label.slice(5).trim();
  else if (lower.startsWith("wings ")) label = label.slice(6).trim();
  if (!label || label.toLowerCase() === "wing" || label.toLowerCase() === "wings"
      || label.toLowerCase() === item.name.toLowerCase()) {
    return String(item.id);
  }
  return label;
}

const SLOT_BY_GROUP = {
  CAPE: "capes",
  BANDANA: "bandanas",
  SHIELD: "shields",
  WINGS: "wings",
  HAT: "heads",
  EARS: "heads",
  NOSE: "heads",
  MOUTH: "heads",
  EYES: "heads",
  NECK: "heads",
  MASK: "heads",
  HALO: "heads",
  BACK: "accessories",
  PET: "accessories",
  PLAYER_PET: "accessories",
  AURA: "accessories",
  SPRAY: "accessories",
  PROJECTILE: "accessories",
  BOW: "accessories",
  SWORD: "accessories",
  ARM_BAND: "accessories",
  BELT: "accessories",
  BOOTS: "accessories",
  TAIL: "accessories",
  BLC_NAMETAG_ICON: "accessories",
  BLC_ICONS: "accessories",
  INSIDER_NAMETAG_ICON: "accessories",
  INSIDER_NAMETAG_BORDER: "accessories",
  COLORED_NAME: "accessories",
  NAME_TEXT: "accessories",
  ACCESSORY: "accessories",
};

/** Maps a cosmetic group to one of the launcher's procedural attachment styles. */
function styleFor(group, seed) {
  switch (group) {
    case "CAPE": return "none";
    case "BANDANA": return seed % 5 === 0 ? "wrap" : "band";
    case "SHIELD": return seed % 3 === 0 ? "kite" : "round";
    case "WINGS": return "feather";
    case "EARS":
    case "NOSE":
    case "MOUTH":
    case "EYES":
    case "MASK": return "beanie";
    case "NECK": return "scarf";
    case "HAT":
    case "HALO": return ["tophat", "crown", "beanie", "halo"][seed % 4];
    case "AURA":
    case "PROJECTILE":
    case "SPRAY": return "halo";
    case "PET":
    case "PLAYER_PET":
    case "ACCESSORY":
    case "BACK": return "backpack";
    case "BOW":
    case "SWORD":
    case "ARM_BAND":
    case "BELT":
    case "TAIL":
    case "COLORED_NAME":
    case "NAME_TEXT": return "scarf";
    default: return "backpack";
  }
}

/** Stable, readable colours; the launcher uses them for its 3D preview. */
function colorsFor(value) {
  let hash = 2166136261;
  for (const char of value) {
    hash ^= char.charCodeAt(0);
    hash = Math.imul(hash, 16777619);
  }
  const hue = Math.abs(hash) % 360;
  const toHex = (h, s, l) => {
    const a = s * Math.min(l, 1 - l);
    const f = (n) => {
      const k = (n + h / 30) % 12;
      return Math.round(255 * (l - a * Math.max(-1, Math.min(k - 3, 9 - k, 1))))
        .toString(16).padStart(2, "0");
    };
    return `#${f(0)}${f(8)}${f(4)}`;
  };
  return {
    primary: toHex(hue, 0.72, 0.58),
    secondary: toHex((hue + 42) % 360, 0.65, 0.34),
    accent: toHex((hue + 180) % 360, 0.85, 0.75),
  };
}

function entry({ id, type, name, description, moduleName, group, kind, slot, style }) {
  return {
    id,
    type,
    name,
    description,
    moduleName,
    group,
    kind,
    slot,
    style,
    ...colorsFor(`${id}|${moduleName}`),
  };
}

// --- Bundled Badlion catalog ------------------------------------------------

const catalog = JSON.parse(read(catalogSource));
const items = catalog.items ?? [];

const nameCounts = new Map();
for (const item of items) {
  const key = `${item.type}|${item.name}`;
  nameCounts.set(key, (nameCounts.get(key) ?? 0) + 1);
}
const seen = new Map();

const catalogEntries = items.map((item) => {
  const key = `${item.type}|${item.name}`;
  let displayName = item.name;
  if (nameCounts.get(key) > 1 || item.type === "WINGS") {
    seen.set(key, (seen.get(key) ?? 0) + 1);
    displayName = `${item.name} ${variantLabel(item)}`;
  }
  const group = item.type;
  return entry({
    id: `${item.type}:${item.id}`,
    type: item.type,
    name: displayName,
    description: `${item.type.replaceAll("_", " ")} · #${item.id}`,
    moduleName: `${item.type.replaceAll("_", " ")}: ${displayName} (#${item.id})`,
    group,
    kind: "catalog",
    slot: SLOT_BY_GROUP[group] ?? "accessories",
    style: styleFor(group, item.id),
  });
});

// --- Hand-written cosmetic modules -----------------------------------------

const moduleManagerSource = read(moduleManagerFile);
const cosmeticsSrc = (file) => read(path.join(javaRoot, "module/cosmetics", file));

const builtinModules = [];
function builtin(moduleName, group, type, source) {
  builtinModules.push({ moduleName, group, type, source });
}

for (const [, name] of moduleManagerSource.matchAll(/new Cape\("([^"]+)",\s*"[^"]+"\)/g)) {
  builtin(name, "CAPE", "CUSTOM_CAPE", "Mod-Cape");
}
for (const name of stringsFromDeclaration(
  cosmeticsSrc("BandanaDesigns.java"), "private static final List<String> FILES = List.of(",
)) {
  builtin(titleFromPath(name), "BANDANA", "CUSTOM_BANDANA", "Mod-Bandana");
}
for (const name of stringsFromDeclaration(
  cosmeticsSrc("WingDesigns.java"), "private static final String[] TEXTURES = {", "};",
)) {
  builtin(titleFromPath(name), "WINGS", "CUSTOM_WINGS", "Mod-Flügel");
}
for (const name of stringsFromDeclaration(
  cosmeticsSrc("AccessoryDesigns.java"), "private static final List<String> FOLDERS = List.of(",
)) {
  builtin(titleFromPath(name), "ACCESSORY", "CUSTOM_ACCESSORY", "Mod-Accessoire");
}
builtin(superName(path.join(javaRoot, "module/cosmetics/Hat.java")),
  "HAT", "CUSTOM_HAT", "Mod-Hut");
for (const name of stringsFromDeclaration(
  cosmeticsSrc("HatDesigns.java"), "private static final List<String> MODELS = List.of(",
)) {
  builtin(titleFromPath(name), "HAT", "CUSTOM_HAT", "Mod-Hut");
}
builtin(superName(path.join(javaRoot, "module/cosmetics/ColoredName.java")),
  "COLORED_NAME", "CUSTOM_NAMETAG", "Mod-Nametag");
builtin(superName(path.join(javaRoot, "module/cosmetics/NameText.java")),
  "NAME_TEXT", "CUSTOM_NAMETAG", "Mod-Nametag");
builtin(superName(path.join(javaRoot, "module/cosmetics/Halo.java")),
  "HALO", "CUSTOM_HALO", "Mod-Halo");

const auraSource = cosmeticsSrc("AuraDesigns.java");
const auraOptions = [
  ...stringsFromDeclaration(auraSource, "private static final List<String> LEGACY_SHAPES = List.of("),
  ...[...auraSource.matchAll(/new Design\("([^"]+)"/g)].map((match) => match[1]),
];
for (const name of auraOptions) {
  builtin(name, "AURA", "CUSTOM_AURA", "Mod-Aura");
}

const builtinIds = new Set();
const builtinEntries = builtinModules.map((item, index) => {
  const id = `BUILTIN:${item.group}:${item.moduleName}`;
  if (builtinIds.has(id)) {
    throw new Error(`Duplicate built-in cosmetic: ${id}`);
  }
  builtinIds.add(id);
  return entry({
    id,
    type: item.type,
    name: item.moduleName,
    description: `${item.source} · ${item.group.replaceAll("_", " ")}`,
    moduleName: item.moduleName,
    group: item.group,
    kind: "builtin",
    slot: SLOT_BY_GROUP[item.group] ?? "accessories",
    style: styleFor(item.group, index),
  });
});

const entries = [...catalogEntries, ...builtinEntries].sort((a, b) =>
  a.type.localeCompare(b.type) || a.name.localeCompare(b.name) || a.id.localeCompare(b.id));

const types = [...new Set(entries.map((item) => item.type))].sort().map((type) => ({
  type,
  label: type.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase()),
  slot: entries.find((item) => item.type === type)?.slot ?? "accessories",
}));

fs.mkdirSync(path.dirname(target), { recursive: true });
fs.writeFileSync(
  target,
  `${JSON.stringify({
    source: "mod catalog.json + cosmetic module registrations",
    count: entries.length,
    catalogCount: catalogEntries.length,
    builtinCount: builtinEntries.length,
    types,
    items: entries,
  }, null, 2)}\n`,
);
console.log(
  `Wrote ${entries.length} cosmetics (${catalogEntries.length} catalog, `
  + `${builtinEntries.length} built-in) to ${target}`,
);
