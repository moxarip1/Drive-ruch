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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.ui.theme.StrawberryRed

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

private fun kartBakeryRecipeAr(style: com.example.sweetrush.model.KartBodyStyle): String = when (style) {
    com.example.sweetrush.model.KartBodyStyle.STRAWBERRY_ROCKET -> "شاسيه ويفر مقرمش • كريمة فراولة مخفوقة • مقدمة مغمسة بالشوكولاتة"
    com.example.sweetrush.model.KartBodyStyle.PEPPERMINT_RACER -> "هيكل قصب السكر الحلزوني • أجنحة فورمولا ويفر • أقراص نعناع"
    com.example.sweetrush.model.KartBodyStyle.CHOCOLATE_CRUISER -> "مربعات شوكولاتة داكنة بارزة • تيربو ويفر ثلاثي • كراميل ذائب"
    com.example.sweetrush.model.KartBodyStyle.JELLYBEAN_MACHINE -> "كبسولة جيلي شفافة لامعة • أنابيب حلوى مطاطية ملونة • جناح حامض"
    com.example.sweetrush.model.KartBodyStyle.CARAMEL_THUNDER -> "توفي ذهبي مصهور • فتحات وافل عسلية • جناح كراميل مزدوج"
    com.example.sweetrush.model.KartBodyStyle.CUPCAKE_KART -> "قالب كب كيك مضلع • قبة كريمة مخفوقة • رشات سكر وكرز لامع"
    com.example.sweetrush.model.KartBodyStyle.DONUT_DRIFTER -> "حلقة دونات ذهبية مزججة بالفراولة • قطرات آيسينغ • 24 حبيبة سكر"
    com.example.sweetrush.model.KartBodyStyle.MARSHMALLOW_SPEEDSTER -> "أسطوانات مارشميلو محمصة • جسر بسكويت وشوكولاتة • غزل البنات"
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
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onFocusCamera: (String) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var activeCamPreset by remember { mutableStateOf("AUTO") }

    val selectedKart = CatalogData.kartById(profile.selectedKartId)
    val selectedRacer = CatalogData.racerById(profile.selectedRacerId)
    val custom = profile.kartCustomizations[profile.selectedKartId] ?: KartCustomization()
    val activePaint = CatalogData.paintById(custom.paintId)
    val primaryColorHex = if (custom.paintId == "default") selectedKart.defaultPrimaryHex else activePaint.primaryHex
    val secondaryColorHex = if (custom.paintId == "default") selectedKart.defaultSecondaryHex else activePaint.secondaryHex
    val stats = CatalogData.effectiveStats(selectedKart, custom)
    val tuning = selectedKart.physicsTuning

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CandyScreenHeader(
                title = if (activeTab == 0) "Racer Roster • المتسابقون" else "Sugar Rush Kart Bakery • مصنع حلوى السيارات",
                profile = profile,
                onBack = onBack
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left side: Transparent window over live 3D Kart Bakery Showroom + camera angle presets + live recipe & stats
                Column(
                    modifier = Modifier
                        .weight(0.98f)
                        .fillMaxHeight()
                        .padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BerryDeep.copy(alpha = 0.80f))
                                    .border(1.5.dp, CaramelGold.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(primaryColorHex))
                                                .border(1.5.dp, Color(secondaryColorHex), CircleShape)
                                        )
                                        Text(
                                            text = "${selectedRacer.name} • #${selectedKart.racingNumber} ${selectedKart.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = FrostingWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = kartBakeryRecipeAr(selectedKart.style),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CaramelGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = onZoomIn,
                                    modifier = Modifier
                                        .testTag("garage_zoom_in")
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(BerryPurple.copy(alpha = 0.88f))
                                        .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", tint = MintCream)
                                }
                                IconButton(
                                    onClick = onZoomOut,
                                    modifier = Modifier
                                        .testTag("garage_zoom_out")
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(BerryPurple.copy(alpha = 0.88f))
                                        .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out", tint = SkyCandy)
                                }
                            }
                        }

                        // Interactive 3D Bakery Camera Angle Quick-Focus Bar
                        val cameraPresets = listOf(
                            "AUTO" to "360° تلقائي",
                            "FRONT" to "الأمام",
                            "SIDE" to "الجانب",
                            "WHEELS" to "الإطارات",
                            "REAR" to "العادم",
                            "TOP" to "السقف"
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(cameraPresets) { (presetKey, labelAr) ->
                                val isCamSelected = activeCamPreset == presetKey
                                Box(
                                    modifier = Modifier
                                        .testTag("garage_cam_${presetKey.lowercase()}")
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isCamSelected) CaramelGold else BerryDeep.copy(alpha = 0.78f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isCamSelected) Color.White else Color.White.copy(alpha = 0.3f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            activeCamPreset = presetKey
                                            onFocusCamera(presetKey)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = labelAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCamSelected) BerryDeep else FrostingWhite
                                    )
                                }
                            }
                        }
                    }

                    // Compact live stat + bakery recipe loadout card at bottom-left
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(BerryDeep.copy(alpha = 0.74f))
                            .border(1.5.dp, CandyPink.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val gripScore = ((stats.stability + (tuning.lateralGrip / 11f).coerceIn(0f, 1f)) * 0.5f).coerceIn(0f, 1f)
                        StatBarRow("TOP SPEED", stats.speed, CandyPink)
                        StatBarRow("ACCELERATION", stats.acceleration, MintCream)
                        StatBarRow("HANDLING", stats.handling, SkyCandy)
                        StatBarRow("GRIP", gripScore, Color(0xFFB388FF))
                        StatBarRow("BOOST", stats.boost, CaramelGold)
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = "إطارات: ${custom.wheelDesign.displayName}  •  عادم: ${custom.exhaustStyle.displayName}  •  زينة: ${custom.decoration.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyCandy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right side: Tabs + Sugar Rush Kart Bakery Customization Panel
                Column(
                    modifier = Modifier
                        .weight(1.22f)
                        .fillMaxHeight()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(BerryDeep.copy(alpha = 0.90f))
                        .border(2.dp, CaramelGold.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                        .padding(12.dp)
                ) {
                    val tabs = listOf(
                        "Racers • السائقون",
                        "Karts • السيارات",
                        "Bakery • التعديل",
                        "Oven • الترقية"
                    )
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
                                        if (sel) CaramelGold else Color.White.copy(alpha = 0.25f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        activeTab = idx
                                        when (idx) {
                                            0 -> {
                                                activeCamPreset = "FRONT"
                                                onFocusCamera("FRONT")
                                            }
                                            1 -> {
                                                activeCamPreset = "AUTO"
                                                onFocusCamera("AUTO")
                                            }
                                            2 -> {
                                                activeCamPreset = "SIDE"
                                                onFocusCamera("SIDE")
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FrostingWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
                                            .background(if (selected) CandyPink.copy(alpha = 0.32f) else BerryPurple)
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
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Color(racer.suitPrimaryHex))
                                                .border(2.5.dp, Color(racer.suitSecondaryHex), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(racer.helmetHex))
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${racer.name} — ${racer.title}", style = MaterialTheme.typography.titleMedium, color = FrostingWhite)
                                            Text(racer.personality, style = MaterialTheme.typography.bodyMedium, color = FrostingWhite.copy(alpha = 0.82f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("الحلوى الخاصة: ${racer.signaturePowerUp.displayName}", style = MaterialTheme.typography.labelSmall, color = MintCream)
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
                                            .background(if (selected) CandyPink.copy(alpha = 0.32f) else BerryPurple)
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
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(kart.defaultPrimaryHex), Color(kart.defaultSecondaryHex))
                                                    )
                                                )
                                                .border(2.dp, Color(kart.accentHex), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("#${kart.racingNumber}", style = MaterialTheme.typography.labelLarge, color = BerryDeep)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(kart.name, style = MaterialTheme.typography.titleMedium, color = FrostingWhite)
                                            Text(
                                                text = kartBakeryRecipeAr(kart.style),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CaramelGold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = kart.tagline,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = FrostingWhite.copy(alpha = 0.78f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (!unlocked) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = CaramelGold, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("${kart.unlockCost} CC", style = MaterialTheme.typography.labelLarge, color = CaramelGold)
                                            }
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("CANDY GLAZE & FROSTING • طلاء الكريمة والحلوى", style = MaterialTheme.typography.labelLarge, color = MintCream)
                                        Text(
                                            text = "معاينة الجانب",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyCandy,
                                            modifier = Modifier.clickable {
                                                activeCamPreset = "SIDE"
                                                onFocusCamera("SIDE")
                                            }
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(CatalogData.paintJobs) { paint ->
                                            val owned = profile.unlockedPaints.contains(paint.id)
                                            val sel = custom.paintId == paint.id
                                            Column(
                                                modifier = Modifier
                                                    .width(104.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (sel) CandyPink.copy(alpha = 0.38f) else BerryPurple)
                                                    .border(2.dp, if (sel) CaramelGold else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                                    .clickable { onSelectPaint(paint.id) }
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(paint.primaryHex))
                                                        .border(3.dp, Color(paint.secondaryHex), CircleShape)
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(paint.name, style = MaterialTheme.typography.labelSmall, color = FrostingWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(if (owned) "مملوك • Owned" else "${paint.cost} CC", style = MaterialTheme.typography.labelSmall, color = if (sel) MintCream else CaramelGold)
                                            }
                                        }
                                    }
                                }
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("BAKERY WHEELS • إطارات البسكويت والدونات", style = MaterialTheme.typography.labelLarge, color = MintCream)
                                        Text(
                                            text = "تقريب الإطارات",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyCandy,
                                            modifier = Modifier.clickable {
                                                activeCamPreset = "WHEELS"
                                                onFocusCamera("WHEELS")
                                            }
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(WheelDesign.entries) { w ->
                                            val owned = profile.unlockedWheels.contains(w)
                                            val sel = custom.wheelDesign == w
                                            Column(
                                                modifier = Modifier
                                                    .width(114.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (sel) CandyPink.copy(alpha = 0.38f) else BerryPurple)
                                                    .border(2.dp, if (sel) CaramelGold else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        activeCamPreset = "WHEELS"
                                                        onSelectWheel(w)
                                                    }
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(w.primaryHex))
                                                        .border(4.dp, Color(w.rimHex), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(w.rimHex))
                                                    )
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text(w.displayName, style = MaterialTheme.typography.labelSmall, color = FrostingWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(if (owned) "مملوك • Owned" else "${w.cost} CC", style = MaterialTheme.typography.labelSmall, color = if (sel) MintCream else CaramelGold)
                                            }
                                        }
                                    }
                                }
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("CANDY EXHAUSTS • عادم الحلوى والتيربو", style = MaterialTheme.typography.labelLarge, color = MintCream)
                                        Text(
                                            text = "تقريب العادم",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyCandy,
                                            modifier = Modifier.clickable {
                                                activeCamPreset = "REAR"
                                                onFocusCamera("REAR")
                                            }
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(ExhaustStyle.entries) { ex ->
                                            val owned = profile.unlockedExhausts.contains(ex)
                                            val sel = custom.exhaustStyle == ex
                                            Column(
                                                modifier = Modifier
                                                    .width(114.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (sel) CandyPink.copy(alpha = 0.38f) else BerryPurple)
                                                    .border(2.dp, if (sel) CaramelGold else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        activeCamPreset = "REAR"
                                                        onSelectExhaust(ex)
                                                    }
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(ex.flameHex))
                                                        .border(2.dp, FrostingWhite, CircleShape)
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(ex.displayName, style = MaterialTheme.typography.labelSmall, color = FrostingWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(if (owned) "مملوك • Owned" else "${ex.cost} CC", style = MaterialTheme.typography.labelSmall, color = if (sel) MintCream else CaramelGold)
                                            }
                                        }
                                    }
                                }
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("BAKERY TOPPERS • تزيين الكرز والماكارون", style = MaterialTheme.typography.labelLarge, color = MintCream)
                                        Text(
                                            text = "تقريب السقف",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyCandy,
                                            modifier = Modifier.clickable {
                                                activeCamPreset = "TOP"
                                                onFocusCamera("TOP")
                                            }
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(KartDecoration.entries) { dec ->
                                            val owned = profile.unlockedDecorations.contains(dec)
                                            val sel = custom.decoration == dec
                                            val badgeColor = when (dec) {
                                                KartDecoration.NONE -> BerryCard
                                                KartDecoration.CHERRY_TOPPER -> StrawberryRed
                                                KartDecoration.CANDY_CANE_WING -> CandyPink
                                                KartDecoration.LOLLIPOP_ANTENNA -> SkyCandy
                                                KartDecoration.CROWN_SPRINKLE -> CaramelGold
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .width(114.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (sel) CandyPink.copy(alpha = 0.38f) else BerryPurple)
                                                    .border(2.dp, if (sel) CaramelGold else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        activeCamPreset = "TOP"
                                                        onSelectDecoration(dec)
                                                    }
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(badgeColor)
                                                        .border(1.5.dp, FrostingWhite, RoundedCornerShape(8.dp))
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(dec.displayName, style = MaterialTheme.typography.labelSmall, color = FrostingWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(if (owned) "مملوك • Owned" else "${dec.cost} CC", style = MaterialTheme.typography.labelSmall, color = if (sel) MintCream else CaramelGold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            val upgrades = listOf(
                                Triple("speed", "Top Speed • السرعة القصوى", custom.upgrades.speed),
                                Triple("acceleration", "Acceleration • التسارع", custom.upgrades.acceleration),
                                Triple("handling", "Handling • المناورة", custom.upgrades.handling),
                                Triple("boost", "Nitro Boost • نيترو السكر", custom.upgrades.boost),
                                Triple("stability", "Stability • الثبات", custom.upgrades.stability)
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
                                        Column(
                                            modifier = Modifier.weight(1f).padding(end = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(label, style = MaterialTheme.typography.titleMedium, color = FrostingWhite)
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                for (stage in 1..5) {
                                                    val activeStage = stage <= lvl
                                                    Box(
                                                        modifier = Modifier
                                                            .width(22.dp)
                                                            .height(8.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(
                                                                if (activeStage) CaramelGold else BerryDeep.copy(alpha = 0.7f)
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (activeStage) FrostingWhite else Color.White.copy(alpha = 0.2f),
                                                                RoundedCornerShape(4.dp)
                                                            )
                                                    )
                                                }
                                                Spacer(Modifier.width(4.dp))
                                                Text("Lv.$lvl/5", style = MaterialTheme.typography.labelSmall, color = MintCream)
                                            }
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
                                            modifier = Modifier.width(146.dp)
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

