#version 150

// Sun Beam Fragment Shader for Ra's Divine Light Attack
// Creates a glowing, pulsing beam with bloom effect

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform mat4 ProjMat;
uniform vec4 ColorModulator;
uniform float GameTime;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in vec4 vertexColor;
in vec2 texCoord0;
in float vertexDistance;
in vec3 worldPos;

out vec4 fragColor;

// Function to create radial glow falloff
float radialGlow(vec2 uv, float intensity) {
    float dist = abs(uv.x - 0.5) * 2.0;
    float glow = 1.0 - smoothstep(0.0, 1.0, dist);
    return pow(glow, intensity);
}

// Function to create animated energy flow
float energyFlow(vec2 uv, float time) {
    float flow1 = sin(uv.y * 10.0 + time * 3.0) * 0.5 + 0.5;
    float flow2 = sin(uv.y * 7.0 - time * 2.0) * 0.5 + 0.5;
    float flow3 = sin(uv.y * 15.0 + time * 5.0) * 0.5 + 0.5;
    return (flow1 + flow2 + flow3) / 3.0;
}

// Function to create divine sparkle effect
float sparkle(vec2 uv, float time) {
    float spark = sin(uv.x * 50.0 + time * 10.0) * sin(uv.y * 50.0 - time * 8.0);
    spark = max(0.0, spark);
    return pow(spark, 8.0) * 2.0;
}

void main() {
    // Sample base texture
    vec4 texColor = texture(Sampler0, texCoord0);

    // Calculate radial glow (center is brightest)
    float glow = radialGlow(texCoord0, 2.0);

    // Calculate energy flow animation
    float time = GameTime * 1200.0;
    float energy = energyFlow(texCoord0, time);

    // Calculate sparkle effect
    float spark = sparkle(texCoord0, time);

    // Combine base color with vertex color
    vec4 baseColor = texColor * vertexColor * ColorModulator;

    // Apply glow enhancement
    float glowIntensity = glow * (0.8 + 0.2 * energy);

    // Create multi-layer bloom effect
    vec3 bloomColor = baseColor.rgb;

    // Layer 1: Core brightness
    bloomColor += baseColor.rgb * glowIntensity * 0.5;

    // Layer 2: Warm corona
    vec3 warmTint = vec3(1.0, 0.9, 0.7);
    bloomColor += warmTint * glow * 0.3 * energy;

    // Layer 3: Divine sparkles
    vec3 sparkleColor = vec3(1.0, 1.0, 0.95);
    bloomColor += sparkleColor * spark;

    // Layer 4: Edge glow (golden rim)
    float edgeFactor = 1.0 - glow;
    vec3 edgeColor = vec3(1.0, 0.7, 0.2);
    bloomColor += edgeColor * edgeFactor * 0.2 * energy;

    // Calculate final alpha with bloom contribution
    float finalAlpha = baseColor.a * glowIntensity;
    finalAlpha = min(1.0, finalAlpha + spark * 0.5);

    // Construct final color
    vec4 color = vec4(bloomColor, finalAlpha);

    // Apply distance-based intensity (closer = brighter)
    float distanceFade = 1.0 - smoothstep(0.0, 32.0, vertexDistance);
    color.rgb *= 0.7 + 0.3 * distanceFade;

    // Apply fog (but reduce its effect for divine light)
    float fragmentDistance = -ProjMat[3].z / ((gl_FragCoord.z) * -2.0 + 1.0 - ProjMat[2].z);
    vec4 foggedColor = linear_fog(color, fragmentDistance, FogStart, FogEnd, FogColor);

    // Blend between fogged and unfogged (divine light pierces fog)
    fragColor = mix(foggedColor, color, 0.7);
}
