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
            float diff = max(dot(N, L) * 0.72 + 0.28, 0.0);
            vec3 fillDir = normalize(vec3(-L.x, 0.38, -L.z));
            float fillDiff = max(dot(N, fillDir), 0.0) * 0.24;

            // Dual-lobe candy clearcoat specular (broad glaze + tight sun glint)
            float specPow = mix(16.0, 128.0, localGloss);
            float spec = pow(max(dot(N, H), 0.0), specPow) * (0.20 + 0.84 * localGloss);
            float coatSpec = pow(max(dot(N, H), 0.0), 192.0) * (0.34 * localGloss);

            // Vivid candy rim highlight
            float rim = pow(1.0 - max(dot(N, V), 0.0), 2.5) * 0.34;

            if (uRainbowTime > 0.0) {
                vec3 rainbow = 0.5 + 0.5 * cos(uRainbowTime * 4.5 + vWorldPos.xyz * 0.6 + vec3(0.0, 2.09, 4.18));
                baseRgb = mix(baseRgb, rainbow, 0.58);
            }

            vec3 lit = baseRgb * (ambient * 0.56 + uSunColor * diff * 0.82 + vec3(0.76, 0.86, 1.0) * fillDiff + emissiveBoost)
                     + uSunColor * (spec + coatSpec)
                     + vec3(1.0, 0.88, 0.98) * rim;

            // Gentle ACES-like saturation/highlight shoulder roll-off
            lit = (lit * (2.51 * lit + 0.03)) / (lit * (2.43 * lit + 0.59) + 0.14);
            lit = clamp(lit * 1.08, 0.0, 1.0);

            // Distance atmospheric pastel fog
            float dist = length(uCameraPos - vWorldPos);
            float fogFactor = clamp((dist - uFogStart) / max(uFogEnd - uFogStart, 1.0), 0.0, 0.88);
            vec3 finalRgb = mix(lit, uFogColor, fogFactor * fogFactor);

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
        void main() {
            float t = clamp(vUv.y, 0.0, 1.0);
            vec3 col = mix(uSkyBottom, uSkyTop, pow(t, 0.70));
            // Layered marshmallow clouds moving across horizon
            float c1 = sin(vUv.x * 8.0 + uTime * 0.05) * cos(vUv.y * 12.0 - 1.8) * 0.5 + 0.5;
            float c2 = sin(vUv.x * 15.0 - uTime * 0.08 + 2.0) * cos(vUv.y * 20.0 - 3.5) * 0.5 + 0.5;
            float cloudMask = (smoothstep(0.54, 0.84, c1) * 0.18 + smoothstep(0.60, 0.90, c2) * 0.10) * smoothstep(0.28, 0.72, t);
            col = mix(col, vec3(1.0, 0.97, 0.99), cloudMask);
            // Warm sun disc glow
            float sunDist = length(vUv - vec2(0.76, 0.78));
            col += vec3(1.0, 0.92, 0.75) * smoothstep(0.24, 0.0, sunDist) * 0.35;
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
