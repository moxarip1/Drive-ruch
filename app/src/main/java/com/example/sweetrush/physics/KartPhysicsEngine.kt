package com.example.sweetrush.physics

import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.Difficulty
import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.KartSpec
import com.example.sweetrush.model.PowerUpType
import com.example.sweetrush.model.RacerResultEntry
import com.example.sweetrush.model.RacerSpec
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.track.GeneratedTrack
import com.example.sweetrush.track.TrackGenerator
import com.example.sweetrush.track.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class PlayerInputState(
    val steer: Float = 0f,        // -1 (left) .. +1 (right)
    val throttle: Boolean = false,
    val brake: Boolean = false,
    val drift: Boolean = false,
    val nitro: Boolean = false,
    val jumpPressed: Boolean = false,
    val usePowerUpPressed: Boolean = false
)

data class DroppedHazard(
    val id: Int,
    val ownerId: String,
    val type: PowerUpType, // PEPPERMINT_BLAST or CARAMEL_TRAP
    val pos: Vec3,
    var ttl: Float
)

data class ActiveItemBox(
    val id: Int,
    val pos: Vec3,
    val yawRad: Float,
    var respawnTimer: Float = 0f
)

data class KartRuntimeState(
    val racer: RacerSpec,
    val kart: KartSpec,
    val customization: KartCustomization,
    val isPlayer: Boolean,
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var yawRad: Float = 0f,
    var speed: Float = 0f,
    var wheelSpinRad: Float = 0f,
    var steerVisualRad: Float = 0f,
    var bodyRollRad: Float = 0f,
    var bodyPitchRad: Float = 0f,
    var suspensionOffset: Float = 0f,
    var isGrounded: Boolean = true,
    var isDrifting: Boolean = false,
    var driftDir: Float = 0f,
    var driftCharge: Float = 0f, // 0..3.0
    var nitroFuel: Float = 0.55f, // 0..1.0
    var isNitroActive: Boolean = false,
    var boostTimer: Float = 0f,
    var shieldTimer: Float = 0f,
    var starTimer: Float = 0f,
    var slowTimer: Float = 0f,
    var smokeTimer: Float = 0f,
    var spinoutTimer: Float = 0f,
    var heldPowerUp: PowerUpType? = null,
    var nodeHintIndex: Int = 0,
    var lateralOffset: Float = 0f,
    var sectorReached: Int = 0, // 0..3 checkpoints per lap
    var currentLap: Int = 1,
    var totalLapsToFinish: Int = 3,
    var finished: Boolean = false,
    var finishTimeMs: Long = 0L,
    var currentLapStartMs: Long = 0L,
    var bestLapMs: Long = Long.MAX_VALUE,
    var position: Int = 1,
    var raceScore: Float = 0f,
    var aiTargetLaneOffset: Float = 0f,
    var aiLaneTimer: Float = 0f,
    var aiStuckTimer: Float = 0f,
    var aiPowerUpDelay: Float = 1.5f
)

enum class RaceEventType {
    COUNTDOWN_TICK,
    RACE_GO,
    DRIFT_RELEASE,
    NITRO_START,
    JUMP,
    POWERUP_PICKUP,
    POWERUP_USED,
    COLLISION,
    HAZARD_HIT,
    LAP_COMPLETED,
    OVERTAKE,
    RACE_FINISHED
}

data class RaceEvent(
    val type: RaceEventType,
    val message: String = "",
    val powerUp: PowerUpType? = null
)

class KartPhysicsEngine(
    val trackId: TrackId,
    val mode: GameMode,
    val difficulty: Difficulty,
    playerRacerId: String,
    playerKartId: String,
    playerCustomization: KartCustomization
) {
    val track: GeneratedTrack = TrackGenerator.getTrack(trackId)
    val trackSpec = CatalogData.trackById(trackId)
    val totalLaps: Int = when (mode) {
        GameMode.TIME_TRIAL, GameMode.PRACTICE -> 99
        else -> trackSpec.laps
    }

    val karts: List<KartRuntimeState>
    val playerKart: KartRuntimeState
    val itemBoxes: MutableList<ActiveItemBox>
    val droppedHazards: MutableList<DroppedHazard> = mutableListOf()

    var countdownSeconds: Float = if (mode == GameMode.PRACTICE) 0f else 3.4f
    var raceStarted: Boolean = (mode == GameMode.PRACTICE)
    var raceElapsedMs: Long = 0L
    var playerFinished: Boolean = false
    var playerDriftBoostCount: Int = 0
    var playerHitHazard: Boolean = false
    var cameraShake: Float = 0f

    private var lastCountdownInt = 4
    private var lastPlayerPos = 8
    private var nextHazardId = 1
    private val rng = Random(System.currentTimeMillis())

    init {
        val startNode = track.nodes[0]
        val baseYaw = atan2(startNode.forward.x, startNode.forward.z)

        val playerRacer = CatalogData.racerById(playerRacerId)
        val pKartSpec = CatalogData.kartById(playerKartId)

        val aiRacers = CatalogData.racers.filter { it.id != playerRacerId }.shuffled(Random(trackId.ordinal * 31 + 7))

        val roster = mutableListOf<KartRuntimeState>()

        if (mode == GameMode.TIME_TRIAL || mode == GameMode.PRACTICE) {
            val pos = startNode.center - startNode.forward * 4f
            roster.add(
                KartRuntimeState(
                    racer = playerRacer,
                    kart = pKartSpec,
                    customization = playerCustomization,
                    isPlayer = true,
                    x = pos.x,
                    y = pos.y,
                    z = pos.z,
                    yawRad = baseYaw,
                    totalLapsToFinish = totalLaps,
                    position = 1
                )
            )
        } else {
            // 7 AI + 1 Player on a clean 2x4 starting grid behind start line!
            for (slot in 0 until 8) {
                val isPlayerSlot = (slot == 7) // Player starts P8 for exciting overtakes!
                val row = slot / 2
                val col = if (slot % 2 == 0) -3.8f else 3.8f
                val backDist = 5f + row * 7.5f
                val spawnPos = startNode.center - startNode.forward * backDist + startNode.right * col

                val racer = if (isPlayerSlot) playerRacer else aiRacers[slot]
                val kartSpec = if (isPlayerSlot) pKartSpec else CatalogData.kartById(racer.favoriteKartId)
                val custom = if (isPlayerSlot) playerCustomization else KartCustomization()

                roster.add(
                    KartRuntimeState(
                        racer = racer,
                        kart = kartSpec,
                        customization = custom,
                        isPlayer = isPlayerSlot,
                        x = spawnPos.x,
                        y = spawnPos.y,
                        z = spawnPos.z,
                        yawRad = baseYaw,
                        totalLapsToFinish = totalLaps,
                        position = slot + 1,
                        aiTargetLaneOffset = col * 0.7f
                    )
                )
            }
        }

        karts = roster
        playerKart = karts.first { it.isPlayer }
        lastPlayerPos = playerKart.position

        itemBoxes = track.itemBoxes.map {
            ActiveItemBox(it.id, it.pos, it.yawRad, 0f)
        }.toMutableList()
    }

    fun step(dtRaw: Float, input: PlayerInputState): List<RaceEvent> {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        val events = mutableListOf<RaceEvent>()
        cameraShake = (cameraShake - dt * 2.5f).coerceAtLeast(0f)

        // Countdown handling
        if (!raceStarted) {
            countdownSeconds -= dt
            val ceilSec = kotlin.math.ceil(countdownSeconds.toDouble()).toInt()
            if (ceilSec in 1..3 && ceilSec != lastCountdownInt) {
                lastCountdownInt = ceilSec
                events.add(RaceEvent(RaceEventType.COUNTDOWN_TICK, ceilSec.toString()))
            }
            if (countdownSeconds <= 0f) {
                raceStarted = true
                events.add(RaceEvent(RaceEventType.RACE_GO, "SWEET RUSH!"))
            } else {
                // Allow revving engine slightly on grid
                playerKart.wheelSpinRad += if (input.throttle) dt * 6f else 0f
                return events
            }
        }

        raceElapsedMs += (dt * 1000f).toLong()

        // Update item boxes respawn
        for (box in itemBoxes) {
            if (box.respawnTimer > 0f) {
                box.respawnTimer = (box.respawnTimer - dt).coerceAtLeast(0f)
            }
        }

        // Update dropped hazards TTL
        val hazIter = droppedHazards.iterator()
        while (hazIter.hasNext()) {
            val h = hazIter.next()
            h.ttl -= dt
            if (h.ttl <= 0f) hazIter.remove()
        }

        // Simulate each kart
        for (kart in karts) {
            val kartInput = if (kart.isPlayer && !kart.finished) {
                input
            } else {
                computeAiInput(kart, dt)
            }
            stepSingleKart(kart, kartInput, dt, events)
        }

        // Kart-to-kart collisions
        resolveKartCollisions(events)

        // Sort race positions
        updateStandings(events)

        return events
    }

    private fun stepSingleKart(
        k: KartRuntimeState,
        input: PlayerInputState,
        dt: Float,
        events: MutableList<RaceEvent>
    ) {
        val stats = CatalogData.effectiveStats(k.kart, k.customization)

        // Timers
        if (k.boostTimer > 0f) k.boostTimer = (k.boostTimer - dt).coerceAtLeast(0f)
        if (k.shieldTimer > 0f) k.shieldTimer = (k.shieldTimer - dt).coerceAtLeast(0f)
        if (k.starTimer > 0f) k.starTimer = (k.starTimer - dt).coerceAtLeast(0f)
        if (k.slowTimer > 0f) k.slowTimer = (k.slowTimer - dt).coerceAtLeast(0f)
        if (k.smokeTimer > 0f) k.smokeTimer = (k.smokeTimer - dt).coerceAtLeast(0f)
        if (k.spinoutTimer > 0f) k.spinoutTimer = (k.spinoutTimer - dt).coerceAtLeast(0f)

        // Power-up activation
        if (input.usePowerUpPressed && k.heldPowerUp != null) {
            activatePowerUp(k, k.heldPowerUp!!, events)
            k.heldPowerUp = null
        }

        val maxBaseSpeed = (44f + stats.speed * 13f) * (if (k.isPlayer) 1.0f else difficulty.aiSpeedMultiplier)
        val accelRate = 24f + stats.acceleration * 16f
        val steerRate = 1.85f + stats.handling * 0.95f

        val isBoosting = k.boostTimer > 0f || k.starTimer > 0f ||
            (input.nitro && k.nitroFuel > 0.03f && k.speed > 8f)

        k.isNitroActive = input.nitro && k.nitroFuel > 0.03f && k.speed > 8f && k.spinoutTimer <= 0f
        if (k.isNitroActive) {
            if (k.isPlayer && k.nitroFuel > 0.95f) events.add(RaceEvent(RaceEventType.NITRO_START))
            k.nitroFuel = (k.nitroFuel - dt * (0.28f - stats.boost * 0.08f)).coerceAtLeast(0f)
        } else {
            // Gentle passive recharge when moving fast
            if (k.speed > 20f) {
                k.nitroFuel = (k.nitroFuel + dt * 0.03f).coerceAtMost(1.0f)
            }
        }

        val speedMultiplier = when {
            k.spinoutTimer > 0f -> 0.25f
            k.slowTimer > 0f -> 0.56f
            k.starTimer > 0f -> 1.38f
            isBoosting -> 1.30f + stats.boost * 0.08f
            else -> 1.0f
        }
        val targetTopSpeed = maxBaseSpeed * speedMultiplier

        // Longitudinal speed
        if (k.spinoutTimer <= 0f) {
            if (input.throttle || isBoosting) {
                val boostExtra = if (isBoosting) 2.1f else 1.0f
                k.speed += accelRate * boostExtra * dt
            } else if (input.brake) {
                if (k.speed > 2f) {
                    k.speed -= 42f * dt
                } else {
                    k.speed -= 18f * dt
                }
            } else {
                // Natural rolling drag
                k.speed -= k.speed * 1.15f * dt
            }
        } else {
            k.speed -= k.speed * 2.5f * dt
        }

        k.speed = k.speed.coerceIn(-16f, targetTopSpeed)
        if (k.speed > targetTopSpeed) {
            k.speed -= (k.speed - targetTopSpeed) * 3.0f * dt
        }

        // Drifting & Charging
        val steerInput = input.steer.coerceIn(-1f, 1f)
        val canContinueDrift = k.isDrifting && input.drift && k.speed > 12f && k.spinoutTimer <= 0f
        val canStartDrift = !k.isDrifting && input.drift && k.isGrounded && k.speed > 18f && k.spinoutTimer <= 0f && abs(steerInput) > 0.18f
        if (canStartDrift) {
            k.isDrifting = true
            k.driftDir = if (steerInput > 0f) 1f else -1f
            // Small drift hop!
            k.vy = 3.2f
            k.isGrounded = false
        }
        if (canStartDrift || canContinueDrift) {
            k.driftCharge = (k.driftCharge + dt * (0.95f + abs(steerInput) * 0.45f)).coerceAtMost(3.0f)
            k.nitroFuel = (k.nitroFuel + dt * 0.14f).coerceAtMost(1.0f)
        } else if (k.isDrifting) {
            // Release drift -> award mini turbo if charged >= 0.7s!
            if (k.driftCharge >= 0.7f) {
                val boostDur = when {
                    k.driftCharge >= 2.2f -> 1.65f
                    k.driftCharge >= 1.35f -> 1.15f
                    else -> 0.70f
                }
                k.boostTimer = maxOf(k.boostTimer, boostDur)
                k.speed = maxOf(k.speed, maxBaseSpeed * 1.24f)
                if (k.isPlayer) {
                    playerDriftBoostCount++
                    events.add(RaceEvent(RaceEventType.DRIFT_RELEASE, if (k.driftCharge >= 2.2f) "SUPER DRIFT!" else "DRIFT BOOST!"))
                }
            }
            k.isDrifting = false
            k.driftCharge = 0f
            k.driftDir = 0f
        }

        // Steering yaw
        val speedFactor = (abs(k.speed) / 14f).coerceIn(0.25f, 1.0f)
        val reverseSign = if (k.speed < -1f) -1f else 1f
        val effectiveSteer = if (k.isDrifting) {
            k.driftDir * 0.85f + steerInput * 0.55f
        } else {
            steerInput
        }
        val airControl = if (k.isGrounded) 1.0f else 0.58f
        k.yawRad -= effectiveSteer * steerRate * speedFactor * reverseSign * airControl * dt

        // Jump input
        if (input.jumpPressed && k.isGrounded && k.spinoutTimer <= 0f) {
            k.vy = 9.2f
            k.isGrounded = false
            if (k.isPlayer) events.add(RaceEvent(RaceEventType.JUMP))
        }

        // Forward velocity integration
        val fwdX = sin(k.yawRad)
        val fwdZ = cos(k.yawRad)
        k.vx = fwdX * k.speed
        k.vz = fwdZ * k.speed

        k.x += k.vx * dt
        k.z += k.vz * dt

        // Project onto track for elevation, boundaries, checkpoints & lap counting
        val proj = track.projectOntoTrack(k.x, k.z, k.nodeHintIndex)
        val prevNodeIndex = k.nodeHintIndex
        k.nodeHintIndex = proj.nodeIndex
        k.lateralOffset = proj.lateralOffset

        // Track Boundary Candy Barriers
        val maxLateral = proj.halfWidth - 1.25f
        if (abs(proj.lateralOffset) > maxLateral) {
            val sign = if (proj.lateralOffset > 0f) 1f else -1f
            val excess = abs(proj.lateralOffset) - maxLateral
            // Push gently back inside road
            k.x -= proj.right.x * sign * excess
            k.z -= proj.right.z * sign * excess
            k.lateralOffset = sign * maxLateral
            k.speed *= (0.94f + stats.stability * 0.04f)
            // Align heading slightly with road forward to prevent getting wedged
            val roadYaw = atan2(proj.forward.x, proj.forward.z)
            k.yawRad = lerpAngle(k.yawRad, roadYaw, 0.18f)
        }

        // Vertical physics & suspension
        val groundY = proj.center.y
        if (!k.isGrounded || k.y > groundY + 0.04f) {
            k.vy -= 26f * dt
            k.y += k.vy * dt
            if (k.y <= groundY) {
                k.y = groundY
                if (k.vy < -4f) {
                    k.suspensionOffset = -0.18f
                }
                k.vy = 0f
                k.isGrounded = true
            }
        } else {
            k.y = groundY
            k.vy = 0f
            k.isGrounded = true
        }
        k.suspensionOffset += (0f - k.suspensionOffset) * 10f * dt

        // Visual tilt & wheel animation
        k.wheelSpinRad += k.speed * dt * 1.4f
        val targetSteerVis = -steerInput * 0.42f
        k.steerVisualRad += (targetSteerVis - k.steerVisualRad) * 12f * dt
        val targetRoll = (if (k.isDrifting) k.driftDir * 0.16f else steerInput * 0.09f) * speedFactor
        k.bodyRollRad += (targetRoll - k.bodyRollRad) * 10f * dt
        val targetPitch = if (!k.isGrounded) (k.vy * 0.025f).coerceIn(-0.22f, 0.22f) else if (isBoosting) -0.05f else 0f
        k.bodyPitchRad += (targetPitch - k.bodyPitchRad) * 8f * dt

        // Boost Pads interaction
        for (pad in track.boostPads) {
            val dx = k.x - pad.pos.x
            val dz = k.z - pad.pos.z
            if (dx * dx + dz * dz < 8.5f) {
                k.boostTimer = maxOf(k.boostTimer, 1.1f)
                k.nitroFuel = (k.nitroFuel + 0.18f).coerceAtMost(1.0f)
                if (k.isPlayer && k.boostTimer > 1.0f) {
                    events.add(RaceEvent(RaceEventType.NITRO_START, "SUGAR PAD!"))
                }
            }
        }

        // Jump Ramps interaction
        for (ramp in track.jumpRamps) {
            val dx = k.x - ramp.pos.x
            val dz = k.z - ramp.pos.z
            if (dx * dx + dz * dz < 11f && k.isGrounded && k.speed > 18f) {
                k.vy = 13.5f
                k.isGrounded = false
                k.boostTimer = maxOf(k.boostTimer, 0.65f)
                if (k.isPlayer) events.add(RaceEvent(RaceEventType.JUMP, "AIR TIME!"))
            }
        }

        // Track Hazards & Dropped Hazards
        if (k.starTimer <= 0f && k.shieldTimer <= 0f) {
            for (h in track.hazards) {
                val dx = k.x - h.pos.x
                val dz = k.z - h.pos.z
                if (dx * dx + dz * dz < 6.5f && k.y < h.pos.y + 1.2f) {
                    k.slowTimer = maxOf(k.slowTimer, 1.2f)
                    if (k.isPlayer) {
                        playerHitHazard = true
                        cameraShake = 0.25f
                        events.add(RaceEvent(RaceEventType.HAZARD_HIT, "STICKY CARAMEL!"))
                    }
                }
            }

            val dhIter = droppedHazards.iterator()
            while (dhIter.hasNext()) {
                val dh = dhIter.next()
                if (dh.ownerId == k.racer.id && dh.ttl > 10.5f) continue // brief grace for dropper
                val dx = k.x - dh.pos.x
                val dz = k.z - dh.pos.z
                if (dx * dx + dz * dz < 6.2f && abs(k.y - dh.pos.y) < 1.8f) {
                    if (dh.type == PowerUpType.PEPPERMINT_BLAST) {
                        k.spinoutTimer = 1.0f
                    } else {
                        k.slowTimer = 1.8f
                    }
                    dhIter.remove()
                    if (k.isPlayer) {
                        playerHitHazard = true
                        cameraShake = 0.45f
                        events.add(RaceEvent(RaceEventType.HAZARD_HIT, "${dh.type.displayName}!"))
                    }
                    break
                }
            }
        }

        // Item Boxes pickup
        for (box in itemBoxes) {
            if (box.respawnTimer > 0f) continue
            val dx = k.x - box.pos.x
            val dz = k.z - box.pos.z
            if (dx * dx + dz * dz < 7.5f && abs(k.y + 1.0f - box.pos.y) < 2.2f) {
                box.respawnTimer = 5.0f
                if (k.heldPowerUp == null) {
                    val chosen = pickWeightedPowerUp(k)
                    k.heldPowerUp = chosen
                    if (k.isPlayer) {
                        events.add(RaceEvent(RaceEventType.POWERUP_PICKUP, chosen.displayName, chosen))
                    }
                }
            }
        }

        // Checkpoints & Lap Counting (4 sectors: 0->1->2->3->finish)
        val sector = ((proj.progress * 4f).toInt()).coerceIn(0, 3)
        if (sector == k.sectorReached + 1) {
            k.sectorReached = sector
        }
        // Detect crossing finish line forward (prevNode near end, currNode near 0)
        val n = track.nodes.size
        if (k.sectorReached >= 2 && prevNodeIndex > (n * 3) / 4 && proj.nodeIndex < n / 4) {
            val lapTime = raceElapsedMs - k.currentLapStartMs
            if (lapTime > 4000L && lapTime < k.bestLapMs) {
                k.bestLapMs = lapTime
            }
            k.currentLapStartMs = raceElapsedMs
            k.sectorReached = 0

            if (k.currentLap >= k.totalLapsToFinish && mode != GameMode.TIME_TRIAL && mode != GameMode.PRACTICE) {
                k.finished = true
                k.finishTimeMs = raceElapsedMs
                if (k.isPlayer) {
                    playerFinished = true
                    events.add(RaceEvent(RaceEventType.RACE_FINISHED, "FINISH!"))
                }
            } else {
                k.currentLap++
                if (k.isPlayer) {
                    events.add(RaceEvent(RaceEventType.LAP_COMPLETED, "LAP ${k.currentLap}/$totalLaps"))
                }
            }
        }

        k.raceScore = if (k.finished) {
            1000000f - k.finishTimeMs.toFloat() * 0.01f
        } else {
            k.currentLap * 1000f + (k.sectorReached * 200f) + proj.progress * 900f
        }
    }

    private fun activatePowerUp(k: KartRuntimeState, powerUp: PowerUpType, events: MutableList<RaceEvent>) {
        if (k.isPlayer) {
            events.add(RaceEvent(RaceEventType.POWERUP_USED, "${powerUp.displayName}!", powerUp))
        }
        when (powerUp) {
            PowerUpType.SUGAR_BOOST -> {
                k.boostTimer = maxOf(k.boostTimer, 2.2f)
                k.nitroFuel = (k.nitroFuel + 0.35f).coerceAtMost(1.0f)
            }
            PowerUpType.GUMMY_SHIELD -> {
                k.shieldTimer = 6.5f
            }
            PowerUpType.PEPPERMINT_BLAST -> {
                val dropPos = Vec3(
                    k.x - sin(k.yawRad) * 4.2f,
                    k.y + 0.1f,
                    k.z - cos(k.yawRad) * 4.2f
                )
                droppedHazards.add(DroppedHazard(nextHazardId++, k.racer.id, PowerUpType.PEPPERMINT_BLAST, dropPos, 12f))
            }
            PowerUpType.CARAMEL_TRAP -> {
                val dropPos = Vec3(
                    k.x - sin(k.yawRad) * 4.0f,
                    k.y + 0.05f,
                    k.z - cos(k.yawRad) * 4.0f
                )
                droppedHazards.add(DroppedHazard(nextHazardId++, k.racer.id, PowerUpType.CARAMEL_TRAP, dropPos, 14f))
            }
            PowerUpType.JELLY_DASH -> {
                k.boostTimer = maxOf(k.boostTimer, 1.4f)
                k.vy = 6.5f
                k.isGrounded = false
                k.speed += 14f
            }
            PowerUpType.RAINBOW_STAR -> {
                k.starTimer = 5.5f
                k.boostTimer = maxOf(k.boostTimer, 5.5f)
            }
            PowerUpType.CHOCOLATE_SMOKE -> {
                for (other in karts) {
                    if (other.racer.id == k.racer.id) continue
                    if (other.shieldTimer > 0f || other.starTimer > 0f) continue
                    val dx = other.x - k.x
                    val dz = other.z - k.z
                    if (dx * dx + dz * dz < 2200f) {
                        other.smokeTimer = 3.2f
                    }
                }
            }
        }
    }

    private fun pickWeightedPowerUp(k: KartRuntimeState): PowerUpType {
        val all = PowerUpType.entries
        return if (k.position >= 5 && rng.nextFloat() < 0.45f) {
            // Trailing racers slightly favor catch-up candy boosts
            listOf(PowerUpType.SUGAR_BOOST, PowerUpType.RAINBOW_STAR, PowerUpType.JELLY_DASH).random(rng)
        } else if (rng.nextFloat() < 0.25f) {
            k.racer.signaturePowerUp
        } else {
            all[rng.nextInt(all.size)]
        }
    }

    private fun computeAiInput(k: KartRuntimeState, dt: Float): PlayerInputState {
        k.aiLaneTimer -= dt
        if (k.aiLaneTimer <= 0f) {
            k.aiLaneTimer = 1.6f + rng.nextFloat() * 2.2f
            k.aiTargetLaneOffset = (rng.nextFloat() * 2f - 1f) * 6.5f
        }

        // Look ahead along track spline
        val n = track.nodes.size
        val lookAheadNodes = (4 + (k.speed / 12f).toInt()).coerceIn(4, 10)
        val targetNode = track.nodes[(k.nodeHintIndex + lookAheadNodes) % n]

        // Avoid nearby karts & dropped hazards by nudging lane
        var laneOffset = k.aiTargetLaneOffset
        for (other in karts) {
            if (other.racer.id == k.racer.id) continue
            val dx = other.x - k.x
            val dz = other.z - k.z
            val d2 = dx * dx + dz * dz
            if (d2 < 36f) {
                laneOffset += if (k.lateralOffset >= other.lateralOffset) 3.2f else -3.2f
            }
        }
        for (h in droppedHazards) {
            val dx = h.pos.x - k.x
            val dz = h.pos.z - k.z
            if (dx * dx + dz * dz < 100f) {
                laneOffset += if (k.lateralOffset >= 0f) -4.5f else 4.5f
            }
        }
        laneOffset = laneOffset.coerceIn(-(targetNode.halfWidth - 3.0f), targetNode.halfWidth - 3.0f)

        val targetPos = targetNode.center + targetNode.right * laneOffset
        val toTargetX = targetPos.x - k.x
        val toTargetZ = targetPos.z - k.z
        val desiredYaw = atan2(toTargetX, toTargetZ)
        val angleDiff = normalizeAngle(desiredYaw - k.yawRad)

        // Note: steer = +1 turns right (decreases yaw), steer = -1 turns left (increases yaw)
        val steer = (-angleDiff * 1.9f * difficulty.aiSkill).coerceIn(-1f, 1f)
        val sharpCorner = abs(angleDiff) > 0.38f

        // Recover if stuck
        if (k.speed < 4f && raceElapsedMs > 2500L) {
            k.aiStuckTimer += dt
            if (k.aiStuckTimer > 1.4f) {
                val recoverNode = track.nodes[(k.nodeHintIndex + 2) % n]
                k.x = recoverNode.center.x
                k.y = recoverNode.center.y
                k.z = recoverNode.center.z
                k.yawRad = atan2(recoverNode.forward.x, recoverNode.forward.z)
                k.speed = 20f
                k.aiStuckTimer = 0f
            }
        } else {
            k.aiStuckTimer = 0f
        }

        // AI power-up usage
        var usePower = false
        if (k.heldPowerUp != null) {
            k.aiPowerUpDelay -= dt
            if (k.aiPowerUpDelay <= 0f) {
                usePower = true
                k.aiPowerUpDelay = 1.0f + rng.nextFloat() * 1.5f
            }
        }

        val useNitro = k.nitroFuel > 0.45f && !sharpCorner && rng.nextFloat() < difficulty.boostFreq * 0.1f

        return PlayerInputState(
            steer = steer,
            throttle = true,
            brake = sharpCorner && k.speed > 38f && difficulty != Difficulty.EASY,
            drift = sharpCorner && k.speed > 26f && difficulty.aiSkill > 0.65f,
            nitro = useNitro,
            jumpPressed = false,
            usePowerUpPressed = usePower
        )
    }

    private fun resolveKartCollisions(events: MutableList<RaceEvent>) {
        val minDist = 2.55f
        val minDistSq = minDist * minDist
        for (i in 0 until karts.size) {
            for (j in i + 1 until karts.size) {
                val a = karts[i]
                val b = karts[j]
                if (abs(a.y - b.y) > 2.2f) continue
                val dx = b.x - a.x
                val dz = b.z - a.z
                val d2 = dx * dx + dz * dz
                if (d2 in 0.0001f..minDistSq) {
                    val d = sqrt(d2)
                    val nx = dx / d
                    val nz = dz / d
                    val overlap = (minDist - d) * 0.5f

                    a.x -= nx * overlap
                    a.z -= nz * overlap
                    b.x += nx * overlap
                    b.z += nz * overlap

                    // Star / Shield bumps opponent harder!
                    when {
                        a.starTimer > 0f && b.starTimer <= 0f -> {
                            b.spinoutTimer = 0.8f
                        }
                        b.starTimer > 0f && a.starTimer <= 0f -> {
                            a.spinoutTimer = 0.8f
                        }
                        else -> {
                            a.speed *= 0.96f
                            b.speed *= 0.96f
                        }
                    }

                    if (a.isPlayer || b.isPlayer) {
                        cameraShake = maxOf(cameraShake, 0.22f)
                        events.add(RaceEvent(RaceEventType.COLLISION))
                    }
                }
            }
        }
    }

    private fun updateStandings(events: MutableList<RaceEvent>) {
        val sorted = karts.sortedByDescending { it.raceScore }
        sorted.forEachIndexed { idx, kart ->
            kart.position = idx + 1
        }
        if (playerKart.position < lastPlayerPos && raceElapsedMs > 3000L && !playerFinished) {
            events.add(RaceEvent(RaceEventType.OVERTAKE, "OVERTAKE! ${ordinal(playerKart.position)}!"))
        }
        lastPlayerPos = playerKart.position
    }

    fun buildResults(): List<RacerResultEntry> {
        val gpPoints = intArrayOf(15, 12, 10, 8, 6, 4, 2, 1)
        val sorted = karts.sortedBy { it.position }
        return sorted.mapIndexed { idx, k ->
            val estTime = if (k.finished) {
                k.finishTimeMs
            } else {
                // Fair estimated finish time for AI behind the player when race ends
                val remaining = (totalLaps - k.currentLap).coerceAtLeast(0) * 28000L +
                    ((1f - (k.raceScore % 1000f) / 1000f).coerceIn(0.05f, 0.95f) * 24000L).toLong()
                raceElapsedMs + remaining + idx * 450L
            }
            val bestLap = if (k.bestLapMs < Long.MAX_VALUE) k.bestLapMs else (estTime / totalLaps.coerceAtLeast(1))
            RacerResultEntry(
                racerId = k.racer.id,
                racerName = k.racer.name,
                kartName = k.kart.name,
                isPlayer = k.isPlayer,
                position = idx + 1,
                totalTimeMs = estTime,
                bestLapMs = bestLap,
                grandPrixPointsAwarded = if (idx < gpPoints.size) gpPoints[idx] else 0
            )
        }
    }

    private fun normalizeAngle(a: Float): Float {
        var r = a
        while (r > PI) r -= (2.0 * PI).toFloat()
        while (r < -PI) r += (2.0 * PI).toFloat()
        return r
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        return a + normalizeAngle(b - a) * t
    }

    companion object {
        fun ordinal(pos: Int): String = when (pos) {
            1 -> "1st"
            2 -> "2nd"
            3 -> "3rd"
            else -> "${pos}th"
        }

        fun formatTime(ms: Long): String {
            if (ms <= 0L || ms == Long.MAX_VALUE) return "--:--.--"
            val minutes = (ms / 60000L)
            val seconds = (ms % 60000L) / 1000L
            val hundredths = (ms % 1000L) / 10L
            return "%02d:%02d.%02d".format(minutes, seconds, hundredths)
        }
    }
}
