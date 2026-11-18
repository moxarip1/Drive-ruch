package com.example.sweetrush

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetrush.audio.CandyAudioEngine
import com.example.sweetrush.audio.MusicTrack
import com.example.sweetrush.audio.SoundEffect
import com.example.sweetrush.model.AppLanguage
import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.ChallengeSpec
import com.example.sweetrush.model.ChallengeTargetType
import com.example.sweetrush.model.ControlScheme
import com.example.sweetrush.model.Difficulty
import com.example.sweetrush.model.ExhaustStyle
import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.GameScreen
import com.example.sweetrush.model.GameSettings
import com.example.sweetrush.model.GrandPrixSession
import com.example.sweetrush.model.GraphicsQuality
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.KartDecoration
import com.example.sweetrush.model.PlayerProfile
import com.example.sweetrush.model.PowerUpType
import com.example.sweetrush.model.RaceSummary
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.model.WheelDesign
import com.example.sweetrush.model.WorldRegion
import com.example.sweetrush.physics.KartPhysicsEngine
import com.example.sweetrush.physics.PlayerInputState
import com.example.sweetrush.physics.RaceEvent
import com.example.sweetrush.physics.RaceEventType
import com.example.sweetrush.render3d.CandyWorldRenderer
import com.example.sweetrush.render3d.RaceHudSnapshot
import com.example.sweetrush.render3d.RenderSceneMode
import com.example.sweetrush.storage.SaveRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ToastNotification(
    val title: String,
    val subtitle: String = "",
    val accentHex: Long = 0xFFFF3D8B,
    val id: Long = System.nanoTime()
)

class SweetRushViewModel(application: Application) : AndroidViewModel(application) {

    private val saveRepo = SaveRepository(application)
    val audioEngine = CandyAudioEngine()

    private val _profile = MutableStateFlow(saveRepo.loadProfile())
    val profile: StateFlow<PlayerProfile> = _profile.asStateFlow()

    private val _currentScreen = MutableStateFlow(GameScreen.MAIN_MENU)
    val currentScreen: StateFlow<GameScreen> = _currentScreen.asStateFlow()

    private val _selectedTrack = MutableStateFlow(TrackId.PEPPERMINT_PLAZA)
    val selectedTrack: StateFlow<TrackId> = _selectedTrack.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow(Difficulty.NORMAL)
    val selectedDifficulty: StateFlow<Difficulty> = _selectedDifficulty.asStateFlow()

    private val _activeMode = MutableStateFlow(GameMode.QUICK_RACE)
    val activeMode: StateFlow<GameMode> = _activeMode.asStateFlow()

    private val _activeChallenge = MutableStateFlow<ChallengeSpec?>(null)
    val activeChallenge: StateFlow<ChallengeSpec?> = _activeChallenge.asStateFlow()

    private val _grandPrixSession = MutableStateFlow<GrandPrixSession?>(null)
    val grandPrixSession: StateFlow<GrandPrixSession?> = _grandPrixSession.asStateFlow()

    private val _raceHud = MutableStateFlow(RaceHudSnapshot())
    val raceHud: StateFlow<RaceHudSnapshot> = _raceHud.asStateFlow()

    private val _isRacePaused = MutableStateFlow(false)
    val isRacePaused: StateFlow<Boolean> = _isRacePaused.asStateFlow()

    private val _raceSummary = MutableStateFlow<RaceSummary?>(null)
    val raceSummary: StateFlow<RaceSummary?> = _raceSummary.asStateFlow()

    private val _raceNotification = MutableStateFlow<ToastNotification?>(null)
    val raceNotification: StateFlow<ToastNotification?> = _raceNotification.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // Track single-frame impulses safely
    private var pendingJumpImpulse = false
    private var pendingPowerUpImpulse = false
    private var lastTouchInput = PlayerInputState()
    private var tiltSteerValue = 0f
    private var finishingRaceHandled = false

    val renderer = CandyWorldRenderer { snap, events ->
        _raceHud.value = snap
        handleRaceEvents(events)
        if (snap.finished && !finishingRaceHandled) {
            finishingRaceHandled = true
            viewModelScope.launch {
                // Let victory orbit camera & fireworks play briefly before Results screen
                delay(2200L)
                finalizeRaceResults()
            }
        }
    }

    init {
        syncRendererWithProfile(_profile.value)
        audioEngine.setVolumes(_profile.value.settings.musicVolume, _profile.value.settings.sfxVolume)
        audioEngine.playMusic(MusicTrack.MENU)
    }

    private fun syncRendererWithProfile(p: PlayerProfile) {
        renderer.graphicsQuality = p.settings.graphicsQuality
        renderer.targetFps = p.settings.targetFps
        renderer.cameraSensitivity = p.settings.cameraSensitivity
        renderer.showroomKartId = p.selectedKartId
        renderer.showroomRacerId = p.selectedRacerId
        renderer.showroomCustom = p.kartCustomizations[p.selectedKartId] ?: KartCustomization()
    }

    fun navigateTo(screen: GameScreen) {
        audioEngine.playSfx(SoundEffect.UI_CLICK)
        _currentScreen.value = screen
        when (screen) {
            GameScreen.GARAGE, GameScreen.CHARACTER_SELECT -> {
                renderer.sceneMode = RenderSceneMode.GARAGE_SHOWROOM
                audioEngine.playMusic(MusicTrack.MENU)
            }
            GameScreen.RACE -> {
                renderer.sceneMode = RenderSceneMode.RACE
                audioEngine.playMusic(MusicTrack.RACE)
            }
            GameScreen.RESULTS -> {
                renderer.sceneMode = RenderSceneMode.GARAGE_SHOWROOM
                audioEngine.playMusic(MusicTrack.VICTORY)
            }
            else -> {
                renderer.sceneMode = RenderSceneMode.MENU_KINGDOM
                audioEngine.playMusic(MusicTrack.MENU)
            }
        }
    }

    fun adjustShowroomZoom(delta: Float) {
        renderer.showroomAutoRotate = false
        renderer.showroomZoom = (renderer.showroomZoom + delta).coerceIn(3.2f, 8.5f)
    }

    fun toggleDebugTelemetry() {
        updateSettings { it.copy(showDebugTelemetry = !it.showDebugTelemetry) }
    }

    fun selectTrack(trackId: TrackId) {
        audioEngine.playSfx(SoundEffect.UI_CLICK)
        _selectedTrack.value = trackId
    }

    fun selectDifficulty(diff: Difficulty) {
        audioEngine.playSfx(SoundEffect.UI_CLICK)
        _selectedDifficulty.value = diff
    }

    fun startFreeRoam(startRegion: WorldRegion = WorldRegion.CANDY_CITY) {
        val curEngine = renderer.physicsEngine
        if (curEngine != null && curEngine.isFreeRoam && _activeMode.value == GameMode.FREE_ROAM) {
            curEngine.teleportPlayerToRegion(startRegion)
            _isRacePaused.value = false
            renderer.isPaused = false
            navigateTo(GameScreen.RACE)
            showRaceToast(startRegion.displayName, startRegion.subtitle, startRegion.accentHex)
            return
        }
        _activeChallenge.value = null
        _grandPrixSession.value = null
        launchRace(TrackId.PEPPERMINT_PLAZA, GameMode.FREE_ROAM, Difficulty.NORMAL, startRegion)
        showRaceToast("OPEN WORLD", "Explore all 5 Candy Regions!", startRegion.accentHex)
    }

    fun enterNearbyPortalRace() {
        val portal = renderer.physicsEngine?.nearbyRacePortal ?: return
        startQuickRace(portal.trackId, _selectedDifficulty.value)
    }

    fun startQuickRace(trackId: TrackId = _selectedTrack.value, difficulty: Difficulty = _selectedDifficulty.value) {
        _activeChallenge.value = null
        _grandPrixSession.value = null
        launchRace(trackId, GameMode.QUICK_RACE, difficulty)
    }

    fun startTimeTrial(trackId: TrackId = _selectedTrack.value) {
        _activeChallenge.value = null
        _grandPrixSession.value = null
        launchRace(trackId, GameMode.TIME_TRIAL, Difficulty.NORMAL)
    }

    fun startPractice(trackId: TrackId = _selectedTrack.value) {
        _activeChallenge.value = null
        _grandPrixSession.value = null
        launchRace(trackId, GameMode.PRACTICE, Difficulty.EASY)
    }

    fun startGrandPrix(difficulty: Difficulty = _selectedDifficulty.value) {
        _activeChallenge.value = null
        val gp = GrandPrixSession(difficulty = difficulty, currentRaceIndex = 0)
        _grandPrixSession.value = gp
        launchRace(gp.trackOrder[0], GameMode.GRAND_PRIX, difficulty)
    }

    fun advanceGrandPrixNextRace() {
        val gp = _grandPrixSession.value ?: return
        val nextIdx = gp.currentRaceIndex + 1
        if (nextIdx >= gp.trackOrder.size) {
            _grandPrixSession.value = null
            navigateTo(GameScreen.MAIN_MENU)
            return
        }
        val nextGp = gp.copy(currentRaceIndex = nextIdx)
        _grandPrixSession.value = nextGp
        launchRace(nextGp.trackOrder[nextIdx], GameMode.GRAND_PRIX, nextGp.difficulty)
    }

    fun startChallenge(challenge: ChallengeSpec) {
        _grandPrixSession.value = null
        _activeChallenge.value = challenge
        launchRace(challenge.trackId, GameMode.CHALLENGE, challenge.difficulty)
    }

    private fun launchRace(
        trackId: TrackId,
        mode: GameMode,
        difficulty: Difficulty,
        startRegion: WorldRegion = WorldRegion.CANDY_CITY
    ) {
        val p = _profile.value
        val custom = p.kartCustomizations[p.selectedKartId] ?: KartCustomization()
        val engine = KartPhysicsEngine(
            trackId = trackId,
            mode = mode,
            difficulty = difficulty,
            playerRacerId = p.selectedRacerId,
            playerKartId = p.selectedKartId,
            playerCustomization = custom,
            startRegion = startRegion,
            initiallyCollectedCoins = p.collectedWorldCoins,
            initiallyCollectedStars = p.collectedWorldStars,
            initiallyDiscoveredRegions = p.discoveredRegions
        )
        _activeMode.value = mode
        _selectedTrack.value = trackId
        _selectedDifficulty.value = difficulty
        _isRacePaused.value = false
        finishingRaceHandled = false
        lastTouchInput = PlayerInputState()
        pendingJumpImpulse = false
        pendingPowerUpImpulse = false

        renderer.isPaused = false
        renderer.physicsEngine = engine
        navigateTo(GameScreen.RACE)
    }

    fun togglePause(paused: Boolean) {
        _isRacePaused.value = paused
        renderer.isPaused = paused
    }

    fun finishTimeTrialOrPractice() {
        if (!finishingRaceHandled) {
            finishingRaceHandled = true
            finalizeRaceResults()
        }
    }

    fun updateTouchControls(
        steer: Float,
        throttle: Boolean,
        brake: Boolean,
        drift: Boolean,
        nitro: Boolean
    ) {
        val p = _profile.value
        val sens = p.settings.steeringSensitivity
        val finalSteer = if (p.settings.tiltSteeringEnabled || p.settings.controlScheme == ControlScheme.TILT_STEER) {
            (tiltSteerValue * sens + steer).coerceIn(-1f, 1f)
        } else {
            (steer * sens).coerceIn(-1f, 1f)
        }
        lastTouchInput = PlayerInputState(
            steer = finalSteer,
            throttle = throttle,
            brake = brake,
            drift = drift,
            nitro = nitro,
            jumpPressed = pendingJumpImpulse,
            usePowerUpPressed = pendingPowerUpImpulse
        )
        pendingJumpImpulse = false
        pendingPowerUpImpulse = false
        renderer.currentInput = lastTouchInput
    }

    fun updateTiltSteer(rawTilt: Float) {
        tiltSteerValue = rawTilt.coerceIn(-1f, 1f)
        val p = _profile.value
        if (p.settings.tiltSteeringEnabled || p.settings.controlScheme == ControlScheme.TILT_STEER) {
            val combined = (tiltSteerValue * p.settings.steeringSensitivity).coerceIn(-1f, 1f)
            lastTouchInput = lastTouchInput.copy(steer = combined)
            renderer.currentInput = lastTouchInput
        }
    }

    fun triggerJump() {
        pendingJumpImpulse = true
        lastTouchInput = lastTouchInput.copy(jumpPressed = true)
        renderer.currentInput = lastTouchInput
    }

    fun triggerPowerUp() {
        pendingPowerUpImpulse = true
        lastTouchInput = lastTouchInput.copy(usePowerUpPressed = true)
        renderer.currentInput = lastTouchInput
    }

    private fun handleRaceEvents(events: List<RaceEvent>) {
        if (events.isEmpty()) return
        for (ev in events) {
            when (ev.type) {
                RaceEventType.COUNTDOWN_TICK -> audioEngine.playSfx(SoundEffect.COUNTDOWN_BEEP)
                RaceEventType.RACE_GO -> {
                    audioEngine.playSfx(SoundEffect.COUNTDOWN_GO)
                    showRaceToast("SWEET RUSH!", "Pedal to the frosting!", 0xFF3CE8B5)
                }
                RaceEventType.DRIFT_RELEASE -> {
                    audioEngine.playSfx(SoundEffect.DRIFT_SPARK)
                    showRaceToast(ev.message, "Mini-Turbo Boost", 0xFFFFB800)
                }
                RaceEventType.NITRO_START -> audioEngine.playSfx(SoundEffect.NITRO_BOOST)
                RaceEventType.JUMP -> audioEngine.playSfx(SoundEffect.JUMP)
                RaceEventType.POWERUP_PICKUP -> {
                    audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
                    showRaceToast(ev.powerUp?.displayName ?: ev.message, ev.powerUp?.shortDesc ?: "", ev.powerUp?.colorArgb ?: 0xFFFF3D8B)
                }
                RaceEventType.POWERUP_USED -> audioEngine.playSfx(SoundEffect.POWERUP_USE)
                RaceEventType.COLLISION -> {
                    audioEngine.playSfx(SoundEffect.COLLISION)
                    vibrateShort(35L)
                }
                RaceEventType.HAZARD_HIT -> {
                    audioEngine.playSfx(SoundEffect.COLLISION)
                    showRaceToast(ev.message, "Watch out!", 0xFFFF3355)
                    vibrateShort(70L)
                }
                RaceEventType.LAP_COMPLETED -> {
                    audioEngine.playSfx(SoundEffect.LAP_COMPLETE)
                    showRaceToast(ev.message, "Keep pushing!", 0xFF5CE1E6)
                }
                RaceEventType.OVERTAKE -> {
                    showRaceToast(ev.message, "Sweet pass!", 0xFF3CE8B5)
                }
                RaceEventType.RACE_FINISHED -> {
                    audioEngine.playSfx(SoundEffect.LAP_COMPLETE)
                    showRaceToast("FINISH!", "Checkered Sugar Flag!", 0xFFFFEB3B)
                }
                RaceEventType.COIN_COLLECTED -> {
                    audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
                    val cur = _profile.value
                    saveAndSync(cur.copy(credits = cur.credits + 25, collectedWorldCoins = cur.collectedWorldCoins + ev.stringArg))
                    showRaceToast(ev.message, "Open World Treasure", 0xFFFFD700)
                }
                RaceEventType.STAR_COLLECTED -> {
                    audioEngine.playSfx(SoundEffect.LAP_COMPLETE)
                    val cur = _profile.value
                    saveAndSync(grantXpAndCredits(cur.copy(collectedWorldStars = cur.collectedWorldStars + ev.stringArg), 100, 120))
                    showRaceToast("GOLDEN STAR!", "+120 CC  •  +100 XP", 0xFFFFEA00)
                }
                RaceEventType.REGION_DISCOVERED -> {
                    audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
                    val reg = ev.regionArg
                    if (reg != null) {
                        val cur = _profile.value
                        saveAndSync(grantXpAndCredits(cur.copy(discoveredRegions = cur.discoveredRegions + reg), 60, 50))
                        showRaceToast("NEW REGION!", reg.displayName, reg.accentHex)
                    }
                }
                RaceEventType.WORLD_CHALLENGE_DONE -> {
                    audioEngine.playSfx(SoundEffect.LAP_COMPLETE)
                    val cur = _profile.value
                    saveAndSync(grantXpAndCredits(cur, 120, 150))
                    showRaceToast("CHALLENGE CLEAR!", "${ev.message} (+150 CC)", 0xFF3CE8B5)
                }
            }
        }
    }

    private fun grantXpAndCredits(p: PlayerProfile, addXp: Int, addCredits: Int): PlayerProfile {
        var newXp = p.xp + addXp
        var newLevel = p.level
        while (newXp >= CatalogData.xpForLevel(newLevel)) {
            newXp -= CatalogData.xpForLevel(newLevel)
            newLevel++
        }
        return p.copy(xp = newXp, level = newLevel, credits = p.credits + addCredits)
    }

    private fun showRaceToast(title: String, subtitle: String, colorHex: Long) {
        val note = ToastNotification(title, subtitle, colorHex)
        _raceNotification.value = note
        viewModelScope.launch {
            delay(2000L)
            if (_raceNotification.value?.id == note.id) {
                _raceNotification.value = null
            }
        }
    }

    private fun finalizeRaceResults() {
        val engine = renderer.physicsEngine ?: return
        val standings = engine.buildResults()
        val playerEntry = standings.first { it.isPlayer }
        val mode = engine.mode

        var profile = _profile.value
        val prevBest = profile.bestLapMsByTrack[engine.trackId] ?: Long.MAX_VALUE
        val bestLap = if (playerEntry.bestLapMs < Long.MAX_VALUE) playerEntry.bestLapMs else 0L
        val isNewBest = bestLap > 0L && bestLap < prevBest

        val updatedBestLaps = profile.bestLapMsByTrack.toMutableMap()
        if (isNewBest) {
            updatedBestLaps[engine.trackId] = bestLap
        }

        // Rewards based on mode & position
        var xpEarned = when (mode) {
            GameMode.PRACTICE -> 25
            GameMode.TIME_TRIAL -> if (isNewBest) 120 else 60
            else -> (160 - (playerEntry.position - 1) * 16).coerceAtLeast(35) +
                (engine.difficulty.ordinal * 25)
        }
        var creditsEarned = when (mode) {
            GameMode.PRACTICE -> 20
            GameMode.TIME_TRIAL -> if (isNewBest) 100 else 45
            else -> (140 - (playerEntry.position - 1) * 15).coerceAtLeast(25) +
                (engine.difficulty.ordinal * 20)
        }

        // Check challenge completion
        var completedChallengeId: String? = null
        val ch = _activeChallenge.value
        if (mode == GameMode.CHALLENGE && ch != null) {
            val met = when (ch.targetType) {
                ChallengeTargetType.WIN_RACE -> playerEntry.position == 1
                ChallengeTargetType.DRIFT_BOOSTS -> engine.playerDriftBoostCount >= ch.targetValue
                ChallengeTargetType.AVOID_HAZARDS_WIN -> playerEntry.position == 1 && !engine.playerHitHazard
                ChallengeTargetType.MAX_SPEED_DURATION -> playerEntry.position <= 2
            }
            if (met) {
                completedChallengeId = ch.id
                if (!profile.completedChallenges.contains(ch.id)) {
                    xpEarned += ch.rewardXp
                    creditsEarned += ch.rewardCredits
                }
            }
        }

        // Grand Prix standings update
        var gpFinished = false
        var gpStandingsList: List<Pair<String, Int>> = emptyList()
        val currentGp = _grandPrixSession.value
        if (mode == GameMode.GRAND_PRIX && currentGp != null) {
            val ptsMap = currentGp.cumulativePointsByRacerId.toMutableMap()
            for (entry in standings) {
                ptsMap[entry.racerId] = (ptsMap[entry.racerId] ?: 0) + entry.grandPrixPointsAwarded
            }
            val updatedGp = currentGp.copy(cumulativePointsByRacerId = ptsMap)
            _grandPrixSession.value = updatedGp

            gpStandingsList = ptsMap.entries
                .sortedByDescending { it.value }
                .map { CatalogData.racerById(it.key).name to it.value }

            gpFinished = updatedGp.currentRaceIndex >= updatedGp.trackOrder.size - 1
            if (gpFinished) {
                val playerGpPts = ptsMap[profile.selectedRacerId] ?: 0
                val isGold = gpStandingsList.firstOrNull()?.first == CatalogData.racerById(profile.selectedRacerId).name
                if (isGold) {
                    creditsEarned += 300
                    xpEarned += 220
                }
                profile = profile.copy(
                    grandPrixGoldTrophies = profile.grandPrixGoldTrophies + if (isGold) 1 else 0,
                    grandPrixBestPoints = maxOf(profile.grandPrixBestPoints, playerGpPts)
                )
            }
        }

        // Level progression
        var newXp = profile.xp + xpEarned
        var newLevel = profile.level
        while (newXp >= CatalogData.xpForLevel(newLevel)) {
            newXp -= CatalogData.xpForLevel(newLevel)
            newLevel++
        }

        val updatedCompletedChallenges = profile.completedChallenges.toMutableSet()
        if (completedChallengeId != null) updatedCompletedChallenges.add(completedChallengeId)

        val updatedProfile = profile.copy(
            xp = newXp,
            level = newLevel,
            credits = profile.credits + creditsEarned,
            bestLapMsByTrack = updatedBestLaps,
            completedChallenges = updatedCompletedChallenges,
            racesCompleted = profile.racesCompleted + 1,
            racesWon = profile.racesWon + if (playerEntry.position == 1 && mode != GameMode.PRACTICE) 1 else 0
        )
        saveAndSync(updatedProfile)

        _raceSummary.value = RaceSummary(
            mode = mode,
            trackId = engine.trackId,
            difficulty = engine.difficulty,
            playerPosition = playerEntry.position,
            playerTotalTimeMs = playerEntry.totalTimeMs,
            playerBestLapMs = bestLap,
            isNewBestLap = isNewBest,
            xpEarned = xpEarned,
            creditsEarned = creditsEarned,
            challengeCompletedId = completedChallengeId,
            standings = standings,
            grandPrixCompleted = gpFinished,
            grandPrixTotalStandings = gpStandingsList
        )

        navigateTo(GameScreen.RESULTS)
    }

    // Garage & Character actions
    fun selectOrUnlockKart(kartId: String) {
        val p = _profile.value
        val spec = CatalogData.kartById(kartId)
        if (p.unlockedKarts.contains(kartId)) {
            audioEngine.playSfx(SoundEffect.UI_CLICK)
            saveAndSync(p.copy(selectedKartId = kartId))
        } else if (p.credits >= spec.unlockCost) {
            audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
            saveAndSync(
                p.copy(
                    credits = p.credits - spec.unlockCost,
                    unlockedKarts = p.unlockedKarts + kartId,
                    selectedKartId = kartId
                )
            )
            flashStatus("Unlocked ${spec.name}!")
        } else {
            audioEngine.playSfx(SoundEffect.COLLISION)
            flashStatus("Need ${spec.unlockCost} Candy Credits to unlock ${spec.name}")
        }
    }

    fun selectOrUnlockRacer(racerId: String) {
        val p = _profile.value
        val spec = CatalogData.racerById(racerId)
        if (p.unlockedRacers.contains(racerId)) {
            audioEngine.playSfx(SoundEffect.UI_CLICK)
            saveAndSync(p.copy(selectedRacerId = racerId))
        } else if (p.credits >= spec.unlockCost) {
            audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
            saveAndSync(
                p.copy(
                    credits = p.credits - spec.unlockCost,
                    unlockedRacers = p.unlockedRacers + racerId,
                    selectedRacerId = racerId
                )
            )
            flashStatus("Unlocked ${spec.name}!")
        } else {
            audioEngine.playSfx(SoundEffect.COLLISION)
            flashStatus("Need ${spec.unlockCost} Credits for ${spec.name}")
        }
    }

    fun selectOrUnlockPaint(paintId: String) {
        val p = _profile.value
        val paint = CatalogData.paintById(paintId)
        val kartId = p.selectedKartId
        val curCustom = p.kartCustomizations[kartId] ?: KartCustomization()
        if (p.unlockedPaints.contains(paintId)) {
            audioEngine.playSfx(SoundEffect.UI_CLICK)
            val map = p.kartCustomizations.toMutableMap()
            map[kartId] = curCustom.copy(paintId = paintId)
            saveAndSync(p.copy(kartCustomizations = map))
        } else if (p.credits >= paint.cost) {
            audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
            val map = p.kartCustomizations.toMutableMap()
            map[kartId] = curCustom.copy(paintId = paintId)
            saveAndSync(
                p.copy(
                    credits = p.credits - paint.cost,
                    unlockedPaints = p.unlockedPaints + paintId,
                    kartCustomizations = map
                )
            )
        } else {
            flashStatus("Need ${paint.cost} Credits for ${paint.name}")
        }
    }

    fun selectOrUnlockWheel(wheel: WheelDesign) {
        val p = _profile.value
        val kartId = p.selectedKartId
        val curCustom = p.kartCustomizations[kartId] ?: KartCustomization()
        if (p.unlockedWheels.contains(wheel)) {
            audioEngine.playSfx(SoundEffect.UI_CLICK)
            val map = p.kartCustomizations.toMutableMap()
            map[kartId] = curCustom.copy(wheelDesign = wheel)
            saveAndSync(p.copy(kartCustomizations = map))
        } else if (p.credits >= wheel.cost) {
            audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
            val map = p.kartCustomizations.toMutableMap()
            map[kartId] = curCustom.copy(wheelDesign = wheel)
            saveAndSync(
                p.copy(
                    credits = p.credits - wheel.cost,
                    unlockedWheels = p.unlockedWheels + wheel,
                    kartCustomizations = map
                )
            )
        } else {
            flashStatus("Need ${wheel.cost} Credits for ${wheel.displayName}")
        }
    }

    fun selectOrUnlockExhaust(exhaust: ExhaustStyle) {
        val p = _profile.value
        val kartId = p.selectedKartId
        val curCustom = p.kartCustomizations[kartId] ?: KartCustomization()
        if (p.unlockedExhausts.contains(exhaust)) {
            audioEngine.playSfx(SoundEffect.UI_CLICK)
            val map = p.kartCustomizations.toMutableMap()
            map[kartId] = curCustom.copy(exhaustStyle = exhaust)
            saveAndSync(p.copy(kartCustomizations = map))
        } else if (p.credits >= exhaust.cost) {
            audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
            val map = p.kartCustomizations.toMutableMap()
            map[kartId] = curCustom.copy(exhaustStyle = exhaust)
            saveAndSync(
                p.copy(
                    credits = p.credits - exhaust.cost,
                    unlockedExhausts = p.unlockedExhausts + exhaust,
                    kartCustomizations = map
                )
            )
        } else {
            flashStatus("Need ${exhaust.cost} Credits for ${exhaust.displayName}")
        }
    }

    fun selectOrUnlockDecoration(deco: KartDecoration) {
        val p = _profile.value
        val kartId = p.selectedKartId
        val curCustom = p.kartCustomizations[kartId] ?: KartCustomization()
        if (p.unlockedDecorations.contains(deco)) {
            audioEngine.playSfx(SoundEffect.UI_CLICK)
            val map = p.kartCustomizations.toMutableMap()
            map[kartId] = curCustom.copy(decoration = deco)
            saveAndSync(p.copy(kartCustomizations = map))
        } else if (p.credits >= deco.cost) {
            audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
            val map = p.kartCustomizations.toMutableMap()
            map[kartId] = curCustom.copy(decoration = deco)
            saveAndSync(
                p.copy(
                    credits = p.credits - deco.cost,
                    unlockedDecorations = p.unlockedDecorations + deco,
                    kartCustomizations = map
                )
            )
        } else {
            flashStatus("Need ${deco.cost} Credits for ${deco.displayName}")
        }
    }

    fun upgradeKartStat(statKey: String) {
        val p = _profile.value
        val kartId = p.selectedKartId
        val curCustom = p.kartCustomizations[kartId] ?: KartCustomization()
        val u = curCustom.upgrades
        val curLevel = when (statKey) {
            "speed" -> u.speed
            "acceleration" -> u.acceleration
            "handling" -> u.handling
            "boost" -> u.boost
            else -> u.stability
        }
        if (curLevel >= 5) {
            flashStatus("Stat already maxed!")
            return
        }
        val cost = 90 + curLevel * 70
        if (p.credits < cost) {
            audioEngine.playSfx(SoundEffect.COLLISION)
            flashStatus("Need $cost Credits to upgrade")
            return
        }
        val newU = when (statKey) {
            "speed" -> u.copy(speed = u.speed + 1)
            "acceleration" -> u.copy(acceleration = u.acceleration + 1)
            "handling" -> u.copy(handling = u.handling + 1)
            "boost" -> u.copy(boost = u.boost + 1)
            else -> u.copy(stability = u.stability + 1)
        }
        val map = p.kartCustomizations.toMutableMap()
        map[kartId] = curCustom.copy(upgrades = newU)
        audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
        saveAndSync(p.copy(credits = p.credits - cost, kartCustomizations = map))
    }

    // Settings
    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        val updated = transform(_profile.value.settings)
        audioEngine.setVolumes(updated.musicVolume, updated.sfxVolume)
        saveAndSync(_profile.value.copy(settings = updated))
    }

    fun restoreDefaultSettings() {
        audioEngine.playSfx(SoundEffect.UI_CLICK)
        val defaults = GameSettings()
        audioEngine.setVolumes(defaults.musicVolume, defaults.sfxVolume)
        saveAndSync(_profile.value.copy(settings = defaults))
        flashStatus("Settings restored to defaults")
    }

    private fun saveAndSync(updated: PlayerProfile) {
        _profile.value = updated
        syncRendererWithProfile(updated)
        saveRepo.saveProfile(updated)
    }

    private fun flashStatus(msg: String) {
        _statusBanner.value = msg
        viewModelScope.launch {
            delay(2200L)
            if (_statusBanner.value == msg) _statusBanner.value = null
        }
    }

    private fun vibrateShort(ms: Long) {
        try {
            val app = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
