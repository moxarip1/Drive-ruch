package com.example.sweetrush.track

import com.example.sweetrush.model.TrackId
import com.example.sweetrush.model.WorldRegion
import com.example.sweetrush.render3d.CandyMaterials
import com.example.sweetrush.render3d.GpuMesh
import com.example.sweetrush.render3d.MeshAccumulator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class RoadSegmentPoint(
    val pos: Vec3,
    val forward: Vec3,
    val right: Vec3,
    val halfWidth: Float,
    val isBridge: Boolean = false
)

data class OpenWorldRoad(
    val id: String,
    val name: String,
    val points: List<RoadSegmentPoint>,
    val isClosedLoop: Boolean,
    val roadColorHex: Long,
    val stripeColorHex: Long
)

data class WorldCollectible(
    val id: String,
    val isStar: Boolean,
    val pos: Vec3,
    val region: WorldRegion
)

data class WorldRacePortal(
    val id: String,
    val title: String,
    val trackId: TrackId,
    val region: WorldRegion,
    val pos: Vec3,
    val yawRad: Float,
    val accentHex: Long
)

data class WorldMiniChallenge(
    val id: String,
    val title: String,
    val description: String,
    val region: WorldRegion,
    val pos: Vec3,
    val radius: Float,
    val targetSpeedKmh: Int = 0
)

data class WorldCompanyDepot(
    val id: String,
    val nameAr: String,
    val branchTitleAr: String,
    val managerNameAr: String,
    val region: WorldRegion,
    val padPos: Vec3,
    val buildingPos: Vec3,
    val buildingYawRad: Float,
    val accentHex: Long,
    val isHeadquarters: Boolean = false
)

data class WorldCompanyContract(
    val id: String,
    val titleAr: String,
    val cargoNameAr: String,
    val pickupDepotId: String,
    val deliveryDepotId: String,
    val baseRewardCredits: Int,
    val rewardXp: Int
)

data class WorldCitizenNpc(
    val id: String,
    val nameAr: String,
    val roleAr: String,
    val variant: CitizenVariant,
    val region: WorldRegion,
    val homePos: Vec3,
    val walkRadius: Float,
    val walkSpeed: Float,
    val phaseOffset: Float,
    val primaryHex: Long,
    val secondaryHex: Long,
    val hatHex: Long,
    val greetingAr: String
)

data class CitizenAnimatedPose(
    val x: Float,
    val y: Float,
    val z: Float,
    val yawRad: Float,
    val bobY: Float,
    val tiltRollRad: Float,
    val isWaving: Boolean,
    val distSqToPlayer: Float
)

data class TerrainChunkData(
    val cx: Int,
    val cz: Int,
    val centerX: Float,
    val centerZ: Float,
    val halfSpan: Float,
    val buildMesh: () -> GpuMesh
)

/**
 * Connected 1100m x 1100m Open-World Candy Kingdom linking 5 seamless regions:
 *   - Candy City (Center)
 *   - Chocolate Mountains (NE)
 *   - Gumdrop Forest (NW)
 *   - Cupcake Valley (SW)
 *   - Peppermint Coast (SE)
 */
object OpenWorldKingdom : WorldSurfaceSampler {

    const val WORLD_HALF_EXTENT = 520f
    const val CHUNK_GRID = 6 // 6x6 = 36 terrain chunks for efficient mobile culling

    val roads: List<OpenWorldRoad>
    val ringRoad: OpenWorldRoad
    val boostPads: List<BoostPad>
    val jumpRamps: List<JumpRamp>
    val collectibles: List<WorldCollectible>
    val racePortals: List<WorldRacePortal>
    val miniChallenges: List<WorldMiniChallenge>
    val companyDepots: List<WorldCompanyDepot>
    val companyContracts: List<WorldCompanyContract>
    val citizens: List<WorldCitizenNpc>
    val decorations: List<DecorationInstance>
    val solidObstacles: List<SolidObstacle>
    val obstacleGrid: SpatialObstacleGrid

    init {
        roads = buildRoadNetwork()
        ringRoad = roads.first()
        boostPads = buildWorldBoostPads()
        jumpRamps = buildWorldRamps()
        collectibles = buildCollectibles()
        racePortals = buildRacePortals()
        miniChallenges = buildMiniChallenges()
        companyDepots = buildCompanyDepots()
        companyContracts = buildCompanyContracts()
        decorations = buildWorldDecorations()
        citizens = buildWorldCitizens()
        solidObstacles = decorations.mapNotNull { d ->
            val r = solidCollisionRadius(d.type, d.scale)
            if (r > 0f) SolidObstacle(d.pos.x, d.pos.y, d.pos.z, r, solidHeightFor(d.type, d.scale), isBuildingDeco(d.type)) else null
        }
        obstacleGrid = SpatialObstacleGrid(solidObstacles, cellSize = 24f)
    }

    fun companyDepotById(id: String): WorldCompanyDepot =
        companyDepots.firstOrNull { it.id == id } ?: companyDepots.first()

    fun companyRankTitleAr(level: Int): String = when {
        level <= 1 -> "متدرب في شركة سويت راش"
        level == 2 -> "سائق شحن الحلويات"
        level == 3 -> "مشرف خطوط التوزيع"
        level == 4 -> "مدير فرع إقليمي"
        level == 5 -> "مدير العمليات الملكية"
        level == 6 -> "نائب رئيس الشركة"
        else -> "رئيس مجلس شركة سويت راش 👑"
    }

    fun companyUpgradeCost(currentLevel: Int): Int = 220 + (currentLevel - 1) * 160

    fun companyPassiveDividendForLevel(level: Int): Int = 45 + (level - 1) * 30

    fun sampleCitizenPose(
        npc: WorldCitizenNpc,
        timeSec: Float,
        playerX: Float = 9999f,
        playerZ: Float = 9999f
    ): CitizenAnimatedPose {
        val t = timeSec * npc.walkSpeed + npc.phaseOffset
        val localX = cos(t) * npc.walkRadius
        val localZ = sin(t * 0.85f) * (npc.walkRadius * 0.68f)
        val wx = npc.homePos.x + localX
        val wz = npc.homePos.z + localZ
        val wy = sampleHeightOnly(wx, wz)

        val dxToPlayer = playerX - wx
        val dzToPlayer = playerZ - wz
        val distSq = dxToPlayer * dxToPlayer + dzToPlayer * dzToPlayer
        val nearPlayer = distSq < 22f * 22f

        val velX = -sin(t) * npc.walkRadius * npc.walkSpeed
        val velZ = cos(t * 0.85f) * (npc.walkRadius * 0.68f) * (npc.walkSpeed * 0.85f)
        val walkYaw = atan2(velX, velZ)
        val facePlayerYaw = atan2(dxToPlayer, dzToPlayer)
        val yaw = if (nearPlayer) facePlayerYaw else walkYaw

        val stridePhase = timeSec * (if (nearPlayer) 7.5f else 5.8f) + npc.phaseOffset * 3f
        val bobY = abs(sin(stridePhase)) * 0.14f
        val tiltRoll = sin(stridePhase) * (if (nearPlayer) 0.14f else 0.08f)

        return CitizenAnimatedPose(
            x = wx,
            y = wy,
            z = wz,
            yawRad = yaw,
            bobY = bobY,
            tiltRollRad = tiltRoll,
            isWaving = nearPlayer,
            distSqToPlayer = distSq
        )
    }

    fun regionAt(x: Float, z: Float): WorldRegion {
        val distCenter = sqrt(x * x + z * z)
        if (distCenter < 145f) return WorldRegion.CANDY_CITY
        return when {
            x >= 0f && z < 0f -> WorldRegion.CHOCOLATE_MOUNTAINS
            x < 0f && z < 0f -> WorldRegion.GUMDROP_FOREST
            x < 0f && z >= 0f -> WorldRegion.CUPCAKE_VALLEY
            else -> WorldRegion.PEPPERMINT_COAST
        }
    }

    fun rawTerrainHeight(x: Float, z: Float): Float {
        // Gentle rolling hills across Candy World
        var h = sin(x * 0.015f) * cos(z * 0.015f) * 2.2f +
            sin((x + z) * 0.008f) * 1.4f

        // Flat central Candy City plaza radius 115m
        val rCenter = sqrt(x * x + z * z)
        val cityFlatten = smoothStep(75f, 145f, rCenter)
        h *= cityFlatten

        // Chocolate Mountains NE rising ridge (peaks ~22m outside roads)
        val dxM = x - 310f
        val dzM = z + 290f
        val dM = sqrt(dxM * dxM + dzM * dzM)
        val mountainDome = max(0f, 1f - dM / 220f)
        h += mountainDome * mountainDome * 22f

        // Gumdrop Forest NW gentle hills
        val dxF = x + 290f
        val dzF = z + 260f
        val dF = sqrt(dxF * dxF + dzF * dzF)
        h += max(0f, 1f - dF / 190f) * 4.5f

        // Cupcake Valley SW playful frosting mounds
        val dxV = x + 280f
        val dzV = z - 260f
        val dV = sqrt(dxV * dxV + dzV * dzV)
        h += max(0f, 1f - dV / 200f) * (3.5f + sin(x * 0.05f) * 1.6f)

        // Peppermint Coast SE slopes down to -0.8m Strawberry Cream Ocean beyond radius 320m SE
        if (x > 120f && z > 120f) {
            val coastDist = sqrt((x - 120f) * (x - 120f) + (z - 120f) * (z - 120f))
            val oceanDip = smoothStep(150f, 290f, coastDist)
            h = h * (1f - oceanDip) - oceanDip * 1.15f
        }

        // River channel flowing from Chocolate Mountains through Valley edge
        val riverDist = abs(z - (60f + sin(x * 0.012f) * 45f))
        if (x in 135f..420f && riverDist < 16f) {
            val depth = 1f - riverDist / 16f
            h -= depth * 1.9f
        }

        return h
    }

    override fun sampleSurface(x: Float, z: Float, hintY: Float): SurfaceSample {
        val region = regionAt(x, z)
        val baseH = rawTerrainHeight(x, z)

        // Check nearest road spline across network
        var bestRoadDistSq = Float.MAX_VALUE
        var bestRoadPt: RoadSegmentPoint? = null
        for (road in roads) {
            for (pt in road.points) {
                val dx = x - pt.pos.x
                val dz = z - pt.pos.z
                val d2 = dx * dx + dz * dz
                if (d2 < bestRoadDistSq) {
                    bestRoadDistSq = d2
                    bestRoadPt = pt
                }
            }
        }

        var finalH = baseH
        var surf = if (baseH < -0.55f) SurfaceType.WATER else SurfaceType.OFFROAD

        val rp = bestRoadPt
        if (rp != null) {
            val dist = sqrt(bestRoadDistSq)
            if (dist <= rp.halfWidth) {
                finalH = rp.pos.y
                surf = if (rp.isBridge) SurfaceType.BRIDGE else SurfaceType.ROAD
            } else if (!rp.isBridge && dist < rp.halfWidth + 14f) {
                // Smoothly blend roadside shoulder into terrain heightfield
                val t = smoothStep(rp.halfWidth, rp.halfWidth + 14f, dist)
                finalH = rp.pos.y * (1f - t) + baseH * t
            }
        }

        // Check jump ramps for elevated ramp surface
        for (ramp in jumpRamps) {
            val dx = x - ramp.pos.x
            val dz = z - ramp.pos.z
            if (dx * dx + dz * dz < 14f) {
                finalH = max(finalH, ramp.pos.y + 0.9f)
                surf = SurfaceType.RAMP
            }
        }

        // Approximate surface normal via finite difference
        val eps = 1.6f
        val hx = sampleHeightOnly(x + eps, z) - sampleHeightOnly(x - eps, z)
        val hz = sampleHeightOnly(x, z + eps) - sampleHeightOnly(x, z - eps)
        val normal = Vec3(-hx / (2f * eps), 1f, -hz / (2f * eps)).normalized()

        return SurfaceSample(
            height = finalH,
            normal = normal,
            surface = surf,
            region = region
        )
    }

    private fun sampleHeightOnly(x: Float, z: Float): Float {
        val baseH = rawTerrainHeight(x, z)
        var bestDistSq = Float.MAX_VALUE
        var bestPt: RoadSegmentPoint? = null
        for (road in roads) {
            for (pt in road.points) {
                val dx = x - pt.pos.x
                val dz = z - pt.pos.z
                val d2 = dx * dx + dz * dz
                if (d2 < bestDistSq) {
                    bestDistSq = d2
                    bestPt = pt
                }
            }
        }
        val rp = bestPt ?: return baseH
        val d = sqrt(bestDistSq)
        if (d <= rp.halfWidth) return rp.pos.y
        if (!rp.isBridge && d < rp.halfWidth + 14f) {
            val t = smoothStep(rp.halfWidth, rp.halfWidth + 14f, d)
            return rp.pos.y * (1f - t) + baseH * t
        }
        return baseH
    }

    fun getTerrainChunks(): List<TerrainChunkData> {
        val chunks = ArrayList<TerrainChunkData>(CHUNK_GRID * CHUNK_GRID)
        val span = (WORLD_HALF_EXTENT * 2f) / CHUNK_GRID
        val halfSpan = span * 0.5f
        for (ix in 0 until CHUNK_GRID) {
            for (iz in 0 until CHUNK_GRID) {
                val x0 = -WORLD_HALF_EXTENT + ix * span
                val z0 = -WORLD_HALF_EXTENT + iz * span
                val cx = x0 + halfSpan
                val cz = z0 + halfSpan
                chunks.add(
                    TerrainChunkData(
                        cx = ix,
                        cz = iz,
                        centerX = cx,
                        centerZ = cz,
                        halfSpan = halfSpan,
                        buildMesh = { buildTerrainChunkMesh(x0, z0, span, cells = 12) }
                    )
                )
            }
        }
        return chunks
    }

    private fun buildTerrainChunkMesh(x0: Float, z0: Float, span: Float, cells: Int): GpuMesh {
        val acc = MeshAccumulator()
        val step = span / cells
        for (ix in 0 until cells) {
            for (iz in 0 until cells) {
                val px0 = x0 + ix * step
                val px1 = px0 + step
                val pz0 = z0 + iz * step
                val pz1 = pz0 + step

                val y00 = sampleHeightOnly(px0, pz0)
                val y10 = sampleHeightOnly(px1, pz0)
                val y11 = sampleHeightOnly(px1, pz1)
                val y01 = sampleHeightOnly(px0, pz1)

                val mx = (px0 + px1) * 0.5f
                val mz = (pz0 + pz1) * 0.5f
                val my = (y00 + y10 + y11 + y01) * 0.25f
                val region = regionAt(mx, mz)
                val shade = if ((ix + iz) % 2 == 0) 1.0f else 0.94f

                val baseHex = when {
                    my < -0.35f -> 0xFFFFB5D0L // Strawberry cream sand near shore
                    region == WorldRegion.CANDY_CITY -> 0xFF72E8BA
                    region == WorldRegion.CHOCOLATE_MOUNTAINS -> if (my > 9f) 0xFF4B2418 else 0xFF6B3A26
                    region == WorldRegion.GUMDROP_FOREST -> 0xFF7BE495
                    region == WorldRegion.CUPCAKE_VALLEY -> 0xFFFFB8DE
                    region == WorldRegion.PEPPERMINT_COAST -> 0xFFFFE0B2
                    else -> 0xFF72E8BA
                }
                val (r, g, b) = MeshAccumulator.hexToRgb(baseHex)
                acc.addTri(px0, y01, pz1, px1, y11, pz1, px1, y10, pz0, r * shade, g * shade, b * shade)
                acc.addTri(px0, y01, pz1, px1, y10, pz0, px0, y00, pz0, r * shade, g * shade, b * shade)
            }
        }
        return acc.build()
    }

    fun buildRoadNetworkMesh(): GpuMesh {
        val acc = MeshAccumulator()
        for (road in roads) {
            val (rr, rg, rb) = MeshAccumulator.hexToRgb(road.roadColorHex)
            val (sr, sg, sb) = MeshAccumulator.hexToRgb(road.stripeColorHex)
            val n = road.points.size
            val limit = if (road.isClosedLoop) n else n - 1
            for (i in 0 until limit) {
                val p0 = road.points[i]
                val p1 = road.points[(i + 1) % n]
                val yOff = 0.06f
                val l0 = p0.pos - p0.right * p0.halfWidth
                val r0 = p0.pos + p0.right * p0.halfWidth
                val l1 = p1.pos - p1.right * p1.halfWidth
                val r1 = p1.pos + p1.right * p1.halfWidth

                val mL0 = p0.pos - p0.right * (p0.halfWidth * 0.12f)
                val mR0 = p0.pos + p0.right * (p0.halfWidth * 0.12f)
                val mL1 = p1.pos - p1.right * (p1.halfWidth * 0.12f)
                val mR1 = p1.pos + p1.right * (p1.halfWidth * 0.12f)

                val shade = if (i % 2 == 0) 1.0f else 0.94f
                acc.addQuad(l0.x, l0.y + yOff, l0.z, mL0.x, mL0.y + yOff, mL0.z, mL1.x, mL1.y + yOff, mL1.z, l1.x, l1.y + yOff, l1.z, rr * shade, rg * shade, rb * shade, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_ROAD)
                acc.addQuad(mR0.x, mR0.y + yOff, mR0.z, r0.x, r0.y + yOff, r0.z, r1.x, r1.y + yOff, r1.z, mR1.x, mR1.y + yOff, mR1.z, rr * shade, rg * shade, rb * shade, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_ROAD)

                val stripeAlt = if ((i / 2) % 2 == 0) 1f else 0.72f
                acc.addQuad(mL0.x, mL0.y + yOff + 0.02f, mL0.z, mR0.x, mR0.y + yOff + 0.02f, mR0.z, mR1.x, mR1.y + yOff + 0.02f, mR1.z, mL1.x, mL1.y + yOff + 0.02f, mL1.z, sr * stripeAlt, sg * stripeAlt, sb * stripeAlt, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_CANDY)

                // crisp glowing sugar lane-edge trim strips
                val leIn0 = p0.pos - p0.right * (p0.halfWidth * 0.86f)
                val leOut0 = p0.pos - p0.right * (p0.halfWidth * 0.92f)
                val leIn1 = p1.pos - p1.right * (p1.halfWidth * 0.86f)
                val leOut1 = p1.pos - p1.right * (p1.halfWidth * 0.92f)
                acc.addQuad(leOut0.x, leOut0.y + yOff + 0.015f, leOut0.z, leIn0.x, leIn0.y + yOff + 0.015f, leIn0.z, leIn1.x, leIn1.y + yOff + 0.015f, leIn1.z, leOut1.x, leOut1.y + yOff + 0.015f, leOut1.z, 1f, 0.95f, 1f, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_EMISSIVE)

                val reIn0 = p0.pos + p0.right * (p0.halfWidth * 0.86f)
                val reOut0 = p0.pos + p0.right * (p0.halfWidth * 0.92f)
                val reIn1 = p1.pos + p1.right * (p1.halfWidth * 0.86f)
                val reOut1 = p1.pos + p1.right * (p1.halfWidth * 0.92f)
                acc.addQuad(reIn0.x, reIn0.y + yOff + 0.015f, reIn0.z, reOut0.x, reOut0.y + yOff + 0.015f, reOut0.z, reOut1.x, reOut1.y + yOff + 0.015f, reOut1.z, reIn1.x, reIn1.y + yOff + 0.015f, reIn1.z, 1f, 0.95f, 1f, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_EMISSIVE)

                // Raised peppermint curbs & pastel wafer sidewalks
                val curbCol = if (i % 2 == 0) 0xFFFF2E63L else 0xFFFFFFFFL
                val (cr, cg, cb) = MeshAccumulator.hexToRgb(curbCol)
                val ch = if (p0.isBridge) 0.78f else 0.40f
                val cw = 1.10f
                val lo0 = l0 - p0.right * cw
                val lo1 = l1 - p1.right * cw
                val ro0 = r0 + p0.right * cw
                val ro1 = r1 + p1.right * cw
                acc.addQuad(l0.x, l0.y + ch, l0.z, l0.x, l0.y, l0.z, l1.x, l1.y, l1.z, l1.x, l1.y + ch, l1.z, cr, cg, cb)
                acc.addQuad(lo0.x, lo0.y + ch, lo0.z, l0.x, l0.y + ch, l0.z, l1.x, l1.y + ch, l1.z, lo1.x, lo1.y + ch, lo1.z, cr, cg, cb)
                acc.addQuad(r0.x, r0.y, r0.z, r0.x, r0.y + ch, r0.z, r1.x, r1.y + ch, r1.z, r1.x, r1.y, r1.z, cr, cg, cb)
                acc.addQuad(r0.x, r0.y + ch, r0.z, ro0.x, ro0.y + ch, ro0.z, ro1.x, ro1.y + ch, ro1.z, r1.x, r1.y + ch, r1.z, cr, cg, cb)

                if (!p0.isBridge && !p1.isBridge) {
                    val sw = 2.6f
                    val lsw0 = lo0 - p0.right * sw
                    val lsw1 = lo1 - p1.right * sw
                    val rsw0 = ro0 + p0.right * sw
                    val rsw1 = ro1 + p1.right * sw
                    val swTone = if (i % 2 == 0) 1.0f else 0.93f
                    acc.addQuad(lsw0.x, lsw0.y + 0.16f, lsw0.z, lo0.x, lo0.y + 0.16f, lo0.z, lo1.x, lo1.y + 0.16f, lo1.z, lsw1.x, lsw1.y + 0.16f, lsw1.z, 0.98f * swTone, 0.86f * swTone, 0.94f * swTone, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_WAFER)
                    acc.addQuad(ro0.x, ro0.y + 0.16f, ro0.z, rsw0.x, rsw0.y + 0.16f, rsw0.z, rsw1.x, rsw1.y + 0.16f, rsw1.z, ro1.x, ro1.y + 0.16f, ro1.z, 0.98f * swTone, 0.86f * swTone, 0.94f * swTone, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_WAFER)
                }

                if (p0.isBridge || p1.isBridge) {
                    acc.addQuad(lo0.x, lo0.y + ch, lo0.z, lo1.x, lo1.y + ch, lo1.z, lo1.x, -2.5f, lo1.z, lo0.x, -2.5f, lo0.z, 0.82f, 0.56f, 0.34f)
                    acc.addQuad(ro1.x, ro1.y + ch, ro1.z, ro0.x, ro0.y + ch, ro0.z, ro0.x, -2.5f, ro0.z, ro1.x, -2.5f, ro1.z, 0.82f, 0.56f, 0.34f)
                }
            }
        }

        // Central Candy City Grand Plaza roundabout paving + luminous mosaic rings
        val plazaR = 38f
        val segs = 32
        val (pr, pg, pb) = MeshAccumulator.hexToRgb(0xFF472D5C)
        val (ar, ag, ab) = MeshAccumulator.hexToRgb(0xFFFF4D8D)
        val (mr, mg, mb) = MeshAccumulator.hexToRgb(0xFF3CE8B5)
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
            val x0 = cos(a0) * plazaR; val z0 = sin(a0) * plazaR
            val x1 = cos(a1) * plazaR; val z1 = sin(a1) * plazaR
            acc.addTri(0f, 0.08f, 0f, x1, 0.08f, z1, x0, 0.08f, z0, pr, pg, pb, 1f, 0f, 0f, 1f, 0f, 0.5f, 1f, CandyMaterials.MAT_ROAD)
            if (i % 2 == 0) {
                acc.addQuad(
                    cos(a0) * 14f, 0.10f, sin(a0) * 14f,
                    cos(a1) * 14f, 0.10f, sin(a1) * 14f,
                    cos(a1) * 17f, 0.10f, sin(a1) * 17f,
                    cos(a0) * 17f, 0.10f, sin(a0) * 17f,
                    ar, ag, ab, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_EMISSIVE
                )
            } else {
                acc.addQuad(
                    cos(a0) * 28f, 0.10f, sin(a0) * 28f,
                    cos(a1) * 28f, 0.10f, sin(a1) * 28f,
                    cos(a1) * 30f, 0.10f, sin(a1) * 30f,
                    cos(a0) * 30f, 0.10f, sin(a0) * 30f,
                    mr, mg, mb, 1f, 0f, 0f, 1f, 1f, CandyMaterials.MAT_EMISSIVE
                )
            }
        }

        return acc.build()
    }

    fun buildLiquidWaterMesh(): GpuMesh {
        val acc = MeshAccumulator()
        // Strawberry cream coastal sea (SE quadrant) + chocolate river channel
        val (sr, sg, sb) = MeshAccumulator.hexToRgb(0xFFFF82B8)
        acc.addQuad(160f, -0.55f, 520f, 520f, -0.55f, 520f, 520f, -0.55f, 150f, 160f, -0.55f, 150f, sr, sg, sb, 0.86f, 0f, 0f, 8f, 8f, CandyMaterials.MAT_LIQUID)
        val (cr, cg, cb) = MeshAccumulator.hexToRgb(0xFF7B3F1D)
        acc.addQuad(135f, -0.45f, 98f, 420f, -0.45f, 98f, 420f, -0.45f, 22f, 135f, -0.45f, 22f, cr, cg, cb, 0.92f, 0f, 0f, 6f, 3f, CandyMaterials.MAT_LIQUID)
        return acc.build()
    }

    private fun buildRoadNetwork(): List<OpenWorldRoad> {
        val list = mutableListOf<OpenWorldRoad>()

        // 1. Grand Candy Ring Highway (connects all 4 outer regions seamlessly!)
        val ringCtrl = listOf(
            Vec3(0f, 0.2f, -245f),      // North gate
            Vec3(195f, 6.5f, -255f),    // Entering Chocolate Mountains
            Vec3(290f, 11.5f, -155f),   // Mountain overlook switchback
            Vec3(270f, 4.5f, 60f),      // Bridge across Chocolate River!
            Vec3(275f, 0.4f, 240f),     // Peppermint Coast Boardwalk
            Vec3(110f, 0.5f, 295f),     // South coast curve
            Vec3(-165f, 2.5f, 270f),    // Cupcake Valley Stunt Avenue
            Vec3(-275f, 4.0f, 145f),    // Valley Ridge
            Vec3(-280f, 2.2f, -95f),    // Gumdrop Forest Highway
            Vec3(-190f, 1.2f, -245f)    // Forest Northern Loop
        )
        list.add(
            sampleRoad(
                id = "grand_ring",
                name = "Grand Kingdom Ring",
                ctrl = ringCtrl,
                samplesPerSeg = 20,
                halfWidth = 10.5f,
                closed = true,
                roadColorHex = 0xFF3E294D,
                stripeColorHex = 0xFFFF4D8D,
                bridgeCondition = { p -> (p.x > 210f && p.z in 15f..105f) || (p.x > 240f && p.z > 210f) }
            )
        )

        // 2. Central North-South Boulevard (Candy City Hub intersection)
        val nsCtrl = listOf(
            Vec3(0f, 0.2f, -245f),
            Vec3(0f, 0.1f, -120f),
            Vec3(0f, 0.05f, 0f),
            Vec3(0f, 0.1f, 130f),
            Vec3(45f, 0.6f, 275f)
        )
        list.add(
            sampleRoad(
                id = "ns_boulevard",
                name = "Sugar Avenue (N-S)",
                ctrl = nsCtrl,
                samplesPerSeg = 16,
                halfWidth = 9.5f,
                closed = false,
                roadColorHex = 0xFF472D5C,
                stripeColorHex = 0xFF3CE8B5
            )
        )

        // 3. Central East-West Avenue (intersects Sugar Avenue at Candy City Plaza!)
        val ewCtrl = listOf(
            Vec3(-278f, 2.5f, 15f),
            Vec3(-140f, 0.8f, 0f),
            Vec3(0f, 0.05f, 0f),
            Vec3(145f, 1.5f, -5f),
            Vec3(270f, 4.5f, 45f)
        )
        list.add(
            sampleRoad(
                id = "ew_avenue",
                name = "Peppermint Parkway (E-W)",
                ctrl = ewCtrl,
                samplesPerSeg = 16,
                halfWidth = 9.5f,
                closed = false,
                roadColorHex = 0xFF472D5C,
                stripeColorHex = 0xFFFFEB3B,
                bridgeCondition = { p -> p.x > 205f && p.z in 15f..70f }
            )
        )

        // 4. Gumdrop Forest Secret Shortcut (diagonal through lollipop woods)
        val shortcutCtrl = listOf(
            Vec3(-190f, 1.2f, -245f),
            Vec3(-165f, 2.8f, -125f),
            Vec3(-115f, 1.2f, -20f)
        )
        list.add(
            sampleRoad(
                id = "forest_shortcut",
                name = "Gummy Trail Shortcut",
                ctrl = shortcutCtrl,
                samplesPerSeg = 10,
                halfWidth = 6.5f,
                closed = false,
                roadColorHex = 0xFF5D3A7A,
                stripeColorHex = 0xFF4FFFB0
            )
        )

        // 5. Chocolate Mountain Peak Switchback Spur
        val mountainCtrl = listOf(
            Vec3(195f, 6.5f, -255f),
            Vec3(295f, 15.5f, -315f),
            Vec3(355f, 19.0f, -240f),
            Vec3(290f, 11.5f, -155f)
        )
        list.add(
            sampleRoad(
                id = "mountain_spur",
                name = "Fudge Peak Switchback",
                ctrl = mountainCtrl,
                samplesPerSeg = 10,
                halfWidth = 7.5f,
                closed = false,
                roadColorHex = 0xFF4E2A1E,
                stripeColorHex = 0xFFFF9800
            )
        )

        return list
    }

    private fun sampleRoad(
        id: String,
        name: String,
        ctrl: List<Vec3>,
        samplesPerSeg: Int,
        halfWidth: Float,
        closed: Boolean,
        roadColorHex: Long,
        stripeColorHex: Long,
        bridgeCondition: (Vec3) -> Boolean = { false }
    ): OpenWorldRoad {
        val raw = mutableListOf<Vec3>()
        if (closed) {
            val c = ctrl.size
            for (i in 0 until c) {
                val p0 = ctrl[(i - 1 + c) % c]
                val p1 = ctrl[i]
                val p2 = ctrl[(i + 1) % c]
                val p3 = ctrl[(i + 2) % c]
                for (s in 0 until samplesPerSeg) {
                    raw.add(catmull(p0, p1, p2, p3, s.toFloat() / samplesPerSeg))
                }
            }
        } else {
            val padded = listOf(ctrl.first()) + ctrl + listOf(ctrl.last())
            for (i in 1 until padded.size - 2) {
                val p0 = padded[i - 1]; val p1 = padded[i]; val p2 = padded[i + 1]; val p3 = padded[i + 2]
                for (s in 0 until samplesPerSeg) {
                    raw.add(catmull(p0, p1, p2, p3, s.toFloat() / samplesPerSeg))
                }
            }
            raw.add(ctrl.last())
        }

        val pts = ArrayList<RoadSegmentPoint>(raw.size)
        val n = raw.size
        for (i in 0 until n) {
            val prev = if (closed) raw[(i - 1 + n) % n] else raw[(i - 1).coerceAtLeast(0)]
            val curr = raw[i]
            val next = if (closed) raw[(i + 1) % n] else raw[(i + 1).coerceAtMost(n - 1)]
            val fwd = (next - prev).normalized()
            val flatFwd = Vec3(fwd.x, 0f, fwd.z).normalized()
            val right = Vec3(-flatFwd.z, 0f, flatFwd.x).normalized()
            pts.add(RoadSegmentPoint(curr, fwd, right, halfWidth, isBridge = bridgeCondition(curr)))
        }
        return OpenWorldRoad(id, name, pts, closed, roadColorHex, stripeColorHex)
    }

    private fun buildWorldBoostPads(): List<BoostPad> {
        val list = mutableListOf<BoostPad>()
        val n = ringRoad.points.size
        for (idx in listOf(8, 28, 52, 78, 104, 126)) {
            val pt = ringRoad.points[idx % n]
            list.add(BoostPad(idx % n, 0f, pt.pos + Vec3(0f, 0.08f, 0f), atan2(pt.forward.x, pt.forward.z)))
        }
        // Central avenues
        for (road in roads.drop(1)) {
            val mid = road.points[road.points.size / 2]
            list.add(BoostPad(0, 0f, mid.pos + Vec3(0f, 0.08f, 0f), atan2(mid.forward.x, mid.forward.z)))
        }
        return list
    }

    private fun buildWorldRamps(): List<JumpRamp> {
        val list = mutableListOf<JumpRamp>()
        val n = ringRoad.points.size
        for (idx in listOf(18, 46, 86, 114)) {
            val pt = ringRoad.points[idx % n]
            list.add(JumpRamp(idx % n, 0f, pt.pos + Vec3(0f, 0.05f, 0f), atan2(pt.forward.x, pt.forward.z)))
        }
        // Cupcake Valley stunt field ramps
        list.add(JumpRamp(0, 0f, Vec3(-210f, sampleHeightOnly(-210f, 205f), 205f), 0.7f))
        list.add(JumpRamp(0, 0f, Vec3(-145f, sampleHeightOnly(-145f, 175f), 175f), -0.4f))
        return list
    }

    private fun buildCollectibles(): List<WorldCollectible> {
        val items = mutableListOf<WorldCollectible>()
        // 48 Candy Coins distributed across highways, plaza, shortcuts & mountain switchbacks
        var coinIdx = 1
        for (road in roads) {
            val step = max(4, road.points.size / 10)
            for (i in 2 until road.points.size step step) {
                if (coinIdx > 48) break
                val pt = road.points[i]
                val lat = if (coinIdx % 2 == 0) 3.2f else -3.2f
                val pos = pt.pos + pt.right * lat + Vec3(0f, 1.3f, 0f)
                items.add(WorldCollectible("coin_${coinIdx++}", isStar = false, pos = pos, region = regionAt(pos.x, pos.z)))
            }
        }
        // Pad up to 48 coins around Candy City plaza
        while (coinIdx <= 48) {
            val a = (coinIdx / 8f) * (2f * PI.toFloat())
            val x = cos(a) * 24f; val z = sin(a) * 24f
            items.add(WorldCollectible("coin_${coinIdx++}", false, Vec3(x, 1.3f, z), WorldRegion.CANDY_CITY))
        }

        // 10 Hidden Golden Sugar Stars across landmarks & peaks
        val starSpots = listOf(
            Vec3(0f, 2.2f, 28f),       // Candy City Fountain plaza
            Vec3(26f, 2.2f, -26f),     // Candy City Clock Plaza
            Vec3(355f, 20.6f, -240f),  // Chocolate Mountain Summit
            Vec3(245f, 10.0f, -210f),  // Chocolate Waterfall Cliff
            Vec3(-260f, 5.5f, -225f),  // Gumdrop Forest Giant Bear Clearing
            Vec3(-165f, 4.5f, -125f),  // Forest Secret Shortcut
            Vec3(-255f, 6.0f, 235f),   // Cupcake Valley Summit
            Vec3(-195f, 5.2f, 195f),   // Cupcake Stunt Hilltop
            Vec3(275f, 2.0f, 240f),    // Peppermint Coast Boardwalk
            Vec3(335f, 1.4f, 320f)     // Strawberry Cream Shoreline Isle
        )
        starSpots.forEachIndexed { idx, pos ->
            items.add(
                WorldCollectible(
                    id = "star_${idx + 1}",
                    isStar = true,
                    pos = Vec3(pos.x, max(pos.y, sampleHeightOnly(pos.x, pos.z) + 1.8f), pos.z),
                    region = regionAt(pos.x, pos.z)
                )
            )
        }
        return items
    }

    private fun buildRacePortals(): List<WorldRacePortal> {
        return listOf(
            WorldRacePortal(
                id = "portal_city",
                title = "Peppermint Plaza Circuit",
                trackId = TrackId.PEPPERMINT_PLAZA,
                region = WorldRegion.CANDY_CITY,
                pos = Vec3(0f, 0.1f, -55f),
                yawRad = 0f,
                accentHex = 0xFFFF3D8B
            ),
            WorldRacePortal(
                id = "portal_mountains",
                title = "Chocolate Volcano Cup",
                trackId = TrackId.CHOCOLATE_VOLCANO,
                region = WorldRegion.CHOCOLATE_MOUNTAINS,
                pos = Vec3(240f, sampleHeightOnly(240f, -185f), -185f),
                yawRad = 0.7f,
                accentHex = 0xFFFF9800
            ),
            WorldRacePortal(
                id = "portal_forest",
                title = "Gumdrop Garden Dash",
                trackId = TrackId.GUMDROP_GARDEN,
                region = WorldRegion.GUMDROP_FOREST,
                pos = Vec3(-235f, sampleHeightOnly(-235f, -170f), -170f),
                yawRad = -0.6f,
                accentHex = 0xFF3CE8B5
            ),
            WorldRacePortal(
                id = "portal_valley",
                title = "Cupcake Valley Sprint",
                trackId = TrackId.CUPCAKE_COAST,
                region = WorldRegion.CUPCAKE_VALLEY,
                pos = Vec3(-215f, sampleHeightOnly(-215f, 225f), 225f),
                yawRad = 1.1f,
                accentHex = 0xFFD980FA
            ),
            WorldRacePortal(
                id = "portal_coast",
                title = "Peppermint Coast GP",
                trackId = TrackId.CUPCAKE_COAST,
                region = WorldRegion.PEPPERMINT_COAST,
                pos = Vec3(235f, sampleHeightOnly(235f, 225f), 225f),
                yawRad = -0.9f,
                accentHex = 0xFF5CE1E6
            )
        )
    }

    private fun buildMiniChallenges(): List<WorldMiniChallenge> {
        return listOf(
            WorldMiniChallenge(
                id = "ow_speed_trap",
                title = "Candy City Speed Trap",
                description = "Blast through Sugar Avenue over 140 KM/H!",
                region = WorldRegion.CANDY_CITY,
                pos = Vec3(0f, 0.1f, 85f),
                radius = 12f,
                targetSpeedKmh = 140
            ),
            WorldMiniChallenge(
                id = "ow_mountain_climb",
                title = "Fudge Peak Summit",
                description = "Reach the top of Chocolate Mountains!",
                region = WorldRegion.CHOCOLATE_MOUNTAINS,
                pos = Vec3(355f, 19.0f, -240f),
                radius = 16f
            ),
            WorldMiniChallenge(
                id = "ow_forest_trail",
                title = "Gummy Trail Shortcut",
                description = "Dash through Gumdrop Forest shortcut at 110+ KM/H!",
                region = WorldRegion.GUMDROP_FOREST,
                pos = Vec3(-165f, 2.8f, -125f),
                radius = 14f,
                targetSpeedKmh = 110
            ),
            WorldMiniChallenge(
                id = "ow_valley_stunt",
                title = "Cupcake Stunt Ring",
                description = "Hit the Cupcake Valley jump ramp beacon!",
                region = WorldRegion.CUPCAKE_VALLEY,
                pos = Vec3(-210f, sampleHeightOnly(-210f, 205f), 205f),
                radius = 15f
            ),
            WorldMiniChallenge(
                id = "ow_coast_sprint",
                title = "Boardwalk Drift Zone",
                description = "Carve a high-speed drift across Peppermint Coast!",
                region = WorldRegion.PEPPERMINT_COAST,
                pos = Vec3(275f, 0.4f, 240f),
                radius = 15f
            ),
            WorldMiniChallenge(
                id = "ow_coastal_beacon",
                title = "Strawberry Shore Sprint",
                description = "Fly down the South Coast curve above 125 KM/H!",
                region = WorldRegion.PEPPERMINT_COAST,
                pos = Vec3(110f, 0.5f, 295f),
                radius = 15f,
                targetSpeedKmh = 125
            )
        )
    }

    private fun buildCompanyDepots(): List<WorldCompanyDepot> {
        return listOf(
            WorldCompanyDepot(
                id = "depot_city_hq",
                nameAr = "المقر الرئيسي لشركة سويت راش",
                branchTitleAr = "كاندي سيتي • الإدارة المركزية",
                managerNameAr = "الرئيس التنفيذي كراميلو",
                region = WorldRegion.CANDY_CITY,
                padPos = Vec3(34f, sampleHeightOnly(34f, -34f), -34f),
                buildingPos = Vec3(46f, sampleHeightOnly(46f, -46f), -46f),
                buildingYawRad = -0.78f,
                accentHex = 0xFFFF3D8B,
                isHeadquarters = true
            ),
            WorldCompanyDepot(
                id = "depot_choco_factory",
                nameAr = "فرع مصنع الشوكولاتة الملكي",
                branchTitleAr = "جبال الشوكولاتة • خط الصهر",
                managerNameAr = "المهندس كاكاو",
                region = WorldRegion.CHOCOLATE_MOUNTAINS,
                padPos = Vec3(215f, sampleHeightOnly(215f, -195f), -195f),
                buildingPos = Vec3(202f, sampleHeightOnly(202f, -210f), -210f),
                buildingYawRad = 0.65f,
                accentHex = 0xFFFF9800
            ),
            WorldCompanyDepot(
                id = "depot_gumdrop_lab",
                nameAr = "مختبر الجيلي والكريستال",
                branchTitleAr = "غابة الجيلي • التطوير السكري",
                managerNameAr = "الدكتورة جيليانا",
                region = WorldRegion.GUMDROP_FOREST,
                padPos = Vec3(-215f, sampleHeightOnly(-215f, -185f), -185f),
                buildingPos = Vec3(-230f, sampleHeightOnly(-230f, -200f), -200f),
                buildingYawRad = 0.72f,
                accentHex = 0xFF3CE8B5
            ),
            WorldCompanyDepot(
                id = "depot_cupcake_bakery",
                nameAr = "مخبز وادي الكب كيك المركزي",
                branchTitleAr = "وادي الكب كيك • إنتاج الكريمة",
                managerNameAr = "الشيف فانيليا",
                region = WorldRegion.CUPCAKE_VALLEY,
                padPos = Vec3(-195f, sampleHeightOnly(-195f, 185f), 185f),
                buildingPos = Vec3(-180f, sampleHeightOnly(-180f, 172f), 172f),
                buildingYawRad = -2.2f,
                accentHex = 0xFFD980FA
            ),
            WorldCompanyDepot(
                id = "depot_coast_port",
                nameAr = "ميناء ساحل النعناع التجاري",
                branchTitleAr = "ساحل النعناع • التصدير البحري",
                managerNameAr = "القبطان مينتو",
                region = WorldRegion.PEPPERMINT_COAST,
                padPos = Vec3(215f, sampleHeightOnly(215f, 195f), 195f),
                buildingPos = Vec3(200f, sampleHeightOnly(200f, 182f), 182f),
                buildingYawRad = 2.3f,
                accentHex = 0xFF5CE1E6
            )
        )
    }

    private fun buildCompanyContracts(): List<WorldCompanyContract> {
        return listOf(
            WorldCompanyContract(
                id = "contract_frosting_express",
                titleAr = "عقد توريد الكريمة الفاخرة",
                cargoNameAr = "صناديق كريمة الفراولة الملكية",
                pickupDepotId = "depot_city_hq",
                deliveryDepotId = "depot_cupcake_bakery",
                baseRewardCredits = 220,
                rewardXp = 120
            ),
            WorldCompanyContract(
                id = "contract_cocoa_gold",
                titleAr = "شحنة الكاكاو الذهبي الساخن",
                cargoNameAr = "سبائك الشوكولاتة والكراميل",
                pickupDepotId = "depot_cupcake_bakery",
                deliveryDepotId = "depot_choco_factory",
                baseRewardCredits = 250,
                rewardXp = 135
            ),
            WorldCompanyContract(
                id = "contract_gummy_crystals",
                titleAr = "نقل بلورات الطاقة السكرية",
                cargoNameAr = "كبشولات الجيلي المضيئة",
                pickupDepotId = "depot_choco_factory",
                deliveryDepotId = "depot_gumdrop_lab",
                baseRewardCredits = 260,
                rewardXp = 140
            ),
            WorldCompanyContract(
                id = "contract_peppermint_cargo",
                titleAr = "تصدير شحنة النعناع المثلج",
                cargoNameAr = "أسطوانات شراب النعناع الفوار",
                pickupDepotId = "depot_gumdrop_lab",
                deliveryDepotId = "depot_coast_port",
                baseRewardCredits = 280,
                rewardXp = 150
            ),
            WorldCompanyContract(
                id = "contract_hq_vip_return",
                titleAr = "توريد أرباح الفروع للمقر الرئيسي",
                cargoNameAr = "خزينة الحلوى الذهبية الخاصة بالشركة",
                pickupDepotId = "depot_coast_port",
                deliveryDepotId = "depot_city_hq",
                baseRewardCredits = 310,
                rewardXp = 165
            )
        )
    }

    private fun buildWorldCitizens(): List<WorldCitizenNpc> {
        val list = mutableListOf<WorldCitizenNpc>()
        val rng = Random(992025)
        val colors = listOf(0xFFFF3D8BL, 0xFF3CE8B5L, 0xFFFFB800L, 0xFF5CE1E6L, 0xFFD980FAL, 0xFFFF5252L)

        // 1. Branch Managers & Corporate Staff stationed right at the 5 Sweet Rush Corp. Depots
        companyDepots.forEachIndexed { idx, depot ->
            val variant = when {
                depot.isHeadquarters -> CitizenVariant.CORP_EXECUTIVE
                depot.region == WorldRegion.CHOCOLATE_MOUNTAINS -> CitizenVariant.FACTORY_ENGINEER
                depot.region == WorldRegion.CUPCAKE_VALLEY -> CitizenVariant.BAKER_CHEF
                depot.region == WorldRegion.PEPPERMINT_COAST -> CitizenVariant.PEPPERMINT_GUARD
                else -> CitizenVariant.CORP_EXECUTIVE
            }
            val managerPos = depot.padPos + Vec3(5.8f, 0f, 3.5f)
            list.add(
                WorldCitizenNpc(
                    id = "citizen_mgr_${depot.id}",
                    nameAr = depot.managerNameAr,
                    roleAr = depot.nameAr,
                    variant = variant,
                    region = depot.region,
                    homePos = Vec3(managerPos.x, sampleHeightOnly(managerPos.x, managerPos.z), managerPos.z),
                    walkRadius = 1.6f,
                    walkSpeed = 0.7f,
                    phaseOffset = idx * 1.1f,
                    primaryHex = depot.accentHex,
                    secondaryHex = 0xFFFFD54FL,
                    hatHex = depot.accentHex,
                    greetingAr = "مرحباً بك في ${depot.nameAr}! الشحنات والأرباح جاهزة 🏢🍬"
                )
            )
            // Two logistics assistants at each depot
            for (a in 0..1) {
                val ax = depot.padPos.x + (if (a == 0) -6.2f else 6.4f)
                val az = depot.padPos.z - 4.8f
                val roleVariant = if (a == 0) CitizenVariant.FACTORY_ENGINEER else CitizenVariant.BAKER_CHEF
                list.add(
                    WorldCitizenNpc(
                        id = "citizen_staff_${depot.id}_$a",
                        nameAr = if (a == 0) "موظف الشحن سكر" else "مسؤول الجودة لوز",
                        roleAr = "شركة سويت راش • لوجستيات",
                        variant = roleVariant,
                        region = depot.region,
                        homePos = Vec3(ax, sampleHeightOnly(ax, az), az),
                        walkRadius = 2.8f,
                        walkSpeed = 0.95f + a * 0.2f,
                        phaseOffset = (idx * 2 + a) * 0.85f,
                        primaryHex = colors[(idx + a) % colors.size],
                        secondaryHex = 0xFFFFF8F2L,
                        hatHex = depot.accentHex,
                        greetingAr = "نجهز صناديق الحلوى لشركة سويت راش على مدار الساعة! 📦🚚"
                    )
                )
            }
        }

        // 2. Candy City Plaza & Downtown Sidewalk Residents (20 living citizens)
        val cityNames = listOf(
            Triple("الشيف مارشميلو", "كبير خبازي الساحة", CitizenVariant.BAKER_CHEF),
            Triple("الشرطي نعناع", "حارس مرور كاندي سيتي", CitizenVariant.PEPPERMINT_GUARD),
            Triple("المستشار توفي", "مدير استثمارات سويت راش", CitizenVariant.CORP_EXECUTIVE),
            Triple("الحلواني كرز", "بائع الآيس كريم", CitizenVariant.BAKER_CHEF),
            Triple("سارة سبرينكل", "سكان كاندي سيتي", CitizenVariant.CANDY_CITIZEN),
            Triple("المهندس ويفر", "مهندس أبراج الوافل", CitizenVariant.FACTORY_ENGINEER),
            Triple("الكابتن دونات", "حارس البوابة الوردية", CitizenVariant.PEPPERMINT_GUARD),
            Triple("ليلى ماكرون", "مصممة متجر الماكرون", CitizenVariant.CANDY_CITIZEN),
            Triple("المدير فستق", "مجلس إدارة سويت راش", CitizenVariant.CORP_EXECUTIVE),
            Triple("رامي جيلي", "ساعي بريد الحلويات", CitizenVariant.CANDY_CITIZEN)
        )
        val cityGreetings = listOf(
            "يا لها من سيارة رائعة! هل تعمل مع شركة سويت راش؟ 🏎️✨",
            "صباح السكر! المدينة مزدحمة اليوم بالمتسابقين والشاحنات 🍭",
            "انتبه في المنعطفات، الكريمة طازجة على الرصيف! 🧁😄",
            "شركة سويت راش رفعت رواتب سائقي التوصيل هذا الأسبوع! 💰🏢",
            "أهلاً بك في قلب كاندي سيتي، استمتع بالموسيقى والسرعة! 🎶🍬"
        )
        for (i in 0 until 20) {
            val angle = (i.toFloat() / 20f) * (2f * PI.toFloat()) + 0.18f
            val radius = if (i % 2 == 0) 27.5f else 54f
            val cx = cos(angle) * radius
            val cz = sin(angle) * radius
            val info = cityNames[i % cityNames.size]
            val c1 = colors[i % colors.size]
            val c2 = colors[(i + 2) % colors.size]
            list.add(
                WorldCitizenNpc(
                    id = "citizen_city_$i",
                    nameAr = info.first,
                    roleAr = info.second,
                    variant = info.third,
                    region = WorldRegion.CANDY_CITY,
                    homePos = Vec3(cx, sampleHeightOnly(cx, cz), cz),
                    walkRadius = 3.2f + (i % 3) * 0.9f,
                    walkSpeed = 0.85f + (i % 4) * 0.18f,
                    phaseOffset = i * 0.62f,
                    primaryHex = c1,
                    secondaryHex = c2,
                    hatHex = colors[(i + 1) % colors.size],
                    greetingAr = cityGreetings[i % cityGreetings.size]
                )
            )
        }

        // 3. Regional Village, Factory, Bakery, Forest & Boardwalk Residents across the 4 outer regions (36 citizens)
        val regionalHubs = listOf(
            Triple(WorldRegion.CHOCOLATE_MOUNTAINS, Vec3(255f, 0f, -220f), listOf(
                Triple("عامل الكاكاو بندق", "مصنع الشوكولاتة", CitizenVariant.FACTORY_ENGINEER),
                Triple("الشيف نوتيلا", "مطبخ شلال الكراميل", CitizenVariant.BAKER_CHEF),
                Triple("الحارس فادج", "أمن قلعة الشوكولاتة", CitizenVariant.PEPPERMINT_GUARD)
            )),
            Triple(WorldRegion.GUMDROP_FOREST, Vec3(-250f, 0f, -205f), listOf(
                Triple("الباحثة غامي", "مختبر غابة الجيلي", CitizenVariant.CORP_EXECUTIVE),
                Triple("حارس الغابة توت", "دورية أشجار المصاصة", CitizenVariant.PEPPERMINT_GUARD),
                Triple("جامع الكريستال سكر", "سكان غابة الجيلي", CitizenVariant.CANDY_CITIZEN)
            )),
            Triple(WorldRegion.CUPCAKE_VALLEY, Vec3(-235f, 0f, 215f), listOf(
                Triple("الشيف كريمة", "مخبز وادي الكب كيك", CitizenVariant.BAKER_CHEF),
                Triple("تاجر الفانيليا", "سوق الحلويات الغربي", CitizenVariant.CORP_EXECUTIVE),
                Triple("نجمة الكب كيك", "سكان وادي الكب كيك", CitizenVariant.CANDY_CITIZEN)
            )),
            Triple(WorldRegion.PEPPERMINT_COAST, Vec3(250f, 0f, 225f), listOf(
                Triple("قبطان الميناء نعناع", "إدارة رصيف الساحل", CitizenVariant.PEPPERMINT_GUARD),
                Triple("بائع البوظة الساحلي", "كورنيش ساحل النعناع", CitizenVariant.BAKER_CHEF),
                Triple("مدير التصدير مرجان", "فرع شركة سويت راش البحري", CitizenVariant.CORP_EXECUTIVE)
            ))
        )

        var regNpcIdx = 0
        for ((reg, hubCenter, roles) in regionalHubs) {
            for (j in 0 until 9) {
                val a = (j.toFloat() / 9f) * (2f * PI.toFloat()) + 0.35f
                val dist = 20f + (j % 3) * 14f
                var rx = hubCenter.x + cos(a) * dist
                var rz = hubCenter.z + sin(a) * dist
                if (isOnAnyRoad(rx, rz, 4.5f)) {
                    rx += cos(a) * 12f
                    rz += sin(a) * 12f
                }
                val ry = max(0.1f, sampleHeightOnly(rx, rz))
                val roleInfo = roles[j % roles.size]
                val c1 = colors[(regNpcIdx + j) % colors.size]
                val c2 = colors[(regNpcIdx + j + 3) % colors.size]
                val greeting = when (reg) {
                    WorldRegion.CHOCOLATE_MOUNTAINS -> "شلالات الشوكولاتة تعمل بأقصى طاقة لتزويد شركة سويت راش! 🍫⚙️"
                    WorldRegion.GUMDROP_FOREST -> "بلورات السكر في الغابة جاهزة للشحن إلى المدينة! 🍬🌲"
                    WorldRegion.CUPCAKE_VALLEY -> "رائحة الكب كيك الطازج تملأ الوادي! مرحباً بك 🧁✨"
                    else -> "نسيم ساحل النعناع منعش اليوم، وسفن الشحن في انتظارك! 🌊🍭"
                }
                list.add(
                    WorldCitizenNpc(
                        id = "citizen_${reg.name.lowercase()}_$j",
                        nameAr = roleInfo.first,
                        roleAr = roleInfo.second,
                        variant = roleInfo.third,
                        region = reg,
                        homePos = Vec3(rx, ry, rz),
                        walkRadius = 2.6f + (rng.nextFloat() * 2.2f),
                        walkSpeed = 0.80f + rng.nextFloat() * 0.45f,
                        phaseOffset = regNpcIdx * 0.7f,
                        primaryHex = c1,
                        secondaryHex = c2,
                        hatHex = reg.accentHex,
                        greetingAr = greeting
                    )
                )
                regNpcIdx++
            }
        }

        return list
    }

    private fun buildWorldDecorations(): List<DecorationInstance> {
        val decos = mutableListOf<DecorationInstance>()
        val rng = Random(202504)
        val palette = listOf(0xFFFF3D8BL, 0xFF3CE8B5L, 0xFFFFB800L, 0xFF5CE1E6L, 0xFF9C4DFFL, 0xFFFF5252L, 0xFFFFEB3BL)

        // Spatial non-overlap circles (x, z, clearanceRadius) so NO objects/houses spawn inside each other or on roads!
        val placedCircles = mutableListOf<Triple<Float, Float, Float>>()
        val protectedZones = mutableListOf(
            Triple(0f, -22f, 15f),     // Candy City spawn
            Triple(245f, -210f, 15f),  // Chocolate Mountains spawn
            Triple(-245f, -195f, 15f), // Gumdrop Forest spawn
            Triple(-230f, 200f, 15f),  // Cupcake Valley spawn
            Triple(245f, 210f, 15f)    // Peppermint Coast spawn
        )
        for (depot in companyDepots) {
            protectedZones.add(Triple(depot.padPos.x, depot.padPos.z, 11.5f))
        }

        fun tryAddWorldDeco(
            inst: DecorationInstance,
            roadClearExtra: Float = 2.0f,
            skipRoadCheck: Boolean = false
        ): Boolean {
            val solidR = solidCollisionRadius(inst.type, inst.scale)
            val x = inst.pos.x
            val z = inst.pos.z

            if (solidR > 0f) {
                if (!skipRoadCheck && isOnAnyRoad(x, z, solidR + roadClearExtra)) return false

                // Keep portals, boost pads, jump ramps, and region spawn points completely unobstructed
                for (portal in racePortals) {
                    val dx = x - portal.pos.x; val dz = z - portal.pos.z
                    if (dx * dx + dz * dz < (12.5f + solidR) * (12.5f + solidR)) return false
                }
                for (pad in boostPads) {
                    val dx = x - pad.pos.x; val dz = z - pad.pos.z
                    if (dx * dx + dz * dz < (8.0f + solidR) * (8.0f + solidR)) return false
                }
                for (ramp in jumpRamps) {
                    val dx = x - ramp.pos.x; val dz = z - ramp.pos.z
                    if (dx * dx + dz * dz < (9.5f + solidR) * (9.5f + solidR)) return false
                }
                for ((zx, zz, zr) in protectedZones) {
                    val dx = x - zx; val dz = z - zz
                    if (dx * dx + dz * dz < (zr + solidR) * (zr + solidR)) return false
                }
                for ((px, pz, pr) in placedCircles) {
                    val dx = x - px; val dz = z - pz
                    val minGap = solidR + pr + 1.8f
                    if (dx * dx + dz * dz < minGap * minGap) return false
                }
                placedCircles.add(Triple(x, z, solidR))
            }
            decos.add(inst)
            return true
        }

        // Place Monumental Sweet Rush Corp. Headquarters & 4 Regional Logistics Branch Depots first!
        for (depot in companyDepots) {
            val bType = if (depot.isHeadquarters) DecoType.SWEET_CORP_HQ else DecoType.SWEET_CORP_DEPOT
            val bScale = if (depot.isHeadquarters) 1.28f else 1.18f
            val bx = depot.buildingPos.x
            val bz = depot.buildingPos.z
            val by = sampleHeightOnly(bx, bz)
            decos.add(
                DecorationInstance(
                    type = bType,
                    pos = Vec3(bx, by, bz),
                    rotY = depot.buildingYawRad,
                    scale = bScale,
                    primaryColor = depot.accentHex,
                    secondaryColor = 0xFFFFD54FL
                )
            )
            placedCircles.add(Triple(bx, bz, solidCollisionRadius(bType, bScale)))
        }

        // Central Candy City Landmark Fountain & inner skyline ring
        tryAddWorldDeco(
            DecorationInstance(DecoType.STRAWBERRY_FOUNTAIN, Vec3(0f, 0f, 0f), 0f, 1.35f, 0xFFFF3D8B, 0xFFFFF8FF),
            skipRoadCheck = true
        )
        val plazaSkylineTypes = listOf(
            DecoType.ICE_CREAM_PARLOR,
            DecoType.MACARON_TOWER,
            DecoType.WAFFLE_SKYSCRAPER,
            DecoType.DONUT_BUILDING,
            DecoType.CUPCAKE_TOWER,
            DecoType.GINGERBREAD_HOUSE,
            DecoType.CANDY_FACTORY,
            DecoType.PEPPERMINT_LIGHTHOUSE
        )
        for (i in 0 until 12) {
            val a = i * (PI.toFloat() / 6f)
            val rad = if (i % 2 == 0) 44f else 66f
            val x = cos(a) * rad; val z = sin(a) * rad
            val building = plazaSkylineTypes[i % plazaSkylineTypes.size]
            tryAddWorldDeco(
                DecorationInstance(
                    building,
                    Vec3(x, sampleHeightOnly(x, z), z),
                    a + 1.57f,
                    1.20f + (i % 3) * 0.10f,
                    palette[i % palette.size],
                    palette[(i + 3) % palette.size]
                ),
                roadClearExtra = 4.2f
            )
        }
        // Dense Candy City Downtown Quadrants (grid of colorful Candy City storefronts & towers!)
        val cityBlockCoords = listOf(-120f, -86f, -52f, 52f, 86f, 120f)
        var cityIdx = 0
        for (bx in cityBlockCoords) {
            for (bz in cityBlockCoords) {
                val x = bx + ((cityIdx % 3) - 1) * 5f
                val z = bz + (((cityIdx / 3) % 3) - 1) * 5f
                cityIdx++
                val y = sampleHeightOnly(x, z)
                if (y < -0.25f) continue
                val bType = plazaSkylineTypes[cityIdx % plazaSkylineTypes.size]
                val yawToRoad = if (abs(x) < abs(z)) (if (x > 0) -1.57f else 1.57f) else (if (z > 0) 3.14f else 0f)
                tryAddWorldDeco(
                    DecorationInstance(
                        type = bType,
                        pos = Vec3(x, y, z),
                        rotY = yawToRoad,
                        scale = 1.05f + (cityIdx % 4) * 0.12f,
                        primaryColor = palette[cityIdx % palette.size],
                        secondaryColor = palette[(cityIdx + 2) % palette.size]
                    ),
                    roadClearExtra = 4.5f
                )
            }
        }

        // Spectator stands, road signs & animated billboards around Candy City intersection
        tryAddWorldDeco(DecorationInstance(DecoType.SPECTATOR_STAND, Vec3(24f, 0f, -34f), -0.6f, 1.1f, 0xFFFF3D8B, 0xFF3CE8B5), roadClearExtra = 2.5f)
        tryAddWorldDeco(DecorationInstance(DecoType.SPECTATOR_STAND, Vec3(-24f, 0f, 34f), 2.5f, 1.1f, 0xFFFFEB3B, 0xFF5CE1E6), roadClearExtra = 2.5f)
        tryAddWorldDeco(DecorationInstance(DecoType.ANIMATED_BILLBOARD, Vec3(22f, 0f, 22f), -2.35f, 1.15f, 0xFFFF3D8B, 0xFF3CE8B5), roadClearExtra = 2.5f)
        tryAddWorldDeco(DecorationInstance(DecoType.ANIMATED_BILLBOARD, Vec3(-22f, 0f, -22f), 0.78f, 1.15f, 0xFFFFB800, 0xFF9C4DFF), roadClearExtra = 2.5f)
        tryAddWorldDeco(DecorationInstance(DecoType.ROAD_SIGN, Vec3(14f, 0f, -18f), 0f, 1.1f, 0xFFFF3D8B, 0xFF3CE8B5), roadClearExtra = 1.5f)
        tryAddWorldDeco(DecorationInstance(DecoType.ROAD_SIGN, Vec3(-14f, 0f, 18f), 3.14f, 1.1f, 0xFF5CE1E6, 0xFFFFEB3B), roadClearExtra = 1.5f)

        // Road-hugging lamps, arches, road signs, outer curve barriers & roadside candy buildings along ALL highways
        val roadsideBuildings = listOf(
            DecoType.GINGERBREAD_HOUSE,
            DecoType.ICE_CREAM_PARLOR,
            DecoType.DONUT_BUILDING,
            DecoType.MACARON_TOWER,
            DecoType.COOKIE_HOUSE,
            DecoType.CUPCAKE_TOWER,
            DecoType.CANDY_FACTORY,
            DecoType.WAFFLE_SKYSCRAPER
        )
        for ((rIdx, road) in roads.withIndex()) {
            val step = if (road.points.size > 120) 6 else 5
            for (i in road.points.indices step step) {
                val pt = road.points[i]
                val yaw = atan2(pt.forward.x, pt.forward.z)
                if (i % (step * 3) == 0 && !pt.isBridge) {
                    val arch = if (rIdx % 2 == 0) DecoType.CANDY_CANE_ARCH else DecoType.DONUT_ARCH
                    tryAddWorldDeco(DecorationInstance(arch, pt.pos, yaw, 0.85f, palette[(i + rIdx) % palette.size], 0xFFFFFFFF))
                }
                if (i % (step * 2) == 0) {
                    for (side in listOf(-1f, 1f)) {
                        val lp = pt.pos + pt.right * (side * (pt.halfWidth + 2.7f))
                        tryAddWorldDeco(
                            DecorationInstance(DecoType.CANDY_LAMP, lp, yaw, 0.95f, palette[i % palette.size], 0xFFFFFFFF),
                            roadClearExtra = 1.2f
                        )
                    }
                }
                // Place themed roadside candy shops & towers framing every avenue outside the sidewalk!
                if (!pt.isBridge && i % (step * 2) == 0) {
                    val side = if (((i / step) + rIdx) % 2 == 0) 1f else -1f
                    val bPos = pt.pos + pt.right * (side * (pt.halfWidth + 15.5f))
                    val by = sampleHeightOnly(bPos.x, bPos.z)
                    if (by > -0.25f) {
                        val bType = roadsideBuildings[(i + rIdx) % roadsideBuildings.size]
                        val faceRoadYaw = yaw + (if (side > 0f) -1.57f else 1.57f)
                        tryAddWorldDeco(
                            DecorationInstance(
                                type = bType,
                                pos = Vec3(bPos.x, by, bPos.z),
                                rotY = faceRoadYaw,
                                scale = 1.0f + ((i % 3) * 0.10f),
                                primaryColor = palette[(i + rIdx) % palette.size],
                                secondaryColor = palette[(i + rIdx + 2) % palette.size]
                            ),
                            roadClearExtra = 4.2f
                        )
                    }
                }
                if (i % (step * 4) == step && !pt.isBridge) {
                    val sp = pt.pos + pt.right * (pt.halfWidth + 3.2f)
                    tryAddWorldDeco(
                        DecorationInstance(DecoType.ROAD_SIGN, sp, yaw, 1.0f, palette[(i + 2) % palette.size], 0xFFFFEB3B),
                        roadClearExtra = 1.4f
                    )
                }
                if ((rIdx == 0 || rIdx == 4) && i % (step * 2) == step && !pt.isBridge) {
                    val bSide = if ((i / step) % 2 == 0) 1f else -1f
                    val bp = pt.pos + pt.right * (bSide * (pt.halfWidth + 3.85f))
                    tryAddWorldDeco(
                        DecorationInstance(DecoType.CANDY_BARRIER, bp, yaw + 1.57f, 1.0f, 0xFFFF2448, 0xFF3CE8B5),
                        roadClearExtra = 0.6f
                    )
                }
            }
        }

        // Wafer Tunnels along Chocolate Mountain Switchback Spur & Gumdrop Shortcut
        if (roads.size > 4) {
            val mountainSpur = roads[4]
            for (idx in 10..16 step 2) {
                if (idx < mountainSpur.points.size) {
                    val pt = mountainSpur.points[idx]
                    val yaw = atan2(pt.forward.x, pt.forward.z)
                    decos.add(DecorationInstance(DecoType.WAFER_TUNNEL_RING, pt.pos, yaw, 0.92f, 0xFF6D3B22, 0xFFFF9800))
                }
            }
        }
        if (roads.size > 3) {
            val shortcut = roads[3]
            for (idx in 7..11 step 2) {
                if (idx < shortcut.points.size) {
                    val pt = shortcut.points[idx]
                    val yaw = atan2(pt.forward.x, pt.forward.z)
                    decos.add(DecorationInstance(DecoType.WAFER_TUNNEL_RING, pt.pos, yaw, 0.85f, 0xFFF4B678, 0xFF3CE8B5))
                }
            }
        }

        // Chocolate Mountain Landmarks & Candy Factory
        tryAddWorldDeco(DecorationInstance(DecoType.CHOCOLATE_CASTLE, Vec3(330f, sampleHeightOnly(330f, -265f), -265f), 0.5f, 1.85f, 0xFFFFB300, 0xFF6D3B22), roadClearExtra = 4f)
        tryAddWorldDeco(DecorationInstance(DecoType.CHOCOLATE_WATERFALL, Vec3(265f, sampleHeightOnly(265f, -110f), -110f), 0.2f, 1.65f, 0xFF8B4726, 0xFFFFB74D), roadClearExtra = 4f)
        tryAddWorldDeco(DecorationInstance(DecoType.CHOCOLATE_WATERFALL, Vec3(340f, sampleHeightOnly(340f, -175f), -175f), -0.8f, 1.50f, 0xFF8B4726, 0xFFFFB74D), roadClearExtra = 4f)
        tryAddWorldDeco(DecorationInstance(DecoType.CANDY_FACTORY, Vec3(155f, sampleHeightOnly(155f, -135f), -135f), -0.4f, 1.45f, 0xFFFF9800, 0xFF3CE8B5), roadClearExtra = 4f)

        // Gumdrop Forest Giant Gummy Bears & Ice Cream Parlor
        tryAddWorldDeco(DecorationInstance(DecoType.GUMMY_BEAR_STATUE, Vec3(-260f, sampleHeightOnly(-260f, -225f), -225f), 0.7f, 1.75f, 0xFF3CE8B5, 0xFFFF3D8B), roadClearExtra = 4f)
        tryAddWorldDeco(DecorationInstance(DecoType.GUMMY_BEAR_STATUE, Vec3(-310f, sampleHeightOnly(-310f, -180f), -180f), 1.2f, 1.55f, 0xFFFF3D8B, 0xFFFFEB3B), roadClearExtra = 4f)
        tryAddWorldDeco(DecorationInstance(DecoType.MACARON_TOWER, Vec3(-210f, sampleHeightOnly(-210f, -135f), -135f), 0.4f, 1.45f, 0xFF3CE8B5, 0xFFFF6EB4), roadClearExtra = 4f)

        // Cupcake Valley Cupcake Castles & Ice Cream Parlor
        tryAddWorldDeco(DecorationInstance(DecoType.CUPCAKE_TOWER, Vec3(-255f, sampleHeightOnly(-255f, 235f), 235f), 0.4f, 1.7f, 0xFFD980FA, 0xFFFFF8FF), roadClearExtra = 4f)
        tryAddWorldDeco(DecorationInstance(DecoType.GINGERBREAD_HOUSE, Vec3(-305f, sampleHeightOnly(-305f, 210f), 210f), 1.0f, 1.45f, 0xFFFF6EB4, 0xFF3CE8B5), roadClearExtra = 4f)
        tryAddWorldDeco(DecorationInstance(DecoType.ICE_CREAM_PARLOR, Vec3(-155f, sampleHeightOnly(-155f, 155f), 155f), -0.6f, 1.4f, 0xFFFF3D8B, 0xFF5CE1E6), roadClearExtra = 4f)

        // Peppermint Coast Lighthouse, Fountains & Coastal Parlor
        tryAddWorldDeco(DecorationInstance(DecoType.PEPPERMINT_LIGHTHOUSE, Vec3(315f, max(0.2f, sampleHeightOnly(315f, 275f)), 275f), -0.5f, 1.55f, 0xFF5CE1E6, 0xFFFF3D8B), roadClearExtra = 4f)
        tryAddWorldDeco(DecorationInstance(DecoType.STRAWBERRY_FOUNTAIN, Vec3(235f, sampleHeightOnly(235f, 185f), 185f), 0f, 1.25f, 0xFF5CE1E6, 0xFFFFF8FF), roadClearExtra = 4f)
        tryAddWorldDeco(DecorationInstance(DecoType.DONUT_BUILDING, Vec3(205f, sampleHeightOnly(205f, 255f), 255f), 2.1f, 1.35f, 0xFFFF6EB4, 0xFFFFFFFF), roadClearExtra = 4f)

        // Scatter regional candy buildings, trees, jellybean bushes, candy flowers, crystals, benches & floating balloons with zero overlap!
        for (i in 0 until 420) {
            val x = (rng.nextFloat() * 2f - 1f) * 435f
            val z = (rng.nextFloat() * 2f - 1f) * 435f
            val y = sampleHeightOnly(x, z)
            if (y < -0.35f) continue // Keep dry land for buildings/trees

            val reg = regionAt(x, z)
            val c1 = palette[rng.nextInt(palette.size)]
            val c2 = palette[rng.nextInt(palette.size)]

            val type = when (reg) {
                WorldRegion.CANDY_CITY -> when (rng.nextInt(10)) {
                    0 -> DecoType.GINGERBREAD_HOUSE
                    1 -> DecoType.DONUT_BUILDING
                    2 -> DecoType.ICE_CREAM_PARLOR
                    3 -> DecoType.MACARON_TOWER
                    4 -> DecoType.WAFFLE_SKYSCRAPER
                    5 -> DecoType.CANDY_FACTORY
                    6 -> DecoType.CANDY_BENCH
                    7 -> DecoType.LOLLIPOP_TREE
                    8 -> DecoType.CANDY_FLOWER
                    else -> DecoType.FLOATING_BALLOON
                }
                WorldRegion.CHOCOLATE_MOUNTAINS -> when (rng.nextInt(8)) {
                    0, 1 -> DecoType.CHOCOLATE_TREE
                    2 -> DecoType.SUGAR_CRYSTAL
                    3 -> DecoType.COOKIE_HOUSE
                    4 -> DecoType.CHOCOLATE_CASTLE
                    5 -> DecoType.GUMDROP_HILL
                    6 -> DecoType.JELLYBEAN_BUSH
                    else -> DecoType.CANDY_FACTORY
                }
                WorldRegion.GUMDROP_FOREST -> when (rng.nextInt(9)) {
                    0, 1 -> DecoType.LOLLIPOP_TREE
                    2 -> DecoType.GUMDROP_TREE
                    3 -> DecoType.COTTON_CANDY_TREE
                    4 -> DecoType.JELLYBEAN_BUSH
                    5 -> DecoType.MACARON_TOWER
                    6 -> DecoType.CANDY_FLOWER
                    7 -> DecoType.MARSHMALLOW_BUSH
                    else -> DecoType.GUMMY_BEAR_STATUE
                }
                WorldRegion.CUPCAKE_VALLEY -> when (rng.nextInt(8)) {
                    0 -> DecoType.CUPCAKE_TOWER
                    1 -> DecoType.ICE_CREAM_PARLOR
                    2 -> DecoType.COTTON_CANDY_TREE
                    3 -> DecoType.COOKIE_HOUSE
                    4 -> DecoType.CANDY_FLOWER
                    5 -> DecoType.JELLYBEAN_BUSH
                    6 -> DecoType.MARSHMALLOW_BUSH
                    else -> DecoType.FLOATING_BALLOON
                }
                WorldRegion.PEPPERMINT_COAST -> when (rng.nextInt(9)) {
                    0 -> DecoType.CANDY_CANE_PLANT
                    1 -> DecoType.ICE_CREAM_PARLOR
                    2 -> DecoType.PEPPERMINT_LIGHTHOUSE
                    3 -> DecoType.COTTON_CANDY_TREE
                    4 -> DecoType.CANDY_BENCH
                    5 -> DecoType.CANDY_FLOWER
                    6 -> DecoType.JELLYBEAN_BUSH
                    7 -> DecoType.MARSHMALLOW_BUSH
                    else -> DecoType.FLOATING_BALLOON
                }
            }

            val finalY = if (type == DecoType.FLOATING_BALLOON) y + 12f + rng.nextFloat() * 10f else y
            val scale = 0.88f + rng.nextFloat() * 0.55f
            tryAddWorldDeco(
                DecorationInstance(
                    type = type,
                    pos = Vec3(x, finalY, z),
                    rotY = rng.nextFloat() * 6.283f,
                    scale = scale,
                    primaryColor = c1,
                    secondaryColor = c2,
                    bobPhase = rng.nextFloat() * 6.283f
                ),
                roadClearExtra = 4.2f
            )
        }

        return decos
    }

    private fun isOnAnyRoad(x: Float, z: Float, extraMargin: Float): Boolean {
        if (x * x + z * z < (24f + extraMargin) * (24f + extraMargin)) return true // Central plaza circle
        for (road in roads) {
            for (pt in road.points) {
                val clear = pt.halfWidth + extraMargin
                val dx = x - pt.pos.x
                val dz = z - pt.pos.z
                if (dx * dx + dz * dz < clear * clear) return true
            }
        }
        return false
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun catmull(p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3, t: Float): Vec3 {
        val t2 = t * t
        val t3 = t2 * t
        return Vec3(
            0.5f * ((2f * p1.x) + (-p0.x + p2.x) * t + (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 + (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3),
            0.5f * ((2f * p1.y) + (-p0.y + p2.y) * t + (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 + (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3),
            0.5f * ((2f * p1.z) + (-p0.z + p2.z) * t + (2f * p0.z - 5f * p1.z + 4f * p2.z - p3.z) * t2 + (-p0.z + 3f * p1.z - 3f * p2.z + p3.z) * t3)
        )
    }
}
