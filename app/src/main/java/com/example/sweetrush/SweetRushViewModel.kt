package com.example.sweetrush

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sweetrush.audio.CandyAudioEngine
import com.example.sweetrush.audio.MusicTrack
import com.example.sweetrush.audio.SoundEffect
import com.example.sweetrush.audio.VoiceChatEngine
import com.example.sweetrush.model.CameraPreset
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
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.KartDecoration
import com.example.sweetrush.model.PlayerProfile
import com.example.sweetrush.model.RaceSummary
import com.example.sweetrush.model.SquadTeam
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.model.WheelDesign
import com.example.sweetrush.model.WorldRegion
import com.example.sweetrush.online.FriendRequestDoc
import com.example.sweetrush.online.FriendRosterEntry
import com.example.sweetrush.online.GameInviteDoc
import com.example.sweetrush.online.InviteNotifier
import com.example.sweetrush.online.OnlineMultiplayerRepository
import com.example.sweetrush.online.OnlineRoomDoc
import com.example.sweetrush.online.OperationType
import com.example.sweetrush.online.RoomMessageDoc
import com.example.sweetrush.online.RoomPlayerDoc
import com.example.sweetrush.online.handleFirestoreError
import com.example.sweetrush.physics.KartPhysicsEngine
import com.example.sweetrush.physics.OnlineRacerParticipant
import com.example.sweetrush.physics.PlayerInputState
import com.example.sweetrush.physics.RaceEvent
import com.example.sweetrush.physics.RaceEventType
import com.example.sweetrush.render3d.CandyWorldRenderer
import com.example.sweetrush.render3d.RaceHudSnapshot
import com.example.sweetrush.render3d.RenderSceneMode
import com.example.sweetrush.storage.SaveRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ToastNotification(
    val title: String,
    val subtitle: String = "",
    val accentHex: Long = 0xFFFF3D8B,
    val id: Long = System.nanoTime()
)

@OptIn(ExperimentalCoroutinesApi::class)
class SweetRushViewModel(
    appContext: Context,
    private val onlineRepo: OnlineMultiplayerRepository,
    val currentUserId: String,
    val userDisplayName: String,
    val userPhotoUrl: String?
) : ViewModel() {

    private val context = appContext.applicationContext
    private val saveRepo = SaveRepository(context)
    val audioEngine = CandyAudioEngine()
    val voiceChatEngine = VoiceChatEngine(context)

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

    // Online Multiplayer, Friends, Squads & Voice Comms State
    private val _friendCode = MutableStateFlow(onlineRepo.computeFriendCode(currentUserId))
    val friendCode: StateFlow<String> = _friendCode.asStateFlow()

    private val _activeRoomCode = MutableStateFlow<String?>(null)
    val activeRoomCode: StateFlow<String?> = _activeRoomCode.asStateFlow()

    private val _isOnlineBusy = MutableStateFlow(false)
    val isOnlineBusy: StateFlow<Boolean> = _isOnlineBusy.asStateFlow()

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _isVoiceMuted = MutableStateFlow(false)
    val isVoiceMuted: StateFlow<Boolean> = _isVoiceMuted.asStateFlow()

    private val _friendPresence = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val friendPresence: StateFlow<Map<String, Boolean>> = _friendPresence.asStateFlow()

    private val _sarcasticTaunt = MutableStateFlow<ToastNotification?>(null)
    val sarcasticTaunt: StateFlow<ToastNotification?> = _sarcasticTaunt.asStateFlow()

    val friends: StateFlow<List<FriendRosterEntry>> = onlineRepo.observeFriends(currentUserId)
        .catch { e ->
            val fsEx = findFirestoreException(e)
            if (fsEx != null) {
                handleFirestoreError(fsEx, OperationType.LIST, "users/$currentUserId/friends")
                emit(emptyList())
            } else {
                throw e
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val friendRequests: StateFlow<List<FriendRequestDoc>> = onlineRepo.observeFriendRequests(currentUserId)
        .catch { e ->
            val fsEx = findFirestoreException(e)
            if (fsEx != null) {
                handleFirestoreError(fsEx, OperationType.LIST, "friendRequests")
                emit(emptyList())
            } else {
                throw e
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val gameInvites: StateFlow<List<GameInviteDoc>> = onlineRepo.observeGameInvites(currentUserId)
        .catch { e ->
            val fsEx = findFirestoreException(e)
            if (fsEx != null) {
                handleFirestoreError(fsEx, OperationType.LIST, "gameInvites")
                emit(emptyList())
            } else {
                throw e
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val onlineRoom: StateFlow<OnlineRoomDoc?> = _activeRoomCode
        .flatMapLatest { code ->
            if (code == null) flowOf(null)
            else onlineRepo.observeRoom(code).catch { e ->
                val fsEx = findFirestoreException(e)
                if (fsEx != null) {
                    handleFirestoreError(fsEx, OperationType.GET, "rooms/$code")
                    emit(null)
                } else {
                    throw e
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val roomPlayers: StateFlow<List<RoomPlayerDoc>> = _activeRoomCode
        .flatMapLatest { code ->
            if (code == null) flowOf(emptyList())
            else onlineRepo.observeRoomPlayers(code, currentUserId).catch { e ->
                val fsEx = findFirestoreException(e)
                if (fsEx != null) {
                    handleFirestoreError(fsEx, OperationType.LIST, "rooms/$code/players")
                    emit(emptyList())
                } else {
                    throw e
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val roomMessages: StateFlow<List<RoomMessageDoc>> = _activeRoomCode
        .flatMapLatest { code ->
            if (code == null) flowOf(emptyList())
            else onlineRepo.observeRoomMessages(code, currentUserId).catch { e ->
                val fsEx = findFirestoreException(e)
                if (fsEx != null) {
                    handleFirestoreError(fsEx, OperationType.LIST, "rooms/$code/messages")
                    emit(emptyList())
                } else {
                    throw e
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Single-frame impulses & online runtime bookkeeping
    private var pendingJumpImpulse = false
    private var pendingPowerUpImpulse = false
    private var lastTouchInput = PlayerInputState()
    private var tiltSteerValue = 0f
    private var finishingRaceHandled = false

    private var onlineMatchStartedForStatus = false
    private var joinedRoomEpochMs = System.currentTimeMillis()
    private val playedVoiceIds = HashSet<String>()
    private val notifiedInviteKeys = HashSet<String>()
    private val shownRemoteTauntIds = HashSet<String>()
    private val syncedAcceptedReqIds = HashSet<String>()
    private var lastSyncedMembersSignature: List<String> = emptyList()
    private var telemetryLoopJob: Job? = null
    private var cloudSyncDebounceJob: Job? = null

    val renderer = CandyWorldRenderer { snap, events ->
        _raceHud.value = snap
        handleRaceEvents(events)
        if (snap.finished && !finishingRaceHandled) {
            finishingRaceHandled = true
            viewModelScope.launch {
                delay(2200L)
                finalizeRaceResults()
            }
        }
    }

    init {
        syncRendererWithProfile(_profile.value)
        audioEngine.setVolumes(_profile.value.settings.musicVolume, _profile.value.settings.sfxVolume)
        audioEngine.playMusic(MusicTrack.MENU)
        bootstrapCloudProfile()
        observeOnlineRoomCoordinators()
        startPresenceLoops()
    }

    private fun findFirestoreException(t: Throwable?): FirebaseFirestoreException? {
        var cur = t
        while (cur != null) {
            if (cur is FirebaseFirestoreException) return cur
            if (cur is CancellationException && cur.cause == null) return null
            cur = cur.cause
        }
        return null
    }

    private fun bootstrapCloudProfile() {
        viewModelScope.launch {
            val local = _profile.value
            val localJson = saveRepo.serializeProfile(local).toString()
            val res = onlineRepo.ensureUserProfileAndDirectory(
                uid = currentUserId,
                displayName = userDisplayName,
                photoUrl = userPhotoUrl,
                localProfile = local,
                localProfileJson = localJson
            )
            res.onSuccess { cloud ->
                _friendCode.value = cloud.friendCode.ifBlank { _friendCode.value }
                if (cloud.profileJson.length > 10 && cloud.racesCompleted > local.racesCompleted) {
                    try {
                        val parsed = saveRepo.parseProfile(JSONObject(cloud.profileJson))
                        _profile.value = parsed
                        saveRepo.saveProfile(parsed)
                        syncRendererWithProfile(parsed)
                        audioEngine.setVolumes(parsed.settings.musicVolume, parsed.settings.sfxVolume)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun observeOnlineRoomCoordinators() {
        viewModelScope.launch {
            onlineRoom.collect { room ->
                val activeCode = _activeRoomCode.value
                if (room == null) {
                    if (activeCode != null && _currentScreen.value == GameScreen.ONLINE_LOBBY) {
                        // Room deleted by host or missing
                        onlineMatchStartedForStatus = false
                    }
                    return@collect
                }

                // Ensure local player's visibleToUids matches room.memberUids so allow list succeeds cleanly
                if (room.memberUids != lastSyncedMembersSignature && room.memberUids.contains(currentUserId)) {
                    lastSyncedMembersSignature = room.memberUids
                    onlineRepo.syncVisibleToUids(room.roomCode, currentUserId, room.memberUids)
                }

                if (room.status == "RACING" && !onlineMatchStartedForStatus) {
                    onlineMatchStartedForStatus = true
                    launchOnlineSessionFromRoom(room, roomPlayers.value)
                } else if (room.status == "WAITING") {
                    onlineMatchStartedForStatus = false
                }
            }
        }

        viewModelScope.launch {
            friendRequests.collect { requests ->
                for (req in requests) {
                    if (req.fromUid == currentUserId &&
                        req.status == "ACCEPTED" &&
                        syncedAcceptedReqIds.add(req.id)
                    ) {
                        onlineRepo.syncAcceptedFriendOnSender(currentUserId, req)
                        audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
                        flashStatus("${req.toName} accepted your friend request!")
                    }
                }
            }
        }

        viewModelScope.launch {
            roomPlayers.collect { players ->
                renderer.lobbyShowcasePlayers = players
                val engine = renderer.physicsEngine
                if (engine != null && engine.isOnlineSession) {
                    engine.updateRemoteStates(players.filter { it.userId != currentUserId })
                }
            }
        }

        viewModelScope.launch {
            friends.collect { list ->
                if (list.isNotEmpty()) {
                    _friendPresence.value = onlineRepo.fetchFriendsPresence(list)
                } else {
                    _friendPresence.value = emptyMap()
                }
            }
        }

        viewModelScope.launch {
            gameInvites.collect { invites ->
                for (invite in invites) {
                    if (invite.toUid == currentUserId && invite.status == "PENDING") {
                        val key = "${invite.id}_${invite.inviteType}_${invite.roomCode}"
                        if (notifiedInviteKeys.add(key)) {
                            audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
                            InviteNotifier.showInviteNotification(context, invite)
                            if (_currentScreen.value == GameScreen.RACE) {
                                showRaceToast(
                                    "دعوة من ${invite.fromName}",
                                    "صديقك يدعوك للعب!",
                                    0xFFFFB800
                                )
                            }
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            roomMessages.collect { messages ->
                val localTeam = roomPlayers.value.firstOrNull { it.userId == currentUserId }?.teamIndex?.toInt() ?: 0
                for (msg in messages) {
                    if (msg.msgType == "VOICE" &&
                        msg.senderUid != currentUserId &&
                        !msg.audioB64.isNullOrBlank() &&
                        msg.clientSeq >= joinedRoomEpochMs - 2000L &&
                        (!msg.targetTeamOnly || msg.teamIndex.toInt() == localTeam) &&
                        playedVoiceIds.add(msg.id)
                    ) {
                        voiceChatEngine.playVoiceBase64(msg.audioB64)
                    } else if (msg.msgType == "QUICK" &&
                        msg.senderUid != currentUserId &&
                        msg.text.startsWith("😏 ") &&
                        msg.clientSeq >= joinedRoomEpochMs - 2000L &&
                        (!msg.targetTeamOnly || msg.teamIndex.toInt() == localTeam) &&
                        shownRemoteTauntIds.add(msg.id)
                    ) {
                        showSarcasticTaunt(msg.senderName, msg.text.removePrefix("😏 ").trim())
                    }
                }
            }
        }
    }

    private fun startPresenceLoops() {
        viewModelScope.launch {
            while (isActive) {
                onlineRepo.updatePresence(currentUserId, true)
                delay(25_000L)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                val list = friends.value
                if (list.isNotEmpty() && (_currentScreen.value == GameScreen.ONLINE_LOBBY || _currentScreen.value == GameScreen.MAIN_MENU)) {
                    _friendPresence.value = onlineRepo.fetchFriendsPresence(list)
                }
                delay(12_000L)
            }
        }
    }

    fun setPresenceOnline(online: Boolean) {
        viewModelScope.launch {
            onlineRepo.updatePresence(currentUserId, online)
            if (online) {
                val list = friends.value
                if (list.isNotEmpty()) {
                    _friendPresence.value = onlineRepo.fetchFriendsPresence(list)
                }
            }
        }
    }

    private fun syncRendererWithProfile(p: PlayerProfile) {
        renderer.graphicsQuality = p.settings.graphicsQuality
        renderer.targetFps = p.settings.targetFps
        renderer.cameraSensitivity = p.settings.cameraSensitivity
        renderer.cameraPreset = p.settings.cameraPreset
        renderer.cameraDistanceScale = p.settings.cameraDistanceScale
        renderer.cameraHeightOffset = p.settings.cameraHeightOffset
        renderer.cameraFovOffset = p.settings.cameraFovOffset
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
            GameScreen.ONLINE_LOBBY -> {
                renderer.lobbyShowcasePlayers = roomPlayers.value
                renderer.sceneMode = RenderSceneMode.ONLINE_SQUAD_LOBBY
                audioEngine.playMusic(MusicTrack.MENU)
                viewModelScope.launch {
                    val list = friends.value
                    if (list.isNotEmpty()) {
                        _friendPresence.value = onlineRepo.fetchFriendsPresence(list)
                    }
                }
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

    // Camera Viewing Angle Controls
    fun cycleCameraPreset() {
        val all = CameraPreset.entries
        val cur = _profile.value.settings.cameraPreset
        val next = all[(cur.ordinal + 1) % all.size]
        selectCameraPreset(next)
    }

    fun selectCameraPreset(preset: CameraPreset) {
        audioEngine.playSfx(SoundEffect.UI_CLICK)
        renderer.resetRaceCameraOffsets()
        updateSettings { it.copy(cameraPreset = preset) }
        val lang = _profile.value.settings.language
        val title = if (lang == com.example.sweetrush.model.AppLanguage.AR) "الكاميرا: ${preset.labelAr}" else "CAM: ${preset.label.uppercase()}"
        showRaceToast(title, if (preset == CameraPreset.FREE_ORBIT) "Drag 3D view freely" else "View angle updated", 0xFF5CE1E6)
    }

    fun resetRaceCameraAngle() {
        audioEngine.playSfx(SoundEffect.UI_CLICK)
        renderer.resetRaceCameraOffsets()
    }

    fun adjustShowroomZoom(delta: Float) {
        renderer.cancelShowroomCameraPreset()
        renderer.showroomAutoRotate = false
        renderer.showroomZoom = (renderer.showroomZoom + delta).coerceIn(3.2f, 8.5f)
    }

    fun focusShowroomCamera(preset: String) {
        audioEngine.playSfx(SoundEffect.UI_CLICK)
        renderer.focusShowroomCamera(preset)
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

    // ===================================================================
    // ONLINE MULTIPLAYER, SQUADS (1 / 2 / 4), FRIENDS & VOICE/TEXT COMMS
    // ===================================================================

    fun sendFriendRequestByCode(code: String) {
        viewModelScope.launch {
            _isOnlineBusy.value = true
            val res = onlineRepo.sendFriendRequestByCode(
                uid = currentUserId,
                displayName = userDisplayName,
                profile = _profile.value,
                rawCode = code
            )
            _isOnlineBusy.value = false
            res.onSuccess { req ->
                audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
                flashStatus("Friend request sent to ${req.toName} (${req.toFriendCode})!")
            }.onFailure { err ->
                flashStatus(err.message ?: "Could not send friend request")
            }
        }
    }

    fun respondToFriendRequest(request: FriendRequestDoc, accept: Boolean) {
        viewModelScope.launch {
            _isOnlineBusy.value = true
            val res = onlineRepo.respondToFriendRequest(currentUserId, request, accept)
            _isOnlineBusy.value = false
            res.onSuccess {
                audioEngine.playSfx(if (accept) SoundEffect.POWERUP_PICKUP else SoundEffect.UI_CLICK)
                flashStatus(if (accept) "${request.fromName} is now your friend!" else "Request declined")
            }.onFailure { err ->
                flashStatus(err.message ?: "Could not respond to request")
            }
        }
    }

    fun dismissFriendRequest(requestId: String) {
        viewModelScope.launch {
            onlineRepo.deleteFriendRequest(currentUserId, requestId)
        }
    }

    fun removeFriend(friendUid: String) {
        viewModelScope.launch {
            onlineRepo.removeFriend(currentUserId, friendUid)
        }
    }

    fun inviteFriendToRoom(friend: FriendRosterEntry) {
        val isOnline = _friendPresence.value[friend.friendUid] == true
        inviteFriendSmart(friend, isOnline)
    }

    fun inviteFriendSmart(friend: FriendRosterEntry, isOnline: Boolean) {
        viewModelScope.launch {
            if (!isOnline) {
                val res = onlineRepo.sendOfflinePlayInvite(
                    uid = currentUserId,
                    senderName = userDisplayName,
                    friendUid = friend.friendUid,
                    currentRoom = onlineRoom.value
                )
                res.onSuccess {
                    audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
                    flashStatus("تم إرسال دعوة الصديق لـ ${friend.displayName}!")
                }.onFailure { err ->
                    flashStatus(err.message ?: "تعذر إرسال الدعوة لـ ${friend.displayName}")
                }
                return@launch
            }

            var targetRoom = onlineRoom.value
            if (targetRoom == null) {
                _isOnlineBusy.value = true
                val createRes = onlineRepo.createRoom(
                    uid = currentUserId,
                    displayName = userDisplayName,
                    profile = _profile.value,
                    teamSize = 2,
                    mode = GameMode.QUICK_RACE,
                    trackId = _selectedTrack.value,
                    fillWithAi = true,
                    isGatheringHub = false
                )
                _isOnlineBusy.value = false
                createRes.onSuccess { created ->
                    joinedRoomEpochMs = System.currentTimeMillis()
                    lastSyncedMembersSignature = created.memberUids
                    onlineMatchStartedForStatus = false
                    _activeRoomCode.value = created.roomCode
                    targetRoom = created
                }.onFailure { err ->
                    flashStatus(err.message ?: "تعذر إنشاء غرفة اللعب")
                    return@launch
                }
            }

            val roomToInvite = targetRoom ?: return@launch
            val res = onlineRepo.sendGameInvite(
                uid = currentUserId,
                senderName = userDisplayName,
                friendUid = friend.friendUid,
                room = roomToInvite,
                inviteType = "LIVE_INVITE"
            )
            res.onSuccess {
                audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
                flashStatus("تم إرسال + دعوة لـ ${friend.displayName} للانضمام (#${roomToInvite.roomCode})!")
            }.onFailure { err ->
                flashStatus(err.message ?: "Could not invite ${friend.displayName}")
            }
        }
    }

    fun respondToGameInvite(invite: GameInviteDoc, accept: Boolean) {
        viewModelScope.launch {
            onlineRepo.respondToGameInvite(currentUserId, invite, accept)
            if (accept) {
                if (invite.isOfflineNudge() || (invite.roomCode == "CANDYPLAZA" && invite.isGatheringHub())) {
                    if (invite.roomCode != "CANDYPLAZA" && invite.roomCode.isNotBlank()) {
                        val joinRes = onlineRepo.joinRoomByCode(currentUserId, userDisplayName, _profile.value, invite.roomCode)
                        if (joinRes.isSuccess) {
                            val room = joinRes.getOrThrow()
                            joinedRoomEpochMs = System.currentTimeMillis()
                            lastSyncedMembersSignature = room.memberUids
                            onlineMatchStartedForStatus = false
                            _activeRoomCode.value = room.roomCode
                            navigateTo(GameScreen.ONLINE_LOBBY)
                            flashStatus("انضممت إلى ${invite.fromName} في اللوبي!")
                            return@launch
                        }
                    }
                    enterGatheringHub(launchDirectWorld = false)
                } else {
                    joinOnlineRoom(invite.roomCode)
                    navigateTo(GameScreen.ONLINE_LOBBY)
                }
            }
        }
    }

    fun enterGatheringHub(launchDirectWorld: Boolean = false) {
        viewModelScope.launch {
            _isOnlineBusy.value = true
            val prevCode = _activeRoomCode.value
            val prevRoom = onlineRoom.value
            if (prevCode != null && prevRoom != null && !prevRoom.isGatheringHub()) {
                onlineRepo.leaveRoom(prevCode, currentUserId, prevRoom.hostUid == currentUserId)
            }
            val res = onlineRepo.enterGatheringHub(
                uid = currentUserId,
                displayName = userDisplayName,
                profile = _profile.value
            )
            _isOnlineBusy.value = false
            res.onSuccess { room ->
                joinedRoomEpochMs = System.currentTimeMillis()
                lastSyncedMembersSignature = room.memberUids
                onlineMatchStartedForStatus = false
                _activeRoomCode.value = room.roomCode
                audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
                if (launchDirectWorld && room.hostUid == currentUserId) {
                    onlineRepo.updateRoomStatus(room.roomCode, "RACING")
                } else {
                    navigateTo(GameScreen.ONLINE_LOBBY)
                    flashStatus("Entered Gathering Hub #${room.roomCode}!")
                }
            }.onFailure { err ->
                flashStatus(err.message ?: "Could not enter Gathering Hub")
            }
        }
    }

    fun createOnlineRoom(
        teamSize: Int = 2,
        mode: GameMode = GameMode.QUICK_RACE,
        trackId: TrackId = _selectedTrack.value,
        fillWithAi: Boolean = true,
        isGatheringHub: Boolean = false
    ) {
        viewModelScope.launch {
            _isOnlineBusy.value = true
            val prevCode = _activeRoomCode.value
            val prevRoom = onlineRoom.value
            if (prevCode != null && prevRoom != null) {
                onlineRepo.leaveRoom(prevCode, currentUserId, prevRoom.hostUid == currentUserId)
            }
            val res = onlineRepo.createRoom(
                uid = currentUserId,
                displayName = userDisplayName,
                profile = _profile.value,
                teamSize = teamSize,
                mode = mode,
                trackId = trackId,
                fillWithAi = fillWithAi,
                isGatheringHub = isGatheringHub
            )
            _isOnlineBusy.value = false
            res.onSuccess { room ->
                joinedRoomEpochMs = System.currentTimeMillis()
                lastSyncedMembersSignature = room.memberUids
                onlineMatchStartedForStatus = false
                _activeRoomCode.value = room.roomCode
                audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
                flashStatus("Room ${room.roomCode} created!")
            }.onFailure { err ->
                flashStatus(err.message ?: "Failed to create room")
            }
        }
    }

    fun joinOnlineRoom(roomCode: String) {
        viewModelScope.launch {
            _isOnlineBusy.value = true
            val prevCode = _activeRoomCode.value
            val prevRoom = onlineRoom.value
            if (prevCode != null && prevRoom != null && prevCode != roomCode.trim().uppercase()) {
                onlineRepo.leaveRoom(prevCode, currentUserId, prevRoom.hostUid == currentUserId)
            }
            val res = onlineRepo.joinRoomByCode(
                uid = currentUserId,
                displayName = userDisplayName,
                profile = _profile.value,
                rawRoomCode = roomCode
            )
            _isOnlineBusy.value = false
            res.onSuccess { room ->
                joinedRoomEpochMs = System.currentTimeMillis()
                lastSyncedMembersSignature = room.memberUids
                onlineMatchStartedForStatus = false
                _activeRoomCode.value = room.roomCode
                audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
                flashStatus("Joined room ${room.roomCode}!")
            }.onFailure { err ->
                flashStatus(err.message ?: "Could not join room")
            }
        }
    }

    fun leaveOnlineRoom(navigateBackToMenu: Boolean = false) {
        val code = _activeRoomCode.value
        val room = onlineRoom.value
        stopLiveMicIfActive()
        stopTelemetryLoop()
        _activeRoomCode.value = null
        onlineMatchStartedForStatus = false
        lastSyncedMembersSignature = emptyList()
        if (code != null) {
            viewModelScope.launch {
                onlineRepo.leaveRoom(code, currentUserId, room?.hostUid == currentUserId)
            }
        }
        if (navigateBackToMenu) {
            navigateTo(GameScreen.MAIN_MENU)
        }
    }

    fun updateRoomConfiguration(
        mode: GameMode,
        trackId: TrackId,
        teamSize: Int,
        fillWithAi: Boolean,
        isGatheringHub: Boolean = false
    ) {
        val room = onlineRoom.value ?: return
        if (room.hostUid != currentUserId) return
        viewModelScope.launch {
            onlineRepo.updateRoomConfig(room.roomCode, mode, trackId, teamSize, fillWithAi, isGatheringHub)
                .onFailure { err -> flashStatus(err.message ?: "Could not update room") }
        }
    }

    fun selectTeamAndReady(teamIndex: Int, ready: Boolean) {
        val room = onlineRoom.value ?: return
        viewModelScope.launch {
            onlineRepo.upsertPlayerDoc(
                roomCode = room.roomCode,
                uid = currentUserId,
                displayName = userDisplayName,
                profile = _profile.value,
                teamIndex = teamIndex,
                ready = ready,
                visibleToUids = room.memberUids
            )
        }
    }

    fun startOnlineMatchAsHost() {
        val room = onlineRoom.value ?: return
        if (room.hostUid != currentUserId) return
        viewModelScope.launch {
            onlineRepo.syncVisibleToUids(room.roomCode, currentUserId, room.memberUids)
            onlineRepo.updateRoomStatus(room.roomCode, "RACING")
                .onFailure { err -> flashStatus(err.message ?: "Failed to start match") }
        }
    }

    fun rematchOnlineRoom() {
        val room = onlineRoom.value
        if (room != null && room.hostUid == currentUserId) {
            viewModelScope.launch {
                onlineRepo.updateRoomStatus(room.roomCode, "WAITING")
                onlineMatchStartedForStatus = false
                navigateTo(GameScreen.ONLINE_LOBBY)
            }
        } else {
            onlineMatchStartedForStatus = false
            navigateTo(GameScreen.ONLINE_LOBBY)
        }
    }

    fun sendRoomTextMessage(text: String, teamOnly: Boolean) {
        val room = onlineRoom.value ?: return
        val clean = text.trim()
        if (clean.isEmpty()) return
        val localTeam = roomPlayers.value.firstOrNull { it.userId == currentUserId }?.teamIndex?.toInt() ?: 0
        viewModelScope.launch {
            onlineRepo.sendMessage(
                roomCode = room.roomCode,
                uid = currentUserId,
                senderName = userDisplayName,
                teamIndex = localTeam,
                targetTeamOnly = teamOnly,
                msgType = "TEXT",
                text = clean,
                memberUids = room.memberUids
            )
        }
    }

    fun sendQuickCallout(text: String, teamOnly: Boolean) {
        val room = onlineRoom.value ?: return
        val localTeam = roomPlayers.value.firstOrNull { it.userId == currentUserId }?.teamIndex?.toInt() ?: 0
        audioEngine.playSfx(SoundEffect.UI_CLICK)
        viewModelScope.launch {
            onlineRepo.sendMessage(
                roomCode = room.roomCode,
                uid = currentUserId,
                senderName = userDisplayName,
                teamIndex = localTeam,
                targetTeamOnly = teamOnly,
                msgType = "QUICK",
                text = text,
                memberUids = room.memberUids
            )
        }
    }

    fun toggleLiveMic(teamOnly: Boolean) {
        if (voiceChatEngine.isMicActive) {
            stopLiveMicIfActive()
            audioEngine.playSfx(SoundEffect.UI_CLICK)
            flashStatus("Mic OFF")
            return
        }
        if (!voiceChatEngine.hasRecordPermission()) {
            flashStatus("Microphone permission needed for Voice Chat")
            return
        }
        val room = onlineRoom.value
        if (room == null) {
            flashStatus("Join an online room or Gathering Hub to talk")
            return
        }
        val started = voiceChatEngine.startContinuousMic { b64, durMs ->
            val activeRoom = onlineRoom.value ?: return@startContinuousMic
            val localTeam = roomPlayers.value.firstOrNull { it.userId == currentUserId }?.teamIndex?.toInt() ?: 0
            viewModelScope.launch {
                onlineRepo.sendMessage(
                    roomCode = activeRoom.roomCode,
                    uid = currentUserId,
                    senderName = userDisplayName,
                    teamIndex = localTeam,
                    targetTeamOnly = teamOnly,
                    msgType = "VOICE",
                    text = "Voice (${String.format("%.1fs", durMs / 1000f)})",
                    audioB64 = b64,
                    durationMs = durMs,
                    memberUids = activeRoom.memberUids
                )
            }
        }
        _isRecordingVoice.value = started
        if (started) {
            audioEngine.playSfx(SoundEffect.POWERUP_PICKUP)
            flashStatus("Mic ON — stays open until turned OFF")
        }
    }

    private fun stopLiveMicIfActive() {
        voiceChatEngine.stopContinuousMic()
        _isRecordingVoice.value = false
    }

    fun toggleVoiceMute() {
        val next = !_isVoiceMuted.value
        _isVoiceMuted.value = next
        voiceChatEngine.isMuted = next
    }

    private fun launchOnlineSessionFromRoom(room: OnlineRoomDoc, players: List<RoomPlayerDoc>) {
        val p = _profile.value
        val custom = p.kartCustomizations[p.selectedKartId] ?: KartCustomization()
        val localPlayerDoc = players.firstOrNull { it.userId == currentUserId }
        val localTeam = localPlayerDoc?.teamIndex?.toInt()?.coerceIn(0, 3) ?: 0
        val remotes = players.filter { it.userId != currentUserId }.map { rp ->
            OnlineRacerParticipant(
                userId = rp.userId,
                displayName = rp.displayName,
                racerId = rp.racerId,
                kartId = rp.kartId,
                customization = KartCustomization(
                    paintId = rp.paintId,
                    wheelDesign = enumValues<WheelDesign>().firstOrNull { it.name == rp.wheelId } ?: WheelDesign.PEPPERMINT_SWIRL,
                    exhaustStyle = enumValues<ExhaustStyle>().firstOrNull { it.name == rp.exhaustId } ?: ExhaustStyle.SUGAR_SPARK,
                    decoration = enumValues<KartDecoration>().firstOrNull { it.name == rp.decoId } ?: KartDecoration.NONE
                ),
                teamIndex = rp.teamIndex.toInt().coerceIn(0, 3)
            )
        }
        val mode = room.parsedMode()
        val trackId = room.parsedTrackId()
        val engine = KartPhysicsEngine(
            trackId = trackId,
            mode = mode,
            difficulty = _selectedDifficulty.value,
            playerRacerId = p.selectedRacerId,
            playerKartId = p.selectedKartId,
            playerCustomization = custom,
            startRegion = WorldRegion.CANDY_CITY,
            initiallyCollectedCoins = p.collectedWorldCoins,
            initiallyCollectedStars = p.collectedWorldStars,
            initiallyDiscoveredRegions = p.discoveredRegions,
            isOnlineSession = true,
            teamSize = room.teamSize.toInt().coerceIn(1, 4),
            localTeamIndex = localTeam,
            localDisplayName = userDisplayName,
            onlineParticipants = remotes,
            fillWithAi = room.fillWithAi
        )
        _activeMode.value = mode
        _selectedTrack.value = trackId
        _isRacePaused.value = false
        finishingRaceHandled = false
        lastTouchInput = PlayerInputState()
        pendingJumpImpulse = false
        pendingPowerUpImpulse = false

        renderer.isPaused = false
        renderer.resetRaceCameraOffsets()
        renderer.physicsEngine = engine
        navigateTo(GameScreen.RACE)
        startTelemetryLoop(room.roomCode)
        val team = SquadTeam.fromIndex(localTeam)
        showRaceToast("ONLINE SQUADS", "${team.teamName} • ${room.teamSize}v${room.teamSize}", team.colorHex)
    }

    private fun startTelemetryLoop(roomCode: String) {
        telemetryLoopJob?.cancel()
        telemetryLoopJob = viewModelScope.launch {
            while (isActive) {
                val engine = renderer.physicsEngine
                val room = onlineRoom.value
                if (engine != null && engine.isOnlineSession && room != null && _currentScreen.value == GameScreen.RACE) {
                    val pk = engine.playerKart
                    onlineRepo.pushTelemetry(
                        roomCode = roomCode,
                        uid = currentUserId,
                        x = pk.x.toDouble(),
                        y = pk.y.toDouble(),
                        z = pk.z.toDouble(),
                        yawRad = pk.yawRad.toDouble(),
                        speed = pk.speed.toDouble(),
                        steerVisualRad = pk.steerVisualRad.toDouble(),
                        lap = pk.currentLap,
                        raceScore = pk.raceScore.toDouble(),
                        finished = pk.finished,
                        finishTimeMs = pk.finishTimeMs,
                        drifting = pk.isDrifting,
                        nitroActive = pk.isNitroActive || pk.boostTimer > 0f,
                        visibleToUids = room.memberUids
                    )
                }
                delay(110L)
            }
        }
    }

    private fun stopTelemetryLoop() {
        telemetryLoopJob?.cancel()
        telemetryLoopJob = null
    }

    // ===================================================================
    // OFFLINE & SINGLEPLAYER MODES (PRESERVED 100%)
    // ===================================================================

    fun startFreeRoam(startRegion: WorldRegion = WorldRegion.CANDY_CITY) {
        val curEngine = renderer.physicsEngine
        if (curEngine != null && curEngine.isFreeRoam && _activeMode.value == GameMode.FREE_ROAM && !curEngine.isOnlineSession) {
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
        stopTelemetryLoop()
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
        renderer.resetRaceCameraOffsets()
        renderer.physicsEngine = engine
        navigateTo(GameScreen.RACE)
    }

    fun togglePause(paused: Boolean) {
        _isRacePaused.value = paused
        // Keep online simulation running in background; pause only pauses offline solo physics
        val isOnline = renderer.physicsEngine?.isOnlineSession == true
        renderer.isPaused = paused && !isOnline
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
                RaceEventType.SARCASTIC_TAUNT -> {
                    showSarcasticTaunt(ev.stringArg.ifBlank { "منافس" }, ev.message)
                }
            }
        }
    }

    fun sendSarcasticTaunt(taunt: String) {
        audioEngine.playSfx(SoundEffect.UI_CLICK)
        showSarcasticTaunt(userDisplayName, taunt)
        val room = onlineRoom.value ?: return
        val localTeam = roomPlayers.value.firstOrNull { it.userId == currentUserId }?.teamIndex?.toInt() ?: 0
        viewModelScope.launch {
            onlineRepo.sendMessage(
                roomCode = room.roomCode,
                uid = currentUserId,
                senderName = userDisplayName,
                teamIndex = localTeam,
                targetTeamOnly = false,
                msgType = "QUICK",
                text = "😏 $taunt",
                memberUids = room.memberUids
            )
        }
    }

    private fun showSarcasticTaunt(speaker: String, line: String) {
        val note = ToastNotification(title = speaker, subtitle = line, accentHex = 0xFFFF7A2E)
        _sarcasticTaunt.value = note
        viewModelScope.launch {
            delay(3200L)
            if (_sarcasticTaunt.value?.id == note.id) {
                _sarcasticTaunt.value = null
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
        stopTelemetryLoop()
        val engine = renderer.physicsEngine ?: return
        val standings = engine.buildResults()
        val teamStandings = engine.buildTeamStandings(standings)
        val playerEntry = standings.first { it.isPlayer }
        val mode = engine.mode

        if (engine.isOnlineSession) {
            val room = onlineRoom.value
            if (room != null && room.hostUid == currentUserId) {
                viewModelScope.launch {
                    onlineRepo.updateRoomStatus(room.roomCode, "FINISHED")
                }
            }
        }

        var profile = _profile.value
        val prevBest = profile.bestLapMsByTrack[engine.trackId] ?: Long.MAX_VALUE
        val bestLap = if (playerEntry.bestLapMs < Long.MAX_VALUE) playerEntry.bestLapMs else 0L
        val isNewBest = bestLap > 0L && bestLap < prevBest

        val updatedBestLaps = profile.bestLapMsByTrack.toMutableMap()
        if (isNewBest) {
            updatedBestLaps[engine.trackId] = bestLap
        }

        var xpEarned = when (mode) {
            GameMode.PRACTICE -> 25
            GameMode.TIME_TRIAL -> if (isNewBest) 120 else 60
            else -> (160 - (playerEntry.position - 1) * 16).coerceAtLeast(35) +
                (engine.difficulty.ordinal * 25) +
                if (engine.isOnlineSession) 45 else 0
        }
        var creditsEarned = when (mode) {
            GameMode.PRACTICE -> 20
            GameMode.TIME_TRIAL -> if (isNewBest) 100 else 45
            else -> (140 - (playerEntry.position - 1) * 15).coerceAtLeast(25) +
                (engine.difficulty.ordinal * 20) +
                if (engine.isOnlineSession) 40 else 0
        }

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
            grandPrixTotalStandings = gpStandingsList,
            isOnlineMatch = engine.isOnlineSession,
            teamSize = engine.teamSize,
            teamStandings = teamStandings
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
            renderer.focusShowroomCamera("WHEELS")
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
            renderer.focusShowroomCamera("WHEELS")
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
            renderer.focusShowroomCamera("REAR")
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
            renderer.focusShowroomCamera("REAR")
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
            renderer.focusShowroomCamera("TOP")
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
            renderer.focusShowroomCamera("TOP")
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

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        val updated = transform(_profile.value.settings)
        audioEngine.setVolumes(updated.musicVolume, updated.sfxVolume)
        saveAndSync(_profile.value.copy(settings = updated))
    }

    fun restoreDefaultSettings() {
        audioEngine.playSfx(SoundEffect.UI_CLICK)
        val defaults = GameSettings()
        audioEngine.setVolumes(defaults.musicVolume, defaults.sfxVolume)
        renderer.resetRaceCameraOffsets()
        saveAndSync(_profile.value.copy(settings = defaults))
        flashStatus("Settings restored to defaults")
    }

    private fun saveAndSync(updated: PlayerProfile) {
        _profile.value = updated
        syncRendererWithProfile(updated)
        saveRepo.saveProfile(updated)

        cloudSyncDebounceJob?.cancel()
        cloudSyncDebounceJob = viewModelScope.launch {
            delay(800L)
            val json = saveRepo.serializeProfile(updated).toString()
            onlineRepo.syncCloudProfile(currentUserId, userDisplayName, userPhotoUrl, updated, json)
        }
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
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {
        }
    }

    suspend fun onPreSignOutCleanup() {
        stopLiveMicIfActive()
        stopTelemetryLoop()
        onlineRepo.updatePresence(currentUserId, false)
        val code = _activeRoomCode.value
        val room = onlineRoom.value
        _activeRoomCode.value = null
        if (code != null) {
            onlineRepo.leaveRoom(code, currentUserId, room?.hostUid == currentUserId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveMicIfActive()
        stopTelemetryLoop()
        cloudSyncDebounceJob?.cancel()
        audioEngine.release()
        voiceChatEngine.release()
    }
}
