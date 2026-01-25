#version 150

// Sun Beam Vertex Shader for Ra's Divine Light Attack
// Creates a vertically slicing beam of divine sunlight

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float GameTime;

out vec4 vertexColor;
out vec2 texCoord0;
out float vertexDistance;
out vec3 worldPos;

void main() {
    // Calculate world-space position for effects
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    // Pass distance for fog/glow falloff
    vertexDistance = length(viewPos.xyz);

    // Store world position for fragment shader effects
    worldPos = Position;

    // Pass through color with pulsing intensity based on time
    float pulse = 1.0 + 0.15 * sin(GameTime * 2400.0); // ~60 cycles per second
    vertexColor = Color * vec4(pulse, pulse, pulse, 1.0);

    // Animate texture coordinates for flowing energy effect
    texCoord0 = UV0 + vec2(0.0, GameTime * 20.0);
}
