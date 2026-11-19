package com.example.sweetrush.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
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
import com.example.ui.theme.StrawberryRed
import kotlin.math.sin

@Composable
fun MainMenuScreen(
    profile: PlayerProfile,
    userDisplayName: String = "Candy Racer",
    userPhotoUrl: String? = null,
    friendCode: String = "",
    activeRoomCode: String? = null,
    pendingRequestsCount: Int = 0,
    isMusicEnabled: Boolean = true,
    currentSongTitle: String = "Gumball Horizon",
    onToggleMusic: () -> Unit = {},
    onSwitchMusicTrack: () -> Unit = {},
    onNavigate: (GameScreen) -> Unit,
    onStartFreeRoam: () -> Unit = {},
    onEnterGatheringHub: () -> Unit = {}
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
        // Subtle perimeter scrim so the centered smooth 3D kart & kingdom stay crisp and clear
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to BerryDeep.copy(alpha = 0.52f),
                        0.20f to Color.Transparent,
                        0.66f to Color.Transparent,
                        1.0f to BerryDeep.copy(alpha = 0.68f)
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(CandyPink, MintCream, CaramelGold, SkyCandy, Color.White)
            for (i in 0 until 14) {
                val xFrac = ((i * 0.173f) % 1f)
                val yFrac = ((i * 0.291f + phase * 0.035f) % 1f)
                val radius = 2.8f + (i % 3) * 1.5f + sin(phase + i) * 1.1f
                drawCircle(
                    color = colors[i % colors.size].copy(alpha = 0.40f),
                    radius = radius.coerceAtLeast(1.8f),
                    center = Offset(size.width * xFrac, size.height * yFrac)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Organized Top Header: Brand Logo + Account / Notifications / Stats Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SweetRushLogo(compact = true)
                    Row(
                        modifier = Modifier
                            .testTag("menu_official_kart_banner")
                            .clip(RoundedCornerShape(16.dp))
                            .background(BerryDeep.copy(alpha = 0.80f))
                            .border(1.5.dp, CaramelGold.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
                            .clickable { onNavigate(GameScreen.GARAGE) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_official_candy_kart),
                            contentDescription = "Official Candy Kart",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 48.dp, height = 32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, CaramelGold, RoundedCornerShape(8.dp))
                        )
                        Column {
                            Text(
                                text = "${racer.name} • ${kart.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CaramelGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Wins ${profile.racesWon}  •  Coins ${profile.collectedWorldCoins.size}/48  •  Stars ${profile.collectedWorldStars.size}/10",
                                style = MaterialTheme.typography.labelSmall,
                                color = FrostingWhite
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (pendingRequestsCount > 0) {
                            Row(
                                modifier = Modifier
                                    .testTag("menu_friend_requests_badge")
                                    .clip(RoundedCornerShape(50))
                                    .background(CandyPink)
                                    .border(1.5.dp, Color.White, RoundedCornerShape(50))
                                    .clickable { onNavigate(GameScreen.ONLINE_LOBBY) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Filled.NotificationsActive,
                                    contentDescription = "Friend Requests",
                                    tint = FrostingWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$pendingRequestsCount REQ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FrostingWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 1-Tap Music ON/OFF Pill
                        Row(
                            modifier = Modifier
                                .testTag("menu_toggle_music_button")
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isMusicEnabled) MintDark.copy(alpha = 0.88f)
                                    else StrawberryRed.copy(alpha = 0.88f)
                                )
                                .border(
                                    1.5.dp,
                                    if (isMusicEnabled) MintCream else CaramelGold,
                                    RoundedCornerShape(50)
                                )
                                .clickable { onToggleMusic() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isMusicEnabled) "🔊 ON" else "🔇 OFF",
                                style = MaterialTheme.typography.labelSmall,
                                color = FrostingWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 1-Tap Uploaded MP3 Soundtrack Switcher Pill
                        Row(
                            modifier = Modifier
                                .testTag("menu_switch_music_button")
                                .clip(RoundedCornerShape(50))
                                .background(BerryDeep.copy(alpha = 0.85f))
                                .border(1.5.dp, CaramelGold.copy(alpha = 0.85f), RoundedCornerShape(50))
                                .clickable { onSwitchMusicTrack() }
                                .padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "🎵 $currentSongTitle",
                                style = MaterialTheme.typography.labelSmall,
                                color = CaramelGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Google Account Racer Chip
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(BerryPurple.copy(alpha = 0.88f))
                                .border(1.5.dp, MintCream.copy(alpha = 0.75f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            if (!userPhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = userPhotoUrl,
                                    contentDescription = "Google Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
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
                }
            }

            // Bottom Dual Frosted Glass Docks — structured & uncluttered
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // LEFT DOCK: Racing & Multiplayer Modes
                Column(
                    modifier = Modifier
                        .width(382.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(BerryDeep.copy(alpha = 0.72f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.34f), RoundedCornerShape(24.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = "RACING & SOCIAL MODES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MintCream,
                        fontWeight = FontWeight.Bold
                    )
                    // Primary Social Row: Online Squads + Gathering Hub
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        CandyButton(
                            text = if (activeRoomCode != null) "#$activeRoomCode" else CatalogData.localized(lang, "online"),
                            onClick = { onNavigate(GameScreen.ONLINE_LOBBY) },
                            testTag = "menu_online_button",
                            icon = Icons.Filled.Groups,
                            topColor = CaramelGold,
                            bottomColor = Color(0xFFFF6D00),
                            textColor = BerryDeep,
                            height = 48.dp,
                            modifier = Modifier.weight(1.05f)
                        )
                        CandyButton(
                            text = CatalogData.localized(lang, "gathering_hub"),
                            onClick = onEnterGatheringHub,
                            testTag = "menu_gathering_hub_button",
                            icon = Icons.Filled.Storefront,
                            topColor = Color(0xFFFF85C1),
                            bottomColor = CandyPinkDark,
                            height = 48.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Primary Play Row: Quick Race + Open World Free Roam
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        CandyButton(
                            text = CatalogData.localized(lang, "play"),
                            onClick = { onNavigate(GameScreen.QUICK_RACE_SETUP) },
                            testTag = "menu_play_button",
                            icon = Icons.Filled.PlayArrow,
                            topColor = Color(0xFFFF4D94),
                            bottomColor = CandyPinkDark,
                            height = 48.dp,
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
                            height = 48.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Secondary Modes Row: Grand Prix + Time Trial + Practice
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
                            modifier = Modifier.weight(1.05f)
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
                        CandyButton(
                            text = CatalogData.localized(lang, "practice"),
                            onClick = { onNavigate(GameScreen.PRACTICE_SETUP) },
                            testTag = "menu_practice_button",
                            icon = Icons.Filled.Speed,
                            topColor = Color(0xFF4DD0E1),
                            bottomColor = Color(0xFF00838F),
                            textColor = BerryDeep,
                            height = 48.dp,
                            modifier = Modifier.weight(0.95f)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // RIGHT DOCK: Garage, Racers, Kingdom & System
                Column(
                    modifier = Modifier
                        .width(344.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(BerryDeep.copy(alpha = 0.72f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.34f), RoundedCornerShape(24.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "GARAGE & KINGDOM",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyCandy,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
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
                            modifier = Modifier.weight(1.05f)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        CandyButton(
                            text = CatalogData.localized(lang, "tracks"),
                            onClick = { onNavigate(GameScreen.TRACK_COLLECTION) },
                            testTag = "menu_tracks_button",
                            icon = Icons.Filled.Map,
                            topColor = Color(0xFF4DD0E1),
                            bottomColor = Color(0xFF00838F),
                            textColor = BerryDeep,
                            height = 48.dp,
                            modifier = Modifier.weight(1f)
                        )
                        CandyButton(
                            text = CatalogData.localized(lang, "settings"),
                            onClick = { onNavigate(GameScreen.SETTINGS) },
                            testTag = "menu_settings_button",
                            icon = Icons.Filled.Settings,
                            topColor = Color(0xFF7E57C2),
                            bottomColor = Color(0xFF311B92),
                            height = 48.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
