package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.Difficulty
import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.GameSettings
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.PlayerProfile
import com.example.sweetrush.model.PowerUpType
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.model.WorldRegion
import com.example.sweetrush.physics.KartPhysicsEngine
import com.example.sweetrush.physics.PlayerInputState
import com.example.sweetrush.storage.SaveRepository
import com.example.sweetrush.track.DecoType
import com.example.sweetrush.track.OpenWorldKingdom
import com.example.sweetrush.track.TrackGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SweetRushTest {

    @Test
    fun `catalog contains 12 movie karts 12 movie racers 4 tracks 7 powerups and distinct physics tuning`() {
        assertEquals(12, CatalogData.karts.size)
        assertEquals(12, CatalogData.racers.size)
        assertEquals(4, CatalogData.tracks.size)
        assertEquals(7, PowerUpType.entries.size)
        assertEquals(4, CatalogData.challenges.size)

        val strawberry = CatalogData.kartById("strawberry_rocket")
        val chocolate = CatalogData.kartById("chocolate_cruiser")
        val caramel = CatalogData.kartById("caramel_thunder")
        assertNotEquals(strawberry.physicsTuning.massKg, chocolate.physicsTuning.massKg)
        assertNotEquals(strawberry.physicsTuning.lateralGrip, caramel.physicsTuning.lateralGrip)

        // Ensure all 12 movie KartBodyStyle variants are represented across the 12 movie karts
        assertEquals(12, CatalogData.karts.map { it.style }.toSet().size)
        // Ensure all 12 movie racers and karts are unlocked by default in PlayerProfile
        val defaultProfile = PlayerProfile()
        assertEquals(12, defaultProfile.unlockedKarts.size)
        assertEquals(12, defaultProfile.unlockedRacers.size)
    }

    @Test
    fun `all 4 tracks generate valid road splines and candy decorations`() {
        for (trackId in TrackId.entries) {
            val track = TrackGenerator.getTrack(trackId)
            assertTrue("Track $trackId should have >= 120 spline nodes", track.nodes.size >= 120)
            assertTrue("Track $trackId should have boost pads", track.boostPads.isNotEmpty())
            assertTrue("Track $trackId should have jump ramps", track.jumpRamps.isNotEmpty())
            assertTrue("Track $trackId should have item boxes", track.itemBoxes.size >= 16)
            assertTrue("Track $trackId should have rich candy decorations", track.decorations.size >= 60)
        }
    }

    @Test
    fun `open world kingdom connects 5 regions with roads terrain chunks portals and collectibles`() {
        assertEquals(5, WorldRegion.entries.size)
        assertTrue("Open world should have at least 5 connected roads", OpenWorldKingdom.roads.size >= 5)
        assertEquals(36, OpenWorldKingdom.getTerrainChunks().size)
        assertEquals(48, OpenWorldKingdom.collectibles.count { !it.isStar })
        assertEquals(10, OpenWorldKingdom.collectibles.count { it.isStar })
        assertEquals(5, OpenWorldKingdom.racePortals.size)
        assertEquals(6, OpenWorldKingdom.miniChallenges.size)
        assertEquals(5, OpenWorldKingdom.miniChallenges.map { it.region }.toSet().size)
        assertTrue("Open world should have >= 240 regional landmarks & props", OpenWorldKingdom.decorations.size >= 240)
        val decoTypes = OpenWorldKingdom.decorations.map { it.type }.toSet()
        assertTrue(decoTypes.contains(DecoType.JELLYBEAN_BUSH))
        assertTrue(decoTypes.contains(DecoType.CANDY_FLOWER))
        assertTrue(decoTypes.contains(DecoType.CANDY_FACTORY))
        assertTrue(decoTypes.contains(DecoType.ICE_CREAM_PARLOR))
        assertTrue(decoTypes.contains(DecoType.MACARON_TOWER))
        assertTrue(decoTypes.contains(DecoType.PEPPERMINT_LIGHTHOUSE))
        assertTrue(decoTypes.contains(DecoType.WAFFLE_SKYSCRAPER))
        assertTrue(decoTypes.contains(DecoType.ROAD_SIGN))
        assertTrue(decoTypes.contains(DecoType.CANDY_BARRIER))

        // Verify each region coordinate resolves to its expected region
        for (region in WorldRegion.entries) {
            assertEquals(region, OpenWorldKingdom.regionAt(region.centerX, region.centerZ))
        }
    }

    @Test
    fun `menu background kart spline interpolation produces continuous smooth motion`() {
        val track = TrackGenerator.getTrack(TrackId.PEPPERMINT_PLAZA)
        val pA = com.example.sweetrush.render3d.CandyWorldRenderer.interpolateTrackPose(track.nodes, 0.2500f)
        val pB = com.example.sweetrush.render3d.CandyWorldRenderer.interpolateTrackPose(track.nodes, 0.2504f)
        val delta = (pB.center - pA.center).length()
        assertTrue("Interpolated pose should advance smoothly rather than snapping", delta > 0.01f && delta < 1.5f)
    }

    @Test
    fun `gathering hub room doc maps cleanly to free roam hub and arabic label`() {
        val hubRoom = com.example.sweetrush.online.OnlineRoomDoc(
            roomCode = "CANDYPLAZA",
            mode = "GATHERING_HUB",
            teamSize = 4L
        )
        assertTrue(hubRoom.isGatheringHub())
        assertEquals(GameMode.FREE_ROAM, hubRoom.parsedMode())
        assertEquals("لوبي التجمع", CatalogData.localized(com.example.sweetrush.model.AppLanguage.AR, "gathering_hub"))
    }

    @Test
    fun `kart accelerates drifts charges mini turbo jumps and updates 4-wheel suspension telemetry`() {
        val engine = KartPhysicsEngine(
            trackId = TrackId.PEPPERMINT_PLAZA,
            mode = GameMode.QUICK_RACE,
            difficulty = Difficulty.NORMAL,
            playerRacerId = "berry_blitz",
            playerKartId = "strawberry_rocket",
            playerCustomization = KartCustomization()
        )
        assertEquals(8, engine.karts.size)

        // Advance past countdown
        repeat(120) {
            engine.step(1f / 30f, PlayerInputState(throttle = true))
        }
        assertTrue(engine.raceStarted)
        assertTrue("Player kart should accelerate forward", engine.playerKart.speed > 20f)
        assertTrue("Telemetry speed should be positive", engine.latestTelemetry.speedKmh > 40f)
        assertEquals(4, engine.latestTelemetry.wheelContacts.size)
        assertEquals(4, engine.latestTelemetry.wheelCompression.size)
        assertTrue("Grounded wheels should have contact", engine.latestTelemetry.wheelContacts.any { it })

        // Drift while steering right to charge drift boost and reduce lateral grip
        val normalGrip = engine.playerKart.activeLateralGrip
        repeat(35) {
            engine.step(1f / 30f, PlayerInputState(steer = 0.85f, throttle = true, drift = true))
        }
        assertTrue("Drift charge should accumulate", engine.playerKart.driftCharge >= 0.7f)
        assertTrue("Drifting should reduce active lateral grip vs normal grip", engine.playerKart.activeLateralGrip < normalGrip)
        assertEquals("DRIFT", engine.latestTelemetry.driftState)

        // Release drift -> triggers boostTimer
        engine.step(1f / 30f, PlayerInputState(steer = 0f, throttle = true, drift = false))
        assertTrue("Releasing charged drift should trigger boost", engine.playerKart.boostTimer > 0.4f)
        assertEquals(1, engine.playerDriftBoostCount)

        // Jump action & landing suspension impulse
        engine.step(1f / 30f, PlayerInputState(throttle = true, jumpPressed = true))
        assertFalse("Kart should leave ground on jump", engine.playerKart.isGrounded)

        var landed = false
        var sawLandingImpulse = false
        repeat(60) {
            engine.step(1f / 30f, PlayerInputState(throttle = true))
            if (engine.playerKart.landingImpulse > 0.01f) {
                sawLandingImpulse = true
            }
            if (engine.playerKart.isGrounded) {
                landed = true
            }
        }
        assertTrue("Kart should return to ground after jump", landed)
        assertTrue("Landing from jump should trigger suspension compression impulse", sawLandingImpulse)

        // Speed-sensitive smoothedSteer interpolation (does not snap instantaneously to 1.0 in 1 step)
        engine.step(1f / 30f, PlayerInputState(steer = 0f, throttle = true))
        val preSteer = engine.playerKart.smoothedSteer
        engine.step(1f / 30f, PlayerInputState(steer = 1f, throttle = true))
        val afterOneStep = engine.playerKart.smoothedSteer
        assertTrue("smoothedSteer should move toward +1", afterOneStep > preSteer)
        assertTrue("smoothedSteer should interpolate smoothly instead of snapping to 1.0", afterOneStep < 0.99f)
    }

    @Test
    fun `free roam mode supports region discovery fast travel and roaming traffic`() {
        val engine = KartPhysicsEngine(
            trackId = TrackId.PEPPERMINT_PLAZA,
            mode = GameMode.FREE_ROAM,
            difficulty = Difficulty.NORMAL,
            playerRacerId = "berry_blitz",
            playerKartId = "strawberry_rocket",
            playerCustomization = KartCustomization(),
            startRegion = WorldRegion.CANDY_CITY
        )
        assertTrue(engine.isFreeRoam)
        assertTrue(engine.raceStarted)
        assertEquals(12, engine.karts.size) // 1 player + 11 roaming movie AI drivers
        assertEquals(WorldRegion.CANDY_CITY, engine.currentRegion)

        engine.teleportPlayerToRegion(WorldRegion.CHOCOLATE_MOUNTAINS)
        engine.step(1f / 30f, PlayerInputState(throttle = true))
        assertEquals(WorldRegion.CHOCOLATE_MOUNTAINS, engine.currentRegion)
        assertTrue(engine.discoveredRegions.contains(WorldRegion.CHOCOLATE_MOUNTAINS))
    }

    @Test
    fun `powerups activate and apply gameplay effects`() {
        val engine = KartPhysicsEngine(
            trackId = TrackId.CUPCAKE_COAST,
            mode = GameMode.QUICK_RACE,
            difficulty = Difficulty.HARD,
            playerRacerId = "minty_swirl",
            playerKartId = "peppermint_racer",
            playerCustomization = KartCustomization()
        )
        engine.raceStarted = true

        engine.playerKart.heldPowerUp = PowerUpType.GUMMY_SHIELD
        engine.step(1f / 30f, PlayerInputState(throttle = true, usePowerUpPressed = true))
        assertTrue(engine.playerKart.shieldTimer > 0f)

        engine.playerKart.heldPowerUp = PowerUpType.CARAMEL_TRAP
        engine.step(1f / 30f, PlayerInputState(throttle = true, usePowerUpPressed = true))
        assertEquals(1, engine.droppedHazards.size)

        engine.playerKart.heldPowerUp = PowerUpType.RAINBOW_STAR
        engine.step(1f / 30f, PlayerInputState(throttle = true, usePowerUpPressed = true))
        assertTrue(engine.playerKart.starTimer > 0f)
    }

    @Test
    fun `save repository persists profile open world progress and recovers from corruption`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SaveRepository(context)

        val customProfile = PlayerProfile(
            xp = 95,
            level = 4,
            credits = 1234,
            selectedKartId = "donut_drifter",
            selectedRacerId = "glazed_gus",
            unlockedKarts = setOf("strawberry_rocket", "donut_drifter"),
            collectedWorldCoins = setOf("coin_0", "coin_1"),
            collectedWorldStars = setOf("star_0"),
            discoveredRegions = setOf(WorldRegion.CANDY_CITY, WorldRegion.PEPPERMINT_COAST),
            racesWon = 7,
            settings = GameSettings(showDebugTelemetry = true)
        )
        repo.saveProfile(customProfile)
        val loaded = repo.loadProfile()
        assertEquals(1234, loaded.credits)
        assertEquals(4, loaded.level)
        assertEquals("donut_drifter", loaded.selectedKartId)
        assertEquals(7, loaded.racesWon)
        assertTrue(loaded.collectedWorldCoins.contains("coin_1"))
        assertTrue(loaded.collectedWorldStars.contains("star_0"))
        assertTrue(loaded.discoveredRegions.contains(WorldRegion.PEPPERMINT_COAST))
        assertTrue(loaded.settings.showDebugTelemetry)

        // Corrupt primary save and verify safe fallback to backup / defaults
        val primary = File(context.filesDir, "sweet_rush_save.json")
        primary.writeText("{not valid json!!!")
        val recovered = repo.loadProfile()
        assertNotNull(recovered)
        assertTrue(recovered.level >= 1)
    }

    @Test
    fun `camera settings and arabic localization persist and resolve properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SaveRepository(context)
        val settings = GameSettings(
            cameraPreset = com.example.sweetrush.model.CameraPreset.FREE_ORBIT,
            cameraDistanceScale = 1.25f,
            cameraHeightOffset = 0.8f,
            cameraFovOffset = 6f,
            language = com.example.sweetrush.model.AppLanguage.AR
        )
        repo.saveProfile(PlayerProfile(settings = settings))
        val loaded = repo.loadProfile()
        assertEquals(com.example.sweetrush.model.CameraPreset.FREE_ORBIT, loaded.settings.cameraPreset)
        assertEquals(1.25f, loaded.settings.cameraDistanceScale, 0.01f)
        assertEquals(0.8f, loaded.settings.cameraHeightOffset, 0.01f)
        assertEquals(6f, loaded.settings.cameraFovOffset, 0.01f)
        assertEquals(com.example.sweetrush.model.AppLanguage.AR, loaded.settings.language)
        assertEquals("أونلاين وفرق", CatalogData.localized(com.example.sweetrush.model.AppLanguage.AR, "online"))
    }

    @Test
    fun `online squad session interpolates remote karts and computes team standings for 2v2 and 4v4`() {
        val remoteFriend = com.example.sweetrush.physics.OnlineRacerParticipant(
            userId = "friend_uid_1",
            displayName = "Ali",
            racerId = "coco_dash",
            kartId = "chocolate_cruiser",
            customization = KartCustomization(),
            teamIndex = 0
        )
        val engine = KartPhysicsEngine(
            trackId = TrackId.PEPPERMINT_PLAZA,
            mode = GameMode.QUICK_RACE,
            difficulty = Difficulty.NORMAL,
            playerRacerId = "berry_blitz",
            playerKartId = "strawberry_rocket",
            playerCustomization = KartCustomization(),
            isOnlineSession = true,
            teamSize = 2,
            localTeamIndex = 0,
            localDisplayName = "Noor",
            onlineParticipants = listOf(remoteFriend),
            fillWithAi = true
        )
        assertEquals(8, engine.karts.size)
        val remoteKart = engine.karts.first { it.isRemotePlayer }
        assertEquals("friend_uid_1", remoteKart.remoteUid)
        assertEquals(0, remoteKart.teamIndex)

        engine.raceStarted = true
        val startX = remoteKart.x
        engine.updateRemoteStates(
            listOf(
                com.example.sweetrush.online.RoomPlayerDoc(
                    userId = "friend_uid_1",
                    x = (startX + 8.0),
                    y = remoteKart.y.toDouble(),
                    z = (remoteKart.z + 10.0),
                    yawRad = 0.25,
                    speed = 32.0,
                    teamIndex = 0L
                )
            )
        )
        engine.step(1f / 30f, PlayerInputState(throttle = true))
        assertNotEquals(startX, remoteKart.x, 0.001f)

        val standings = engine.buildResults()
        val teamStandings = engine.buildTeamStandings(standings)
        assertTrue("2v2 squads should produce team standings", teamStandings.size >= 2)
    }

    @Test
    fun `open world solid obstacles do not overlap each other and resolve physical collisions`() {
        val solids = OpenWorldKingdom.solidObstacles
        assertTrue("Open world should have solid obstacles for buildings & landmarks", solids.size >= 60)

        for (i in solids.indices) {
            val a = solids[i]
            for (j in i + 1 until solids.size) {
                val b = solids[j]
                val dx = a.x - b.x
                val dz = a.z - b.z
                val dist = kotlin.math.sqrt(dx * dx + dz * dz)
                val minDist = a.radius + b.radius
                assertTrue(
                    "Obstacles $i (${a.x},${a.z} r=${a.radius}) and $j (${b.x},${b.z} r=${b.radius}) overlap: dist=$dist < min=$minDist",
                    dist >= minDist - 0.1f
                )
            }
        }

        // Verify kart physically collides and gets pushed out instead of driving through a building
        val targetObs = solids.first { it.isBuilding && it.radius >= 3.0f }
        val engine = KartPhysicsEngine(
            trackId = TrackId.PEPPERMINT_PLAZA,
            mode = GameMode.FREE_ROAM,
            difficulty = Difficulty.NORMAL,
            playerRacerId = "berry_blitz",
            playerKartId = "strawberry_rocket",
            playerCustomization = KartCustomization(),
            startRegion = WorldRegion.CANDY_CITY
        )
        engine.playerKart.x = targetObs.x - (targetObs.radius * 0.4f)
        engine.playerKart.y = targetObs.y
        engine.playerKart.z = targetObs.z
        engine.playerKart.yawRad = Math.PI.toFloat() * 0.5f
        engine.playerKart.vx = 28f
        engine.playerKart.vz = 0f
        engine.playerKart.speed = 28f

        val events = engine.step(1f / 30f, PlayerInputState(throttle = true))
        val dx = engine.playerKart.x - targetObs.x
        val dz = engine.playerKart.z - targetObs.z
        val distAfter = kotlin.math.sqrt(dx * dx + dz * dz)
        assertTrue(
            "Kart should be pushed outside obstacle radius (${targetObs.radius} + 1.35), got dist=$distAfter",
            distAfter >= targetObs.radius + 1.25f
        )
        assertTrue(
            "High-speed crash into building should emit COLLISION and SARCASTIC_TAUNT",
            events.any { it.type == com.example.sweetrush.physics.RaceEventType.COLLISION } &&
                events.any { it.type == com.example.sweetrush.physics.RaceEventType.SARCASTIC_TAUNT }
        )
    }

    @Test
    fun `presence helper and offline nudge invite distinguish online vs offline friends`() {
        val now = 1_000_000L
        val onlineCard = com.example.sweetrush.online.RacerDirectoryCard(
            userId = "u1",
            isOnline = true,
            lastActiveMs = now - 15_000L
        )
        val staleCard = com.example.sweetrush.online.RacerDirectoryCard(
            userId = "u2",
            isOnline = true,
            lastActiveMs = now - 120_000L
        )
        val offlineCard = com.example.sweetrush.online.RacerDirectoryCard(
            userId = "u3",
            isOnline = false,
            lastActiveMs = now - 5_000L
        )
        assertTrue(onlineCard.isCurrentlyOnline(now))
        assertFalse(staleCard.isCurrentlyOnline(now))
        assertFalse(offlineCard.isCurrentlyOnline(now))

        val offlineInvite = com.example.sweetrush.online.GameInviteDoc(
            fromName = "أحمد",
            inviteType = "OFFLINE_NUDGE",
            roomCode = "CANDYPLAZA"
        )
        val liveInvite = com.example.sweetrush.online.GameInviteDoc(
            fromName = "سارة",
            inviteType = "LIVE_INVITE",
            roomCode = "XYZ123"
        )
        assertTrue(offlineInvite.isOfflineNudge())
        assertFalse(liveInvite.isOfflineNudge())
    }

    @Test
    fun `all 8 movie style bakery karts custom wheels exhausts decorations and 3d bakery showroom build rich geometry`() {
        val racer = CatalogData.racers.first()
        val wheels = com.example.sweetrush.model.WheelDesign.entries
        val exhausts = com.example.sweetrush.model.ExhaustStyle.entries
        val decos = com.example.sweetrush.model.KartDecoration.entries

        for ((idx, kart) in CatalogData.karts.withIndex()) {
            val custom = KartCustomization(
                wheelDesign = wheels[idx % wheels.size],
                exhaustStyle = exhausts[idx % exhausts.size],
                decoration = decos[idx % decos.size]
            )
            val bundle = com.example.sweetrush.render3d.VehicleMeshBuilder.buildKartBundle(kart, racer, custom)
            assertTrue(
                "Kart ${kart.id} chassis should have rich confectionery geometry (> 1200 vertices), got ${bundle.chassisMesh.vertexCount}",
                bundle.chassisMesh.vertexCount > 1200
            )
            assertTrue("Kart ${kart.id} tire mesh should have edible sidewall details", bundle.tireMesh.vertexCount > 300)
            assertTrue("Kart ${kart.id} rim mesh should have spoke & hubcap vertices", bundle.rimMesh.vertexCount > 60)
            assertTrue("Kart ${kart.id} driver mesh should have helmet & suit vertices", bundle.driverMesh.vertexCount > 250)
        }

        val turntable = com.example.sweetrush.render3d.MeshFactory.buildBakeryTurntablePedestalMesh()
        val workshop = com.example.sweetrush.render3d.MeshFactory.buildGarageBakeryWorkshopMesh()
        assertTrue("Bakery turntable pedestal should have piped frosting & cake layers", turntable.vertexCount > 600)
        assertTrue("3D Garage Bakery Workshop should have columns, nozzles, jars & racks", workshop.vertexCount > 1500)

        val renderer = com.example.sweetrush.render3d.CandyWorldRenderer { _, _ -> }
        renderer.focusShowroomCamera("WHEELS")
        assertFalse(renderer.showroomAutoRotate)
        assertTrue(renderer.hasShowroomCameraTarget)
        assertEquals(3.55f, renderer.showroomTargetZoom, 0.01f)

        renderer.focusShowroomCamera("AUTO")
        assertTrue(renderer.showroomAutoRotate)
    }

    @Test
    fun `uploaded mp3 background music raw resources exist and audio engine cycles tracks cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val gumballBytes = context.resources.openRawResource(R.raw.gumball_horizon).use { it.available() }
        val turboBytes = context.resources.openRawResource(R.raw.turbo_candy_drift).use { it.available() }
        assertTrue("gumball_horizon.mp3 raw resource should have non-zero size", gumballBytes > 100_000)
        assertTrue("turbo_candy_drift.mp3 raw resource should have non-zero size", turboBytes > 100_000)

        context.resources.openRawResourceFd(R.raw.gumball_horizon)?.use { afd ->
            assertTrue("gumball_horizon.mp3 AssetFileDescriptor length should be > 4MB", afd.length > 4_000_000L)
        }
        context.resources.openRawResourceFd(R.raw.turbo_candy_drift)?.use { afd ->
            assertTrue("turbo_candy_drift.mp3 AssetFileDescriptor length should be > 4MB", afd.length > 4_000_000L)
        }

        val audioEngine = com.example.sweetrush.audio.CandyAudioEngine(context)
        audioEngine.setVolumes(music = 0.85f, sfx = 0.85f)
        audioEngine.playMusic(com.example.sweetrush.audio.MusicTrack.MENU)
        assertEquals("Gumball Horizon", audioEngine.activeSongTitle.value)
        val switched = audioEngine.switchBackgroundSong()
        assertEquals("Turbo Candy Drift", switched)
        assertEquals("Turbo Candy Drift", audioEngine.activeSongTitle.value)
        audioEngine.playMusic(com.example.sweetrush.audio.MusicTrack.RACE)
        audioEngine.pauseMusic()
        audioEngine.resumeMusic()
        audioEngine.release()
    }

    @Test
    fun `remote friend kart advances monotonically without rubber band stutter and detects friend join and leave`() {
        val friendA = com.example.sweetrush.physics.OnlineRacerParticipant(
            userId = "friend_a",
            displayName = "Omar",
            racerId = "berry_blitz",
            kartId = "strawberry_rocket",
            customization = KartCustomization(),
            teamIndex = 0
        )
        val engine = KartPhysicsEngine(
            trackId = TrackId.PEPPERMINT_PLAZA,
            mode = GameMode.FREE_ROAM,
            difficulty = Difficulty.NORMAL,
            playerRacerId = "berry_blitz",
            playerKartId = "strawberry_rocket",
            playerCustomization = KartCustomization(),
            isOnlineSession = true,
            teamSize = 2,
            localTeamIndex = 0,
            localDisplayName = "Mbihi",
            onlineParticipants = listOf(friendA),
            fillWithAi = false
        )
        engine.raceStarted = true
        val remoteKart = engine.karts.first { it.isRemotePlayer && it.remoteUid == "friend_a" }

        // First telemetry packet at z = 100 moving forward (+Z) at 30 m/s
        engine.updateRemoteStates(
            listOf(
                com.example.sweetrush.online.RoomPlayerDoc(
                    userId = "friend_a",
                    displayName = "Omar",
                    x = 0.0,
                    y = remoteKart.y.toDouble(),
                    z = 100.0,
                    yawRad = 0.0,
                    speed = 30.0
                )
            )
        )
        var lastZ = remoteKart.z
        repeat(5) {
            engine.step(1f / 60f, PlayerInputState(throttle = true))
            assertTrue("Remote kart should advance smoothly forward every frame", remoteKart.z > lastZ)
            lastZ = remoteKart.z
        }

        // Second packet arrives ~83ms later at z = 102.5 (30 m/s * 0.0833s) -> kart must NOT snap backward!
        val zBeforePacket = remoteKart.z
        engine.updateRemoteStates(
            listOf(
                com.example.sweetrush.online.RoomPlayerDoc(
                    userId = "friend_a",
                    displayName = "Omar",
                    x = 0.0,
                    y = remoteKart.y.toDouble(),
                    z = 102.5,
                    yawRad = 0.0,
                    speed = 30.0
                )
            )
        )
        engine.step(1f / 60f, PlayerInputState(throttle = true))
        assertTrue(
            "Remote kart must continue moving forward smoothly on packet arrival without snapping backward ($zBeforePacket -> ${remoteKart.z})",
            remoteKart.z >= zBeforePacket
        )

        // New friend joins mid-session in Open World / Gathering Hub
        val joinDiff = engine.updateRemoteStates(
            listOf(
                com.example.sweetrush.online.RoomPlayerDoc(
                    userId = "friend_a",
                    displayName = "Omar",
                    x = 0.0,
                    y = remoteKart.y.toDouble(),
                    z = 105.0,
                    yawRad = 0.0,
                    speed = 30.0
                ),
                com.example.sweetrush.online.RoomPlayerDoc(
                    userId = "friend_b",
                    displayName = "Khalid",
                    x = 4.0,
                    y = remoteKart.y.toDouble(),
                    z = 102.0,
                    yawRad = 0.0,
                    speed = 25.0
                )
            )
        )
        assertEquals(listOf("Khalid"), joinDiff.joinedNames)
        assertTrue(engine.karts.any { it.isRemotePlayer && it.remoteUid == "friend_b" })

        // Friend Omar leaves the room / Open World
        val leaveDiff = engine.updateRemoteStates(
            listOf(
                com.example.sweetrush.online.RoomPlayerDoc(
                    userId = "friend_b",
                    displayName = "Khalid",
                    x = 4.0,
                    y = remoteKart.y.toDouble(),
                    z = 104.0,
                    yawRad = 0.0,
                    speed = 25.0
                )
            )
        )
        assertEquals(listOf("Omar"), leaveDiff.leftNames)
        assertFalse("Departed friend kart should be removed from active 3D scene", engine.karts.any { it.remoteUid == "friend_a" })
    }

    @Test
    fun `open world populates 71 living candy citizens across all 5 regions and executes Sweet Rush Corp logistics contracts`() {
        assertTrue("Open world should have at least 65 living citizens", OpenWorldKingdom.citizens.size >= 65)
        assertEquals(5, OpenWorldKingdom.citizens.map { it.region }.toSet().size)
        assertEquals(5, OpenWorldKingdom.companyDepots.size)
        assertEquals(5, OpenWorldKingdom.companyContracts.size)

        val decoTypes = OpenWorldKingdom.decorations.map { it.type }.toSet()
        assertTrue("Open world should include 3D Sweet Rush Corp HQ skyscraper", decoTypes.contains(DecoType.SWEET_CORP_HQ))
        assertTrue("Open world should include 3D regional company depots", decoTypes.contains(DecoType.SWEET_CORP_DEPOT))

        // Verify citizen animation pose sampling and player-proximity greeting
        val sampleNpc = OpenWorldKingdom.citizens.first()
        val poseFar = OpenWorldKingdom.sampleCitizenPose(sampleNpc, timeSec = 2.5f, playerX = 999f, playerZ = 999f)
        val poseNear = OpenWorldKingdom.sampleCitizenPose(sampleNpc, timeSec = 2.5f, playerX = poseFar.x + 2f, playerZ = poseFar.z + 2f)
        assertFalse(poseFar.isWaving)
        assertTrue("Citizen should wave when player approaches within 16m", poseNear.isWaving)

        // Verify Sweet Rush Corp. pickup -> cargo loaded -> delivery -> contract completion & promotion
        val engine = KartPhysicsEngine(
            trackId = TrackId.PEPPERMINT_PLAZA,
            mode = GameMode.FREE_ROAM,
            difficulty = Difficulty.NORMAL,
            playerRacerId = "berry_blitz",
            playerKartId = "strawberry_rocket",
            playerCustomization = KartCustomization(),
            startRegion = WorldRegion.CANDY_CITY,
            initialCompanyLevel = 1,
            initialCompanyContractsCompleted = 1
        )
        assertFalse(engine.companyCargoLoaded)
        val contract = engine.activeCompanyContract()
        val pickupDepot = OpenWorldKingdom.companyDepotById(contract.pickupDepotId)
        val dropoffDepot = OpenWorldKingdom.companyDepotById(contract.deliveryDepotId)

        // Drive onto pickup pad
        engine.playerKart.x = pickupDepot.padPos.x
        engine.playerKart.z = pickupDepot.padPos.z
        engine.playerKart.y = pickupDepot.padPos.y
        val pickupEvents = engine.step(1f / 30f, PlayerInputState(throttle = false))
        assertTrue("Entering pickup pad should load company cargo", engine.companyCargoLoaded)
        assertTrue(pickupEvents.any { it.type == com.example.sweetrush.physics.RaceEventType.COMPANY_CARGO_PICKUP })

        // Drive onto dropoff pad
        engine.playerKart.x = dropoffDepot.padPos.x
        engine.playerKart.z = dropoffDepot.padPos.z
        engine.playerKart.y = dropoffDepot.padPos.y
        val deliveryEvents = engine.step(1f / 30f, PlayerInputState(throttle = false))
        assertFalse("Entering dropoff pad should unload cargo and complete contract", engine.companyCargoLoaded)
        assertEquals(2, engine.companyContractsCompleted)
        assertEquals(2, engine.companyLevel)
        assertTrue(deliveryEvents.any { it.type == com.example.sweetrush.physics.RaceEventType.COMPANY_DELIVERY_COMPLETED })
    }
}

