const fs = require("node:fs/promises");
const path = require("node:path");
const { spawnSync } = require("node:child_process");
const sharp = require("sharp");

const projectRoot = path.resolve(__dirname, "..");
const sourceRoot = path.resolve(process.argv[2] || path.join(projectRoot, "cosmetics"));
const outputRoot = path.resolve(
  process.argv[3] || path.join(projectRoot, "src", "main", "resources", "assets", "octoclient"),
);
const generatedSource = path.join(outputRoot, "textures", "cosmetics", "source");
const MAX_TEXTURE_EDGE = 256;

const folderMap = {
  CAPE: "cloak",
  WINGS: "wing",
  INSIDER_NAMETAG_ICON: "insider_nametag",
  BLC_NAMETAG_ICON: "insider_nametag",
  INSIDER_NAMETAG_BORDER: "insider_nametag",
  PROJECTILE: "particles",
  ARM_BAND: "armband",
};
const prefixMap = {
  CAPE: "cloak",
  WINGS: "wing",
  ARM_BAND: "armband",
};

function relativePosix(root, absolutePath) {
  return path.relative(root, absolutePath).split(path.sep).join("/");
}

async function walk(directory) {
  const entries = await fs.readdir(directory, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const absolutePath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await walk(absolutePath)));
    } else if (entry.isFile() && /\.(avif|obj|md5mesh|md5anim|json|png)$/i.test(entry.name)) {
      files.push(absolutePath);
    }
  }
  return files;
}

function normalizedName(name) {
  return name.replace(/[^A-Za-z0-9]+/g, "_").replace(/^_+|_+$/g, "").toLowerCase();
}

function convertWithWindowsDecoder(files) {
  if (!files.length || process.platform !== "win32") {
    return [];
  }
  const script = [
    "$ErrorActionPreference = 'Stop'",
    "Add-Type -AssemblyName PresentationCore",
    "$items = [Console]::In.ReadToEnd() | ConvertFrom-Json",
    "$results = foreach ($item in $items) {",
    "  try {",
    "    $decoder = [System.Windows.Media.Imaging.BitmapDecoder]::Create(",
    "      [Uri]::new($item.source),",
    "      [System.Windows.Media.Imaging.BitmapCreateOptions]::PreservePixelFormat,",
    "      [System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad)",
    "    $encoder = [System.Windows.Media.Imaging.PngBitmapEncoder]::new()",
    "    $encoder.Frames.Add($decoder.Frames[0])",
    "    $stream = [IO.File]::Open($item.target, [IO.FileMode]::Create, [IO.FileAccess]::Write, [IO.FileShare]::None)",
    "    try { $encoder.Save($stream) } finally { $stream.Dispose() }",
    "    [pscustomobject]@{ target = $item.target; error = $null }",
    "  } catch {",
    "    [pscustomobject]@{ target = $item.target; error = $_.Exception.Message }",
    "  }",
    "}",
    "ConvertTo-Json -InputObject @($results) -Compress",
  ].join("\n");
  const result = spawnSync("powershell.exe", ["-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script], {
    input: JSON.stringify(files.map(({ source, wicTarget }) => ({ source, target: wicTarget }))),
    encoding: "utf8",
    maxBuffer: 8 * 1024 * 1024,
  });
  if (result.error || result.status !== 0) {
    throw new Error(`Windows AVIF conversion failed: ${result.error?.message || result.stderr || result.status}`);
  }
  try {
    return JSON.parse(result.stdout);
  } catch (error) {
    throw new Error(`Could not read Windows AVIF conversion results: ${error.message}`, { cause: error });
  }
}

async function optimizePng(input, output) {
  const metadata = await sharp(input).metadata();
  const image = sharp(input);
  if (Math.max(metadata.width || 0, metadata.height || 0) > MAX_TEXTURE_EDGE) {
    image.resize({
      width: MAX_TEXTURE_EDGE,
      height: MAX_TEXTURE_EDGE,
      fit: "inside",
      withoutEnlargement: true,
    });
  }
  await image.png().toFile(output);
}

async function main() {
  const catalogPath = path.join(sourceRoot, "_Katalog", "cosmetics.json");
  const modelsPath = path.join(sourceRoot, "_Katalog", "models.json");
  const [catalog, models] = await Promise.all([
    fs.readFile(catalogPath, "utf8").then(JSON.parse),
    fs.readFile(modelsPath, "utf8").then(JSON.parse),
  ]);
  const sourceFiles = await walk(sourceRoot);
  const textureFiles = sourceFiles
    .filter((file) => file.toLowerCase().endsWith(".avif"))
    .map((absolutePath) => ({
      absolutePath,
      relativePath: relativePosix(sourceRoot, absolutePath),
    }))
    .sort((a, b) => a.relativePath.length - b.relativePath.length ||
      a.relativePath.localeCompare(b.relativePath));
  const modelFiles = sourceFiles
    .filter((file) => /\.(obj|md5mesh)$/i.test(file))
    .map((absolutePath) => ({
      absolutePath,
      relativePath: relativePosix(sourceRoot, absolutePath),
      extension: path.extname(absolutePath).toLowerCase(),
    }));
  const animationFiles = sourceFiles
    .filter((file) => /\.md5anim$/i.test(file))
    .map((absolutePath) => ({
      absolutePath,
      relativePath: relativePosix(sourceRoot, absolutePath),
    }));
  const bandanaMesh = modelFiles.find(
    (file) => file.relativePath.toLowerCase() === "bandana/models/bandana.obj",
  );

  const texturesByKey = new Map();
  const texturesByFolder = new Map();
  for (const file of textureFiles) {
    const folder = file.relativePath.split("/")[0].toLowerCase();
    const key = `${folder}|${path.basename(file.relativePath).toLowerCase()}`;
    if (!texturesByKey.has(key)) {
      texturesByKey.set(key, file.relativePath);
    }
    if (!texturesByFolder.has(folder)) {
      texturesByFolder.set(folder, []);
    }
    texturesByFolder.get(folder).push(file.relativePath);
  }

  const items = [];
  const textureFallbacks = [];
  let modelsFound = 0;
  for (const [type, cosmetics] of Object.entries(catalog.registeredCosmetics)) {
    const folder = (folderMap[type] || type.toLowerCase()).toLowerCase();
    const prefix = prefixMap[type] || folder;
    const typeModels = models.modelData[type] || {};

    for (const cosmetic of cosmetics) {
      const id = String(cosmetic.cosmeticId);
      const candidates = [];
      if (["INSIDER_NAMETAG_ICON", "BLC_NAMETAG_ICON", "INSIDER_NAMETAG_BORDER", "PROJECTILE"].includes(type)) {
        candidates.push(`${id}.avif`);
      }
      if (prefix) {
        candidates.push(`${prefix}_${id}.avif`);
      }
      if (cosmetic.modelName) {
        candidates.push(`${cosmetic.modelName}.avif`);
      }
      const safeName = normalizedName(cosmetic.name);
      if (safeName) {
        candidates.push(`${safeName}.avif`);
      }
      if (type === "BOW") {
        candidates.push("bow.avif");
      } else if (type === "SWORD") {
        candidates.push("diamond.avif", "gold.avif");
      }

      let texturePath;
      for (const candidate of candidates) {
        texturePath = texturesByKey.get(`${folder}|${candidate.toLowerCase()}`);
        if (texturePath) {
          break;
        }
      }
      if (!texturePath) {
        texturePath = texturesByFolder.get(folder)?.[0];
        if (texturePath) {
          textureFallbacks.push(`${type}:${id}`);
        }
      }
      if (!texturePath) {
        throw new Error(`No texture asset found for ${type}/${id} (${cosmetic.name}).`);
      }

      const modelDefinition = cosmetic.modelName ? typeModels[cosmetic.modelName] : null;
      let modelPaths = [];
      if (cosmetic.modelName) {
        const extension = modelDefinition?.md5format ? ".md5mesh" : ".obj";
        const matchingModels = modelFiles.filter((file) => {
          const parts = file.relativePath.split("/");
          const baseName = path.basename(file.relativePath, file.extension).toLowerCase();
          const modelName = cosmetic.modelName.toLowerCase();
          return parts[0].toLowerCase() === folder && file.extension === extension &&
            (baseName === modelName || baseName.startsWith(`${modelName}_`) || baseName.startsWith(modelName));
        });
        const exactModels = matchingModels.filter((file) =>
          path.basename(file.relativePath, file.extension).toLowerCase() === cosmetic.modelName.toLowerCase());
        modelPaths = (exactModels.length ? exactModels : matchingModels)
          .map((file) => `source/${file.relativePath}`)
          .sort((a, b) => a.localeCompare(b));
        if (modelPaths.length) {
          modelsFound++;
        }
      }

      // Model-less bandanas used to fall back to a flat plane floating in the
      // head. Give them the authored bandana wrap instead.
      if (type === "BANDANA" && modelPaths.length === 0 && bandanaMesh) {
        modelPaths = [`source/${bandanaMesh.relativePath}`];
      }

      let animationPaths = [];
      if (["CAPE", "WINGS"].includes(type) && modelPaths.some((model) => /\.md5mesh$/i.test(model))) {
        const modelDirectory = path.posix.dirname(modelPaths[0]);
        const candidates = animationFiles.filter(
          (file) => path.posix.dirname(`source/${file.relativePath}`) === modelDirectory,
        );
        const preferred = type === "WINGS"
          ? ["flapping.md5anim", "wings.md5anim", "animation.md5anim"]
          : ["dynamic.md5anim", "curved.md5anim", "rect.md5anim", "animation.md5anim"];
        let chosen = null;
        for (const name of preferred) {
          chosen = candidates.find(
            (file) => path.posix.basename(file.relativePath).toLowerCase() === name,
          );
          if (chosen) {
            break;
          }
        }
        chosen = chosen || candidates[0];
        if (chosen) {
          animationPaths = [`source/${chosen.relativePath}`];
        }
      }

      let scale = Number(modelDefinition?.scale) || 0.0625;
      if (["CAPE", "WINGS", "BANDANA"].includes(type)) {
        // The source layout file uses renderer directives (for example
        // -2 for Christmas wings), not world-space scales.
        scale = Number.isFinite(scale) && scale > 0 && scale <= 0.5 ? scale : 0.0625;
      }
      items.push({
        type,
        id: Number(cosmetic.cosmeticId),
        name: cosmetic.name,
        hasModel: Boolean(cosmetic.hasModel),
        texture: `source/${texturePath.replace(/\.avif$/i, ".png")}`,
        modelPaths,
        animationPaths,
        scale,
        flipUVs: Boolean(modelDefinition?.flipUVs),
        translationX: Number(modelDefinition?.translation?.x) || 0,
        translationY: Number(modelDefinition?.translation?.y) || 0,
        translationZ: Number(modelDefinition?.translation?.z) || 0,
      });
    }
  }

  const requiredTexturePaths = new Set(items.map((item) => item.texture));
  const requiredModelPaths = new Set(items.flatMap((item) => item.modelPaths));
  const requiredAnimationPaths = new Set(items.flatMap((item) => item.animationPaths));
  const requiredTextureFiles = textureFiles.filter((file) => requiredTexturePaths.has(
    `source/${file.relativePath.replace(/\.avif$/i, ".png")}`,
  ));
  const requiredModelFiles = modelFiles.filter((file) =>
    requiredModelPaths.has(`source/${file.relativePath}`));
  const requiredAnimationFiles = animationFiles.filter((file) =>
    requiredAnimationPaths.has(`source/${file.relativePath}`));
  if (requiredTextureFiles.length !== requiredTexturePaths.size
      || requiredModelFiles.length !== requiredModelPaths.size
      || requiredAnimationFiles.length !== requiredAnimationPaths.size) {
    throw new Error("The catalog references texture, model or animation files that are missing from the source directory.");
  }

  await fs.rm(generatedSource, { recursive: true, force: true });
  await fs.mkdir(generatedSource, { recursive: true });
  await fs.mkdir(path.join(outputRoot, "cosmetics"), { recursive: true });

  for (const file of [...requiredModelFiles, ...requiredAnimationFiles]) {
    const target = path.join(generatedSource, file.relativePath);
    await fs.mkdir(path.dirname(target), { recursive: true });
    await fs.copyFile(file.absolutePath, target);
  }

  let converted = 0;
  const unsupportedTextures = [];
  let nextTexture = 0;
  const workerCount = Math.min(1, requiredTextureFiles.length);
  await Promise.all(Array.from({ length: workerCount }, async () => {
    while (true) {
      const index = nextTexture++;
      if (index >= requiredTextureFiles.length) {
        return;
      }
      const file = requiredTextureFiles[index];
      const outputRelative = file.relativePath.replace(/\.avif$/i, ".png");
      const target = path.join(generatedSource, outputRelative);
      await fs.mkdir(path.dirname(target), { recursive: true });
      try {
        await optimizePng(file.absolutePath, target);
      } catch (error) {
        const pixels = Buffer.alloc(16 * 16 * 4);
        for (let y = 0; y < 16; y++) {
          for (let x = 0; x < 16; x++) {
            const offset = (y * 16 + x) * 4;
            const isLight = (Math.floor(x / 4) + Math.floor(y / 4)) % 2 === 0;
            pixels[offset] = isLight ? 255 : 32;
            pixels[offset + 1] = 0;
            pixels[offset + 2] = isLight ? 255 : 32;
            pixels[offset + 3] = 255;
          }
        }
        await sharp(pixels, { raw: { width: 16, height: 16, channels: 4 } }).png().toFile(target);
        unsupportedTextures.push({
          source: file.absolutePath,
          target,
          wicTarget: `${target}.wic.png`,
          relativePath: file.relativePath,
          decodeError: error.message,
        });
      }
      converted++;
      if (converted % 250 === 0 || converted === requiredTextureFiles.length) {
        console.log(`Converted ${converted}/${requiredTextureFiles.length} catalog AVIF textures`);
      }
    }
  }));

  const windowsFailures = convertWithWindowsDecoder(unsupportedTextures);
  const windowsFailuresByTarget = new Map(windowsFailures.map((failure) => [failure.target, failure.error]));
  const fallbackTextures = [];
  for (const file of unsupportedTextures) {
    if (process.platform !== "win32" || !windowsFailuresByTarget.has(file.wicTarget)
        || windowsFailuresByTarget.get(file.wicTarget)) {
      fallbackTextures.push({
        ...file,
        error: process.platform !== "win32"
          ? "Windows imaging decoder unavailable"
          : windowsFailuresByTarget.get(file.wicTarget) || "Windows decoder returned no result",
      });
      await fs.rm(file.wicTarget, { force: true });
      continue;
    }
    await optimizePng(file.wicTarget, file.target);
    await fs.rm(file.wicTarget, { force: true });
  }

  const outputCatalog = path.join(outputRoot, "cosmetics", "catalog.json");
  await fs.writeFile(outputCatalog, `${JSON.stringify({ items }, null, 2)}\n`, "utf8");
  console.log(`Generated ${items.length} catalog entries; resolved ${modelsFound} model entries.`);
  if (textureFallbacks.length) {
    console.log(`${textureFallbacks.length} entries use a shared category texture: ${textureFallbacks.slice(0, 12).join(", ")}`);
  }
  if (fallbackTextures.length) {
    console.warn(
      `Generated fallback textures for ${fallbackTextures.length} unsupported AVIF files: ` +
      fallbackTextures.slice(0, 20)
        .map((file) => `${file.relativePath} (${file.error})`)
        .join(", "),
    );
  }
  if (unsupportedTextures.length - fallbackTextures.length > 0) {
    console.log(`Recovered ${unsupportedTextures.length - fallbackTextures.length} AVIF files with the Windows decoder.`);
  }
  console.log(
    `Bundled ${requiredTextureFiles.length} referenced textures, ${requiredModelFiles.length} models and ` +
    `${requiredAnimationFiles.length} animations; ` +
    `skipped ${textureFiles.length - requiredTextureFiles.length} unused AVIF files.`,
  );
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
