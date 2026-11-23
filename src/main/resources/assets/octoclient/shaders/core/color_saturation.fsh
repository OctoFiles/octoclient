#version 330

#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 localCoord;
in vec4 tint;
in vec3 panel;

out vec4 fragColor;

void main() {
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;
    vec3 source = texture(Sampler0, clamp(screenUv, vec2(0.0), vec2(1.0))).rgb;
    float hue = panel.x * 6.28318530718;
    vec3 axis = normalize(vec3(1.0));
    source = source * cos(hue) + cross(axis, source) * sin(hue)
        + axis * dot(axis, source) * (1.0 - cos(hue));
    float luminance = dot(source, vec3(0.2126, 0.7152, 0.0722));
    vec3 result = mix(vec3(luminance), source, clamp(panel.y, 0.0, 2.0));
    result *= max(panel.z, 0.0);
    result = (result - 0.5) * clamp(tint.a, 0.0, 2.0) + 0.5;
    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0) * ColorModulator;
}
