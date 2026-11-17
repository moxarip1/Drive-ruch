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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
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

@Composable
fun MainMenuScreen(
    profile: PlayerProfile,
    onNavigate: (GameScreen) -> Unit
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
        // Subtle gradient scrim over the live 3D Candy Kingdom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            BerryDeep.copy(alpha = 0.58f),
                            Color.Transparent,
                            BerryDeep.copy(alpha = 0.68f)
                        )
                    )
                )
        )

        // Floating candy sparkle particles over menu
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(CandyPink, MintCream, CaramelGold, SkyCandy, Color.White)
            for (i in 0 until 18) {
                val xFrac = ((i * 0.173f) % 1f)
                val yFrac = ((i * 0.291f + phase * 0.04f) % 1f)
                val radius = 3.5f + (i % 3) * 2f + sin(phase + i) * 1.5f
                drawCircle(
                    color = colors[i % colors.size].copy(alpha = 0.55f),
                    radius = radius.coerceAtLeast(2f),
                    center = Offset(size.width * xFrac, size.height * yFrac)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: Logo + Player Progression Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                SweetRushLogo()

                Column(horizontalAlignment = Alignment.End) {
                    PlayerProfilePill(profile = profile)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(BerryPurple.copy(alpha = 0.85f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${racer.name} • ${kart.name}  |  Wins: ${profile.racesWon}  •  GP Cups: ${profile.grandPrixGoldTrophies}",
                            style = MaterialTheme.typography.labelSmall,
                            color = FrostingWhite
                        )
                    }
                }
            }

            // Bottom landscape button deck: Left (Race Modes) + Right (Garage & Kingdom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Primary Racing Modes column
                Column(
                    modifier = Modifier.width(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CandyButton(
                        text = CatalogData.localized(lang, "play"),
                        onClick = { onNavigate(GameScreen.QUICK_RACE_SETUP) },
                        testTag = "menu_play_button",
                        icon = Icons.Filled.PlayArrow,
                        topColor = Color(0xFFFF4D94),
                        bottomColor = CandyPinkDark,
                        height = 56.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            topColor = MintCream,
                            bottomColor = MintDark,
                            textColor = BerryDeep,
                            height = 48.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CandyButton(
                            text = CatalogData.localized(lang, "practice"),
                            onClick = { onNavigate(GameScreen.PRACTICE_SETUP) },
                            testTag = "menu_practice_button",
                            icon = Icons.Filled.Speed,
                            topColor = SkyCandy,
                            bottomColor = Color(0xFF0288D1),
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

                Spacer(Modifier.width(16.dp))

                // Kingdom & Customization deck
                Column(
                    modifier = Modifier.width(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            modifier = Modifier.weight(1.25f)
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
