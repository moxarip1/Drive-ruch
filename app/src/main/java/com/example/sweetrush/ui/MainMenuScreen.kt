package com.example.sweetrush.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.GameScreen
import com.example.sweetrush.model.PlayerProfile
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
import kotlin.math.sin

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Groups
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun MainMenuScreen(
    profile: PlayerProfile,
    userDisplayName: String = "Candy Racer",
    userPhotoUrl: String? = null,
    friendCode: String = "",
    activeRoomCode: String? = null,
    onNavigate: (GameScreen) -> Unit,
    onStartFreeRoam: () -> Unit = {}
) {
    val lang = profile.settings.language
    val racer = CatalogData.racerById(profile.selectedRacerId)
    val kart = CatalogData.kartById(profile.selectedKartId)

    val inf = rememberInfiniteTransition(label = "sparkles")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "sparklePhase"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Light edge-only vertical scrims so the central 3D Candy World & cruising kart remain 100% crisp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to BerryDeep.copy(alpha = 0.42f),
                        0.22f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1.0f to BerryDeep.copy(alpha = 0.56f)
                    )
                )
        )

        // Subtle drifting sugar sparkles near the outer frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(CandyPink, MintCream, CaramelGold, SkyCandy, Color.White)
            for (i in 0 until 14) {
                val xFrac = ((i * 0.173f) % 1f)
                val yFrac = ((i * 0.291f + phase * 0.035f) % 1f)
                val radius = 3f + (i % 3) * 1.6f + sin(phase + i) * 1.2f
                drawCircle(
                    color = colors[i % colors.size].copy(alpha = 0.42f),
                    radius = radius.coerceAtLeast(1.8f),
                    center = Offset(size.width * xFrac, size.height * yFrac)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Compact Top Bar: Brand Logo (Left) + Google Account & Player Progression Pill (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                SweetRushLogo(compact = true)

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Google Account Racer Chip
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(BerryPurple.copy(alpha = 0.84f))
                                .border(1.5.dp, MintCream.copy(alpha = 0.7f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!userPhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = userPhotoUrl,
                                    contentDescription = "Google Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(24.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(24.dp)
                                        .clip(CircleShape)
                                        .background(CandyPink),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userDisplayName.firstOrNull()?.uppercase() ?: "R",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FrostingWhite
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = userDisplayName.take(18),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FrostingWhite
                                )
                                if (friendCode.isNotBlank()) {
                                    Text(
                                        text = if (activeRoomCode != null) "ROOM #$activeRoomCode" else "ID: $friendCode",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CaramelGold
                                    )
                                }
                            }
                        }

                        PlayerProfilePill(profile = profile)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(BerryDeep.copy(alpha = 0.66f))
                            .border(1.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${racer.name} • ${kart.name}  |  Wins ${profile.racesWon} • ${profile.collectedWorldCoins.size}/48 Coins • ${profile.collectedWorldStars.size}/10 Stars",
                            style = MaterialTheme.typography.labelSmall,
                            color = FrostingWhite
                        )
                    }
                }
            }

            // Sleek Bottom Floating Docks — keeps ~75% center screen open for the 3D Candy World
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Bottom-Left Racing Dock
                Column(
                    modifier = Modifier
                        .width(362.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(BerryDeep.copy(alpha = 0.56f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CandyButton(
                        text = if (activeRoomCode != null) "${CatalogData.localized(lang, "online")} • #$activeRoomCode"
                        else CatalogData.localized(lang, "online"),
                        onClick = { onNavigate(GameScreen.ONLINE_LOBBY) },
                        testTag = "menu_online_button",
                        icon = Icons.Filled.Groups,
                        topColor = CaramelGold,
                        bottomColor = Color(0xFFFF6D00),
                        textColor = BerryDeep,
                        height = 50.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CandyButton(
                            text = CatalogData.localized(lang, "play"),
                            onClick = { onNavigate(GameScreen.QUICK_RACE_SETUP) },
                            testTag = "menu_play_button",
                            icon = Icons.Filled.PlayArrow,
                            topColor = Color(0xFFFF4D94),
                            bottomColor = CandyPinkDark,
                            height = 50.dp,
                            modifier = Modifier.weight(1f)
                        )
                        CandyButton(
                            text = CatalogData.localized(lang, "free_roam"),
                            onClick = onStartFreeRoam,
                            testTag = "menu_free_roam_button",
                            icon = Icons.Filled.Explore,
                            topColor = MintCream,
                            bottomColor = MintDark,
                            textColor = BerryDeep,
                            height = 50.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CandyButton(
                            text = CatalogData.localized(lang, "grand_prix"),
                            onClick = { onNavigate(GameScreen.GRAND_PRIX_SETUP) },
                            testTag = "menu_grand_prix_button",
                            icon = Icons.Filled.EmojiEvents,
                            topColor = CaramelGold,
                            bottomColor = Butterscotch,
                            textColor = BerryDeep,
                            height = 48.dp,
                            modifier = Modifier.weight(1f)
                        )
                        CandyButton(
                            text = CatalogData.localized(lang, "time_trial"),
                            onClick = { onNavigate(GameScreen.TIME_TRIAL_SETUP) },
                            testTag = "menu_time_trial_button",
                            icon = Icons.Filled.Timer,
                            topColor = SkyCandy,
                            bottomColor = Color(0xFF0288D1),
                            textColor = BerryDeep,
                            height = 48.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CandyButton(
                            text = CatalogData.localized(lang, "practice"),
                            onClick = { onNavigate(GameScreen.PRACTICE_SETUP) },
                            testTag = "menu_practice_button",
                            icon = Icons.Filled.Speed,
                            topColor = Color(0xFF4DD0E1),
                            bottomColor = Color(0xFF00838F),
                            textColor = BerryDeep,
                            height = 48.dp,
                            modifier = Modifier.weight(1f)
                        )
                        CandyButton(
                            text = CatalogData.localized(lang, "challenges"),
                            onClick = { onNavigate(GameScreen.CHALLENGES) },
                            testTag = "menu_challenges_button",
                            icon = Icons.Filled.Flag,
                            topColor = GrapeJelly,
                            bottomColor = Color(0xFF5E35B1),
                            height = 48.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Bottom-Right Garage & Kingdom Utility Dock
                Column(
                    modifier = Modifier
                        .width(342.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(BerryDeep.copy(alpha = 0.56f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CandyButton(
                            text = CatalogData.localized(lang, "garage"),
                            onClick = { onNavigate(GameScreen.GARAGE) },
                            testTag = "menu_garage_button",
                            icon = Icons.Filled.Build,
                            topColor = Color(0xFF7C4DFF),
                            bottomColor = Color(0xFF4527A0),
                            height = 48.dp,
                            modifier = Modifier.weight(1f)
                        )
                        CandyButton(
                            text = CatalogData.localized(lang, "characters"),
                            onClick = { onNavigate(GameScreen.CHARACTER_SELECT) },
                            testTag = "menu_characters_button",
                            icon = Icons.Filled.Face,
                            topColor = Color(0xFFFF7EB6),
                            bottomColor = Color(0xFFD81B60),
                            height = 48.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CandyButton(
                            text = CatalogData.localized(lang, "world_map"),
                            onClick = { onNavigate(GameScreen.WORLD_MAP) },
                            testTag = "menu_world_map_button",
                            icon = Icons.Filled.Public,
                            topColor = MintCream,
                            bottomColor = MintDark,
                            textColor = BerryDeep,
                            height = 48.dp,
                            modifier = Modifier.weight(1.1f)
                        )
                        CandyButton(
                            text = CatalogData.localized(lang, "tracks"),
                            onClick = { onNavigate(GameScreen.TRACK_COLLECTION) },
                            testTag = "menu_tracks_button",
                            icon = Icons.Filled.Map,
                            topColor = Color(0xFF4DD0E1),
                            bottomColor = Color(0xFF00838F),
                            textColor = BerryDeep,
                            height = 48.dp,
                            modifier = Modifier.weight(0.95f)
                        )
                        CandyButton(
                            text = CatalogData.localized(lang, "settings"),
                            onClick = { onNavigate(GameScreen.SETTINGS) },
                            testTag = "menu_settings_button",
                            icon = Icons.Filled.Settings,
                            topColor = Color(0xFF7E57C2),
                            bottomColor = Color(0xFF311B92),
                            height = 48.dp,
                            modifier = Modifier.weight(0.95f)
                        )
                    }
                }
            }
        }
    }
}
