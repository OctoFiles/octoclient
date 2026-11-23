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

// Frosted body: two-ring disc blur over the backdrop copy. The radius is
// given in window pixels, so the frost keeps the same strength at every GUI
// scale. Seventeen taps are enough to hide the scene's shapes without the
// sample pattern itself becoming visible.
vec3 blurredBackdrop(vec2 uv, float radiusPx) {
    vec2 disc = vec2(radiusPx) / ScreenSize;
    vec3 acc = texture(Sampler0, clamp(uv, vec2(0.0), vec2(1.0))).rgb * 0.16;
    for (int i = 0; i < 6; i++) {
        float a = float(i) * 1.0471976;
        vec2 off = vec2(cos(a), sin(a)) * disc * 0.45;
        acc += texture(Sampler0, clamp(uv + off, vec2(0.0), vec2(1.0))).rgb * 0.10;
    }
    for (int i = 0; i < 10; i++) {
        float a = float(i) * 3.8832221;
        vec2 off = vec2(cos(a), sin(a)) * disc * 0.9;
        acc += texture(Sampler0, clamp(uv + off, vec2(0.0), vec2(1.0))).rgb * 0.024;
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
    // Border width in physical pixels. Deriving it from the SDF keeps the line
    // exactly one pixel-ish wide and anti-aliased at every GUI scale, which a
    // rectangle fill in virtual coordinates can never be.
    float borderWidth = max(1.0, fwidth(dist) * 1.6);

    if (shape <= 0.003) {
        discard;
    }

    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    // Thickness profile of the sheet: thin at the rim, full in the middle.
    // Light is bent where the thickness gradient is steepest.
    float inner = clamp(-dist / max(radius * 1.5 + 4.0, 1.0), 0.0, 1.0);
    float lens = pow(1.0 - inner, 2.4);

    // Only a hint of lens bend is left: the frost, not the warp, is what sells
    // the sheet. A strong radius here produced a fish-eye edge that read as
    // distortion rather than glass.
    vec2 bend = -normal * lens * (1.6 + radius * 0.10) / ScreenSize;

    // Blur radius scales with the sheet, so a 640px panel and a 150px column
    // frost to the same apparent softness.
    float blurPx = clamp(minSide * 0.050, 12.0, 72.0);
    vec3 refracted = blurredBackdrop(screenUv + bend, blurPx);

    // Smoked body: the backdrop is dimmed the way the scrim dims it, then pulled
    // toward the tint so panels keep their dark identity instead of becoming
    // windows. The vertical grade keeps the top-lit look of the rest of the UI.
    // Values are the web preview's, verbatim except for the world factor: a
    // vertex stream in the GUI pipeline has no spare float channel for the
    // per-panel value (shell 0.18 / search 0.26 / cards 0.30 / status 0.34),
    // so a single constant close to the card value is used.
    float worldFactor = 1.0;
    vec3 bodyTint = mix(vec3(1.0), tint.rgb, 0.08);
    vec3 color = refracted * worldFactor;
    color = mix(color, color * bodyTint, 0.35);
    // Milky veil: the sheet is a frosted pane, not a tinted window, so the
    // blurred scene is lifted toward white instead of being dimmed.
    color = mix(color, vec3(1.0), 0.06);
    color *= mix(0.90, 0.84, localCoord.y);
    // Bright daylight behind the window would otherwise blow the plate out and
    // leave white labels floating on white. Tone the body down as the scene
    // behind it gets brighter, so a panel reads the same over sky and over cave.
    float sceneLum = dot(refracted, vec3(0.2126, 0.7152, 0.0722));
    color *= mix(1.0, 0.40, smoothstep(0.22, 0.72, sceneLum));

    // Specular rim lit from the upper left. This shader works in window
    // coordinates where +y points up, so the light vector has to point up as
    // well — and the highlight must stay welded to the rim, otherwise it turns
    // into a broad white wash across the sheet.
    vec2 lightDir = normalize(vec2(-0.45, 0.89));
    float rim = clamp(1.0 - inner, 0.0, 1.0);
    float spec = pow(max(dot(normal, lightDir), 0.0), 2.0) * rim * rim * rim;
    // panel.z < 0 is the "matte" flag: the sheet keeps its refraction and rim
    // but drops the white specular glint (used by the HUD editor bar).
    float specStrength = panel.z < -0.5 ? 0.0 : (0.30 + panel.z * 0.22);
    color += vec3(1.0) * spec * specStrength;

    // Border: tint.a carries its strength, so the rim is drawn from the same
    // signed distance field as the sheet and stays smooth instead of being
    // quantised to virtual pixels.
    float borderAlpha = clamp(tint.a, 0.0, 1.0);
    if (borderAlpha > 0.002) {
        float band = 1.0 - smoothstep(borderWidth - 0.75, borderWidth + 0.75, -dist);
        // No white in the rim: the border is the panel tint itself, dimmed
        // toward the bottom so it still reads as lit from above.
        vec3 borderColor = mix(vec3(1.0), tint.rgb, 0.15);
        // Top-lit rim: the same hairline reads as light coming from above.
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
