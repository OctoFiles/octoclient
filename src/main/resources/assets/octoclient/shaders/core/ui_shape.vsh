#version 330

#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

out vec2 localCoord;
out vec4 tint;
out vec3 panel;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // UV0 is the 0..1 position inside the quad, which the fragment stage turns
    // back into a signed distance field for the rounded outline.
    localCoord = UV0;
    tint = Color;
    // x = corner radius as a fraction of the short side
    panel = Normal;
}
