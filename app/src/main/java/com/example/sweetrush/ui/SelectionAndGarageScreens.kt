package com.example.sweetrush.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
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

internal fun trackMovieMapRes(trackId: TrackId): Int = when (trackId) {
    TrackId.PEPPERMINT_PLAZA -> R.drawable.img_sugar_rush_world_map
    TrackId.CHOCOLATE_VOLCANO -> R.drawable.img_map_diet_cola_mountain
    TrackId.GUMDROP_GARDEN -> R.drawable.img_map_candy_cane_forest
    TrackId.CUPCAKE_COAST -> R.drawable.img_map_gumball_gorge
}

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
    onSelectRacer: (String) -> Unit = {},
    onSelectKart: (String) -> Unit = {},
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
                // Left: 4 Movie Tracks with authentic downloaded movie map vistas
                LazyColumn(
                    modifier = Modifier
                        .weight(1.30f)
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
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Downloaded Movie Track Vista Photo + Lap Badge
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Image(
                                    painter = painterResource(id = trackMovieMapRes(track.id)),
                                    contentDescription = track.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 88.dp, height = 62.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(2.dp, if (isSelected) CaramelGold else Color.White.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BerryDeep.copy(alpha = 0.88f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${track.laps}L",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CaramelGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(track.name, style = MaterialTheme.typography.titleMedium, color = FrostingWhite)
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

                // Right: Selected Movie Track Vista + Quick Movie Racer & Kart Switcher + Difficulty + Start CTA
                val curTrack = CatalogData.trackById(selectedTrack)
                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(BerryPurple.copy(alpha = 0.92f))
                        .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(
                                    id = if (mode == GameMode.GRAND_PRIX) R.drawable.img_map_sugar_rush_kingdom
                                    else trackMovieMapRes(selectedTrack)
                                ),
                                contentDescription = curTrack.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 86.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, CaramelGold, RoundedCornerShape(12.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (mode == GameMode.GRAND_PRIX) "4-RACE SUGAR RUSH CUP" else curTrack.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = FrostingWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    items(curTrack.featureHighlights) { tag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(CandyPink.copy(alpha = 0.28f))
                                                .border(1.dp, CandyPink, RoundedCornerShape(50))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(tag, style = MaterialTheme.typography.labelSmall, color = FrostingWhite)
                                        }
                                    }
                                }
                            }
                        }

                        // Quick 24 Movie & Anime Racers & Karts Switcher Strip inside Race Setup
                        Text(
                            text = "اختر الشخصية والسيارة (24 متسابق من الفيلم والأنمي):",
                            style = MaterialTheme.typography.labelSmall,
                            color = CaramelGold,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(CatalogData.racers) { rItem ->
                                val isSelRacer = rItem.id == profile.selectedRacerId
                                val sigKart = CatalogData.kartById(rItem.favoriteKartId)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelRacer) CandyPink.copy(alpha = 0.45f) else BerryDeep.copy(alpha = 0.78f))
                                        .border(
                                            1.5.dp,
                                            if (isSelRacer) CaramelGold else Color.White.copy(alpha = 0.22f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            onSelectRacer(rItem.id)
                                            onSelectKart(rItem.favoriteKartId)
                                        }
                                        .padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    RacerAvatarThumb(
                                        racerId = rItem.id,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, if (isSelRacer) CaramelGold else Color.White, CircleShape)
                                    )
                                    Column {
                                        Text(
                                            text = rItem.name.substringBefore(" "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = FrostingWhite,
                                            fontWeight = if (isSelRacer) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = "#${sigKart.racingNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CaramelGold
                                        )
                                    }
                                }
                            }
                        }

                        if (mode == GameMode.QUICK_RACE || mode == GameMode.GRAND_PRIX) {
                            Text("AI DIFFICULTY", style = MaterialTheme.typography.labelSmall, color = MintCream)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Difficulty.entries.forEach { diff ->
                                    val sel = diff == selectedDifficulty
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (sel) CaramelGold else BerryDeep.copy(alpha = 0.7f))
                                            .border(
                                                1.5.dp,
                                                if (sel) Color.White else Color.White.copy(alpha = 0.25f),
                                                RoundedCornerShape(12.dp)
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
                            Text(
                                text = when (mode) {
                                    GameMode.TIME_TRIAL -> "Solo lap attack! Beat your personal track records and earn bonus Candy Credits."
                                    GameMode.PRACTICE -> "Free-drive cruise with zero opponents. Master drifting, boost pads, and shortcuts."
                                    else -> curTrack.description
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = FrostingWhite.copy(alpha = 0.88f),
                                maxLines = 2
                            )
                        }
                    }

                    val racer = CatalogData.racerById(profile.selectedRacerId)
                    val kart = CatalogData.kartById(profile.selectedKartId)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RacerAvatarThumb(
                                racerId = racer.id,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, CaramelGold, CircleShape)
                            )
                            Image(
                                painter = painterResource(id = kartPreviewRes(kart.id)),
                                contentDescription = kart.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 44.dp, height = 30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, MintCream, RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = "${racer.name} • ${kart.name}",
                                style = MaterialTheme.typography.labelLarge,
                                color = SkyCandy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        CandyButton(
                            text = if (isCollectionPreviewOnly) "RACE THIS TRACK" else if (mode == GameMode.GRAND_PRIX) "START GRAND PRIX" else "START RACE",
                            onClick = onStart,
                            testTag = "start_race_button",
                            icon = Icons.Filled.PlayArrow,
                            topColor = MintCream,
                            bottomColor = MintDark,
                            textColor = BerryDeep,
                            height = 50.dp,
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

internal fun kartBakeryRecipeAr(style: com.example.sweetrush.model.KartBodyStyle): String = when (style) {
    com.example.sweetrush.model.KartBodyStyle.STRAWBERRY_ROCKET -> "السيارة الرسمية (Made by Vanellope) • شوكولاتة وويفر • كريمة وردية ورشات سكر • أنابيب حلوى وجناح بسكويت"
    com.example.sweetrush.model.KartBodyStyle.PEPPERMINT_RACER -> "تافيتا (Pink Lightning) • هيكل حلوى وردية مصقولة • خطوط برق قصب السكر • أجنحة فورمولا"
    com.example.sweetrush.model.KartBodyStyle.CUPCAKE_KART -> "كاندل هيد (Ice Cream & Birthday Cake) • كعكة عيد ميلاد • 5 شموع مشتعلة باللهب • كريمة نعناع"
    com.example.sweetrush.model.KartBodyStyle.CHOCOLATE_CRUISER -> "رانسيس (Reese's Peanut Butter Cup) • قالب زبدة الفول السوداني المسنن • كراميل ذهبي وشوكولاتة"
    com.example.sweetrush.model.KartBodyStyle.CHERRY_PIE_KART -> "جوبيلينا (Cherry Pie Tart) • فطيرة كرز مشبكة • حشوة ياقوتية وكريمة مخفوقة • كرز مزدوج"
    com.example.sweetrush.model.KartBodyStyle.FRO_CONE_KART -> "سنوانا (Fro-Cone Snow Shaver) • مخروط ثلج مجروش بالتوت والبرتقال • شفرات جليد وأقراص ويفر"
    com.example.sweetrush.model.KartBodyStyle.TIRAMISSILE_KART -> "كرامبيلينا (Tiramissile Espresso) • طبقات تيراميسو وبسكويت ليدي فينغر • مدافع إسبريسو مزدوجة"
    com.example.sweetrush.model.KartBodyStyle.MARSHMALLOW_SPEEDSTER -> "أدورابيزل (Rocket Pop Missile) • صاروخ مثلجات ثلاثي الألوان (أحمر وأبيض وأزرق) • زعانف ثلجية"
    com.example.sweetrush.model.KartBodyStyle.DONUT_DRIFTER -> "غلويد (Pumpkin Candy Corn) • مخروط حلوى الذرة ثلاثي الألوان • زينة قرع الهالوين البرتقالية"
    com.example.sweetrush.model.KartBodyStyle.JELLYBEAN_MACHINE -> "سويزل (Tongue Twister) • صاروخ مصاصة حلزونية بـ 5 ألوان نيون • قضبان حلوى حامضة"
    com.example.sweetrush.model.KartBodyStyle.MINT_CANDY_KART -> "مينتي زاكي (Velvet Mint Drop) • كبسولة نعناع زمردية يابانية • دوامات ماتشا وورق ذهب"
    com.example.sweetrush.model.KartBodyStyle.CARAMEL_THUNDER -> "الملك كاندي (King Candy Royal Sovereign) • العربة الملكية المذهبة • تاج ورايات القصر"
}

internal fun racerCharacterLookAr(racerId: String): String = when (racerId) {
    "berry_blitz" -> "فانيلوبي فون شويتز • هودي نعناع أخضر وتنورة بنية • ذيل حصان مزين بالحلوى وخطوط السكر"
    "minty_swirl" -> "تافيتا ميتونفدج • خوذة فراولة وردية مع فيونكة • بدلة سباق التوفي الوردي ونظارات"
    "frosting_faye" -> "كاندل هيد • قبعة كب كيك نعناع مع شمعة عيد ميلاد مشتعلة فوق الرأس • سترة آيس كريم"
    "coco_dash" -> "رانسيس فلوغلبيرغر • قبعة زبدة الفول السوداني المقلوبة • سترة شوكولاتة وكراميل رسمية"
    "jubileena" -> "جوبيلينا بينغ-بينغ • قبعة فطيرة الكرز الحمراء مع ساق الكرز • سترة تارت الكرز"
    "snowanna" -> "سنوانا رينبو • تسريحة أفرو قوس قزح العملاقة المتوهجة • سترة ديسكو مثلجات الفواكه"
    "crumbelina" -> "كرامبيلينا دي كاراميلو • بيريه التيراميسو الإيطالي مع كريمة مخفوقة • وشاح إسبريسو"
    "mallow_max" -> "أدورابيزل وينتربوب • قبعة فرو شتوية بيضاء مع كرات ثلج • معطف صاروخ المثلجات الأزرق"
    "glazed_gus" -> "غلويد أورانجبور • قبعة حلوى الذرة المخروطية ثلاثية الألوان • سترة قرع الهالوين"
    "fizzy_pop" -> "سويزل ذا مالارك • خوذة مصاصة حلزونية بنظارات نيون • سترة سباق الحلوى الفوارة"
    "minty_zaki" -> "مينتي زاكي • فيونكة نعناع يابانية مع عود مصاصة أبيض • معطف النعناع الملكي الأخضر"
    "toffee_rex" -> "الملك كاندي • التاج الذهبي الإمبراطوري وربطة الفراشة الوردية • الرداء الملكي البنفسجي"
    "anime_goku" -> "سون غوكو (Dragon Ball) • شعر السوبر سايان الذهبي الشائك المتوهج • بدلة القتال البرتقالية والزرقاء"
    "anime_naruto" -> "ناروتو أوزوماكي (Naruto) • شعر أشقر شائك وعصابة كونوها المعدنية • علامات الخدود وسترة الهوكاجي"
    "anime_luffy" -> "مونكي دي لوفي (One Piece) • قبعة القش الصفراء بشريط أحمر • ندبة تحت العين وسترة القرصان الحمراء"
    "anime_gojo" -> "ساتورو غوجو (Jujutsu Kaisen) • شعر أبيض فضي مرفوع وعصابة العينين السوداء • عيون اللانهاية الزرقاء"
    "anime_tanjiro" -> "تانجيرو كامادو (Demon Slayer) • شعر عنابي وندبة تنفس الشمس • أقراط الهانافودا وهاوري الأخضر والأسود"
    "anime_nezuko" -> "نيزوكو كامادو (Demon Slayer) • شعر أسود بأطراف برتقالية نارية • خيزران الفم الأخضر وكيمونو الزهور الوردي"
    "anime_zoro" -> "رورونوا زورو (One Piece) • شعر أخضر زمردي قصير • ثلاثة أقراط ذهبية في الأذن اليسرى ورداء السياف"
    "anime_Deku" -> "إيزوكو ميدوريا ديكو (My Hero Academia) • شعر أخضر كثيف ونمش الخدود • قناع وبدلة ون فور أول الخضراء"
    "anime_anya" -> "آنيا فورجر (Spy x Family) • شعر وردي قصير وزينتا الرأس المخروطيتان السوداء والذهبية • زي إيدن"
    "anime_sailor" -> "سيلور مون (Sailor Moon) • ضفيرتا أودانغو الشقراء الطويلة • تاج الهلال الذهبي وفيونكة البحارة الحمراء"
    "anime_miku" -> "هاتسوني ميكو (Vocaloid) • ضفيرتان تركوازيتان طويلتان • سماعات ومشابك شعر نيون مستقبلية"
    "anime_killua" -> "كيلوا زولديك (Hunter x Hunter) • شعر فضي شائك متوهج بالبرق الأزرق (Godspeed) • عيون القاتل الزرقاء"
    else -> "شخصية ثلاثية الأبعاد عالية التفاصيل"
}

internal fun isAnimeRacer(racerId: String): Boolean = racerId.startsWith("anime_")

internal fun racerPortraitRes(racerId: String): Int = when (racerId) {
    "berry_blitz" -> R.drawable.img_racer_vanellope
    "minty_swirl" -> R.drawable.img_racer_taffyta
    "frosting_faye" -> R.drawable.img_racer_candlehead
    "coco_dash" -> R.drawable.img_racer_rancis
    "jubileena" -> R.drawable.img_racer_jubileena
    "snowanna" -> R.drawable.img_racer_snowanna
    "crumbelina" -> R.drawable.img_racer_crumbelina
    "mallow_max" -> R.drawable.img_racer_adorabeezle
    "glazed_gus" -> R.drawable.img_racer_gloyd
    "fizzy_pop" -> R.drawable.img_racer_swizzle
    "minty_zaki" -> R.drawable.img_racer_minty_zaki
    "toffee_rex" -> R.drawable.img_racer_king_candy
    else -> R.drawable.img_racer_vanellope
}

@Composable
internal fun RacerAvatarThumb(
    racerId: String,
    modifier: Modifier = Modifier
) {
    val spec = CatalogData.racerById(racerId)
    if (!isAnimeRacer(racerId)) {
        Image(
            painter = painterResource(id = racerPortraitRes(racerId)),
            contentDescription = spec.name,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Custom Vector Anime Character Portrait Avatar
        androidx.compose.foundation.Canvas(
            modifier = modifier.background(
                Brush.verticalGradient(
                    listOf(Color(spec.suitPrimaryHex), Color(spec.suitSecondaryHex))
                )
            )
        ) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.54f
            val skinCol = Color(spec.skinHex)
            val hairCol = Color(spec.helmetHex)
            val eyeCol = Color(spec.visorHex)
            val suitCol = Color(spec.suitPrimaryHex)

            // Back hair / aura
            drawCircle(
                color = hairCol.copy(alpha = 0.35f),
                radius = w * 0.44f,
                center = androidx.compose.ui.geometry.Offset(cx, cy - h * 0.04f)
            )
            // Shoulders / anime outfit collar
            drawOval(
                color = suitCol,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.72f),
                size = androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.36f)
            )
            // Anime face oval
            drawOval(
                color = skinCol,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.26f),
                size = androidx.compose.ui.geometry.Size(w * 0.50f, h * 0.52f)
            )
            // Large anime eyes + white catchlights
            for (side in listOf(-1f, 1f)) {
                val ex = cx + side * w * 0.11f
                val ey = cy + h * 0.01f
                drawCircle(Color.White, radius = w * 0.072f, center = androidx.compose.ui.geometry.Offset(ex, ey))
                drawCircle(eyeCol, radius = w * 0.052f, center = androidx.compose.ui.geometry.Offset(ex, ey))
                drawCircle(Color(0xFF140822), radius = w * 0.028f, center = androidx.compose.ui.geometry.Offset(ex, ey))
                drawCircle(
                    Color.White,
                    radius = w * 0.016f,
                    center = androidx.compose.ui.geometry.Offset(ex - side * w * 0.015f, ey - h * 0.018f)
                )
            }
            // Smile
            drawArc(
                color = Color(0xFFD81B60),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.08f, cy + h * 0.08f),
                size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.09f)
            )
            // Spiky / sculpted anime hair crown & signature headgear
            for (i in -3..3) {
                drawCircle(
                    color = hairCol,
                    radius = w * 0.11f,
                    center = androidx.compose.ui.geometry.Offset(
                        cx + i * w * 0.075f,
                        h * 0.23f + kotlin.math.abs(i) * h * 0.025f
                    )
                )
            }
            if (racerId == "anime_luffy") {
                // Straw hat red ribbon band
                drawRect(
                    color = Color(0xFFD50000),
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.22f),
                    size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.06f)
                )
            } else if (racerId == "anime_naruto") {
                // Konoha metal headband plate
                drawRect(
                    color = Color(0xFF1565C0),
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.26f),
                    size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.08f)
                )
            } else if (racerId == "anime_gojo") {
                // Gojo black blindfold
                drawRect(
                    color = Color(0xFF141622),
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.23f, cy - h * 0.05f),
                    size = androidx.compose.ui.geometry.Size(w * 0.54f, h * 0.11f)
                )
            } else if (racerId == "anime_nezuko") {
                // Nezuko green bamboo muzzle
                drawRect(
                    color = Color(0xFF2ECC71),
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.30f, cy + h * 0.09f),
                    size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.07f)
                )
            }
        }
    }
}

internal fun kartPreviewRes(kartId: String): Int = when (kartId) {
    "strawberry_rocket" -> R.drawable.img_official_candy_kart
    "peppermint_racer" -> R.drawable.img_kart_sugar_rush_car
    "cupcake_kart" -> R.drawable.img_kart_storybook_lineup
    "chocolate_cruiser" -> R.drawable.img_kart_sour_candy
    "cherry_pie_kart" -> R.drawable.img_kart_jubileena
    "fro_cone_kart" -> R.drawable.img_kart_snowanna
    "tiramissile_kart" -> R.drawable.img_kart_crumbelina
    "marshmallow_speedster" -> R.drawable.img_kart_storybook_lineup
    "donut_drifter" -> R.drawable.img_kart_sour_candy
    "jellybean_machine" -> R.drawable.img_kart_sugar_rush_car
    "mint_candy_kart" -> R.drawable.img_kart_storybook_lineup
    "caramel_thunder" -> R.drawable.img_kart_king_candy
    else -> R.drawable.img_official_candy_kart
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
    var activeCamPreset by remember(initialTab) { mutableStateOf(if (initialTab == 0) "DRIVER" else "AUTO") }
    // 0 = All (24), 1 = Sugar Rush Movie (12), 2 = Anime Legends (12)
    var characterFilter by remember { mutableIntStateOf(0) }

    val selectedKart = CatalogData.kartById(profile.selectedKartId)
    val selectedRacer = CatalogData.racerById(profile.selectedRacerId)
    val custom = profile.kartCustomizations[profile.selectedKartId] ?: KartCustomization()
    val activePaint = CatalogData.paintById(custom.paintId)
    val primaryColorHex = if (custom.paintId == "default") selectedKart.defaultPrimaryHex else activePaint.primaryHex
    val secondaryColorHex = if (custom.paintId == "default") selectedKart.defaultSecondaryHex else activePaint.secondaryHex
    val stats = CatalogData.effectiveStats(selectedKart, custom)
    val tuning = selectedKart.physicsTuning

    val filteredRacers = remember(characterFilter) {
        when (characterFilter) {
            1 -> CatalogData.racers.filter { !isAnimeRacer(it.id) }
            2 -> CatalogData.racers.filter { isAnimeRacer(it.id) }
            else -> CatalogData.racers
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CandyScreenHeader(
                title = if (activeTab == 0) "غاراج الشخصيات والأنمي (24 شخصية)" else "غاراج السيارات • Kart Garage",
                profile = profile,
                onBack = onBack
            )

            // Quick Horizontal Racer + Kart Switcher Strip inside the Garage (moved from Main Menu as requested!)
            LazyRow(
                modifier = Modifier
                    .testTag("garage_quick_racer_carousel")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(CatalogData.racers) { rItem ->
                    val isSel = rItem.id == profile.selectedRacerId
                    val rKart = CatalogData.kartById(rItem.favoriteKartId)
                    Row(
                        modifier = Modifier
                            .testTag("garage_racer_chip_${rItem.id}")
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSel) CandyPink.copy(alpha = 0.88f)
                                else BerryDeep.copy(alpha = 0.82f)
                            )
                            .border(
                                width = if (isSel) 2.dp else 1.dp,
                                color = if (isSel) CaramelGold else Color.White.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onSelectRacer(rItem.id)
                                onSelectKart(rItem.favoriteKartId)
                                activeCamPreset = "DRIVER"
                                onFocusCamera("DRIVER")
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RacerAvatarThumb(
                            racerId = rItem.id,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .border(1.dp, if (isSel) CaramelGold else Color(rItem.suitPrimaryHex), CircleShape)
                        )
                        Column {
                            Text(
                                text = rItem.name.substringBefore("•").trim().take(12),
                                style = MaterialTheme.typography.labelSmall,
                                color = FrostingWhite,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "#${rKart.racingNumber} ${rKart.name.substringBefore(" ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSel) MintCream else CaramelGold
                            )
                        }
                    }
                }
            }

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
                        .padding(bottom = 8.dp),
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
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (activeTab == 0) {
                                        RacerAvatarThumb(
                                            racerId = selectedRacer.id,
                                            modifier = Modifier
                                                .size(width = 48.dp, height = 38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.5.dp, CaramelGold, RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = kartPreviewRes(selectedKart.id)),
                                            contentDescription = "Selected Kart Reference",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 52.dp, height = 38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.5.dp, CaramelGold, RoundedCornerShape(10.dp))
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
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
                                            text = if (activeTab == 0) racerCharacterLookAr(selectedRacer.id) else kartBakeryRecipeAr(selectedKart.style),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CaramelGold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = onZoomIn,
                                    modifier = Modifier
                                        .testTag("garage_zoom_in")
                                        .size(44.dp)
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
                                        .size(44.dp)
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
                            "DRIVER" to "الشخصية 3D",
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
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
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
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val gripScore = ((stats.stability + (tuning.lateralGrip / 11f).coerceIn(0f, 1f)) * 0.5f).coerceIn(0f, 1f)
                        StatBarRow("TOP SPEED", stats.speed, CandyPink)
                        StatBarRow("ACCELERATION", stats.acceleration, MintCream)
                        StatBarRow("HANDLING", stats.handling, SkyCandy)
                        StatBarRow("GRIP", gripScore, Color(0xFFB388FF))
                        StatBarRow("BOOST", stats.boost, CaramelGold)
                    }
                }

                // Right side: Tabs + Sugar Rush & Anime Customization Panel
                Column(
                    modifier = Modifier
                        .weight(1.22f)
                        .fillMaxHeight()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(BerryDeep.copy(alpha = 0.90f))
                        .border(2.dp, CaramelGold.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                        .padding(10.dp)
                ) {
                    val tabs = listOf(
                        "الشخصيات (24)",
                        "السيارات (12)",
                        "التعديل • Paint",
                        "الترقية • Tune"
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
                                    .height(42.dp)
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
                                                activeCamPreset = "DRIVER"
                                                onFocusCamera("DRIVER")
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

                    Spacer(Modifier.height(6.dp))

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
                            // Filter chips: الكل (24) | شخصيات الفيلم (12) | نجوم الأنمي (12)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val filterOptions = listOf(
                                    0 to "الكل (24)",
                                    1 to "شخصيات الفيلم (12)",
                                    2 to "نجوم الأنمي (12)"
                                )
                                filterOptions.forEach { (fIdx, fLabel) ->
                                    val isFSel = characterFilter == fIdx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isFSel) CaramelGold else BerryPurple)
                                            .border(
                                                1.dp,
                                                if (isFSel) Color.White else Color.White.copy(alpha = 0.25f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { characterFilter = fIdx },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = fLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isFSel) BerryDeep else FrostingWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filteredRacers) { racer ->
                                    val unlocked = profile.unlockedRacers.contains(racer.id)
                                    val selected = profile.selectedRacerId == racer.id
                                    val sigKart = CatalogData.kartById(racer.favoriteKartId)
                                    val animeTag = isAnimeRacer(racer.id)
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
                                            .clickable {
                                                onSelectRacer(racer.id)
                                                onSelectKart(racer.favoriteKartId)
                                                activeCamPreset = "DRIVER"
                                                onFocusCamera("DRIVER")
                                            }
                                            .padding(9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RacerAvatarThumb(
                                            racerId = racer.id,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .border(2.dp, if (selected) CaramelGold else Color(racer.suitPrimaryHex), RoundedCornerShape(14.dp))
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = racer.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = FrostingWhite,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (animeTag) SkyCandy else CaramelGold)
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = if (animeTag) "ANIME ★" else "MOVIE ★",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = BerryDeep,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = racerCharacterLookAr(racer.id),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CaramelGold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "السيارة: #${sigKart.racingNumber} ${sigKart.name} • ${racer.signaturePowerUp.displayName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MintCream,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
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
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(BerryCard)
                                            .border(1.5.dp, CaramelGold.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_kart_storybook_lineup),
                                            contentDescription = "Sugar Rush 12 Movie Karts Lineup",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 110.dp, height = 52.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.5.dp, CaramelGold, RoundedCornerShape(10.dp))
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "أسطول سيارات فيلم Sugar Rush (12 سيارة) ★",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = CaramelGold
                                            )
                                            Text(
                                                text = "جميع سيارات المتسابقين الـ 12 من الفيلم مصممة 3D بالكامل وقابلة للتعديل",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MintCream,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                                items(CatalogData.karts) { kart ->
                                    val unlocked = profile.unlockedKarts.contains(kart.id)
                                    val selected = profile.selectedKartId == kart.id
                                    val isOfficialKart = kart.id == "strawberry_rocket"
                                    Row(
                                        modifier = Modifier
                                            .testTag("kart_item_${kart.id}")
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (selected) CandyPink.copy(alpha = 0.32f) else BerryPurple)
                                            .border(
                                                2.dp,
                                                if (selected) CaramelGold else if (isOfficialKart) MintCream.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.2f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable { onSelectKart(kart.id) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(contentAlignment = Alignment.BottomEnd) {
                                            Image(
                                                painter = painterResource(id = kartPreviewRes(kart.id)),
                                                contentDescription = kart.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(width = 72.dp, height = 50.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(
                                                        2.dp,
                                                        if (selected || isOfficialKart) CaramelGold else Color(kart.accentHex),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(BerryDeep.copy(alpha = 0.88f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "#${kart.racingNumber}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = CaramelGold,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(kart.name, style = MaterialTheme.typography.titleMedium, color = FrostingWhite)
                                                if (isOfficialKart) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .background(CaramelGold)
                                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "OFFICIAL ★ الرسمية",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = BerryDeep,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
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

