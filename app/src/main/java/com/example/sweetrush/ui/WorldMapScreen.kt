package com.example.sweetrush.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.sweetrush.model.PlayerProfile
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.model.WorldRegion
import com.example.sweetrush.render3d.RaceHudSnapshot
import com.example.sweetrush.track.OpenWorldKingdom
import com.example.ui.theme.BerryCard
import com.example.ui.theme.BerryDeep
import com.example.ui.theme.BerryPurple
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyPinkDark
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.FrostingWhite
import com.example.ui.theme.MintCream
import com.example.ui.theme.MintDark
import com.example.ui.theme.SkyCandy

private fun regionMovieMapRes(region: WorldRegion): Int = when (region) {
    WorldRegion.CANDY_CITY -> R.drawable.img_sugar_rush_world_map
    WorldRegion.CHOCOLATE_MOUNTAINS -> R.drawable.img_map_diet_cola_mountain
    WorldRegion.GUMDROP_FOREST -> R.drawable.img_map_candy_cane_forest
    WorldRegion.CUPCAKE_VALLEY -> R.drawable.img_map_cakeway
    WorldRegion.PEPPERMINT_COAST -> R.drawable.img_map_gumball_gorge
}

private fun regionMovieLoreAr(region: WorldRegion): String = when (region) {
    WorldRegion.CANDY_CITY -> "قلعة الملك كاندي وحلبة السباق الرئيسية • يحرسها وينتشل ودانكن وحراس الأوريو"
    WorldRegion.CHOCOLATE_MOUNTAINS -> "جبل الدايت كولا وينابيع المنثول الفوارة • مخبأ فانيلوبي السري داخل البركان"
    WorldRegion.GUMDROP_FOREST -> "غابة قصب السكر وأغصان النعناع المخططة • موطن ساور بيل ومختبر الأشجار"
    WorldRegion.CUPCAKE_VALLEY -> "وادي الكيك واي ومخبز عربات الحلوى (Kart Bakery) • تلال الكريمة والفراولة"
    WorldRegion.PEPPERMINT_COAST -> "مضيق الغامبول العملاق ومنحدرات كرات العلكة الملونة • جسور المصاصات"
}

@Composable
fun WorldMapScreen(
    profile: PlayerProfile,
    hud: RaceHudSnapshot,
    onFastTravelRegion: (WorldRegion) -> Unit,
    onStartRegionRace: (TrackId) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedRegion by remember { mutableStateOf(hud.currentRegion) }
    var showMovieBackdrop by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BerryDeep.copy(alpha = 0.86f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CandyScreenHeader(
                title = "Sugar Rush Kingdom Official World Map • خريطة عالم الفيلم",
                profile = profile,
                onBack = onBack
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left: Official Movie Published World Map + Interactive 2D GPS Overlay of all 5 connected regions & road splines
                Box(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF28113B))
                        .border(2.5.dp, CaramelGold.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                ) {
                    if (showMovieBackdrop) {
                        Image(
                            painter = painterResource(id = R.drawable.img_map_sugar_rush_kingdom),
                            contentDescription = "Published Sugar Rush Kingdom World Map",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.55f)
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        val extent = OpenWorldKingdom.WORLD_HALF_EXTENT
                        fun mapPt(wx: Float, wz: Float): Offset {
                            val nx = ((wx + extent) / (2f * extent)).coerceIn(0.05f, 0.95f)
                            val nz = (1f - (wz + extent) / (2f * extent)).coerceIn(0.05f, 0.95f)
                            return Offset(nx * size.width, nz * size.height)
                        }

                        // Biome quadrant color washes
                        drawCircle(Color(0xFF3CE8B5).copy(alpha = 0.16f), radius = size.minDimension * 0.28f, center = mapPt(-265f, -225f))
                        drawCircle(Color(0xFFFF9800).copy(alpha = 0.18f), radius = size.minDimension * 0.28f, center = mapPt(270f, -240f))
                        drawCircle(Color(0xFFD980FA).copy(alpha = 0.16f), radius = size.minDimension * 0.28f, center = mapPt(-255f, 235f))
                        drawCircle(Color(0xFF5CE1E6).copy(alpha = 0.18f), radius = size.minDimension * 0.28f, center = mapPt(260f, 240f))
                        drawCircle(Color(0xFFFF3D8B).copy(alpha = 0.22f), radius = size.minDimension * 0.18f, center = mapPt(0f, 0f))

                        // Connected Road Spline Network
                        for (road in OpenWorldKingdom.roads) {
                            if (road.points.size < 2) continue
                            val path = Path()
                            val p0 = mapPt(road.points[0].pos.x, road.points[0].pos.z)
                            path.moveTo(p0.x, p0.y)
                            for (i in 1 until road.points.size) {
                                val p = mapPt(road.points[i].pos.x, road.points[i].pos.z)
                                path.lineTo(p.x, p.y)
                            }
                            if (road.isClosedLoop) path.close()
                            drawPath(path, Color(road.stripeColorHex).copy(alpha = 0.88f), style = Stroke(width = 6.5f))
                        }

                        // Remaining Collectibles (Stars & Coins)
                        for (c in OpenWorldKingdom.collectibles) {
                            val collected = if (c.isStar) profile.collectedWorldStars.contains(c.id) else profile.collectedWorldCoins.contains(c.id)
                            if (collected) continue
                            val pt = mapPt(c.pos.x, c.pos.z)
                            drawCircle(
                                color = if (c.isStar) Color(0xFFFFEA00) else Color(0xFFFFB800).copy(alpha = 0.70f),
                                radius = if (c.isStar) 6.5f else 3f,
                                center = pt
                            )
                        }

                        // Region Landmarks
                        for (reg in WorldRegion.entries) {
                            val pt = mapPt(reg.centerX, reg.centerZ)
                            val isSel = reg == selectedRegion
                            drawCircle(
                                color = Color(reg.accentHex).copy(alpha = 0.90f),
                                radius = if (isSel) 17f else 11f,
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = if (isSel) 17f else 11f,
                                center = pt,
                                style = Stroke(width = if (isSel) 4f else 2f)
                            )
                        }

                        // Central Candy City Hub / Garage Badge (0, 0)
                        val hubPt = mapPt(0f, 0f)
                        val hubSize = 11f
                        val hubPath = Path().apply {
                            moveTo(hubPt.x, hubPt.y - hubSize)
                            lineTo(hubPt.x + hubSize, hubPt.y - hubSize * 0.2f)
                            lineTo(hubPt.x + hubSize * 0.7f, hubPt.y + hubSize)
                            lineTo(hubPt.x - hubSize * 0.7f, hubPt.y + hubSize)
                            lineTo(hubPt.x - hubSize, hubPt.y - hubSize * 0.2f)
                            close()
                        }
                        drawPath(hubPath, CandyPink)
                        drawPath(hubPath, Color.White, style = Stroke(width = 2.5f))

                        // 5 Race Portal Diamond Markers
                        for (portal in OpenWorldKingdom.racePortals) {
                            val pt = mapPt(portal.pos.x, portal.pos.z)
                            val d = 9.5f
                            val diamond = Path().apply {
                                moveTo(pt.x, pt.y - d)
                                lineTo(pt.x + d, pt.y)
                                lineTo(pt.x, pt.y + d)
                                lineTo(pt.x - d, pt.y)
                                close()
                            }
                            drawPath(diamond, Color(portal.accentHex))
                            drawPath(diamond, Color.White, style = Stroke(width = 2.2f))
                        }

                        // 6 Mini-Challenge Amber Beacon Triangles
                        for (ch in OpenWorldKingdom.miniChallenges) {
                            val pt = mapPt(ch.pos.x, ch.pos.z)
                            val h = 8.5f
                            val tri = Path().apply {
                                moveTo(pt.x, pt.y - h)
                                lineTo(pt.x + h * 0.88f, pt.y + h * 0.7f)
                                lineTo(pt.x - h * 0.88f, pt.y + h * 0.7f)
                                close()
                            }
                            drawPath(tri, Color(0xFFFFB300))
                            drawPath(tri, Color.White, style = Stroke(width = 2f))
                        }

                        // Current Player Position Arrow Marker
                        val pPt = mapPt(hud.playerWorldX, hud.playerWorldZ)
                        val playerArrow = Path().apply {
                            moveTo(pPt.x, pPt.y - 11f)
                            lineTo(pPt.x + 8.5f, pPt.y + 8f)
                            lineTo(pPt.x, pPt.y + 4f)
                            lineTo(pPt.x - 8.5f, pPt.y + 8f)
                            close()
                        }
                        drawPath(playerArrow, CaramelGold)
                        drawPath(playerArrow, Color.White, style = Stroke(width = 2.5f))
                    }

                    // Top-left toggle for official movie world map backdrop + Citizens badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag("toggle_movie_map_layer")
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (showMovieBackdrop) CaramelGold else BerryDeep.copy(alpha = 0.85f))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { showMovieBackdrop = !showMovieBackdrop }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (showMovieBackdrop) "خريطة الفيلم: مفعّلة ★" else "خريطة الفيلم: مخفية",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (showMovieBackdrop) BerryDeep else FrostingWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Movie Citizens Live Strip (Sour Bill + Wynnchel & Duncan)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(BerryDeep.copy(alpha = 0.84f))
                                .border(1.dp, MintCream.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_citizen_sour_bill),
                                contentDescription = "Sour Bill",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MintCream, CircleShape)
                            )
                            Image(
                                painter = painterResource(id = R.drawable.img_citizen_wynnchel_duncan),
                                contentDescription = "Wynnchel & Duncan Donut Police",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, CandyPink, CircleShape)
                            )
                            Text(
                                text = "مواطنو الفيلم (${OpenWorldKingdom.citizens.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MintCream
                            )
                        }
                    }

                    // Bottom-left Map Legend (Markers + Live Counts)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BerryDeep.copy(alpha = 0.86f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("◆ 5 Portals", style = MaterialTheme.typography.labelSmall, color = SkyCandy)
                            Text("▲ ${OpenWorldKingdom.miniChallenges.size} Challenges", style = MaterialTheme.typography.labelSmall, color = CaramelGold)
                            Text("⬟ Castle Hub", style = MaterialTheme.typography.labelSmall, color = CandyPink)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Coins: ${profile.collectedWorldCoins.size}/48", style = MaterialTheme.typography.labelSmall, color = CaramelGold)
                            Text("Stars: ${profile.collectedWorldStars.size}/10", style = MaterialTheme.typography.labelSmall, color = MintCream)
                            Text("Regions: ${profile.discoveredRegions.size}/5", style = MaterialTheme.typography.labelSmall, color = SkyCandy)
                        }
                    }
                }

                // Right: Selected Movie Landmark Vista Card + 5 Region Cards + Fast Travel & Region Race Launch
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(BerryPurple.copy(alpha = 0.92f))
                        .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Authentic Movie Vista Preview Banner for the selected region
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BerryDeep.copy(alpha = 0.88f))
                            .border(1.5.dp, CaramelGold.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = regionMovieMapRes(selectedRegion)),
                            contentDescription = selectedRegion.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 86.dp, height = 56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, Color(selectedRegion.accentHex), RoundedCornerShape(12.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedRegion.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = CaramelGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = regionMovieLoreAr(selectedRegion),
                                style = MaterialTheme.typography.labelSmall,
                                color = FrostingWhite.copy(alpha = 0.9f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(WorldRegion.entries) { region ->
                            val isSel = region == selectedRegion
                            val discovered = profile.discoveredRegions.contains(region)
                            Row(
                                modifier = Modifier
                                    .testTag("region_card_${region.name.lowercase()}")
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSel) Color(region.accentHex).copy(alpha = 0.28f) else BerryCard)
                                    .border(
                                        2.dp,
                                        if (isSel) CaramelGold else Color.White.copy(alpha = 0.22f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedRegion = region }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Image(
                                    painter = painterResource(id = regionMovieMapRes(region)),
                                    contentDescription = region.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(region.accentHex), RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(region.displayName, style = MaterialTheme.typography.titleMedium, color = FrostingWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(region.subtitle, style = MaterialTheme.typography.labelSmall, color = Color(region.accentHex), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(
                                    text = if (discovered) "EXPLORED" else "NEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (discovered) MintCream else CaramelGold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val portalTrack = when (selectedRegion) {
                        WorldRegion.CANDY_CITY -> TrackId.PEPPERMINT_PLAZA
                        WorldRegion.CHOCOLATE_MOUNTAINS -> TrackId.CHOCOLATE_VOLCANO
                        WorldRegion.GUMDROP_FOREST -> TrackId.GUMDROP_GARDEN
                        WorldRegion.CUPCAKE_VALLEY, WorldRegion.PEPPERMINT_COAST -> TrackId.CUPCAKE_COAST
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CandyButton(
                            text = "FREE ROAM HERE",
                            onClick = { onFastTravelRegion(selectedRegion) },
                            testTag = "world_map_free_roam_btn",
                            icon = Icons.Filled.Explore,
                            topColor = MintCream,
                            bottomColor = MintDark,
                            textColor = BerryDeep,
                            height = 46.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        CandyButton(
                            text = "RACE REGION CIRCUIT",
                            onClick = { onStartRegionRace(portalTrack) },
                            testTag = "world_map_race_btn",
                            icon = Icons.Filled.Flag,
                            topColor = CandyPink,
                            bottomColor = CandyPinkDark,
                            height = 46.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
