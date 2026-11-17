package com.example.sweetrush.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.ChallengeSpec
import com.example.sweetrush.model.Difficulty
import com.example.sweetrush.model.ExhaustStyle
import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.KartDecoration
import com.example.sweetrush.model.PlayerProfile
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.model.WheelDesign
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

@Composable
fun RaceSetupScreen(
    title: String,
    mode: GameMode,
    isCollectionPreviewOnly: Boolean,
    profile: PlayerProfile,
    selectedTrack: TrackId,
    selectedDifficulty: Difficulty,
    onSelectTrack: (TrackId) -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BerryDeep.copy(alpha = 0.78f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CandyScreenHeader(title = title, profile = profile, onBack = onBack)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left: 4 Tracks horizontal/vertical grid
                LazyColumn(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(CatalogData.tracks) { track ->
                        val isSelected = track.id == selectedTrack
                        val bestMs = profile.bestLapMsByTrack[track.id]
                        Row(
                            modifier = Modifier
                                .testTag("track_card_${track.id.name.lowercase()}")
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(listOf(CandyPink.copy(alpha = 0.35f), BerryCard))
                                    } else {
                                        Brush.horizontalGradient(listOf(BerryPurple.copy(alpha = 0.9f), BerryCard.copy(alpha = 0.8f)))
                                    }
                                )
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.5.dp,
                                    color = if (isSelected) CaramelGold else Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { onSelectTrack(track.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color swatch preview
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(track.skyTopHex), Color(track.roadStripeHex))
                                        )
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${track.laps}L",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = FrostingWhite
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(track.name, style = MaterialTheme.typography.titleLarge, color = FrostingWhite)
                                    Text(
                                        text = "★".repeat(track.difficultyStars),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = CaramelGold
                                    )
                                }
                                Text(track.subtitle, style = MaterialTheme.typography.labelSmall, color = MintCream)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = track.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FrostingWhite.copy(alpha = 0.82f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (bestMs != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Best Lap: ${KartPhysicsEngine.formatTime(bestMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CaramelGold
                                    )
                                }
                            }
                        }
                    }
                }

                // Right: Selected Track details + Difficulty + Start CTA
                val curTrack = CatalogData.trackById(selectedTrack)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(BerryPurple.copy(alpha = 0.92f))
                        .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (mode == GameMode.GRAND_PRIX) "4-RACE CANDY CUP" else curTrack.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = FrostingWhite
                        )
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(curTrack.featureHighlights) { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(CandyPink.copy(alpha = 0.28f))
                                        .border(1.dp, CandyPink, RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(tag, style = MaterialTheme.typography.labelSmall, color = FrostingWhite)
                                }
                            }
                        }

                        if (mode == GameMode.QUICK_RACE || mode == GameMode.GRAND_PRIX) {
                            Spacer(Modifier.height(14.dp))
                            Text("AI DIFFICULTY", style = MaterialTheme.typography.labelLarge, color = MintCream)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Difficulty.entries.forEach { diff ->
                                    val sel = diff == selectedDifficulty
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (sel) CaramelGold else BerryDeep.copy(alpha = 0.7f))
                                            .border(
                                                1.5.dp,
                                                if (sel) Color.White else Color.White.copy(alpha = 0.25f),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable { onSelectDifficulty(diff) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = diff.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (sel) BerryDeep else FrostingWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = when (mode) {
                                    GameMode.TIME_TRIAL -> "Solo lap attack! Beat your personal track records and earn bonus Candy Credits."
                                    GameMode.PRACTICE -> "Free-drive cruise with zero opponents. Master drifting, boost pads, and shortcuts."
                                    else -> curTrack.description
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = FrostingWhite.copy(alpha = 0.88f)
                            )
                        }
                    }

                    val racer = CatalogData.racerById(profile.selectedRacerId)
                    val kart = CatalogData.kartById(profile.selectedKartId)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Lineup: ${racer.name} in ${kart.name}",
                            style = MaterialTheme.typography.labelLarge,
                            color = SkyCandy
                        )
                        CandyButton(
                            text = if (isCollectionPreviewOnly) "RACE THIS TRACK" else if (mode == GameMode.GRAND_PRIX) "START GRAND PRIX" else "START RACE",
                            onClick = onStart,
                            testTag = "start_race_button",
                            icon = Icons.Filled.PlayArrow,
                            topColor = MintCream,
                            bottomColor = MintDark,
                            textColor = BerryDeep,
                            height = 54.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengesScreen(
    profile: PlayerProfile,
    onStartChallenge: (ChallengeSpec) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BerryDeep.copy(alpha = 0.84f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CandyScreenHeader(title = "Candy Challenges", profile = profile, onBack = onBack)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(CatalogData.challenges) { ch ->
                    val completed = profile.completedChallenges.contains(ch.id)
                    val track = CatalogData.trackById(ch.trackId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(BerryPurple.copy(alpha = 0.92f))
                            .border(
                                2.dp,
                                if (completed) MintCream else Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (completed) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Done", tint = MintCream, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(ch.title, style = MaterialTheme.typography.titleLarge, color = FrostingWhite)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${track.name} • ${ch.difficulty.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyCandy
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(ch.description, style = MaterialTheme.typography.bodyMedium, color = FrostingWhite.copy(alpha = 0.85f))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Reward: +${ch.rewardCredits} CC  •  +${ch.rewardXp} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = CaramelGold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        CandyButton(
                            text = if (completed) "REPLAY" else "ACCEPT",
                            onClick = { onStartChallenge(ch) },
                            testTag = "challenge_btn_${ch.id}",
                            icon = Icons.Filled.PlayArrow,
                            topColor = if (completed) SkyCandy else CaramelGold,
                            bottomColor = if (completed) Color(0xFF0288D1) else Butterscotch,
                            textColor = BerryDeep,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GarageAndCharacterScreen(
    initialTab: Int, // 0 = Characters, 1 = Karts, 2 = Customize, 3 = Upgrades
    profile: PlayerProfile,
    statusMessage: String?,
    onSelectRacer: (String) -> Unit,
    onSelectKart: (String) -> Unit,
    onSelectPaint: (String) -> Unit,
    onSelectWheel: (WheelDesign) -> Unit,
    onSelectExhaust: (ExhaustStyle) -> Unit,
    onSelectDecoration: (KartDecoration) -> Unit,
    onUpgradeStat: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    val selectedKart = CatalogData.kartById(profile.selectedKartId)
    val selectedRacer = CatalogData.racerById(profile.selectedRacerId)
    val custom = profile.kartCustomizations[profile.selectedKartId] ?: KartCustomization()
    val stats = CatalogData.effectiveStats(selectedKart, custom)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CandyScreenHeader(
                title = if (activeTab == 0) "Racer Roster" else "3D Candy Garage",
                profile = profile,
                onBack = onBack
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left side: Transparent window over live 3D Showroom + drag hint & live stats
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(BerryDeep.copy(alpha = 0.72f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(
                                text = "${selectedRacer.name} • #${selectedKart.racingNumber} ${selectedKart.name}",
                                style = MaterialTheme.typography.titleLarge,
                                color = FrostingWhite
                            )
                            Text(
                                text = "Drag anywhere on the 3D kart to rotate 360°",
                                style = MaterialTheme.typography.labelSmall,
                                color = MintCream
                            )
                        }
                    }

                    // Compact live stat card at bottom-left
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(BerryDeep.copy(alpha = 0.80f))
                            .border(1.5.dp, CandyPink.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatBarRow("SPEED", stats.speed, CandyPink)
                        StatBarRow("ACCEL", stats.acceleration, MintCream)
                        StatBarRow("HANDLING", stats.handling, SkyCandy)
                        StatBarRow("BOOST", stats.boost, CaramelGold)
                        StatBarRow("STABILITY", stats.stability, Color(0xFFB388FF))
                    }
                }

                // Right side: Tabs + Customization Panel
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(BerryDeep.copy(alpha = 0.88f))
                        .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                        .padding(12.dp)
                ) {
                    val tabs = listOf("Racers", "Karts", "Style", "Upgrades")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tabs.forEachIndexed { idx, label ->
                            val sel = activeTab == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (sel) CandyPink else BerryCard)
                                    .border(
                                        1.5.dp,
                                        if (sel) Color.White else Color.White.copy(alpha = 0.25f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { activeTab = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (statusMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CaramelGold)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(statusMessage, style = MaterialTheme.typography.labelSmall, color = BerryDeep)
                        }
                    }

                    when (activeTab) {
                        0 -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(CatalogData.racers) { racer ->
                                    val unlocked = profile.unlockedRacers.contains(racer.id)
                                    val selected = profile.selectedRacerId == racer.id
                                    Row(
                                        modifier = Modifier
                                            .testTag("racer_item_${racer.id}")
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (selected) CandyPink.copy(alpha = 0.3f) else BerryPurple)
                                            .border(
                                                2.dp,
                                                if (selected) CaramelGold else Color.White.copy(alpha = 0.2f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable { onSelectRacer(racer.id) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(racer.suitPrimaryHex))
                                                .border(2.dp, Color(racer.suitSecondaryHex), CircleShape)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${racer.name} — ${racer.title}", style = MaterialTheme.typography.titleMedium, color = FrostingWhite)
                                            Text(racer.personality, style = MaterialTheme.typography.bodyMedium, color = FrostingWhite.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Power: ${racer.signaturePowerUp.displayName}", style = MaterialTheme.typography.labelSmall, color = MintCream)
                                        }
                                        if (!unlocked) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = CaramelGold, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("${racer.unlockCost} CC", style = MaterialTheme.typography.labelLarge, color = CaramelGold)
                                            }
                                        } else if (selected) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = MintCream)
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(CatalogData.karts) { kart ->
                                    val unlocked = profile.unlockedKarts.contains(kart.id)
                                    val selected = profile.selectedKartId == kart.id
                                    Row(
                                        modifier = Modifier
                                            .testTag("kart_item_${kart.id}")
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (selected) CandyPink.copy(alpha = 0.3f) else BerryPurple)
                                            .border(
                                                2.dp,
                                                if (selected) CaramelGold else Color.White.copy(alpha = 0.2f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable { onSelectKart(kart.id) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(kart.defaultPrimaryHex))
                                                .border(2.dp, Color(kart.defaultSecondaryHex), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("#${kart.racingNumber}", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(kart.name, style = MaterialTheme.typography.titleMedium, color = FrostingWhite)
                                            Text(kart.tagline, style = MaterialTheme.typography.bodyMedium, color = FrostingWhite.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (!unlocked) {
                                            Text("${kart.unlockCost} CC", style = MaterialTheme.typography.labelLarge, color = CaramelGold)
                                        } else if (selected) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = MintCream)
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                item {
                                    Text("CANDY PAINT", style = MaterialTheme.typography.labelLarge, color = MintCream)
                                    Spacer(Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(CatalogData.paintJobs) { paint ->
                                            val owned = profile.unlockedPaints.contains(paint.id)
                                            val sel = custom.paintId == paint.id
                                            Column(
                                                modifier = Modifier
                                                    .width(96.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (sel) CandyPink.copy(alpha = 0.35f) else BerryPurple)
                                                    .border(2.dp, if (sel) CaramelGold else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                                    .clickable { onSelectPaint(paint.id) }
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(paint.primaryHex))
                                                        .border(2.dp, Color(paint.secondaryHex), CircleShape)
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(paint.name, style = MaterialTheme.typography.labelSmall, color = FrostingWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(if (owned) "Owned" else "${paint.cost} CC", style = MaterialTheme.typography.labelSmall, color = CaramelGold)
                                            }
                                        }
                                    }
                                }
                                item {
                                    Text("WHEEL DESIGNS", style = MaterialTheme.typography.labelLarge, color = MintCream)
                                    Spacer(Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(WheelDesign.entries) { w ->
                                            val owned = profile.unlockedWheels.contains(w)
                                            val sel = custom.wheelDesign == w
                                            Column(
                                                modifier = Modifier
                                                    .width(106.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (sel) CandyPink.copy(alpha = 0.35f) else BerryPurple)
                                                    .border(2.dp, if (sel) CaramelGold else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                                    .clickable { onSelectWheel(w) }
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(w.displayName, style = MaterialTheme.typography.labelSmall, color = FrostingWhite, maxLines = 1)
                                                Text(if (owned) "Owned" else "${w.cost} CC", style = MaterialTheme.typography.labelSmall, color = CaramelGold)
                                            }
                                        }
                                    }
                                }
                                item {
                                    Text("EXHAUST EFFECTS", style = MaterialTheme.typography.labelLarge, color = MintCream)
                                    Spacer(Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(ExhaustStyle.entries) { ex ->
                                            val owned = profile.unlockedExhausts.contains(ex)
                                            val sel = custom.exhaustStyle == ex
                                            Column(
                                                modifier = Modifier
                                                    .width(106.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (sel) CandyPink.copy(alpha = 0.35f) else BerryPurple)
                                                    .border(2.dp, if (sel) CaramelGold else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                                    .clickable { onSelectExhaust(ex) }
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(ex.displayName, style = MaterialTheme.typography.labelSmall, color = FrostingWhite, maxLines = 1)
                                                Text(if (owned) "Owned" else "${ex.cost} CC", style = MaterialTheme.typography.labelSmall, color = CaramelGold)
                                            }
                                        }
                                    }
                                }
                                item {
                                    Text("KART DECORATIONS", style = MaterialTheme.typography.labelLarge, color = MintCream)
                                    Spacer(Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(KartDecoration.entries) { dec ->
                                            val owned = profile.unlockedDecorations.contains(dec)
                                            val sel = custom.decoration == dec
                                            Column(
                                                modifier = Modifier
                                                    .width(106.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (sel) CandyPink.copy(alpha = 0.35f) else BerryPurple)
                                                    .border(2.dp, if (sel) CaramelGold else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                                    .clickable { onSelectDecoration(dec) }
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(dec.displayName, style = MaterialTheme.typography.labelSmall, color = FrostingWhite, maxLines = 1)
                                                Text(if (owned) "Owned" else "${dec.cost} CC", style = MaterialTheme.typography.labelSmall, color = CaramelGold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            val upgrades = listOf(
                                Triple("speed", "Top Speed", custom.upgrades.speed),
                                Triple("acceleration", "Acceleration", custom.upgrades.acceleration),
                                Triple("handling", "Handling", custom.upgrades.handling),
                                Triple("boost", "Nitro Boost", custom.upgrades.boost),
                                Triple("stability", "Stability", custom.upgrades.stability)
                            )
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(upgrades) { (key, label, lvl) ->
                                    val cost = 90 + lvl * 70
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(BerryPurple)
                                            .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(label, style = MaterialTheme.typography.titleMedium, color = FrostingWhite)
                                            Text("Level $lvl / 5", style = MaterialTheme.typography.labelSmall, color = MintCream)
                                        }
                                        CandyButton(
                                            text = if (lvl >= 5) "MAX" else "+1 ($cost CC)",
                                            onClick = { if (lvl < 5) onUpgradeStat(key) },
                                            testTag = "upgrade_$key",
                                            icon = if (lvl < 5) Icons.Filled.Add else Icons.Filled.Star,
                                            topColor = if (lvl >= 5) MintCream else CaramelGold,
                                            bottomColor = if (lvl >= 5) MintDark else Butterscotch,
                                            textColor = BerryDeep,
                                            height = 48.dp,
                                            modifier = Modifier.width(150.dp)
                                        )
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
