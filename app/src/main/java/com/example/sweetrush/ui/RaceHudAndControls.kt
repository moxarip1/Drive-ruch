package com.example.sweetrush.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sweetrush.ToastNotification
import com.example.sweetrush.model.ControlScheme
import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.GameSettings
import com.example.sweetrush.model.VehicleDebugTelemetry
import com.example.sweetrush.physics.KartPhysicsEngine
import com.example.sweetrush.render3d.RaceHudSnapshot
import com.example.ui.theme.BerryDeep
import com.example.ui.theme.BerryPurple
import com.example.ui.theme.Butterscotch
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyPinkDark
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.FrostingWhite
import com.example.ui.theme.GrapeJelly
import com.example.ui.theme.MintCream
import com.example.ui.theme.MintDark
import com.example.ui.theme.SkyCandy
import com.example.ui.theme.StrawberryRed
import kotlin.math.atan2

@Composable
fun RaceHudScreen(
    mode: GameMode,
    hud: RaceHudSnapshot,
    settings: GameSettings,
    isPaused: Boolean,
    notification: ToastNotification?,
    onControlsChanged: (steer: Float, throttle: Boolean, brake: Boolean, drift: Boolean, nitro: Boolean) -> Unit,
    onJump: () -> Unit,
    onUsePowerUp: () -> Unit,
    onPauseToggle: (Boolean) -> Unit,
    onRestart: () -> Unit,
    onFinishSession: () -> Unit,
    onQuitToMenu: () -> Unit,
    onToggleDebug: () -> Unit = {},
    onOpenWorldMap: () -> Unit = {},
    onEnterPortalRace: () -> Unit = {}
) {
    BackHandler { onPauseToggle(!isPaused) }

    var steerLeft by remember { mutableStateOf(false) }
    var steerRight by remember { mutableStateOf(false) }
    var wheelSteer by remember { mutableFloatStateOf(0f) }
    var throttleHeld by remember { mutableStateOf(false) }
    var brakeHeld by remember { mutableStateOf(false) }
    var driftHeld by remember { mutableStateOf(false) }
    var nitroHeld by remember { mutableStateOf(false) }

    val isFreeRoam = hud.isFreeRoam || mode == GameMode.FREE_ROAM

    fun pushInput() {
        val btnSteer = (if (steerLeft) -1f else 0f) + (if (steerRight) 1f else 0f)
        val steer = if (settings.controlScheme == ControlScheme.VIRTUAL_WHEEL) wheelSteer else btnSteer
        onControlsChanged(steer, throttleHeld, brakeHeld, driftHeld, nitroHeld)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Radial Speed Lines during Nitro / Boost / High Speed
        if (hud.isBoostOrNitroActive || hud.speedKmh >= 175) {
            val intensity = if (hud.isBoostOrNitroActive) 1f else ((hud.speedKmh - 175) / 80f).coerceIn(0.25f, 0.85f)
            val pulseOffset = ((hud.elapsedMs % 400L) / 400f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width * 0.5f
                val cy = size.height * 0.5f
                val maxR = kotlin.math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
                val streakCount = 24
                for (i in 0 until streakCount) {
                    val angle = (i.toFloat() / streakCount) * 6.283185f + pulseOffset * 0.14f
                    val cosA = kotlin.math.cos(angle)
                    val sinA = kotlin.math.sin(angle)
                    val innerFrac = 0.62f + ((i * 37) % 15) * 0.01f
                    val outerFrac = 0.98f
                    val pStart = Offset(cx + cosA * maxR * innerFrac, cy + sinA * maxR * innerFrac)
                    val pEnd = Offset(cx + cosA * maxR * outerFrac, cy + sinA * maxR * outerFrac)
                    val lineColor = when {
                        hud.isBoostOrNitroActive && i % 3 == 0 -> CaramelGold
                        hud.isBoostOrNitroActive -> MintCream
                        else -> Color.White
                    }
                    drawLine(
                        color = lineColor.copy(alpha = 0.34f * intensity),
                        start = pStart,
                        end = pEnd,
                        strokeWidth = if (i % 2 == 0) 4.5f else 2.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Chocolate Smoke splatter overlay if hit by opponent's Chocolate Smoke!
        if (hud.smokeAlpha > 0.02f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Color(0xFF3A1C12).copy(alpha = hud.smokeAlpha * 0.72f))
                drawCircle(
                    color = Color(0xFF5D3422).copy(alpha = hud.smokeAlpha * 0.85f),
                    radius = size.height * 0.35f,
                    center = Offset(size.width * 0.3f, size.height * 0.45f)
                )
                drawCircle(
                    color = Color(0xFF4E2A1E).copy(alpha = hud.smokeAlpha * 0.85f),
                    radius = size.height * 0.38f,
                    center = Offset(size.width * 0.68f, size.height * 0.55f)
                )
            }
        }

        // Main HUD + Touch Controls inside Safe Insets
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // TOP HUD ROW: Position/Region + Lap/Collectibles (Left) | Toast (Center) | Mini-Map + Debug/Map/Pause (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left column: Position/Lap OR Free Roam Region + Collectibles, plus optional Debug Telemetry overlay
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BerryDeep.copy(alpha = 0.62f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isFreeRoam) {
                            Icon(
                                imageVector = Icons.Filled.Explore,
                                contentDescription = "Free Roam Region",
                                tint = Color(hud.currentRegion.accentHex),
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = hud.currentRegion.displayName.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(hud.currentRegion.accentHex)
                                )
                                Text(
                                    text = "Coins ${hud.worldCoinsCollected}/48  •  Stars ${hud.worldStarsCollected}/10",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CaramelGold
                                )
                                Text(
                                    text = "OPEN WORLD  •  ${KartPhysicsEngine.formatTime(hud.elapsedMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MintCream
                                )
                            }
                        } else {
                            if (mode != GameMode.TIME_TRIAL && mode != GameMode.PRACTICE) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = KartPhysicsEngine.ordinal(hud.position),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = if (hud.position == 1) CaramelGold else FrostingWhite
                                    )
                                    Text("OF 8", style = MaterialTheme.typography.labelSmall, color = MintCream)
                                }
                            }
                            Column {
                                Text(
                                    text = if (mode == GameMode.TIME_TRIAL || mode == GameMode.PRACTICE) {
                                        "LAP ${hud.currentLap}"
                                    } else {
                                        "LAP ${hud.currentLap}/${hud.totalLaps}"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = CandyPink
                                )
                                Text(
                                    text = KartPhysicsEngine.formatTime(hud.elapsedMs),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FrostingWhite
                                )
                                if (hud.bestLapMs < Long.MAX_VALUE) {
                                    Text(
                                        text = "BEST ${KartPhysicsEngine.formatTime(hud.bestLapMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CaramelGold
                                    )
                                }
                            }
                        }
                    }

                    if (settings.showDebugTelemetry) {
                        VehicleDebugTelemetryOverlay(telemetry = hud.telemetry)
                    }
                }

                // Center Power-Up / Overtake Notification + Active Power-Up Slot
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Held Power-Up display
                        Box(
                            modifier = Modifier
                                .testTag("hud_powerup_slot")
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (hud.heldPowerUp != null) Color(hud.heldPowerUp.colorArgb)
                                    else BerryDeep.copy(alpha = 0.7f)
                                )
                                .border(2.5.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                                .clickable(enabled = hud.heldPowerUp != null) { onUsePowerUp() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (hud.heldPowerUp != null) {
                                Icon(Icons.Filled.Star, contentDescription = hud.heldPowerUp.displayName, tint = FrostingWhite, modifier = Modifier.size(30.dp))
                            } else {
                                Text("?", style = MaterialTheme.typography.headlineMedium, color = FrostingWhite.copy(alpha = 0.4f))
                            }
                        }

                        // Speedometer & Nitro Gauge
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(BerryDeep.copy(alpha = 0.62f))
                                .border(1.5.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${hud.speedKmh}", style = MaterialTheme.typography.headlineMedium, color = FrostingWhite)
                                Spacer(Modifier.width(4.dp))
                                Text("KM/H", style = MaterialTheme.typography.labelSmall, color = SkyCandy)
                            }
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(105.dp)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(hud.nitroRatio.coerceIn(0f, 1f))
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Brush.horizontalGradient(listOf(MintCream, CaramelGold)))
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = notification != null,
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut() + slideOutVertically { -it / 2 }
                    ) {
                        notification?.let { note ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(note.accentHex))
                                    .border(2.dp, Color.White, RoundedCornerShape(50))
                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (note.subtitle.isNotEmpty()) "${note.title} • ${note.subtitle}" else note.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = FrostingWhite
                                )
                            }
                        }
                    }
                }

                // Right: Live Mini-Map + Pause / Map / Debug / End Session buttons
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrackMiniMap(hud = hud, modifier = Modifier.size(92.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onToggleDebug,
                                modifier = Modifier
                                    .testTag("toggle_debug_button")
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (settings.showDebugTelemetry) MintDark.copy(alpha = 0.92f)
                                        else BerryDeep.copy(alpha = 0.78f)
                                    )
                                    .border(
                                        1.5.dp,
                                        if (settings.showDebugTelemetry) MintCream else Color.White.copy(alpha = 0.45f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BugReport,
                                    contentDescription = "Toggle Vehicle Debug Telemetry",
                                    tint = FrostingWhite
                                )
                            }

                            IconButton(
                                onClick = { onPauseToggle(true) },
                                modifier = Modifier
                                    .testTag("pause_button")
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(BerryPurple.copy(alpha = 0.88f))
                                    .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = FrostingWhite)
                            }
                        }

                        if (isFreeRoam) {
                            CandyButton(
                                text = "MAP",
                                onClick = onOpenWorldMap,
                                testTag = "hud_world_map_button",
                                icon = Icons.Filled.Public,
                                topColor = SkyCandy,
                                bottomColor = Color(0xFF0288D1),
                                textColor = BerryDeep,
                                height = 48.dp,
                                modifier = Modifier.width(102.dp)
                            )
                        } else if (mode == GameMode.TIME_TRIAL || mode == GameMode.PRACTICE) {
                            CandyButton(
                                text = "END",
                                onClick = onFinishSession,
                                testTag = "finish_session_button",
                                topColor = CaramelGold,
                                bottomColor = Butterscotch,
                                textColor = BerryDeep,
                                height = 48.dp,
                                modifier = Modifier.width(102.dp)
                            )
                        }
                    }
                }
            }

            // CENTER COUNTDOWN BANNER
            if (hud.countdownText.isNotEmpty()) {
                Text(
                    text = hud.countdownText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        shadow = Shadow(BerryDeep, Offset(0f, 6f), 12f)
                    ),
                    color = if (hud.countdownText == "SWEET RUSH!") MintCream else CaramelGold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // NEARBY RACE PORTAL GATE CTA IN FREE ROAM
            val portalTitle = hud.nearbyPortalTitle
            if (isFreeRoam && portalTitle != null) {
                CandyButton(
                    text = "ENTER RACE: ${portalTitle.uppercase()}",
                    onClick = onEnterPortalRace,
                    testTag = "enter_portal_button",
                    icon = Icons.Filled.Flag,
                    topColor = CaramelGold,
                    bottomColor = Butterscotch,
                    textColor = BerryDeep,
                    height = 50.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp)
                        .width(300.dp)
                )
            }

            // BOTTOM-LEFT CONTROLS: Steering Left/Right OR Virtual Wheel
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 6.dp)
            ) {
                when {
                    settings.tiltSteeringEnabled || settings.controlScheme == ControlScheme.TILT_STEER -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(BerryDeep.copy(alpha = 0.65f))
                                .border(1.5.dp, MintCream, RoundedCornerShape(18.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text("TILT STEERING ACTIVE", style = MaterialTheme.typography.labelLarge, color = MintCream)
                        }
                    }
                    settings.controlScheme == ControlScheme.VIRTUAL_WHEEL -> {
                        VirtualSteeringWheel(
                            onSteer = { v ->
                                wheelSteer = v
                                pushInput()
                            }
                        )
                    }
                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            HoldTouchButton(
                                label = "",
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                baseColor = BerryPurple,
                                activeColor = CandyPink,
                                size = 76.dp,
                                testTag = "steer_left_button",
                                onHoldChange = { held ->
                                    steerLeft = held
                                    pushInput()
                                }
                            )
                            HoldTouchButton(
                                label = "",
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                baseColor = BerryPurple,
                                activeColor = CandyPink,
                                size = 76.dp,
                                testTag = "steer_right_button",
                                onHoldChange = { held ->
                                    steerRight = held
                                    pushInput()
                                }
                            )
                        }
                    }
                }
            }

            // BOTTOM-RIGHT CONTROLS: Gas, Brake, Drift, Nitro, Jump, PowerUp
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top action row: Power-Up + Jump + Nitro
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hud.heldPowerUp != null) {
                        CandyButton(
                            text = "USE",
                            onClick = onUsePowerUp,
                            testTag = "use_powerup_button",
                            icon = Icons.Filled.Star,
                            topColor = Color(hud.heldPowerUp.colorArgb),
                            bottomColor = BerryPurple,
                            height = 52.dp,
                            modifier = Modifier.width(92.dp)
                        )
                    }
                    CandyButton(
                        text = "JUMP",
                        onClick = onJump,
                        testTag = "jump_button",
                        icon = Icons.Filled.ArrowUpward,
                        topColor = SkyCandy,
                        bottomColor = Color(0xFF0288D1),
                        textColor = BerryDeep,
                        height = 52.dp,
                        modifier = Modifier.width(96.dp)
                    )
                    HoldTouchButton(
                        label = "NITRO",
                        icon = Icons.Filled.Bolt,
                        baseColor = Butterscotch,
                        activeColor = CaramelGold,
                        size = 64.dp,
                        testTag = "nitro_button",
                        onHoldChange = { held ->
                            nitroHeld = held
                            pushInput()
                        }
                    )
                }

                // Primary pedal row: Brake + Drift + Gas
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    HoldTouchButton(
                        label = "BRAKE",
                        icon = null,
                        baseColor = StrawberryRed.copy(alpha = 0.85f),
                        activeColor = StrawberryRed,
                        size = 62.dp,
                        testTag = "brake_button",
                        onHoldChange = { held ->
                            brakeHeld = held
                            pushInput()
                        }
                    )
                    HoldTouchButton(
                        label = if (hud.isDrifting) "CHARGING" else "DRIFT",
                        icon = null,
                        baseColor = GrapeJelly,
                        activeColor = when {
                            hud.driftCharge >= 2.2f -> CaramelGold
                            hud.driftCharge >= 1.35f -> CandyPink
                            else -> MintCream
                        },
                        size = 70.dp,
                        testTag = "drift_button",
                        onHoldChange = { held ->
                            driftHeld = held
                            pushInput()
                        }
                    )
                    HoldTouchButton(
                        label = "GAS",
                        icon = null,
                        baseColor = MintDark,
                        activeColor = MintCream,
                        size = 84.dp,
                        testTag = "gas_button",
                        onHoldChange = { held ->
                            throttleHeld = held
                            pushInput()
                        }
                    )
                }
            }
        }

        // PAUSE MODAL
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BerryDeep.copy(alpha = 0.82f))
                    .clickable(enabled = true) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(BerryPurple)
                        .border(2.5.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("RACE PAUSED", style = MaterialTheme.typography.displayMedium, color = FrostingWhite)
                    CandyButton(
                        text = "RESUME RACE",
                        onClick = { onPauseToggle(false) },
                        testTag = "resume_button",
                        icon = Icons.Filled.PlayArrow,
                        topColor = MintCream,
                        bottomColor = MintDark,
                        textColor = BerryDeep,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CandyButton(
                        text = "RESTART TRACK",
                        onClick = onRestart,
                        testTag = "restart_button",
                        icon = Icons.Filled.Refresh,
                        topColor = CaramelGold,
                        bottomColor = Butterscotch,
                        textColor = BerryDeep,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CandyButton(
                        text = "QUIT TO MENU",
                        onClick = onQuitToMenu,
                        testTag = "quit_button",
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        topColor = CandyPink,
                        bottomColor = CandyPinkDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun HoldTouchButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    baseColor: Color,
    activeColor: Color,
    size: Dp,
    testTag: String,
    onHoldChange: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .testTag(testTag)
            .size(size)
            .clip(CircleShape)
            .background(
                if (pressed) activeColor.copy(alpha = 0.92f) else baseColor.copy(alpha = 0.64f)
            )
            .border(
                width = if (pressed) 3.dp else 2.dp,
                color = Color.White.copy(alpha = if (pressed) 1f else 0.68f),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onHoldChange(true)
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.none { it.pressed }) break
                        }
                    } finally {
                        pressed = false
                        onHoldChange(false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(icon, contentDescription = label.ifEmpty { testTag }, tint = FrostingWhite, modifier = Modifier.size(32.dp))
            }
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = FrostingWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VirtualSteeringWheel(onSteer: (Float) -> Unit) {
    var angleDeg by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .testTag("virtual_steering_wheel")
            .size(128.dp)
            .clip(CircleShape)
            .background(BerryDeep.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.5f
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val ptr = event.changes.firstOrNull { it.pressed } ?: break
                            val dx = ptr.position.x - cx
                            val dy = ptr.position.y - cy
                            val rawDeg = Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble())).toFloat()
                            val clamped = rawDeg.coerceIn(-75f, 75f)
                            angleDeg = clamped
                            onSteer((clamped / 60f).coerceIn(-1f, 1f))
                        }
                    } finally {
                        angleDeg = 0f
                        onSteer(0f)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(116.dp)
                .rotate(angleDeg)
        ) {
            val r = size.minDimension * 0.44f
            drawCircle(CandyPink, radius = r, style = Stroke(width = 16f))
            drawCircle(Color.White.copy(alpha = 0.85f), radius = r, style = Stroke(width = 4f))
            drawLine(FrostingWhite, Offset(center.x - r, center.y), Offset(center.x + r, center.y), strokeWidth = 10f, cap = StrokeCap.Round)
            drawLine(FrostingWhite, Offset(center.x, center.y), Offset(center.x, center.y + r), strokeWidth = 10f, cap = StrokeCap.Round)
            drawCircle(CaramelGold, radius = 14f)
        }
    }
}

@Composable
private fun TrackMiniMap(hud: RaceHudSnapshot, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BerryDeep.copy(alpha = 0.62f))
            .border(1.5.dp, Color.White.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
            .padding(6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pts = hud.trackOutline
            if (pts.size < 3) return@Canvas
            val spanX = (hud.trackMaxX - hud.trackMinX).coerceAtLeast(1f)
            val spanZ = (hud.trackMaxZ - hud.trackMinZ).coerceAtLeast(1f)

            fun mapPt(wx: Float, wz: Float): Offset {
                val nx = ((wx - hud.trackMinX) / spanX).coerceIn(0f, 1f)
                val nz = 1f - ((wz - hud.trackMinZ) / spanZ).coerceIn(0f, 1f)
                return Offset(nx * size.width, nz * size.height)
            }

            val path = Path()
            val first = mapPt(pts[0].first, pts[0].second)
            path.moveTo(first.x, first.y)
            for (i in 1 until pts.size) {
                val p = mapPt(pts[i].first, pts[i].second)
                path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, Color.White.copy(alpha = 0.45f), style = Stroke(width = 5f))

            for (dot in hud.mapDots) {
                val p = mapPt(dot.x, dot.z)
                drawCircle(
                    color = if (dot.isPlayer) CaramelGold else Color(dot.colorHex),
                    radius = if (dot.isPlayer) 6.5f else 4f,
                    center = p
                )
            }
        }
    }
}

@Composable
private fun VehicleDebugTelemetryOverlay(telemetry: VehicleDebugTelemetry) {
    Column(
        modifier = Modifier
            .testTag("vehicle_debug_overlay")
            .width(236.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BerryDeep.copy(alpha = 0.80f))
            .border(1.5.dp, MintCream.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "TELEMETRY • ${telemetry.surfaceName.uppercase()} • ${telemetry.driftState}",
            style = MaterialTheme.typography.labelSmall,
            color = MintCream,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "SPD ${telemetry.speedKmh.toInt()} km/h  |  SLIP ${String.format("%.1f", telemetry.slipAngleDeg)}°",
            style = MaterialTheme.typography.labelSmall,
            color = FrostingWhite
        )
        Text(
            text = "VEL (${String.format("%.1f", telemetry.vx)}, ${String.format("%.1f", telemetry.vy)}, ${String.format("%.1f", telemetry.vz)})",
            style = MaterialTheme.typography.labelSmall,
            color = SkyCandy
        )
        Text(
            text = "GRIP Lat ${String.format("%.2f", telemetry.lateralGrip)} / Fwd ${String.format("%.2f", telemetry.forwardGrip)}",
            style = MaterialTheme.typography.labelSmall,
            color = CaramelGold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val labels = listOf("FL", "FR", "RL", "RR")
            for (i in 0 until 4) {
                val contact = telemetry.wheelContacts.getOrElse(i) { false }
                val comp = telemetry.wheelCompression.getOrElse(i) { 0f }.coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (contact) MintCream else StrawberryRed)
                        )
                        Text(labels[i], style = MaterialTheme.typography.labelSmall, color = FrostingWhite)
                    }
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(comp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (contact) CandyPink else CaramelGold)
                        )
                    }
                }
            }
        }
    }
}
