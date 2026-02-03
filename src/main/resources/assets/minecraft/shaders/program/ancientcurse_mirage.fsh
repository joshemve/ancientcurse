#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;
uniform float Time;
uniform float Intensity;

out vec4 fragColor;

void main() {
    // Early exit when effect is inactive (zero cost)
    if (Intensity < 0.001) {
        fragColor = texture(DiffuseSampler, texCoord);
        return;
    }

    vec2 uv = texCoord;

    // === HEIGHT-BASED INTENSITY GRADIENT ===
    // Strongest near bottom of screen (horizon/ground), fades toward top (sky)
    float heightFactor = pow(1.0 - uv.y, 2.5);
    float effectStrength = Intensity * heightFactor;

    // === MULTI-FREQUENCY HEAT SHIMMER ===
    // Three overlapping wave patterns create organic-looking ripple
    float t = Time * 6.28318; // Convert to radians-per-second scale

    // Primary shimmer - large slow waves (dominant vertical displacement)
    float wave1 = sin(uv.x * 15.0 + t * 0.8) * 0.005;
    // Secondary shimmer - medium frequency for detail
    float wave2 = sin(uv.x * 31.0 + t * 1.3 + 1.3) * 0.0025;
    // Tertiary shimmer - high frequency micro-distortion
    float wave3 = sin(uv.x * 7.5 + uv.y * 12.0 + t * 0.5) * 0.003;

    // Horizontal distortion (much subtler than vertical)
    float waveX1 = sin(uv.y * 20.0 + t * 0.6) * 0.0012;
    float waveX2 = sin(uv.y * 40.0 + t * 1.1) * 0.0006;

    vec2 distortion = vec2(
        (waveX1 + waveX2) * effectStrength,
        (wave1 + wave2 + wave3) * effectStrength
    );

    // === CHROMATIC ABERRATION ===
    // RGB channel separation proportional to distortion magnitude
    float chromaticOffset = length(distortion) * 0.6;

    vec2 uvR = uv + distortion + vec2(chromaticOffset, 0.0);
    vec2 uvG = uv + distortion;
    vec2 uvB = uv + distortion - vec2(chromaticOffset, 0.0);

    // Clamp UVs to prevent sampling outside the framebuffer
    uvR = clamp(uvR, vec2(0.0), vec2(1.0));
    uvG = clamp(uvG, vec2(0.0), vec2(1.0));
    uvB = clamp(uvB, vec2(0.0), vec2(1.0));

    float r = texture(DiffuseSampler, uvR).r;
    float g = texture(DiffuseSampler, uvG).g;
    float b = texture(DiffuseSampler, uvB).b;
    vec3 color = vec3(r, g, b);

    // === MIRAGE PUDDLE REFLECTION ===
    // At the very bottom of screen, sample from above to create false water illusion
    float mirageZone = smoothstep(0.85, 1.0, 1.0 - uv.y);
    if (mirageZone > 0.0 && Intensity > 0.3) {
        float mirageIntensity = mirageZone * (Intensity - 0.3) / 0.7;

        // Sample reflected pixels from higher up (flipped vertically with offset)
        vec2 reflectedUV = vec2(uv.x + distortion.x * 3.0, 0.3 + (0.15 - uv.y) * 0.4);
        reflectedUV = clamp(reflectedUV, vec2(0.0), vec2(1.0));
        vec3 reflected = texture(DiffuseSampler, reflectedUV).rgb;

        // Desaturate and brighten the reflection for water-like appearance
        float reflLum = dot(reflected, vec3(0.299, 0.587, 0.114));
        reflected = mix(reflected, vec3(reflLum), 0.3);
        reflected *= 1.15;

        // Blend the mirage reflection
        color = mix(color, reflected, mirageIntensity * 0.35);
    }

    // === WARM COLOR SHIFT ===
    // Subtle push toward warm desert tones
    vec3 warmTint = vec3(1.04, 1.01, 0.96);
    color *= mix(vec3(1.0), warmTint, effectStrength * 0.7);

    // === WARM VIGNETTE ===
    // Heat-tinted darkening at screen edges
    float vignette = distance(uv, vec2(0.5));
    vignette = smoothstep(0.3, 0.9, vignette);
    color = mix(color, color * vec3(1.05, 0.95, 0.85), vignette * effectStrength * 0.3);

    fragColor = vec4(color, 1.0);
}
