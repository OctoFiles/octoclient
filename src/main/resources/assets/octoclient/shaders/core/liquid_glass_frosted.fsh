#version 330

#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 localCoord;
in vec4 tint;
in vec3 panel;

out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;
}

// Strong frost: the same two-ring disc as the clear card, but sampled wider so
// the scene behind the sheet reads as soft shapes instead of legible detail.
vec3 blurredBackdrop(vec2 uv, float radiusPx) {
    vec2 disc = vec2(radiusPx) / ScreenSize;
    vec3 acc = texture(Sampler0, clamp(uv, vec2(0.0), vec2(1.0))).rgb * 0.12;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7853982;
        vec2 off = vec2(cos(a), sin(a)) * disc * 0.55;
        acc += texture(Sampler0, clamp(uv + off, vec2(0.0), vec2(1.0))).rgb * 0.070;
    }
    for (int i = 0; i < 12; i++) {
        float a = float(i) * 3.8832221;
        vec2 off = vec2(cos(a), sin(a)) * disc;
        acc += texture(Sampler0, clamp(uv + off, vec2(0.0), vec2(1.0))).rgb * 0.0275;
    }
    return acc;
}

void main() {
    // Rebuild the panel geometry from the screen-space derivatives of UV0.
    // Every fragment of the quad shares the same panel, so this is exact.
    vec2 panelPx = vec2(
        1.0 / max(abs(dFdx(localCoord.x)), 1e-6),
        1.0 / max(abs(dFdy(localCoord.y)), 1e-6));
    float minSide = min(panelPx.x, panelPx.y);
    float radius = clamp(panel.x, 0.0, 0.5) * minSide;

    vec2 halfSize = max(panelPx * 0.5 - vec2(0.75), vec2(0.5));
    vec2 point = (localCoord - 0.5) * panelPx;
    float dist = sdRoundBox(point, halfSize, radius);

    // All derivatives are taken before the discard so neighbouring fragments
    // never see a divergent control flow.
    vec2 grad = vec2(dFdx(dist), dFdy(dist));
    vec2 normal = length(grad) > 1e-6 ? normalize(grad) : vec2(0.0, -1.0);
    float aa = max(fwidth(dist) * 1.15, 0.6);
    float shape = 1.0 - smoothstep(-aa, aa, dist);
    // Hairline rim: one physical pixel wide at every GUI scale, anti-aliased.
    float borderWidth = max(1.0, fwidth(dist) * 1.6);

    if (shape <= 0.003) {
        discard;
    }

    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    // Thickness profile of the sheet: thin at the rim, full in the middle.
    float inner = clamp(-dist / max(radius * 1.5 + 4.0, 1.0), 0.0, 1.0);
    float lens = pow(1.0 - inner, 2.4);

    // Matte glass: only a trace of lens bend, so the surface stays calm instead
    // of showing the liquid-glass refraction rings.
    vec2 bend = -normal * lens * (0.7 + radius * 0.04) / ScreenSize;

    // The frost is the point of this style, so it blurs harder than the clear
    // card does. Scaling with the sheet keeps small chips and large panels
    // equally soft.
    float blurPx = clamp(minSide * 0.085, 18.0, 96.0);
    vec3 refracted = blurredBackdrop(screenUv + bend, blurPx);

    // Dark milk: a graphite pane that lets the blurred scene glow through at a
    // little under half strength. No white veil and no grey lift, unlike the
    // liquid shell, so the sheet reads as smoked glass rather than a lens.
    vec3 frostBase = vec3(0.070, 0.074, 0.086);
    vec3 bodyTint = mix(vec3(1.0), tint.rgb, 0.06);
    vec3 color = mix(refracted, refracted * bodyTint, 0.15);
    color = mix(color, frostBase, 0.58);
    // Very gentle top-lit grade; the style is deliberately matte.
    color *= mix(1.02, 0.94, localCoord.y);
    // A bright sky behind the pane must not turn it into a grey plate: the
    // darker the body has to stay, the more the scene is pulled back.
    float sceneLum = dot(refracted, vec3(0.2126, 0.7152, 0.0722));
    color = mix(color, frostBase, smoothstep(0.35, 0.95, sceneLum) * 0.35);

    // No specular glint: the only light on the surface is the hairline rim.
    // Border: tint.a carries its strength, drawn from the same SDF as the sheet
    // so it stays exactly one pixel wide and smooth at every scale.
    float borderAlpha = clamp(tint.a, 0.0, 1.0);
    if (borderAlpha > 0.002) {
        float band = 1.0 - smoothstep(borderWidth - 0.75, borderWidth + 0.75, -dist);
        vec3 borderColor = mix(vec3(1.0), tint.rgb, 0.18);
        borderColor *= mix(1.0, 0.90, smoothstep(0.15, 0.85, localCoord.y));
        color = mix(color, borderColor, band * borderAlpha);
    }

    float opacity = clamp(panel.y, 0.0, 1.0);

    // 8-bit targets band badly on these very dark gradients; a sub-LSB dither
    // removes the stepping without being visible.
    float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5;
    color += dither / 255.0;

    fragColor = vec4(color, opacity * shape) * ColorModulator;
}
