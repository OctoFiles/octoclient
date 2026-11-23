#version 330

#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 localCoord;
in vec4 tint;
in vec3 panel;

out vec4 fragColor;

// Whole-window frost: the backdrop copy is sampled in a two-ring disc, the way
// the Blur mod blurs the already rendered scene. The quad covers the screen
// exactly, so the frozen sheet has square corners — no rounded mask, no inset.
// panel.x = blur radius as a fraction of the window height
// panel.y = darkening amount, 0..1
void main() {
    // ScreenSize is the GUI size, not the size of the frame copy, so the UVs
    // come from the copy itself: gl_FragCoord is in render-target pixels and
    // the backdrop is a full copy of that target.
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 screenUv = gl_FragCoord.xy / texSize;
    vec2 disc = vec2(max(panel.x, 0.0) * texSize.y) / texSize;

    vec3 acc = texture(Sampler0, clamp(screenUv, vec2(0.0), vec2(1.0))).rgb * 0.16;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7853981634;
        vec2 off = vec2(cos(a), sin(a)) * disc * 0.46;
        acc += texture(Sampler0, clamp(screenUv + off, vec2(0.0), vec2(1.0))).rgb * 0.06;
    }
    for (int i = 0; i < 12; i++) {
        float a = float(i) * 0.5235987756 + 0.26;
        vec2 off = vec2(cos(a), sin(a)) * disc * 0.86;
        acc += texture(Sampler0, clamp(screenUv + off, vec2(0.0), vec2(1.0))).rgb * 0.03;
    }

    // Darken just enough that the glass sheets keep their contrast against a
    // bright sky; the frost carries the separation, not the veil.
    float dim = clamp(panel.y, 0.0, 1.0);
    vec3 color = acc * (1.0 - dim * 0.62);

    // Frosted glass, not a blurred window: pull the saturation out and lift the
    // sheet toward a cool milk, so the whole backdrop reads as one pane of
    // etched glass instead of a softened screenshot.
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(luma), color, 0.92);
    color = mix(color, vec3(0.80, 0.82, 0.88), 0.05);

    // 8-bit targets band on these very dark gradients; a sub-LSB dither hides
    // the stepping without being visible.
    float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5;
    color += dither / 255.0;

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0) * ColorModulator;
}
