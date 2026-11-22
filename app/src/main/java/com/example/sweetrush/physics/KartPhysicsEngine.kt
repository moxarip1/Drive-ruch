package com.example.sweetrush.physics

import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.Difficulty
import com.example.sweetrush.model.ExhaustStyle
import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.KartDecoration
import com.example.sweetrush.model.KartSpec
import com.example.sweetrush.model.PowerUpType
import com.example.sweetrush.model.RacerResultEntry
import com.example.sweetrush.model.RacerSpec
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.model.VehicleDebugTelemetry
import com.example.sweetrush.model.WheelDesign
import com.example.sweetrush.model.WorldRegion
import com.example.sweetrush.online.RoomPlayerDoc
import com.example.sweetrush.track.GeneratedTrack
import com.example.sweetrush.track.OpenWorldKingdom
import com.example.sweetrush.track.SurfaceType
import com.example.sweetrush.track.TrackGenerator
import com.example.sweetrush.track.Vec3
import com.example.sweetrush.track.WorldCompanyContract
import com.example.sweetrush.track.WorldCompanyDepot
import com.example.sweetrush.track.WorldRacePortal
import com.example.sweetrush.track.WorldSurfaceSampler
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
    var isBraking: Boolean = false,
    var isDrifting: Boolean = false,
    var driftDir: Float = 0f,
    var driftCharge: Float = 0f, // 0..3.0
    var nitroFuel: Float = 0.60f, // 0..1.0
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
    var sectorReached: Int = 0,
    var currentLap: Int = 1,
    var totalLapsToFinish: Int = 3,
    var finished: Boolean = false,
    var finishTimeMs: Long = 0L,
    var currentLapStartMs: Long = 0L,
    var bestLapMs: Long = Long.MAX_VALUE,
    var position: Int = 1,
    var raceScore: Float = 0f,
    // 4-wheel suspension runtime (FL, FR, RL, RR)
    val wheelContacts: BooleanArray = booleanArrayOf(true, true, true, true),
    val wheelCompression: FloatArray = floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f),
    val wheelYOffsets: FloatArray = floatArrayOf(0f, 0f, 0f, 0f),
    var currentSurface: SurfaceType = SurfaceType.ROAD,
    var lateralSlipDeg: Float = 0f,
    var activeLateralGrip: Float = 8.5f,
    var activeForwardGrip: Float = 1.0f,
    var smoothedSteer: Float = 0f,
    var landingImpulse: Float = 0f,
    var justLandedImpulse: Float = 0f,
    // Articulated Character Motion Physics (Spring-Damper G-Force Torso, Head, Arms & Hair/Hat)
    var prevForwardVel: Float = 0f,
    var driverTorsoRollRad: Float = 0f,
    var driverTorsoRollVel: Float = 0f,
    var driverTorsoPitchRad: Float = 0f,
    var driverTorsoPitchVel: Float = 0f,
    var driverBounceY: Float = 0f,
    var driverBounceVel: Float = 0f,
    var driverHeadYawRad: Float = 0f,
    var driverHeadPitchRad: Float = 0f,
    var driverHairRollRad: Float = 0f,
    var driverHairRollVel: Float = 0f,
    var driverHairPitchRad: Float = 0f,
    var driverHairPitchVel: Float = 0f,
    var driverArmRaiseRad: Float = 0f,
    // AI helpers
    var aiTargetLaneOffset: Float = 0f,
    var aiLaneTimer: Float = 0f,
    var aiStuckTimer: Float = 0f,
    var aiPowerUpDelay: Float = 1.5f,
    // Online Multiplayer & Squads (1 / 2 / 4)
    val isRemotePlayer: Boolean = false,
    val remoteUid: String = "",
    val displayName: String = racer.name,
    var teamIndex: Int = 0,
    var remoteTargetX: Float = x,
    var remoteTargetY: Float = y,
    var remoteTargetZ: Float = z,
    var remoteTargetYawRad: Float = yawRad,
    var remoteTargetSpeed: Float = 0f,
    var remoteTargetSteerRad: Float = 0f,
    var remoteVelX: Float = 0f,
    var remoteVelZ: Float = 0f,
    var remoteYawRate: Float = 0f,
    var remoteSyncIntervalSec: Float = 0.08f,
    var remoteLastPacketWallNs: Long = 0L,
    var remoteHasUpdate: Boolean = false,
    var remoteSyncAgeSec: Float = 0f
)

data class OnlineRosterDiff(
    val joinedNames: List<String> = emptyList(),
    val leftNames: List<String> = emptyList()
)

data class OnlineRacerParticipant(
    val userId: String,
    val displayName: String,
    val racerId: String,
    val kartId: String,
    val customization: KartCustomization,
    val teamIndex: Int
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
    RACE_FINISHED,
    COIN_COLLECTED,
    STAR_COLLECTED,
    REGION_DISCOVERED,
    WORLD_CHALLENGE_DONE,
    SARCASTIC_TAUNT,
    COMPANY_CARGO_PICKUP,
    COMPANY_DELIVERY_COMPLETED,
    COMPANY_DIVIDEND_PAID,
    CITIZEN_GREETING
}

data class RaceEvent(
    val type: RaceEventType,
    val message: String = "",
    val powerUp: PowerUpType? = null,
    val stringArg: String = "",
    val regionArg: WorldRegion? = null
)

class KartPhysicsEngine(
    val trackId: TrackId,
    val mode: GameMode,
    val difficulty: Difficulty,
    playerRacerId: String,
    playerKartId: String,
    playerCustomization: KartCustomization,
    startRegion: WorldRegion = WorldRegion.CANDY_CITY,
    initiallyCollectedCoins: Set<String> = emptySet(),
    initiallyCollectedStars: Set<String> = emptySet(),
    initiallyDiscoveredRegions: Set<WorldRegion> = setOf(WorldRegion.CANDY_CITY),
    val isOnlineSession: Boolean = false,
    val teamSize: Int = 1,
    val localTeamIndex: Int = 0,
    val localDisplayName: String = "",
    val onlineParticipants: List<OnlineRacerParticipant> = emptyList(),
    val fillWithAi: Boolean = true,
    initialCompanyLevel: Int = 1,
    initialCompanyContractsCompleted: Int = 0
) {
    val isFreeRoam: Boolean = (mode == GameMode.FREE_ROAM)
    val track: GeneratedTrack = TrackGenerator.getTrack(trackId)
    val trackSpec = CatalogData.trackById(trackId)
    val surfaceSampler: WorldSurfaceSampler = if (isFreeRoam) OpenWorldKingdom else track

    val totalLaps: Int = when (mode) {
        GameMode.TIME_TRIAL, GameMode.PRACTICE, GameMode.FREE_ROAM -> 99
        else -> trackSpec.laps
    }

    val karts: java.util.concurrent.CopyOnWriteArrayList<KartRuntimeState>
    val playerKart: KartRuntimeState
    val itemBoxes: MutableList<ActiveItemBox>
    val droppedHazards: MutableList<DroppedHazard> = mutableListOf()

    // Open-World Free Roam state
    val collectedWorldCoins: MutableSet<String> = initiallyCollectedCoins.toMutableSet()
    val collectedWorldStars: MutableSet<String> = initiallyCollectedStars.toMutableSet()
    val discoveredRegions: MutableSet<WorldRegion> = initiallyDiscoveredRegions.toMutableSet()
    val completedWorldMiniChallenges: MutableSet<String> = mutableSetOf()
    var currentRegion: WorldRegion = startRegion
    var nearbyRacePortal: WorldRacePortal? = null

    // Open-World Sweet Rush Corp. (شركة سويت راش للحلويات) & Living Citizens state
    @Volatile var companyLevel: Int = initialCompanyLevel.coerceIn(1, 10)
    @Volatile var companyContractsCompleted: Int = initialCompanyContractsCompleted.coerceAtLeast(0)
    @Volatile var activeCompanyContractIndex: Int =
        if (OpenWorldKingdom.companyContracts.isNotEmpty()) {
            initialCompanyContractsCompleted.coerceAtLeast(0) % OpenWorldKingdom.companyContracts.size
        } else 0
    @Volatile var companyCargoLoaded: Boolean = false
    private var companyDividendTimerSec: Float = 0f
    private var citizenGreetingCooldownSec: Float = 0f
    private var lastGreetedCitizenId: String = ""
    @Volatile var nearbyCitizenStatusAr: String? = null

    fun activeCompanyContract(): WorldCompanyContract =
        OpenWorldKingdom.companyContracts[activeCompanyContractIndex % OpenWorldKingdom.companyContracts.size]

    fun activeCompanyTargetDepot(): WorldCompanyDepot {
        val contract = activeCompanyContract()
        val targetId = if (companyCargoLoaded) contract.deliveryDepotId else contract.pickupDepotId
        return OpenWorldKingdom.companyDepotById(targetId)
    }

    var countdownSeconds: Float = if (mode == GameMode.PRACTICE || isFreeRoam) 0f else 3.4f
    var raceStarted: Boolean = (mode == GameMode.PRACTICE || isFreeRoam)
    var raceElapsedMs: Long = 0L
    var playerFinished: Boolean = false
    var playerDriftBoostCount: Int = 0
    var playerHitHazard: Boolean = false
    var cameraShake: Float = 0f

    var latestTelemetry: VehicleDebugTelemetry = VehicleDebugTelemetry()
        private set

    private var lastCountdownInt = 4
    private var lastPlayerPos = 8
    private var nextHazardId = 1
    private var tauntCooldownSec = if (mode == GameMode.FREE_ROAM) 0f else 1.5f
    private val rng = Random(System.currentTimeMillis())

    init {
        val playerRacer = CatalogData.racerById(playerRacerId)
        val pKartSpec = CatalogData.kartById(playerKartId)
        val aiRacers = CatalogData.racers.filter { it.id != playerRacerId }.shuffled(Random(trackId.ordinal * 31 + 7))
        val roster = mutableListOf<KartRuntimeState>()
        val resolvedPlayerName = localDisplayName.ifBlank { playerRacer.name }
        val validTeamSize = if (teamSize in listOf(1, 2, 4)) teamSize else 1
        val numTeams = (8 / validTeamSize).coerceIn(1, 4)

        if (isFreeRoam) {
            val spawnPos = freeRoamSpawnForRegion(startRegion)
            val spawnYaw = when (startRegion) {
                WorldRegion.CANDY_CITY -> 0f
                else -> atan2(-spawnPos.x, -spawnPos.z)
            }
            roster.add(
                KartRuntimeState(
                    racer = playerRacer,
                    kart = pKartSpec,
                    customization = playerCustomization,
                    isPlayer = true,
                    x = spawnPos.x,
                    y = spawnPos.y,
                    z = spawnPos.z,
                    yawRad = spawnYaw,
                    totalLapsToFinish = 99,
                    position = 1,
                    displayName = resolvedPlayerName,
                    teamIndex = localTeamIndex.coerceIn(0, numTeams - 1)
                )
            )
            // Online friends join near the local player in Open World Free Roam!
            onlineParticipants.forEachIndexed { idx, part ->
                val offsetSide = ((idx % 2) * 2 - 1) * (4.2f + (idx / 2) * 2.5f)
                val offsetBack = 4.5f + (idx / 2) * 5.2f
                val rx = spawnPos.x + cos(spawnYaw) * offsetSide - sin(spawnYaw) * offsetBack
                val rz = spawnPos.z - sin(spawnYaw) * offsetSide - cos(spawnYaw) * offsetBack
                val ry = OpenWorldKingdom.sampleSurface(rx, rz).height
                roster.add(
                    KartRuntimeState(
                        racer = CatalogData.racerById(part.racerId),
                        kart = CatalogData.kartById(part.kartId),
                        customization = part.customization,
                        isPlayer = false,
                        x = rx,
                        y = ry,
                        z = rz,
                        yawRad = spawnYaw,
                        totalLapsToFinish = 99,
                        position = idx + 2,
                        isRemotePlayer = true,
                        remoteUid = part.userId,
                        displayName = part.displayName,
                        teamIndex = part.teamIndex.coerceIn(0, numTeams - 1)
                    )
                )
            }
            // Roaming AI Movie Drivers cruising the Castle Plaza & Grand Ring Highway in their signature movie karts
            if (!isOnlineSession || fillWithAi) {
                val ringPts = OpenWorldKingdom.ringRoad.points
                for (slot in aiRacers.indices) {
                    val pIdx = ((slot * ringPts.size) / aiRacers.size.coerceAtLeast(1)) % ringPts.size
                    val pt = ringPts[pIdx]
                    val r = aiRacers[slot]
                    val ks = CatalogData.kartById(r.favoriteKartId)
                    val sx: Float
                    val sz: Float
                    val sy: Float
                    val sYaw: Float
                    if (slot < 3) {
                        // Place 3 iconic movie racers right ahead of the player's Free Roam spawn so they are immediately visible!
                        val side = (slot - 1) * 4.2f
                        val ahead = 15f + slot * 7.5f
                        sx = spawnPos.x + cos(spawnYaw) * side + sin(spawnYaw) * ahead
                        sz = spawnPos.z - sin(spawnYaw) * side + cos(spawnYaw) * ahead
                        sy = OpenWorldKingdom.sampleSurface(sx, sz).height
                        sYaw = spawnYaw
                    } else {
                        sx = pt.pos.x
                        sy = pt.pos.y
                        sz = pt.pos.z
                        sYaw = atan2(pt.forward.x, pt.forward.z)
                    }
                    roster.add(
                        KartRuntimeState(
                            racer = r,
                            kart = ks,
                            customization = KartCustomization(),
                            isPlayer = false,
                            x = sx,
                            y = sy,
                            z = sz,
                            yawRad = sYaw,
                            nodeHintIndex = pIdx,
                            totalLapsToFinish = 99,
                            position = roster.size + 1,
                            teamIndex = (slot % numTeams)
                        )
                    )
                }
            }
            itemBoxes = mutableListOf()
        } else {
            val startNode = track.nodes[0]
            val baseYaw = atan2(startNode.forward.x, startNode.forward.z)

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
                        position = 1,
                        displayName = resolvedPlayerName,
                        teamIndex = localTeamIndex.coerceIn(0, numTeams - 1)
                    )
                )
            } else if (isOnlineSession) {
                // Build balanced squad grid: human remote players + local player + optional AI bots
                val totalSlots = if (fillWithAi) 8 else (1 + onlineParticipants.size).coerceAtMost(8)
                val teamCounts = IntArray(numTeams)
                val humanTeam = localTeamIndex.coerceIn(0, numTeams - 1)
                teamCounts[humanTeam]++
                onlineParticipants.take(7).forEach { part ->
                    val t = part.teamIndex.coerceIn(0, numTeams - 1)
                    teamCounts[t]++
                }

                // Helper to assign AI bots to least-populated teams so squads are balanced
                fun nextAiTeam(): Int {
                    var bestTeam = 0
                    var bestCount = Int.MAX_VALUE
                    for (t in 0 until numTeams) {
                        if (teamCounts[t] < bestCount) {
                            bestCount = teamCounts[t]
                            bestTeam = t
                        }
                    }
                    teamCounts[bestTeam]++
                    return bestTeam
                }

                val humansSorted = (listOf(null) + onlineParticipants.take(7)) // null marks local player
                for (slot in 0 until totalSlots) {
                    val row = slot / 2
                    val col = if (slot % 2 == 0) -3.8f else 3.8f
                    val backDist = 5f + row * 7.5f
                    val spawnPos = startNode.center - startNode.forward * backDist + startNode.right * col

                    val humanIndex = totalSlots - 1 - slot
                    if (humanIndex in humansSorted.indices) {
                        val part = humansSorted[humanIndex]
                        if (part == null) {
                            roster.add(
                                KartRuntimeState(
                                    racer = playerRacer,
                                    kart = pKartSpec,
                                    customization = playerCustomization,
                                    isPlayer = true,
                                    x = spawnPos.x,
                                    y = spawnPos.y,
                                    z = spawnPos.z,
                                    yawRad = baseYaw,
                                    totalLapsToFinish = totalLaps,
                                    position = slot + 1,
                                    displayName = resolvedPlayerName,
                                    teamIndex = humanTeam
                                )
                            )
                        } else {
                            roster.add(
                                KartRuntimeState(
                                    racer = CatalogData.racerById(part.racerId),
                                    kart = CatalogData.kartById(part.kartId),
                                    customization = part.customization,
                                    isPlayer = false,
                                    x = spawnPos.x,
                                    y = spawnPos.y,
                                    z = spawnPos.z,
                                    yawRad = baseYaw,
                                    totalLapsToFinish = totalLaps,
                                    position = slot + 1,
                                    isRemotePlayer = true,
                                    remoteUid = part.userId,
                                    displayName = part.displayName,
                                    teamIndex = part.teamIndex.coerceIn(0, numTeams - 1)
                                )
                            )
                        }
                    } else {
                        val aiSpec = aiRacers[slot % aiRacers.size]
                        roster.add(
                            KartRuntimeState(
                                racer = aiSpec,
                                kart = CatalogData.kartById(aiSpec.favoriteKartId),
                                customization = KartCustomization(),
                                isPlayer = false,
                                x = spawnPos.x,
                                y = spawnPos.y,
                                z = spawnPos.z,
                                yawRad = baseYaw,
                                totalLapsToFinish = totalLaps,
                                position = slot + 1,
                                aiTargetLaneOffset = col * 0.7f,
                                teamIndex = nextAiTeam()
                            )
                        )
                    }
                }
            } else {
                for (slot in 0 until 8) {
                    val isPlayerSlot = (slot == 7)
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
            itemBoxes = track.itemBoxes.map {
                ActiveItemBox(it.id, it.pos, it.yawRad, 0f)
            }.toMutableList()
        }

        karts = java.util.concurrent.CopyOnWriteArrayList(roster)
        playerKart = karts.first { it.isPlayer }
        lastPlayerPos = playerKart.position
        discoveredRegions.add(currentRegion)
    }

    private fun freeRoamSpawnForRegion(region: WorldRegion): Vec3 {
        val sample = when (region) {
            WorldRegion.CANDY_CITY -> Vec3(0f, 0f, -22f)
            WorldRegion.CHOCOLATE_MOUNTAINS -> Vec3(245f, 0f, -210f)
            WorldRegion.GUMDROP_FOREST -> Vec3(-245f, 0f, -195f)
            WorldRegion.CUPCAKE_VALLEY -> Vec3(-230f, 0f, 200f)
            WorldRegion.PEPPERMINT_COAST -> Vec3(245f, 0f, 210f)
        }
        val h = OpenWorldKingdom.sampleSurface(sample.x, sample.z).height
        return Vec3(sample.x, h, sample.z)
    }

    fun teleportPlayerToRegion(region: WorldRegion) {
        if (!isFreeRoam) return
        val pos = freeRoamSpawnForRegion(region)
        playerKart.x = pos.x
        playerKart.y = pos.y
        playerKart.z = pos.z
        playerKart.vx = 0f
        playerKart.vy = 0f
        playerKart.vz = 0f
        playerKart.speed = 0f
        playerKart.yawRad = if (region == WorldRegion.CANDY_CITY) 0f else atan2(-pos.x, -pos.z)
        currentRegion = region
        discoveredRegions.add(region)
    }

    fun step(dtRaw: Float, input: PlayerInputState): List<RaceEvent> {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        val events = mutableListOf<RaceEvent>()
        cameraShake = (cameraShake - dt * 2.5f).coerceAtLeast(0f)
        if (tauntCooldownSec > 0f) tauntCooldownSec = (tauntCooldownSec - dt).coerceAtLeast(0f)

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
                playerKart.wheelSpinRad += if (input.throttle) dt * 6f else 0f
                updateTelemetrySnapshot()
                return events
            }
        }

        raceElapsedMs += (dt * 1000f).toLong()

        for (box in itemBoxes) {
            if (box.respawnTimer > 0f) {
                box.respawnTimer = (box.respawnTimer - dt).coerceAtLeast(0f)
            }
        }

        val hazIter = droppedHazards.iterator()
        while (hazIter.hasNext()) {
            val h = hazIter.next()
            h.ttl -= dt
            if (h.ttl <= 0f) hazIter.remove()
        }

        for (kart in karts) {
            if (kart.isRemotePlayer) {
                stepRemoteKart(kart, dt)
            } else {
                val kartInput = when {
                    kart.isPlayer && !kart.finished -> input
                    isFreeRoam -> computeRoamingAiInput(kart, dt)
                    else -> computeTrackAiInput(kart, dt)
                }
                stepSingleKart(kart, kartInput, dt, events)
            }
        }

        resolveKartCollisions(events)

        if (isFreeRoam) {
            stepOpenWorldInteractions(dt, events)
        } else {
            updateStandings(events)
        }

        updateTelemetrySnapshot()
        return events
    }

    fun updateRemoteStates(remotePlayers: List<RoomPlayerDoc>): OnlineRosterDiff {
        if (!isOnlineSession) return OnlineRosterDiff()
        val nowNs = System.nanoTime()
        val byUid = remotePlayers.associateBy { it.userId }
        val leftNames = mutableListOf<String>()
        val joinedNames = mutableListOf<String>()

        // 1. Remove any remote karts whose player left the room or open world
        val existingRemotes = karts.filter { it.isRemotePlayer }
        for (existing in existingRemotes) {
            if (!byUid.containsKey(existing.remoteUid)) {
                leftNames.add(existing.displayName)
                karts.remove(existing)
            }
        }

        val existingByUid = karts.filter { it.isRemotePlayer }.associateBy { it.remoteUid }.toMutableMap()

        // 2. Dynamically spawn any new friend who joined mid-session (e.g. Gathering Hub / Open World / Room)
        val validTeamSize = if (teamSize in listOf(1, 2, 4)) teamSize else 1
        val numTeams = (8 / validTeamSize).coerceIn(1, 4)
        for (doc in remotePlayers) {
            if (doc.userId.isBlank() || existingByUid.containsKey(doc.userId)) continue
            val hasValidCoords = abs(doc.x) > 0.01 || abs(doc.z) > 0.01
            val spawnX = if (hasValidCoords) doc.x.toFloat() else playerKart.x + cos(playerKart.yawRad) * 4.2f - sin(playerKart.yawRad) * 4.5f
            val spawnZ = if (hasValidCoords) doc.z.toFloat() else playerKart.z - sin(playerKart.yawRad) * 4.2f - cos(playerKart.yawRad) * 4.5f
            val spawnY = if (hasValidCoords) doc.y.toFloat() else surfaceSampler.sampleSurface(spawnX, spawnZ, playerKart.y).height
            val spawnYaw = if (hasValidCoords) doc.yawRad.toFloat() else playerKart.yawRad

            val custom = KartCustomization(
                paintId = doc.paintId.ifBlank { "default" },
                wheelDesign = enumValues<WheelDesign>().firstOrNull { it.name == doc.wheelId } ?: WheelDesign.PEPPERMINT_SWIRL,
                exhaustStyle = enumValues<ExhaustStyle>().firstOrNull { it.name == doc.exhaustId } ?: ExhaustStyle.SUGAR_SPARK,
                decoration = enumValues<KartDecoration>().firstOrNull { it.name == doc.decoId } ?: KartDecoration.NONE
            )
            val newKart = KartRuntimeState(
                racer = CatalogData.racerById(doc.racerId),
                kart = CatalogData.kartById(doc.kartId),
                customization = custom,
                isPlayer = false,
                x = spawnX,
                y = spawnY,
                z = spawnZ,
                yawRad = spawnYaw,
                totalLapsToFinish = totalLaps,
                position = (karts.size + 1).coerceAtMost(8),
                isRemotePlayer = true,
                remoteUid = doc.userId,
                displayName = doc.displayName.ifBlank { "Friend" },
                teamIndex = doc.teamIndex.toInt().coerceIn(0, numTeams - 1),
                remoteTargetX = spawnX,
                remoteTargetY = spawnY,
                remoteTargetZ = spawnZ,
                remoteTargetYawRad = spawnYaw,
                remoteTargetSpeed = doc.speed.toFloat(),
                remoteTargetSteerRad = doc.steerVisualRad.toFloat(),
                remoteVelX = sin(spawnYaw) * doc.speed.toFloat(),
                remoteVelZ = cos(spawnYaw) * doc.speed.toFloat(),
                remoteLastPacketWallNs = nowNs,
                remoteHasUpdate = hasValidCoords
            )
            karts.add(newKart)
            existingByUid[doc.userId] = newKart
            joinedNames.add(newKart.displayName)
        }

        // 3. Update target pose & compute smooth velocity / angular velocity vectors for existing remote karts
        for (k in karts) {
            if (!k.isRemotePlayer) continue
            val doc = byUid[k.remoteUid] ?: continue
            val inX = doc.x.toFloat()
            val inY = doc.y.toFloat()
            val inZ = doc.z.toFloat()
            val inYaw = doc.yawRad.toFloat()
            val inSpeed = doc.speed.toFloat()

            // Ignore uninitialized default (0,0,0) lobby document before remote player sends first telemetry packet
            val isDefaultZero = abs(inX) < 0.001f && abs(inY) < 0.001f && abs(inZ) < 0.001f && abs(inSpeed) < 0.001f
            if (isDefaultZero && !k.remoteHasUpdate) {
                k.teamIndex = doc.teamIndex.toInt().coerceIn(0, 3)
                continue
            }

            val packetDt = if (k.remoteLastPacketWallNs > 0L) {
                ((nowNs - k.remoteLastPacketWallNs) / 1_000_000_000.0f).coerceIn(0.025f, 0.45f)
            } else {
                0.055f
            }

            // Skip duplicate snapshot emission if coordinates & yaw did not change
            val movedSq = (inX - k.remoteTargetX) * (inX - k.remoteTargetX) + (inZ - k.remoteTargetZ) * (inZ - k.remoteTargetZ)
            val yawDiff = normalizeAngle(inYaw - k.remoteTargetYawRad)
            if (k.remoteHasUpdate && movedSq < 0.00002f && abs(yawDiff) < 0.0002f && abs(inSpeed - k.remoteTargetSpeed) < 0.005f) {
                k.teamIndex = doc.teamIndex.toInt().coerceIn(0, 3)
                k.isDrifting = doc.drifting
                k.isNitroActive = doc.nitroActive
                continue
            }

            k.remoteLastPacketWallNs = nowNs
            k.remoteSyncIntervalSec = (k.remoteSyncIntervalSec * 0.60f + packetDt * 0.40f).coerceIn(0.035f, 0.35f)

            val headingVx = sin(inYaw) * inSpeed
            val headingVz = cos(inYaw) * inSpeed
            if (k.remoteHasUpdate && movedSq < 900f) {
                val obsVx = (inX - k.remoteTargetX) / packetDt
                val obsVz = (inZ - k.remoteTargetZ) / packetDt
                // Blend heading velocity with observed chord velocity for accurate cornering & drift vectors
                k.remoteVelX = headingVx * 0.72f + obsVx * 0.28f
                k.remoteVelZ = headingVz * 0.72f + obsVz * 0.28f
                val obsYawRate = (yawDiff / packetDt).coerceIn(-4.2f, 4.2f)
                val steerYawRate = (-doc.steerVisualRad.toFloat() * 3.1f * (inSpeed / 24f).coerceIn(0f, 1.4f)).coerceIn(-4.2f, 4.2f)
                k.remoteYawRate = obsYawRate * 0.65f + steerYawRate * 0.35f
            } else {
                k.remoteVelX = headingVx
                k.remoteVelZ = headingVz
                k.remoteYawRate = 0f
            }

            if (!k.remoteHasUpdate) {
                k.x = inX
                k.y = inY
                k.z = inZ
                k.yawRad = inYaw
                k.speed = inSpeed
                k.vx = headingVx
                k.vz = headingVz
            }

            k.teamIndex = doc.teamIndex.toInt().coerceIn(0, 3)
            k.remoteTargetX = inX
            k.remoteTargetY = inY
            k.remoteTargetZ = inZ
            k.remoteTargetYawRad = inYaw
            k.remoteTargetSpeed = inSpeed
            k.remoteTargetSteerRad = doc.steerVisualRad.toFloat()
            k.remoteSyncAgeSec = 0f
            k.remoteHasUpdate = true
            k.currentLap = doc.lap.toInt().coerceIn(1, totalLaps)
            k.raceScore = doc.raceScore.toFloat()
            k.finished = doc.finished
            if (doc.finished && doc.finishTimeMs > 0L) {
                k.finishTimeMs = doc.finishTimeMs
            }
            k.isDrifting = doc.drifting
            k.isNitroActive = doc.nitroActive
            if (doc.nitroActive) {
                k.boostTimer = maxOf(k.boostTimer, 0.25f)
            }
            if (doc.drifting) {
                k.driftDir = if (doc.steerVisualRad < -0.02) 1f else if (doc.steerVisualRad > 0.02) -1f else k.driftDir
                k.driftCharge = (k.driftCharge + packetDt * 1.3f).coerceAtMost(2.5f)
            } else {
                k.driftCharge = 0f
            }
        }

        return OnlineRosterDiff(joinedNames = joinedNames, leftNames = leftNames)
    }

    private fun stepRemoteKart(k: KartRuntimeState, dt: Float) {
        if (!k.remoteHasUpdate) {
            // Keep idle wheels/suspension grounded before first packet arrives
            val groundY = surfaceSampler.sampleSurface(k.x, k.z, k.y).height
            k.y += (groundY - k.y) * (14f * dt).coerceIn(0.08f, 0.75f)
            return
        }
        k.remoteSyncAgeSec = (k.remoteSyncAgeSec + dt).coerceAtMost(0.95f)

        // Continuous Curved Dead-Reckoning up to 0.70s with smooth velocity decay so network jitter never causes stuttering
        val maxExtrap = (k.remoteSyncIntervalSec * 3.2f).coerceIn(0.22f, 0.70f)
        val extrapTime = k.remoteSyncAgeSec.coerceAtMost(maxExtrap)
        val decay = if (k.remoteSyncAgeSec <= k.remoteSyncIntervalSec * 1.5f) {
            1.0f
        } else {
            (1.0f - (k.remoteSyncAgeSec - k.remoteSyncIntervalSec * 1.5f) * 1.15f).coerceIn(0.15f, 1.0f)
        }

        val predYaw = k.remoteTargetYawRad + k.remoteYawRate * extrapTime * 0.75f * decay
        // Integrate curved velocity vector along predicted yaw arc
        val midYaw = k.remoteTargetYawRad + k.remoteYawRate * extrapTime * 0.38f * decay
        val curvedVx = sin(midYaw) * k.remoteTargetSpeed * 0.65f + k.remoteVelX * 0.35f
        val curvedVz = cos(midYaw) * k.remoteTargetSpeed * 0.65f + k.remoteVelZ * 0.35f

        val predX = k.remoteTargetX + curvedVx * extrapTime * decay
        val predZ = k.remoteTargetZ + curvedVz * extrapTime * decay

        val errX = predX - k.x
        val errZ = predZ - k.z
        val distSq = errX * errX + errZ * errZ

        if (distSq > 3600f) {
            // Hard snap only if teleported > 60m (e.g. region fast-travel)
            k.x = predX
            k.y = k.remoteTargetY
            k.z = predZ
            k.yawRad = predYaw
            k.vx = curvedVx
            k.vz = curvedVz
        } else {
            // Adaptive Hermite spring + velocity integration: glides seamlessly at 60fps
            val catchupRate = when {
                distSq > 64f -> 20.0f
                distSq > 9f -> 15.5f
                else -> 12.0f
            }
            val corrAlpha = (catchupRate * dt).coerceIn(0.05f, 0.68f)
            k.x += curvedVx * decay * dt + errX * corrAlpha
            k.z += curvedVz * decay * dt + errZ * corrAlpha

            val yawAlpha = (16.0f * dt).coerceIn(0.08f, 0.75f)
            k.yawRad = lerpAngle(k.yawRad + k.remoteYawRate * decay * 0.65f * dt, predYaw, yawAlpha)

            val groundY = surfaceSampler.sampleSurface(k.x, k.z, k.y).height
            val targetY = maxOf(k.remoteTargetY, groundY)
            k.y += (targetY - k.y) * (18f * dt).coerceIn(0.10f, 0.85f)
            k.isGrounded = (k.y - groundY) <= 0.26f
        }

        // Smooth speed, wheel rotation, 4-wheel suspension & chassis roll/pitch so remote character & kart look 100% alive
        k.speed += (k.remoteTargetSpeed * decay - k.speed) * (16f * dt).coerceIn(0.12f, 0.90f)
        k.vx = sin(k.yawRad) * k.speed
        k.vz = cos(k.yawRad) * k.speed
        k.steerVisualRad += (k.remoteTargetSteerRad - k.steerVisualRad) * (18f * dt).coerceAtMost(1f)
        k.wheelSpinRad += k.speed * dt * 1.45f
        if (k.boostTimer > 0f && !k.isNitroActive) {
            k.boostTimer = (k.boostTimer - dt).coerceAtLeast(0f)
        }

        val targetRoll = (if (k.isDrifting) -k.steerVisualRad * 0.34f else -k.steerVisualRad * 0.18f).coerceIn(-0.22f, 0.22f)
        k.bodyRollRad += (targetRoll - k.bodyRollRad) * (13f * dt).coerceAtMost(1f)
        val targetPitch = when {
            !k.isGrounded -> ((k.remoteTargetY - k.y) * 0.18f).coerceIn(-0.22f, 0.22f)
            k.isNitroActive -> -0.052f
            else -> 0f
        }
        k.bodyPitchRad += (targetPitch - k.bodyPitchRad) * (11f * dt).coerceAtMost(1f)

        val fwdX = sin(k.yawRad)
        val fwdZ = cos(k.yawRad)
        val rightX = cos(k.yawRad)
        val rightZ = -sin(k.yawRad)
        val wheelLocalOffsets = arrayOf(
            -0.92f to 0.96f,
            0.92f to 0.96f,
            -0.96f to -0.96f,
            0.96f to -0.96f
        )
        for (i in 0 until 4) {
            val (lx, lz) = wheelLocalOffsets[i]
            val wx = k.x + rightX * lx + fwdX * lz
            val wz = k.z + rightZ * lz + fwdZ * lz
            val wGround = surfaceSampler.sampleSurface(wx, wz, k.y).height
            val clearance = k.y - wGround
            val comp = ((0.58f - clearance) / 0.58f).coerceIn(0f, 1f)
            k.wheelCompression[i] = comp
            k.wheelContacts[i] = clearance <= 0.24f
            val targetOffset = if (k.wheelContacts[i]) (comp - 0.5f) * 0.18f else -0.08f
            k.wheelYOffsets[i] += (targetOffset - k.wheelYOffsets[i]) * (18f * dt).coerceAtMost(1f)
        }

        // Update track progress & lap score in real time for closed-track races
        if (!isFreeRoam) {
            val proj = track.projectOntoTrack(k.x, k.z, k.nodeHintIndex)
            k.nodeHintIndex = proj.nodeIndex
            k.lateralOffset = proj.lateralOffset
            val computedScore = k.currentLap * 1000f + proj.progress * 900f
            if (!k.finished) {
                k.raceScore = maxOf(k.raceScore, computedScore)
            }
        }
        stepCharacterMotionPhysics(k, dt, k.remoteYawRate * dt)
    }

    private fun stepSingleKart(
        k: KartRuntimeState,
        input: PlayerInputState,
        dt: Float,
        events: MutableList<RaceEvent>
    ) {
        val stats = CatalogData.effectiveStats(k.kart, k.customization)
        val tuning = k.kart.physicsTuning

        if (k.boostTimer > 0f) k.boostTimer = (k.boostTimer - dt).coerceAtLeast(0f)
        if (k.shieldTimer > 0f) k.shieldTimer = (k.shieldTimer - dt).coerceAtLeast(0f)
        if (k.starTimer > 0f) k.starTimer = (k.starTimer - dt).coerceAtLeast(0f)
        if (k.slowTimer > 0f) k.slowTimer = (k.slowTimer - dt).coerceAtLeast(0f)
        if (k.smokeTimer > 0f) k.smokeTimer = (k.smokeTimer - dt).coerceAtLeast(0f)
        if (k.spinoutTimer > 0f) k.spinoutTimer = (k.spinoutTimer - dt).coerceAtLeast(0f)

        if (input.usePowerUpPressed && k.heldPowerUp != null) {
            activatePowerUp(k, k.heldPowerUp!!, events)
            k.heldPowerUp = null
        }

        val centerSample = surfaceSampler.sampleSurface(k.x, k.z, k.y)
        k.currentSurface = centerSample.surface
        val surfaceGripMult = centerSample.surface.gripMultiplier

        val maxBaseSpeed = (44f + stats.speed * 14f) * (if (k.isPlayer || isFreeRoam) 1.0f else difficulty.aiSpeedMultiplier)
        val accelRate = 25f + stats.acceleration * 17f
        val steerRate = (1.75f + stats.handling * 0.90f) * tuning.steerResponse

        val isBoosting = k.boostTimer > 0f || k.starTimer > 0f ||
            (input.nitro && k.nitroFuel > 0.03f && k.speed > 7f)

        k.isNitroActive = input.nitro && k.nitroFuel > 0.03f && k.speed > 7f && k.spinoutTimer <= 0f
        if (k.isNitroActive) {
            if (k.isPlayer && k.nitroFuel > 0.95f) events.add(RaceEvent(RaceEventType.NITRO_START))
            k.nitroFuel = (k.nitroFuel - dt * (0.26f - stats.boost * 0.08f)).coerceAtLeast(0f)
        } else if (abs(k.speed) > 16f) {
            k.nitroFuel = (k.nitroFuel + dt * 0.035f).coerceAtMost(1.0f)
        }

        val speedMultiplier = when {
            k.spinoutTimer > 0f -> 0.25f
            k.slowTimer > 0f -> 0.56f
            k.starTimer > 0f -> 1.38f
            isBoosting -> 1.32f + stats.boost * 0.08f
            centerSample.surface == SurfaceType.WATER -> 0.72f
            centerSample.surface == SurfaceType.OFFROAD -> 0.88f
            else -> 1.0f
        }
        val targetTopSpeed = maxBaseSpeed * speedMultiplier

        // Decompose current world horizontal velocity (vx, vz) into kart forward & right vectors!
        val fwdX = sin(k.yawRad)
        val fwdZ = cos(k.yawRad)
        val rightX = cos(k.yawRad)
        val rightZ = -sin(k.yawRad)

        var forwardVel = k.vx * fwdX + k.vz * fwdZ
        var lateralVel = k.vx * rightX + k.vz * rightZ

        k.isBraking = input.brake && k.spinoutTimer <= 0f

        // Longitudinal acceleration / braking / rolling drag
        if (k.spinoutTimer <= 0f) {
            if (input.throttle || isBoosting) {
                val boostExtra = if (isBoosting) 2.15f else 1.0f
                forwardVel += accelRate * boostExtra * dt
            } else if (input.brake) {
                forwardVel -= if (forwardVel > 2f) 44f * dt else 19f * dt
            } else {
                forwardVel -= forwardVel * 1.10f * dt
            }
        } else {
            forwardVel -= forwardVel * 2.4f * dt
        }

        forwardVel = forwardVel.coerceIn(-16f, targetTopSpeed)
        if (forwardVel > targetTopSpeed) {
            forwardVel -= (forwardVel - targetTopSpeed) * 3.0f * dt
        }

        // Speed-sensitive steering interpolation (responsive at low speed, planted at high speed)
        val rawSteer = input.steer.coerceIn(-1f, 1f)
        val speedNormSteer = (abs(forwardVel) / 56f).coerceIn(0f, 1f)
        val steerLerpRate = (14.5f - speedNormSteer * 6.5f) * tuning.steerResponse
        k.smoothedSteer += (rawSteer - k.smoothedSteer) * (steerLerpRate * dt).coerceIn(0f, 1f)
        val steerInput = k.smoothedSteer

        // Progressive Drifting & Charging (supports hop on start without cancelling on frame 2!)
        val canContinueDrift = k.isDrifting && input.drift && abs(forwardVel) > 12f && k.spinoutTimer <= 0f
        val canStartDrift = !k.isDrifting && input.drift && k.isGrounded && forwardVel > 17f && k.spinoutTimer <= 0f && abs(rawSteer) > 0.16f
        if (canStartDrift) {
            k.isDrifting = true
            k.driftDir = if (rawSteer > 0f) 1f else -1f
            k.vy = 2.9f
            k.isGrounded = false
            // Smooth progressive drift tail-out
            lateralVel -= k.driftDir * 4.2f
        }
        if (canStartDrift || canContinueDrift) {
            k.driftCharge = (k.driftCharge + dt * (0.95f + abs(steerInput) * 0.45f)).coerceAtMost(3.0f)
            k.nitroFuel = (k.nitroFuel + dt * 0.14f).coerceAtMost(1.0f)
        } else if (k.isDrifting) {
            if (k.driftCharge >= 0.7f) {
                val boostDur = when {
                    k.driftCharge >= 2.2f -> 1.65f
                    k.driftCharge >= 1.35f -> 1.15f
                    else -> 0.70f
                }
                k.boostTimer = maxOf(k.boostTimer, boostDur)
                forwardVel = maxOf(forwardVel, maxBaseSpeed * 1.24f)
                if (k.isPlayer) {
                    playerDriftBoostCount++
                    events.add(RaceEvent(RaceEventType.DRIFT_RELEASE, if (k.driftCharge >= 2.2f) "SUPER DRIFT!" else "DRIFT BOOST!"))
                }
            }
            k.isDrifting = false
            k.driftCharge = 0f
            k.driftDir = 0f
        }

        // Progressive Yaw Steering
        val speedFactor = (abs(forwardVel) / 14f).coerceIn(0.22f, 1.0f)
        val reverseSign = if (forwardVel < -1f) -1f else 1f
        val effectiveSteer = if (k.isDrifting) {
            k.driftDir * 0.78f + steerInput * 0.62f
        } else {
            steerInput
        }
        val airControl = if (k.isGrounded) 1.0f else 0.55f
        val yawDelta = -effectiveSteer * steerRate * speedFactor * reverseSign * airControl * dt
        k.yawRad += yawDelta

        // Recompute new forward & right after yaw rotation, applying tire lateral grip!
        val newFwdX = sin(k.yawRad)
        val newFwdZ = cos(k.yawRad)
        val newRightX = cos(k.yawRad)
        val newRightZ = -sin(k.yawRad)

        // When yaw rotates while carrying momentum, some forward speed spills into lateral slip
        lateralVel += forwardVel * sin(-yawDelta) * 0.72f
        val baseGrip = if (k.isDrifting) tuning.driftGrip else tuning.lateralGrip
        val effLateralGrip = baseGrip * surfaceGripMult * (0.9f + stats.stability * 0.25f)
        if (k.isGrounded) {
            lateralVel -= lateralVel * (effLateralGrip * dt).coerceAtMost(0.95f)
        } else {
            lateralVel -= lateralVel * 0.35f * dt
        }

        k.activeLateralGrip = effLateralGrip
        k.activeForwardGrip = surfaceGripMult
        k.lateralSlipDeg = Math.toDegrees(atan2(lateralVel.toDouble(), forwardVel.coerceAtLeast(0.5f).toDouble())).toFloat()

        // Recombine world horizontal velocity
        k.vx = newFwdX * forwardVel + newRightX * lateralVel
        k.vz = newFwdZ * forwardVel + newRightZ * lateralVel
        k.speed = forwardVel

        // Jump input
        if (input.jumpPressed && k.isGrounded && k.spinoutTimer <= 0f) {
            k.vy = 9.4f
            k.isGrounded = false
            if (k.isPlayer) events.add(RaceEvent(RaceEventType.JUMP))
        }

        k.x += k.vx * dt
        k.z += k.vz * dt

        // World / Track boundary enforcement
        var prevNodeIndex = k.nodeHintIndex
        if (isFreeRoam) {
            val bound = OpenWorldKingdom.WORLD_HALF_EXTENT - 24f
            if (k.x < -bound) { k.x = -bound; k.vx = abs(k.vx) * 0.4f }
            if (k.x > bound) { k.x = bound; k.vx = -abs(k.vx) * 0.4f }
            if (k.z < -bound) { k.z = -bound; k.vz = abs(k.vz) * 0.4f }
            if (k.z > bound) { k.z = bound; k.vz = -abs(k.vz) * 0.4f }
        } else {
            val proj = track.projectOntoTrack(k.x, k.z, k.nodeHintIndex)
            prevNodeIndex = k.nodeHintIndex
            k.nodeHintIndex = proj.nodeIndex
            k.lateralOffset = proj.lateralOffset

            val maxLateral = proj.halfWidth - 1.25f
            if (abs(proj.lateralOffset) > maxLateral) {
                val sign = if (proj.lateralOffset > 0f) 1f else -1f
                val excess = abs(proj.lateralOffset) - maxLateral
                k.x -= proj.right.x * sign * excess
                k.z -= proj.right.z * sign * excess
                k.lateralOffset = sign * maxLateral
                k.vx *= 0.92f
                k.vz *= 0.92f
                val roadYaw = atan2(proj.forward.x, proj.forward.z)
                k.yawRad = lerpAngle(k.yawRad, roadYaw, 0.18f)
            }
        }

        // Solid Physical Collisions against buildings, towers, statues, trees, fountains & barriers
        resolveObstacleCollisions(k, events)

        // ============================================================
        // 4-WHEEL INDEPENDENT SUSPENSION SAMPLING (FL, FR, RL, RR)
        // ============================================================
        val wheelLocalOffsets = arrayOf(
            -0.92f to 0.96f,  // FL
            0.92f to 0.96f,   // FR
            -0.96f to -0.96f, // RL
            0.96f to -0.96f   // RR
        )
        val restLength = 0.58f
        var avgGroundH = 0f
        var anyContact = false
        for (i in 0 until 4) {
            val (lx, lz) = wheelLocalOffsets[i]
            val wx = k.x + newRightX * lx + newFwdX * lz
            val wz = k.z + newRightZ * lx + newFwdZ * lz
            val wSample = surfaceSampler.sampleSurface(wx, wz, k.y)
            val wGround = wSample.height
            avgGroundH += wGround
            val clearance = k.y - wGround
            val comp = ((restLength - clearance) / restLength).coerceIn(0f, 1f)
            k.wheelCompression[i] = comp
            val contact = clearance <= 0.22f
            k.wheelContacts[i] = contact
            if (contact) anyContact = true
            // Independent wheel Y bounce relative to chassis
            val targetOffset = if (contact) (comp - 0.5f) * 0.20f else -0.10f
            k.wheelYOffsets[i] += (targetOffset - k.wheelYOffsets[i]) * (18f * dt).coerceAtMost(1f)
        }
        avgGroundH *= 0.25f

        val wasGrounded = k.isGrounded
        val targetGroundY = maxOf(centerSample.height, avgGroundH)
        if (!k.isGrounded || k.y > targetGroundY + 0.08f) {
            k.vy -= 26f * dt
            k.y += k.vy * dt
            if (k.y <= targetGroundY) {
                k.y = targetGroundY
                if (!wasGrounded && k.vy < -3.2f) {
                    val impact = (-k.vy).coerceIn(0f, 22f)
                    k.suspensionOffset = (-0.26f).coerceAtLeast(-impact * 0.020f)
                    k.landingImpulse = (impact / 18f).coerceIn(0.18f, 1.0f)
                    k.justLandedImpulse = k.landingImpulse
                    for (wi in 0 until 4) {
                        k.wheelCompression[wi] = maxOf(k.wheelCompression[wi], 0.92f)
                    }
                }
                k.vy = 0f
                k.isGrounded = true
            } else {
                k.isGrounded = anyContact && k.vy <= 0.5f && (k.y - targetGroundY) < 0.22f
            }
        } else {
            // Spring-damper terrain follow
            val deltaY = targetGroundY - k.y
            k.y += deltaY * (tuning.suspensionStiffness * 0.45f * dt).coerceIn(0.15f, 1.0f)
            k.vy = 0f
            k.isGrounded = true
        }
        k.suspensionOffset += (0f - k.suspensionOffset) * (tuning.suspensionDamping * 1.6f * dt).coerceAtMost(1f)
        k.landingImpulse = (k.landingImpulse - dt * 3.8f).coerceAtLeast(0f)

        // Visual tilt: wheel spin + progressive front wheel angle + chassis roll/pitch weight transfer
        k.wheelSpinRad += k.speed * dt * 1.45f
        val targetSteerVis = -steerInput * 0.46f
        k.steerVisualRad += (targetSteerVis - k.steerVisualRad) * 14f * dt

        val rollFromSusp = (k.wheelCompression[0] + k.wheelCompression[2] - k.wheelCompression[1] - k.wheelCompression[3]) * 0.05f
        val rollFromTurn = (if (k.isDrifting) k.driftDir * 0.14f else steerInput * 0.08f) * speedFactor
        val targetRoll = rollFromTurn + rollFromSusp
        k.bodyRollRad += (targetRoll - k.bodyRollRad) * 10f * dt

        val pitchFromSusp = (k.wheelCompression[0] + k.wheelCompression[1] - k.wheelCompression[2] - k.wheelCompression[3]) * 0.06f
        val targetPitch = if (!k.isGrounded) (k.vy * 0.032f).coerceIn(-0.28f, 0.28f)
        else pitchFromSusp + (if (isBoosting) -0.048f else if (k.isBraking) 0.058f else 0f)
        k.bodyPitchRad += (targetPitch - k.bodyPitchRad) * 9f * dt

        // Boost Pads & Jump Ramps (works in BOTH closed tracks and Open World!)
        val activePads = if (isFreeRoam) OpenWorldKingdom.boostPads else track.boostPads
        for (pad in activePads) {
            val dx = k.x - pad.pos.x
            val dz = k.z - pad.pos.z
            if (dx * dx + dz * dz < 9.5f && abs(k.y - pad.pos.y) < 3.0f) {
                val prevBoost = k.boostTimer
                k.boostTimer = maxOf(k.boostTimer, 1.2f)
                k.nitroFuel = (k.nitroFuel + 0.18f).coerceAtMost(1.0f)
                if (k.isPlayer && prevBoost < 0.5f) {
                    events.add(RaceEvent(RaceEventType.NITRO_START, "SUGAR PAD!"))
                }
            }
        }

        val activeRamps = if (isFreeRoam) OpenWorldKingdom.jumpRamps else track.jumpRamps
        for (ramp in activeRamps) {
            val dx = k.x - ramp.pos.x
            val dz = k.z - ramp.pos.z
            if (dx * dx + dz * dz < 12f && k.isGrounded && k.speed > 17f && abs(k.y - ramp.pos.y) < 3.5f) {
                k.vy = 13.8f
                k.isGrounded = false
                k.boostTimer = maxOf(k.boostTimer, 0.70f)
                if (k.isPlayer) events.add(RaceEvent(RaceEventType.JUMP, "AIR TIME!"))
            }
        }

        if (!isFreeRoam) {
            stepTrackHazardsItemsAndLaps(k, prevNodeIndex, events)
        }
        stepCharacterMotionPhysics(k, dt, yawDelta)
    }

    /**
     * Simulates real-time second-order spring-damper physics for the articulated 3D racer character:
     * - Torso lateral roll: combines centrifugal G-force whip on corner entry with active lean-in during sustained curves/drifts.
     * - Torso longitudinal pitch: rocks backward under acceleration/nitro boost and lunges forward under heavy braking.
     * - Seat cushion bounce: compresses on landing impacts & off-road bumps, springing back smoothly.
     * - Head look-ahead: turns smoothly toward the corner apex and nods with speed and jump arcs.
     * - Hair / Pigtails / Hat pendulum: secondary inertia + high-speed aerodynamic wind flutter.
     * - Arm raise: celebratory/airtime arm pump when jumping off ramps or crossing the finish line.
     */
    private fun stepCharacterMotionPhysics(
        k: KartRuntimeState,
        dt: Float,
        yawDelta: Float
    ) {
        val safeDt = dt.coerceIn(0.001f, 0.05f)
        val longAccel = ((k.speed - k.prevForwardVel) / safeDt).coerceIn(-65f, 65f)
        k.prevForwardVel = k.speed

        val yawRate = (yawDelta / safeDt).coerceIn(-4.5f, 4.5f)
        val lateralG = (k.speed * yawRate) / 36f // normalized lateral centripetal G

        val isBoosting = k.isNitroActive || k.boostTimer > 0f || k.starTimer > 0f

        // 1. Torso Roll Spring-Damper (active corner lean + centrifugal G impulse)
        val steerNorm = (-k.steerVisualRad / 0.46f).coerceIn(-1f, 1f)
        val targetTorsoRoll = (steerNorm * 0.24f - lateralG * 0.09f + (if (k.isDrifting) k.driftDir * 0.14f else 0f))
            .coerceIn(-0.42f, 0.42f)
        val rollSpring = 54f
        val rollDamping = 10.5f
        k.driverTorsoRollVel += ((targetTorsoRoll - k.driverTorsoRollRad) * rollSpring - k.driverTorsoRollVel * rollDamping) * safeDt
        k.driverTorsoRollRad = (k.driverTorsoRollRad + k.driverTorsoRollVel * safeDt).coerceIn(-0.48f, 0.48f)

        // 2. Torso Pitch Spring-Damper (acceleration kickback, braking forward lunge, nitro tuck)
        val accelPitchImpulse = (-longAccel * 0.0055f).coerceIn(-0.22f, 0.24f)
        val baseDrivePitch = when {
            isBoosting -> -0.11f
            k.isBraking -> 0.16f
            !k.isGrounded -> (-k.vy * 0.015f).coerceIn(-0.18f, 0.18f)
            else -> 0.03f
        }
        val targetTorsoPitch = (baseDrivePitch + accelPitchImpulse).coerceIn(-0.34f, 0.36f)
        val pitchSpring = 48f
        val pitchDamping = 9.5f
        k.driverTorsoPitchVel += ((targetTorsoPitch - k.driverTorsoPitchRad) * pitchSpring - k.driverTorsoPitchVel * pitchDamping) * safeDt
        k.driverTorsoPitchRad = (k.driverTorsoPitchRad + k.driverTorsoPitchVel * safeDt).coerceIn(-0.38f, 0.40f)

        // 3. Vertical Seat Cushion Bounce (landing impact + off-road chatter + engine idle hum)
        if (k.justLandedImpulse > 0.05f) {
            k.driverBounceVel -= k.justLandedImpulse * 1.45f
        }
        val surfaceBump = if (k.isGrounded && k.currentSurface == SurfaceType.OFFROAD && abs(k.speed) > 8f) {
            sin(k.wheelSpinRad * 2.4f) * 0.025f
        } else {
            0f
        }
        val bounceSpring = 95f
        val bounceDamping = 11.5f
        k.driverBounceVel += ((surfaceBump - k.driverBounceY) * bounceSpring - k.driverBounceVel * bounceDamping) * safeDt
        k.driverBounceY = (k.driverBounceY + k.driverBounceVel * safeDt).coerceIn(-0.11f, 0.11f)

        // 4. Articulated Head Look-Ahead (turns toward corner apex + drift counter-look)
        val targetHeadYaw = (steerNorm * 0.44f + (if (k.isDrifting) k.driftDir * 0.18f else 0f) + sin(k.wheelSpinRad * 0.25f) * 0.03f)
            .coerceIn(-0.58f, 0.58f)
        k.driverHeadYawRad += (targetHeadYaw - k.driverHeadYawRad) * (14f * safeDt).coerceAtMost(1f)

        val targetHeadPitch = (-k.driverTorsoPitchRad * 0.45f + (if (isBoosting) -0.08f else 0f) + sin(k.wheelSpinRad * 0.65f) * 0.025f)
            .coerceIn(-0.28f, 0.28f)
        k.driverHeadPitchRad += (targetHeadPitch - k.driverHeadPitchRad) * (15f * safeDt).coerceAtMost(1f)

        // 5. Secondary Hair / Pigtails / Hat Pendulum + Aerodynamic High-Speed Wind Flutter
        val speedNorm = (abs(k.speed) / 55f).coerceIn(0f, 1.35f)
        val windFlutterRoll = sin(k.wheelSpinRad * 2.1f + k.x * 0.15f) * 0.085f * speedNorm
        val windFlutterPitch = (0.16f + 0.09f * cos(k.wheelSpinRad * 2.7f)) * speedNorm + (if (isBoosting) 0.18f else 0f)

        val targetHairRoll = (-k.driverTorsoRollRad * 0.85f - lateralG * 0.18f + windFlutterRoll).coerceIn(-0.55f, 0.55f)
        val targetHairPitch = (windFlutterPitch - k.driverTorsoPitchRad * 0.70f - k.driverBounceVel * 0.14f).coerceIn(-0.35f, 0.62f)

        val hairSpring = 72f
        val hairDamping = 7.2f // under-damped so hair/pigtails/hat bounce and whip livelily!
        k.driverHairRollVel += ((targetHairRoll - k.driverHairRollRad) * hairSpring - k.driverHairRollVel * hairDamping) * safeDt
        k.driverHairRollRad = (k.driverHairRollRad + k.driverHairRollVel * safeDt).coerceIn(-0.60f, 0.60f)

        k.driverHairPitchVel += ((targetHairPitch - k.driverHairPitchRad) * hairSpring - k.driverHairPitchVel * hairDamping) * safeDt
        k.driverHairPitchRad = (k.driverHairPitchRad + k.driverHairPitchVel * safeDt).coerceIn(-0.40f, 0.68f)

        // 6. Celebratory / Air-Time Arm Raise
        val targetArmRaise = when {
            k.finished && k.position <= 3 -> 0.55f + 0.25f * sin(k.wheelSpinRad * 1.5f + k.y)
            !k.isGrounded && k.vy > 2.5f -> 0.42f
            isBoosting -> -0.08f
            else -> 0f
        }
        k.driverArmRaiseRad += (targetArmRaise - k.driverArmRaiseRad) * (12f * safeDt).coerceAtMost(1f)
    }

    private fun stepTrackHazardsItemsAndLaps(
        k: KartRuntimeState,
        prevNodeIndex: Int,
        events: MutableList<RaceEvent>
    ) {
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
                        emitSarcasticTaunt(
                            events,
                            speaker = "Berry Blitz",
                            taunt = "طعم الكراميل لذيذ على إطاراتك صح؟ 😜"
                        )
                    }
                }
            }

            val dhIter = droppedHazards.iterator()
            while (dhIter.hasNext()) {
                val dh = dhIter.next()
                if (dh.ownerId == k.racer.id && dh.ttl > 10.5f) continue
                val dx = k.x - dh.pos.x
                val dz = k.z - dh.pos.z
                if (dx * dx + dz * dz < 6.2f && abs(k.y - dh.pos.y) < 1.8f) {
                    if (dh.type == PowerUpType.PEPPERMINT_BLAST) k.spinoutTimer = 1.0f else k.slowTimer = 1.8f
                    dhIter.remove()
                    if (k.isPlayer) {
                        playerHitHazard = true
                        cameraShake = 0.45f
                        events.add(RaceEvent(RaceEventType.HAZARD_HIT, "${dh.type.displayName}!"))
                        emitSarcasticTaunt(
                            events,
                            speaker = dh.ownerId.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            taunt = "هدية صغيرة مني لتبقي في الخلف! 🍬😂"
                        )
                    }
                    break
                }
            }
        }

        for (box in itemBoxes) {
            if (box.respawnTimer > 0f) continue
            val dx = k.x - box.pos.x
            val dz = k.z - box.pos.z
            if (dx * dx + dz * dz < 7.5f && abs(k.y + 1.0f - box.pos.y) < 2.2f) {
                box.respawnTimer = 5.0f
                if (k.heldPowerUp == null) {
                    val chosen = pickWeightedPowerUp(k)
                    k.heldPowerUp = chosen
                    if (k.isPlayer) events.add(RaceEvent(RaceEventType.POWERUP_PICKUP, chosen.displayName, chosen))
                }
            }
        }

        val proj = track.projectOntoTrack(k.x, k.z, k.nodeHintIndex)
        val sector = ((proj.progress * 4f).toInt()).coerceIn(0, 3)
        if (sector == k.sectorReached + 1) k.sectorReached = sector
        val n = track.nodes.size
        if (k.sectorReached >= 2 && prevNodeIndex > (n * 3) / 4 && proj.nodeIndex < n / 4) {
            val lapTime = raceElapsedMs - k.currentLapStartMs
            if (lapTime > 4000L && lapTime < k.bestLapMs) k.bestLapMs = lapTime
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
                if (k.isPlayer) events.add(RaceEvent(RaceEventType.LAP_COMPLETED, "LAP ${k.currentLap}/$totalLaps"))
            }
        }

        k.raceScore = if (k.finished) {
            1000000f - k.finishTimeMs.toFloat() * 0.01f
        } else {
            k.currentLap * 1000f + (k.sectorReached * 200f) + proj.progress * 900f
        }
    }

    private fun stepOpenWorldInteractions(dt: Float, events: MutableList<RaceEvent>) {
        val p = playerKart
        val reg = OpenWorldKingdom.regionAt(p.x, p.z)
        if (reg != currentRegion) {
            currentRegion = reg
            if (discoveredRegions.add(reg)) {
                events.add(RaceEvent(RaceEventType.REGION_DISCOVERED, "DISCOVERED ${reg.displayName.uppercase()}!", regionArg = reg))
            }
        }

        // Collect World Coins & Hidden Golden Stars
        for (item in OpenWorldKingdom.collectibles) {
            val already = if (item.isStar) collectedWorldStars.contains(item.id) else collectedWorldCoins.contains(item.id)
            if (already) continue
            val dx = p.x - item.pos.x
            val dz = p.z - item.pos.z
            val dy = abs(p.y + 1.0f - item.pos.y)
            if (dx * dx + dz * dz < 9.0f && dy < 3.5f) {
                if (item.isStar) {
                    collectedWorldStars.add(item.id)
                    events.add(RaceEvent(RaceEventType.STAR_COLLECTED, "GOLDEN STAR FOUND!", stringArg = item.id))
                } else {
                    collectedWorldCoins.add(item.id)
                    events.add(RaceEvent(RaceEventType.COIN_COLLECTED, "+25 CANDY COINS!", stringArg = item.id))
                }
            }
        }

        // Check nearby interactive Race Portals
        var foundPortal: WorldRacePortal? = null
        for (portal in OpenWorldKingdom.racePortals) {
            val dx = p.x - portal.pos.x
            val dz = p.z - portal.pos.z
            if (dx * dx + dz * dz < 196f) { // 14m radius
                foundPortal = portal
                break
            }
        }
        nearbyRacePortal = foundPortal

        // Open-World Mini-Challenges
        val kmh = (abs(p.speed) * 3.1f).toInt()
        for (ch in OpenWorldKingdom.miniChallenges) {
            if (completedWorldMiniChallenges.contains(ch.id)) continue
            val dx = p.x - ch.pos.x
            val dz = p.z - ch.pos.z
            if (dx * dx + dz * dz < ch.radius * ch.radius) {
                val speedOk = ch.targetSpeedKmh <= 0 || kmh >= ch.targetSpeedKmh
                if (speedOk) {
                    completedWorldMiniChallenges.add(ch.id)
                    events.add(RaceEvent(RaceEventType.WORLD_CHALLENGE_DONE, ch.title, stringArg = ch.id))
                }
            }
        }

        // =========================================================================
        // SWEET RUSH CORP. (شركة سويت راش للحلويات) LOGISTICS CONTRACTS & DIVIDENDS
        // =========================================================================
        val contract = activeCompanyContract()
        val targetDepot = activeCompanyTargetDepot()
        val dxDepot = p.x - targetDepot.padPos.x
        val dzDepot = p.z - targetDepot.padPos.z
        val distDepotSq = dxDepot * dxDepot + dzDepot * dzDepot

        if (!companyCargoLoaded) {
            // Driving into the Pickup Depot loads the shipment
            if (distDepotSq < 15.0f * 15.0f) {
                companyCargoLoaded = true
                p.nitroFuel = (p.nitroFuel + 0.35f).coerceAtMost(1.0f)
                val destDepot = OpenWorldKingdom.companyDepotById(contract.deliveryDepotId)
                events.add(
                    RaceEvent(
                        type = RaceEventType.COMPANY_CARGO_PICKUP,
                        message = "📦 تم استلام شحنة: ${contract.cargoNameAr}",
                        stringArg = "وجهتك: ${destDepot.nameAr}"
                    )
                )
            }
        } else {
            // Driving into the Delivery Depot completes the contract & pays out Company Salary!
            if (distDepotSq < 16.0f * 16.0f) {
                companyCargoLoaded = false
                companyContractsCompleted++
                val bonusCredits = (companyLevel - 1) * 45
                val totalCredits = contract.baseRewardCredits + bonusCredits
                val totalXp = contract.rewardXp + (companyLevel - 1) * 15
                if (companyContractsCompleted % 2 == 0 && companyLevel < 7) {
                    companyLevel++
                }
                activeCompanyContractIndex = (activeCompanyContractIndex + 1) % OpenWorldKingdom.companyContracts.size
                p.boostTimer = maxOf(p.boostTimer, 1.8f)
                p.nitroFuel = 1.0f
                events.add(
                    RaceEvent(
                        type = RaceEventType.COMPANY_DELIVERY_COMPLETED,
                        message = "🏢 تم التسليم لشركة سويت راش! +$totalCredits CC",
                        stringArg = "$totalCredits:$totalXp:$companyLevel:$companyContractsCompleted"
                    )
                )
            }
        }

        // Periodic Passive Company Dividend Payouts while roaming & operating in the Open World
        companyDividendTimerSec += dt
        if (companyDividendTimerSec >= 26f) {
            companyDividendTimerSec = 0f
            val dividendCc = OpenWorldKingdom.companyPassiveDividendForLevel(companyLevel)
            events.add(
                RaceEvent(
                    type = RaceEventType.COMPANY_DIVIDEND_PAID,
                    message = "💰 أرباح شركة سويت راش: +$dividendCc CC",
                    stringArg = dividendCc.toString()
                )
            )
        }

        // =========================================================================
        // LIVING OPEN-WORLD CITIZENS PROXIMITY & GREETINGS
        // =========================================================================
        if (citizenGreetingCooldownSec > 0f) {
            citizenGreetingCooldownSec = (citizenGreetingCooldownSec - dt).coerceAtLeast(0f)
        }
        val timeSec = raceElapsedMs / 1000f
        var closestCitizen: com.example.sweetrush.track.WorldCitizenNpc? = null
        var closestDistSq = 22f * 22f
        for (npc in OpenWorldKingdom.citizens) {
            // Fast coarse check around homePos before sampling exact walk loop
            val cdx = p.x - npc.homePos.x
            val cdz = p.z - npc.homePos.z
            if (cdx * cdx + cdz * cdz > 32f * 32f) continue
            val pose = OpenWorldKingdom.sampleCitizenPose(npc, timeSec, p.x, p.z)
            if (pose.distSqToPlayer < closestDistSq) {
                closestDistSq = pose.distSqToPlayer
                closestCitizen = npc
            }
        }
        if (closestCitizen != null) {
            nearbyCitizenStatusAr = "👋 ${closestCitizen.nameAr} (${closestCitizen.roleAr}): ${closestCitizen.greetingAr}"
            if (closestDistSq < 14.5f * 14.5f &&
                citizenGreetingCooldownSec <= 0f &&
                closestCitizen.id != lastGreetedCitizenId
            ) {
                lastGreetedCitizenId = closestCitizen.id
                citizenGreetingCooldownSec = 9.5f
                events.add(
                    RaceEvent(
                        type = RaceEventType.CITIZEN_GREETING,
                        message = closestCitizen.greetingAr,
                        stringArg = "${closestCitizen.nameAr} • ${closestCitizen.roleAr}"
                    )
                )
            }
        } else {
            nearbyCitizenStatusAr = null
        }
    }

    private fun activatePowerUp(k: KartRuntimeState, powerUp: PowerUpType, events: MutableList<RaceEvent>) {
        if (k.isPlayer) events.add(RaceEvent(RaceEventType.POWERUP_USED, "${powerUp.displayName}!", powerUp))
        when (powerUp) {
            PowerUpType.SUGAR_BOOST -> {
                k.boostTimer = maxOf(k.boostTimer, 2.2f)
                k.nitroFuel = (k.nitroFuel + 0.35f).coerceAtMost(1.0f)
            }
            PowerUpType.GUMMY_SHIELD -> k.shieldTimer = 6.5f
            PowerUpType.PEPPERMINT_BLAST -> {
                val dropPos = Vec3(k.x - sin(k.yawRad) * 4.2f, k.y + 0.1f, k.z - cos(k.yawRad) * 4.2f)
                droppedHazards.add(DroppedHazard(nextHazardId++, k.racer.id, PowerUpType.PEPPERMINT_BLAST, dropPos, 12f))
            }
            PowerUpType.CARAMEL_TRAP -> {
                val dropPos = Vec3(k.x - sin(k.yawRad) * 4.0f, k.y + 0.05f, k.z - cos(k.yawRad) * 4.0f)
                droppedHazards.add(DroppedHazard(nextHazardId++, k.racer.id, PowerUpType.CARAMEL_TRAP, dropPos, 14f))
            }
            PowerUpType.JELLY_DASH -> {
                k.boostTimer = maxOf(k.boostTimer, 1.4f)
                k.vy = 6.5f
                k.isGrounded = false
                k.vx += sin(k.yawRad) * 12f
                k.vz += cos(k.yawRad) * 12f
            }
            PowerUpType.RAINBOW_STAR -> {
                k.starTimer = 5.5f
                k.boostTimer = maxOf(k.boostTimer, 5.5f)
            }
            PowerUpType.CHOCOLATE_SMOKE -> {
                for (other in karts) {
                    if (other.racer.id == k.racer.id || other.shieldTimer > 0f || other.starTimer > 0f) continue
                    val dx = other.x - k.x
                    val dz = other.z - k.z
                    if (dx * dx + dz * dz < 2200f) other.smokeTimer = 3.2f
                }
            }
        }
    }

    private fun pickWeightedPowerUp(k: KartRuntimeState): PowerUpType {
        val all = PowerUpType.entries
        return if (k.position >= 5 && rng.nextFloat() < 0.45f) {
            listOf(PowerUpType.SUGAR_BOOST, PowerUpType.RAINBOW_STAR, PowerUpType.JELLY_DASH).random(rng)
        } else if (rng.nextFloat() < 0.25f) {
            k.racer.signaturePowerUp
        } else {
            all[rng.nextInt(all.size)]
        }
    }

    private fun computeRoamingAiInput(k: KartRuntimeState, dt: Float): PlayerInputState {
        val pts = OpenWorldKingdom.ringRoad.points
        val n = pts.size
        val currPt = pts[k.nodeHintIndex % n]
        val dxC = k.x - currPt.pos.x
        val dzC = k.z - currPt.pos.z
        if (dxC * dxC + dzC * dzC < 400f) {
            k.nodeHintIndex = (k.nodeHintIndex + 1) % n
        }
        val targetPt = pts[(k.nodeHintIndex + 4) % n]
        val desiredYaw = atan2(targetPt.pos.x - k.x, targetPt.pos.z - k.z)
        val angleDiff = normalizeAngle(desiredYaw - k.yawRad)
        val steer = (-angleDiff * 1.4f).coerceIn(-1f, 1f)
        return PlayerInputState(steer = steer, throttle = true)
    }

    private fun computeTrackAiInput(k: KartRuntimeState, dt: Float): PlayerInputState {
        k.aiLaneTimer -= dt
        if (k.aiLaneTimer <= 0f) {
            k.aiLaneTimer = 1.6f + rng.nextFloat() * 2.2f
            k.aiTargetLaneOffset = (rng.nextFloat() * 2f - 1f) * 6.5f
        }

        val n = track.nodes.size
        val lookAheadNodes = (4 + (k.speed / 12f).toInt()).coerceIn(4, 10)
        val targetNode = track.nodes[(k.nodeHintIndex + lookAheadNodes) % n]

        var laneOffset = k.aiTargetLaneOffset
        for (other in karts) {
            if (other.racer.id == k.racer.id) continue
            val dx = other.x - k.x
            val dz = other.z - k.z
            if (dx * dx + dz * dz < 36f) {
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
        val desiredYaw = atan2(targetPos.x - k.x, targetPos.z - k.z)
        val angleDiff = normalizeAngle(desiredYaw - k.yawRad)

        val steer = (-angleDiff * 1.9f * difficulty.aiSkill).coerceIn(-1f, 1f)
        val sharpCorner = abs(angleDiff) > 0.38f

        if (k.speed < 4f && raceElapsedMs > 2500L) {
            k.aiStuckTimer += dt
            if (k.aiStuckTimer > 1.4f) {
                val recoverNode = track.nodes[(k.nodeHintIndex + 2) % n]
                k.x = recoverNode.center.x; k.y = recoverNode.center.y; k.z = recoverNode.center.z
                k.yawRad = atan2(recoverNode.forward.x, recoverNode.forward.z)
                k.vx = sin(k.yawRad) * 20f; k.vz = cos(k.yawRad) * 20f; k.speed = 20f
                k.aiStuckTimer = 0f
            }
        } else {
            k.aiStuckTimer = 0f
        }

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
        val minDist = 2.65f
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

                    a.x -= nx * overlap; a.z -= nz * overlap
                    b.x += nx * overlap; b.z += nz * overlap

                    // Momentum & impact-direction elastic impulse transfer
                    val rvx = b.vx - a.vx
                    val rvz = b.vz - a.vz
                    val velAlongNormal = rvx * nx + rvz * nz
                    if (velAlongNormal < 0f) {
                        val massA = a.kart.physicsTuning.massKg
                        val massB = b.kart.physicsTuning.massKg
                        val impulse = -(1.35f * velAlongNormal) / (1f / massA + 1f / massB)
                        a.vx -= (impulse / massA) * nx
                        a.vz -= (impulse / massA) * nz
                        b.vx += (impulse / massB) * nx
                        b.vz += (impulse / massB) * nz
                    }

                    when {
                        a.starTimer > 0f && b.starTimer <= 0f -> b.spinoutTimer = 0.8f
                        b.starTimer > 0f && a.starTimer <= 0f -> a.spinoutTimer = 0.8f
                    }

                    if (a.isPlayer || b.isPlayer) {
                        cameraShake = maxOf(cameraShake, 0.24f)
                        events.add(RaceEvent(RaceEventType.COLLISION))
                        val rival = if (a.isPlayer) b else a
                        emitSarcasticTaunt(
                            events,
                            speaker = rival.displayName,
                            taunt = bumpTaunts[rng.nextInt(bumpTaunts.size)]
                        )
                    }
                }
            }
        }
    }

    private fun resolveObstacleCollisions(k: KartRuntimeState, events: MutableList<RaceEvent>) {
        val grid = if (isFreeRoam) OpenWorldKingdom.obstacleGrid else track.obstacleGrid
        val nearby = grid.queryNear(k.x, k.z, 9.5f)
        if (nearby.isEmpty()) return

        val kartRadius = 1.38f
        for (obs in nearby) {
            if (k.y > obs.y + obs.height || k.y + 1.8f < obs.y) continue
            val dx = k.x - obs.x
            val dz = k.z - obs.z
            val minDist = obs.radius + kartRadius
            val d2 = dx * dx + dz * dz
            if (d2 >= minDist * minDist) continue

            val (nx, nz) = if (d2 > 1e-4f) {
                val d = sqrt(d2)
                dx / d to dz / d
            } else {
                val yawN = normalizeAngle(k.yawRad)
                -sin(yawN) to -cos(yawN)
            }

            // Push kart cleanly outside solid building/object perimeter
            k.x = obs.x + nx * (minDist + 0.04f)
            k.z = obs.z + nz * (minDist + 0.04f)

            val vn = k.vx * nx + k.vz * nz
            if (vn < 0f) {
                val impactSpeed = -vn
                // Slide along wall with light elastic bounce
                k.vx -= nx * vn * 1.38f
                k.vz -= nz * vn * 1.38f
                k.vx *= 0.84f
                k.vz *= 0.84f
                val fwdX = sin(k.yawRad)
                val fwdZ = cos(k.yawRad)
                k.speed = k.vx * fwdX + k.vz * fwdZ

                if (k.isPlayer && impactSpeed > 7.5f) {
                    cameraShake = maxOf(cameraShake, (impactSpeed / 40f).coerceIn(0.18f, 0.45f))
                    events.add(RaceEvent(RaceEventType.COLLISION))
                    if (obs.isBuilding && impactSpeed > 11.5f) {
                        val speaker = karts.firstOrNull { !it.isPlayer }?.displayName ?: "Candy Rival"
                        emitSarcasticTaunt(
                            events,
                            speaker = speaker,
                            taunt = crashTaunts[rng.nextInt(crashTaunts.size)]
                        )
                    }
                }
            }
        }
    }

    private fun updateStandings(events: MutableList<RaceEvent>) {
        val sorted = karts.sortedByDescending { it.raceScore }
        sorted.forEachIndexed { idx, kart -> kart.position = idx + 1 }
        if (playerKart.position < lastPlayerPos && raceElapsedMs > 3000L && !playerFinished) {
            events.add(RaceEvent(RaceEventType.OVERTAKE, "OVERTAKE! ${ordinal(playerKart.position)}!"))
            emitSarcasticTaunt(
                events,
                speaker = playerKart.displayName,
                taunt = overtakeTaunts[rng.nextInt(overtakeTaunts.size)]
            )
        } else if (playerKart.position > lastPlayerPos && raceElapsedMs > 5000L && !playerFinished) {
            val passer = sorted.getOrNull((playerKart.position - 2).coerceAtLeast(0))
            if (passer != null && !passer.isPlayer) {
                emitSarcasticTaunt(
                    events,
                    speaker = passer.displayName,
                    taunt = passedByRivalTaunts[rng.nextInt(passedByRivalTaunts.size)]
                )
            }
        }
        lastPlayerPos = playerKart.position
    }

    private fun emitSarcasticTaunt(events: MutableList<RaceEvent>, speaker: String, taunt: String) {
        if (tauntCooldownSec > 0f) return
        tauntCooldownSec = 6.5f
        events.add(RaceEvent(RaceEventType.SARCASTIC_TAUNT, message = taunt, stringArg = speaker))
    }

    private val bumpTaunts = listOf(
        "عفواً، هل اشتريت رخصتك من محل الحلويات؟ 🍭😂",
        "ابتعد عن الطريق! هذا مساري المفضل 🏎️💨",
        "تصادم لطيف... سيارتي ما زالت أسرع! 😎",
        "Bump all you want, you still can't catch me! 😜"
    )

    private val crashTaunts = listOf(
        "البيت للعيش مش للقيادة بداخله يا بطل! 🏠💥",
        "تحاول طلب آيس كريم من داخل الجدار؟ 🍦😂",
        "الفرامل موجودة على اليسار للمعلومية فقط! 😉",
        "Nice parking job inside that candy house! 🏠🤣"
    )

    private val overtakeTaunts = listOf(
        "مع السلامة! أراك في خط النهاية يا بطيء 👋🔥",
        "هل نسيت ترفع فرامل اليد؟ 😂💨",
        "تجاوز نظيف! تذوق غبار السكر خلفك 🍬⚡"
    )

    private val passedByRivalTaunts = listOf(
        "شكراً على إفساح الطريق يا صديقي! 😎🏁",
        "سيارتك تحتاج شوية سكر إضافي لتلحق بي! 🍭",
        "Too slow! Need a sugar push? 😜⚡"
    )

    private fun updateTelemetrySnapshot() {
        val p = playerKart
        latestTelemetry = VehicleDebugTelemetry(
            speedKmh = abs(p.speed) * 3.1f,
            vx = p.vx,
            vy = p.vy,
            vz = p.vz,
            wheelContacts = p.wheelContacts.toList(),
            wheelCompression = p.wheelCompression.toList(),
            lateralGrip = p.activeLateralGrip,
            forwardGrip = p.activeForwardGrip,
            slipAngleDeg = p.lateralSlipDeg,
            driftState = when {
                p.spinoutTimer > 0f -> "SPINOUT"
                p.isDrifting -> "DRIFT"
                !p.isGrounded -> "AIRBORNE"
                p.isBraking -> "BRAKING"
                else -> "GRIP"
            },
            surfaceName = p.currentSurface.displayName
        )
    }

    fun buildResults(): List<RacerResultEntry> {
        val gpPoints = intArrayOf(15, 12, 10, 8, 6, 4, 2, 1)
        val sorted = karts.sortedBy { it.position }
        return sorted.mapIndexed { idx, k ->
            val estTime = if (k.finished) {
                k.finishTimeMs
            } else {
                val remaining = (totalLaps - k.currentLap).coerceAtLeast(0) * 28000L +
                    ((1f - (k.raceScore % 1000f) / 1000f).coerceIn(0.05f, 0.95f) * 24000L).toLong()
                raceElapsedMs + remaining + idx * 450L
            }
            val bestLap = if (k.bestLapMs < Long.MAX_VALUE) k.bestLapMs else (estTime / totalLaps.coerceAtLeast(1))
            RacerResultEntry(
                racerId = k.racer.id,
                racerName = k.displayName,
                kartName = k.kart.name,
                isPlayer = k.isPlayer,
                position = idx + 1,
                totalTimeMs = estTime,
                bestLapMs = bestLap,
                grandPrixPointsAwarded = if (idx < gpPoints.size) gpPoints[idx] else 0,
                teamIndex = k.teamIndex
            )
        }
    }

    fun buildTeamStandings(standings: List<RacerResultEntry>): List<Pair<Int, Int>> {
        if (teamSize <= 1) return emptyList()
        return standings
            .groupBy { it.teamIndex }
            .map { (teamIdx, entries) ->
                teamIdx to entries.sumOf { it.grandPrixPointsAwarded }
            }
            .sortedByDescending { it.second }
    }

    private fun normalizeAngle(a: Float): Float {
        var r = a
        while (r > PI) r -= (2.0 * PI).toFloat()
        while (r < -PI) r += (2.0 * PI).toFloat()
        return r
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float = a + normalizeAngle(b - a) * t

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
