package com.example

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sweetrush.SweetRushViewModel
import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.GameScreen
import com.example.sweetrush.online.OnlineMultiplayerRepository
import com.example.sweetrush.online.attemptAutoSignIn
import com.example.sweetrush.online.authStateFlow
import com.example.sweetrush.online.onGoogleSignInClicked
import com.example.sweetrush.online.signOutFromGoogle
import com.example.sweetrush.render3d.CandyViewport3D
import com.example.sweetrush.ui.AuthScreen
import com.example.sweetrush.ui.ChallengesScreen
import com.example.sweetrush.ui.GarageAndCharacterScreen
import com.example.sweetrush.ui.MainMenuScreen
import com.example.sweetrush.ui.OnlineLobbyScreen
import com.example.sweetrush.ui.RaceHudScreen
import com.example.sweetrush.ui.RaceSetupScreen
import com.example.sweetrush.ui.ResultsScreen
import com.example.sweetrush.ui.SettingsScreen
import com.example.sweetrush.ui.WorldMapScreen
import com.example.ui.theme.BerryDeep
import com.example.ui.theme.BerryPurple
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.FrostingWhite
import com.example.ui.theme.MintCream
import com.example.ui.theme.SkyCandy
import com.example.ui.theme.StrawberryRed
import com.example.ui.theme.SweetRushTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity(), SensorEventListener {

    private var activeViewModel: SweetRushViewModel? = null
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            SweetRushTheme {
                val appContext = applicationContext
                val credentialManager = remember { CredentialManager.create(appContext) }
                val authScope = rememberCoroutineScope()

                val currentUser: FirebaseUser? by Firebase.auth.authStateFlow()
                    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                var isAuthBusy by remember { mutableStateOf(Firebase.auth.currentUser == null) }
                var authError by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    if (Firebase.auth.currentUser == null) {
                        attemptAutoSignIn(
                            context = this@MainActivity,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isAuthBusy = false
                                authError = null
                            },
                            onUnauthenticated = {
                                isAuthBusy = false
                            },
                            scope = authScope
                        )
                    } else {
                        isAuthBusy = false
                    }
                }

                val signedInUser = currentUser
                if (signedInUser == null) {
                    activeViewModel = null
                    AuthScreen(
                        isLoading = isAuthBusy,
                        errorMessage = authError,
                        onGoogleSignInClick = {
                            isAuthBusy = true
                            authError = null
                            onGoogleSignInClicked(
                                context = this@MainActivity,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isAuthBusy = false
                                    authError = null
                                },
                                onAuthError = { msg ->
                                    isAuthBusy = false
                                    authError = msg
                                },
                                onAuthCancelled = {
                                    isAuthBusy = false
                                },
                                scope = authScope
                            )
                        }
                    )
                } else {
                    key(signedInUser.uid) {
                        val dbId = remember(appContext) { appContext.getString(R.string.firestore_database_id) }
                        val onlineRepo = remember(dbId, signedInUser.uid) {
                            val db = FirebaseFirestore.getInstance(dbId)
                            OnlineMultiplayerRepository(db = db, auth = Firebase.auth)
                        }

                        val viewModel: SweetRushViewModel = viewModel(
                            key = signedInUser.uid,
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return SweetRushViewModel(
                                        appContext = appContext,
                                        onlineRepo = onlineRepo,
                                        currentUserId = signedInUser.uid,
                                        userDisplayName = signedInUser.displayName?.takeIf { it.isNotBlank() }
                                            ?: signedInUser.email?.substringBefore("@")
                                            ?: "Candy Racer",
                                        userPhotoUrl = signedInUser.photoUrl?.toString()
                                    ) as T
                                }
                            }
                        )
                        activeViewModel = viewModel

                        val screen by viewModel.currentScreen.collectAsStateWithLifecycle()
                        val profile by viewModel.profile.collectAsStateWithLifecycle()
                        val selectedTrack by viewModel.selectedTrack.collectAsStateWithLifecycle()
                        val selectedDifficulty by viewModel.selectedDifficulty.collectAsStateWithLifecycle()
                        val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
                        val raceHud by viewModel.raceHud.collectAsStateWithLifecycle()
                        val isPaused by viewModel.isRacePaused.collectAsStateWithLifecycle()
                        val raceNotification by viewModel.raceNotification.collectAsStateWithLifecycle()
                        val raceSummary by viewModel.raceSummary.collectAsStateWithLifecycle()
                        val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
                        val gpSession by viewModel.grandPrixSession.collectAsStateWithLifecycle()

                        val friendCode by viewModel.friendCode.collectAsStateWithLifecycle()
                        val onlineRoom by viewModel.onlineRoom.collectAsStateWithLifecycle()
                        val roomPlayers by viewModel.roomPlayers.collectAsStateWithLifecycle()
                        val roomMessages by viewModel.roomMessages.collectAsStateWithLifecycle()
                        val friends by viewModel.friends.collectAsStateWithLifecycle()
                        val friendPresence by viewModel.friendPresence.collectAsStateWithLifecycle()
                        val friendRequests by viewModel.friendRequests.collectAsStateWithLifecycle()
                        val gameInvites by viewModel.gameInvites.collectAsStateWithLifecycle()
                        val isOnlineBusy by viewModel.isOnlineBusy.collectAsStateWithLifecycle()
                        val isRecordingVoice by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
                        val isVoiceMuted by viewModel.isVoiceMuted.collectAsStateWithLifecycle()
                        val sarcasticTaunt by viewModel.sarcasticTaunt.collectAsStateWithLifecycle()

                        val notifPermLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) { _ -> }

                        LaunchedEffect(signedInUser.uid) {
                            viewModel.setPresenceOnline(true)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    appContext,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }

                        val pendingIncomingReqCount = friendRequests.count {
                            it.toUid == viewModel.currentUserId && it.status == "PENDING"
                        }
                        val pendingInvite = gameInvites.firstOrNull {
                            it.toUid == viewModel.currentUserId && it.status == "PENDING"
                        }

                        val allowShowroomOrbit = screen == GameScreen.GARAGE ||
                            screen == GameScreen.CHARACTER_SELECT ||
                            screen == GameScreen.RESULTS
                        val allowRaceCamDrag = (screen == GameScreen.RACE) && !isPaused

                        Box(modifier = Modifier.fillMaxSize()) {
                            CandyViewport3D(
                                renderer = viewModel.renderer,
                                allowOrbitDrag = allowShowroomOrbit,
                                allowRaceCameraDrag = allowRaceCamDrag,
                                modifier = Modifier.fillMaxSize()
                            )

                            when (screen) {
                                GameScreen.MAIN_MENU -> {
                                    MainMenuScreen(
                                        profile = profile,
                                        userDisplayName = viewModel.userDisplayName,
                                        userPhotoUrl = viewModel.userPhotoUrl,
                                        friendCode = friendCode,
                                        activeRoomCode = onlineRoom?.roomCode,
                                        pendingRequestsCount = pendingIncomingReqCount,
                                        onNavigate = { target -> viewModel.navigateTo(target) },
                                        onStartFreeRoam = { viewModel.startFreeRoam() },
                                        onEnterGatheringHub = { viewModel.enterGatheringHub(launchDirectWorld = false) }
                                    )
                                }

                                GameScreen.ONLINE_LOBBY -> {
                                    OnlineLobbyScreen(
                                        currentUserId = viewModel.currentUserId,
                                        friendCode = friendCode,
                                        profile = profile,
                                        room = onlineRoom,
                                        players = roomPlayers,
                                        messages = roomMessages,
                                        friends = friends,
                                        friendPresence = friendPresence,
                                        friendRequests = friendRequests,
                                        isBusy = isOnlineBusy,
                                        isRecordingVoice = isRecordingVoice,
                                        isVoiceMuted = isVoiceMuted,
                                        statusMessage = statusBanner,
                                        onCreateRoom = viewModel::createOnlineRoom,
                                        onEnterGatheringHub = { viewModel.enterGatheringHub(launchDirectWorld = false) },
                                        onJoinRoom = viewModel::joinOnlineRoom,
                                        onLeaveRoom = { viewModel.leaveOnlineRoom(navigateBackToMenu = false) },
                                        onUpdateRoomConfig = viewModel::updateRoomConfiguration,
                                        onSelectTeamAndReady = viewModel::selectTeamAndReady,
                                        onStartMatch = viewModel::startOnlineMatchAsHost,
                                        onSendFriendRequest = viewModel::sendFriendRequestByCode,
                                        onRespondFriendRequest = viewModel::respondToFriendRequest,
                                        onDismissFriendRequest = viewModel::dismissFriendRequest,
                                        onInviteFriend = viewModel::inviteFriendSmart,
                                        onRemoveFriend = viewModel::removeFriend,
                                        onSendChat = viewModel::sendRoomTextMessage,
                                        onSendQuickCallout = viewModel::sendQuickCallout,
                                        onToggleLiveMic = viewModel::toggleLiveMic,
                                        onToggleVoiceMute = viewModel::toggleVoiceMute,
                                        onBack = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                                    )
                                }

                                GameScreen.WORLD_MAP -> {
                                    WorldMapScreen(
                                        profile = profile,
                                        hud = raceHud,
                                        onFastTravelRegion = { region -> viewModel.startFreeRoam(region) },
                                        onStartRegionRace = { trackId -> viewModel.startQuickRace(trackId, selectedDifficulty) },
                                        onBack = {
                                            if (activeMode == GameMode.FREE_ROAM && viewModel.renderer.physicsEngine?.isFreeRoam == true) {
                                                viewModel.navigateTo(GameScreen.RACE)
                                            } else {
                                                viewModel.navigateTo(GameScreen.MAIN_MENU)
                                            }
                                        }
                                    )
                                }

                                GameScreen.QUICK_RACE_SETUP -> {
                                    RaceSetupScreen(
                                        title = "Quick Race",
                                        mode = GameMode.QUICK_RACE,
                                        isCollectionPreviewOnly = false,
                                        profile = profile,
                                        selectedTrack = selectedTrack,
                                        selectedDifficulty = selectedDifficulty,
                                        onSelectTrack = viewModel::selectTrack,
                                        onSelectDifficulty = viewModel::selectDifficulty,
                                        onStart = { viewModel.startQuickRace() },
                                        onBack = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                                    )
                                }

                                GameScreen.GRAND_PRIX_SETUP -> {
                                    RaceSetupScreen(
                                        title = "Grand Prix Cup",
                                        mode = GameMode.GRAND_PRIX,
                                        isCollectionPreviewOnly = false,
                                        profile = profile,
                                        selectedTrack = selectedTrack,
                                        selectedDifficulty = selectedDifficulty,
                                        onSelectTrack = viewModel::selectTrack,
                                        onSelectDifficulty = viewModel::selectDifficulty,
                                        onStart = { viewModel.startGrandPrix() },
                                        onBack = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                                    )
                                }

                                GameScreen.TIME_TRIAL_SETUP -> {
                                    RaceSetupScreen(
                                        title = "Time Trial",
                                        mode = GameMode.TIME_TRIAL,
                                        isCollectionPreviewOnly = false,
                                        profile = profile,
                                        selectedTrack = selectedTrack,
                                        selectedDifficulty = selectedDifficulty,
                                        onSelectTrack = viewModel::selectTrack,
                                        onSelectDifficulty = viewModel::selectDifficulty,
                                        onStart = { viewModel.startTimeTrial() },
                                        onBack = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                                    )
                                }

                                GameScreen.PRACTICE_SETUP -> {
                                    RaceSetupScreen(
                                        title = "Free Practice",
                                        mode = GameMode.PRACTICE,
                                        isCollectionPreviewOnly = false,
                                        profile = profile,
                                        selectedTrack = selectedTrack,
                                        selectedDifficulty = selectedDifficulty,
                                        onSelectTrack = viewModel::selectTrack,
                                        onSelectDifficulty = viewModel::selectDifficulty,
                                        onStart = { viewModel.startPractice() },
                                        onBack = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                                    )
                                }

                                GameScreen.TRACK_COLLECTION -> {
                                    RaceSetupScreen(
                                        title = "Track Collection",
                                        mode = GameMode.QUICK_RACE,
                                        isCollectionPreviewOnly = true,
                                        profile = profile,
                                        selectedTrack = selectedTrack,
                                        selectedDifficulty = selectedDifficulty,
                                        onSelectTrack = viewModel::selectTrack,
                                        onSelectDifficulty = viewModel::selectDifficulty,
                                        onStart = { viewModel.startQuickRace() },
                                        onBack = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                                    )
                                }

                                GameScreen.CHALLENGES -> {
                                    ChallengesScreen(
                                        profile = profile,
                                        onStartChallenge = viewModel::startChallenge,
                                        onBack = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                                    )
                                }

                                GameScreen.CHARACTER_SELECT, GameScreen.GARAGE -> {
                                    GarageAndCharacterScreen(
                                        initialTab = if (screen == GameScreen.CHARACTER_SELECT) 0 else 1,
                                        profile = profile,
                                        statusMessage = statusBanner,
                                        onSelectRacer = viewModel::selectOrUnlockRacer,
                                        onSelectKart = viewModel::selectOrUnlockKart,
                                        onSelectPaint = viewModel::selectOrUnlockPaint,
                                        onSelectWheel = viewModel::selectOrUnlockWheel,
                                        onSelectExhaust = viewModel::selectOrUnlockExhaust,
                                        onSelectDecoration = viewModel::selectOrUnlockDecoration,
                                        onUpgradeStat = viewModel::upgradeKartStat,
                                        onZoomIn = { viewModel.adjustShowroomZoom(-0.8f) },
                                        onZoomOut = { viewModel.adjustShowroomZoom(0.8f) },
                                        onFocusCamera = viewModel::focusShowroomCamera,
                                        onBack = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                                    )
                                }

                                GameScreen.SETTINGS -> {
                                    SettingsScreen(
                                        profile = profile,
                                        userDisplayName = viewModel.userDisplayName,
                                        friendCode = friendCode,
                                        statusMessage = statusBanner,
                                        onUpdateSettings = viewModel::updateSettings,
                                        onSwitchMusicTrack = viewModel::switchBackgroundSong,
                                        onRestoreDefaults = viewModel::restoreDefaultSettings,
                                        onSignOutClick = {
                                            signOutFromGoogle(
                                                credentialManager = credentialManager,
                                                scope = authScope,
                                                onPreSignOutCleanup = { viewModel.onPreSignOutCleanup() }
                                            )
                                        },
                                        onBack = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                                    )
                                }

                                GameScreen.RACE -> {
                                    RaceHudScreen(
                                        mode = activeMode,
                                        hud = raceHud,
                                        settings = profile.settings,
                                        isPaused = isPaused,
                                        notification = raceNotification,
                                        sarcasticTaunt = sarcasticTaunt,
                                        roomMessages = roomMessages,
                                        isRecordingVoice = isRecordingVoice,
                                        isVoiceMuted = isVoiceMuted,
                                        onControlsChanged = viewModel::updateTouchControls,
                                        onJump = viewModel::triggerJump,
                                        onUsePowerUp = viewModel::triggerPowerUp,
                                        onCycleCameraPreset = viewModel::cycleCameraPreset,
                                        onResetCameraAngle = viewModel::resetRaceCameraAngle,
                                        onToggleLiveMic = viewModel::toggleLiveMic,
                                        onToggleVoiceMute = viewModel::toggleVoiceMute,
                                        onSendQuickCallout = viewModel::sendQuickCallout,
                                        onSendChat = viewModel::sendRoomTextMessage,
                                        onSendSarcasticTaunt = viewModel::sendSarcasticTaunt,
                                        onPauseToggle = viewModel::togglePause,
                                        onRestart = {
                                            when (activeMode) {
                                                GameMode.FREE_ROAM -> viewModel.startFreeRoam(raceHud.currentRegion)
                                                GameMode.TIME_TRIAL -> viewModel.startTimeTrial(selectedTrack)
                                                GameMode.PRACTICE -> viewModel.startPractice(selectedTrack)
                                                else -> viewModel.startQuickRace(selectedTrack, selectedDifficulty)
                                            }
                                        },
                                        onFinishSession = viewModel::finishTimeTrialOrPractice,
                                        onQuitToMenu = {
                                            if (raceHud.isOnlineSession) {
                                                viewModel.rematchOnlineRoom()
                                            } else {
                                                viewModel.navigateTo(GameScreen.MAIN_MENU)
                                            }
                                        },
                                        onToggleDebug = viewModel::toggleDebugTelemetry,
                                        onOpenWorldMap = { viewModel.navigateTo(GameScreen.WORLD_MAP) },
                                        onEnterPortalRace = viewModel::enterNearbyPortalRace,
                                        onUpgradeCompany = viewModel::upgradeCompanyInOpenWorld
                                    )
                                }

                                GameScreen.RESULTS -> {
                                    val sum = raceSummary
                                    if (sum != null) {
                                        val hasNextGp = sum.mode == GameMode.GRAND_PRIX &&
                                            !sum.grandPrixCompleted &&
                                            gpSession != null
                                        ResultsScreen(
                                            summary = sum,
                                            profile = profile,
                                            hasNextGrandPrixRace = hasNextGp,
                                            onNextGrandPrixRace = viewModel::advanceGrandPrixNextRace,
                                            onPlayAgain = {
                                                when (sum.mode) {
                                                    GameMode.TIME_TRIAL -> viewModel.startTimeTrial(sum.trackId)
                                                    GameMode.PRACTICE -> viewModel.startPractice(sum.trackId)
                                                    else -> viewModel.startQuickRace(sum.trackId, sum.difficulty)
                                                }
                                            },
                                            onReturnOnlineLobby = viewModel::rematchOnlineRoom,
                                            onBackToMenu = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                                        )
                                    } else {
                                        viewModel.navigateTo(GameScreen.MAIN_MENU)
                                    }
                                }
                            }

                            // Global Incoming Friend Play Alert & Live Squad Invite Overlay Banner
                            AnimatedVisibility(
                                visible = pendingInvite != null,
                                enter = fadeIn() + slideInVertically { -it },
                                exit = fadeOut() + slideOutVertically { -it },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .windowInsetsPadding(WindowInsets.safeDrawing)
                                    .padding(top = 10.dp)
                            ) {
                                pendingInvite?.let { invite ->
                                    Row(
                                        modifier = Modifier
                                            .testTag("incoming_game_invite_banner")
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(BerryPurple.copy(alpha = 0.96f))
                                            .border(2.dp, CaramelGold, RoundedCornerShape(20.dp))
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Filled.Mail, contentDescription = null, tint = CaramelGold, modifier = Modifier.size(22.dp))
                                        Column {
                                            Text(
                                                text = "صديقك ${invite.fromName} يدعوك للعب!",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = CaramelGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val modeLabel = if (invite.isOfflineNudge()) {
                                                "انضم إلى لوبي السيارات 3D"
                                            } else if (invite.isGatheringHub()) {
                                                "لوبي التجمع • #${invite.roomCode}"
                                            } else {
                                                "${invite.roomName} • #${invite.roomCode}"
                                            }
                                            Text(
                                                text = modeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SkyCandy
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.respondToGameInvite(invite, true) },
                                            modifier = Modifier
                                                .testTag("accept_game_invite_button")
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(MintCream)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = "قبول الدعوة والانضمام", tint = BerryDeep)
                                        }
                                        IconButton(
                                            onClick = { viewModel.respondToGameInvite(invite, false) },
                                            modifier = Modifier
                                                .testTag("decline_game_invite_button")
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(StrawberryRed)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "رفض الدعوة", tint = FrostingWhite)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activeViewModel?.setPresenceOnline(true)
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        activeViewModel?.setPresenceOnline(false)
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val rawTilt = (event.values[1] / 5.5f).coerceIn(-1f, 1f)
            activeViewModel?.updateTiltSteer(rawTilt)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
