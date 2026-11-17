package com.example

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sweetrush.SweetRushViewModel
import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.GameScreen
import com.example.sweetrush.render3d.CandyViewport3D
import com.example.sweetrush.ui.ChallengesScreen
import com.example.sweetrush.ui.GarageAndCharacterScreen
import com.example.sweetrush.ui.MainMenuScreen
import com.example.sweetrush.ui.RaceHudScreen
import com.example.sweetrush.ui.RaceSetupScreen
import com.example.sweetrush.ui.ResultsScreen
import com.example.sweetrush.ui.SettingsScreen
import com.example.ui.theme.SweetRushTheme

class MainActivity : ComponentActivity(), SensorEventListener {

    private val viewModel: SweetRushViewModel by viewModels()
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

                val allowShowroomOrbit = screen == GameScreen.GARAGE ||
                    screen == GameScreen.CHARACTER_SELECT ||
                    screen == GameScreen.RESULTS

                Box(modifier = Modifier.fillMaxSize()) {
                    // Persistent hardware-accelerated 3D OpenGL ES 3.0 Candy World Viewport
                    CandyViewport3D(
                        renderer = viewModel.renderer,
                        allowOrbitDrag = allowShowroomOrbit,
                        modifier = Modifier.fillMaxSize()
                    )

                    when (screen) {
                        GameScreen.MAIN_MENU -> {
                            MainMenuScreen(
                                profile = profile,
                                onNavigate = { target -> viewModel.navigateTo(target) }
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
                                onBack = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                            )
                        }

                        GameScreen.SETTINGS -> {
                            SettingsScreen(
                                profile = profile,
                                statusMessage = statusBanner,
                                onUpdateSettings = viewModel::updateSettings,
                                onRestoreDefaults = viewModel::restoreDefaultSettings,
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
                                onControlsChanged = viewModel::updateTouchControls,
                                onJump = viewModel::triggerJump,
                                onUsePowerUp = viewModel::triggerPowerUp,
                                onPauseToggle = viewModel::togglePause,
                                onRestart = {
                                    when (activeMode) {
                                        GameMode.TIME_TRIAL -> viewModel.startTimeTrial(selectedTrack)
                                        GameMode.PRACTICE -> viewModel.startPractice(selectedTrack)
                                        else -> viewModel.startQuickRace(selectedTrack, selectedDifficulty)
                                    }
                                },
                                onFinishSession = viewModel::finishTimeTrialOrPractice,
                                onQuitToMenu = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
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
                                    onBackToMenu = { viewModel.navigateTo(GameScreen.MAIN_MENU) }
                                )
                            } else {
                                viewModel.navigateTo(GameScreen.MAIN_MENU)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // In landscape mode, tilting left/right maps to sensor Y-axis
            val rawTilt = (event.values[1] / 5.5f).coerceIn(-1f, 1f)
            viewModel.updateTiltSteer(rawTilt)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
