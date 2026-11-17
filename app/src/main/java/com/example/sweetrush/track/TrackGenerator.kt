package com.example.sweetrush.track

import com.example.sweetrush.model.TrackId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3 {
        val len = length()
        return if (len > 1e-5f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, 1f)
    }
}

enum class DecoType {
    LOLLIPOP_TREE,
    GUMDROP_TREE,
    GUMDROP_HILL,
    MARSHMALLOW_BUSH,
    COTTON_CANDY_TREE,
    CANDY_CANE_PLANT,
    CHOCOLATE_TREE,
    GINGERBREAD_HOUSE,
    CUPCAKE_TOWER,
    COOKIE_HOUSE,
    CHOCOLATE_CASTLE,
    DONUT_BUILDING,
    GUMMY_BEAR_STATUE,
    SUGAR_CRYSTAL,
    FLOATING_BALLOON,
    CHOCOLATE_WATERFALL,
    STRAWBERRY_FOUNTAIN,
    CANDY_LAMP,
    CANDY_CANE_ARCH,
    DONUT_ARCH,
    WAFER_TUNNEL_RING
}

data class TrackNode(
    val index: Int,
    val center: Vec3,
    val forward: Vec3,
    val right: Vec3,
    val up: Vec3,
    val halfWidth: Float,
    val progress: Float,
    val curvature: Float,
    val isBridge: Boolean = false
)

data class BoostPad(val nodeIndex: Int, val lateralOffset: Float, val pos: Vec3, val yawRad: Float)
data class JumpRamp(val nodeIndex: Int, val lateralOffset: Float, val pos: Vec3, val yawRad: Float)
data class TrackHazard(val nodeIndex: Int, val lateralOffset: Float, val pos: Vec3, val isCaramel: Boolean)

data class ItemBoxSpot(
    val id: Int,
    val pos: Vec3,
    val yawRad: Float
)

data class DecorationInstance(
    val type: DecoType,
    val pos: Vec3,
    val rotY: Float,
    val scale: Float,
    val primaryColor: Long,
    val secondaryColor: Long,
    val bobPhase: Float = 0f
)

data class TrackProjection(
    val nodeIndex: Int,
    val center: Vec3,
    val forward: Vec3,
    val right: Vec3,
    val lateralOffset: Float,
    val halfWidth: Float,
    val progress: Float,
    val curvature: Float
)

data class GeneratedTrack(
    val id: TrackId,
    val nodes: List<TrackNode>,
    val boostPads: List<BoostPad>,
    val jumpRamps: List<JumpRamp>,
    val hazards: List<TrackHazard>,
    val itemBoxes: List<ItemBoxSpot>,
    val decorations: List<DecorationInstance>,
    val totalLength: Float,
    val minX: Float,
    val maxX: Float,
    val minZ: Float,
    val maxZ: Float
) {
    fun projectOntoTrack(x: Float, z: Float, hintIndex: Int = -1): TrackProjection {
        val n = nodes.size
        var bestIdx = 0
        var bestDistSq = Float.MAX_VALUE

        if (hintIndex in 0 until n) {
            val window = 26
            for (offset in -window..window) {
                val idx = ((hintIndex + offset) % n + n) % n
                val c = nodes[idx].center
                val dx = x - c.x
                val dz = z - c.z
                val d2 = dx * dx + dz * dz
                if (d2 < bestDistSq) {
                    bestDistSq = d2
                    bestIdx = idx
                }
            }
            // Fallback to global search if far away from hint
            if (bestDistSq > 1600f) {
                for (i in 0 until n) {
                    val c = nodes[i].center
                    val dx = x - c.x
                    val dz = z - c.z
                    val d2 = dx * dx + dz * dz
                    if (d2 < bestDistSq) {
                        bestDistSq = d2
                        bestIdx = i
                    }
                }
            }
        } else {
            for (i in 0 until n) {
                val c = nodes[i].center
                val dx = x - c.x
                val dz = z - c.z
                val d2 = dx * dx + dz * dz
                if (d2 < bestDistSq) {
                    bestDistSq = d2
                    bestIdx = i
                }
            }
        }

        val node = nodes[bestIdx]
        val dx = x - node.center.x
        val dz = z - node.center.z
        val lateral = dx * node.right.x + dz * node.right.z
        return TrackProjection(
            nodeIndex = bestIdx,
            center = node.center,
            forward = node.forward,
            right = node.right,
            lateralOffset = lateral,
            halfWidth = node.halfWidth,
            progress = node.progress,
            curvature = node.curvature
        )
    }
}

object TrackGenerator {

    private val trackCache = mutableMapOf<TrackId, GeneratedTrack>()

    fun getTrack(id: TrackId): GeneratedTrack {
        return trackCache.getOrPut(id) { buildTrack(id) }
    }

    private fun buildTrack(id: TrackId): GeneratedTrack {
        val controlPoints = when (id) {
            TrackId.PEPPERMINT_PLAZA -> listOf(
                Vec3(0f, 0f, 0f),
                Vec3(0f, 0f, 95f),
                Vec3(35f, 1.5f, 165f),
                Vec3(110f, 3.0f, 185f),
                Vec3(185f, 2.0f, 145f),
                Vec3(205f, 0.5f, 70f),
                Vec3(155f, 0f, 15f),
                Vec3(145f, 3.5f, -65f),
                Vec3(195f, 5.0f, -135f),
                Vec3(140f, 2.5f, -195f),
                Vec3(55f, 0.5f, -180f),
                Vec3(5f, 0f, -110f)
            )
            TrackId.CUPCAKE_COAST -> listOf(
                Vec3(0f, 0.5f, 0f),
                Vec3(0f, 0.5f, 110f),
                Vec3(55f, 4.0f, 195f),
                Vec3(145f, 6.0f, 195f),
                Vec3(215f, 2.0f, 130f),
                Vec3(190f, 0.5f, 45f),
                Vec3(135f, 0.5f, -10f),
                Vec3(175f, 3.0f, -95f),
                Vec3(150f, 4.5f, -180f),
                Vec3(70f, 2.0f, -200f),
                Vec3(10f, 0.5f, -135f),
                Vec3(-15f, 0.5f, -65f)
            )
            TrackId.CHOCOLATE_VOLCANO -> listOf(
                Vec3(0f, 0f, 0f),
                Vec3(0f, 2.0f, 100f),
                Vec3(50f, 9.0f, 185f),
                Vec3(140f, 14.0f, 200f),
                Vec3(220f, 10.0f, 130f),
                Vec3(200f, 4.0f, 45f),
                Vec3(145f, 6.0f, -25f),
                Vec3(185f, 11.0f, -115f),
                Vec3(135f, 7.0f, -195f),
                Vec3(45f, 2.5f, -190f),
                Vec3(-25f, 1.0f, -125f),
                Vec3(-15f, 0f, -60f)
            )
            TrackId.GUMDROP_GARDEN -> listOf(
                Vec3(0f, 0f, 0f),
                Vec3(10f, 1.0f, 105f),
                Vec3(75f, 4.5f, 175f),
                Vec3(165f, 3.0f, 165f),
                Vec3(215f, 0.5f, 90f),
                Vec3(165f, 2.5f, 20f),
                Vec3(120f, 5.5f, -45f),
                Vec3(165f, 4.0f, -135f),
                Vec3(105f, 1.0f, -195f),
                Vec3(25f, 2.0f, -175f),
                Vec3(-25f, 1.0f, -105f),
                Vec3(-12f, 0f, -50f)
            )
        }

        val samplesPerSegment = 16
        val rawPoints = sampleClosedCatmullRom(controlPoints, samplesPerSegment)
        val n = rawPoints.size
        val roadHalfWidth = when (id) {
            TrackId.PEPPERMINT_PLAZA -> 13.5f
            TrackId.CUPCAKE_COAST -> 13.0f
            TrackId.CHOCOLATE_VOLCANO -> 12.0f
            TrackId.GUMDROP_GARDEN -> 13.0f
        }

        var totalLen = 0f
        val cumDist = FloatArray(n)
        for (i in 0 until n) {
            val next = rawPoints[(i + 1) % n]
            val seg = (next - rawPoints[i]).length()
            cumDist[i] = totalLen
            totalLen += seg
        }

        val nodes = ArrayList<TrackNode>(n)
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (i in 0 until n) {
            val prev = rawPoints[(i - 1 + n) % n]
            val curr = rawPoints[i]
            val next = rawPoints[(i + 1) % n]
            val fwd = (next - prev).normalized()
            val flatFwd = Vec3(fwd.x, 0f, fwd.z).normalized()
            val right = Vec3(-flatFwd.z, 0f, flatFwd.x).normalized()
            val up = Vec3(0f, 1f, 0f)

            val prevFwd = (curr - prev).normalized()
            val nextFwd = (next - curr).normalized()
            val crossY = prevFwd.x * nextFwd.z - prevFwd.z * nextFwd.x

            if (curr.x < minX) minX = curr.x
            if (curr.x > maxX) maxX = curr.x
            if (curr.z < minZ) minZ = curr.z
            if (curr.z > maxZ) maxZ = curr.z

            val isBridge = curr.y > 3.2f
            nodes.add(
                TrackNode(
                    index = i,
                    center = curr,
                    forward = fwd,
                    right = right,
                    up = up,
                    halfWidth = roadHalfWidth,
                    progress = cumDist[i] / totalLen,
                    curvature = crossY,
                    isBridge = isBridge
                )
            )
        }

        // Boost Pads
        val boostIndices = listOf(18, 48, 86, 128, 162)
        val boostPads = boostIndices.mapIndexed { idx, nodeIdx ->
            val node = nodes[nodeIdx % n]
            val lat = if (idx % 2 == 0) -3.5f else 3.5f
            val pos = node.center + node.right * lat + Vec3(0f, 0.08f, 0f)
            val yaw = atan2(node.forward.x, node.forward.z)
            BoostPad(nodeIdx % n, lat, pos, yaw)
        }

        // Jump Ramps
        val rampIndices = listOf(34, 110, 152)
        val jumpRamps = rampIndices.map { nodeIdx ->
            val node = nodes[nodeIdx % n]
            val pos = node.center + Vec3(0f, 0.05f, 0f)
            val yaw = atan2(node.forward.x, node.forward.z)
            JumpRamp(nodeIdx % n, 0f, pos, yaw)
        }

        // Track-specific hazards (caramel pools on Volcano / sticky sugar on others)
        val hazardIndices = when (id) {
            TrackId.CHOCOLATE_VOLCANO -> listOf(40, 72, 102, 138, 170)
            TrackId.CUPCAKE_COAST -> listOf(64, 120, 174)
            else -> listOf(58, 118, 168)
        }
        val hazards = hazardIndices.mapIndexed { idx, nodeIdx ->
            val node = nodes[nodeIdx % n]
            val lat = when (idx % 3) {
                0 -> -4.8f
                1 -> 4.6f
                else -> 0f
            }
            TrackHazard(
                nodeIdx % n,
                lat,
                node.center + node.right * lat + Vec3(0f, 0.06f, 0f),
                isCaramel = (id == TrackId.CHOCOLATE_VOLCANO)
            )
        }

        // Item Box Clusters
        val itemClusters = listOf(14, 52, 92, 136, 172)
        val itemBoxes = mutableListOf<ItemBoxSpot>()
        var boxId = 0
        for (cIdx in itemClusters) {
            val node = nodes[cIdx % n]
            val yaw = atan2(node.forward.x, node.forward.z)
            for (lat in listOf(-6.2f, -2.1f, 2.1f, 6.2f)) {
                itemBoxes.add(
                    ItemBoxSpot(
                        id = boxId++,
                        pos = node.center + node.right * lat + Vec3(0f, 1.35f, 0f),
                        yawRad = yaw
                    )
                )
            }
        }

        // Rich Handcrafted-Looking Candy Decorations
        val decorations = generateDecorations(id, nodes, Random(id.ordinal * 7919 + 42))

        return GeneratedTrack(
            id = id,
            nodes = nodes,
            boostPads = boostPads,
            jumpRamps = jumpRamps,
            hazards = hazards,
            itemBoxes = itemBoxes,
            decorations = decorations,
            totalLength = totalLen,
            minX = minX - 40f,
            maxX = maxX + 40f,
            minZ = minZ - 40f,
            maxZ = maxZ + 40f
        )
    }

    private fun generateDecorations(
        id: TrackId,
        nodes: List<TrackNode>,
        rng: Random
    ): List<DecorationInstance> {
        val decos = mutableListOf<DecorationInstance>()
        val n = nodes.size
        val candyColors = listOf(
            0xFFFF3D8B, 0xFF3CE8B5, 0xFFFFB800, 0xFF5CE1E6,
            0xFF9C4DFF, 0xFFFF5252, 0xFFFFEB3B, 0xFFFF80AB
        )

        // Over-track arches and tunnels
        val archIndices = listOf(22, 68, 114, 158)
        for ((idx, nodeIdx) in archIndices.withIndex()) {
            val node = nodes[nodeIdx % n]
            val yaw = atan2(node.forward.x, node.forward.z)
            val archType = when (id) {
                TrackId.PEPPERMINT_PLAZA -> DecoType.CANDY_CANE_ARCH
                TrackId.CUPCAKE_COAST -> if (idx % 2 == 0) DecoType.DONUT_ARCH else DecoType.WAFER_TUNNEL_RING
                TrackId.CHOCOLATE_VOLCANO -> DecoType.WAFER_TUNNEL_RING
                TrackId.GUMDROP_GARDEN -> if (idx % 2 == 0) DecoType.CANDY_CANE_ARCH else DecoType.DONUT_ARCH
            }
            if (archType == DecoType.WAFER_TUNNEL_RING) {
                for (ringOffset in 0..3) {
                    val rNode = nodes[(nodeIdx + ringOffset * 2) % n]
                    val rYaw = atan2(rNode.forward.x, rNode.forward.z)
                    decos.add(
                        DecorationInstance(
                            type = DecoType.WAFER_TUNNEL_RING,
                            pos = rNode.center,
                            rotY = rYaw,
                            scale = 1.0f,
                            primaryColor = if (id == TrackId.CHOCOLATE_VOLCANO) 0xFF6D3B22 else 0xFFF4B678,
                            secondaryColor = if (id == TrackId.CHOCOLATE_VOLCANO) 0xFFFF9800 else 0xFFFF7EB6
                        )
                    )
                }
            } else {
                decos.add(
                    DecorationInstance(
                        type = archType,
                        pos = node.center,
                        rotY = yaw,
                        scale = 1.0f,
                        primaryColor = candyColors[idx % candyColors.size],
                        secondaryColor = 0xFFFFFFFF
                    )
                )
            }
        }

        // Trackside lights & dense scenery along both sides
        for (i in 0 until n step 2) {
            val node = nodes[i]
            val yaw = atan2(node.forward.x, node.forward.z)

            // Trackside candy lamp every 6 nodes
            if (i % 6 == 0) {
                for (side in listOf(-1f, 1f)) {
                    val pos = node.center + node.right * (side * (node.halfWidth + 2.6f))
                    // Avoid placing inside another segment of the road
                    if (!isTooCloseToAnyRoad(pos, nodes, i, minDist = node.halfWidth + 1.5f)) {
                        decos.add(
                            DecorationInstance(
                                type = DecoType.CANDY_LAMP,
                                pos = pos,
                                rotY = yaw,
                                scale = 1.0f,
                                primaryColor = candyColors[(i + if (side > 0) 1 else 3) % candyColors.size],
                                secondaryColor = 0xFFFFFFFF
                            )
                        )
                    }
                }
            }

            // Inner & outer candy objects
            for (side in listOf(-1f, 1f)) {
                val dist = node.halfWidth + 7f + rng.nextFloat() * 24f
                val pos = node.center + node.right * (side * dist)
                if (isTooCloseToAnyRoad(pos, nodes, i, minDist = node.halfWidth + 5.5f)) continue

                val c1 = candyColors[rng.nextInt(candyColors.size)]
                val c2 = candyColors[rng.nextInt(candyColors.size)]
                val groundY = if (id == TrackId.CHOCOLATE_VOLCANO) 0f else (node.center.y * 0.4f).coerceAtLeast(0f)
                val placedPos = Vec3(pos.x, groundY, pos.z)

                val type = when (id) {
                    TrackId.PEPPERMINT_PLAZA -> when (rng.nextInt(10)) {
                        0, 1 -> DecoType.LOLLIPOP_TREE
                        2 -> DecoType.GINGERBREAD_HOUSE
                        3 -> DecoType.CUPCAKE_TOWER
                        4 -> DecoType.CANDY_CANE_PLANT
                        5 -> DecoType.GUMDROP_TREE
                        6 -> DecoType.COOKIE_HOUSE
                        7 -> DecoType.DONUT_BUILDING
                        8 -> DecoType.MARSHMALLOW_BUSH
                        else -> DecoType.FLOATING_BALLOON
                    }
                    TrackId.CUPCAKE_COAST -> when (rng.nextInt(10)) {
                        0, 1 -> DecoType.CUPCAKE_TOWER
                        2, 3 -> DecoType.COTTON_CANDY_TREE
                        4 -> DecoType.STRAWBERRY_FOUNTAIN
                        5 -> DecoType.DONUT_BUILDING
                        6 -> DecoType.MARSHMALLOW_BUSH
                        7 -> DecoType.LOLLIPOP_TREE
                        8 -> DecoType.COOKIE_HOUSE
                        else -> DecoType.FLOATING_BALLOON
                    }
                    TrackId.CHOCOLATE_VOLCANO -> when (rng.nextInt(10)) {
                        0, 1 -> DecoType.CHOCOLATE_TREE
                        2, 3 -> DecoType.CHOCOLATE_CASTLE
                        4 -> DecoType.CHOCOLATE_WATERFALL
                        5 -> DecoType.SUGAR_CRYSTAL
                        6 -> DecoType.GUMDROP_HILL
                        7 -> DecoType.COOKIE_HOUSE
                        8 -> DecoType.CANDY_CANE_PLANT
                        else -> DecoType.SUGAR_CRYSTAL
                    }
                    TrackId.GUMDROP_GARDEN -> when (rng.nextInt(10)) {
                        0, 1 -> DecoType.LOLLIPOP_TREE
                        2, 3 -> DecoType.GUMDROP_HILL
                        4 -> DecoType.GUMMY_BEAR_STATUE
                        5 -> DecoType.COTTON_CANDY_TREE
                        6 -> DecoType.MARSHMALLOW_BUSH
                        7 -> DecoType.SUGAR_CRYSTAL
                        8 -> DecoType.GUMDROP_TREE
                        else -> DecoType.FLOATING_BALLOON
                    }
                }

                val scale = when (type) {
                    DecoType.GINGERBREAD_HOUSE, DecoType.CUPCAKE_TOWER,
                    DecoType.CHOCOLATE_CASTLE, DecoType.DONUT_BUILDING,
                    DecoType.COOKIE_HOUSE, DecoType.GUMDROP_HILL -> 1.1f + rng.nextFloat() * 0.7f
                    DecoType.GUMMY_BEAR_STATUE, DecoType.CHOCOLATE_WATERFALL -> 1.2f + rng.nextFloat() * 0.5f
                    else -> 0.85f + rng.nextFloat() * 0.75f
                }

                val finalY = if (type == DecoType.FLOATING_BALLOON) groundY + 11f + rng.nextFloat() * 9f else groundY

                decos.add(
                    DecorationInstance(
                        type = type,
                        pos = Vec3(placedPos.x, finalY, placedPos.z),
                        rotY = rng.nextFloat() * (2f * PI.toFloat()),
                        scale = scale,
                        primaryColor = c1,
                        secondaryColor = c2,
                        bobPhase = rng.nextFloat() * 6.28f
                    )
                )
            }
        }

        // Far landmark giants so the horizon always looks like a rich Sugar Rush kingdom!
        val landmarkSpots = listOf(
            Vec3(95f, 0f, 35f),
            Vec3(105f, 0f, -95f),
            Vec3(-65f, 0f, 90f),
            Vec3(270f, 0f, 20f),
            Vec3(85f, 0f, 255f),
            Vec3(95f, 0f, -260f)
        )
        for ((idx, spot) in landmarkSpots.withIndex()) {
            if (isTooCloseToAnyRoad(spot, nodes, -1, minDist = 20f)) continue
            val type = when (id) {
                TrackId.CHOCOLATE_VOLCANO -> if (idx % 2 == 0) DecoType.CHOCOLATE_CASTLE else DecoType.CHOCOLATE_WATERFALL
                TrackId.GUMDROP_GARDEN -> if (idx % 2 == 0) DecoType.GUMMY_BEAR_STATUE else DecoType.GUMDROP_HILL
                TrackId.CUPCAKE_COAST -> if (idx % 2 == 0) DecoType.CUPCAKE_TOWER else DecoType.STRAWBERRY_FOUNTAIN
                TrackId.PEPPERMINT_PLAZA -> if (idx % 2 == 0) DecoType.CHOCOLATE_CASTLE else DecoType.CUPCAKE_TOWER
            }
            decos.add(
                DecorationInstance(
                    type = type,
                    pos = spot,
                    rotY = idx * 1.1f,
                    scale = 2.4f,
                    primaryColor = candyColors[idx % candyColors.size],
                    secondaryColor = candyColors[(idx + 2) % candyColors.size],
                    bobPhase = idx.toFloat()
                )
            )
        }

        return decos
    }

    private fun isTooCloseToAnyRoad(
        pos: Vec3,
        nodes: List<TrackNode>,
        skipIndex: Int,
        minDist: Float
    ): Boolean {
        val minSq = minDist * minDist
        for (i in nodes.indices) {
            if (abs(i - skipIndex) < 4) continue
            val c = nodes[i].center
            val dx = pos.x - c.x
            val dz = pos.z - c.z
            if (dx * dx + dz * dz < minSq) return true
        }
        return false
    }

    private fun sampleClosedCatmullRom(points: List<Vec3>, samplesPerSeg: Int): List<Vec3> {
        val out = ArrayList<Vec3>(points.size * samplesPerSeg)
        val count = points.size
        for (i in 0 until count) {
            val p0 = points[(i - 1 + count) % count]
            val p1 = points[i]
            val p2 = points[(i + 1) % count]
            val p3 = points[(i + 2) % count]
            for (s in 0 until samplesPerSeg) {
                val t = s.toFloat() / samplesPerSeg.toFloat()
                out.add(catmullRom(p0, p1, p2, p3, t))
            }
        }
        return out
    }

    private fun catmullRom(p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3, t: Float): Vec3 {
        val t2 = t * t
        val t3 = t2 * t
        val x = 0.5f * ((2f * p1.x) +
            (-p0.x + p2.x) * t +
            (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
            (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3)
        val y = 0.5f * ((2f * p1.y) +
            (-p0.y + p2.y) * t +
            (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
            (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3)
        val z = 0.5f * ((2f * p1.z) +
            (-p0.z + p2.z) * t +
            (2f * p0.z - 5f * p1.z + 4f * p2.z - p3.z) * t2 +
            (-p0.z + 3f * p1.z - 3f * p2.z + p3.z) * t3)
        return Vec3(x, y, z)
    }
}
