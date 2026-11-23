/* OctoClient dashboard — web preview.
 *
 * Every number below is the game's virtual design pixel. The UI pass mirrors
 * DashboardScreen/Draw one call at a time, and the glass pass runs the exact
 * fragment shader from liquid_glass.fsh, so what is drawn here can be ported
 * back to Java without reinterpretation.
 */

// ---------------------------------------------------------------- constants
const VW = 996, VH = 560;
const WIN = { x: 118, y: 18, w: 760, h: 524 };
const HEADER_H = 52, FOOTER_H = 26, SIDEBAR_W = 170, SEARCH_W = 150;
const CARD_H = 64, GAP = 10, CARD_R = 12, CARD_LIFT = 2.5;
const CARD_PAD = 12;

const CONTENT = { x: WIN.x + SIDEBAR_W, y: WIN.y + HEADER_H, w: WIN.w - SIDEBAR_W, h: WIN.h - HEADER_H - FOOTER_H };
const CARD_W = Math.floor((CONTENT.w - 28 - GAP) / 2);
const AX = CONTENT.x + 14, AY = CONTENT.y + 12;
const AW = CONTENT.w - 28, AH = CONTENT.h - 12;

const SEARCH = { x: WIN.x + WIN.w - SEARCH_W - 58, y: WIN.y + 13, w: SEARCH_W, h: 22 };
const CLOSE = { x: WIN.x + WIN.w - 44, y: WIN.y + 13, w: 24, h: 22 };
const SIDEBAR = { x: WIN.x + 1, y: WIN.y + HEADER_H + 1, w: SIDEBAR_W - 1, h: WIN.h - HEADER_H - FOOTER_H - 2 };
const STATUS = { x: SIDEBAR.x + 7, y: SIDEBAR.y + SIDEBAR.h - 46 - 10, w: SIDEBAR_W - 26, h: 46 };
const FOOTER_Y = WIN.y + WIN.h - FOOTER_H + 8;

const C = {
  primary: '#f8f8f8', secondary: '#d6d6d6', muted: '#b4b4b4', faint: '#8a8a8a',
  accent: '#4f8bf7', emerald: '#10b981',
  dark500: '#525256', dark600: '#3f3f44', dark700: '#2b2b2f', dark800: '#1c1c1f',
  dark900: '#0f0f11', dark950: '#050506',
};
const ACCENT = [0x4f, 0x8b, 0xf7];
const WHITE = [255, 255, 255];

const ICON = {
  gauge: '\uf625', eye: '\uf06e', wrench: '\uf7d9', sliders: '\uf1de',
  search: '\uf002', shield: '\uf3ed', check: '\uf058', xmark: '\uf00d', gear: '\uf013',
  chevron: '\uf078', reset: '\uf2ea',
};

// ---------------------------------------------------------------- settings
const S = {
  b: (name, v) => ({ t: 'bool', name, v }),
  n: (name, v, min, max, step, unit) => ({ t: 'num', name, v, min, max, step, unit }),
  m: (name, v, options) => ({ t: 'mode', name, v, options }),
  c: (name, v) => ({ t: 'color', name, v, open: false }),
};

// ---------------------------------------------------------------- module data
const MODULES = [
  { n: 'Watermark', d: 'Client branding with FPS / ping', i: 'diamond', cat: 'HUD', on: true,
    s: [S.b('Show Stats', true)] },
  { n: 'Stats', d: 'FPS, CPS and ping counter', i: 'emerald', cat: 'HUD', on: true,
    s: [S.m('Mode', 'Compact', ['Compact', 'Detailed']), S.b('FPS', true), S.b('CPS', true), S.b('Ping', true)] },
  { n: 'Keystrokes', d: 'WASD and mouse buttons with CPS', i: 'redstone', cat: 'HUD', on: true,
    s: [S.n('Scale', 1, 0.5, 2, 0.05), S.b('Mouse Buttons', true), S.b('Space Bar', true)] },
  { n: 'Armor Status', d: 'Armor durability at a glance', i: 'diamond_chestplate', cat: 'HUD', on: true,
    s: [S.n('Scale', 1, 0.5, 2, 0.05), S.m('Orientation', 'Horizontal', ['Horizontal', 'Vertical']),
        S.b('Percent', true), S.b('Offhand', false)] },
  { n: 'Target HUD', d: 'Info about your last attack target', i: 'diamond_sword', cat: 'HUD', on: true,
    s: [S.n('Scale', 1, 0.5, 2, 0.05), S.b('Distance', true), S.b('Held Item', true)] },
  { n: 'Arraylist', d: 'Enabled modules list', i: 'paper', cat: 'HUD', on: false,
    s: [S.b('Lowercase', false)] },
  { n: 'Direction Compass', d: 'Compass strip with facing direction', i: 'compass', cat: 'HUD', on: false,
    s: [S.b('Degrees', true)] },
  { n: 'Potion Status', d: 'Active effects with timers', i: 'potion', cat: 'HUD', on: true,
    s: [S.b('Sort by Time', true), S.b('Show Icons', true)] },
  { n: 'Coordinates', d: 'XYZ, biome and sprint state', i: 'map', cat: 'HUD', on: true,
    s: [S.b('Biome', true), S.b('Sprint State', false)] },
  { n: 'Clock', d: 'Real or in-game time', i: 'clock', cat: 'HUD', on: true,
    s: [S.m('Source', 'In-Game', ['Real', 'In-Game']), S.m('Format', '24h', ['24h', '12h'])] },
  { n: 'Memory', d: 'JVM heap usage', i: 'bookshelf', cat: 'HUD', on: true,
    s: [S.b('Bar', true), S.b('Show in MB', false)] },
  { n: 'Speed', d: 'Movement in blocks per second', i: 'sugar', cat: 'HUD', on: true,
    s: [S.b('Vertical', false)] },
  { n: 'CPS', d: 'Clicks per second counter', i: 'feather', cat: 'HUD', on: false,
    s: [S.n('Scale', 1, 0.5, 2, 0.05), S.b('Show Both', true)] },
  { n: 'Ping', d: 'Show your ping to server', i: 'ender_pearl', cat: 'HUD', on: false,
    s: [S.n('Scale', 1, 0.5, 2, 0.05)] },
  { n: 'Server Info', d: 'Show server name and IP', i: 'oak_sign', cat: 'HUD', on: false,
    s: [S.n('Scale', 1, 0.5, 2, 0.05)] },
  { n: 'Direction', d: 'Show player facing direction', i: 'compass', cat: 'HUD', on: false,
    s: [S.n('Scale', 1, 0.5, 2, 0.05)] },
  { n: 'FPS', d: 'Frames per second', i: 'clock', cat: 'HUD', on: false,
    s: [S.b('Label', true), S.b('Colour by Value', true)] },
  { n: 'Saturation', d: 'Hunger and saturation', i: 'cooked_beef', cat: 'HUD', on: false,
    s: [S.b('Label', true)] },
  { n: 'Session', d: 'How long you have been playing', i: 'clock', cat: 'HUD', on: false, s: [] },

  { n: 'Fullbright', d: 'See in the dark', i: 'glowstone_dust', cat: 'Render', on: false,
    s: [S.n('Brightness', 0.7, 0, 1, 0.05)] },
  { n: 'Name Protect', d: 'Hides your name on the HUD', i: 'name_tag', cat: 'Render', on: false,
    s: [S.m('Alias', 'Hidden', ['Hidden', 'Anonymous'])] },
  { n: 'Toggle Animations', d: 'Animated module list transitions', i: 'clock', cat: 'Render', on: false,
    s: [S.m('Style', 'Slide', ['Slide', 'Fade']), S.n('Speed', 1, 0.1, 3, 0.1), S.b('Shadows', true)] },
  { n: 'Zoom', d: 'Hold a key to zoom in', i: 'spyglass', cat: 'Render', on: false,
    s: [S.n('Amount', 4, 2, 10, 0.5), S.n('Speed', 1, 0.2, 3, 0.1), S.b('Smooth', true)] },
  { n: 'No Weather', d: 'Clears rain and thunder', i: 'water_bucket', cat: 'Render', on: false,
    s: [S.b('No Rain', true), S.b('No Thunder', true)] },
  { n: 'No Bobbing', d: 'Stops the camera bob while walking', i: 'leather_boots', cat: 'Render', on: false, s: [] },
  { n: 'No FOV Effects', d: 'Removes sprint and damage FOV kicks', i: 'sugar', cat: 'Render', on: false, s: [] },

  { n: 'Sprint', d: 'Auto-sprint while walking forward', i: 'leather_boots', cat: 'Utility', on: false, s: [] },
  { n: 'Free Look', d: 'Perspective toggle on a hotkey', i: 'bow', cat: 'Utility', on: false,
    s: [S.m('Mode', 'Hold', ['Hold', 'Toggle'])] },
  { n: 'Chat Suffix', d: 'Adds a tag to your chat messages', i: 'oak_sign', cat: 'Utility', on: false,
    s: [S.m('Suffix', 'OctoClient', ['None', 'OctoClient', 'Custom'])] },
  { n: 'Auto GG', d: 'Says gg when a game ends', i: 'golden_apple', cat: 'Utility', on: false,
    s: [S.b('Auto Send', true)] },
  { n: 'Auto Respawn', d: 'Respawns without clicking', i: 'totem_of_undying', cat: 'Utility', on: false,
    s: [S.n('Delay', 1, 0, 5, 0.5, 's'), S.b('Close Screen', true)] },
  { n: 'Panic', d: 'Disables all modules (DELETE key)', i: 'arrow', cat: 'Utility', on: false, s: [] },

  { n: 'Client Settings', d: 'Dashboard appearance and behavior', i: 'amethyst_shard', cat: 'Settings', on: false,
    s: [S.c('Accent Color', '#4f8bf7'), S.n('Animation Speed', 1, 0.2, 3, 0.1), S.b('Tooltips', true)] },
];

const CATEGORIES = [
  { name: 'HUD', icon: ICON.gauge },
  { name: 'Render', icon: ICON.eye },
  { name: 'Utility', icon: ICON.wrench },
  { name: 'Settings', icon: ICON.sliders },
];

// ------------------------------------------------------------------ state
const state = {
  cat: 'HUD',
  search: '',
  focused: false,
  scroll: 0,
  scrollTarget: 0,
  mx: -1,
  my: -1,
  inside: false,
  time: 0,
  drag: null,
  intro: 0,      // window scale / fade in
  catAnim: 0,    // 0..1 reveal of the current card list
  focusA: 0,     // animated search focus
  indY: null,    // sliding sidebar pill
  lastCat: 'HUD',
};

MODULES.forEach((m) => {
  m.hover = 0;
  m.onA = m.on ? 1 : 0;
  m.vel = 0;
  m.glow = m.on ? 1 : 0;
  m.expand = 0;
  m.open = false;
  m.spin = 0;
  m.press = 0;
  m.flash = 0;
  m.s.forEach((s) => {
    if (s.t === 'bool') { s.a = s.v ? 1 : 0; s.va = 0; }
    if (s.t === 'mode') s.pw = null;
  });
});

CATEGORIES.forEach((c) => { c.a = c.name === 'HUD' ? 1 : 0; });

// ------------------------------------------------------------------ canvases
const glassCanvas = document.getElementById('glass');
const uiCanvas = document.getElementById('ui');
const gl = glassCanvas.getContext('webgl2', { alpha: false, antialias: true });
const ctx = uiCanvas.getContext('2d');

let ui = 1, ox = 0, oy = 0, dpr = 1;

const items = {};
function loadItems() {
  const names = [...new Set(MODULES.map((m) => m.i))];
  names.forEach((n) => {
    const img = new Image();
    img.src = `items/${n}.png`;
    items[n] = img;
  });
}

// ------------------------------------------------------------------- helpers
const MUTED_RGB = [0xb4, 0xb4, 0xb4], PRIMARY_RGB = [0xf8, 0xf8, 0xf8], FAINT_RGB = [0x8a, 0x8a, 0x8a];

const A = (a) => `rgba(255,255,255,${a / 255})`;
const B = (a) => `rgba(0,0,0,${a / 255})`;
const rgba = (c, a) => `rgba(${Math.round(c[0])},${Math.round(c[1])},${Math.round(c[2])},${a})`;
const mixc = (a, b, t) => [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t];
const clamp = (v, a, b) => Math.max(a, Math.min(b, v));

function rr(x, y, w, h, r) {
  ctx.beginPath();
  const rad = Math.max(0, Math.min(r, Math.min(w, h) / 2));
  ctx.roundRect(x, y, w, h, rad);
}

function fillRR(x, y, w, h, r, color) {
  rr(x, y, w, h, r);
  ctx.fillStyle = color;
  ctx.fill();
}

function strokeRR(x, y, w, h, r, color, width = 1) {
  rr(x + 0.5, y + 0.5, w - 1, h - 1, Math.max(0, r - 0.5));
  ctx.strokeStyle = color;
  ctx.lineWidth = width;
  ctx.stroke();
}

function rect(x, y, w, h, color) {
  ctx.fillStyle = color;
  ctx.fillRect(x, y, w, h);
}

/* --------------------------------------------------------------- UI font
 *
 * Minecraft's own font: assets/minecraft/font/ascii.png, the 128x128 bitmap
 * atlas with 16x16 cells of 8x8 pixels. A glyph advances by its ink width plus
 * the vanilla one-pixel gap; the space glyph is empty in the atlas and comes
 * from minecraft:include/space, so it advances 4 px. Bold is the game's own
 * pass: every glyph is drawn a second time one font pixel to the right, and it
 * does not change the measured width.
 *
 * Sizes are therefore Minecraft sizes, exactly like DashboardScreen's
 * S_TEXT / S_TITLE: 1 = the 8 px glyphs, 2 = vanilla's 16 px title.
 */
const FONT_CELL = 8;
const FONT_COLS = 16;
const S_TEXT = 1;
const S_TITLE = 2;
const S_NAME = 1;
const S_DESC = 1;
const S_LABEL = 1;

const atlas = new Image();
let fontReady = false;
/** Per-codepoint advance in font pixels (ink + gap). */
const advance = new Uint8Array(256);
/** Atlas recoloured per text colour; the palette is small, so this stays tiny. */
const glyphSheets = new Map();

/** Ink widths come straight from the atlas, the same scan the game does. */
function measureGlyphs() {
  const c = document.createElement('canvas');
  c.width = atlas.width;
  c.height = atlas.height;
  const g = c.getContext('2d', { willReadFrequently: true });
  g.drawImage(atlas, 0, 0);
  const px = g.getImageData(0, 0, c.width, c.height).data;
  for (let code = 0; code < 256; code++) {
    const ox = (code % FONT_COLS) * FONT_CELL;
    const oy = Math.floor(code / FONT_COLS) * FONT_CELL;
    let ink = 0;
    for (let x = 0; x < FONT_CELL; x++) {
      for (let y = 0; y < FONT_CELL; y++) {
        if (px[((oy + y) * c.width + ox + x) * 4 + 3] !== 0) ink = Math.max(ink, x + 1);
      }
    }
    advance[code] = ink + 1;
  }
  advance[32] = 4;
  fontReady = true;
}

function glyphSheet(color) {
  let sheet = glyphSheets.get(color);
  if (sheet) return sheet;
  sheet = document.createElement('canvas');
  sheet.width = atlas.width;
  sheet.height = atlas.height;
  const g = sheet.getContext('2d');
  g.fillStyle = color;
  g.fillRect(0, 0, sheet.width, sheet.height);
  g.globalCompositeOperation = 'destination-in';
  g.drawImage(atlas, 0, 0);
  if (glyphSheets.size > 256) glyphSheets.clear();
  glyphSheets.set(color, sheet);
  return sheet;
}

function text(s, x, y, color, scale = S_TEXT, bold = false) {
  if (!fontReady) return;
  const sheet = glyphSheet(color);
  const size = FONT_CELL * scale;
  // Minecraft truncates every text origin to whole GUI pixels.
  let pen = Math.floor(x);
  const top = Math.floor(y);
  for (let i = 0; i < s.length; i++) {
    const code = s.charCodeAt(i) & 0xFF;
    if (code !== 32) {
      const gx = (code % FONT_COLS) * FONT_CELL;
      const gy = Math.floor(code / FONT_COLS) * FONT_CELL;
      ctx.drawImage(sheet, gx, gy, FONT_CELL, FONT_CELL, pen, top, size, size);
      if (bold) ctx.drawImage(sheet, gx, gy, FONT_CELL, FONT_CELL, pen + scale, top, size, size);
    }
    pen += advance[code] * scale;
  }
}

function textW(s, scale = S_TEXT) {
  let w = 0;
  for (let i = 0; i < s.length; i++) w += advance[s.charCodeAt(i) & 0xFF];
  return w * scale;
}

function icon(ch, cx, cy, color, size = 10) {
  ctx.font = `${size}px Icons`;
  ctx.fillStyle = color;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(ch, cx, cy);
  ctx.textAlign = 'start';
  ctx.textBaseline = 'alphabetic';
}

const inRect = (x, y, rx, ry, rw, rh) => x >= rx && y >= ry && x < rx + rw && y < ry + rh;
const approach = (v, t, k, dt) => v + (t - v) * (1 - Math.exp(-k * dt));

/** Critically-ish damped spring, gives the toggle knob its little overshoot. */
function spring(v, vel, target, dt, k = 260, d = 17) {
  const acc = (target - v) * k - vel * d;
  const nv = vel + acc * dt;
  return [v + nv * dt, nv];
}

/** Window entrance: everything scales a hair around the window centre. */
const introK = () => 0.982 + 0.018 * state.intro;
const INTRO_CX = WIN.x + WIN.w / 2, INTRO_CY = WIN.y + WIN.h / 2;
function ix(x) { return INTRO_CX + (x - INTRO_CX) * introK(); }
function iy(y) { return INTRO_CY + (y - INTRO_CY) * introK(); }
function iw(w) { return w * introK(); }

/** Shared card geometry for the glass and the UI pass. */
function cardVisual(c, i) {
  const m = c.m;
  const appear = clamp((state.catAnim - i * 0.045) / 0.55, 0, 1);
  const lift = m.expand > 0.02 ? 0 : m.hover * CARD_LIFT;
  return {
    x: c.x,
    y: c.y - lift + (1 - appear) * 10,
    w: c.w,
    h: c.h,
    appear,
  };
}

/** Same truncation the game uses before drawing a card label. */
function fit(s, maxW, scale = S_TEXT) {
  if (textW(s, scale) <= maxW) return s;
  let out = s;
  while (out.length > 0 && textW(out + '...', scale) > maxW) out = out.slice(0, -1);
  return out + '...';
}

function fmt(v, step, unit) {
  const d = step < 0.095 ? 2 : (step < 0.95 ? 1 : 0);
  let s = v.toFixed(d);
  if (s.includes('.')) s = s.replace(/0+$/, '').replace(/\.$/, '');
  return s + (unit || '');
}

// ------------------------------------------------------------------ glass GL
const VERT = `#version 300 es
precision highp float;
in vec2 aPos;
in vec2 aUv;
out vec2 vUv;
void main() { vUv = aUv; gl_Position = vec4(aPos, 0.0, 1.0); }`;

const WORLD_FRAG = `#version 300 es
precision highp float;
uniform sampler2D uTex;
uniform float uDim;
in vec2 vUv;
out vec4 fragColor;
void main() {
    vec3 c = texture(uTex, vUv).rgb;
    fragColor = vec4(c * (1.0 - uDim), 1.0);
}`;

const GLASS_FRAG = `#version 300 es
precision highp float;

uniform sampler2D uBackdrop;
uniform vec2 uScreen;
uniform vec4 uTint;
uniform vec4 uPanel;  // x = radius / short side, y = opacity, z = hover, w = world factor
uniform float uFlat;  // 1 = plain tinted sheet (accent glow under a card)

in vec2 vUv;
out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;
}

void main() {
    vec2 panelPx = vec2(
        1.0 / max(abs(dFdx(vUv.x)), 1e-6),
        1.0 / max(abs(dFdy(vUv.y)), 1e-6));
    float minSide = min(panelPx.x, panelPx.y);
    float radius = clamp(uPanel.x, 0.0, 0.5) * minSide;

    vec2 halfSize = max(panelPx * 0.5 - vec2(0.75), vec2(0.5));
    vec2 point = (vUv - 0.5) * panelPx;
    float dist = sdRoundBox(point, halfSize, radius);

    vec2 grad = vec2(dFdx(dist), dFdy(dist));
    vec2 normal = length(grad) > 1e-6 ? normalize(grad) : vec2(0.0, -1.0);
    float aa = max(fwidth(dist) * 1.15, 0.6);
    float shape = 1.0 - smoothstep(-aa, aa, dist);
    float borderWidth = max(1.0, fwidth(dist) * 1.6);

    if (shape <= 0.003) {
        discard;
    }

    float opacity = clamp(uPanel.y, 0.0, 1.0);

    if (uFlat > 0.5) {
        fragColor = vec4(uTint.rgb, opacity * shape);
        return;
    }

    vec2 screenUv = gl_FragCoord.xy / uScreen;

    float inner = clamp(-dist / max(radius * 1.5 + 4.0, 1.0), 0.0, 1.0);
    float lens = pow(1.0 - inner, 2.4);

    vec2 bend = -normal * lens * (5.0 + radius * 0.45) / uScreen;
    float dispersion = lens * 1.6 / uScreen.x;

    vec3 refracted;
    refracted.r = texture(uBackdrop, clamp(screenUv + bend * 1.03 + vec2(dispersion, 0.0), vec2(0.0), vec2(1.0))).r;
    refracted.g = texture(uBackdrop, clamp(screenUv + bend, vec2(0.0), vec2(1.0))).g;
    refracted.b = texture(uBackdrop, clamp(screenUv + bend * 0.97 - vec2(dispersion, 0.0), vec2(0.0), vec2(1.0))).b;

    vec3 bodyTint = mix(vec3(1.0), uTint.rgb, 0.10);
    vec3 color = refracted * uPanel.w;
    color = mix(color, color * bodyTint, 0.60);
    color += vec3(0.030, 0.032, 0.040);
    color *= mix(1.0, 0.82, vUv.y);
    color = max(color, vec3(0.022, 0.024, 0.030));

    vec2 lightDir = normalize(vec2(-0.45, 0.89));
    float rim = clamp(1.0 - inner, 0.0, 1.0);
    float spec = pow(max(dot(normal, lightDir), 0.0), 2.0) * rim * rim * rim;
    color += vec3(1.0) * spec * (0.30 + uPanel.z * 0.22);

    float borderAlpha = clamp(uTint.a, 0.0, 1.0);
    if (borderAlpha > 0.002) {
        float band = 1.0 - smoothstep(borderWidth - 0.75, borderWidth + 0.75, -dist);
        vec3 borderColor = mix(vec3(1.0), uTint.rgb, 0.45);
        // top-lit rim: the same hairline reads as light coming from above
        borderColor *= mix(1.0, 0.72, smoothstep(0.15, 0.85, vUv.y));
        color = mix(color, borderColor, band * borderAlpha);
    }

    float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5;
    color += dither / 255.0;

    fragColor = vec4(color, opacity * shape);
}`;

function compile(type, src) {
  const s = gl.createShader(type);
  gl.shaderSource(s, src);
  gl.compileShader(s);
  if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s));
  return s;
}

function program(vs, fs) {
  const p = gl.createProgram();
  gl.attachShader(p, compile(gl.VERTEX_SHADER, vs));
  gl.attachShader(p, compile(gl.FRAGMENT_SHADER, fs));
  gl.bindAttribLocation(p, 0, 'aPos');
  gl.bindAttribLocation(p, 1, 'aUv');
  gl.linkProgram(p);
  if (!gl.getProgramParameter(p, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(p));
  return p;
}

let worldProg, glassProg, quadBuf, uW = {}, uG = {};

function initGL() {
  worldProg = program(VERT, WORLD_FRAG);
  glassProg = program(VERT, GLASS_FRAG);
  uW = { tex: gl.getUniformLocation(worldProg, 'uTex'), dim: gl.getUniformLocation(worldProg, 'uDim') };
  uG = {
    backdrop: gl.getUniformLocation(glassProg, 'uBackdrop'),
    screen: gl.getUniformLocation(glassProg, 'uScreen'),
    tint: gl.getUniformLocation(glassProg, 'uTint'),
    panel: gl.getUniformLocation(glassProg, 'uPanel'),
    flat: gl.getUniformLocation(glassProg, 'uFlat'),
  };

  quadBuf = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, quadBuf);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(24), gl.DYNAMIC_DRAW);

  const vao = gl.createVertexArray();
  gl.bindVertexArray(vao);
  gl.bindBuffer(gl.ARRAY_BUFFER, quadBuf);
  gl.enableVertexAttribArray(0);
  gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 16, 0);
  gl.enableVertexAttribArray(1);
  gl.vertexAttribPointer(1, 2, gl.FLOAT, false, 16, 8);
}

const backdropTex = gl.createTexture();
let backdropReady = false;

function uploadBackdrop(img) {
  const off = document.createElement('canvas');
  off.width = glassCanvas.width;
  off.height = glassCanvas.height;
  const c = off.getContext('2d');
  const s = Math.max(off.width / img.width, off.height / img.height);
  const w = img.width * s, h = img.height * s;
  c.drawImage(img, (off.width - w) / 2, (off.height - h) / 2, w, h);

  gl.bindTexture(gl.TEXTURE_2D, backdropTex);
  gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
  gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, off);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  backdropReady = true;
}

// -------------------------------------------------------------------- layout
function visibleModules() {
  const q = state.search.trim().toLowerCase();
  if (q) {
    return MODULES.filter((m) => m.n.toLowerCase().includes(q) || m.d.toLowerCase().includes(q));
  }
  return MODULES.filter((m) => m.cat === state.cat);
}

function rowHeight(s) {
  if (s.t === 'bool') return 26;
  if (s.t === 'num') return 40;
  if (s.t === 'mode') return 30;
  if (s.t === 'color') return 30 + (s.open ? 4 * 22 : 0);
  return 26;
}

function settingsHeight(m) {
  return m.s.reduce((a, s) => a + rowHeight(s), 0);
}

function cardHeight(m) {
  if (m.expand < 0.001) return CARD_H;
  return CARD_H + m.expand * (settingsHeight(m) + 16);
}

function cardLayout() {
  const out = [];
  const list = visibleModules();
  let y0 = AY, y1 = AY;
  for (let i = 0; i < list.length; i++) {
    const m = list[i];
    const left = i % 2 === 0;
    const h = cardHeight(m);
    const x = left ? AX : AX + CARD_W + GAP;
    const y = (left ? y0 : y1) - state.scroll;
    if (left) y0 += h + GAP; else y1 += h + GAP;
    out.push({ m, x, y, w: CARD_W, h });
  }
  return out;
}

function maxScroll() {
  const list = visibleModules();
  let y0 = 0, y1 = 0;
  list.forEach((m, i) => {
    const h = cardHeight(m) + GAP;
    if (i % 2 === 0) y0 += h; else y1 += h;
  });
  const total = Math.max(y0, y1) - GAP;
  return Math.max(0, total - AH);
}

function hudButtonRect() {
  const btnW = Math.round(textW('Adjust HUD Layout')) + 24;
  const cfgW = Math.round(textW('octoclient.json'));
  const x = Math.round(WIN.x + WIN.w - 18 - cfgW - 20 - btnW);
  return { x, y: FOOTER_Y - 5, w: btnW, h: 20 };
}

/** Rows of the settings accordion, in the same space the draw pass uses. */
function settingsLayout(c) {
  const m = c.m;
  const rows = [];
  let y = c.y + CARD_H + 10;
  const x = c.x + CARD_PAD + 4;
  const w = c.w - (CARD_PAD + 4) * 2;
  for (const s of m.s) {
    rows.push({ s, x, y, w, h: rowHeight(s) });
    y += rowHeight(s);
  }
  return rows;
}

// ---------------------------------------------------------------- glass pass
const panels = [];

function panelFor(p) {
  panels.push(p);
}

function buildPanels() {
  panels.length = 0;

  const ia = state.intro;
  const k = introK();
  const shell = { x: ix(WIN.x), y: iy(WIN.y), w: iw(WIN.w), h: iw(WIN.h) };
  panelFor({ ...shell, r: 18 * k, tint: [0x8e, 0x97, 0xc4], opacity: ia, border: 0, hover: 0, world: 0.18 });

  panelFor({
    x: ix(SEARCH.x), y: iy(SEARCH.y), w: iw(SEARCH.w), h: iw(SEARCH.h), r: 8 * k,
    tint: [0xb9, 0xc0, 0xd8], opacity: 0.94 * ia,
    border: (26 + state.focusA * 40) / 255, hover: 0, world: 0.26,
  });

  panelFor({ x: ix(STATUS.x), y: iy(STATUS.y), w: iw(STATUS.w), h: iw(STATUS.h), r: 10 * k, tint: WHITE, opacity: 0.72 * ia, border: 0, hover: 0, world: 0.34 });

  const list = cardLayout();
  for (let i = 0; i < list.length; i++) {
    const c = list[i];
    const m = c.m;
    const v = cardVisual(c, i);
    if (v.appear < 0.01) continue;
    const tint = mixc(WHITE, ACCENT, m.onA);
    if (m.glow > 0.01) {
      panelFor({
        x: ix(v.x - 1), y: iy(v.y - 1), w: iw(v.w + 2), h: iw(v.h + 2), r: (CARD_R + 1) * k,
        tint: ACCENT, opacity: ((26 * m.glow + 26 * m.flash) / 255) * v.appear * ia,
        border: 0, hover: 0, world: 0, flat: 1, clip: true,
      });
    }
    panelFor({
      x: ix(v.x), y: iy(v.y), w: iw(v.w), h: iw(v.h), r: CARD_R * k,
      tint, opacity: (0.90 + m.hover * 0.06) * v.appear * ia,
      border: (clamp(120 * m.onA + (18 + m.hover * 46) * (1 - m.onA) + 90 * m.flash, 0, 255)) / 255,
      hover: m.hover, world: 0.30, clip: true,
    });
  }
}

function drawGlass() {
  if (!gl || !backdropReady) return;
  gl.viewport(0, 0, glassCanvas.width, glassCanvas.height);
  gl.disable(gl.SCISSOR_TEST);
  gl.disable(gl.BLEND);

  gl.useProgram(worldProg);
  gl.bindBuffer(gl.ARRAY_BUFFER, quadBuf);
  gl.bufferSubData(gl.ARRAY_BUFFER, 0, new Float32Array([
    -1, -1, 0, 0, -1, 1, 0, 1, 1, 1, 1, 1,
    -1, -1, 0, 0, 1, 1, 1, 1, 1, -1, 1, 0,
  ]));
  gl.activeTexture(gl.TEXTURE0);
  gl.bindTexture(gl.TEXTURE_2D, backdropTex);
  gl.uniform1i(uW.tex, 0);
  gl.uniform1f(uW.dim, 0x70 / 255);
  gl.drawArrays(gl.TRIANGLES, 0, 6);

  gl.enable(gl.BLEND);
  gl.blendFuncSeparate(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA, gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
  gl.useProgram(glassProg);
  gl.uniform2f(uG.screen, glassCanvas.width, glassCanvas.height);
  gl.uniform1i(uG.backdrop, 0);

  for (const p of panels) {
    if (p.clip) {
      const cx = Math.round((ox + ix(AX) * ui) * dpr);
      const cy = Math.round((oy + iy(AY) * ui) * dpr);
      const cw = Math.round(iw(AW) * ui * dpr);
      const ch = Math.round(iw(AH) * ui * dpr);
      gl.enable(gl.SCISSOR_TEST);
      gl.scissor(cx, glassCanvas.height - cy - ch, cw, ch);
    } else {
      gl.disable(gl.SCISSOR_TEST);
    }

    const sx = (ox + p.x * ui) * dpr;
    const sy = (oy + p.y * ui) * dpr;
    const sw = p.w * ui * dpr;
    const sh = p.h * ui * dpr;
    const x0 = (sx / glassCanvas.width) * 2 - 1;
    const x1 = ((sx + sw) / glassCanvas.width) * 2 - 1;
    const y0 = 1 - (sy / glassCanvas.height) * 2;
    const y1 = 1 - ((sy + sh) / glassCanvas.height) * 2;

    gl.bufferSubData(gl.ARRAY_BUFFER, 0, new Float32Array([
      x0, y0, 0, 0, x0, y1, 0, 1, x1, y1, 1, 1,
      x0, y0, 0, 0, x1, y1, 1, 1, x1, y0, 1, 0,
    ]));
    gl.uniform4f(uG.tint, p.tint[0] / 255, p.tint[1] / 255, p.tint[2] / 255, p.border);
    gl.uniform4f(uG.panel, p.r / Math.max(1, Math.min(p.w, p.h)), p.opacity, p.hover, p.world);
    gl.uniform1f(uG.flat, p.flat ? 1 : 0);
    gl.drawArrays(gl.TRIANGLES, 0, 6);
  }
  gl.disable(gl.SCISSOR_TEST);
}

// ------------------------------------------------------------------ controls
function drawSwitch(x, y, w, h, a, hover) {
  const r = h / 2;
  const ac = clamp(a, 0, 1);
  fillRR(x, y, w, h, r, B(230));
  if (ac > 0.01) {
    ctx.save();
    ctx.globalAlpha *= ac;
    fillRR(x, y, w, h, r, 'rgba(248,248,248,.918)');
    ctx.restore();
  }
  strokeRR(x, y, w, h, r, rgba(mixc([0x3f, 0x3f, 0x44], ACCENT, ac), 1));
  const knob = h - 6;
  fillRR(x + 3 + a * (w - knob - 6), y + 3, knob, knob, knob / 2,
    rgba(mixc([0x8a, 0x8a, 0x8a], [0x05, 0x05, 0x06], ac), 1));
}

function drawSlider(x, y, w, frac, color, hot) {
  const h = 4;
  const kr = hot ? 6.2 : 5;
  fillRR(x, y, w, h, h / 2, A(26));
  fillRR(x, y, Math.max(h, w * frac), h, h / 2, color);
  const kx = x + clamp(w * frac, 0, w);
  ctx.beginPath();
  ctx.arc(kx, y + h / 2, kr, 0, Math.PI * 2);
  ctx.fillStyle = '#f8f8f8';
  ctx.fill();
  ctx.beginPath();
  ctx.arc(kx, y + h / 2, kr, 0, Math.PI * 2);
  ctx.strokeStyle = 'rgba(0,0,0,.45)';
  ctx.lineWidth = 1;
  ctx.stroke();
}

// --------------------------------------------------------------------- UI
function drawUI() {
  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.clearRect(0, 0, uiCanvas.width, uiCanvas.height);
  const s = ui * dpr;
  ctx.setTransform(s, 0, 0, s, ox * dpr, oy * dpr);
  ctx.imageSmoothingEnabled = false;
  ctx.globalAlpha = clamp(state.intro, 0, 1);
  if (state.intro < 0.999) {
    const k = introK();
    ctx.translate(INTRO_CX, INTRO_CY);
    ctx.scale(k, k);
    ctx.translate(-INTRO_CX, -INTRO_CY);
  }

  const wx = WIN.x, wy = WIN.y, ww = WIN.w, wh = WIN.h;

  // ---- header / footer washes -------------------------------------------
  fillRR(wx + 1, wy + 1, ww - 2, HEADER_H, 15, A(9));
  rect(wx + 1, wy + HEADER_H - 6, ww - 2, 6, A(9));
  rect(wx + 1, wy + HEADER_H, ww - 2, 1, A(13));
  fillRR(wx + 1, wy + wh - FOOTER_H - 1, ww - 2, FOOTER_H, 15, B(34));
  rect(wx + 1, wy + wh - FOOTER_H - 1, ww - 2, 1, A(11));

  // ---- sidebar -----------------------------------------------------------
  const sx = SIDEBAR.x, sy = SIDEBAR.y, sh = SIDEBAR.h;
  const grad = ctx.createLinearGradient(0, sy, 0, sy + sh);
  grad.addColorStop(0, B(58));
  grad.addColorStop(1, B(22));
  ctx.fillStyle = grad;
  ctx.fillRect(sx, sy, SIDEBAR_W - 1, sh);
  rect(sx + SIDEBAR_W - 2, sy, 1, sh, B(70));

  text('CATEGORIES', sx + 14, sy + 11, C.faint);

  // sliding pill follows the active category
  const indY = state.indY === null ? sy + 26 : state.indY;
  fillRR(sx + 8, indY, SIDEBAR_W - 17, 29, 9, A(17));
  strokeRR(sx + 8, indY, SIDEBAR_W - 17, 29, 9, A(36));
  fillRR(sx + 9, indY + 8.5, 2, 12, 1, 'rgba(79,139,247,.784)');

  CATEGORIES.forEach((cat, i) => {
    const y = sy + 26 + i * 32;
    const a = clamp(cat.a || 0, 0, 1);
    const active = cat.name === state.cat;
    const hov = state.inside && inRect(state.mx, state.my, sx + 8, y, SIDEBAR_W - 17, 29) && !active;
    if (hov) fillRR(sx + 8, y, SIDEBAR_W - 17, 29, 9, B(128));
    const col = rgba(mixc(MUTED_RGB, PRIMARY_RGB, a), 1);
    icon(cat.icon, sx + 19, y + 15, col);
    text(cat.name, sx + 33, y + 10, col);
    const count = MODULES.filter((m) => m.cat === cat.name).length;
    const cw = textW(String(count));
    const bw = cw + 9;
    const bx = sx + SIDEBAR_W - 17 - bw - 6;
    fillRR(bx, y + 8.5, bw, 12, 4, B(58 + 28 * a));
    strokeRR(bx, y + 8.5, bw, 12, 4, A(22 + 18 * a));
    text(String(count), bx + 4.5, y + 10.5, rgba(mixc(FAINT_RGB, PRIMARY_RGB, a), 1));
  });

  // ---- status card -------------------------------------------------------
  text('STATUS', STATUS.x + 11, STATUS.y + 9, C.faint);
  text('Fair Play Client', STATUS.x + 11, STATUS.y + 24, C.secondary);
  fillRR(STATUS.x + STATUS.w - 24, STATUS.y + 24, 13, 13, 6.5, 'rgba(16,185,129,.14)');
  icon(ICON.check, STATUS.x + STATUS.w - 17.5, STATUS.y + 30.5, C.emerald, 10);

  // ---- header ------------------------------------------------------------
  const tile = 32, tx = wx + 16, ty = wy + 10;
  const lg = ctx.createLinearGradient(0, ty, 0, ty + tile);
  lg.addColorStop(0, '#32323a');
  lg.addColorStop(1, C.dark900);
  fillRR(tx, ty, tile, tile, 10, lg);
  strokeRR(tx, ty, tile, tile, 10, '#46464d');
  rect(tx + 8, ty + 1, tile - 16, 1, A(40));
  icon(ICON.shield, tx + tile / 2, ty + tile / 2 + 1, C.primary, 16);

  const titleX = tx + tile + 12, titleY = wy + 11;
  text('OCTOCLIENT', titleX, titleY, C.primary, S_TITLE, true);

  const badgeX = Math.round(titleX + textW('OCTOCLIENT', S_TITLE) + 9);
  const badgeW = Math.round(textW('Legit Utility')) + 11;
  fillRR(badgeX, titleY + 1.5, badgeW, 12, 6, A(13));
  strokeRR(badgeX, titleY + 1.5, badgeW, 12, 6, A(28));
  text('Legit Utility', badgeX + 5.5, titleY + 3, C.muted);

  const subY = titleY + 19;
  let sxr = titleX;
  text('Press', sxr, subY, C.faint);
  sxr += textW('Press ');
  text('RIGHT-SHIFT', sxr, subY, C.secondary);
  sxr += textW('RIGHT-SHIFT ');
  text('or', sxr, subY, C.faint);
  sxr += textW('or ');
  text('ESC', sxr, subY, C.secondary);
  sxr += textW('ESC ');
  text('to close', sxr, subY, C.faint);

  // search well
  icon(ICON.search, SEARCH.x + 12, SEARCH.y + 11, C.faint);
  if (state.search) {
    text(state.search, SEARCH.x + 24, SEARCH.y + 7, C.secondary);
    if (state.focused && Math.floor(state.time * 2) % 2 === 0) {
      const cx = SEARCH.x + 24 + textW(state.search) + 1;
      rect(cx, SEARCH.y + 6, 1, 11, A(150));
    }
    const clearHover = state.inside && inRect(state.mx, state.my, SEARCH.x + SEARCH.w - 20, SEARCH.y, 20, SEARCH.h);
    icon(ICON.xmark, SEARCH.x + SEARCH.w - 10, SEARCH.y + 11, clearHover ? C.primary : C.faint, 9);
  } else {
    text('Search utility...', SEARCH.x + 24, SEARCH.y + 7, C.faint);
  }

  const closeHover = state.inside && inRect(state.mx, state.my, CLOSE.x, CLOSE.y, CLOSE.w, CLOSE.h);
  fillRR(CLOSE.x, CLOSE.y, CLOSE.w, CLOSE.h, 8, closeHover ? A(30) : B(62));
  strokeRR(CLOSE.x, CLOSE.y, CLOSE.w, CLOSE.h, 8, A(closeHover ? 62 : 28));
  icon(ICON.xmark, CLOSE.x + CLOSE.w / 2, CLOSE.y + CLOSE.h / 2 + 0.5, closeHover ? C.primary : C.muted);

  // ---- cards -------------------------------------------------------------
  ctx.save();
  ctx.beginPath();
  ctx.rect(AX, AY, AW, AH);
  ctx.clip();

  const list = cardLayout();
  for (let i = 0; i < list.length; i++) {
    const c = list[i];
    const m = c.m;
    const v = cardVisual(c, i);
    if (v.appear < 0.02) continue;

    const x = v.x, y = v.y, w = v.w;
    const baseAlpha = ctx.globalAlpha;
    ctx.globalAlpha = baseAlpha * v.appear;

    // icon tile
    fillRR(x + 11, y + 18, 26, 26, 8, B(38 + 18 * m.hover));
    strokeRR(x + 11, y + 18, 26, 26, 8, A(14 + 16 * m.hover));
    const img = items[m.i];
    if (img && img.complete) ctx.drawImage(img, x + 16, y + 23, 16, 16);

    // name + description (description runs under the controls, full card width)
    text(fit(m.n, w - 45 - 78, S_NAME), x + 45, y + 20, C.primary, S_NAME, true);
    text(fit(m.d, w - 89, S_DESC), x + 45, y + 33, C.muted, S_DESC);

    // gear tile (rotates a quarter turn while opening)
    const hasSettings = m.s.length > 0;
    if (hasSettings) {
      const gx = x + w - 74, gy = y + 13;
      const gearHover = state.inside && inRect(state.mx, state.my, gx, gy, 22, 20);
      const open = m.open;
      fillRR(gx, gy, 22, 20, 7, B(gearHover || open ? 92 : 56));
      strokeRR(gx, gy, 22, 20, 7, A(gearHover || open ? 54 : 26));
      ctx.save();
      ctx.translate(gx + 11, gy + 10);
      ctx.rotate(m.spin * Math.PI * 0.5);
      icon(ICON.gear, 0, 0, open ? C.primary : (gearHover ? C.secondary : C.muted));
      ctx.restore();
    }

    // toggle
    drawSwitch(x + w - 46, y + 13, 38, 20, m.onA, m.hover);

    // press feedback
    if (m.press > 0.01) fillRR(x, y, w, c.h, CARD_R, B(70 * m.press));

    // settings accordion
    if (m.expand > 0.02) {
      ctx.save();
      ctx.beginPath();
      ctx.rect(x, y + CARD_H - 2, w, c.h - CARD_H);
      ctx.clip();

      const fade = clamp((m.expand - 0.02) / 0.5, 0, 1);
      ctx.globalAlpha = baseAlpha * v.appear * fade;

      // inset well behind the controls so the accordion reads as a sub-surface
      fillRR(x + 8, y + CARD_H + 3, w - 16, Math.max(0, c.h - CARD_H - 6), 9, B(86));
      strokeRR(x + 8, y + CARD_H + 3, w - 16, Math.max(0, c.h - CARD_H - 6), 9, A(10));
      rect(x + 18, y + CARD_H + 3, w - 36, 1, A(12));

      const rows = settingsLayout({ ...c, y });
      for (let ri = 0; ri < rows.length; ri++) {
        const ra = clamp((m.expand * 1.3 - (ri / Math.max(1, rows.length)) * 0.55) / 0.55, 0, 1);
        if (ra < 0.02) continue;
        ctx.globalAlpha = baseAlpha * v.appear * fade * ra;
        drawSettingRow({ ...rows[ri], y: rows[ri].y + (1 - ra) * 7 }, x, y, m);
      }
      ctx.restore();
    }

    ctx.globalAlpha = baseAlpha;
  }
  ctx.restore();

  if (list.length === 0) {
    const hint = state.search ? 'No utility found' : 'Nothing here';
    ctx.globalAlpha = clamp(state.intro, 0, 1) * clamp(state.catAnim, 0, 1);
    text(hint, AX + AW / 2 - textW(hint) / 2, AY + 20, C.faint);
    ctx.globalAlpha = clamp(state.intro, 0, 1);
  }

  // ---- scrollbar ---------------------------------------------------------
  const max = maxScroll();
  if (max > 0) {
    const sbX = AX + AW + 6;
    const thumbH = Math.max(28, (AH * AH) / (AH + max));
    const t = max > 0 ? clamp(state.scroll / max, 0, 1) : 0;
    fillRR(sbX, AY, 2, AH, 1, A(16));
    fillRR(sbX, AY + t * (AH - thumbH), 2, thumbH, 1, A(95));
  }

  // ---- footer ------------------------------------------------------------
  fillRR(wx + 18, FOOTER_Y + 3, 6, 6, 3, C.emerald);
  text('Mod Status:', wx + 30, FOOTER_Y, C.faint);
  text('Clean Utility Client', wx + 30 + textW('Mod Status: '), FOOTER_Y, C.secondary);

  const btn = hudButtonRect();
  const btnHover = state.inside && inRect(state.mx, state.my, btn.x, btn.y, btn.w, btn.h);
  fillRR(btn.x, btn.y, btn.w, btn.h, 7, A(btnHover ? 24 : 11));
  strokeRR(btn.x, btn.y, btn.w, btn.h, 7, A(btnHover ? 78 : 32));
  const label = 'Adjust HUD Layout';
  text(label, btn.x + (btn.w - textW(label)) / 2, FOOTER_Y, btnHover ? C.primary : C.muted);
  text('|', btn.x + btn.w + 9, FOOTER_Y, C.dark600);
  const cfgW = Math.round(textW('octoclient.json'));
  text('octoclient.json', wx + ww - 18 - cfgW, FOOTER_Y, C.faint);
}

function drawSettingRow(row, cardX, cardY, m) {
  const { s, x, y, w } = row;
  const hoverRow = state.inside && inRect(state.mx, state.my, x, y, w, row.h);

  if (s.t === 'bool') {
    text(fit(s.name, w - 40, S_LABEL), x, y + 9, C.secondary, S_LABEL);
    drawSwitch(x + w - 30, y + 5.5, 30, 15, s.a === undefined ? (s.v ? 1 : 0) : s.a, hoverRow ? 1 : 0);
    return;
  }

  if (s.t === 'num') {
    text(fit(s.name, w - 80, S_LABEL), x, y + 6, C.secondary, S_LABEL);
    const val = fmt(s.v, s.step, s.unit);
    text(val, x + w - textW(val, S_LABEL), y + 6, C.primary, S_LABEL);
    const frac = (s.v - s.min) / (s.max - s.min);
    drawSlider(x, y + 25, w, frac, 'rgba(248,248,248,.85)', hoverRow || (state.drag && state.drag.s === s));
    return;
  }

  if (s.t === 'mode') {
    text(fit(s.name, w - 100, S_LABEL), x, y + 9, C.secondary, S_LABEL);
    const pw = s.pw == null ? Math.min(96, Math.round(textW(s.v, S_LABEL)) + 26) : s.pw;
    const px = x + w - pw;
    const pillHover = state.inside && inRect(state.mx, state.my, px, y + 4, pw, 18);
    fillRR(px, y + 4, pw, 18, 6, B(pillHover ? 100 : 72));
    strokeRR(px, y + 4, pw, 18, 6, A(pillHover ? 54 : 30));
    text(fit(s.v, pw - 22, S_LABEL), px + 7, y + 9, C.primary, S_LABEL);
    icon(ICON.chevron, px + pw - 9, y + 13, C.faint, 7);
    return;
  }

  if (s.t === 'color') {
    text(fit(s.name, w - 60, S_LABEL), x, y + 9, C.secondary, S_LABEL);
    const swX = x + w - 44;
    const swHover = state.inside && inRect(state.mx, state.my, swX, y + 4, 44, 18);
    fillRR(swX, y + 4, 44, 18, 5, B(120));
    strokeRR(swX, y + 4, 44, 18, 5, A(swHover ? 90 : 46));
    fillRR(swX + 4, y + 8, 36, 10, 3, s.v);
    if (s.open) {
      const names = ['R', 'G', 'B', 'A'];
      const cols = ['#ff6b6b', '#6bff8e', '#6b8eff', '#e8e8e8'];
      const rgb = hexToRgb(s.v);
      const vals = [rgb[0], rgb[1], rgb[2], Math.round(alphaOf(s.v) * 255)];
      for (let i = 0; i < 4; i++) {
        const ry = y + 28 + i * 22;
        text(names[i], x, ry + 4, C.faint, S_TEXT);
        const barX = x + 18, barW = w - 18 - 30;
        drawSlider(barX, ry + 6, barW, vals[i] / 255, cols[i],
          state.drag && state.drag.s === s && state.drag.channel === i);
        const vs = String(vals[i]);
        text(vs, x + w - textW(vs, S_TEXT), ry + 4, C.faint, S_TEXT);
      }
    }
    return;
  }
}

function hexToRgb(hex) {
  const n = parseInt(hex.slice(1, 7), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}
function alphaOf(hex) {
  if (hex.length > 7) return parseInt(hex.slice(7, 9), 16) / 255;
  return 1;
}

// ------------------------------------------------------------------- render
function render() {
  buildPanels();
  drawGlass();
  drawUI();
}

function resize() {
  dpr = Math.min(window.devicePixelRatio || 1, 2);
  const vw = window.innerWidth, vh = window.innerHeight;
  glassCanvas.width = Math.round(vw * dpr);
  glassCanvas.height = Math.round(vh * dpr);
  uiCanvas.width = glassCanvas.width;
  uiCanvas.height = glassCanvas.height;
  ui = Math.min(vw / VW, vh / VH);
  ox = (vw - VW * ui) / 2;
  oy = (vh - VH * ui) / 2;
  if (backdropImage) uploadBackdrop(backdropImage);
  render();
}

// ------------------------------------------------------------------- input
function toVirtual(clientX, clientY) {
  const r = uiCanvas.getBoundingClientRect();
  return { x: (clientX - r.left - ox) / ui, y: (clientY - r.top - oy) / ui };
}

function hitCard(x, y) {
  for (const c of cardLayout()) {
    if (inRect(x, y, c.x, c.y, c.w, c.h)) return c;
  }
  return null;
}

function hitSetting(c, x, y) {
  if (!c || !c.m.open) return null;
  for (const row of settingsLayout(c)) {
    const s = row.s;
    if (!inRect(x, y, row.x, row.y, row.w, row.h)) continue;
    if (s.t === 'bool') return { s, row, kind: 'bool' };
    if (s.t === 'num') return { s, row, kind: 'num' };
    if (s.t === 'mode') return { s, row, kind: 'mode' };
    if (s.t === 'color') {
      if (inRect(x, y, row.x + row.w - 44, row.y + 4, 44, 18)) return { s, row, kind: 'color-swatch' };
      if (s.open) {
        for (let i = 0; i < 4; i++) {
          const ry = row.y + 28 + i * 22;
          if (inRect(x, y, row.x, ry, row.w, 22)) return { s, row, kind: 'channel', channel: i };
        }
      }
    }
  }
  return null;
}

function applyDrag(x) {
  const d = state.drag;
  if (!d) return;
  if (d.kind === 'num') {
    const t = clamp((x - d.row.x) / d.row.w, 0, 1);
    const raw = d.s.min + t * (d.s.max - d.s.min);
    d.s.v = Math.round(raw / d.s.step) * d.s.step;
  } else if (d.kind === 'channel') {
    const barX = d.row.x + 18, barW = d.row.w - 18 - 30;
    const t = clamp((x - barX) / barW, 0, 1);
    setChannel(d.s, d.channel, Math.round(t * 255));
  }
}

function setChannel(s, i, v) {
  const rgb = hexToRgb(s.v);
  const a = Math.round(alphaOf(s.v) * 255);
  const out = [rgb[0], rgb[1], rgb[2], a];
  out[i] = v;
  s.v = '#' + out.slice(0, 3).map((c) => c.toString(16).padStart(2, '0')).join('')
    + (out[3] < 255 ? out[3].toString(16).padStart(2, '0') : '');
}

function updateHover() {
  const x = state.mx, y = state.my;
  const hovered = state.inside ? hitCard(x, y) : null;
  MODULES.forEach((m) => { m.target = 0; });
  if (hovered) hovered.m.target = 1;

  let cursor = 'default';
  if (state.inside) {
    if (hovered) cursor = 'pointer';
    if (hovered && hovered.m.s.length && inRect(x, y, hovered.x + hovered.w - 74, hovered.y + 13, 22, 20)) cursor = 'pointer';
    if (inRect(x, y, CLOSE.x, CLOSE.y, CLOSE.w, CLOSE.h)) cursor = 'pointer';
    if (inRect(x, y, SEARCH.x, SEARCH.y, SEARCH.w, SEARCH.h)) cursor = 'text';
    const b = hudButtonRect();
    if (inRect(x, y, b.x, b.y, b.w, b.h)) cursor = 'pointer';
    for (let i = 0; i < CATEGORIES.length; i++) {
      if (inRect(x, y, SIDEBAR.x + 8, SIDEBAR.y + 26 + i * 32, SIDEBAR_W - 17, 29)) cursor = 'pointer';
    }
    if (hovered) {
      const hs = hitSetting(hovered, x, y);
      if (hs) cursor = hs.kind === 'num' || hs.kind === 'channel' ? 'ew-resize' : 'pointer';
    }
  }
  uiCanvas.style.cursor = cursor;
}

function onDown() {
  const x = state.mx, y = state.my;
  if (!state.inside) return;

  if (inRect(x, y, SEARCH.x, SEARCH.y, SEARCH.w, SEARCH.h)) {
    if (state.search && inRect(x, y, SEARCH.x + SEARCH.w - 20, SEARCH.y, 20, SEARCH.h)) {
      state.search = '';
      return;
    }
    state.focused = true;
    return;
  }
  state.focused = false;

  for (let i = 0; i < CATEGORIES.length; i++) {
    if (inRect(x, y, SIDEBAR.x + 8, SIDEBAR.y + 26 + i * 32, SIDEBAR_W - 17, 29)) {
      if (CATEGORIES[i].name !== state.cat) state.catAnim = 0;
      state.cat = CATEGORIES[i].name;
      state.search = '';
      state.scroll = 0;
      state.scrollTarget = 0;
      MODULES.forEach((m) => { m.open = false; });
      updateHover();
      return;
    }
  }

  const c = hitCard(x, y);
  if (!c) return;

  // gear toggles the settings accordion
  if (c.m.s.length && inRect(x, y, c.x + c.w - 74, c.y + 13, 22, 20)) {
    c.m.open = !c.m.open;
    c.m.press = 1;
    return;
  }

  // settings controls
  const hs = hitSetting(c, x, y);
  if (hs) {
    if (hs.kind === 'bool') hs.s.v = !hs.s.v;
    else if (hs.kind === 'mode') {
      const i = hs.s.options.indexOf(hs.s.v);
      hs.s.v = hs.s.options[(i + 1) % hs.s.options.length];
    } else if (hs.kind === 'color-swatch') hs.s.open = !hs.s.open;
    else if (hs.kind === 'num' || hs.kind === 'channel') {
      state.drag = hs;
      applyDrag(x);
    }
    return;
  }

  // anything else on the card toggles the module
  if (!inRect(x, y, c.x, c.y + CARD_H - 2, c.w, c.h)) {
    c.m.on = !c.m.on;
    c.m.flash = c.m.on ? 1 : 0;
    c.m.press = 1;
  }
}

uiCanvas.addEventListener('mousemove', (e) => {
  const p = toVirtual(e.clientX, e.clientY);
  state.mx = p.x;
  state.my = p.y;
  state.inside = p.x >= 0 && p.y >= 0 && p.x <= VW && p.y <= VH;
  if (state.drag) applyDrag(p.x);
  updateHover();
});

uiCanvas.addEventListener('mouseleave', () => {
  state.inside = false;
  state.drag = null;
  updateHover();
});

uiCanvas.addEventListener('mousedown', (e) => {
  if (e.button !== 0) return;
  const p = toVirtual(e.clientX, e.clientY);
  state.mx = p.x;
  state.my = p.y;
  state.inside = true;
  onDown();
});

window.addEventListener('mouseup', () => { state.drag = null; });

uiCanvas.addEventListener('wheel', (e) => {
  const max = maxScroll();
  if (max <= 0) return;
  e.preventDefault();
  state.scrollTarget = clamp(state.scrollTarget + e.deltaY * 0.35, 0, max);
}, { passive: false });

window.addEventListener('keydown', (e) => {
  if (!state.focused) return;
  if (e.key === 'Escape') { state.focused = false; e.preventDefault(); return; }
  if (e.key === 'Backspace') { state.search = state.search.slice(0, -1); e.preventDefault(); return; }
  if (e.key === 'Enter') { state.focused = false; e.preventDefault(); return; }
  if (e.key.length === 1 && !e.ctrlKey && !e.metaKey) {
    state.search += e.key;
    state.scroll = 0;
    state.scrollTarget = 0;
    e.preventDefault();
  }
});

// -------------------------------------------------------------------- loop
let last = performance.now();

function step(now) {
  const dt = Math.min(0.05, (now - last) / 1000);
  last = now;
  state.time += dt;

  state.intro = approach(state.intro, 1, 7, dt);
  state.catAnim = approach(state.catAnim, 1, 7, dt);
  state.focusA = approach(state.focusA, state.focused ? 1 : 0, 14, dt);

  // sidebar: sliding pill + cross-fading label colours
  const idx = Math.max(0, CATEGORIES.findIndex((c) => c.name === state.cat));
  const indTarget = SIDEBAR.y + 26 + idx * 32;
  if (state.indY === null) state.indY = indTarget;
  state.indY = approach(state.indY, indTarget, 16, dt);
  CATEGORIES.forEach((c) => {
    c.a = approach(c.a, c.name === state.cat ? 1 : 0, 14, dt);
  });

  for (const m of MODULES) {
    m.hover = approach(m.hover, m.target || 0, 18, dt);
    const [nv, nvel] = spring(m.onA, m.vel, m.on ? 1 : 0, dt);
    m.onA = clamp(nv, -0.12, 1.12);
    m.vel = nvel;
    m.glow = approach(m.glow, m.on ? 1 : 0, 10, dt);
    m.expand = approach(m.expand, m.open ? 1 : 0, 14, dt);
    m.spin = approach(m.spin, m.open ? 1 : 0, 11, dt);
    m.press = approach(m.press, 0, 12, dt);
    m.flash = approach(m.flash, 0, 4.5, dt);

    // settings controls animate too
    for (const s of m.s) {
      if (s.t === 'bool') {
        const [nv2, nvel2] = spring(s.a, s.va, s.v ? 1 : 0, dt, 320, 21);
        s.a = clamp(nv2, -0.12, 1.12);
        s.va = nvel2;
      } else if (s.t === 'mode') {
        const target = Math.min(96, Math.round(textW(s.v, S_LABEL)) + 26);
        s.pw = s.pw === null ? target : approach(s.pw, target, 18, dt);
      }
    }
  }

  const max = maxScroll();
  if (state.scrollTarget > max) state.scrollTarget = max;
  state.scroll = approach(state.scroll, state.scrollTarget, 16, dt);
}

function frame(now) {
  step(now);
  render();
  requestAnimationFrame(frame);
}

// -------------------------------------------------------------------- boot
let backdropImage = null;

function boot() {
  initGL();
  loadItems();

  atlas.onload = () => {
    measureGlyphs();
    render();
  };
  atlas.src = 'fonts/ascii.png';

  const img = new Image();
  img.onload = () => {
    backdropImage = img;
    uploadBackdrop(img);
    resize();
  };
  img.onerror = () => {
    const c = document.createElement('canvas');
    c.width = 16; c.height = 16;
    const g = c.getContext('2d');
    const gr = g.createLinearGradient(0, 0, 0, 16);
    gr.addColorStop(0, '#5b7fa8');
    gr.addColorStop(1, '#2c3a2a');
    g.fillStyle = gr; g.fillRect(0, 0, 16, 16);
    const fake = new Image();
    fake.onload = () => { backdropImage = fake; uploadBackdrop(fake); resize(); };
    fake.src = c.toDataURL();
  };
  img.src = 'bg.png';

  resize();
  window.addEventListener('resize', resize);
  requestAnimationFrame(frame);
}

// export hook used to verify the render without a desktop screenshot
function settle(frames = 120) {
  for (let i = 0; i < frames; i++) step(last + 16.7);
}

window.__shot = async (frames = 120) => {
  if (frames > 0) settle(frames);
  render();
  const out = document.createElement('canvas');
  out.width = glassCanvas.width;
  out.height = glassCanvas.height;
  const c = out.getContext('2d');
  c.drawImage(glassCanvas, 0, 0);
  c.drawImage(uiCanvas, 0, 0);
  const blob = await new Promise((r) => out.toBlob(r, 'image/png'));
  const res = await fetch('/save', { method: 'POST', body: blob });
  return res.text();
};

boot();
