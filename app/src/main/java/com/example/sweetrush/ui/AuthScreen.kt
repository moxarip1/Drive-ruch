package com.example.sweetrush.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sweetrush.render3d.CandyViewport3D
import com.example.sweetrush.render3d.CandyWorldRenderer
import com.example.sweetrush.render3d.RenderSceneMode
import com.example.ui.theme.BerryDeep
import com.example.ui.theme.BerryPurple
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyPinkDark
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.FrostingWhite
import com.example.ui.theme.MintCream
import com.example.ui.theme.MintDark
import com.example.ui.theme.SkyCandy

@Composable
fun AuthScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignInClick: () -> Unit
) {
    // Showcase the real 3D Candy Kingdom behind the Frosted Glass Sign-In Card
    val previewRenderer = remember {
        CandyWorldRenderer { _, _ -> }.apply {
            sceneMode = RenderSceneMode.MENU_KINGDOM
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CandyViewport3D(
            renderer = previewRenderer,
            allowOrbitDrag = false,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to BerryDeep.copy(alpha = 0.72f),
                        0.45f to BerryDeep.copy(alpha = 0.35f),
                        1.0f to BerryDeep.copy(alpha = 0.78f)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Brand + Feature Highlights
            Column(
                modifier = Modifier.weight(1.1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SweetRushLogo(compact = false)

                Text(
                    text = "سابق مع أصدقائك في مملكة الحلوى ثلاثية الأبعاد!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MintCream,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Race online with friends, create 1 / 2 / 4 player squads, talk live in-race, and sync your garage across devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostingWhite.copy(alpha = 0.90f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeaturePill(icon = Icons.Filled.Groups, label = "Squads 1•2•4", tint = CandyPink)
                    FeaturePill(icon = Icons.Filled.Mic, label = "Live Voice & Chat", tint = MintCream)
                    FeaturePill(icon = Icons.Filled.CameraAlt, label = "360° Pro Cam", tint = CaramelGold)
                }
            }

            Spacer(Modifier.width(20.dp))

            // Right: Frosted Neo-Arcade Sign-In Card
            Column(
                modifier = Modifier
                    .width(340.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(BerryPurple.copy(alpha = 0.88f))
                    .border(2.dp, Color.White.copy(alpha = 0.48f), RoundedCornerShape(28.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(CandyPink, CaramelGold)))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = "Online Account",
                        tint = FrostingWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "PLAYER SIGN IN",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FrostingWhite
                )
                Text(
                    text = "تسجيل الدخول بحساب Google لحفظ التقدم واللعب أونلاين",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyCandy
                )

                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CandyPinkDark.copy(alpha = 0.55f))
                            .border(1.dp, CandyPink, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = FrostingWhite
                        )
                    }
                }

                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(BerryDeep.copy(alpha = 0.72f))
                            .border(1.5.dp, MintCream.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = MintCream,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Connecting to Sugar Cloud...",
                            style = MaterialTheme.typography.labelLarge,
                            color = MintCream
                        )
                    }
                } else {
                    CandyButton(
                        text = "CONTINUE WITH GOOGLE",
                        onClick = onGoogleSignInClick,
                        testTag = "google_sign_in_button",
                        topColor = MintCream,
                        bottomColor = MintDark,
                        textColor = BerryDeep,
                        height = 54.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_sign_in_cta")
                    )
                }

                Text(
                    text = "Includes Free Friend Codes & Team Matchmaking",
                    style = MaterialTheme.typography.labelSmall,
                    color = FrostingWhite.copy(alpha = 0.74f)
                )
            }
        }
    }
}

@Composable
private fun FeaturePill(icon: ImageVector, label: String, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BerryDeep.copy(alpha = 0.72f))
            .border(1.dp, tint.copy(alpha = 0.65f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = FrostingWhite)
    }
}
