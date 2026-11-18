package com.example.sweetrush.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.PlayerProfile
import com.example.ui.theme.BerryDeep
import com.example.ui.theme.BerryPurple
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyPinkDark
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.FrostingWhite
import com.example.ui.theme.MintCream
import com.example.ui.theme.MintDark

@Composable
fun CandyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    icon: ImageVector? = null,
    topColor: Color = CandyPink,
    bottomColor: Color = CandyPinkDark,
    textColor: Color = FrostingWhite,
    height: Dp = 50.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "candyBtnScale"
    )

    Box(
        modifier = modifier
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .scale(scale)
            .height(height)
            .shadow(8.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            .border(2.5.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(22.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Glossy candy top highlight strip
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.86f)
                .padding(top = 4.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.28f))
        )

        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 4f)
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SweetRushLogo(modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Peppermint Swirl Emblem
            Canvas(modifier = Modifier.size(if (compact) 36.dp else 44.dp)) {
                val r = size.minDimension * 0.48f
                val c = center
                drawCircle(Color.White, radius = r, center = c)
                for (i in 0 until 6) {
                    val startA = i * 60f
                    drawArc(
                        color = if (i % 2 == 0) Color(0xFFFF2448) else Color(0xFF3CE8B5),
                        startAngle = startA,
                        sweepAngle = 36f,
                        useCenter = true,
                        topLeft = Offset(c.x - r * 0.88f, c.y - r * 0.88f),
                        size = androidx.compose.ui.geometry.Size(r * 1.76f, r * 1.76f)
                    )
                }
                drawCircle(Color.White, radius = r * 0.24f, center = c)
                drawCircle(Color(0xFFFFD6EC), radius = r, center = c, style = Stroke(width = 4f))
            }
            Spacer(Modifier.width(if (compact) 8.dp else 10.dp))
            Column {
                Text(
                    text = "SWEET RUSH",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = if (compact) 30.sp else 38.sp,
                        letterSpacing = 1.4.sp,
                        shadow = Shadow(Color(0xFF880E4F), Offset(0f, 4f), 8f)
                    ),
                    color = FrostingWhite
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(CaramelGold, CandyPink)))
                        .border(1.5.dp, Color.White, RoundedCornerShape(50))
                        .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CANDY WORLD 3D",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                        color = FrostingWhite
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerProfilePill(profile: PlayerProfile, modifier: Modifier = Modifier) {
    val xpNeeded = CatalogData.xpForLevel(profile.level).coerceAtLeast(1)
    val xpFrac = (profile.xp.toFloat() / xpNeeded.toFloat()).coerceIn(0f, 1f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(BerryDeep.copy(alpha = 0.82f))
            .border(2.dp, CandyPink.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Level badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CaramelGold, CandyPink))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${profile.level}",
                    style = MaterialTheme.typography.labelLarge,
                    color = FrostingWhite
                )
            }
            Spacer(Modifier.width(6.dp))
            Column {
                Text("LVL ${profile.level}", style = MaterialTheme.typography.labelSmall, color = FrostingWhite)
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(xpFrac)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MintCream)
                    )
                }
            }
        }

        // Credits
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Credits",
                tint = CaramelGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${profile.credits} CC",
                style = MaterialTheme.typography.titleMedium,
                color = CaramelGold
            )
        }
    }
}

@Composable
fun CandyScreenHeader(
    title: String,
    profile: PlayerProfile,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .testTag("back_button")
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BerryPurple.copy(alpha = 0.9f))
                    .border(2.dp, Color.White.copy(alpha = 0.65f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = FrostingWhite
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium.copy(
                    shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 3f), 6f)
                ),
                color = FrostingWhite
            )
        }
        PlayerProfilePill(profile = profile)
    }
}

@Composable
fun StatBarRow(
    label: String,
    value: Float,
    color: Color = MintCream,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 340f),
        label = "statBarAnim"
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = FrostingWhite.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedValue)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(color, Color.White.copy(alpha = 0.92f))))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${(animatedValue * 100).toInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
