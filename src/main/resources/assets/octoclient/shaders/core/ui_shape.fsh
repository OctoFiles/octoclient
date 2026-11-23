#version 330

#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in vec2 localCoord;
in vec4 tint;
in vec3 panel;

out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;
}

void main() {
    // Rebuild the quad's pixel size from the derivatives of the 0..1 UV, so the
    // outline is defined in device pixels and does not depend on GUI scale.
    vec2 panelPx = vec2(
        1.0 / max(abs(dFdx(localCoord.x)), 1e-6),
        1.0 / max(abs(dFdy(localCoord.y)), 1e-6));
    float minSide = min(panelPx.x, panelPx.y);
    float radius = clamp(panel.x, 0.0, 0.5) * minSide;

    // Keep the antialiased band inside the quad: the distance is measured to a
    // box half a pixel smaller than the geometry.
    vec2 halfSize = max(panelPx * 0.5 - vec2(0.5), vec2(0.5));
    vec2 point = (localCoord - 0.5) * panelPx;
    float dist = sdRoundBox(point, halfSize, radius);

    float aa = max(fwidth(dist) * 1.1, 0.45);
    float shape = 1.0 - smoothstep(-aa, aa, dist);
    if (shape <= 0.002) {
        discard;
    }

    fragColor = vec4(tint.rgb, tint.a * shape) * ColorModulator;
}
