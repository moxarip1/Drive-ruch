package com.example.sweetrush.render3d

import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.ExhaustStyle
import com.example.sweetrush.model.KartBodyStyle
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.KartDecoration
import com.example.sweetrush.model.KartSpec
import com.example.sweetrush.model.RacerSpec
import com.example.sweetrush.model.WheelDesign
import com.example.sweetrush.render3d.MeshAccumulator.LoftStation
import com.example.sweetrush.track.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class KartModelBundle(
    val chassisMesh: GpuMesh,
    val steeringWheelMesh: GpuMesh,
    val driverMesh: GpuMesh,
    val brakeLightMesh: GpuMesh,
    val tireMesh: GpuMesh,
    val rimMesh: GpuMesh,
    val suspensionMesh: GpuMesh,
    val shadowBlobMesh: GpuMesh
) {
    fun release() {
        chassisMesh.release()
        steeringWheelMesh.release()
        driverMesh.release()
        brakeLightMesh.release()
        tireMesh.release()
        rimMesh.release()
        suspensionMesh.release()
        shadowBlobMesh.release()
    }
}

/**
 * Builds rich, movie-inspired 3D Sugar Rush Bakery Karts constructed out of authentic
 * layered confectioneries:
 * - Wafer-biscuit & cream sandwich chassis undertrays
 * - Piped whipped-cream frosting scallops, 3D rainbow sugar sprinkles, molded chocolate squares,
 *   fluted cupcake liners, glazed donut rings, toasted marshmallow pontoons, and candy-cane frames
 * - Custom edible wheels (Peppermint Swirl, Choco-Chip Cookie, Frosted Donut Ring, Neon Gummy Glow, Gold Caramel)
 * - Custom 3D confectionery exhaust assemblies (Sugar Spark, Mint Candy-Cane J-Pipes, Caramel Wafer Quads, Rainbow Turbo Jets)
 * - Custom 3D Bakery Toppers (Twin Cherries, Candy-Cane Bi-Wing, Spiral Lollipops, Royal Sugar Crown)
 * - Sugar-glass cockpit windscreen, candy-button gauge cluster, pretzel/candy steering wheel & animated racer
 */
object VehicleMeshBuilder {

    private val SPRINKLE_PALETTE = longArrayOf(
        0xFFFF3D8BL,
        0xFF3CE8B5L,
        0xFFFFD54FL,
        0xFF5CE1E6L,
        0xFFFF85C2L,
        0xFFFFFFFFL,
        0xFFB388FFL
    )

    fun buildKartBundle(
        kart: KartSpec,
        racer: RacerSpec,
        custom: KartCustomization
    ): KartModelBundle {
        val chassisMesh = buildDetailedChassis(kart, custom)
        val steeringWheelMesh = buildSteeringWheelMesh(kart, custom)
        val driverMesh = buildDriverMesh(racer)
        val brakeLightMesh = buildBrakeLights(kart)
        val tireMesh = buildSculptedTireMesh(custom.wheelDesign)
        val rimMesh = buildAlloyRimMesh(custom.wheelDesign)
        val suspensionMesh = buildSuspensionAssemblyMesh(kart)
        val shadowBlobMesh = MeshAccumulator().apply {
            addShadowOval(rx = 1.54f, rz = 2.02f, segs = 18, alpha = 0.46f)
        }.build()

        return KartModelBundle(
            chassisMesh = chassisMesh,
            steeringWheelMesh = steeringWheelMesh,
            driverMesh = driverMesh,
            brakeLightMesh = brakeLightMesh,
            tireMesh = tireMesh,
            rimMesh = rimMesh,
            suspensionMesh = suspensionMesh,
            shadowBlobMesh = shadowBlobMesh
        )
    }

    private fun buildDetailedChassis(
        kart: KartSpec,
        custom: KartCustomization
    ): GpuMesh {
        val acc = MeshAccumulator()
        val paint = CatalogData.paintById(custom.paintId)
        val primary = if (custom.paintId == "default") kart.defaultPrimaryHex else paint.primaryHex
        val secondary = if (custom.paintId == "default") kart.defaultSecondaryHex else paint.secondaryHex
        val accent = kart.accentHex
        val darkCocoa = 0xFF281524L
        val waferGold = 0xFFE6A95CL
        val creamWhite = 0xFFFFF8F2L
        val chrome = 0xFFEAF0F6L

        // 1. Multi-layer Bakery Wafer-Sandwich Chassis Bed (Wafer -> Cream Filling -> Dark Cocoa Frame)
        acc.addBeveledBox(0f, 0.30f, 0.04f, 1.44f, 0.08f, 2.82f, 0.94f, 0.94f, darkCocoa, darkCocoa, CandyMaterials.MAT_CARAMEL)
        acc.addBeveledBox(0f, 0.35f, 0.04f, 1.48f, 0.06f, 2.76f, 0.95f, 0.95f, waferGold, secondary, CandyMaterials.MAT_WAFER)
        acc.addBeveledBox(0f, 0.39f, 0.04f, 1.40f, 0.05f, 2.70f, 0.95f, 0.95f, creamWhite, secondary, CandyMaterials.MAT_MARSHMALLOW)
        // Side wafer cookie step bars
        acc.addBeveledBox(-0.78f, 0.36f, -0.04f, 0.16f, 0.10f, 1.64f, 0.92f, 0.92f, waferGold, darkCocoa, CandyMaterials.MAT_WAFER)
        acc.addBeveledBox(0.78f, 0.36f, -0.04f, 0.16f, 0.10f, 1.64f, 0.92f, 0.92f, waferGold, darkCocoa, CandyMaterials.MAT_WAFER)

        when (kart.style) {
            KartBodyStyle.STRAWBERRY_ROCKET -> {
                // Sculpted glossy strawberry-glazed monocoque with chocolate-dipped nose tip
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.50f, 0.66f, 0.36f, 0.88f, primary, secondary),
                        LoftStation(-0.58f, 0.78f, 0.36f, 0.92f, primary, secondary),
                        LoftStation(0.42f, 0.72f, 0.36f, 0.86f, primary, primary),
                        LoftStation(1.25f, 0.54f, 0.38f, 0.72f, secondary, primary),
                        LoftStation(1.78f, 0.26f, 0.40f, 0.54f, darkCocoa, darkCocoa)
                    ),
                    matType = CandyMaterials.MAT_CANDY
                )
                // Golden strawberry seed dimples along flanks & hood
                val seedCoords = listOf(
                    Vec3(-0.62f, 0.68f, 0.45f), Vec3(0.62f, 0.68f, 0.45f),
                    Vec3(-0.56f, 0.64f, 0.85f), Vec3(0.56f, 0.64f, 0.85f),
                    Vec3(-0.68f, 0.70f, -0.35f), Vec3(0.68f, 0.70f, -0.35f),
                    Vec3(-0.30f, 0.78f, 0.95f), Vec3(0.30f, 0.78f, 0.95f),
                    Vec3(0f, 0.72f, 1.26f)
                )
                for (s in seedCoords) {
                    acc.addSphere(s.x, s.y, s.z, 0.045f, 0.035f, 0.065f, 4, 6, 0xFFFFE066L, matType = CandyMaterials.MAT_CHROME)
                }
                // Waffle-cone side turbo intakes with whipped strawberry cream swirl cores
                for (side in listOf(-1f, 1f)) {
                    val sx = side * 0.78f
                    acc.addHorizontalCylinderZ(sx, 0.58f, -0.14f, 0.22f, 0.16f, 1.28f, 10, waferGold, secondary, CandyMaterials.MAT_WAFER)
                    acc.addSphere(sx, 0.58f, 0.50f, 0.18f, 0.18f, 0.12f, 5, 8, creamWhite, secondary, CandyMaterials.MAT_MARSHMALLOW)
                    addPipedFrostingLine(acc, Vec3(sx, 0.78f, -0.68f), Vec3(sx, 0.76f, 0.42f), 6, 0.085f, creamWhite, secondary)
                }
                // Green strawberry leaf calyx crown behind cockpit
                for (leafAngle in listOf(-0.55f, -0.22f, 0f, 0.22f, 0.55f)) {
                    acc.addBeveledBox(
                        sin(leafAngle) * 0.42f,
                        0.86f,
                        -0.88f - cos(leafAngle) * 0.18f,
                        0.22f,
                        0.08f,
                        0.44f,
                        0.35f,
                        0.50f,
                        0xFF2ECC71L,
                        0xFF69F0AEL,
                        CandyMaterials.MAT_CANDY
                    )
                }
                // Wafer & frosting front splitter bumper with piped cream puffs
                acc.addBeveledBox(0f, 0.36f, 1.68f, 1.58f, 0.14f, 0.38f, 0.92f, 0.78f, waferGold, creamWhite, CandyMaterials.MAT_WAFER)
                addPipedFrostingLine(acc, Vec3(-0.64f, 0.51f, 1.72f), Vec3(0.64f, 0.51f, 1.72f), 7, 0.075f, creamWhite, primary)
                // Twin strawberry-wafer rocket fins & bi-plane biscuit spoiler
                acc.addBeveledBox(-0.70f, 0.88f, -1.18f, 0.12f, 0.56f, 0.70f, 0.9f, 0.72f, secondary, accent, CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0.70f, 0.88f, -1.18f, 0.12f, 0.56f, 0.70f, 0.9f, 0.72f, secondary, accent, CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0f, 1.20f, -1.26f, 1.62f, 0.10f, 0.42f, 0.95f, 0.85f, waferGold, primary, CandyMaterials.MAT_WAFER)
                addPipedFrostingLine(acc, Vec3(-0.72f, 1.31f, -1.26f), Vec3(0.72f, 1.31f, -1.26f), 8, 0.065f, creamWhite, secondary)
                acc.addBeveledBox(0f, 0.38f, -1.54f, 1.48f, 0.18f, 0.24f, 0.9f, 0.85f, darkCocoa, matType = CandyMaterials.MAT_CARAMEL)
            }

            KartBodyStyle.PEPPERMINT_RACER -> {
                // Open-wheel Sugar Rush Formula-1 candy-cane racer with striped peppermint fuselage
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.46f, 0.52f, 0.35f, 0.90f, primary, secondary),
                        LoftStation(-0.80f, 0.64f, 0.34f, 0.86f, secondary, primary),
                        LoftStation(-0.10f, 0.62f, 0.34f, 0.82f, primary, secondary),
                        LoftStation(0.58f, 0.46f, 0.36f, 0.76f, secondary, primary),
                        LoftStation(1.22f, 0.32f, 0.38f, 0.62f, primary, secondary),
                        LoftStation(1.80f, 0.16f, 0.38f, 0.48f, secondary, primary)
                    ),
                    matType = CandyMaterials.MAT_CANDY
                )
                // Candy-cane striped sidepods & tubular spaceframe bars
                acc.addBeveledBox(-0.64f, 0.50f, -0.22f, 0.46f, 0.34f, 1.20f, 0.82f, 0.78f, secondary, primary, CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0.64f, 0.50f, -0.22f, 0.46f, 0.34f, 1.20f, 0.82f, 0.78f, secondary, primary, CandyMaterials.MAT_CANDY)
                addCandyCaneTube(acc, Vec3(-0.76f, 0.68f, -0.82f), Vec3(-0.48f, 0.62f, 0.88f), 0.055f, 8, primary, creamWhite)
                addCandyCaneTube(acc, Vec3(0.76f, 0.68f, -0.82f), Vec3(0.48f, 0.62f, 0.88f), 0.055f, 8, primary, creamWhite)
                // Swirled Peppermint Disc badges on sidepods & nose
                addPeppermintDisc(acc, -0.88f, 0.66f, -0.20f, 0.22f, 0.04f, primary, creamWhite, verticalX = true)
                addPeppermintDisc(acc, 0.88f, 0.66f, -0.20f, 0.22f, 0.04f, primary, creamWhite, verticalX = true)
                // Multi-element wafer & peppermint front F1 wing with round peppermint disc endplates
                acc.addBeveledBox(0f, 0.33f, 1.68f, 1.80f, 0.08f, 0.40f, 0.98f, 0.85f, creamWhite, primary, CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0f, 0.41f, 1.62f, 1.64f, 0.06f, 0.26f, 0.96f, 0.85f, primary, accent, CandyMaterials.MAT_CANDY)
                addPeppermintDisc(acc, -0.92f, 0.46f, 1.66f, 0.22f, 0.05f, primary, creamWhite, verticalX = true)
                addPeppermintDisc(acc, 0.92f, 0.46f, 1.66f, 0.22f, 0.05f, primary, creamWhite, verticalX = true)
                // Overhead Mint Airbox Scoop + Candy-Cane Rear Wing Struts & Peppermint Endplates
                acc.addHorizontalCylinderZ(0f, 1.24f, -0.58f, 0.18f, 0.16f, 0.56f, 10, primary, creamWhite, CandyMaterials.MAT_CANDY)
                addCandyCaneTube(acc, Vec3(-0.34f, 0.82f, -1.24f), Vec3(-0.46f, 1.28f, -1.32f), 0.05f, 6, primary, creamWhite)
                addCandyCaneTube(acc, Vec3(0.34f, 0.82f, -1.24f), Vec3(0.46f, 1.28f, -1.32f), 0.05f, 6, primary, creamWhite)
                acc.addBeveledBox(0f, 1.28f, -1.34f, 1.68f, 0.10f, 0.44f, 0.95f, 0.85f, creamWhite, primary, CandyMaterials.MAT_CANDY)
                addPeppermintDisc(acc, -0.86f, 1.28f, -1.34f, 0.24f, 0.05f, primary, creamWhite, verticalX = true)
                addPeppermintDisc(acc, 0.86f, 1.28f, -1.34f, 0.24f, 0.05f, primary, creamWhite, verticalX = true)
                acc.addBeveledBox(0f, 0.36f, -1.52f, 1.32f, 0.16f, 0.24f, 0.9f, 0.85f, darkCocoa, matType = CandyMaterials.MAT_CARAMEL)
            }

            KartBodyStyle.CHOCOLATE_CRUISER -> {
                // Muscular Fudge-Brownie & Wafer Muscle Kart with 3D Molded Chocolate Bar Hood Squares
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.50f, 0.84f, 0.36f, 0.94f, primary, secondary),
                        LoftStation(-0.50f, 0.88f, 0.34f, 0.90f, primary, primary),
                        LoftStation(0.52f, 0.86f, 0.34f, 0.88f, primary, secondary),
                        LoftStation(1.40f, 0.80f, 0.36f, 0.78f, primary, secondary),
                        LoftStation(1.70f, 0.68f, 0.38f, 0.66f, secondary, primary)
                    ),
                    matType = CandyMaterials.MAT_CARAMEL
                )
                // Wafer-cookie layered stripe along both body sides
                acc.addBeveledBox(-0.84f, 0.56f, 0.08f, 0.12f, 0.16f, 2.55f, 0.92f, 0.95f, waferGold, creamWhite, CandyMaterials.MAT_WAFER)
                acc.addBeveledBox(0.84f, 0.56f, 0.08f, 0.12f, 0.16f, 2.55f, 0.92f, 0.95f, waferGold, creamWhite, CandyMaterials.MAT_WAFER)
                // 3D Molded Chocolate Bar Squares on Hood & Rear Deck
                for (row in 0..1) {
                    for (col in -1..1) {
                        val sqX = col * 0.34f
                        val sqZ = 0.82f + row * 0.38f
                        acc.addBeveledBox(sqX, 0.78f, sqZ, 0.28f, 0.10f, 0.30f, 0.76f, 0.76f, primary, secondary, CandyMaterials.MAT_CARAMEL)
                    }
                }
                // Supercharged Wafer & Caramel Triple-Barrel Blower Scoop
                acc.addBeveledBox(0f, 0.88f, 0.72f, 0.62f, 0.24f, 0.68f, 0.85f, 0.80f, waferGold, accent, CandyMaterials.MAT_WAFER)
                for (bx in listOf(-0.18f, 0f, 0.18f)) {
                    acc.addHorizontalCylinderZ(bx, 1.02f, 1.06f, 0.075f, 0.075f, 0.12f, 8, chrome, accent, CandyMaterials.MAT_CHROME)
                }
                // Melted Golden Caramel Drip Valance along front & rear fenders
                addPipedFrostingLine(acc, Vec3(-0.76f, 0.76f, -1.20f), Vec3(-0.76f, 0.72f, 1.30f), 9, 0.08f, accent, waferGold)
                addPipedFrostingLine(acc, Vec3(0.76f, 0.76f, -1.20f), Vec3(0.76f, 0.72f, 1.30f), 9, 0.08f, accent, waferGold)
                // Chocolate & Wafer Bullbars + Caramel Ducktail Spoiler
                acc.addBeveledBox(0f, 0.42f, 1.72f, 1.70f, 0.24f, 0.28f, 0.95f, 0.82f, chrome, accent, CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 0.42f, -1.58f, 1.70f, 0.24f, 0.26f, 0.95f, 0.85f, chrome, darkCocoa, CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 1.06f, -1.36f, 1.68f, 0.18f, 0.38f, 0.94f, 0.80f, waferGold, accent, CandyMaterials.MAT_WAFER)
                addCandySprinkles(acc, 0f, 1.25f, -1.36f, 0.68f, 0.12f, count = 12)
            }

            KartBodyStyle.JELLYBEAN_MACHINE -> {
                // Translucent Glossy Jellybean Capsule with Rainbow Gummy-Worm Headers & Citrus Grille
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.44f, 0.68f, 0.38f, 0.90f, secondary, primary),
                        LoftStation(-0.35f, 0.84f, 0.36f, 0.96f, primary, primary),
                        LoftStation(0.65f, 0.76f, 0.36f, 0.86f, primary, secondary),
                        LoftStation(1.56f, 0.46f, 0.40f, 0.66f, secondary, primary)
                    ),
                    matType = CandyMaterials.MAT_GUMMY
                )
                // Translucent Bubble Canopy Hood Dome
                acc.addSphere(0f, 0.86f, 0.66f, 0.56f, 0.26f, 0.72f, 7, 12, 0xFF80FFE5L, primary, matType = CandyMaterials.MAT_GUMMY)
                // Coiled Rainbow Gummy-Worm Side Tubes along both flanks
                val gummyColors = longArrayOf(0xFFFF3D8BL, 0xFFFFD54FL, 0xFF3CE8B5L, 0xFF5CE1E6L)
                for (side in listOf(-1f, 1f)) {
                    for (seg in 0 until 7) {
                        val z = -1.05f + seg * 0.32f
                        val yWave = 0.62f + sin(seg * 0.95f) * 0.06f
                        val c1 = gummyColors[seg % gummyColors.size]
                        val c2 = gummyColors[(seg + 1) % gummyColors.size]
                        acc.addSphere(side * 0.78f, yWave, z, 0.13f, 0.12f, 0.20f, 5, 8, c1, c2, matType = CandyMaterials.MAT_GUMMY)
                    }
                }
                // Jellybean Nose Grille & Sour-Ribbon Rear Wing
                addPeppermintDisc(acc, 0f, 0.54f, 1.58f, 0.26f, 0.06f, accent, primary, verticalX = false)
                acc.addBeveledBox(0f, 0.38f, 1.58f, 1.52f, 0.16f, 0.28f, 0.9f, 0.8f, chrome, accent, CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 0.38f, -1.50f, 1.46f, 0.18f, 0.24f, 0.9f, 0.8f, darkCocoa, matType = CandyMaterials.MAT_CARAMEL)
                acc.addStrut(Vec3(-0.42f, 0.84f, -1.20f), Vec3(-0.50f, 1.16f, -1.28f), 0.05f, 6, secondary, CandyMaterials.MAT_GUMMY)
                acc.addStrut(Vec3(0.42f, 0.84f, -1.20f), Vec3(0.50f, 1.16f, -1.28f), 0.05f, 6, secondary, CandyMaterials.MAT_GUMMY)
                acc.addBeveledBox(0f, 1.14f, -1.28f, 1.56f, 0.10f, 0.38f, 0.92f, 0.85f, accent, primary, matType = CandyMaterials.MAT_GUMMY)
            }

            KartBodyStyle.CARAMEL_THUNDER -> {
                // Golden Molten-Toffee Hyper-Kart with Honeycomb Waffle Intakes & Dripped Caramel Fenders
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.48f, 0.78f, 0.34f, 0.88f, primary, secondary),
                        LoftStation(-0.40f, 0.84f, 0.34f, 0.86f, primary, primary),
                        LoftStation(0.60f, 0.76f, 0.34f, 0.78f, primary, accent),
                        LoftStation(1.44f, 0.58f, 0.34f, 0.58f, secondary, primary),
                        LoftStation(1.78f, 0.34f, 0.36f, 0.44f, accent, secondary)
                    ),
                    matType = CandyMaterials.MAT_CARAMEL
                )
                // Waffle-Biscuit Side Aero Pods with Molten Toffee Trim
                acc.addBeveledBox(-0.82f, 0.54f, 0.02f, 0.28f, 0.26f, 1.48f, 0.86f, 0.84f, waferGold, secondary, CandyMaterials.MAT_WAFER)
                acc.addBeveledBox(0.82f, 0.54f, 0.02f, 0.28f, 0.26f, 1.48f, 0.86f, 0.84f, waferGold, secondary, CandyMaterials.MAT_WAFER)
                // Flared Caramel Front Fenders & Butterscotch Splitter Blades
                acc.addBeveledBox(-0.84f, 0.62f, 0.94f, 0.30f, 0.24f, 0.76f, 0.85f, 0.85f, accent, primary, CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0.84f, 0.62f, 0.94f, 0.30f, 0.24f, 0.76f, 0.85f, 0.85f, accent, primary, CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0f, 0.34f, 1.74f, 1.70f, 0.10f, 0.36f, 0.96f, 0.82f, chrome, accent, CandyMaterials.MAT_CHROME)
                // Twin Caramel-Swirl Roof Turbines & Dual-Element Toffee Hyper-Wing
                acc.addHorizontalCylinderZ(-0.26f, 0.92f, -0.88f, 0.14f, 0.11f, 0.58f, 8, accent, secondary, CandyMaterials.MAT_CARAMEL)
                acc.addHorizontalCylinderZ(0.26f, 0.92f, -0.88f, 0.14f, 0.11f, 0.58f, 8, accent, secondary, CandyMaterials.MAT_CARAMEL)
                acc.addStrut(Vec3(-0.46f, 0.82f, -1.28f), Vec3(-0.58f, 1.24f, -1.36f), 0.05f, 6, chrome)
                acc.addStrut(Vec3(0.46f, 0.82f, -1.28f), Vec3(0.58f, 1.24f, -1.36f), 0.05f, 6, chrome)
                acc.addBeveledBox(0f, 1.24f, -1.36f, 1.76f, 0.10f, 0.44f, 0.96f, 0.85f, accent, primary, CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0f, 1.36f, -1.40f, 1.48f, 0.07f, 0.30f, 0.94f, 0.85f, waferGold, accent, CandyMaterials.MAT_WAFER)
                acc.addBeveledBox(0f, 0.38f, -1.54f, 1.56f, 0.18f, 0.24f, 0.9f, 0.85f, darkCocoa, matType = CandyMaterials.MAT_CARAMEL)
            }

            KartBodyStyle.CUPCAKE_KART -> {
                // Fluted Pastel Cupcake-Liner Tub + Puffed Sponge Cake + Swirled Whipped-Cream Dome + Sprinkles & Cherry
                acc.addCylinder(0f, 0.34f, 0.04f, 0.80f, 1.00f, 0.54f, 16, primary, secondary, matType = CandyMaterials.MAT_WAFER)
                // 16 Pleated Cupcake Paper Liner Ribs around the lower tub
                val pleats = 16
                for (i in 0 until pleats) {
                    val angle = (i.toFloat() / pleats) * (2f * PI.toFloat())
                    val px = cos(angle) * 0.92f
                    val pz = 0.04f + sin(angle) * 0.98f
                    val ribHex = if (i % 2 == 0) primary else secondary
                    acc.addCylinder(px, 0.35f, pz, 0.07f, 0.095f, 0.52f, 6, ribHex, creamWhite, matType = CandyMaterials.MAT_WAFER)
                }
                // Golden Puffed Sponge Cake Upper Deck & Swirled Frosting Collars
                acc.addSphere(0f, 0.88f, 0.18f, 0.92f, 0.32f, 1.08f, 7, 14, secondary, creamWhite, matType = CandyMaterials.MAT_MARSHMALLOW)
                // Multi-Tier Swirled Whipped-Cream Frosting Rear Engine Mound
                acc.addSphere(0f, 1.02f, -0.82f, 0.56f, 0.24f, 0.56f, 6, 12, creamWhite, secondary, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(0f, 1.18f, -0.84f, 0.40f, 0.22f, 0.40f, 6, 10, secondary, creamWhite, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(0f, 1.36f, -0.86f, 0.22f, 0.18f, 0.22f, 5, 8, accent, matType = CandyMaterials.MAT_CANDY)
                // Rainbow Sugar Sprinkles scattered across the cupcake frosting dome
                addCandySprinkles(acc, 0f, 1.08f, 0.42f, 0.62f, 0.42f, count = 18)
                addCandySprinkles(acc, 0f, 1.20f, -0.82f, 0.44f, 0.36f, count = 14)
                // Rolled Wafer-Stick Front & Rear Bumpers + Frosting Spoiler
                acc.addBeveledBox(0f, 0.36f, 1.52f, 1.48f, 0.14f, 0.30f, 0.9f, 0.8f, waferGold, accent, CandyMaterials.MAT_WAFER)
                addPipedFrostingLine(acc, Vec3(-0.58f, 0.50f, 1.54f), Vec3(0.58f, 0.50f, 1.54f), 7, 0.075f, creamWhite, primary)
                acc.addBeveledBox(0f, 0.36f, -1.42f, 1.44f, 0.16f, 0.24f, 0.9f, 0.8f, darkCocoa, matType = CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0f, 1.16f, -1.26f, 1.48f, 0.10f, 0.34f, 0.92f, 0.85f, creamWhite, secondary, matType = CandyMaterials.MAT_MARSHMALLOW)
            }

            KartBodyStyle.DONUT_DRIFTER -> {
                // Giant Golden-Fried Donut Torus Body + Scalloped Pink Glaze Icing + 24 Rainbow Sprinkles
                acc.addTorus(0f, 0.58f, 0.06f, 0.92f, 0.38f, 20, 10, waferGold, primary, matType = CandyMaterials.MAT_WAFER)
                // Upper Glossy Strawberry/Chocolate Glaze Icing Ring
                acc.addTorus(0f, 0.72f, 0.06f, 0.90f, 0.26f, 20, 8, primary, secondary, matType = CandyMaterials.MAT_CANDY)
                // Scalloped Icing Drips around the outer donut perimeter
                val dripCount = 18
                for (i in 0 until dripCount) {
                    val a = (i.toFloat() / dripCount) * (2f * PI.toFloat())
                    val dx = cos(a) * 1.10f
                    val dz = 0.06f + sin(a) * 1.12f
                    val dropY = 0.62f + (i % 2) * 0.05f
                    acc.addSphere(dx, dropY, dz, 0.11f, 0.14f, 0.11f, 5, 6, primary, secondary, matType = CandyMaterials.MAT_CANDY)
                }
                // 24 3D Rainbow Sprinkles embedded into the top donut glaze ring
                val sprinkleCount = 24
                for (i in 0 until sprinkleCount) {
                    val a = (i.toFloat() / sprinkleCount) * (2f * PI.toFloat())
                    val rad = 0.78f + (i % 3) * 0.09f
                    val sx = cos(a) * rad
                    val sz = 0.06f + sin(a) * rad
                    val col = SPRINKLE_PALETTE[i % SPRINKLE_PALETTE.size]
                    acc.addBox(sx, 0.96f, sz, 0.11f, 0.045f, 0.045f, col, matType = CandyMaterials.MAT_CANDY)
                }
                // Powdered-Sugar Front Drift Splitter & Mini-Donut Rear Spoiler
                acc.addBeveledBox(0f, 0.36f, 1.56f, 1.54f, 0.14f, 0.30f, 0.92f, 0.8f, chrome, accent, CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 0.36f, -1.46f, 1.48f, 0.16f, 0.24f, 0.92f, 0.8f, darkCocoa, matType = CandyMaterials.MAT_CARAMEL)
                acc.addStrut(Vec3(-0.42f, 0.84f, -1.16f), Vec3(-0.50f, 1.16f, -1.26f), 0.05f, 6, waferGold, CandyMaterials.MAT_WAFER)
                acc.addStrut(Vec3(0.42f, 0.84f, -1.16f), Vec3(0.50f, 1.16f, -1.26f), 0.05f, 6, waferGold, CandyMaterials.MAT_WAFER)
                acc.addBeveledBox(0f, 1.16f, -1.26f, 1.56f, 0.10f, 0.40f, 0.94f, 0.85f, secondary, primary, CandyMaterials.MAT_CANDY)
                addCandySprinkles(acc, 0f, 1.27f, -1.26f, 0.66f, 0.14f, count = 10)
            }

            KartBodyStyle.MARSHMALLOW_SPEEDSTER -> {
                // Quilted Toasted-Marshmallow Twin Pontoons + S'more Graham-Chocolate Deck + Cotton-Candy Aero Nose
                for (side in listOf(-1f, 1f)) {
                    val px = side * 0.60f
                    acc.addHorizontalCylinderZ(px, 0.58f, 0.08f, 0.34f, 0.30f, 2.62f, 14, primary, secondary, CandyMaterials.MAT_MARSHMALLOW)
                    // Quilted marshmallow puff rings along each pontoon
                    for (puff in -2..2) {
                        val pz = 0.08f + puff * 0.46f
                        acc.addSphere(px, 0.60f, pz, 0.36f, 0.32f, 0.24f, 6, 10, creamWhite, secondary, CandyMaterials.MAT_MARSHMALLOW)
                    }
                    // Golden toasted marshmallow nose cap
                    acc.addSphere(px, 0.58f, 1.40f, 0.31f, 0.28f, 0.22f, 6, 10, waferGold, creamWhite, CandyMaterials.MAT_CARAMEL)
                }
                // S'more Graham Cracker & Melted Chocolate Center Bridge Deck
                acc.addBeveledBox(0f, 0.46f, 0.08f, 1.22f, 0.14f, 2.30f, 0.92f, 0.88f, waferGold, waferGold, CandyMaterials.MAT_WAFER)
                acc.addBeveledBox(0f, 0.58f, 0.08f, 1.16f, 0.12f, 2.22f, 0.90f, 0.86f, darkCocoa, secondary, CandyMaterials.MAT_CARAMEL)
                // Fluffy Spun Cotton-Candy Nose Fairing
                acc.addSphere(0f, 0.72f, 1.02f, 0.48f, 0.24f, 0.58f, 6, 10, secondary, accent, CandyMaterials.MAT_MARSHMALLOW)
                addPipedFrostingLine(acc, Vec3(-0.56f, 0.88f, -0.95f), Vec3(0.56f, 0.88f, -0.95f), 7, 0.09f, creamWhite, accent)
                acc.addBeveledBox(0f, 0.38f, 1.60f, 1.56f, 0.14f, 0.28f, 0.92f, 0.8f, chrome, primary, CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 1.16f, -1.24f, 1.54f, 0.12f, 0.38f, 0.92f, 0.85f, accent, creamWhite, CandyMaterials.MAT_MARSHMALLOW)
            }
        }

        // 2. Sculpted Candy Wheel Fenders with Piped Frosting Trim (except Open-Wheel Peppermint F1)
        if (kart.style != KartBodyStyle.PEPPERMINT_RACER) {
            val archMat = when (kart.style) {
                KartBodyStyle.JELLYBEAN_MACHINE -> CandyMaterials.MAT_GUMMY
                KartBodyStyle.CARAMEL_THUNDER, KartBodyStyle.CHOCOLATE_CRUISER -> CandyMaterials.MAT_CARAMEL
                else -> CandyMaterials.MAT_CANDY
            }
            for ((wx, wz) in listOf(-0.86f to 0.96f, 0.86f to 0.96f, -0.90f to -0.96f, 0.90f to -0.96f)) {
                acc.addBeveledBox(wx, 0.90f, wz, 0.38f, 0.09f, 0.72f, 0.85f, 0.78f, secondary, accent, archMat)
                acc.addBeveledBox(wx, 0.70f, wz - 0.35f, 0.32f, 0.32f, 0.10f, 0.90f, 0.85f, darkCocoa, matType = CandyMaterials.MAT_CARAMEL)
                // Mini frosting bead accent on each fender crest
                acc.addSphere(wx, 1.00f, wz, 0.12f, 0.04f, 0.24f, 4, 6, creamWhite, accent, CandyMaterials.MAT_MARSHMALLOW)
            }
        }

        // 3. Sugar-Crystal Headlights with Frosted Candy Bezels
        val headlightZ = when (kart.style) {
            KartBodyStyle.CHOCOLATE_CRUISER, KartBodyStyle.CARAMEL_THUNDER -> 1.68f
            else -> 1.52f
        }
        for (hx in listOf(-0.46f, 0.46f)) {
            acc.addHorizontalCylinderZ(hx, 0.58f, headlightZ - 0.03f, 0.16f, 0.15f, 0.12f, 10, chrome, accent, CandyMaterials.MAT_CHROME)
            acc.addSphere(hx, 0.58f, headlightZ + 0.02f, 0.13f, 0.11f, 0.09f, 6, 8, 0xFFFFF9D2L, matType = CandyMaterials.MAT_EMISSIVE)
        }

        // 4. Bakery Cockpit: Sugar-Glass Aero Windscreen + Candy-Button Dashboard + Biscuit Bucket Seat + Candy Roll Hoop
        acc.addBeveledBox(0f, 0.72f, -0.14f, 0.78f, 0.24f, 0.84f, 0.86f, 0.86f, darkCocoa, matType = CandyMaterials.MAT_CARAMEL)
        // Quilted marshmallow & biscuit bucket seat with side bolsters
        acc.addBeveledBox(0f, 1.00f, -0.46f, 0.64f, 0.54f, 0.18f, 0.82f, 0.85f, waferGold, accent, CandyMaterials.MAT_WAFER)
        acc.addBeveledBox(0f, 1.32f, -0.46f, 0.36f, 0.18f, 0.16f, 0.82f, 0.82f, creamWhite, secondary, CandyMaterials.MAT_MARSHMALLOW)
        acc.addBox(-0.31f, 0.92f, -0.26f, 0.10f, 0.36f, 0.44f, accent)
        acc.addBox(0.31f, 0.92f, -0.26f, 0.10f, 0.36f, 0.44f, accent)
        // Candy-Cane Roll Bar behind seat
        addCandyCaneTube(acc, Vec3(-0.28f, 1.18f, -0.52f), Vec3(0f, 1.52f, -0.54f), 0.045f, 5, primary, creamWhite)
        addCandyCaneTube(acc, Vec3(0.28f, 1.18f, -0.52f), Vec3(0f, 1.52f, -0.54f), 0.045f, 5, primary, creamWhite)
        // Dash cowl + Translucent Sugar-Glass Windscreen + Glowing Gummy-Button Gauges
        acc.addBeveledBox(0f, 0.90f, 0.44f, 0.68f, 0.18f, 0.36f, 0.82f, 0.78f, darkCocoa, secondary, CandyMaterials.MAT_CARAMEL)
        // Sugar-glass tinted aero windscreen
        acc.addBeveledBox(0f, 1.08f, 0.52f, 0.60f, 0.16f, 0.10f, 0.78f, 0.55f, 0xFF80FFEAL, secondary, CandyMaterials.MAT_GUMMY)
        // Glowing candy gauge buttons
        acc.addBox(0f, 0.95f, 0.31f, 0.44f, 0.11f, 0.04f, 0xFF141024L)
        acc.addSphere(-0.12f, 0.95f, 0.29f, 0.055f, 0.055f, 0.03f, 5, 6, 0xFF3CE8B5L, matType = CandyMaterials.MAT_EMISSIVE)
        acc.addSphere(0f, 0.96f, 0.29f, 0.065f, 0.065f, 0.03f, 5, 6, 0xFFFFD54FL, matType = CandyMaterials.MAT_EMISSIVE)
        acc.addSphere(0.12f, 0.95f, 0.29f, 0.055f, 0.055f, 0.03f, 5, 6, 0xFFFF3D8BL, matType = CandyMaterials.MAT_EMISSIVE)
        // Steering column shaft (connects chassis to animated steering wheel hub at 0, 1.04, 0.42)
        acc.addStrut(Vec3(0f, 0.76f, 0.58f), Vec3(0f, 1.04f, 0.42f), 0.048f, 6, chrome)

        // 5. Distinct 3D Confectionery Exhaust Assemblies based on custom.exhaustStyle
        buildCustomExhaustAssembly(acc, kart, custom.exhaustStyle, waferGold, creamWhite, chrome)

        // 6. Distinct 3D Bakery Toppers & Decorations based on custom.decoration
        buildCustomDecoration(acc, custom.decoration, secondary, creamWhite, chrome)

        return acc.build()
    }

    private fun buildCustomExhaustAssembly(
        acc: MeshAccumulator,
        kart: KartSpec,
        style: ExhaustStyle,
        waferGold: Long,
        creamWhite: Long,
        chrome: Long
    ) {
        val flameHex = style.flameHex
        when (style) {
            ExhaustStyle.SUGAR_SPARK -> {
                val offsets = if (kart.style == KartBodyStyle.CHOCOLATE_CRUISER || kart.style == KartBodyStyle.CARAMEL_THUNDER) {
                    listOf(-0.46f, 0.46f)
                } else {
                    listOf(-0.36f, 0.36f)
                }
                for (px in offsets) {
                    acc.addHorizontalCylinderZ(px, 0.56f, -1.52f, 0.12f, 0.095f, 0.50f, 10, chrome, chrome, CandyMaterials.MAT_CHROME)
                    acc.addTorus(px, 0.56f, -1.75f, 0.11f, 0.028f, 10, 6, waferGold, chrome, verticalArch = true, matType = CandyMaterials.MAT_CHROME)
                    acc.addHorizontalCylinderZ(px, 0.56f, -1.76f, 0.085f, 0.085f, 0.10f, 8, flameHex, 0xFFFFFFFFL, CandyMaterials.MAT_EMISSIVE)
                }
            }

            ExhaustStyle.MINT_FLAME -> {
                // High-swept red & white striped Candy-Cane J-Pipes rising up behind the rear fenders
                for (side in listOf(-1f, 1f)) {
                    val px = side * 0.52f
                    addCandyCaneTube(acc, Vec3(px, 0.52f, -1.36f), Vec3(px + side * 0.08f, 1.18f, -1.68f), 0.075f, 8, 0xFFFF2448L, creamWhite)
                    acc.addSphere(px + side * 0.08f, 1.22f, -1.70f, 0.09f, 0.09f, 0.09f, 5, 8, flameHex, 0xFFFFFFFFL, CandyMaterials.MAT_EMISSIVE)
                }
            }

            ExhaustStyle.CARAMEL_JET -> {
                // Quad Rolled-Wafer & Dark-Chocolate Cannons with Golden Caramel Collars
                for (px in listOf(-0.54f, -0.30f, 0.30f, 0.54f)) {
                    acc.addHorizontalCylinderZ(px, 0.56f, -1.54f, 0.11f, 0.09f, 0.54f, 10, 0xFF3E2218L, waferGold, CandyMaterials.MAT_WAFER)
                    acc.addHorizontalCylinderZ(px, 0.56f, -1.80f, 0.085f, 0.085f, 0.09f, 8, flameHex, 0xFFFFF3B0L, CandyMaterials.MAT_EMISSIVE)
                }
            }

            ExhaustStyle.RAINBOW_STREAM -> {
                // Triple Waffle-Cone Rocket Boosters with Multi-Colored Rainbow Rings & Plasma Cones
                val ringColors = listOf(0xFFFF3D8BL, 0xFFFFD54FL, 0xFF3CE8B5L)
                for ((idx, px) in listOf(-0.44f, 0f, 0.44f).withIndex()) {
                    val yPos = if (idx == 1) 0.68f else 0.56f
                    acc.addHorizontalCylinderZ(px, yPos, -1.54f, 0.15f, 0.10f, 0.56f, 12, waferGold, ringColors[idx], CandyMaterials.MAT_WAFER)
                    acc.addTorus(px, yPos, -1.78f, 0.14f, 0.035f, 12, 6, ringColors[idx], 0xFFFFFFFFL, verticalArch = true, matType = CandyMaterials.MAT_EMISSIVE)
                    acc.addSphere(px, yPos, -1.84f, 0.10f, 0.10f, 0.18f, 6, 8, flameHex, 0xFF5CE1E6L, CandyMaterials.MAT_EMISSIVE)
                }
            }
        }
    }

    private fun buildCustomDecoration(
        acc: MeshAccumulator,
        decoration: KartDecoration,
        secondary: Long,
        creamWhite: Long,
        chrome: Long
    ) {
        when (decoration) {
            KartDecoration.NONE -> Unit
            KartDecoration.CHERRY_TOPPER -> {
                // Whipped cream dollop base + Twin glossy Maraschino cherries on curved green stems
                acc.addSphere(0f, 1.28f, -1.20f, 0.24f, 0.14f, 0.24f, 6, 10, creamWhite, secondary, CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(-0.16f, 1.48f, -1.20f, 0.19f, 0.19f, 0.19f, 6, 10, 0xFFFF1744L, 0xFFFF616FL, CandyMaterials.MAT_CANDY)
                acc.addSphere(0.16f, 1.48f, -1.20f, 0.19f, 0.19f, 0.19f, 6, 10, 0xFFFF1744L, 0xFFFF616FL, CandyMaterials.MAT_CANDY)
                acc.addStrut(Vec3(-0.16f, 1.62f, -1.20f), Vec3(0f, 1.98f, -1.10f), 0.03f, 6, 0xFF2ECC71L)
                acc.addStrut(Vec3(0.16f, 1.62f, -1.20f), Vec3(0f, 1.98f, -1.10f), 0.03f, 6, 0xFF2ECC71L)
            }
            KartDecoration.CANDY_CANE_WING -> {
                // High-downforce Red-and-White Striped Candy-Cane Bi-Wing with Peppermint Disc Endplates
                addCandyCaneTube(acc, Vec3(-0.46f, 1.12f, -1.24f), Vec3(-0.52f, 1.50f, -1.32f), 0.045f, 6, 0xFFFF2448L, creamWhite)
                addCandyCaneTube(acc, Vec3(0.46f, 1.12f, -1.24f), Vec3(0.52f, 1.50f, -1.32f), 0.045f, 6, 0xFFFF2448L, creamWhite)
                acc.addBeveledBox(0f, 1.44f, -1.32f, 1.72f, 0.10f, 0.38f, 0.95f, 0.85f, 0xFFFF2448L, creamWhite, CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0f, 1.56f, -1.34f, 1.38f, 0.07f, 0.28f, 0.95f, 0.85f, creamWhite, 0xFFFF2448L, CandyMaterials.MAT_CANDY)
                addPeppermintDisc(acc, -0.88f, 1.48f, -1.32f, 0.22f, 0.05f, 0xFFFF2448L, creamWhite, verticalX = true)
                addPeppermintDisc(acc, 0.88f, 1.48f, -1.32f, 0.22f, 0.05f, 0xFFFF2448L, creamWhite, verticalX = true)
            }
            KartDecoration.LOLLIPOP_ANTENNA -> {
                // Twin Giant Swirled Carnival Lollipops on White Candy Sticks
                for (side in listOf(-1f, 1f)) {
                    val lx = side * 0.56f
                    acc.addCylinder(lx, 0.90f, -1.16f, 0.038f, 0.038f, 0.88f, 6, creamWhite, chrome, CandyMaterials.MAT_CHROME)
                    addPeppermintDisc(acc, lx, 1.84f, -1.16f, 0.26f, 0.06f, 0xFFFF3D8BL, 0xFF5CE1E6L, verticalX = false)
                }
            }
            KartDecoration.CROWN_SPRINKLE -> {
                // Royal Sugar Rush Golden Crown with Glowing Gummy Gems above the Roll Hoop
                val crownY = 1.44f
                val crownZ = -0.46f
                acc.addCylinder(0f, crownY, crownZ, 0.22f, 0.28f, 0.22f, 10, 0xFFFFD700L, 0xFFFFA000L, matType = CandyMaterials.MAT_CHROME)
                val gemColors = longArrayOf(0xFFFF1744L, 0xFF00E5FFL, 0xFF76FF03L, 0xFFFF4081L, 0xFFE040FBL)
                for (i in 0 until 5) {
                    val a = (i.toFloat() / 5f) * (2f * PI.toFloat())
                    val gx = cos(a) * 0.25f
                    val gz = crownZ + sin(a) * 0.25f
                    acc.addSphere(gx, crownY + 0.24f, gz, 0.06f, 0.08f, 0.06f, 5, 6, gemColors[i], matType = CandyMaterials.MAT_EMISSIVE)
                }
            }
        }
    }

    /**
     * Separate animated steering wheel centered around origin (placed at (0, 1.04, 0.42) by renderer).
     * Styled like a candy-cane / glazed pretzel racing wheel with glowing center horn button.
     */
    private fun buildSteeringWheelMesh(kart: KartSpec, custom: KartCustomization): GpuMesh {
        val acc = MeshAccumulator()
        val paint = CatalogData.paintById(custom.paintId)
        val primary = if (custom.paintId == "default") kart.defaultPrimaryHex else paint.primaryHex
        val secondary = if (custom.paintId == "default") kart.defaultSecondaryHex else paint.secondaryHex
        val chrome = 0xFFE5EAF0L

        acc.addTorus(0f, 0f, 0f, 0.22f, 0.048f, 16, 8, primary, secondary, verticalArch = true, matType = CandyMaterials.MAT_CANDY)
        acc.addBox(0f, 0f, 0f, 0.38f, 0.054f, 0.044f, chrome, matType = CandyMaterials.MAT_CHROME)
        acc.addBox(0f, -0.09f, 0f, 0.054f, 0.18f, 0.044f, chrome, matType = CandyMaterials.MAT_CHROME)
        acc.addSphere(0f, 0f, -0.01f, 0.075f, 0.075f, 0.055f, 6, 8, kart.accentHex, matType = CandyMaterials.MAT_EMISSIVE)
        return acc.build()
    }

    /**
     * High-detail movie-style 3D Sugar Rush racer character centered at hip pivot
     * (placed at (0, 0.85, -0.14) by renderer).
     * Features:
     * - Layered candy racing suit with pleated skirt/pants, belt & golden candy buckle,
     *   open jacket lapels, candy buttons, whipped-cream collar, shoulder pads,
     *   articulated upper/lower arms, and cuffed racing gloves gripping the wheel.
     * - Expressive 3D head & face with ears, rosy candy blush cheeks, button nose,
     *   cheerful smile, arched eyebrows, and multi-layered anime eyes (white sclera,
     *   colored iris, dark pupil, and bright white specular catchlights).
     * - 8 unique sculpted 3D hairstyles & signature headgear/accessories per racer ID.
     */
    private fun buildDriverMesh(racer: RacerSpec): GpuMesh {
        val acc = MeshAccumulator()
        val pHex = racer.suitPrimaryHex
        val sHex = racer.suitSecondaryHex
        val hHex = racer.helmetHex
        val vHex = racer.visorHex
        val skin = racer.skinHex
        val cream = 0xFFFFF8F2L
        val gold = 0xFFFFD54FL

        // 1. Seated Hips, Pleated Candy Skirt / Racing Shorts & Thighs
        acc.addCylinder(0f, -0.04f, 0.02f, 0.25f, 0.21f, 0.16f, 12, sHex, pHex, matType = CandyMaterials.MAT_CANDY)
        // Pleated candy trim around hips
        for (i in 0 until 8) {
            val a = (i.toFloat() / 8f) * (2f * PI.toFloat())
            val px = cos(a) * 0.23f
            val pz = 0.02f + sin(a) * 0.19f
            val col = if (i % 2 == 0) pHex else cream
            acc.addSphere(px, 0.03f, pz, 0.055f, 0.065f, 0.055f, 4, 6, col, matType = CandyMaterials.MAT_MARSHMALLOW)
        }
        // Seated thighs extending forward into cockpit
        for (side in listOf(-1f, 1f)) {
            acc.addStrut(
                Vec3(side * 0.11f, 0.01f, 0.05f),
                Vec3(side * 0.12f, -0.02f, 0.30f),
                0.072f, 6, sHex, CandyMaterials.MAT_WAFER
            )
        }

        // 2. Racing Waist Belt & Golden Candy-Wrapper Buckle
        acc.addCylinder(0f, 0.10f, 0.01f, 0.215f, 0.210f, 0.065f, 12, 0xFF2B1535L, gold, matType = CandyMaterials.MAT_CARAMEL)
        acc.addBeveledBox(0f, 0.13f, 0.215f, 0.12f, 0.075f, 0.045f, 0.9f, 0.9f, gold, cream, CandyMaterials.MAT_CHROME)
        acc.addSphere(0f, 0.13f, 0.24f, 0.032f, 0.032f, 0.020f, 5, 6, pHex, matType = CandyMaterials.MAT_EMISSIVE)

        // 3. Layered Sugar Rush Racing Jacket, Inner Candy Shirt, Buttons & Lapels
        // Inner shirt torso
        acc.addCylinder(0f, 0.15f, 0.0f, 0.195f, 0.175f, 0.31f, 12, cream, sHex, matType = CandyMaterials.MAT_MARSHMALLOW)
        // Outer tailored racing jacket shell (left & right jacket panels + back)
        acc.addBeveledBox(0f, 0.29f, -0.02f, 0.44f, 0.29f, 0.31f, 0.88f, 0.90f, pHex, sHex, CandyMaterials.MAT_CANDY)
        // Contrasting front racing stripes & open lapels
        for (side in listOf(-1f, 1f)) {
            acc.addBeveledBox(side * 0.11f, 0.30f, 0.135f, 0.085f, 0.26f, 0.05f, 0.85f, 0.92f, sHex, cream, CandyMaterials.MAT_CANDY)
            // Wafer / frosting shoulder pads
            acc.addSphere(side * 0.235f, 0.40f, 0.01f, 0.088f, 0.068f, 0.095f, 6, 8, sHex, gold, matType = CandyMaterials.MAT_CANDY)
        }
        // Center candy buttons down the chest placket
        for (by in listOf(0.22f, 0.29f, 0.36f)) {
            acc.addSphere(0f, by, 0.158f, 0.026f, 0.026f, 0.018f, 5, 6, gold, matType = CandyMaterials.MAT_CHROME)
        }
        // Rear racing backpack / candy nitro pack on jacket back
        acc.addBeveledBox(0f, 0.30f, -0.185f, 0.24f, 0.19f, 0.075f, 0.86f, 0.86f, sHex, pHex, CandyMaterials.MAT_CANDY)
        acc.addSphere(0f, 0.30f, -0.228f, 0.055f, 0.055f, 0.028f, 5, 6, vHex, matType = CandyMaterials.MAT_EMISSIVE)

        // 4. Frosted Whipped-Cream / Candy Scarf Collar
        acc.addTorus(0f, 0.445f, 0.015f, 0.145f, 0.044f, 12, 6, cream, sHex, matType = CandyMaterials.MAT_MARSHMALLOW)

        // 5. Articulated Arms (Upper Arm -> Elbow Joint -> Forearm -> Glove Cuff -> Sculpted Racing Glove)
        for (side in listOf(-1f, 1f)) {
            val shoulder = Vec3(side * 0.23f, 0.38f, 0.02f)
            val elbow = Vec3(side * 0.25f, 0.26f, 0.21f)
            val wrist = Vec3(side * 0.17f, 0.22f, 0.41f)
            // Upper sleeve
            acc.addStrut(shoulder, elbow, 0.062f, 8, pHex, CandyMaterials.MAT_CANDY)
            // Elbow pad
            acc.addSphere(elbow.x, elbow.y, elbow.z, 0.065f, 0.065f, 0.065f, 5, 6, sHex, matType = CandyMaterials.MAT_CANDY)
            // Forearm sleeve
            acc.addStrut(elbow, wrist, 0.056f, 8, pHex, CandyMaterials.MAT_CANDY)
            // Frosted glove cuff ring
            acc.addTorus(wrist.x, wrist.y, wrist.z, 0.058f, 0.022f, 8, 5, cream, sHex, verticalArch = true, matType = CandyMaterials.MAT_MARSHMALLOW)
            // Racing glove fist gripping the steering wheel rim + sculpted thumb
            val handX = side * 0.16f
            val handY = 0.22f
            val handZ = 0.46f
            acc.addSphere(handX, handY, handZ, 0.068f, 0.062f, 0.074f, 6, 8, sHex, cream, matType = CandyMaterials.MAT_CANDY)
            acc.addSphere(handX - side * 0.045f, handY + 0.025f, handZ - 0.01f, 0.032f, 0.028f, 0.040f, 5, 6, cream, matType = CandyMaterials.MAT_CANDY)
        }

        // 6. Neck & Sculpted Expressive 3D Head & Face
        acc.addCylinder(0f, 0.45f, 0.02f, 0.085f, 0.080f, 0.11f, 10, skin)
        val headY = 0.64f
        val headZ = 0.04f
        // Main sculpted head with soft anime proportions
        acc.addSphere(0f, headY, headZ, 0.215f, 0.205f, 0.210f, 10, 14, skin)
        // Lower cheek fullness for cute Sugar Rush silhouette
        acc.addSphere(0f, headY - 0.045f, headZ + 0.035f, 0.195f, 0.125f, 0.175f, 8, 12, skin)

        // Left & Right Ears with inner ear tint
        for (side in listOf(-1f, 1f)) {
            acc.addSphere(side * 0.210f, headY - 0.01f, headZ - 0.01f, 0.042f, 0.058f, 0.036f, 5, 6, skin)
            acc.addSphere(side * 0.216f, headY - 0.01f, headZ + 0.005f, 0.024f, 0.034f, 0.020f, 4, 5, 0xFFFF8DA1L)
        }

        // Rosy Candy Blush Cheeks
        for (side in listOf(-1f, 1f)) {
            acc.addSphere(side * 0.122f, headY - 0.042f, headZ + 0.168f, 0.044f, 0.026f, 0.022f, 5, 6, 0xFFFF6699L, matType = CandyMaterials.MAT_CANDY)
        }

        // Cute 3D Button Nose
        acc.addSphere(0f, headY - 0.012f, headZ + 0.212f, 0.028f, 0.025f, 0.030f, 5, 6, skin)

        // Cheerful Smiling Mouth (berry lip arch + bright white teeth smile)
        acc.addBeveledBox(0f, headY - 0.072f, headZ + 0.188f, 0.088f, 0.030f, 0.028f, 0.8f, 0.8f, 0xFFD81B60L, 0xFFFF80ABL, CandyMaterials.MAT_CANDY)
        acc.addBox(0f, headY - 0.066f, headZ + 0.196f, 0.064f, 0.014f, 0.018f, 0xFFFFFFFFL, matType = CandyMaterials.MAT_EMISSIVE)

        // Large Expressive Multi-Layer Sugar Rush Eyes (Sclera + Colored Iris + Dark Pupil + Twin Catchlights + Eyebrows)
        for (side in listOf(-1f, 1f)) {
            val ex = side * 0.082f
            val ey = headY + 0.018f
            val ez = headZ + 0.182f
            // White sclera
            acc.addSphere(ex, ey, ez, 0.052f, 0.058f, 0.032f, 6, 8, 0xFFFFFFFFL)
            // Vibrant candy iris
            acc.addSphere(ex, ey, ez + 0.016f, 0.038f, 0.044f, 0.022f, 6, 8, vHex, pHex, matType = CandyMaterials.MAT_CANDY)
            // Deep dark pupil
            acc.addSphere(ex, ey, ez + 0.028f, 0.023f, 0.027f, 0.014f, 5, 6, 0xFF160824L)
            // Primary & secondary bright white specular eye catchlights
            acc.addSphere(ex - side * 0.012f, ey + 0.015f, ez + 0.036f, 0.011f, 0.011f, 0.009f, 4, 5, 0xFFFFFFFFL, matType = CandyMaterials.MAT_EMISSIVE)
            acc.addSphere(ex + side * 0.010f, ey - 0.012f, ez + 0.035f, 0.006f, 0.006f, 0.006f, 4, 5, 0xFFFFFFFFL, matType = CandyMaterials.MAT_EMISSIVE)
            // Upper eyelash / eyelid liner
            acc.addBeveledBox(ex, ey + 0.048f, ez + 0.012f, 0.086f, 0.016f, 0.026f, 0.9f, 0.9f, 0xFF230D30L)
            // Expressive arched eyebrow
            acc.addBeveledBox(ex, ey + 0.078f, ez + 0.008f, 0.082f, 0.018f, 0.024f, 0.85f, 0.85f, hHex, sHex)
        }

        // 7. Character-Specific Sculpted 3D Hairstyles & Signature Sugar Rush Headgear
        when (racer.id) {
            "berry_blitz" -> {
                // Strawberry-pink hair cap + swept bangs + bouncy twin pigtails with mint leaf ribbons + Golden Strawberry Crown & brow goggles
                val hairHex = 0xFFD81B60L
                val hairHi = 0xFFFF5C8DL
                // Back & top hair volume
                acc.addSphere(0f, headY + 0.05f, headZ - 0.03f, 0.228f, 0.195f, 0.215f, 8, 12, hairHex, hairHi, matType = CandyMaterials.MAT_CANDY)
                // Side hair locks framing face & front swept bangs
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.175f, headY + 0.02f, headZ + 0.09f, 0.062f, 0.125f, 0.075f, 6, 8, hairHex, hairHi)
                    acc.addSphere(side * 0.07f, headY + 0.135f, headZ + 0.155f, 0.085f, 0.055f, 0.065f, 5, 7, hairHi, hairHex)
                    // Mint-leaf ponytail ribbon ties + bouncy strawberry pigtails
                    val px = side * 0.24f
                    val py = headY + 0.08f
                    val pz = headZ - 0.05f
                    acc.addSphere(px, py, pz, 0.052f, 0.052f, 0.052f, 5, 6, 0xFF2ED8A3L, matType = CandyMaterials.MAT_CANDY)
                    acc.addSphere(px + side * 0.09f, py - 0.04f, pz - 0.04f, 0.095f, 0.145f, 0.095f, 7, 9, hairHex, hairHi, matType = CandyMaterials.MAT_CANDY)
                    acc.addSphere(px + side * 0.12f, py - 0.16f, pz - 0.06f, 0.068f, 0.085f, 0.068f, 6, 8, hairHi, matType = CandyMaterials.MAT_CANDY)
                }
                // Forehead Racing Goggles pushed up on brow
                acc.addTorus(0f, headY + 0.125f, headZ + 0.02f, 0.205f, 0.022f, 12, 5, cream, gold)
                for (side in listOf(-1f, 1f)) {
                    acc.addCylinder(side * 0.085f, headY + 0.125f, headZ + 0.175f, 0.055f, 0.055f, 0.035f, 10, gold, matType = CandyMaterials.MAT_CHROME)
                    acc.addSphere(side * 0.085f, headY + 0.135f, headZ + 0.195f, 0.044f, 0.044f, 0.018f, 5, 6, vHex, matType = CandyMaterials.MAT_EMISSIVE)
                }
                // Royal Strawberry Crown on top of head
                acc.addCylinder(0f, headY + 0.195f, headZ - 0.01f, 0.115f, 0.135f, 0.085f, 10, gold, cream, matType = CandyMaterials.MAT_CHROME)
                acc.addSphere(0f, headY + 0.265f, headZ - 0.01f, 0.065f, 0.075f, 0.065f, 6, 8, 0xFFFF1744L, matType = CandyMaterials.MAT_CANDY)
            }

            "minty_swirl" -> {
                // Mint-and-white swirled pompadour hair + Open-Face Peppermint Aero Helmet + Chrome Aviation Goggles on forehead
                acc.addSphere(0f, headY + 0.055f, headZ - 0.02f, 0.232f, 0.205f, 0.222f, 9, 12, hHex, cream, matType = CandyMaterials.MAT_CANDY)
                // Mint pompadour swirl quiff peeking out front
                acc.addSphere(0f, headY + 0.155f, headZ + 0.165f, 0.095f, 0.068f, 0.085f, 6, 8, 0xFF3CE8B5L, cream, matType = CandyMaterials.MAT_MARSHMALLOW)
                // Peppermint swirl ear pods on helmet sides
                for (side in listOf(-1f, 1f)) {
                    acc.addCylinder(side * 0.225f, headY, headZ, 0.075f, 0.075f, 0.045f, 10, 0xFFFF2448L, cream, matType = CandyMaterials.MAT_CANDY)
                    acc.addSphere(side * 0.245f, headY + 0.02f, headZ, 0.032f, 0.055f, 0.055f, 5, 6, 0xFF3CE8B5L, matType = CandyMaterials.MAT_EMISSIVE)
                }
                // Twin chrome aviation goggles on helmet brow + top peppermint fin
                for (side in listOf(-1f, 1f)) {
                    acc.addTorus(side * 0.085f, headY + 0.13f, headZ + 0.185f, 0.052f, 0.016f, 10, 5, 0xFFE5EAF0L, gold, verticalArch = true, matType = CandyMaterials.MAT_CHROME)
                    acc.addSphere(side * 0.085f, headY + 0.13f, headZ + 0.192f, 0.042f, 0.042f, 0.016f, 5, 6, vHex, matType = CandyMaterials.MAT_EMISSIVE)
                }
                acc.addBeveledBox(0f, headY + 0.25f, headZ - 0.02f, 0.065f, 0.085f, 0.26f, 0.6f, 0.8f, 0xFFFF2448L, cream, CandyMaterials.MAT_CANDY)
            }

            "coco_dash" -> {
                // Spiky dark-chocolate layered hair locks + Golden Caramel DJ Racing Headphones + Amber Visor Crest
                val chocoHair = 0xFF3B1E12L
                val caramelHi = 0xFFD99B52L
                acc.addSphere(0f, headY + 0.05f, headZ - 0.03f, 0.225f, 0.19f, 0.215f, 8, 12, chocoHair, caramelHi, matType = CandyMaterials.MAT_CARAMEL)
                // Sculpted spiky chocolate hair tufts across top & front
                val spikeOffsets = listOf(
                    Triple(0f, 0.23f, 0.08f),
                    Triple(-0.11f, 0.21f, 0.06f),
                    Triple(0.11f, 0.21f, 0.06f),
                    Triple(-0.08f, 0.22f, -0.08f),
                    Triple(0.08f, 0.22f, -0.08f),
                    Triple(0f, 0.15f, 0.17f)
                )
                for ((sx, sy, sz) in spikeOffsets) {
                    acc.addCylinder(sx, headY + sy - 0.06f, headZ + sz, 0.068f, 0.012f, 0.12f, 6, chocoHair, caramelHi, matType = CandyMaterials.MAT_CARAMEL)
                }
                // Golden Caramel Over-Ear DJ Headphones
                acc.addTorus(0f, headY + 0.06f, headZ, 0.235f, 0.028f, 14, 6, gold, 0xFF4E2A1EL, verticalArch = true, matType = CandyMaterials.MAT_CHROME)
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.235f, headY, headZ, 0.048f, 0.082f, 0.076f, 6, 8, gold, 0xFF3B1E12L, matType = CandyMaterials.MAT_CHROME)
                    acc.addSphere(side * 0.258f, headY, headZ, 0.022f, 0.048f, 0.044f, 5, 6, vHex, matType = CandyMaterials.MAT_EMISSIVE)
                }
                // Sleek flip-up amber forehead visor bar
                acc.addBeveledBox(0f, headY + 0.115f, headZ + 0.185f, 0.26f, 0.052f, 0.045f, 0.85f, 0.85f, vHex, gold, CandyMaterials.MAT_EMISSIVE)
            }

            "jelly_spark" -> {
                // Translucent Gummy-Bear Ears Racing Hood + Neon Berry Bob Hair + Glowing Cyan Star Glasses on brow
                val gummyHood = pHex
                val gummyHi = sHex
                // Gummy bear racing hood wrapping the head (leaving face open)
                acc.addSphere(0f, headY + 0.04f, headZ - 0.02f, 0.235f, 0.215f, 0.220f, 9, 12, gummyHood, gummyHi, matType = CandyMaterials.MAT_GUMMY)
                // Side neon hair bangs peeking from hood
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.155f, headY + 0.05f, headZ + 0.135f, 0.058f, 0.105f, 0.055f, 5, 7, 0xFF00E5FFL, gummyHood, matType = CandyMaterials.MAT_GUMMY)
                    // Cute round 3D Gummy Bear Ears on top corners of the hood
                    acc.addSphere(side * 0.165f, headY + 0.215f, headZ + 0.02f, 0.075f, 0.078f, 0.048f, 6, 8, gummyHood, gummyHi, matType = CandyMaterials.MAT_GUMMY)
                    acc.addSphere(side * 0.165f, headY + 0.215f, headZ + 0.048f, 0.042f, 0.044f, 0.022f, 5, 6, 0xFFFFF59DL, matType = CandyMaterials.MAT_EMISSIVE)
                    // Glowing cyan star goggles resting on forehead
                    acc.addSphere(side * 0.088f, headY + 0.125f, headZ + 0.185f, 0.058f, 0.046f, 0.022f, 5, 6, 0xFF76FF03L, 0xFF00E5FFL, matType = CandyMaterials.MAT_EMISSIVE)
                }
            }

            "caramel_nova" -> {
                // Golden-toffee hair + long sculpted streaming ponytail + Winged Valkyrie Butterscotch Crown Tiara
                val goldHair = 0xFFE6A100L
                val toffeeHi = 0xFFFFE082L
                acc.addSphere(0f, headY + 0.05f, headZ - 0.02f, 0.226f, 0.198f, 0.215f, 8, 12, goldHair, toffeeHi, matType = CandyMaterials.MAT_CARAMEL)
                // Long flowing golden ponytail arching behind the driver
                acc.addSphere(0f, headY + 0.06f, headZ - 0.22f, 0.065f, 0.065f, 0.065f, 5, 6, pHex, matType = CandyMaterials.MAT_CHROME)
                acc.addStrut(
                    Vec3(0f, headY + 0.05f, headZ - 0.23f),
                    Vec3(0f, headY - 0.14f, headZ - 0.42f),
                    0.078f, 8, goldHair, CandyMaterials.MAT_CARAMEL
                )
                acc.addSphere(0f, headY - 0.16f, headZ - 0.44f, 0.065f, 0.085f, 0.075f, 6, 8, toffeeHi, goldHair, matType = CandyMaterials.MAT_CARAMEL)
                // Golden Valkyrie Tiara Crown + Swept Side Wings
                acc.addBeveledBox(0f, headY + 0.165f, headZ + 0.155f, 0.22f, 0.075f, 0.045f, 0.7f, 0.85f, gold, cream, CandyMaterials.MAT_CHROME)
                acc.addSphere(0f, headY + 0.175f, headZ + 0.182f, 0.036f, 0.048f, 0.022f, 5, 6, 0xFFFF3D00L, matType = CandyMaterials.MAT_EMISSIVE)
                for (side in listOf(-1f, 1f)) {
                    acc.addStrut(
                        Vec3(side * 0.20f, headY + 0.08f, headZ + 0.04f),
                        Vec3(side * 0.31f, headY + 0.24f, headZ - 0.08f),
                        0.036f, 6, gold, CandyMaterials.MAT_CHROME
                    )
                }
            }

            "vanilla_twirl" -> {
                // Frosted vanilla twin curls + Puffed Whipped-Cream Pastry Chef Beret topped with sprinkles & Maraschino Cherry
                val vanillaHair = 0xFFFFF3D6L
                val pinkCurl = 0xFFFF85C2L
                acc.addSphere(0f, headY + 0.04f, headZ - 0.02f, 0.222f, 0.185f, 0.210f, 8, 12, vanillaHair, pinkCurl, matType = CandyMaterials.MAT_MARSHMALLOW)
                // Twin vanilla side-curls framing the cheeks
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.195f, headY - 0.05f, headZ + 0.08f, 0.065f, 0.115f, 0.068f, 6, 8, vanillaHair, pinkCurl, matType = CandyMaterials.MAT_MARSHMALLOW)
                }
                // Puffed Pastry Chef Beret Hat
                acc.addCylinder(0f, headY + 0.145f, headZ - 0.01f, 0.205f, 0.215f, 0.048f, 12, pinkCurl, cream, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(0f, headY + 0.235f, headZ - 0.01f, 0.255f, 0.115f, 0.245f, 8, 12, cream, pinkCurl, matType = CandyMaterials.MAT_MARSHMALLOW)
                // Rainbow sprinkles + Glossy Maraschino Cherry on top of beret
                for (sp in 0 until 8) {
                    val a = (sp.toFloat() / 8f) * (2f * PI.toFloat())
                    val sx = cos(a) * 0.14f
                    val sz = headZ - 0.01f + sin(a) * 0.14f
                    acc.addSphere(sx, headY + 0.31f, sz, 0.026f, 0.018f, 0.018f, 4, 5, SPRINKLE_PALETTE[sp % SPRINKLE_PALETTE.size], matType = CandyMaterials.MAT_CANDY)
                }
                acc.addSphere(0f, headY + 0.355f, headZ - 0.01f, 0.058f, 0.058f, 0.058f, 6, 8, 0xFFFF1744L, matType = CandyMaterials.MAT_CANDY)
            }

            "glazed_gus" -> {
                // Chocolate-glazed hair + Frosted Donut Captain Cap with a sculpted mini glazed donut on top + Donut Earmuffs
                val doughHex = 0xFFE6A95CL
                val glazeHex = 0xFFFF4D94L
                acc.addSphere(0f, headY + 0.04f, headZ - 0.02f, 0.228f, 0.19f, 0.215f, 8, 12, 0xFF4A2518L, doughHex, matType = CandyMaterials.MAT_CARAMEL)
                // Captain Cap visor brim & crown
                acc.addBeveledBox(0f, headY + 0.135f, headZ + 0.175f, 0.25f, 0.032f, 0.11f, 0.85f, 0.9f, 0xFF3A1D12L, gold, CandyMaterials.MAT_CARAMEL)
                acc.addCylinder(0f, headY + 0.14f, headZ - 0.01f, 0.21f, 0.23f, 0.11f, 12, doughHex, glazeHex, matType = CandyMaterials.MAT_WAFER)
                // Sculpted frosted donut ring crowning the cap
                acc.addTorus(0f, headY + 0.265f, headZ - 0.01f, 0.145f, 0.055f, 14, 7, glazeHex, doughHex, matType = CandyMaterials.MAT_CARAMEL)
                // Side Donut Earmuffs
                for (side in listOf(-1f, 1f)) {
                    acc.addTorus(side * 0.225f, headY, headZ, 0.055f, 0.024f, 10, 5, glazeHex, doughHex, verticalArch = true, matType = CandyMaterials.MAT_CARAMEL)
                }
            }

            else -> {
                // "fizz_pop": Fluffy Dual Cotton-Candy Space Buns (Pink & Sky-Blue) + Fizzy Pop-Rock Antenna Headband
                val pinkFluff = 0xFFFF4081L
                val blueFluff = 0xFF18FFFFL
                acc.addSphere(0f, headY + 0.05f, headZ - 0.02f, 0.225f, 0.195f, 0.215f, 8, 12, pinkFluff, blueFluff, matType = CandyMaterials.MAT_MARSHMALLOW)
                // Left Pink & Right Sky-Blue Cotton-Candy Space Buns
                acc.addSphere(-0.185f, headY + 0.205f, headZ - 0.01f, 0.110f, 0.110f, 0.110f, 7, 10, pinkFluff, cream, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(0.185f, headY + 0.205f, headZ - 0.01f, 0.110f, 0.110f, 0.110f, 7, 10, blueFluff, cream, matType = CandyMaterials.MAT_MARSHMALLOW)
                // Twin Fizzy Antenna Springs with glowing candy orbs
                for (side in listOf(-1f, 1f)) {
                    val orbCol = if (side < 0) blueFluff else pinkFluff
                    acc.addStrut(
                        Vec3(side * 0.08f, headY + 0.18f, headZ + 0.04f),
                        Vec3(side * 0.14f, headY + 0.34f, headZ + 0.08f),
                        0.018f, 6, gold, CandyMaterials.MAT_CHROME
                    )
                    acc.addSphere(side * 0.14f, headY + 0.355f, headZ + 0.08f, 0.042f, 0.042f, 0.042f, 5, 6, orbCol, matType = CandyMaterials.MAT_EMISSIVE)
                }
                // Forehead Pop-Rock Neon Visor Shades
                acc.addBeveledBox(0f, headY + 0.125f, headZ + 0.185f, 0.25f, 0.052f, 0.036f, 0.9f, 0.9f, blueFluff, pinkFluff, CandyMaterials.MAT_EMISSIVE)
            }
        }

        return acc.build()
    }

    private fun buildBrakeLights(kart: KartSpec): GpuMesh {
        val acc = MeshAccumulator()
        val zBack = when (kart.style) {
            KartBodyStyle.CHOCOLATE_CRUISER, KartBodyStyle.CARAMEL_THUNDER -> -1.54f
            else -> -1.50f
        }
        // u = 0.95f signals CandyShaders.LIT_FRAGMENT_SHADER to modulate intensity via uBrakeGlow
        acc.addBox(-0.52f, 0.64f, zBack, 0.28f, 0.12f, 0.10f, 0xFFFF2244L, matType = CandyMaterials.MAT_EMISSIVE, uTag = 0.95f)
        acc.addBox(0.52f, 0.64f, zBack, 0.28f, 0.12f, 0.10f, 0xFFFF2244L, matType = CandyMaterials.MAT_EMISSIVE, uTag = 0.95f)
        return acc.build()
    }

    /**
     * Sculpted Confectionery Tire with wheel-design-specific edible sidewalls:
     * - CHOCO_COOKIE: Baked cookie-dough sidewall studded with 3D dark chocolate chips
     * - DONUT_RING: Golden fried-donut tire with raised pink frosting ring & rainbow sprinkles
     * - PEPPERMINT_SWIRL: Dark cocoa tire with crisp whitewall icing ring
     * - GUMMY_GLOW: Translucent-style neon berry/cyan gummy tire shoulders
     * - GOLD_CARAMEL: Espresso tire with golden molten-caramel sidewall ribbon
     */
    private fun buildSculptedTireMesh(design: WheelDesign): GpuMesh {
        val acc = MeshAccumulator()
        val treadHex = when (design) {
            WheelDesign.CHOCO_COOKIE -> 0xFF3A2314L
            WheelDesign.DONUT_RING -> 0xFF5A3418L
            WheelDesign.GUMMY_GLOW -> 0xFF1E2238L
            else -> 0xFF231829L
        }
        val sidewallHex = when (design) {
            WheelDesign.CHOCO_COOKIE -> 0xFFC68B59L
            WheelDesign.DONUT_RING -> 0xFFD99B52L
            WheelDesign.PEPPERMINT_SWIRL -> 0xFF2B1D30L
            WheelDesign.GUMMY_GLOW -> 0xFF00C8B4L
            WheelDesign.GOLD_CARAMEL -> 0xFF3B2314L
        }
        val (tr, tg, tb) = MeshAccumulator.hexToRgb(treadHex)
        val (sr, sg, sb) = MeshAccumulator.hexToRgb(sidewallHex)
        val segs = 18
        val outerR = 0.46f
        val shoulderR = 0.43f
        val innerR = 0.29f
        val halfW = 0.21f
        val treadHalfW = 0.165f

        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val v0 = i.toFloat() / segs
            val v1 = (i + 1).toFloat() / segs

            // Center tread band (u in 0.35..0.65 -> deep tread pattern in shader)
            acc.addQuad(
                -treadHalfW, c0 * outerR, s0 * outerR,
                -treadHalfW, c1 * outerR, s1 * outerR,
                treadHalfW, c1 * outerR, s1 * outerR,
                treadHalfW, c0 * outerR, s0 * outerR,
                tr, tg, tb, 1f, 0.35f, v0, 0.65f, v1, CandyMaterials.MAT_TIRE
            )
            // Left & right rounded tire shoulders
            acc.addQuad(
                -halfW, c0 * shoulderR, s0 * shoulderR,
                -halfW, c1 * shoulderR, s1 * shoulderR,
                -treadHalfW, c1 * outerR, s1 * outerR,
                -treadHalfW, c0 * outerR, s0 * outerR,
                sr * 0.92f, sg * 0.92f, sb * 0.92f, 1f, 0.2f, v0, 0.35f, v1, CandyMaterials.MAT_TIRE
            )
            acc.addQuad(
                treadHalfW, c0 * outerR, s0 * outerR,
                treadHalfW, c1 * outerR, s1 * outerR,
                halfW, c1 * shoulderR, s1 * shoulderR,
                halfW, c0 * shoulderR, s0 * shoulderR,
                sr * 0.92f, sg * 0.92f, sb * 0.92f, 1f, 0.65f, v0, 0.8f, v1, CandyMaterials.MAT_TIRE
            )
            // Sidewalls down to alloy rim bead
            acc.addQuad(
                halfW, c0 * innerR, s0 * innerR,
                halfW, c1 * innerR, s1 * innerR,
                halfW, c1 * shoulderR, s1 * shoulderR,
                halfW, c0 * shoulderR, s0 * shoulderR,
                sr, sg, sb, 1f, 0.82f, v0, 0.96f, v1, CandyMaterials.MAT_TIRE
            )
            acc.addQuad(
                -halfW, c0 * shoulderR, s0 * shoulderR,
                -halfW, c1 * shoulderR, s1 * shoulderR,
                -halfW, c1 * innerR, s1 * innerR,
                -halfW, c0 * innerR, s0 * innerR,
                sr, sg, sb, 1f, 0.04f, v0, 0.18f, v1, CandyMaterials.MAT_TIRE
            )
        }

        // Edible 3D sidewall details on both inner & outer wheel faces
        for (side in listOf(-1f, 1f)) {
            val faceX = side * (halfW + 0.012f)
            when (design) {
                WheelDesign.CHOCO_COOKIE -> {
                    // 8 3D dark-chocolate chips embedded in the cookie sidewall
                    for (k in 0 until 8) {
                        val a = (k.toFloat() / 8f) * (2f * PI.toFloat())
                        val cy = cos(a) * 0.36f
                        val cz = sin(a) * 0.36f
                        acc.addSphere(faceX, cy, cz, 0.025f, 0.042f, 0.042f, 4, 6, 0xFF24120BL, matType = CandyMaterials.MAT_CARAMEL)
                    }
                }
                WheelDesign.DONUT_RING -> {
                    // Raised strawberry icing ring + 10 colorful sugar sprinkles on the tire sidewall
                    for (k in 0 until 10) {
                        val a = (k.toFloat() / 10f) * (2f * PI.toFloat())
                        val cy = cos(a) * 0.36f
                        val cz = sin(a) * 0.36f
                        val spCol = SPRINKLE_PALETTE[k % SPRINKLE_PALETTE.size]
                        acc.addSphere(faceX, cy, cz, 0.024f, 0.052f, 0.052f, 4, 6, 0xFFFF6FAEL, matType = CandyMaterials.MAT_CANDY)
                        acc.addBox(faceX + side * 0.012f, cy, cz, 0.02f, 0.028f, 0.055f, spCol, matType = CandyMaterials.MAT_CANDY)
                    }
                }
                WheelDesign.PEPPERMINT_SWIRL -> {
                    // Crisp white-icing sidewall dots
                    for (k in 0 until 8) {
                        val a = (k.toFloat() / 8f) * (2f * PI.toFloat())
                        acc.addSphere(faceX, cos(a) * 0.36f, sin(a) * 0.36f, 0.018f, 0.036f, 0.036f, 4, 6, 0xFFFFFFFFL, matType = CandyMaterials.MAT_CANDY)
                    }
                }
                WheelDesign.GUMMY_GLOW -> {
                    for (k in 0 until 8) {
                        val a = (k.toFloat() / 8f) * (2f * PI.toFloat())
                        val neonHex = if (k % 2 == 0) 0xFF00F5D4L else 0xFFFF4D8DL
                        acc.addSphere(faceX, cos(a) * 0.36f, sin(a) * 0.36f, 0.022f, 0.044f, 0.044f, 4, 6, neonHex, matType = CandyMaterials.MAT_EMISSIVE)
                    }
                }
                WheelDesign.GOLD_CARAMEL -> {
                    for (k in 0 until 8) {
                        val a = (k.toFloat() / 8f) * (2f * PI.toFloat())
                        acc.addSphere(faceX, cos(a) * 0.36f, sin(a) * 0.36f, 0.020f, 0.040f, 0.040f, 4, 6, 0xFFFFD54FL, matType = CandyMaterials.MAT_CHROME)
                    }
                }
            }
        }
        return acc.build()
    }

    private fun buildAlloyRimMesh(design: WheelDesign): GpuMesh {
        val acc = MeshAccumulator()
        val (r1, g1, b1) = MeshAccumulator.hexToRgb(design.primaryHex)
        val (r2, g2, b2) = MeshAccumulator.hexToRgb(design.rimHex)
        val rimR = 0.295f
        val halfW = 0.215f
        val segs = when (design) {
            WheelDesign.PEPPERMINT_SWIRL -> 16
            WheelDesign.GOLD_CARAMEL -> 16
            else -> 12
        }
        // Deep-dish confectionery wheel rim with alternating candy/chrome spokes
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
            val isSpoke = (i % 2 == 0)
            val (rr, rg, rb) = if (isSpoke) Triple(r1, g1, b1) else Triple(r2, g2, b2)
            val mat = when (design) {
                WheelDesign.CHOCO_COOKIE -> if (isSpoke) CandyMaterials.MAT_WAFER else CandyMaterials.MAT_CARAMEL
                WheelDesign.GUMMY_GLOW -> if (isSpoke) CandyMaterials.MAT_GUMMY else CandyMaterials.MAT_CHROME
                WheelDesign.GOLD_CARAMEL -> CandyMaterials.MAT_CHROME
                else -> if (isSpoke) CandyMaterials.MAT_CANDY else CandyMaterials.MAT_CHROME
            }
            val y0 = cos(a0) * rimR; val z0 = sin(a0) * rimR
            val y1 = cos(a1) * rimR; val z1 = sin(a1) * rimR
            acc.addTri(halfW, 0f, 0f, halfW, y0, z0, halfW, y1, z1, rr, rg, rb, 1f, 0f, 0f, 1f, 0f, 0.5f, 1f, mat)
            acc.addTri(-halfW, 0f, 0f, -halfW, y1, z1, -halfW, y0, z0, rr, rg, rb, 1f, 0f, 0f, 1f, 0f, 0.5f, 1f, mat)
        }
        // Center candy button hubcap spinner
        acc.addSphere(halfW + 0.025f, 0f, 0f, 0.055f, 0.105f, 0.105f, 5, 8, design.primaryHex, design.rimHex, matType = CandyMaterials.MAT_CHROME)
        acc.addSphere(-halfW - 0.025f, 0f, 0f, 0.055f, 0.105f, 0.105f, 5, 8, design.primaryHex, design.rimHex, matType = CandyMaterials.MAT_CHROME)
        return acc.build()
    }

    private fun buildSuspensionAssemblyMesh(kart: KartSpec): GpuMesh {
        val acc = MeshAccumulator()
        val chrome = 0xFFD8E0E8L
        val springHex = kart.accentHex
        val wheelCoords = listOf(
            Vec3(-0.92f, 0.45f, 0.96f),
            Vec3(0.92f, 0.45f, 0.96f),
            Vec3(-0.96f, 0.45f, -0.96f),
            Vec3(0.96f, 0.45f, -0.96f)
        )
        for (w in wheelCoords) {
            val side = if (w.x < 0f) -1f else 1f
            val innerX = side * 0.42f
            // Candy-coated double wishbone A-arms + coilover candy shock strut + steering tie rod + brake caliper
            acc.addStrut(Vec3(innerX, 0.44f, w.z + 0.14f), Vec3(w.x - side * 0.12f, w.y, w.z), 0.044f, 6, chrome)
            acc.addStrut(Vec3(innerX, 0.44f, w.z - 0.14f), Vec3(w.x - side * 0.12f, w.y, w.z), 0.044f, 6, chrome)
            addCandyCaneTube(acc, Vec3(innerX + side * 0.12f, 0.72f, w.z), Vec3(w.x - side * 0.14f, w.y + 0.04f, w.z), 0.056f, 5, springHex, 0xFFFFFFFFL)
            if (w.z > 0f) {
                acc.addStrut(Vec3(innerX, 0.50f, w.z - 0.20f), Vec3(w.x - side * 0.16f, w.y, w.z - 0.12f), 0.028f, 5, chrome)
            }
            // Brake disc & candy caliper housing on inner hub
            acc.addSphere(w.x - side * 0.19f, w.y, w.z, 0.04f, 0.19f, 0.19f, 4, 8, chrome, matType = CandyMaterials.MAT_CHROME)
            acc.addBox(w.x - side * 0.18f, w.y + 0.10f, w.z - 0.10f, 0.08f, 0.12f, 0.14f, springHex)
        }
        return acc.build()
    }

    // ========================================================================
    // CONFECTIONERY GEOMETRY HELPERS (Frosting Piping, Candy Canes, Sprinkles)
    // ========================================================================

    private fun addPipedFrostingLine(
        acc: MeshAccumulator,
        start: Vec3,
        end: Vec3,
        dollopCount: Int,
        radius: Float,
        primaryHex: Long,
        secondaryHex: Long
    ) {
        val steps = dollopCount.coerceAtLeast(2)
        for (i in 0 until steps) {
            val t = i.toFloat() / (steps - 1).toFloat()
            val x = start.x + (end.x - start.x) * t
            val y = start.y + (end.y - start.y) * t
            val z = start.z + (end.z - start.z) * t
            val hex = if (i % 2 == 0) primaryHex else secondaryHex
            acc.addSphere(x, y, z, radius, radius * 0.88f, radius, 5, 7, hex, primaryHex, CandyMaterials.MAT_MARSHMALLOW)
        }
    }

    private fun addCandyCaneTube(
        acc: MeshAccumulator,
        start: Vec3,
        end: Vec3,
        radius: Float,
        segments: Int,
        stripeHexA: Long,
        stripeHexB: Long
    ) {
        val count = segments.coerceAtLeast(2)
        for (i in 0 until count) {
            val t0 = i.toFloat() / count
            val t1 = (i + 1).toFloat() / count
            val p0 = Vec3(
                start.x + (end.x - start.x) * t0,
                start.y + (end.y - start.y) * t0,
                start.z + (end.z - start.z) * t0
            )
            val p1 = Vec3(
                start.x + (end.x - start.x) * t1,
                start.y + (end.y - start.y) * t1,
                start.z + (end.z - start.z) * t1
            )
            val hex = if (i % 2 == 0) stripeHexA else stripeHexB
            acc.addStrut(p0, p1, radius, 6, hex, CandyMaterials.MAT_CANDY)
        }
    }

    private fun addPeppermintDisc(
        acc: MeshAccumulator,
        cx: Float,
        cy: Float,
        cz: Float,
        radius: Float,
        halfThickness: Float,
        colorA: Long,
        colorB: Long,
        verticalX: Boolean
    ) {
        val segs = 12
        val (r1, g1, b1) = MeshAccumulator.hexToRgb(colorA)
        val (r2, g2, b2) = MeshAccumulator.hexToRgb(colorB)
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
            val (rr, rg, rb) = if (i % 2 == 0) Triple(r1, g1, b1) else Triple(r2, g2, b2)
            if (verticalX) {
                val y0 = cy + cos(a0) * radius; val z0 = cz + sin(a0) * radius
                val y1 = cy + cos(a1) * radius; val z1 = cz + sin(a1) * radius
                acc.addTri(cx + halfThickness, cy, cz, cx + halfThickness, y0, z0, cx + halfThickness, y1, z1, rr, rg, rb, 1f, 0f, 0f, 0.5f, 0.5f, 1f, 1f, CandyMaterials.MAT_CANDY)
                acc.addTri(cx - halfThickness, cy, cz, cx - halfThickness, y1, z1, cx - halfThickness, y0, z0, rr, rg, rb, -1f, 0f, 0f, 0.5f, 0.5f, 1f, 1f, CandyMaterials.MAT_CANDY)
            } else {
                val x0 = cx + cos(a0) * radius; val y0 = cy + sin(a0) * radius
                val x1 = cx + cos(a1) * radius; val y1 = cy + sin(a1) * radius
                acc.addTri(cx, cy, cz + halfThickness, x0, y0, cz + halfThickness, x1, y1, cz + halfThickness, rr, rg, rb, 0f, 0f, 1f, 0.5f, 0.5f, 1f, 1f, CandyMaterials.MAT_CANDY)
                acc.addTri(cx, cy, cz - halfThickness, x1, y1, cz - halfThickness, x0, y0, cz - halfThickness, rr, rg, rb, 0f, 0f, -1f, 0.5f, 0.5f, 1f, 1f, CandyMaterials.MAT_CANDY)
            }
        }
    }

    private fun addCandySprinkles(
        acc: MeshAccumulator,
        cx: Float,
        cy: Float,
        cz: Float,
        radiusX: Float,
        radiusZ: Float,
        count: Int
    ) {
        for (i in 0 until count) {
            val angle = i * 2.39996f // Golden angle distribution
            val dist = kotlin.math.sqrt((i + 0.5f) / count.toFloat())
            val sx = cx + cos(angle) * radiusX * dist
            val sz = cz + sin(angle) * radiusZ * dist
            val col = SPRINKLE_PALETTE[i % SPRINKLE_PALETTE.size]
            val isLongX = (i % 2 == 0)
            acc.addBox(
                sx,
                cy,
                sz,
                if (isLongX) 0.11f else 0.045f,
                0.042f,
                if (isLongX) 0.045f else 0.11f,
                col,
                matType = CandyMaterials.MAT_CANDY
            )
        }
    }
}
