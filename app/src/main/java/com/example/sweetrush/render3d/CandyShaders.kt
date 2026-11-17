package com.example.sweetrush.render3d

import android.opengl.GLES30

object CandyShaders {

    const val LIT_VERTEX_SHADER = """#version 300 es
        layout(location = 0) in vec3 aPosition;
        layout(location = 1) in vec3 aNormal;
        layout(location = 2) in vec4 aColor;

        uniform mat4 uMvpMatrix;
        uniform mat4 uModelMatrix;
        uniform mat3 uNormalMatrix;

        out vec3 vWorldPos;
        out vec3 vNormal;
        out vec4 vColor;

        void main() {
            vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
            vWorldPos = worldPos.xyz;
            vNormal = normalize(uNormalMatrix * aNormal);
            vColor = aColor;
            gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
        }
    """

    const val LIT_FRAGMENT_SHADER = """#version 300 es
        precision mediump float;

        in vec3 vWorldPos;
        in vec3 vNormal;
        in vec4 vColor;

        uniform vec3 uCameraPos;
        uniform vec3 uSunDir;
        uniform vec3 uFogColor;
        uniform float uFogStart;
        uniform float uFogEnd;
        uniform float uGlossiness;
        uniform float uRainbowTime; // > 0 adds rainbow star shimmer
        uniform vec4 uTintColor;    // multiplier tint (1,1,1,1 normally)

        out vec4 fragColor;

        void main() {
            vec3 N = normalize(vNormal);
            vec3 L = normalize(uSunDir);
            vec3 V = normalize(uCameraPos - vWorldPos);
            vec3 H = normalize(L + V);

            // Hemisphere candy ambient (pastel sky top + warm cream bounce bottom)
            float hemi = N.y * 0.5 + 0.5;
            vec3 ambient = mix(vec3(0.42, 0.30, 0.45), vec3(0.78, 0.72, 0.85), hemi);

            // Soft wrapped diffuse lighting for appetizing candy shading
            float diff = max(dot(N, L) * 0.65 + 0.35, 0.0);

            // Glossy sugar-glaze specular highlight
            float specPow = mix(16.0, 72.0, uGlossiness);
            float spec = pow(max(dot(N, H), 0.0), specPow) * (0.28 + 0.55 * uGlossiness);

            // Candy rim highlight
            float rim = pow(1.0 - max(dot(N, V), 0.0), 3.0) * 0.22;

            vec3 baseRgb = vColor.rgb * uTintColor.rgb;

            if (uRainbowTime > 0.0) {
                vec3 rainbow = 0.5 + 0.5 * cos(uRainbowTime * 4.0 + vWorldPos.xyz * 0.6 + vec3(0.0, 2.09, 4.18));
                baseRgb = mix(baseRgb, rainbow, 0.55);
            }

            vec3 lit = baseRgb * (ambient * 0.55 + vec3(0.95, 0.92, 0.90) * diff * 0.72)
                     + vec3(1.0, 0.98, 0.99) * spec
                     + vec3(1.0, 0.82, 0.94) * rim;

            // Distance atmospheric pastel fog
            float dist = length(uCameraPos - vWorldPos);
            float fogFactor = clamp((dist - uFogStart) / max(uFogEnd - uFogStart, 1.0), 0.0, 1.0);
            vec3 finalRgb = mix(lit, uFogColor, fogFactor * fogFactor);

            fragColor = vec4(finalRgb, vColor.a * uTintColor.a);
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
            vec3 col = mix(uSkyBottom, uSkyTop, pow(t, 0.72));
            // Subtle drifting candy cloud bands
            float cloud = sin(vUv.x * 9.0 + uTime * 0.08) * cos(vUv.y * 14.0 - 2.0) * 0.5 + 0.5;
            float cloudMask = smoothstep(0.55, 0.85, cloud) * smoothstep(0.35, 0.75, t) * 0.16;
            col = mix(col, vec3(1.0, 0.97, 0.99), cloudMask);
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
