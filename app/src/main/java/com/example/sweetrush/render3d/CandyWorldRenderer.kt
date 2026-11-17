package com.example.sweetrush.render3d

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.GraphicsQuality
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.PowerUpType
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.physics.KartPhysicsEngine
import com.example.sweetrush.physics.KartRuntimeState
import com.example.sweetrush.physics.PlayerInputState
import com.example.sweetrush.physics.RaceEvent
import com.example.sweetrush.track.DecoType
import com.example.sweetrush.track.GeneratedTrack
import com.example.sweetrush.track.TrackGenerator
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class RenderSceneMode {
    MENU_KINGDOM,
    GARAGE_SHOWROOM,
    RACE
}

data class Particle3D(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var life: Float = 0f,
    var maxLife: Float = 1f,
    var size: Float = 0.3f,
    var r: Float = 1f,
    var g: Float = 0.4f,
    var b: Float = 0.7f
)

data class RacerMapDot(
    val x: Float,
    val z: Float,
    val isPlayer: Boolean,
    val colorHex: Long
)

data class RaceHudSnapshot(
    val position: Int = 8,
    val currentLap: Int = 1,
    val totalLaps: Int = 3,
    val elapsedMs: Long = 0L,
    val bestLapMs: Long = Long.MAX_VALUE,
    val speedKmh: Int = 0,
    val nitroRatio: Float = 0.55f,
    val driftCharge: Float = 0f,
    val isDrifting: Boolean = false,
    val heldPowerUp: PowerUpType? = null,
    val activeShield: Boolean = false,
    val activeStar: Boolean = false,
    val smokeAlpha: Float = 0f,
    val countdownText: String = "",
    val raceStarted: Boolean = false,
    val finished: Boolean = false,
    val mapDots: List<RacerMapDot> = emptyList(),
    val trackMinX: Float = -100f,
    val trackMaxX: Float = 250f,
    val trackMinZ: Float = -240f,
    val trackMaxZ: Float = 240f,
    val trackOutline: List<Pair<Float, Float>> = emptyList()
)

class CandyWorldRenderer(
    private val onRaceStep: (RaceHudSnapshot, List<RaceEvent>) -> Unit
) : GLSurfaceView.Renderer {

    @Volatile var sceneMode: RenderSceneMode = RenderSceneMode.MENU_KINGDOM
    @Volatile var graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH
    @Volatile var cameraSensitivity: Float = 1.0f
    @Volatile var targetFps: Int = 60
    @Volatile var isPaused: Boolean = false

    // Showroom state
    @Volatile var showroomKartId: String = "strawberry_rocket"
    @Volatile var showroomRacerId: String = "berry_blitz"
    @Volatile var showroomCustom: KartCustomization = KartCustomization()
    @Volatile var showroomOrbitYaw: Float = 0.65f
    @Volatile var showroomOrbitPitch: Float = 0.28f
    @Volatile var showroomAutoRotate: Boolean = true

    // Active race engine & input
    @Volatile var physicsEngine: KartPhysicsEngine? = null
    @Volatile var currentInput: PlayerInputState = PlayerInputState()

    // GL programs & uniforms
    private var litProgram = 0
    private var skyProgram = 0
    private var skyVao = 0

    private var uMvp = 0
    private var uModel = 0
    private var uNormalMat = 0
    private var uCamPos = 0
    private var uSunDir = 0
    private var uFogCol = 0
    private var uFogStart = 0
    private var uFogEnd = 0
    private var uGloss = 0
    private var uRainbow = 0
    private var uTint = 0

    private var uSkyTop = 0
    private var uSkyBottom = 0
    private var uSkyTime = 0

    // Cached Track meshes
    private var loadedTrackId: TrackId? = null
    private var loadedTrack: GeneratedTrack? = null
    private var trackMesh: GpuMesh? = null
    private var groundMesh: GpuMesh? = null
    private val decoMeshCache = HashMap<String, GpuMesh>()

    // Shared gameplay meshes
    private var boostPadMesh: GpuMesh? = null
    private var jumpRampMesh: GpuMesh? = null
    private var caramelHazardMesh: GpuMesh? = null
    private var peppermintHazardMesh: GpuMesh? = null
    private var itemBoxMesh: GpuMesh? = null
    private var shieldMesh: GpuMesh? = null
    private var cubeParticleMesh: GpuMesh? = null
    private var pedestalMesh: GpuMesh? = null

    // Kart & Wheel mesh cache
    private val kartMeshCache = HashMap<String, GpuMesh>()
    private val wheelMeshCache = HashMap<String, GpuMesh>()

    // Matrices
    private val projMat = FloatArray(16)
    private val viewMat = FloatArray(16)
    private val vpMat = FloatArray(16)
    private val modelMat = FloatArray(16)
    private val mvpMat = FloatArray(16)
    private val normalMat = FloatArray(9)

    private var aspectRatio = 16f / 9f
    private var lastFrameNs = System.nanoTime()
    private var totalTimeSec = 0f

    // Smooth camera state
    private var camX = 0f; private var camY = 6f; private var camZ = -12f
    private var camTargetX = 0f; private var camTargetY = 2f; private var camTargetZ = 0f
    private var currentFov = 58f

    // Particle pool
    private val particles = Array(180) { Particle3D() }
    private var particleCursor = 0
    private val rng = Random(1337)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        litProgram = CandyShaders.compileProgram(CandyShaders.LIT_VERTEX_SHADER, CandyShaders.LIT_FRAGMENT_SHADER)
        skyProgram = CandyShaders.compileProgram(CandyShaders.SKY_VERTEX_SHADER, CandyShaders.SKY_FRAGMENT_SHADER)

        uMvp = GLES30.glGetUniformLocation(litProgram, "uMvpMatrix")
        uModel = GLES30.glGetUniformLocation(litProgram, "uModelMatrix")
        uNormalMat = GLES30.glGetUniformLocation(litProgram, "uNormalMatrix")
        uCamPos = GLES30.glGetUniformLocation(litProgram, "uCameraPos")
        uSunDir = GLES30.glGetUniformLocation(litProgram, "uSunDir")
        uFogCol = GLES30.glGetUniformLocation(litProgram, "uFogColor")
        uFogStart = GLES30.glGetUniformLocation(litProgram, "uFogStart")
        uFogEnd = GLES30.glGetUniformLocation(litProgram, "uFogEnd")
        uGloss = GLES30.glGetUniformLocation(litProgram, "uGlossiness")
        uRainbow = GLES30.glGetUniformLocation(litProgram, "uRainbowTime")
        uTint = GLES30.glGetUniformLocation(litProgram, "uTintColor")

        uSkyTop = GLES30.glGetUniformLocation(skyProgram, "uSkyTop")
        uSkyBottom = GLES30.glGetUniformLocation(skyProgram, "uSkyBottom")
        uSkyTime = GLES30.glGetUniformLocation(skyProgram, "uTime")

        initSkyQuad()
        initSharedMeshes()

        // Clear caches on GL context recreation
        loadedTrackId = null
        trackMesh = null
        groundMesh = null
        decoMeshCache.clear()
        kartMeshCache.clear()
        wheelMeshCache.clear()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        aspectRatio = width.toFloat() / height.coerceAtLeast(1).toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastFrameNs) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
        lastFrameNs = nowNs
        totalTimeSec += dt

        // Frame limiter for 30fps battery mode
        if (targetFps <= 35) {
            try { Thread.sleep(16) } catch (_: InterruptedException) {}
        }

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        when (sceneMode) {
            RenderSceneMode.MENU_KINGDOM -> drawMenuKingdom(dt)
            RenderSceneMode.GARAGE_SHOWROOM -> drawGarageShowroom(dt)
            RenderSceneMode.RACE -> drawRace(dt)
        }
    }

    private fun ensureTrackLoaded(trackId: TrackId) {
        if (loadedTrackId == trackId && trackMesh != null) return
        trackMesh?.release()
        groundMesh?.release()
        decoMeshCache.values.forEach { it.release() }
        decoMeshCache.clear()

        val track = TrackGenerator.getTrack(trackId)
        loadedTrack = track
        loadedTrackId = trackId
        trackMesh = MeshFactory.buildTrackMesh(track)
        groundMesh = MeshFactory.buildGroundMesh(trackId)
    }

    private fun drawSky(topHex: Long, botHex: Long) {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glUseProgram(skyProgram)
        val (tr, tg, tb) = MeshAccumulator.hexToRgb(topHex)
        val (br, bg, bb) = MeshAccumulator.hexToRgb(botHex)
        GLES30.glUniform3f(uSkyTop, tr, tg, tb)
        GLES30.glUniform3f(uSkyBottom, br, bg, bb)
        GLES30.glUniform1f(uSkyTime, totalTimeSec)
        GLES30.glBindVertexArray(skyVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)
        GLES30.glBindVertexArray(0)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    private fun drawMenuKingdom(dt: Float) {
        ensureTrackLoaded(TrackId.PEPPERMINT_PLAZA)
        val track = loadedTrack ?: return
        val spec = CatalogData.trackById(TrackId.PEPPERMINT_PLAZA)

        drawSky(spec.skyTopHex, spec.skyBottomHex)

        // Scenic flyover orbit around Peppermint Plaza + hero kart cruising!
        val flyProgress = (totalTimeSec * 0.03f) % 1.0f
        val nodeIdx = ((flyProgress * track.nodes.size).toInt()) % track.nodes.size
        val node = track.nodes[nodeIdx]
        val nextNode = track.nodes[(nodeIdx + 8) % track.nodes.size]

        camX = node.center.x - node.forward.x * 16f + node.right.x * 8f
        camY = node.center.y + 11f + sin(totalTimeSec * 0.6f) * 1.5f
        camZ = node.center.z - node.forward.z * 16f + node.right.z * 8f

        Matrix.perspectiveM(projMat, 0, 56f, aspectRatio, 0.5f, 450f)
        Matrix.setLookAtM(
            viewMat, 0,
            camX, camY, camZ,
            nextNode.center.x, nextNode.center.y + 2.5f, nextNode.center.z,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMat, 0, projMat, 0, viewMat, 0)

        setupLitProgram(spec.fogHex, 140f, 360f)

        // Ground & Track
        drawIdentityMesh(groundMesh, gloss = 0.25f)
        drawIdentityMesh(trackMesh, gloss = 0.65f)
        drawTrackDecorations(track, camX, camZ)

        // Showcase hero kart cruising along the track in background!
        val kartSpec = CatalogData.kartById(showroomKartId)
        val racerSpec = CatalogData.racerById(showroomRacerId)
        val yaw = kotlin.math.atan2(node.forward.x, node.forward.z)
        drawKartModel(
            kart = kartSpec,
            racer = racerSpec,
            custom = showroomCustom,
            x = node.center.x,
            y = node.center.y,
            z = node.center.z,
            yawRad = yaw,
            rollRad = 0f,
            pitchRad = 0f,
            wheelSpin = totalTimeSec * 14f,
            steerVis = 0f,
            shieldActive = false,
            starActive = false
        )

        // Ambient floating sugar sparkles
        if (rng.nextFloat() < 0.35f) {
            spawnParticle(
                camX + (rng.nextFloat() - 0.5f) * 35f,
                camY + (rng.nextFloat() - 0.5f) * 10f,
                camZ + (rng.nextFloat() - 0.5f) * 35f,
                0f, 1.2f, 0f, 2.2f, 0.26f,
                1f, 0.7f + rng.nextFloat() * 0.3f, 0.9f
            )
        }
        updateAndDrawParticles(dt)
    }

    private fun drawGarageShowroom(dt: Float) {
        drawSky(0xFF421766, 0xFFFF88C2)

        if (showroomAutoRotate) {
            showroomOrbitYaw += dt * 0.45f
        }
        val dist = 5.4f
        val pitch = showroomOrbitPitch.coerceIn(0.08f, 0.75f)
        camX = sin(showroomOrbitYaw) * cos(pitch) * dist
        camY = 1.25f + sin(pitch) * dist
        camZ = cos(showroomOrbitYaw) * cos(pitch) * dist

        Matrix.perspectiveM(projMat, 0, 46f, aspectRatio, 0.3f, 80f)
        Matrix.setLookAtM(viewMat, 0, camX, camY, camZ, 0f, 0.9f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMat, 0, projMat, 0, viewMat, 0)

        setupLitProgram(0xFF421766, 30f, 70f)

        // Draw turntable pedestal
        Matrix.setIdentityM(modelMat, 0)
        Matrix.translateM(modelMat, 0, 0f, -0.35f, 0f)
        drawMesh(pedestalMesh, modelMat, gloss = 0.85f)

        val kartSpec = CatalogData.kartById(showroomKartId)
        val racerSpec = CatalogData.racerById(showroomRacerId)

        drawKartModel(
            kart = kartSpec,
            racer = racerSpec,
            custom = showroomCustom,
            x = 0f,
            y = sin(totalTimeSec * 2.2f) * 0.03f,
            z = 0f,
            yawRad = 0f,
            rollRad = 0f,
            pitchRad = 0f,
            wheelSpin = totalTimeSec * 1.2f,
            steerVis = sin(totalTimeSec * 1.4f) * 0.18f,
            shieldActive = false,
            starActive = false
        )

        // Exhaust showcase sparkles
        if (rng.nextFloat() < 0.4f) {
            val (er, eg, eb) = MeshAccumulator.hexToRgb(showroomCustom.exhaustStyle.flameHex)
            spawnParticle(
                (rng.nextFloat() - 0.5f) * 0.8f,
                0.65f,
                -1.65f,
                (rng.nextFloat() - 0.5f) * 0.6f,
                0.4f + rng.nextFloat() * 0.6f,
                -1.8f,
                0.7f, 0.18f, er, eg, eb
            )
        }
        updateAndDrawParticles(dt)
    }

    private fun drawRace(dt: Float) {
        val engine = physicsEngine ?: return
        val events = if (!isPaused) engine.step(dt, currentInput) else emptyList()

        val track = engine.track
        ensureTrackLoaded(engine.trackId)
        val spec = engine.trackSpec

        drawSky(spec.skyTopHex, spec.skyBottomHex)

        val p = engine.playerKart
        // Dynamic third-person racing camera
        if (p.finished) {
            // Sweeping victory orbit camera around player's kart!
            val orbitA = totalTimeSec * 0.8f
            camX = p.x + sin(orbitA) * 8.5f
            camY = p.y + 4.2f
            camZ = p.z + cos(orbitA) * 8.5f
            camTargetX = p.x; camTargetY = p.y + 1.2f; camTargetZ = p.z
            currentFov += (54f - currentFov) * 4f * dt
        } else {
            val speedNorm = (p.speed / 65f).coerceIn(0f, 1.2f)
            val targetFov = 58f + speedNorm * 12f + (if (p.boostTimer > 0f || p.isNitroActive) 6f else 0f)
            currentFov += (targetFov - currentFov) * 6f * dt

            val driftCamSwing = if (p.isDrifting) p.driftDir * 0.18f else 0f
            val camYaw = p.yawRad + driftCamSwing
            val followDist = 7.4f + speedNorm * 1.4f
            val desiredCamX = p.x - sin(camYaw) * followDist
            val desiredCamY = p.y + 3.65f - (if (!p.isGrounded) 0.4f else 0f)
            val desiredCamZ = p.z - cos(camYaw) * followDist

            val followLerp = (dt * (10f * cameraSensitivity.coerceIn(0.5f, 1.6f))).coerceIn(0.05f, 0.95f)
            camX += (desiredCamX - camX) * followLerp
            camY += (desiredCamY - camY) * followLerp
            camZ += (desiredCamZ - camZ) * followLerp

            val shakeAmt = engine.cameraShake * 0.35f
            val shakeX = if (shakeAmt > 0.01f) (rng.nextFloat() - 0.5f) * shakeAmt else 0f
            val shakeY = if (shakeAmt > 0.01f) (rng.nextFloat() - 0.5f) * shakeAmt else 0f

            camTargetX = p.x + sin(p.yawRad) * 5.0f + shakeX
            camTargetY = p.y + 1.45f + shakeY
            camTargetZ = p.z + cos(p.yawRad) * 5.0f
        }

        Matrix.perspectiveM(projMat, 0, currentFov, aspectRatio, 0.4f, 420f)
        Matrix.setLookAtM(viewMat, 0, camX, camY, camZ, camTargetX, camTargetY, camTargetZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMat, 0, projMat, 0, viewMat, 0)

        setupLitProgram(spec.fogHex, 130f, 340f)

        // Ground & Track
        drawIdentityMesh(groundMesh, gloss = 0.25f)
        drawIdentityMesh(trackMesh, gloss = 0.65f)

        // Boost Pads, Ramps, Hazards
        for (pad in track.boostPads) {
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, pad.pos.x, pad.pos.y, pad.pos.z)
            Matrix.rotateM(modelMat, 0, Math.toDegrees(pad.yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
            drawMesh(boostPadMesh, modelMat, gloss = 0.9f)
        }
        for (ramp in track.jumpRamps) {
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, ramp.pos.x, ramp.pos.y, ramp.pos.z)
            Matrix.rotateM(modelMat, 0, Math.toDegrees(ramp.yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
            drawMesh(jumpRampMesh, modelMat, gloss = 0.75f)
        }
        for (h in track.hazards) {
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, h.pos.x, h.pos.y, h.pos.z)
            drawMesh(if (h.isCaramel) caramelHazardMesh else peppermintHazardMesh, modelMat, gloss = 0.95f)
        }
        for (dh in engine.droppedHazards) {
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, dh.pos.x, dh.pos.y, dh.pos.z)
            Matrix.rotateM(modelMat, 0, totalTimeSec * 120f, 0f, 1f, 0f)
            drawMesh(
                if (dh.type == PowerUpType.CARAMEL_TRAP) caramelHazardMesh else peppermintHazardMesh,
                modelMat,
                gloss = 0.9f
            )
        }

        // Item Boxes
        for (box in engine.itemBoxes) {
            if (box.respawnTimer > 0f) continue
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, box.pos.x, box.pos.y + sin(totalTimeSec * 3.5f + box.id) * 0.25f, box.pos.z)
            Matrix.rotateM(modelMat, 0, totalTimeSec * 90f + box.id * 25f, 0f, 1f, 0f)
            Matrix.rotateM(modelMat, 0, 18f, 1f, 0f, 0f)
            drawMesh(itemBoxMesh, modelMat, gloss = 0.95f)
        }

        // Candy Environment Decorations
        drawTrackDecorations(track, camX, camZ)

        // All 8 Karts + Particles
        for (k in engine.karts) {
            val spinExtra = if (k.spinoutTimer > 0f) k.spinoutTimer * 12f else 0f
            drawKartModel(
                kart = k.kart,
                racer = k.racer,
                custom = k.customization,
                x = k.x,
                y = k.y + k.suspensionOffset,
                z = k.z,
                yawRad = k.yawRad + spinExtra,
                rollRad = k.bodyRollRad,
                pitchRad = k.bodyPitchRad,
                wheelSpin = k.wheelSpinRad,
                steerVis = k.steerVisualRad,
                shieldActive = k.shieldTimer > 0f,
                starActive = k.starTimer > 0f
            )

            // Spawn kart nitro / drift / fireworks particles
            if (!isPaused) {
                emitKartParticles(k, engine.playerFinished)
            }
        }

        if (!isPaused) {
            updateAndDrawParticles(dt)
        }

        // Emit HUD snapshot back to Compose
        val snap = RaceHudSnapshot(
            position = p.position,
            currentLap = p.currentLap.coerceAtMost(engine.totalLaps),
            totalLaps = engine.totalLaps,
            elapsedMs = engine.raceElapsedMs,
            bestLapMs = p.bestLapMs,
            speedKmh = (kotlin.math.abs(p.speed) * 3.1f).toInt(),
            nitroRatio = p.nitroFuel,
            driftCharge = p.driftCharge,
            isDrifting = p.isDrifting,
            heldPowerUp = p.heldPowerUp,
            activeShield = p.shieldTimer > 0f,
            activeStar = p.starTimer > 0f,
            smokeAlpha = (p.smokeTimer / 3.2f).coerceIn(0f, 0.85f),
            countdownText = if (!engine.raceStarted) {
                val c = kotlin.math.ceil(engine.countdownSeconds.toDouble()).toInt().coerceIn(1, 3)
                c.toString()
            } else if (engine.raceElapsedMs < 1100L && engine.mode != com.example.sweetrush.model.GameMode.PRACTICE) {
                "SWEET RUSH!"
            } else "",
            raceStarted = engine.raceStarted,
            finished = engine.playerFinished,
            mapDots = engine.karts.map { RacerMapDot(it.x, it.z, it.isPlayer, it.racer.suitPrimaryHex) },
            trackMinX = track.minX,
            trackMaxX = track.maxX,
            trackMinZ = track.minZ,
            trackMaxZ = track.maxZ,
            trackOutline = track.nodes.filterIndexed { idx, _ -> idx % 3 == 0 }.map { it.center.x to it.center.z }
        )
        onRaceStep(snap, events)
    }

    private fun drawTrackDecorations(track: GeneratedTrack, refX: Float, refZ: Float) {
        val quality = graphicsQuality
        val maxDist = when (quality) {
            GraphicsQuality.LOW -> 185f
            GraphicsQuality.MEDIUM -> 265f
            GraphicsQuality.HIGH -> 370f
        }
        val maxDistSq = maxDist * maxDist
        val stride = if (quality == GraphicsQuality.LOW) 2 else 1

        for (i in track.decorations.indices step stride) {
            val d = track.decorations[i]
            val dx = d.pos.x - refX
            val dz = d.pos.z - refZ
            if (dx * dx + dz * dz > maxDistSq) continue

            val key = "${d.type.name}_${d.primaryColor}_${d.secondaryColor}"
            val mesh = decoMeshCache.getOrPut(key) {
                MeshFactory.buildDecorationMesh(d.type, d.primaryColor, d.secondaryColor)
            }

            val bobY = if (d.type == DecoType.FLOATING_BALLOON) sin(totalTimeSec * 1.7f + d.bobPhase) * 1.1f else 0f
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, d.pos.x, d.pos.y + bobY, d.pos.z)
            Matrix.rotateM(modelMat, 0, Math.toDegrees(d.rotY.toDouble()).toFloat(), 0f, 1f, 0f)
            Matrix.scaleM(modelMat, 0, d.scale, d.scale, d.scale)
            drawMesh(mesh, modelMat, gloss = 0.72f)
        }
    }

    private fun drawKartModel(
        kart: com.example.sweetrush.model.KartSpec,
        racer: com.example.sweetrush.model.RacerSpec,
        custom: KartCustomization,
        x: Float, y: Float, z: Float,
        yawRad: Float, rollRad: Float, pitchRad: Float,
        wheelSpin: Float, steerVis: Float,
        shieldActive: Boolean, starActive: Boolean
    ) {
        val bodyKey = "${kart.id}_${racer.id}_${custom.paintId}_${custom.exhaustStyle.name}_${custom.decoration.name}"
        val bodyMesh = kartMeshCache.getOrPut(bodyKey) {
            MeshFactory.buildKartAndRacerMesh(kart, racer, custom)
        }
        val wheelMesh = wheelMeshCache.getOrPut(custom.wheelDesign.name) {
            MeshFactory.buildWheelMesh(custom.wheelDesign)
        }

        val rainbowVal = if (starActive) totalTimeSec else 0f
        GLES30.glUniform1f(uRainbow, rainbowVal)

        // Draw Kart Body + Seated Racer
        Matrix.setIdentityM(modelMat, 0)
        Matrix.translateM(modelMat, 0, x, y, z)
        Matrix.rotateM(modelMat, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMat, 0, Math.toDegrees(rollRad.toDouble()).toFloat(), 0f, 0f, 1f)
        Matrix.rotateM(modelMat, 0, Math.toDegrees(pitchRad.toDouble()).toFloat(), 1f, 0f, 0f)
        drawMesh(bodyMesh, modelMat, gloss = 0.88f)

        // Draw 4 Visible Rotating & Steerable Wheels
        val wheelOffsets = listOf(
            Triple(-0.88f, 0.44f, 0.92f) to true,   // Front Left
            Triple(0.88f, 0.44f, 0.92f) to true,    // Front Right
            Triple(-0.92f, 0.44f, -0.92f) to false, // Rear Left
            Triple(0.92f, 0.44f, -0.92f) to false   // Rear Right
        )
        val spinDeg = Math.toDegrees(wheelSpin.toDouble()).toFloat()
        val steerDeg = Math.toDegrees(steerVis.toDouble()).toFloat()

        for ((pos, isFront) in wheelOffsets) {
            val wMat = FloatArray(16)
            Matrix.translateM(wMat, 0, modelMat, 0, pos.first, pos.second, pos.third)
            if (isFront) {
                Matrix.rotateM(wMat, 0, steerDeg, 0f, 1f, 0f)
            }
            Matrix.rotateM(wMat, 0, spinDeg, 1f, 0f, 0f)
            drawMesh(wheelMesh, wMat, gloss = 0.7f)
        }

        GLES30.glUniform1f(uRainbow, 0f)

        // Translucent Gummy Shield Bubble
        if (shieldActive) {
            val sMat = FloatArray(16)
            Matrix.setIdentityM(sMat, 0)
            Matrix.translateM(sMat, 0, x, y + 1.0f, z)
            val pulse = 1.95f + sin(totalTimeSec * 6f) * 0.08f
            Matrix.scaleM(sMat, 0, pulse, pulse * 0.78f, pulse * 1.15f)
            GLES30.glDepthMask(false)
            drawMesh(shieldMesh, sMat, gloss = 0.98f)
            GLES30.glDepthMask(true)
        }
    }

    private fun emitKartParticles(k: KartRuntimeState, playerFinished: Boolean) {
        val density = graphicsQuality.particleMultiplier
        if (rng.nextFloat() > density) return

        // Nitro / Boost flame particles
        if (k.isNitroActive || k.boostTimer > 0f || k.starTimer > 0f) {
            val (er, eg, eb) = if (k.starTimer > 0f) {
                Triple(rng.nextFloat(), rng.nextFloat(), 1f)
            } else {
                MeshAccumulator.hexToRgb(k.customization.exhaustStyle.flameHex)
            }
            val backX = k.x - sin(k.yawRad) * 1.6f
            val backZ = k.z - cos(k.yawRad) * 1.6f
            spawnParticle(
                backX + (rng.nextFloat() - 0.5f) * 0.5f,
                k.y + 0.65f,
                backZ + (rng.nextFloat() - 0.5f) * 0.5f,
                -sin(k.yawRad) * 5f, 1.2f, -cos(k.yawRad) * 5f,
                0.35f, 0.28f, er, eg, eb
            )
        }

        // Drift sparks
        if (k.isDrifting && k.isGrounded) {
            val (sr, sg, sb) = when {
                k.driftCharge >= 2.2f -> Triple(1f, 0.85f, 0.1f) // Gold Super
                k.driftCharge >= 1.35f -> Triple(1f, 0.25f, 0.6f) // Pink Tier 2
                else -> Triple(0.25f, 0.9f, 1f) // Cyan Tier 1
            }
            spawnParticle(
                k.x - sin(k.yawRad) * 1.1f + (rng.nextFloat() - 0.5f) * 1.4f,
                k.y + 0.25f,
                k.z - cos(k.yawRad) * 1.1f + (rng.nextFloat() - 0.5f) * 1.4f,
                (rng.nextFloat() - 0.5f) * 4f, 2.5f, (rng.nextFloat() - 0.5f) * 4f,
                0.32f, 0.22f, sr, sg, sb
            )
        }

        // Finish line celebratory fireworks around player!
        if (k.isPlayer && playerFinished && rng.nextFloat() < 0.45f) {
            spawnParticle(
                k.x + (rng.nextFloat() - 0.5f) * 10f,
                k.y + 3.5f + rng.nextFloat() * 4f,
                k.z + (rng.nextFloat() - 0.5f) * 10f,
                (rng.nextFloat() - 0.5f) * 7f, 4f + rng.nextFloat() * 5f, (rng.nextFloat() - 0.5f) * 7f,
                0.95f, 0.34f,
                0.4f + rng.nextFloat() * 0.6f,
                0.4f + rng.nextFloat() * 0.6f,
                0.4f + rng.nextFloat() * 0.6f
            )
        }
    }

    private fun spawnParticle(
        x: Float, y: Float, z: Float,
        vx: Float, vy: Float, vz: Float,
        life: Float, size: Float,
        r: Float, g: Float, b: Float
    ) {
        val pt = particles[particleCursor]
        particleCursor = (particleCursor + 1) % particles.size
        pt.x = x; pt.y = y; pt.z = z
        pt.vx = vx; pt.vy = vy; pt.vz = vz
        pt.life = life; pt.maxLife = life; pt.size = size
        pt.r = r; pt.g = g; pt.b = b
    }

    private fun updateAndDrawParticles(dt: Float) {
        for (pt in particles) {
            if (pt.life <= 0f) continue
            pt.life -= dt
            if (pt.life <= 0f) continue
            pt.x += pt.vx * dt
            pt.y += pt.vy * dt
            pt.z += pt.vz * dt
            val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
            val s = pt.size * (0.5f + 0.5f * alpha)

            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, pt.x, pt.y, pt.z)
            Matrix.scaleM(modelMat, 0, s, s, s)
            GLES30.glUniform4f(uTint, pt.r, pt.g, pt.b, alpha)
            drawMesh(cubeParticleMesh, modelMat, gloss = 0.9f)
        }
        GLES30.glUniform4f(uTint, 1f, 1f, 1f, 1f)
    }

    private fun setupLitProgram(fogHex: Long, fogStart: Float, fogEnd: Float) {
        GLES30.glUseProgram(litProgram)
        GLES30.glUniform3f(uCamPos, camX, camY, camZ)
        GLES30.glUniform3f(uSunDir, 0.45f, 0.85f, 0.35f)
        val (fr, fg, fb) = MeshAccumulator.hexToRgb(fogHex)
        GLES30.glUniform3f(uFogCol, fr, fg, fb)
        GLES30.glUniform1f(uFogStart, fogStart)
        GLES30.glUniform1f(uFogEnd, fogEnd)
        GLES30.glUniform1f(uRainbow, 0f)
        GLES30.glUniform4f(uTint, 1f, 1f, 1f, 1f)
    }

    private fun drawIdentityMesh(mesh: GpuMesh?, gloss: Float) {
        Matrix.setIdentityM(modelMat, 0)
        drawMesh(mesh, modelMat, gloss)
    }

    private fun drawMesh(mesh: GpuMesh?, model: FloatArray, gloss: Float) {
        if (mesh == null) return
        Matrix.multiplyMM(mvpMat, 0, vpMat, 0, model, 0)
        // Extract upper-left 3x3 for normal matrix (valid for uniform scale + rigid transform)
        normalMat[0] = model[0]; normalMat[1] = model[1]; normalMat[2] = model[2]
        normalMat[3] = model[4]; normalMat[4] = model[5]; normalMat[5] = model[6]
        normalMat[6] = model[8]; normalMat[7] = model[9]; normalMat[8] = model[10]

        GLES30.glUniformMatrix4fv(uMvp, 1, false, mvpMat, 0)
        GLES30.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES30.glUniformMatrix3fv(uNormalMat, 1, false, normalMat, 0)
        GLES30.glUniform1f(uGloss, gloss)
        mesh.draw()
    }

    private fun initSkyQuad() {
        val quad = floatArrayOf(
            -1f, -1f, 1f, -1f, 1f, 1f,
            -1f, -1f, 1f, 1f, -1f, 1f
        )
        val vaos = IntArray(1)
        val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        skyVao = vaos[0]
        val buf = ByteBuffer.allocateDirect(quad.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(quad)
        buf.position(0)
        GLES30.glBindVertexArray(skyVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbos[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quad.size * 4, buf, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun initSharedMeshes() {
        boostPadMesh = MeshFactory.buildBoostPadMesh()
        jumpRampMesh = MeshFactory.buildJumpRampMesh()
        caramelHazardMesh = MeshFactory.buildHazardMesh(isCaramel = true)
        peppermintHazardMesh = MeshFactory.buildHazardMesh(isCaramel = false)
        itemBoxMesh = MeshFactory.buildItemBoxMesh()
        shieldMesh = MeshFactory.buildSimpleSphereMesh(0xFF3CE8B5, 0.36f)
        cubeParticleMesh = MeshFactory.buildSimpleCubeMesh()

        val ped = MeshAccumulator()
        ped.addCylinder(0f, 0f, 0f, 2.7f, 2.4f, 0.38f, 24, 0xFFFF6EB4, 0xFFFFF8FC)
        ped.addCylinder(0f, 0.36f, 0f, 2.25f, 2.25f, 0.08f, 24, 0xFF3CE8B5, 0xFFFFFFFF)
        pedestalMesh = ped.build()
    }
}
