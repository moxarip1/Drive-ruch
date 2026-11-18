package com.example.sweetrush.render3d

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.GraphicsQuality
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.PowerUpType
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.model.VehicleDebugTelemetry
import com.example.sweetrush.model.WorldRegion
import com.example.sweetrush.physics.KartPhysicsEngine
import com.example.sweetrush.physics.KartRuntimeState
import com.example.sweetrush.physics.PlayerInputState
import com.example.sweetrush.physics.RaceEvent
import com.example.sweetrush.track.DecoType
import com.example.sweetrush.track.DecorationInstance
import com.example.sweetrush.track.GeneratedTrack
import com.example.sweetrush.track.OpenWorldKingdom
import com.example.sweetrush.track.SurfaceType
import com.example.sweetrush.track.TerrainChunkData
import com.example.sweetrush.track.TrackGenerator
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
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
    val nitroRatio: Float = 0.60f,
    val driftCharge: Float = 0f,
    val isDrifting: Boolean = false,
    val heldPowerUp: PowerUpType? = null,
    val activeShield: Boolean = false,
    val activeStar: Boolean = false,
    val smokeAlpha: Float = 0f,
    val countdownText: String = "",
    val raceStarted: Boolean = false,
    val finished: Boolean = false,
    val isFreeRoam: Boolean = false,
    val currentRegion: WorldRegion = WorldRegion.CANDY_CITY,
    val nearbyPortalTitle: String? = null,
    val nearbyPortalTrackId: TrackId? = null,
    val worldCoinsCollected: Int = 0,
    val worldStarsCollected: Int = 0,
    val playerWorldX: Float = 0f,
    val playerWorldZ: Float = 0f,
    val isBoostOrNitroActive: Boolean = false,
    val isGrounded: Boolean = true,
    val telemetry: VehicleDebugTelemetry = VehicleDebugTelemetry(),
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
    @Volatile var showroomZoom: Float = 5.2f
    @Volatile var showroomAutoRotate: Boolean = true

    // Active physics engine & input
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
    private var uSunColor = 0
    private var uFogCol = 0
    private var uFogStart = 0
    private var uFogEnd = 0
    private var uGloss = 0
    private var uRainbow = 0
    private var uTime = 0
    private var uBrakeGlow = 0
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

    // Open World cached meshes (chunked terrain + road network + flowing water)
    private var openWorldBuilt = false
    private var openWorldRoadMesh: GpuMesh? = null
    private var openWorldWaterMesh: GpuMesh? = null
    private var openWorldChunkMeta: List<TerrainChunkData> = emptyList()
    private val openWorldChunkMeshes = ArrayList<GpuMesh>()
    private val portalMeshCache = HashMap<Long, GpuMesh>()

    // Shared gameplay meshes
    private var boostPadMesh: GpuMesh? = null
    private var jumpRampMesh: GpuMesh? = null
    private var caramelHazardMesh: GpuMesh? = null
    private var peppermintHazardMesh: GpuMesh? = null
    private var itemBoxMesh: GpuMesh? = null
    private var shieldMesh: GpuMesh? = null
    private var cubeParticleMesh: GpuMesh? = null
    private var pedestalMesh: GpuMesh? = null
    private var worldCoinMesh: GpuMesh? = null
    private var worldStarMesh: GpuMesh? = null
    private var challengeBeaconMesh: GpuMesh? = null
    private var cloudClusterMesh: GpuMesh? = null

    // Multi-part Kart Model cache
    private val kartBundleCache = HashMap<String, KartModelBundle>()

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

    // Real spring-damped third-person camera state
    private var camX = 0f; private var camY = 6f; private var camZ = -12f
    private var camVelX = 0f; private var camVelY = 0f; private var camVelZ = 0f
    private var camTargetX = 0f; private var camTargetY = 2f; private var camTargetZ = 0f
    private var currentFov = 58f
    private var camInitializedForEngine: KartPhysicsEngine? = null

    // Particle pool
    private val particles = Array(200) { Particle3D() }
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
        uSunColor = GLES30.glGetUniformLocation(litProgram, "uSunColor")
        uFogCol = GLES30.glGetUniformLocation(litProgram, "uFogColor")
        uFogStart = GLES30.glGetUniformLocation(litProgram, "uFogStart")
        uFogEnd = GLES30.glGetUniformLocation(litProgram, "uFogEnd")
        uGloss = GLES30.glGetUniformLocation(litProgram, "uGlossiness")
        uRainbow = GLES30.glGetUniformLocation(litProgram, "uRainbowTime")
        uTime = GLES30.glGetUniformLocation(litProgram, "uTime")
        uBrakeGlow = GLES30.glGetUniformLocation(litProgram, "uBrakeGlow")
        uTint = GLES30.glGetUniformLocation(litProgram, "uTintColor")

        uSkyTop = GLES30.glGetUniformLocation(skyProgram, "uSkyTop")
        uSkyBottom = GLES30.glGetUniformLocation(skyProgram, "uSkyBottom")
        uSkyTime = GLES30.glGetUniformLocation(skyProgram, "uTime")

        initSkyQuad()
        initSharedMeshes()

        loadedTrackId = null
        trackMesh = null
        groundMesh = null
        openWorldBuilt = false
        openWorldChunkMeshes.clear()
        openWorldRoadMesh = null
        openWorldWaterMesh = null
        decoMeshCache.clear()
        kartBundleCache.clear()
        portalMeshCache.clear()
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
        val track = TrackGenerator.getTrack(trackId)
        loadedTrack = track
        loadedTrackId = trackId
        trackMesh = MeshFactory.buildTrackMesh(track)
        groundMesh = MeshFactory.buildGroundMesh(trackId)
    }

    private fun ensureOpenWorldLoaded() {
        if (openWorldBuilt) return
        openWorldChunkMeshes.forEach { it.release() }
        openWorldChunkMeshes.clear()
        openWorldRoadMesh?.release()
        openWorldWaterMesh?.release()

        openWorldChunkMeta = OpenWorldKingdom.getTerrainChunks()
        for (chunk in openWorldChunkMeta) {
            openWorldChunkMeshes.add(chunk.buildMesh())
        }
        openWorldRoadMesh = OpenWorldKingdom.buildRoadNetworkMesh()
        openWorldWaterMesh = OpenWorldKingdom.buildLiquidWaterMesh()
        openWorldBuilt = true
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

        val flyProgress = (totalTimeSec * 0.025f) % 1.0f
        val nodeIdx = ((flyProgress * track.nodes.size).toInt()) % track.nodes.size
        val node = track.nodes[nodeIdx]
        val yaw = atan2(node.forward.x, node.forward.z)

        // Hero showcase sweep around the cruising kart so center screen showcases 3D kart & world
        val heroOrbit = sin(totalTimeSec * 0.35f) * 0.55f + 0.38f
        val camAngle = yaw + heroOrbit
        val camDist = 6.4f
        camX = node.center.x - sin(camAngle) * camDist
        camY = node.center.y + 2.65f + sin(totalTimeSec * 0.7f) * 0.25f
        camZ = node.center.z - cos(camAngle) * camDist

        Matrix.perspectiveM(projMat, 0, 52f, aspectRatio, 0.4f, 460f)
        Matrix.setLookAtM(
            viewMat, 0,
            camX, camY, camZ,
            node.center.x + node.forward.x * 1.4f, node.center.y + 1.05f, node.center.z + node.forward.z * 1.4f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMat, 0, projMat, 0, viewMat, 0)

        setupLitProgram(spec.fogHex, 140f, 380f, WorldRegion.CANDY_CITY)

        drawIdentityMesh(groundMesh, gloss = 0.25f)
        drawIdentityMesh(trackMesh, gloss = 0.65f)
        drawDecorationsList(track.decorations, camX, camZ)
        drawSkyClouds(node.center.x, node.center.z)

        val kartSpec = CatalogData.kartById(showroomKartId)
        val racerSpec = CatalogData.racerById(showroomRacerId)
        drawDetailedKart(
            kart = kartSpec,
            racer = racerSpec,
            custom = showroomCustom,
            x = node.center.x,
            y = node.center.y,
            z = node.center.z,
            groundY = node.center.y,
            yawRad = yaw,
            rollRad = node.curvature * 0.15f,
            pitchRad = 0f,
            wheelSpin = totalTimeSec * 14f,
            steerVis = (node.curvature * 2.2f).coerceIn(-0.4f, 0.4f),
            wheelYOffsets = floatArrayOf(0f, 0f, 0f, 0f),
            isBraking = false,
            shieldActive = false,
            starActive = false
        )

        if (rng.nextFloat() < 0.35f) {
            spawnParticle(
                camX + (rng.nextFloat() - 0.5f) * 35f,
                camY + (rng.nextFloat() - 0.5f) * 10f,
                camZ + (rng.nextFloat() - 0.5f) * 35f,
                0f, 1.2f, 0f, 2.2f, 0.24f,
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
        val dist = showroomZoom.coerceIn(3.2f, 8.5f)
        val pitch = showroomOrbitPitch.coerceIn(0.08f, 0.78f)
        camX = sin(showroomOrbitYaw) * cos(pitch) * dist
        camY = 1.18f + sin(pitch) * dist
        camZ = cos(showroomOrbitYaw) * cos(pitch) * dist

        Matrix.perspectiveM(projMat, 0, 45f, aspectRatio, 0.25f, 80f)
        Matrix.setLookAtM(viewMat, 0, camX, camY, camZ, 0f, 0.85f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMat, 0, projMat, 0, viewMat, 0)

        setupLitProgram(0xFF421766, 30f, 70f, WorldRegion.CANDY_CITY)

        Matrix.setIdentityM(modelMat, 0)
        Matrix.translateM(modelMat, 0, 0f, -0.35f, 0f)
        drawMesh(pedestalMesh, modelMat, gloss = 0.85f)

        val kartSpec = CatalogData.kartById(showroomKartId)
        val racerSpec = CatalogData.racerById(showroomRacerId)

        drawDetailedKart(
            kart = kartSpec,
            racer = racerSpec,
            custom = showroomCustom,
            x = 0f,
            y = sin(totalTimeSec * 2.2f) * 0.025f,
            z = 0f,
            groundY = 0f,
            yawRad = 0f,
            rollRad = 0f,
            pitchRad = 0f,
            wheelSpin = totalTimeSec * 1.4f,
            steerVis = sin(totalTimeSec * 1.4f) * 0.22f,
            wheelYOffsets = floatArrayOf(0f, 0f, 0f, 0f),
            isBraking = false,
            shieldActive = false,
            starActive = false
        )

        if (rng.nextFloat() < 0.4f) {
            val (er, eg, eb) = MeshAccumulator.hexToRgb(showroomCustom.exhaustStyle.flameHex)
            spawnParticle(
                (rng.nextFloat() - 0.5f) * 0.8f,
                0.60f,
                -1.85f,
                (rng.nextFloat() - 0.5f) * 0.5f,
                0.4f + rng.nextFloat() * 0.6f,
                -1.8f,
                0.7f, 0.16f, er, eg, eb
            )
        }
        updateAndDrawParticles(dt)
    }

    private fun drawRace(dt: Float) {
        val engine = physicsEngine ?: return
        val events = if (!isPaused) engine.step(dt, currentInput) else emptyList()

        val p = engine.playerKart
        val region = engine.currentRegion
        val skyTopHex = if (engine.isFreeRoam) region.skyTopHex else engine.trackSpec.skyTopHex
        val skyBotHex = if (engine.isFreeRoam) region.skyBottomHex else engine.trackSpec.skyBottomHex
        val fogHex = if (engine.isFreeRoam) region.fogHex else engine.trackSpec.fogHex

        if (engine.isFreeRoam) {
            ensureOpenWorldLoaded()
        } else {
            ensureTrackLoaded(engine.trackId)
        }

        drawSky(skyTopHex, skyBotHex)

        // Snap camera smoothly behind player when starting a new session
        if (camInitializedForEngine !== engine) {
            camInitializedForEngine = engine
            camX = p.x - sin(p.yawRad) * 7.8f
            camY = p.y + 3.4f
            camZ = p.z - cos(p.yawRad) * 7.8f
            camVelX = 0f; camVelY = 0f; camVelZ = 0f
        }

        // ===============================================================
        // REAL SPRING-DAMPED THIRD-PERSON CHASE CAMERA + TERRAIN AVOIDANCE
        // ===============================================================
        val regionForLighting = if (engine.isFreeRoam) region else when (engine.trackId) {
            TrackId.PEPPERMINT_PLAZA -> WorldRegion.CANDY_CITY
            TrackId.CHOCOLATE_VOLCANO -> WorldRegion.CHOCOLATE_MOUNTAINS
            TrackId.GUMDROP_GARDEN -> WorldRegion.GUMDROP_FOREST
            TrackId.CUPCAKE_COAST -> WorldRegion.PEPPERMINT_COAST
        }

        if (p.finished) {
            val orbitA = totalTimeSec * 0.8f
            camX = p.x + sin(orbitA) * 8.2f
            camY = p.y + 4.0f
            camZ = p.z + cos(orbitA) * 8.2f
            camTargetX = p.x; camTargetY = p.y + 1.2f; camTargetZ = p.z
            currentFov += (54f - currentFov) * 4f * dt
        } else {
            val speedNorm = (abs(p.speed) / 62f).coerceIn(0f, 1.25f)
            val nitroBoostActive = (p.boostTimer > 0f || p.isNitroActive || p.starTimer > 0f)
            val targetFov = 58f + speedNorm * 13f + (if (nitroBoostActive) 7f else 0f)
            currentFov += (targetFov - currentFov) * 6.5f * dt

            // Subtle drift & cornering camera swing
            val driftCamSwing = if (p.isDrifting) p.driftDir * 0.22f else 0f
            val steerCamSwing = -p.steerVisualRad * 0.25f
            val camYaw = p.yawRad + driftCamSwing + steerCamSwing

            val followDist = 6.9f + speedNorm * 1.7f + (if (nitroBoostActive) 0.55f else 0f)
            val followHeight = 3.15f + speedNorm * 0.35f - (if (!p.isGrounded) 0.22f else 0f) - p.landingImpulse * 0.36f

            val desiredCamX = p.x - sin(camYaw) * followDist
            val desiredCamY = p.y + followHeight
            val desiredCamZ = p.z - cos(camYaw) * followDist

            // Critically damped spring chase physics
            val stiffness = 58f * cameraSensitivity.coerceIn(0.6f, 1.5f)
            val damping = 13.5f
            val ax = (desiredCamX - camX) * stiffness - camVelX * damping
            val ay = (desiredCamY - camY) * stiffness - camVelY * damping
            val az = (desiredCamZ - camZ) * stiffness - camVelZ * damping
            camVelX += ax * dt
            camVelY += ay * dt
            camVelZ += az * dt
            camX += camVelX * dt
            camY += camVelY * dt
            camZ += camVelZ * dt

            // Camera Terrain & Midpoint Ridge Collision Avoidance — NEVER clips below terrain or road!
            val camGroundH = engine.surfaceSampler.sampleSurface(camX, camZ, p.y).height
            val midGroundH = engine.surfaceSampler.sampleSurface((camX + p.x) * 0.5f, (camZ + p.z) * 0.5f, p.y).height
            val minCamY = maxOf(camGroundH + 1.85f, midGroundH + 1.45f)
            if (camY < minCamY) {
                camY = minCamY
                if (camVelY < 0f) camVelY = 0f
            }

            // Look-ahead targeting aligned with velocity & kart heading + landing dip
            val shakeAmt = engine.cameraShake * 0.34f
            val shakeX = if (shakeAmt > 0.01f) (rng.nextFloat() - 0.5f) * shakeAmt else 0f
            val shakeY = if (shakeAmt > 0.01f) (rng.nextFloat() - 0.5f) * shakeAmt else 0f

            val lookAhead = 5.4f + speedNorm * 2.2f
            val aimTargetX = p.x + sin(p.yawRad) * lookAhead + shakeX
            val aimTargetY = p.y + 1.38f - p.landingImpulse * 0.22f + shakeY
            val aimTargetZ = p.z + cos(p.yawRad) * lookAhead
            val targetLerp = (dt * 16f).coerceIn(0.1f, 0.95f)
            camTargetX += (aimTargetX - camTargetX) * targetLerp
            camTargetY += (aimTargetY - camTargetY) * targetLerp
            camTargetZ += (aimTargetZ - camTargetZ) * targetLerp
        }

        val farPlane = when (graphicsQuality) {
            GraphicsQuality.LOW -> 290f
            GraphicsQuality.MEDIUM -> 390f
            GraphicsQuality.HIGH -> 500f
        }
        Matrix.perspectiveM(projMat, 0, currentFov, aspectRatio, 0.35f, farPlane)
        Matrix.setLookAtM(viewMat, 0, camX, camY, camZ, camTargetX, camTargetY, camTargetZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMat, 0, projMat, 0, viewMat, 0)

        setupLitProgram(fogHex, farPlane * 0.42f, farPlane * 0.90f, regionForLighting)

        if (engine.isFreeRoam) {
            drawOpenWorldScene(engine)
        } else {
            val track = engine.track
            drawIdentityMesh(groundMesh, gloss = 0.25f)
            drawIdentityMesh(trackMesh, gloss = 0.65f)
            drawTrackGameplayObjects(engine, track)
            drawDecorationsList(track.decorations, camX, camZ)
        }

        drawSkyClouds(p.x, p.z)

        // Draw all karts with 4 independent suspension wheels, animated steering wheel & animated driver
        for (k in engine.karts) {
            val spinExtra = if (k.spinoutTimer > 0f) k.spinoutTimer * 12f else 0f
            val kGround = engine.surfaceSampler.sampleSurface(k.x, k.z, k.y).height
            drawDetailedKart(
                kart = k.kart,
                racer = k.racer,
                custom = k.customization,
                x = k.x,
                y = k.y + k.suspensionOffset,
                z = k.z,
                groundY = kGround,
                yawRad = k.yawRad + spinExtra,
                rollRad = k.bodyRollRad,
                pitchRad = k.bodyPitchRad,
                wheelSpin = k.wheelSpinRad,
                steerVis = k.steerVisualRad,
                wheelYOffsets = k.wheelYOffsets,
                isBraking = k.isBraking,
                shieldActive = k.shieldTimer > 0f,
                starActive = k.starTimer > 0f,
                isBoosting = k.boostTimer > 0f || k.isNitroActive,
                isFinished = k.finished,
                isSpunOut = k.spinoutTimer > 0f
            )

            if (!isPaused) {
                emitKartParticles(k, engine.playerFinished)
            }
        }

        if (!isPaused) {
            updateAndDrawParticles(dt)
        }

        val track = engine.track
        val snap = RaceHudSnapshot(
            position = p.position,
            currentLap = p.currentLap.coerceAtMost(engine.totalLaps),
            totalLaps = engine.totalLaps,
            elapsedMs = engine.raceElapsedMs,
            bestLapMs = p.bestLapMs,
            speedKmh = (abs(p.speed) * 3.1f).toInt(),
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
            } else if (engine.raceElapsedMs < 1100L && engine.mode != com.example.sweetrush.model.GameMode.PRACTICE && !engine.isFreeRoam) {
                "SWEET RUSH!"
            } else "",
            raceStarted = engine.raceStarted,
            finished = engine.playerFinished,
            isFreeRoam = engine.isFreeRoam,
            currentRegion = engine.currentRegion,
            nearbyPortalTitle = engine.nearbyRacePortal?.title,
            nearbyPortalTrackId = engine.nearbyRacePortal?.trackId,
            worldCoinsCollected = engine.collectedWorldCoins.size,
            worldStarsCollected = engine.collectedWorldStars.size,
            playerWorldX = p.x,
            playerWorldZ = p.z,
            isBoostOrNitroActive = (p.boostTimer > 0f || p.isNitroActive || p.starTimer > 0f),
            isGrounded = p.isGrounded,
            telemetry = engine.latestTelemetry,
            mapDots = engine.karts.map { RacerMapDot(it.x, it.z, it.isPlayer, it.racer.suitPrimaryHex) },
            trackMinX = if (engine.isFreeRoam) -440f else track.minX,
            trackMaxX = if (engine.isFreeRoam) 440f else track.maxX,
            trackMinZ = if (engine.isFreeRoam) -440f else track.minZ,
            trackMaxZ = if (engine.isFreeRoam) 440f else track.maxZ,
            trackOutline = if (engine.isFreeRoam) {
                OpenWorldKingdom.ringRoad.points.filterIndexed { i, _ -> i % 3 == 0 }.map { it.pos.x to it.pos.z }
            } else {
                track.nodes.filterIndexed { idx, _ -> idx % 3 == 0 }.map { it.center.x to it.center.z }
            }
        )
        onRaceStep(snap, events)
    }

    private fun drawOpenWorldScene(engine: KartPhysicsEngine) {
        val cullDist = when (graphicsQuality) {
            GraphicsQuality.LOW -> 270f
            GraphicsQuality.MEDIUM -> 360f
            GraphicsQuality.HIGH -> 480f
        }
        val chunkMaxSq = (cullDist + 130f) * (cullDist + 130f)
        for (i in openWorldChunkMeta.indices) {
            val ch = openWorldChunkMeta[i]
            val dx = ch.centerX - camX
            val dz = ch.centerZ - camZ
            if (dx * dx + dz * dz <= chunkMaxSq) {
                drawIdentityMesh(openWorldChunkMeshes[i], gloss = 0.22f)
            }
        }
        drawIdentityMesh(openWorldRoadMesh, gloss = 0.55f)
        drawIdentityMesh(openWorldWaterMesh, gloss = 0.92f)

        // World Boost Pads & Ramps
        for (pad in OpenWorldKingdom.boostPads) {
            if (distSq2D(pad.pos.x, pad.pos.z, camX, camZ) > cullDist * cullDist) continue
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, pad.pos.x, pad.pos.y, pad.pos.z)
            Matrix.rotateM(modelMat, 0, Math.toDegrees(pad.yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
            drawMesh(boostPadMesh, modelMat, gloss = 0.9f)
        }
        for (ramp in OpenWorldKingdom.jumpRamps) {
            if (distSq2D(ramp.pos.x, ramp.pos.z, camX, camZ) > cullDist * cullDist) continue
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, ramp.pos.x, ramp.pos.y, ramp.pos.z)
            Matrix.rotateM(modelMat, 0, Math.toDegrees(ramp.yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
            drawMesh(jumpRampMesh, modelMat, gloss = 0.78f)
        }

        // Collectible Candy Coins & Hidden Golden Stars
        val itemCullSq = 240f * 240f
        for (item in OpenWorldKingdom.collectibles) {
            val taken = if (item.isStar) engine.collectedWorldStars.contains(item.id)
            else engine.collectedWorldCoins.contains(item.id)
            if (taken) continue
            if (distSq2D(item.pos.x, item.pos.z, camX, camZ) > itemCullSq) continue
            Matrix.setIdentityM(modelMat, 0)
            val bob = sin(totalTimeSec * 3.0f + item.pos.x) * 0.28f
            Matrix.translateM(modelMat, 0, item.pos.x, item.pos.y + bob, item.pos.z)
            Matrix.rotateM(modelMat, 0, totalTimeSec * 105f, 0f, 1f, 0f)
            drawMesh(if (item.isStar) worldStarMesh else worldCoinMesh, modelMat, gloss = 0.96f)
        }

        // 5 Interactive Race Portals
        for (portal in OpenWorldKingdom.racePortals) {
            if (distSq2D(portal.pos.x, portal.pos.z, camX, camZ) > cullDist * cullDist) continue
            val pMesh = portalMeshCache.getOrPut(portal.accentHex) {
                MeshFactory.buildRacePortalMesh(portal.accentHex)
            }
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, portal.pos.x, portal.pos.y, portal.pos.z)
            Matrix.rotateM(modelMat, 0, Math.toDegrees(portal.yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
            drawMesh(pMesh, modelMat, gloss = 0.95f)
        }

        // Mini-Challenge Beacons
        for (ch in OpenWorldKingdom.miniChallenges) {
            if (engine.completedWorldMiniChallenges.contains(ch.id)) continue
            if (distSq2D(ch.pos.x, ch.pos.z, camX, camZ) > itemCullSq) continue
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, ch.pos.x, ch.pos.y + 3.0f + sin(totalTimeSec * 2.5f) * 0.4f, ch.pos.z)
            Matrix.rotateM(modelMat, 0, totalTimeSec * 80f, 0f, 1f, 0f)
            drawMesh(challengeBeaconMesh, modelMat, gloss = 0.95f)
        }

        drawDecorationsList(OpenWorldKingdom.decorations, camX, camZ)
    }

    private fun drawTrackGameplayObjects(engine: KartPhysicsEngine, track: GeneratedTrack) {
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
        for (box in engine.itemBoxes) {
            if (box.respawnTimer > 0f) continue
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, box.pos.x, box.pos.y + sin(totalTimeSec * 3.5f + box.id) * 0.25f, box.pos.z)
            Matrix.rotateM(modelMat, 0, totalTimeSec * 90f + box.id * 25f, 0f, 1f, 0f)
            Matrix.rotateM(modelMat, 0, 18f, 1f, 0f, 0f)
            drawMesh(itemBoxMesh, modelMat, gloss = 0.95f)
        }
    }

    private fun drawDecorationsList(decos: List<DecorationInstance>, refX: Float, refZ: Float) {
        val quality = graphicsQuality
        val maxDist = when (quality) {
            GraphicsQuality.LOW -> 180f
            GraphicsQuality.MEDIUM -> 260f
            GraphicsQuality.HIGH -> 355f
        }
        val maxDistSq = maxDist * maxDist
        val stride = if (quality == GraphicsQuality.LOW) 2 else 1

        for (i in decos.indices step stride) {
            val d = decos[i]
            val dx = d.pos.x - refX
            val dz = d.pos.z - refZ
            if (dx * dx + dz * dz > maxDistSq) continue

            val key = "${d.type.name}_${d.primaryColor}_${d.secondaryColor}"
            val mesh = decoMeshCache.getOrPut(key) {
                MeshFactory.buildDecorationMesh(d.type, d.primaryColor, d.secondaryColor)
            }

            val animY = when (d.type) {
                DecoType.FLOATING_BALLOON -> sin(totalTimeSec * 1.7f + d.bobPhase) * 1.15f
                DecoType.SPECTATOR_STAND -> sin(totalTimeSec * 5.2f + d.bobPhase) * 0.14f
                else -> 0f
            }
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, d.pos.x, d.pos.y + animY, d.pos.z)
            Matrix.rotateM(modelMat, 0, Math.toDegrees(d.rotY.toDouble()).toFloat(), 0f, 1f, 0f)
            Matrix.scaleM(modelMat, 0, d.scale, d.scale, d.scale)
            drawMesh(mesh, modelMat, gloss = 0.72f)
        }
    }

    private fun drawSkyClouds(refX: Float, refZ: Float) {
        if (graphicsQuality == GraphicsQuality.LOW) return
        val drift = (totalTimeSec * 3.5f) % 420f
        for (i in 0 until 8) {
            val cx = refX + ((i * 110f + drift) % 420f) - 210f
            val cz = refZ + ((i * 145f) % 380f) - 190f
            val cy = 54f + (i % 3) * 8f
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, cx, cy, cz)
            Matrix.scaleM(modelMat, 0, 5.5f + (i % 2) * 2.2f, 3.2f, 4.5f)
            drawMesh(cloudClusterMesh, modelMat, gloss = 0.2f)
        }
    }

    private fun drawDetailedKart(
        kart: com.example.sweetrush.model.KartSpec,
        racer: com.example.sweetrush.model.RacerSpec,
        custom: KartCustomization,
        x: Float, y: Float, z: Float,
        groundY: Float,
        yawRad: Float, rollRad: Float, pitchRad: Float,
        wheelSpin: Float, steerVis: Float,
        wheelYOffsets: FloatArray,
        isBraking: Boolean,
        shieldActive: Boolean, starActive: Boolean,
        isBoosting: Boolean = false,
        isFinished: Boolean = false,
        isSpunOut: Boolean = false
    ) {
        val bodyKey = "${kart.id}_${racer.id}_${custom.paintId}_${custom.wheelDesign.name}_${custom.exhaustStyle.name}_${custom.decoration.name}"
        val bundle = kartBundleCache.getOrPut(bodyKey) {
            VehicleMeshBuilder.buildKartBundle(kart, racer, custom)
        }

        // Ground contact shadow oval directly on sampled terrain
        val heightAbove = (y - groundY).coerceIn(0f, 6f)
        val shadowScale = (1.0f - heightAbove * 0.11f).coerceIn(0.45f, 1.0f)
        val shadowAlpha = (1.0f - heightAbove * 0.15f).coerceIn(0.2f, 1.0f)
        val shMat = FloatArray(16)
        Matrix.setIdentityM(shMat, 0)
        Matrix.translateM(shMat, 0, x, groundY + 0.04f, z)
        Matrix.rotateM(shMat, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(shMat, 0, shadowScale, 1f, shadowScale)
        GLES30.glUniform4f(uTint, 1f, 1f, 1f, shadowAlpha)
        GLES30.glDepthMask(false)
        drawMesh(bundle.shadowBlobMesh, shMat, gloss = 0.05f)
        GLES30.glDepthMask(true)
        GLES30.glUniform4f(uTint, 1f, 1f, 1f, 1f)

        val rainbowVal = if (starActive) totalTimeSec else 0f
        GLES30.glUniform1f(uRainbow, rainbowVal)

        // Transform for chassis + suspension + brake lights
        Matrix.setIdentityM(modelMat, 0)
        Matrix.translateM(modelMat, 0, x, y, z)
        Matrix.rotateM(modelMat, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMat, 0, Math.toDegrees(rollRad.toDouble()).toFloat(), 0f, 0f, 1f)
        Matrix.rotateM(modelMat, 0, Math.toDegrees(pitchRad.toDouble()).toFloat(), 1f, 0f, 0f)

        drawMesh(bundle.chassisMesh, modelMat, gloss = 0.88f)
        drawMesh(bundle.suspensionMesh, modelMat, gloss = 0.90f)

        // Dynamic brake lights
        GLES30.glUniform1f(uBrakeGlow, if (isBraking) 1.0f else 0.18f)
        drawMesh(bundle.brakeLightMesh, modelMat, gloss = 0.95f)
        GLES30.glUniform1f(uBrakeGlow, 0f)

        val spinDeg = Math.toDegrees(wheelSpin.toDouble()).toFloat()
        val steerDeg = Math.toDegrees(steerVis.toDouble()).toFloat()

        // Animated Steering Wheel inside cockpit
        val swMat = FloatArray(16)
        Matrix.translateM(swMat, 0, modelMat, 0, 0f, 1.04f, 0.42f)
        Matrix.rotateM(swMat, 0, -24f, 1f, 0f, 0f)
        Matrix.rotateM(swMat, 0, steerDeg * 1.45f, 0f, 0f, 1f)
        drawMesh(bundle.steeringWheelMesh, swMat, gloss = 0.88f)

        // Animated Seated Driver Character (leaning into turns, hunching on boost, cheering on finish)
        val drvMat = FloatArray(16)
        val celebrateY = if (isFinished) abs(sin(totalTimeSec * 7f)) * 0.14f else 0f
        val driverLeanZ = -steerDeg * 0.55f
        val driverPitchX = when {
            isSpunOut -> 18f
            isFinished -> sin(totalTimeSec * 7f) * 8f
            isBoosting -> -10f
            isBraking -> 7f
            else -> 0f
        }
        Matrix.translateM(drvMat, 0, modelMat, 0, 0f, 0.85f + celebrateY, -0.14f)
        Matrix.rotateM(drvMat, 0, driverLeanZ, 0f, 0f, 1f)
        Matrix.rotateM(drvMat, 0, driverPitchX, 1f, 0f, 0f)
        drawMesh(bundle.driverMesh, drvMat, gloss = 0.72f)

        // 4 Independent Wheels (FL, FR, RL, RR) with independent suspension compression offsets!
        val wheelCoords = arrayOf(
            Triple(-0.92f, 0.45f, 0.96f) to true,
            Triple(0.92f, 0.45f, 0.96f) to true,
            Triple(-0.96f, 0.45f, -0.96f) to false,
            Triple(0.96f, 0.45f, -0.96f) to false
        )

        for (i in 0 until 4) {
            val (pos, isFront) = wheelCoords[i]
            val wMat = FloatArray(16)
            val suspY = if (i < wheelYOffsets.size) wheelYOffsets[i] else 0f
            Matrix.translateM(wMat, 0, modelMat, 0, pos.first, pos.second + suspY, pos.third)
            if (isFront) {
                Matrix.rotateM(wMat, 0, steerDeg, 0f, 1f, 0f)
            }
            Matrix.rotateM(wMat, 0, spinDeg, 1f, 0f, 0f)
            drawMesh(bundle.tireMesh, wMat, gloss = 0.25f)
            drawMesh(bundle.rimMesh, wMat, gloss = 0.92f)
        }

        GLES30.glUniform1f(uRainbow, 0f)

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

        if (k.isNitroActive || k.boostTimer > 0f || k.starTimer > 0f) {
            val (er, eg, eb) = if (k.starTimer > 0f) {
                Triple(rng.nextFloat(), rng.nextFloat(), 1f)
            } else {
                MeshAccumulator.hexToRgb(k.customization.exhaustStyle.flameHex)
            }
            val backX = k.x - sin(k.yawRad) * 1.75f
            val backZ = k.z - cos(k.yawRad) * 1.75f
            spawnParticle(
                backX + (rng.nextFloat() - 0.5f) * 0.5f,
                k.y + 0.56f,
                backZ + (rng.nextFloat() - 0.5f) * 0.5f,
                -sin(k.yawRad) * 5f, 1.1f, -cos(k.yawRad) * 5f,
                0.35f, 0.26f, er, eg, eb
            )
        }

        if (k.isDrifting && k.isGrounded) {
            val (sr, sg, sb) = when {
                k.driftCharge >= 2.2f -> Triple(1f, 0.85f, 0.1f)
                k.driftCharge >= 1.35f -> Triple(1f, 0.25f, 0.6f)
                else -> Triple(0.25f, 0.9f, 1f)
            }
            spawnParticle(
                k.x - sin(k.yawRad) * 1.1f + (rng.nextFloat() - 0.5f) * 1.4f,
                k.y + 0.22f,
                k.z - cos(k.yawRad) * 1.1f + (rng.nextFloat() - 0.5f) * 1.4f,
                (rng.nextFloat() - 0.5f) * 4f, 2.5f, (rng.nextFloat() - 0.5f) * 4f,
                0.32f, 0.22f, sr, sg, sb
            )
        }

        if (k.justLandedImpulse > 0.05f) {
            val count = (4 + (k.justLandedImpulse * 6).toInt()).coerceAtMost(10)
            for (pi in 0 until count) {
                val a = (pi.toFloat() / count) * 6.283f + rng.nextFloat() * 0.4f
                spawnParticle(
                    k.x + cos(a) * 0.9f,
                    k.y + 0.18f,
                    k.z + sin(a) * 0.9f,
                    cos(a) * 4.5f, 1.8f + rng.nextFloat() * 2.0f, sin(a) * 4.5f,
                    0.42f, 0.24f, 0.98f, 0.92f, 0.96f
                )
            }
            k.justLandedImpulse = 0f
        }

        if (k.isGrounded && abs(k.speed) > 12f && (k.currentSurface == SurfaceType.OFFROAD || k.currentSurface == SurfaceType.WATER)) {
            val isWater = k.currentSurface == SurfaceType.WATER
            spawnParticle(
                k.x - sin(k.yawRad) * 1.0f + (rng.nextFloat() - 0.5f) * 1.4f,
                k.y + 0.16f,
                k.z - cos(k.yawRad) * 1.0f + (rng.nextFloat() - 0.5f) * 1.4f,
                (rng.nextFloat() - 0.5f) * 2.6f, 1.5f + rng.nextFloat() * 1.4f, (rng.nextFloat() - 0.5f) * 2.6f,
                0.35f, 0.20f,
                if (isWater) 1f else 0.95f,
                if (isWater) 0.72f else 0.85f,
                if (isWater) 0.88f else 0.72f
            )
        }

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

    private fun setupLitProgram(
        fogHex: Long,
        fogStart: Float,
        fogEnd: Float,
        region: WorldRegion = WorldRegion.CANDY_CITY
    ) {
        GLES30.glUseProgram(litProgram)
        GLES30.glUniform3f(uCamPos, camX, camY, camZ)
        GLES30.glUniform3f(uSunDir, 0.45f, 0.85f, 0.35f)
        when (region) {
            WorldRegion.CANDY_CITY -> GLES30.glUniform3f(uSunColor, 1.00f, 0.96f, 0.98f)
            WorldRegion.CHOCOLATE_MOUNTAINS -> GLES30.glUniform3f(uSunColor, 1.00f, 0.78f, 0.58f)
            WorldRegion.GUMDROP_FOREST -> GLES30.glUniform3f(uSunColor, 0.84f, 1.00f, 0.94f)
            WorldRegion.CUPCAKE_VALLEY -> GLES30.glUniform3f(uSunColor, 1.00f, 0.88f, 0.98f)
            WorldRegion.PEPPERMINT_COAST -> GLES30.glUniform3f(uSunColor, 1.00f, 0.98f, 0.90f)
        }
        val (fr, fg, fb) = MeshAccumulator.hexToRgb(fogHex)
        GLES30.glUniform3f(uFogCol, fr, fg, fb)
        GLES30.glUniform1f(uFogStart, fogStart)
        GLES30.glUniform1f(uFogEnd, fogEnd)
        GLES30.glUniform1f(uRainbow, 0f)
        GLES30.glUniform1f(uTime, totalTimeSec)
        GLES30.glUniform1f(uBrakeGlow, 0f)
        GLES30.glUniform4f(uTint, 1f, 1f, 1f, 1f)
    }

    private fun drawIdentityMesh(mesh: GpuMesh?, gloss: Float) {
        Matrix.setIdentityM(modelMat, 0)
        drawMesh(mesh, modelMat, gloss)
    }

    private fun drawMesh(mesh: GpuMesh?, model: FloatArray, gloss: Float) {
        if (mesh == null) return
        Matrix.multiplyMM(mvpMat, 0, vpMat, 0, model, 0)
        normalMat[0] = model[0]; normalMat[1] = model[1]; normalMat[2] = model[2]
        normalMat[3] = model[4]; normalMat[4] = model[5]; normalMat[5] = model[6]
        normalMat[6] = model[8]; normalMat[7] = model[9]; normalMat[8] = model[10]

        GLES30.glUniformMatrix4fv(uMvp, 1, false, mvpMat, 0)
        GLES30.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES30.glUniformMatrix3fv(uNormalMat, 1, false, normalMat, 0)
        GLES30.glUniform1f(uGloss, gloss)
        mesh.draw()
    }

    private fun distSq2D(ax: Float, az: Float, bx: Float, bz: Float): Float {
        val dx = ax - bx; val dz = az - bz
        return dx * dx + dz * dz
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
        worldCoinMesh = MeshFactory.buildWorldCoinMesh()
        worldStarMesh = MeshFactory.buildWorldStarMesh()

        val beacon = MeshAccumulator()
        beacon.addCylinder(0f, -2.5f, 0f, 1.4f, 0.2f, 5.5f, 10, 0xFF3CE8B5, 0xFFFFFFFF, 0.8f, CandyMaterials.MAT_EMISSIVE)
        challengeBeaconMesh = beacon.build()

        val cloud = MeshAccumulator()
        cloud.addSphere(0f, 0f, 0f, 1.4f, 0.7f, 1.0f, 5, 8, 0xFFFFF8FF)
        cloud.addSphere(1.0f, -0.1f, 0.3f, 0.95f, 0.55f, 0.8f, 5, 7, 0xFFFFEEF8)
        cloud.addSphere(-1.0f, -0.1f, -0.2f, 0.90f, 0.50f, 0.75f, 5, 7, 0xFFFFF8FF)
        cloudClusterMesh = cloud.build()

        val ped = MeshAccumulator()
        ped.addCylinder(0f, 0f, 0f, 2.7f, 2.4f, 0.38f, 24, 0xFFFF6EB4, 0xFFFFF8FC)
        ped.addCylinder(0f, 0.36f, 0f, 2.25f, 2.25f, 0.08f, 24, 0xFF3CE8B5, 0xFFFFFFFF, matType = CandyMaterials.MAT_EMISSIVE)
        pedestalMesh = ped.build()
    }
}
