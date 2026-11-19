package com.example.sweetrush.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.sweetrush.model.AppLanguage
import com.example.sweetrush.model.CameraPreset
import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.ControlScheme
import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.GameSettings
import com.example.sweetrush.model.GraphicsQuality
import com.example.sweetrush.model.PlayerProfile
import com.example.sweetrush.model.RaceSummary
import com.example.sweetrush.model.SquadTeam
import com.example.sweetrush.physics.KartPhysicsEngine
import com.example.ui.theme.BerryCard
import com.example.ui.theme.BerryDeep
import com.example.ui.theme.BerryPurple
import com.example.ui.theme.Butterscotch
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyPinkDark
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.FrostingWhite
import com.example.ui.theme.MintCream
import com.example.ui.theme.MintDark
import com.example.ui.theme.SkyCandy
import com.example.ui.theme.StrawberryRed

@Composable
fun ResultsScreen(
    summary: RaceSummary,
    profile: PlayerProfile,
    hasNextGrandPrixRace: Boolean,
    onNextGrandPrixRace: () -> Unit,
    onPlayAgain: () -> Unit,
    onReturnOnlineLobby: () -> Unit = {},
    onBackToMenu: () -> Unit
) {
    BackHandler(onBack = onBackToMenu)
    val racer = CatalogData.racerById(profile.selectedRacerId)
    val won = summary.playerPosition == 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BerryDeep.copy(alpha = 0.85f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Trophy / Victory Celebration + Rewards
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BerryPurple.copy(alpha = 0.94f))
                    .border(2.dp, CaramelGold.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (summary.mode == GameMode.TIME_TRIAL) "TIME TRIAL COMPLETE!"
                        else if (summary.mode == GameMode.PRACTICE) "PRACTICE COMPLETE!"
                        else "${KartPhysicsEngine.ordinal(summary.playerPosition)} PLACE!",
                        style = MaterialTheme.typography.displayMedium,
                        color = if (won) CaramelGold else FrostingWhite
                    )
                    Spacer(Modifier.height(6.dp))
                    Image(
                        painter = painterResource(id = R.drawable.img_candy_trophy),
                        contentDescription = "Candy Cup Trophy",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(2.dp, CaramelGold, RoundedCornerShape(20.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "\"${if (won) racer.victoryLine else racer.defeatLine}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MintCream
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Rewards: +${summary.creditsEarned} CC  •  +${summary.xpEarned} XP",
                        style = MaterialTheme.typography.titleMedium,
                        color = CaramelGold
                    )
                    if (summary.isNewBestLap) {
                        Text(
                            text = "NEW TRACK RECORD: ${KartPhysicsEngine.formatTime(summary.playerBestLapMs)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MintCream
                        )
                    }
                    if (summary.challengeCompletedId != null) {
                        Text(
                            text = "CHALLENGE COMPLETED!",
                            style = MaterialTheme.typography.labelLarge,
                            color = SkyCandy
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasNextGrandPrixRace) {
                        CandyButton(
                            text = "NEXT GP RACE",
                            onClick = onNextGrandPrixRace,
                            testTag = "next_gp_button",
                            icon = Icons.Filled.PlayArrow,
                            topColor = MintCream,
                            bottomColor = MintDark,
                            textColor = BerryDeep,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (summary.isOnlineMatch) {
                        CandyButton(
                            text = "SQUAD REMATCH",
                            onClick = onReturnOnlineLobby,
                            testTag = "online_rematch_button",
                            icon = Icons.Filled.Refresh,
                            topColor = MintCream,
                            bottomColor = MintDark,
                            textColor = BerryDeep,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        CandyButton(
                            text = "RACE AGAIN",
                            onClick = onPlayAgain,
                            testTag = "play_again_button",
                            icon = Icons.Filled.Refresh,
                            topColor = MintCream,
                            bottomColor = MintDark,
                            textColor = BerryDeep,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CandyButton(
                        text = "MAIN MENU",
                        onClick = onBackToMenu,
                        testTag = "results_menu_button",
                        icon = Icons.Filled.Home,
                        topColor = CandyPink,
                        bottomColor = CandyPinkDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Right: Race Standings or Squad Standings
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BerryPurple.copy(alpha = 0.92f))
                    .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = if (summary.isOnlineMatch && summary.teamSize > 1) "SQUAD & RACER STANDINGS"
                    else if (summary.mode == GameMode.GRAND_PRIX) "RACE & CHAMPIONSHIP STANDINGS"
                    else "RACE STANDINGS",
                    style = MaterialTheme.typography.titleLarge,
                    color = FrostingWhite
                )

                if (summary.teamStandings.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        summary.teamStandings.forEachIndexed { rank, (teamIdx, pts) ->
                            val team = SquadTeam.fromIndex(teamIdx)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(team.colorHex).copy(alpha = 0.25f))
                                    .border(1.5.dp, Color(team.colorHex), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("#${rank + 1} ${team.teamName}", style = MaterialTheme.typography.labelSmall, color = Color(team.colorHex))
                                Text("$pts PTS", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(summary.standings) { entry ->
                        val team = SquadTeam.fromIndex(entry.teamIndex)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (entry.isPlayer) CandyPink.copy(alpha = 0.38f) else BerryDeep.copy(alpha = 0.65f))
                                .border(
                                    1.5.dp,
                                    if (entry.isPlayer) CaramelGold else if (summary.teamSize > 1) Color(team.colorHex).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.18f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${entry.position}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (entry.position <= 3) CaramelGold else FrostingWhite,
                                    modifier = Modifier.width(34.dp)
                                )
                                Column {
                                    Text(
                                        text = if (entry.isPlayer) "${entry.racerName} (YOU)" else entry.racerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = FrostingWhite
                                    )
                                    Text(
                                        text = if (summary.teamSize > 1) "${team.teamName} • ${entry.kartName}" else entry.kartName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (summary.teamSize > 1) Color(team.colorHex) else SkyCandy
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(KartPhysicsEngine.formatTime(entry.totalTimeMs), style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                                if (summary.mode == GameMode.GRAND_PRIX || summary.teamSize > 1) {
                                    Text("+${entry.grandPrixPointsAwarded} PTS", style = MaterialTheme.typography.labelSmall, color = MintCream)
                                }
                            }
                        }
                    }
                }

                if (summary.grandPrixTotalStandings.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "GP Leader: ${summary.grandPrixTotalStandings.first().first} (${summary.grandPrixTotalStandings.first().second} pts)",
                        style = MaterialTheme.typography.labelLarge,
                        color = CaramelGold
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    profile: PlayerProfile,
    userDisplayName: String = "Candy Racer",
    friendCode: String = "",
    statusMessage: String?,
    isMusicEnabled: Boolean = true,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
    onRestoreDefaults: () -> Unit,
    onToggleMusic: () -> Unit = {},
    onSwitchMusicTrack: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val s = profile.settings

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BerryDeep.copy(alpha = 0.86f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CandyScreenHeader(title = "Settings", profile = profile, onBack = onBack)

            if (statusMessage != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MintCream)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(statusMessage, style = MaterialTheme.typography.labelLarge, color = BerryDeep)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left column: Graphics, FPS & Audio
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(BerryPurple.copy(alpha = 0.92f))
                        .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("GRAPHICS & PERFORMANCE", style = MaterialTheme.typography.titleLarge, color = MintCream)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GraphicsQuality.entries.forEach { q ->
                            val sel = s.graphicsQuality == q
                            SettingChip(
                                text = q.label,
                                selected = sel,
                                onClick = { onUpdateSettings { it.copy(graphicsQuality = q) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("TARGET FPS", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 60).forEach { fps ->
                            SettingChip(
                                text = "$fps FPS",
                                selected = s.targetFps == fps,
                                onClick = { onUpdateSettings { it.copy(targetFps = fps) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("MUSIC VOLUME (${(s.musicVolume * 100).toInt()}%)", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SettingChip(
                                text = if (isMusicEnabled) "🔊 ON" else "🔇 OFF",
                                selected = isMusicEnabled,
                                onClick = onToggleMusic,
                                modifier = Modifier.testTag("settings_toggle_music_button")
                            )
                            SettingChip(
                                text = "🎵 Switch Track",
                                selected = true,
                                onClick = onSwitchMusicTrack,
                                modifier = Modifier.testTag("switch_music_track_button")
                            )
                        }
                    }
                    Slider(
                        value = s.musicVolume,
                        onValueChange = { v -> onUpdateSettings { it.copy(musicVolume = v) } },
                        colors = SliderDefaults.colors(thumbColor = CandyPink, activeTrackColor = CandyPink)
                    )

                    Text("SFX VOLUME (${(s.sfxVolume * 100).toInt()}%)", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                    Slider(
                        value = s.sfxVolume,
                        onValueChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } },
                        colors = SliderDefaults.colors(thumbColor = CaramelGold, activeTrackColor = CaramelGold)
                    )

                    Text("CAMERA ANGLE PRESET (زاوية الكاميرا)", style = MaterialTheme.typography.titleLarge, color = CaramelGold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CameraPreset.entries.forEach { preset ->
                            SettingChip(
                                text = preset.label,
                                selected = s.cameraPreset == preset,
                                onClick = { onUpdateSettings { it.copy(cameraPreset = preset) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("CAMERA DISTANCE (${String.format("%.2f", s.cameraDistanceScale)}x)", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                    Slider(
                        value = s.cameraDistanceScale,
                        valueRange = 0.65f..1.45f,
                        onValueChange = { v -> onUpdateSettings { it.copy(cameraDistanceScale = v) } },
                        colors = SliderDefaults.colors(thumbColor = SkyCandy, activeTrackColor = SkyCandy)
                    )

                    Text("CAMERA HEIGHT (${String.format("%+.2f", s.cameraHeightOffset)}m)  •  FOV (${String.format("%+.0f", s.cameraFovOffset)}°)", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                    Slider(
                        value = s.cameraHeightOffset,
                        valueRange = -1.0f..2.0f,
                        onValueChange = { v -> onUpdateSettings { it.copy(cameraHeightOffset = v) } },
                        colors = SliderDefaults.colors(thumbColor = MintCream, activeTrackColor = MintCream)
                    )
                    Slider(
                        value = s.cameraFovOffset,
                        valueRange = -8f..14f,
                        onValueChange = { v -> onUpdateSettings { it.copy(cameraFovOffset = v) } },
                        colors = SliderDefaults.colors(thumbColor = CandyPink, activeTrackColor = CandyPink)
                    )
                }

                // Right column: Controls, Sensitivity, Language & Restore Defaults
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(BerryPurple.copy(alpha = 0.92f))
                        .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("CONTROLS & LANGUAGE", style = MaterialTheme.typography.titleLarge, color = SkyCandy)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ControlScheme.entries.forEach { cs ->
                            SettingChip(
                                text = cs.label,
                                selected = s.controlScheme == cs,
                                onClick = {
                                    onUpdateSettings {
                                        it.copy(
                                            controlScheme = cs,
                                            tiltSteeringEnabled = (cs == ControlScheme.TILT_STEER)
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tilt Steering Assist", style = MaterialTheme.typography.titleMedium, color = FrostingWhite)
                        Switch(
                            checked = s.tiltSteeringEnabled,
                            onCheckedChange = { checked ->
                                onUpdateSettings { it.copy(tiltSteeringEnabled = checked) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = MintCream, checkedTrackColor = MintDark)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Vehicle Debug Telemetry", style = MaterialTheme.typography.titleMedium, color = FrostingWhite)
                        Switch(
                            checked = s.showDebugTelemetry,
                            onCheckedChange = { checked ->
                                onUpdateSettings { it.copy(showDebugTelemetry = checked) }
                            },
                            modifier = Modifier.testTag("settings_debug_telemetry_switch"),
                            colors = SwitchDefaults.colors(checkedThumbColor = MintCream, checkedTrackColor = MintDark)
                        )
                    }

                    Text("STEERING SENSITIVITY (${String.format("%.1f", s.steeringSensitivity)}x)", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                    Slider(
                        value = s.steeringSensitivity,
                        valueRange = 0.5f..1.7f,
                        onValueChange = { v -> onUpdateSettings { it.copy(steeringSensitivity = v) } },
                        colors = SliderDefaults.colors(thumbColor = MintCream, activeTrackColor = MintCream)
                    )

                    Text("CAMERA SENSITIVITY (${String.format("%.1f", s.cameraSensitivity)}x)", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                    Slider(
                        value = s.cameraSensitivity,
                        valueRange = 0.5f..1.6f,
                        onValueChange = { v -> onUpdateSettings { it.copy(cameraSensitivity = v) } },
                        colors = SliderDefaults.colors(thumbColor = SkyCandy, activeTrackColor = SkyCandy)
                    )

                    Text("LANGUAGE", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            SettingChip(
                                text = lang.displayName,
                                selected = s.language == lang,
                                onClick = { onUpdateSettings { it.copy(language = lang) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    CandyButton(
                        text = "RESTORE DEFAULTS",
                        onClick = onRestoreDefaults,
                        testTag = "restore_defaults_button",
                        topColor = Butterscotch,
                        bottomColor = CandyPinkDark,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("GOOGLE ACCOUNT: $userDisplayName ($friendCode)", style = MaterialTheme.typography.labelSmall, color = SkyCandy)
                    CandyButton(
                        text = "SIGN OUT GOOGLE",
                        onClick = onSignOutClick,
                        testTag = "settings_sign_out_button",
                        topColor = StrawberryRed,
                        bottomColor = CandyPinkDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) CaramelGold else BerryCard)
            .border(1.5.dp, if (selected) Color.White else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) BerryDeep else FrostingWhite
        )
    }
}
