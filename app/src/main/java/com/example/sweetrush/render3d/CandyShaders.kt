package com.example.sweetrush.render3d

import android.opengl.GLES30

object CandyShaders {

    // Vertex layout (13 floats = 52 bytes):
    // loc 0: aPosition (vec3)
    // loc 1: aNormal   (vec3)
    // loc 2: aColor    (vec4)
    // loc 3: aTexInfo  (vec3: u, v, matType)
    //   matType:
    //     0.0 = Glossy Candy / Icing
    //     1.0 = Road Asphalt / Sugar Paving
    //     2.0 = Matte Rubber Tire (with procedural tread)
    //     3.0 = Metallic Chrome / Alloy Rim
    //     4.0 = Translucent Gummy / Glass
    //     5.0 = Emissive Light / Neon Sign / Boost Pad
    //     6.0 = Flowing Chocolate / Caramel / Cream Wave
    //     7.0 = Glossy Warm Caramel
    //     8.0 = Baked Wafer Texture
    //     9.0 = Soft Matte Marshmallow / Frosting
    const val LIT_VERTEX_SHADER = """#version 300 es
        layout(location = 0) in vec3 aPosition;
        layout(location = 1) in vec3 aNormal;
        layout(location = 2) in vec4 aColor;
        layout(location = 3) in vec3 aTexInfo;

        uniform mat4 uMvpMatrix;
        uniform mat4 uModelMatrix;
        uniform mat3 uNormalMatrix;

        out vec3 vWorldPos;
        out vec3 vNormal;
        out vec4 vColor;
        out vec3 vTexInfo;

        void main() {
            vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
            vWorldPos = worldPos.xyz;
            vNormal = normalize(uNormalMatrix * aNormal);
            vColor = aColor;
            vTexInfo = aTexInfo;
            gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
        }
    """

    const val LIT_FRAGMENT_SHADER = """#version 300 es
        precision mediump float;

        in vec3 vWorldPos;
        in vec3 vNormal;
        in vec4 vColor;
        in vec3 vTexInfo;

        uniform vec3 uCameraPos;
        uniform vec3 uSunDir;
        uniform vec3 uSunColor;
        uniform vec3 uFogColor;
        uniform float uFogStart;
        uniform float uFogEnd;
        uniform float uGlossiness;
        uniform float uRainbowTime;
        uniform float uTime;
        uniform float uBrakeGlow;
        uniform vec4 uTintColor;

        out vec4 fragColor;

        float hash21(vec2 p) {
            p = fract(p * vec2(123.34, 456.21));
            p += dot(p, p + 45.32);
            return fract(p.x * p.y);
        }

        void main() {
            vec3 N = normalize(vNormal);
            vec3 L = normalize(uSunDir);
            vec3 V = normalize(uCameraPos - vWorldPos);
            float matType = vTexInfo.z;
            vec2 uv = vTexInfo.xy;

            vec3 baseRgb = vColor.rgb * uTintColor.rgb;
            float alpha = vColor.a * uTintColor.a;
            float localGloss = uGlossiness;
            float emissiveBoost = 0.0;

            if (matType > 0.5 && matType < 1.5) {
                // Road asphalt micro-grain + sugar crystal pavement sheen + crisp lane contrast
                float grain = hash21(floor(vWorldPos.xz * 4.5));
                float microBand = sin(vWorldPos.x * 0.45) * cos(vWorldPos.z * 0.45) * 0.04;
                baseRgb *= (0.92 + grain * 0.14 + microBand);
                localGloss = 0.54;
            } else if (matType >= 1.5 && matType < 2.5) {
                // Tire rubber with deep chevron tread grooves & sidewall highlight
                float tread = sin(uv.y * 48.0 + abs(uv.x - 0.5) * 14.0) * 0.5 + 0.5;
                float sideWall = smoothstep(0.18, 0.42, abs(uv.x - 0.5));
                baseRgb *= mix(0.76, 1.12, tread * (1.0 - sideWall * 0.52));
                N = normalize(N + vec3(0.0, (tread - 0.5) * 0.22, 0.0));
                localGloss = 0.24;
            } else if (matType >= 2.5 && matType < 3.5) {
                // Chrome / metallic alloy rim reflection
                localGloss = 0.99;
                float envRef = pow(clamp(reflect(-V, N).y * 0.5 + 0.5, 0.0, 1.0), 1.35);
                baseRgb = mix(baseRgb, vec3(0.98, 0.98, 1.0), envRef * 0.56);
            } else if (matType >= 3.5 && matType < 4.5) {
                // Translucent gummy / crystal fresnel glow
                float fresnel = pow(1.0 - max(dot(N, V), 0.0), 1.9);
                baseRgb = mix(baseRgb, baseRgb * 1.48 + vec3(0.20, 0.14, 0.26), fresnel * 0.72);
                localGloss = 0.96;
            } else if (matType >= 4.5 && matType < 5.5) {
                // Emissive lights & signs
                emissiveBoost = 0.95 + 0.18 * sin(uTime * 4.2 + vWorldPos.x * 0.2);
                if (uv.x > 0.85) {
                    emissiveBoost = mix(0.28, 1.48, uBrakeGlow);
                    baseRgb = mix(baseRgb * 0.54, vec3(1.0, 0.12, 0.20), uBrakeGlow);
                }
            } else if (matType >= 5.5 && matType < 6.5) {
                // Flowing chocolate / caramel / cream water ripples
                float wave = sin(uv.x * 20.0 + uTime * 3.2) * cos(uv.y * 16.0 - uTime * 2.4);
                baseRgb *= (0.92 + wave * 0.15);
                N = normalize(N + vec3(wave * 0.24, 0.0, wave * 0.24));
                localGloss = 0.94;
            } else if (matType >= 6.5 && matType < 7.5) {
                // Glossy warm caramel / sugar-glazed roof coat
                localGloss = 0.96;
                float sheen = pow(1.0 - max(dot(N, V), 0.0), 2.3);
                baseRgb = mix(baseRgb, vec3(1.0, 0.88, 0.52), sheen * 0.34);
            } else if (matType >= 7.5 && matType < 8.5) {
                // Baked wafer grid texture & warm depth
                float gx = abs(sin(vWorldPos.x * 2.4 + vWorldPos.y * 1.6));
                float gz = abs(sin(vWorldPos.z * 2.4));
                float grid = smoothstep(0.15, 0.42, min(gx, gz));
                baseRgb *= mix(0.74, 1.08, grid);
                N = normalize(N + vec3((gx - 0.5) * 0.15, 0.0, (gz - 0.5) * 0.15));
                localGloss = 0.18;
            } else if (matType >= 8.5) {
                // Soft matte marshmallow / whipped frosting
                localGloss = 0.16;
                float wrap = pow(1.0 - max(dot(N, V), 0.0), 1.7) * 0.18;
                baseRgb = mix(baseRgb, vec3(1.0, 0.98, 1.0), wrap);
            } else {
                // Glossy candy / sugar crystal sparkle
                float sparkle = hash21(floor(vWorldPos.xz * 7.5 + vWorldPos.y * 5.5));
                if (sparkle > 0.96) {
                    emissiveBoost = 0.42;
                }
                localGloss = max(localGloss, 0.78);
            }

            vec3 H = normalize(L + V);

            // Crevice / ground-facing ambient occlusion approximation
            float ao = clamp(0.62 + 0.38 * (N.y * 0.5 + 0.5), 0.52, 1.0);

            // Hemisphere candy ambient (pastel sky top + warm cream bounce bottom)
            float hemi = N.y * 0.5 + 0.5;
            vec3 ambient = mix(vec3(0.46, 0.35, 0.52), vec3(0.95, 0.89, 1.0), hemi) * ao;

            // Soft wrapped diffuse lighting + cool opposite fill bounce
            float rawNdotL = dot(N, L);
            float diff = max(rawNdotL * 0.72 + 0.28, 0.0);
            vec3 fillDir = normalize(vec3(-L.x, 0.38, -L.z));
            float fillDiff = max(dot(N, fillDir), 0.0) * 0.26;

            // Subsurface Scattering (SSS) warm candy/skin/frosting translucency
            float backLight = max(0.0, dot(V, -L + N * 0.42));
            float sssStrength = (matType >= 8.5 || (matType >= 3.5 && matType < 4.5) || matType < 0.5) ? 0.34 : 0.14;
            float sss = (pow(backLight, 2.4) * 0.65 + (1.0 - max(rawNdotL, 0.0)) * max(0.0, rawNdotL + 0.55) * 0.35) * sssStrength;
            vec3 sssColor = mix(baseRgb, vec3(1.0, 0.38, 0.54), 0.46) * sss;

            // Dual-lobe candy clearcoat specular (broad glaze + tight sun glint)
            float specPow = mix(18.0, 136.0, localGloss);
            float spec = pow(max(dot(N, H), 0.0), specPow) * (0.22 + 0.86 * localGloss);
            float coatSpec = pow(max(dot(N, H), 0.0), 210.0) * (0.40 * localGloss);

            // Vivid candy rim highlight
            float rim = pow(1.0 - max(dot(N, V), 0.0), 2.6) * (0.36 + 0.14 * max(N.y, 0.0));

            if (uRainbowTime > 0.0) {
                vec3 rainbow = 0.5 + 0.5 * cos(uRainbowTime * 4.5 + vWorldPos.xyz * 0.6 + vec3(0.0, 2.09, 4.18));
                baseRgb = mix(baseRgb, rainbow, 0.58);
            }

            vec3 lit = baseRgb * (ambient * 0.58 + uSunColor * diff * 0.84 + vec3(0.76, 0.86, 1.0) * fillDiff + emissiveBoost)
                     + sssColor
                     + uSunColor * (spec + coatSpec)
                     + vec3(1.0, 0.88, 0.98) * rim;

            // Gentle ACES-like saturation/highlight shoulder roll-off
            lit = (lit * (2.51 * lit + 0.03)) / (lit * (2.43 * lit + 0.59) + 0.14);
            lit = clamp(lit * 1.09, 0.0, 1.0);

            // Distance atmospheric pastel fog with warm sun-aligned inscattering
            float dist = length(uCameraPos - vWorldPos);
            float heightAtten = exp(-max(vWorldPos.y, 0.0) * 0.008);
            float fogFactor = clamp((dist - uFogStart) / max(uFogEnd - uFogStart, 1.0), 0.0, 0.86) * heightAtten;
            float sunAlign = max(dot(normalize(vWorldPos - uCameraPos), L), 0.0);
            vec3 horizonFog = mix(uFogColor, vec3(1.0, 0.89, 0.78), pow(sunAlign, 5.0) * 0.42);
            vec3 finalRgb = mix(lit, horizonFog, fogFactor * fogFactor);

            fragColor = vec4(finalRgb, alpha);
        }
    """

    const val SKY_VERTEX_SHADER = """#version 300 es
        layout(location = 0) in vec2 aPos;
        out vec2 vUv;
        void main() {
            vUv = aPos * 0.5 + 0.5;
            gl_Position = vec4(aPos, 0.999, 1.0);
        }
    """

    const val SKY_FRAGMENT_SHADER = """#version 300 es
        precision mediump float;
        in vec2 vUv;
        uniform vec3 uSkyTop;
        uniform vec3 uSkyBottom;
        uniform float uTime;
        out vec4 fragColor;

        float hash(vec2 p) {
            return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
        }

        float noise(vec2 p) {
            vec2 i = floor(p);
            vec2 f = fract(p);
            vec2 u = f * f * (3.0 - 2.0 * f);
            return mix(
                mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
                mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
                u.y
            );
        }

        float fbm(vec2 p) {
            float v = 0.0;
            float a = 0.5;
            for (int i = 0; i < 4; i++) {
                v += a * noise(p);
                p *= 2.03;
                a *= 0.5;
            }
            return v;
        }

        void main() {
            float t = clamp(vUv.y, 0.0, 1.0);
            vec3 midSky = mix(uSkyBottom, uSkyTop, 0.46) + vec3(0.07, 0.02, 0.06);
            vec3 col = mix(uSkyBottom, midSky, smoothstep(0.0, 0.48, t));
            col = mix(col, uSkyTop, smoothstep(0.42, 0.96, t));

            // Sugar Rush cotton-candy horizon ribbon
            float horizonBand = exp(-abs(t - 0.30) * 8.0);
            col += vec3(0.20, 0.09, 0.14) * horizonBand;

            // Volumetric FBM marshmallow clouds with sunlit tops and berry underbellies
            if (t > 0.20) {
                vec2 cloudUv = vec2(vUv.x * 3.8 + uTime * 0.015, t * 5.2);
                float c1 = fbm(cloudUv);
                float c2 = fbm(cloudUv * 1.35 + vec2(0.0, 0.16));
                float cloudMask = smoothstep(0.49, 0.78, c1) * smoothstep(0.20, 0.48, t) * smoothstep(0.98, 0.68, t);
                float litTop = clamp((c2 - c1) * 3.0 + 0.55, 0.0, 1.0);
                vec3 cloudUnder = mix(uSkyBottom, vec3(0.95, 0.72, 0.88), 0.55);
                vec3 cloudLit = vec3(1.0, 0.97, 0.99);
                col = mix(col, mix(cloudUnder, cloudLit, litTop), cloudMask * 0.62);
            }

            // Warm sun disc, halo & crepuscular rays
            vec2 sunPos = vec2(0.76, 0.76);
            vec2 delta = (vUv - sunPos) * vec2(1.7, 1.0);
            float sunDist = length(delta);
            float sunDisc = smoothstep(0.075, 0.028, sunDist);
            float sunHalo = exp(-sunDist * 4.6) * 0.45;
            float rayAngle = atan(delta.y, delta.x);
            float sunRays = pow(max(0.0, cos(rayAngle * 8.0 + uTime * 0.22)), 6.0) * exp(-sunDist * 5.0) * 0.16;
            col += vec3(1.0, 0.93, 0.78) * (sunDisc * 0.85 + sunHalo + sunRays);

            fragColor = vec4(col, 1.0);
        }
    """

    fun compileProgram(vertSrc: String, fragSrc: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vs)
        GLES30.glAttachShader(prog, fs)
        GLES30.glLinkProgram(prog)
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        return shader
    }
}
