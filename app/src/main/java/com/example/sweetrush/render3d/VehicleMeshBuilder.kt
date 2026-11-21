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
                // =========================================================================
                // OFFICIAL SUGAR RUSH "CANDY KART" (Made by Vanellope — Sketchfab Model)
                // =========================================================================
                val berryGlaze = primary         // Deep sugar-dusted berry-plum hood glaze (0xFF7B2851)
                val hotPinkIcing = secondary     // Bright hot-pink piped icing squiggles (0xFFFF2E88)
                val mintIcing = accent           // Mint-green nose frosting & side scripts (0xFF3CE8B5)
                val chocoFudge = 0xFF3A1E18L     // Dark chocolate rear deck & spoiler icing

                // 1. Two-tone Lofted Confectionery Body: Dark Chocolate Rear Hull -> Sugar-Dusted Berry Front Hood
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.52f, 0.72f, 0.36f, 0.90f, chocoFudge, chocoFudge),
                        LoftStation(-0.56f, 0.80f, 0.35f, 0.92f, chocoFudge, berryGlaze),
                        LoftStation(0.24f, 0.72f, 0.35f, 0.88f, berryGlaze, hotPinkIcing),
                        LoftStation(1.12f, 0.64f, 0.37f, 0.76f, berryGlaze, berryGlaze),
                        LoftStation(1.78f, 0.30f, 0.39f, 0.54f, mintIcing, berryGlaze)
                    ),
                    matType = CandyMaterials.MAT_CANDY
                )

                // 2. Mint-Green Frosted Nose Tip with Rainbow Sprinkles & Big Red Cherry/Strawberry Candy Ornament
                acc.addSphere(0f, 0.56f, 1.68f, 0.44f, 0.16f, 0.28f, 7, 10, mintIcing, creamWhite, CandyMaterials.MAT_MARSHMALLOW)
                addCandySprinkles(acc, 0f, 0.68f, 1.66f, 0.36f, 0.18f, count = 12)
                acc.addSphere(0f, 0.78f, 1.56f, 0.16f, 0.17f, 0.18f, 7, 10, 0xFFFF2438L, 0xFFFF6E40L, CandyMaterials.MAT_CANDY)
                // Swirled Pink-and-Cream Marshmallow Twist Headlights on Front Hood Corners
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.46f, 0.64f, 1.48f, 0.13f, 0.13f, 0.15f, 6, 8, creamWhite, hotPinkIcing, CandyMaterials.MAT_MARSHMALLOW)
                    acc.addTorus(side * 0.46f, 0.64f, 1.50f, 0.09f, 0.032f, 10, 5, hotPinkIcing, creamWhite, verticalArch = true, matType = CandyMaterials.MAT_CANDY)
                }

                // 3. Hot-Pink Piped Icing Squiggle Lines Across the Berry Hood & Chocolate Rear Deck
                addPipedFrostingLine(acc, Vec3(-0.34f, 0.82f, 0.56f), Vec3(0.08f, 0.75f, 1.38f), 8, 0.055f, hotPinkIcing, hotPinkIcing)
                addPipedFrostingLine(acc, Vec3(0.36f, 0.82f, 0.52f), Vec3(-0.12f, 0.78f, 1.18f), 8, 0.055f, hotPinkIcing, hotPinkIcing)
                addPipedFrostingLine(acc, Vec3(-0.48f, 0.78f, 0.94f), Vec3(0.46f, 0.77f, 0.98f), 9, 0.052f, hotPinkIcing, creamWhite)
                // White & Pink Piped Icing Rim around the Open Cockpit Tub
                addPipedFrostingLine(acc, Vec3(-0.56f, 0.85f, -0.58f), Vec3(-0.54f, 0.82f, 0.46f), 8, 0.052f, creamWhite, hotPinkIcing)
                addPipedFrostingLine(acc, Vec3(0.56f, 0.85f, -0.58f), Vec3(0.54f, 0.82f, 0.46f), 8, 0.052f, creamWhite, hotPinkIcing)

                // 4. Dense Multi-Color Rainbow & Star Sprinkles on Hood Flanks and Chocolate Rear Deck
                addCandySprinkles(acc, -0.44f, 0.78f, 0.96f, 0.24f, 0.42f, count = 16)
                addCandySprinkles(acc, 0.44f, 0.78f, 0.96f, 0.24f, 0.42f, count = 16)
                addCandySprinkles(acc, 0f, 0.88f, -0.96f, 0.56f, 0.36f, count = 26)

                // 5. 4 Angled Candy-Cane / Peppermint Stick Side Exhaust Pipes on Each Flank (Mint & Red/Orange Striped)
                val pipeOffsetsZ = listOf(-0.24f, -0.02f, 0.20f, 0.42f)
                val pipeStripeColors = listOf(mintIcing, 0xFFFF3D47L, 0xFFFF9100L, 0xFFFF3D8BL)
                for (side in listOf(-1f, 1f)) {
                    for ((idx, pz) in pipeOffsetsZ.withIndex()) {
                        val pStart = Vec3(side * 0.68f, 0.44f, pz)
                        val pMid = Vec3(side * 0.92f, 0.50f, pz - 0.10f)
                        val pTip = Vec3(side * 1.02f, 0.68f, pz - 0.24f)
                        val cStripe = pipeStripeColors[idx % pipeStripeColors.size]
                        addCandyCaneTube(acc, pStart, pMid, 0.048f, 4, cStripe, creamWhite)
                        addCandyCaneTube(acc, pMid, pTip, 0.050f, 5, cStripe, creamWhite)
                    }
                    // "Made by Vanellope" Mint & Pink Icing Signature Script Plaque along Side Doors
                    addPipedFrostingLine(acc, Vec3(side * 0.77f, 0.66f, -0.52f), Vec3(side * 0.77f, 0.68f, -0.08f), 6, 0.036f, mintIcing, hotPinkIcing)
                }

                // 6. Iconic Layered Ice-Cream Wafer-Sandwich Rear Spoiler with Dripping Dark Chocolate Glaze
                // Striped Candy-Cane Wing Struts
                addCandyCaneTube(acc, Vec3(-0.36f, 0.84f, -1.16f), Vec3(-0.42f, 1.24f, -1.30f), 0.054f, 6, hotPinkIcing, creamWhite)
                addCandyCaneTube(acc, Vec3(0.36f, 0.84f, -1.16f), Vec3(0.42f, 1.24f, -1.30f), 0.054f, 6, waferGold, creamWhite)
                // Pink Strawberry Ice-Cream Center Slab
                acc.addBeveledBox(0f, 1.24f, -1.30f, 1.66f, 0.14f, 0.46f, 0.96f, 0.92f, 0xFFFF7EB6L, hotPinkIcing, CandyMaterials.MAT_MARSHMALLOW)
                // Upper Thick Melted Dark Chocolate Glaze Layer
                acc.addBeveledBox(0f, 1.33f, -1.30f, 1.70f, 0.09f, 0.50f, 0.95f, 0.90f, chocoFudge, 0xFF4E2A1EL, CandyMaterials.MAT_CARAMEL)
                // Sculpted Chocolate Glaze Drips hanging along the Spoiler Perimeter
                val spoilerDripsX = listOf(-0.74f, -0.44f, -0.14f, 0.18f, 0.48f, 0.74f)
                for ((di, dx) in spoilerDripsX.withIndex()) {
                    val dripLen = if (di % 2 == 0) 0.11f else 0.08f
                    acc.addSphere(dx, 1.25f, -1.05f, 0.065f, dripLen, 0.055f, 5, 6, chocoFudge, matType = CandyMaterials.MAT_CARAMEL)
                    acc.addSphere(dx, 1.25f, -1.55f, 0.065f, dripLen, 0.055f, 5, 6, chocoFudge, matType = CandyMaterials.MAT_CARAMEL)
                }
                // Translucent Orange Gummy-Candy Windscreen Shield in front of the driver
                acc.addSphere(0f, 0.98f, 0.38f, 0.34f, 0.24f, 0.16f, 7, 10, 0xFFFF7A29L, 0xFFFFB74DL, CandyMaterials.MAT_GUMMY)
                acc.addBeveledBox(0f, 0.38f, -1.54f, 1.48f, 0.18f, 0.24f, 0.9f, 0.85f, chocoFudge, matType = CandyMaterials.MAT_CARAMEL)
            }

            KartBodyStyle.PEPPERMINT_RACER -> {
                // Taffyta Muttonfudge's "Pink Lightning" Open-Wheel Hard-Candy Formula Racer
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.46f, 0.56f, 0.35f, 0.90f, primary, secondary),
                        LoftStation(-0.80f, 0.66f, 0.34f, 0.86f, primary, primary),
                        LoftStation(-0.10f, 0.64f, 0.34f, 0.82f, primary, secondary),
                        LoftStation(0.58f, 0.50f, 0.36f, 0.76f, primary, primary),
                        LoftStation(1.22f, 0.36f, 0.38f, 0.62f, primary, secondary),
                        LoftStation(1.82f, 0.20f, 0.38f, 0.48f, secondary, primary)
                    ),
                    matType = CandyMaterials.MAT_CANDY
                )
                // Glossy Strawberry Hard-Candy Sidepods with White Polka-Dot Candy Trim
                acc.addBeveledBox(-0.64f, 0.50f, -0.22f, 0.46f, 0.34f, 1.20f, 0.82f, 0.78f, primary, secondary, CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0.64f, 0.50f, -0.22f, 0.46f, 0.34f, 1.20f, 0.82f, 0.78f, primary, secondary, CandyMaterials.MAT_CANDY)
                for (side in listOf(-1f, 1f)) {
                    for (dz in listOf(-0.56f, -0.20f, 0.16f)) {
                        acc.addSphere(side * 0.88f, 0.64f, dz, 0.045f, 0.075f, 0.075f, 5, 6, creamWhite, matType = CandyMaterials.MAT_MARSHMALLOW)
                    }
                    // Iconic Lollipop Side Mirrors on White Candy Sticks (from Sketchfab model)
                    acc.addStrut(Vec3(side * 0.44f, 0.84f, 0.36f), Vec3(side * 0.72f, 1.12f, 0.38f), 0.032f, 6, creamWhite, CandyMaterials.MAT_CHROME)
                    addPeppermintDisc(acc, side * 0.74f, 1.16f, 0.38f, 0.14f, 0.038f, primary, creamWhite, verticalX = false)
                }
                addCandyCaneTube(acc, Vec3(-0.76f, 0.68f, -0.82f), Vec3(-0.48f, 0.62f, 0.88f), 0.055f, 8, primary, creamWhite)
                addCandyCaneTube(acc, Vec3(0.76f, 0.68f, -0.82f), Vec3(0.48f, 0.62f, 0.88f), 0.055f, 8, primary, creamWhite)
                // Multi-element Pink Lightning front F1 wing with round lollipop disc endplates
                acc.addBeveledBox(0f, 0.33f, 1.68f, 1.80f, 0.08f, 0.40f, 0.98f, 0.85f, creamWhite, primary, CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0f, 0.41f, 1.62f, 1.64f, 0.06f, 0.26f, 0.96f, 0.85f, primary, accent, CandyMaterials.MAT_CANDY)
                addPeppermintDisc(acc, -0.92f, 0.46f, 1.66f, 0.22f, 0.05f, primary, creamWhite, verticalX = true)
                addPeppermintDisc(acc, 0.92f, 0.46f, 1.66f, 0.22f, 0.05f, primary, creamWhite, verticalX = true)
                // Heart-Candy Airbox Scoop + Candy-Cane Rear Wing Struts & Peppermint Endplates
                acc.addHorizontalCylinderZ(0f, 1.24f, -0.58f, 0.18f, 0.16f, 0.56f, 10, primary, creamWhite, CandyMaterials.MAT_CANDY)
                addCandyCaneTube(acc, Vec3(-0.34f, 0.82f, -1.24f), Vec3(-0.46f, 1.28f, -1.32f), 0.05f, 6, primary, creamWhite)
                addCandyCaneTube(acc, Vec3(0.34f, 0.82f, -1.24f), Vec3(0.46f, 1.28f, -1.32f), 0.05f, 6, primary, creamWhite)
                acc.addBeveledBox(0f, 1.28f, -1.34f, 1.68f, 0.10f, 0.44f, 0.95f, 0.85f, primary, creamWhite, CandyMaterials.MAT_CANDY)
                addPeppermintDisc(acc, -0.86f, 1.28f, -1.34f, 0.24f, 0.05f, primary, creamWhite, verticalX = true)
                addPeppermintDisc(acc, 0.86f, 1.28f, -1.34f, 0.24f, 0.05f, primary, creamWhite, verticalX = true)
                acc.addBeveledBox(0f, 0.36f, -1.52f, 1.32f, 0.16f, 0.24f, 0.9f, 0.85f, darkCocoa, matType = CandyMaterials.MAT_CARAMEL)
            }

            KartBodyStyle.CHOCOLATE_CRUISER -> {
                // Rancis Fluggerbutter's "Peanut Butter Cup" Chocolate & Golden Peanut-Butter Wedge Kart
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
                // Fluted Reese's Peanut-Butter-Cup Ridges along both body sides
                for (side in listOf(-1f, 1f)) {
                    for (rib in -5..5) {
                        val rz = rib * 0.22f
                        acc.addCylinder(side * 0.85f, 0.44f, rz, 0.055f, 0.068f, 0.36f, 6, if (rib % 2 == 0) primary else secondary, accent, CandyMaterials.MAT_CARAMEL)
                    }
                }
                // Molded Chocolate Roof Canopy Pillars & Peanut-Butter-Cup Roof Top (matching Sketchfab model)
                for (side in listOf(-1f, 1f)) {
                    acc.addStrut(Vec3(side * 0.42f, 0.88f, 0.42f), Vec3(side * 0.34f, 1.48f, 0.10f), 0.048f, 6, primary, CandyMaterials.MAT_CARAMEL)
                    acc.addStrut(Vec3(side * 0.44f, 0.88f, -0.56f), Vec3(side * 0.36f, 1.48f, -0.44f), 0.052f, 6, primary, CandyMaterials.MAT_CARAMEL)
                }
                acc.addCylinder(0f, 1.46f, -0.16f, 0.44f, 0.50f, 0.14f, 14, primary, secondary, CandyMaterials.MAT_CARAMEL)
                // 3D Molded Chocolate Bar Squares on Hood
                for (row in 0..1) {
                    for (col in -1..1) {
                        val sqX = col * 0.34f
                        val sqZ = 0.82f + row * 0.38f
                        acc.addBeveledBox(sqX, 0.78f, sqZ, 0.28f, 0.10f, 0.30f, 0.76f, 0.76f, primary, secondary, CandyMaterials.MAT_CARAMEL)
                    }
                }
                // Chocolate & Wafer Bullbars + Caramel Ducktail Spoiler
                acc.addBeveledBox(0f, 0.42f, 1.72f, 1.70f, 0.24f, 0.28f, 0.95f, 0.82f, secondary, accent, CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0f, 0.42f, -1.58f, 1.70f, 0.24f, 0.26f, 0.95f, 0.85f, darkCocoa, secondary, CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0f, 1.06f, -1.36f, 1.68f, 0.18f, 0.38f, 0.94f, 0.80f, secondary, accent, CandyMaterials.MAT_WAFER)
                addCandySprinkles(acc, 0f, 1.25f, -1.36f, 0.68f, 0.12f, count = 12)
            }

            KartBodyStyle.JELLYBEAN_MACHINE -> {
                // Swizzle "The Swizz" Malarkey's "Tongue Twister" Neon Gummy & Swirl Lollipop Racer
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.44f, 0.68f, 0.38f, 0.90f, secondary, primary),
                        LoftStation(-0.35f, 0.84f, 0.36f, 0.96f, primary, primary),
                        LoftStation(0.65f, 0.76f, 0.36f, 0.86f, primary, secondary),
                        LoftStation(1.56f, 0.46f, 0.40f, 0.66f, secondary, primary)
                    ),
                    matType = CandyMaterials.MAT_GUMMY
                )
                acc.addSphere(0f, 0.86f, 0.66f, 0.56f, 0.26f, 0.72f, 7, 12, 0xFF80FFE5L, primary, matType = CandyMaterials.MAT_GUMMY)
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
                addPeppermintDisc(acc, 0f, 0.54f, 1.58f, 0.26f, 0.06f, accent, primary, verticalX = false)
                acc.addBeveledBox(0f, 0.38f, 1.58f, 1.52f, 0.16f, 0.28f, 0.9f, 0.8f, chrome, accent, CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 0.38f, -1.50f, 1.46f, 0.18f, 0.24f, 0.9f, 0.8f, darkCocoa, matType = CandyMaterials.MAT_CARAMEL)
                acc.addStrut(Vec3(-0.42f, 0.84f, -1.20f), Vec3(-0.50f, 1.16f, -1.28f), 0.05f, 6, secondary, CandyMaterials.MAT_GUMMY)
                acc.addStrut(Vec3(0.42f, 0.84f, -1.20f), Vec3(0.50f, 1.16f, -1.28f), 0.05f, 6, secondary, CandyMaterials.MAT_GUMMY)
                acc.addBeveledBox(0f, 1.14f, -1.28f, 1.56f, 0.10f, 0.38f, 0.92f, 0.85f, accent, primary, matType = CandyMaterials.MAT_GUMMY)
            }

            KartBodyStyle.CARAMEL_THUNDER -> {
                // =========================================================================
                // KING CANDY'S ROYAL SOVEREIGN KART (Matching Sketchfab `img_kart_king_candy.jpg`)
                // =========================================================================
                val royalPink = primary       // Salmon-pink royal chassis (0xFFFF5E7E)
                val royalCream = secondary    // Whipped white-cream fenders & side trim (0xFFFFF8E7)
                val royalGold = accent        // Golden crown & royal crest (0xFFFFC107)
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.48f, 0.80f, 0.34f, 0.90f, royalPink, royalCream),
                        LoftStation(-0.40f, 0.86f, 0.34f, 0.88f, royalPink, royalPink),
                        LoftStation(0.60f, 0.78f, 0.34f, 0.80f, royalPink, royalCream),
                        LoftStation(1.44f, 0.62f, 0.34f, 0.64f, royalPink, royalGold),
                        LoftStation(1.80f, 0.42f, 0.36f, 0.50f, royalCream, royalGold)
                    ),
                    matType = CandyMaterials.MAT_CANDY
                )
                // Royal Cream Scalloped Side Fenders & Golden Piped Trim
                acc.addBeveledBox(-0.84f, 0.56f, 0.04f, 0.26f, 0.24f, 1.56f, 0.88f, 0.86f, royalCream, royalGold, CandyMaterials.MAT_MARSHMALLOW)
                acc.addBeveledBox(0.84f, 0.56f, 0.04f, 0.26f, 0.24f, 1.56f, 0.88f, 0.86f, royalCream, royalGold, CandyMaterials.MAT_MARSHMALLOW)
                addPipedFrostingLine(acc, Vec3(-0.74f, 0.78f, -1.10f), Vec3(-0.66f, 0.72f, 1.32f), 10, 0.068f, royalCream, royalGold)
                addPipedFrostingLine(acc, Vec3(0.74f, 0.78f, -1.10f), Vec3(0.66f, 0.72f, 1.32f), 10, 0.068f, royalCream, royalGold)
                // Glowing Golden Front Royal Crest Grille & Crown Emblem on Nose
                acc.addSphere(0f, 0.58f, 1.78f, 0.28f, 0.20f, 0.10f, 7, 10, royalGold, 0xFFFFF59DL, CandyMaterials.MAT_EMISSIVE)
                // Giant Sculpted Golden Royal Crown Behind the Cockpit Seat (Signature feature from Sketchfab model!)
                val crownZ = -0.84f
                acc.addCylinder(0f, 0.90f, crownZ, 0.44f, 0.54f, 0.48f, 12, royalGold, 0xFFFFA000L, CandyMaterials.MAT_CHROME)
                for (i in 0 until 7) {
                    val a = (i.toFloat() / 6f - 0.5f) * PI.toFloat() * 1.15f
                    val px = sin(a) * 0.50f
                    val pz = crownZ - cos(a) * 0.26f
                    acc.addCylinder(px, 1.36f, pz, 0.08f, 0.02f, 0.26f, 6, royalGold, royalCream, CandyMaterials.MAT_CHROME)
                    acc.addSphere(px, 1.64f, pz, 0.065f, 0.065f, 0.065f, 5, 6, if (i % 2 == 0) royalPink else 0xFF00E5FFL, matType = CandyMaterials.MAT_EMISSIVE)
                }
                // Twin Royal Pennant Flags behind the Crown
                for (side in listOf(-1f, 1f)) {
                    acc.addStrut(Vec3(side * 0.48f, 0.88f, -1.24f), Vec3(side * 0.56f, 1.76f, -1.36f), 0.032f, 6, royalGold, CandyMaterials.MAT_CHROME)
                    acc.addBeveledBox(side * 0.56f, 1.66f, -1.48f, 0.04f, 0.16f, 0.26f, 0.8f, 0.8f, royalPink, royalGold, CandyMaterials.MAT_CANDY)
                }
                acc.addBeveledBox(0f, 0.38f, -1.54f, 1.56f, 0.18f, 0.24f, 0.9f, 0.85f, royalCream, royalGold, CandyMaterials.MAT_MARSHMALLOW)
            }

            KartBodyStyle.CUPCAKE_KART -> {
                // =========================================================================
                // CANDLEHEAD'S BIRTHDAY CAKE KART (5 Lit Birthday Candles at Rear — Sketchfab)
                // =========================================================================
                val cakeChoco = primary       // Rich chocolate sponge cake body (0xFF5D2E1E)
                val pinkFrost = secondary     // Bright strawberry-pink cake frosting top (0xFFFF7EB9)
                val mintTrim = accent         // Mint-green frosting trim (0xFF3CE8B5)
                // Chocolate Cake Wedge Hull topped with Thick Pink Birthday Frosting
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.46f, 0.78f, 0.34f, 0.92f, cakeChoco, pinkFrost),
                        LoftStation(-0.42f, 0.86f, 0.34f, 0.90f, cakeChoco, pinkFrost),
                        LoftStation(0.54f, 0.76f, 0.35f, 0.80f, cakeChoco, pinkFrost),
                        LoftStation(1.42f, 0.62f, 0.36f, 0.64f, cakeChoco, pinkFrost),
                        LoftStation(1.76f, 0.38f, 0.38f, 0.48f, pinkFrost, mintTrim)
                    ),
                    matType = CandyMaterials.MAT_CARAMEL
                )
                // Thick Pink Birthday Cake Frosting Blanket over the Hood & Rear Deck
                acc.addBeveledBox(0f, 0.86f, 0.86f, 1.18f, 0.10f, 1.06f, 0.86f, 0.82f, pinkFrost, creamWhite, CandyMaterials.MAT_MARSHMALLOW)
                acc.addBeveledBox(0f, 0.92f, -1.04f, 1.52f, 0.12f, 0.68f, 0.90f, 0.88f, pinkFrost, creamWhite, CandyMaterials.MAT_MARSHMALLOW)
                // Piped Whipped-Cream Scallop Borders along the Cake Sides
                addPipedFrostingLine(acc, Vec3(-0.74f, 0.78f, -1.24f), Vec3(-0.56f, 0.68f, 1.48f), 11, 0.075f, creamWhite, pinkFrost)
                addPipedFrostingLine(acc, Vec3(0.74f, 0.78f, -1.24f), Vec3(0.56f, 0.68f, 1.48f), 11, 0.075f, creamWhite, pinkFrost)
                addCandySprinkles(acc, 0f, 0.94f, 0.88f, 0.48f, 0.40f, count = 18)
                // 5 Tall Lit Pink-and-White Striped Birthday Candles Standing in a Row at the Rear!
                val candleXs = listOf(-0.58f, -0.29f, 0f, 0.29f, 0.58f)
                for (cx in candleXs) {
                    acc.addSphere(cx, 0.98f, -1.16f, 0.11f, 0.07f, 0.11f, 5, 6, mintTrim, creamWhite, CandyMaterials.MAT_MARSHMALLOW)
                    addCandyCaneTube(acc, Vec3(cx, 1.00f, -1.16f), Vec3(cx, 1.58f, -1.18f), 0.062f, 6, pinkFrost, creamWhite)
                    // Glowing Golden Birthday Candle Flame
                    acc.addSphere(cx, 1.68f, -1.18f, 0.058f, 0.11f, 0.058f, 5, 6, 0xFFFFD600L, 0xFFFF6D00L, CandyMaterials.MAT_EMISSIVE)
                }
                acc.addBeveledBox(0f, 0.36f, 1.62f, 1.48f, 0.14f, 0.28f, 0.9f, 0.8f, waferGold, mintTrim, CandyMaterials.MAT_WAFER)
                acc.addBeveledBox(0f, 0.36f, -1.46f, 1.48f, 0.16f, 0.24f, 0.9f, 0.8f, cakeChoco, matType = CandyMaterials.MAT_CARAMEL)
            }

            KartBodyStyle.DONUT_DRIFTER -> {
                // Gloyd Orangeboar's "Pumpkin Corn Racer" & Glazed Donut Drift Ring
                acc.addTorus(0f, 0.58f, 0.06f, 0.92f, 0.38f, 20, 10, waferGold, primary, matType = CandyMaterials.MAT_WAFER)
                acc.addTorus(0f, 0.72f, 0.06f, 0.90f, 0.26f, 20, 8, primary, secondary, matType = CandyMaterials.MAT_CANDY)
                val dripCount = 18
                for (i in 0 until dripCount) {
                    val a = (i.toFloat() / dripCount) * (2f * PI.toFloat())
                    val dx = cos(a) * 1.10f
                    val dz = 0.06f + sin(a) * 1.12f
                    val dropY = 0.62f + (i % 2) * 0.05f
                    acc.addSphere(dx, dropY, dz, 0.11f, 0.14f, 0.11f, 5, 6, primary, accent, matType = CandyMaterials.MAT_CANDY)
                }
                val sprinkleCount = 24
                for (i in 0 until sprinkleCount) {
                    val a = (i.toFloat() / sprinkleCount) * (2f * PI.toFloat())
                    val rad = 0.78f + (i % 3) * 0.09f
                    val sx = cos(a) * rad
                    val sz = 0.06f + sin(a) * rad
                    val col = SPRINKLE_PALETTE[i % SPRINKLE_PALETTE.size]
                    acc.addBox(sx, 0.96f, sz, 0.11f, 0.045f, 0.045f, col, matType = CandyMaterials.MAT_CANDY)
                }
                acc.addBeveledBox(0f, 0.36f, 1.56f, 1.54f, 0.14f, 0.30f, 0.92f, 0.8f, chrome, accent, CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 0.36f, -1.46f, 1.48f, 0.16f, 0.24f, 0.92f, 0.8f, darkCocoa, matType = CandyMaterials.MAT_CARAMEL)
                acc.addStrut(Vec3(-0.42f, 0.84f, -1.16f), Vec3(-0.50f, 1.16f, -1.26f), 0.05f, 6, waferGold, CandyMaterials.MAT_WAFER)
                acc.addStrut(Vec3(0.42f, 0.84f, -1.16f), Vec3(0.50f, 1.16f, -1.26f), 0.05f, 6, waferGold, CandyMaterials.MAT_WAFER)
                acc.addBeveledBox(0f, 1.16f, -1.26f, 1.56f, 0.10f, 0.40f, 0.94f, 0.85f, primary, accent, CandyMaterials.MAT_CANDY)
                addCandySprinkles(acc, 0f, 1.27f, -1.26f, 0.66f, 0.14f, count = 10)
            }

            KartBodyStyle.MARSHMALLOW_SPEEDSTER -> {
                // =========================================================================
                // ADORABEEZLE WINTERPOP'S "ROCKET POP" POPSICLE KART (Red / White / Blue)
                // =========================================================================
                val popRed = primary          // Cherry Rocket-Pop Red Front (0xFFE51C38)
                val popWhite = secondary      // Lemon-Ice White Mid Hull (0xFFFFFFFF)
                val popBlue = accent          // Blue-Raspberry Rear & Spoiler (0xFF1E88E5)
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.52f, 0.74f, 0.35f, 0.88f, popBlue, popBlue),
                        LoftStation(-0.54f, 0.80f, 0.35f, 0.88f, popBlue, popWhite),
                        LoftStation(0.32f, 0.78f, 0.35f, 0.84f, popWhite, popWhite),
                        LoftStation(1.06f, 0.68f, 0.36f, 0.74f, popRed, popWhite),
                        LoftStation(1.80f, 0.36f, 0.38f, 0.56f, popRed, popRed)
                    ),
                    matType = CandyMaterials.MAT_CANDY
                )
                // Fluted Rocket-Pop Popsicle Groove Ribs along Both Sides
                for (side in listOf(-1f, 1f)) {
                    acc.addHorizontalCylinderZ(side * 0.74f, 0.58f, -0.84f, 0.18f, 0.18f, 0.88f, 10, popBlue, popWhite, CandyMaterials.MAT_CANDY)
                    acc.addHorizontalCylinderZ(side * 0.72f, 0.58f, 0.04f, 0.18f, 0.18f, 0.88f, 10, popWhite, popRed, CandyMaterials.MAT_MARSHMALLOW)
                    acc.addHorizontalCylinderZ(side * 0.66f, 0.56f, 0.90f, 0.16f, 0.14f, 0.84f, 10, popRed, popRed, CandyMaterials.MAT_CANDY)
                }
                // Wooden Popsicle-Stick Rear Bumper & Blue-Raspberry Rocket Wing
                acc.addBeveledBox(0f, 0.42f, -1.62f, 0.68f, 0.10f, 0.36f, 0.92f, 0.92f, waferGold, waferGold, CandyMaterials.MAT_WAFER)
                acc.addStrut(Vec3(-0.42f, 0.82f, -1.24f), Vec3(-0.48f, 1.24f, -1.34f), 0.055f, 6, popBlue, CandyMaterials.MAT_CANDY)
                acc.addStrut(Vec3(0.42f, 0.82f, -1.24f), Vec3(0.48f, 1.24f, -1.34f), 0.055f, 6, popBlue, CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0f, 1.24f, -1.34f, 1.66f, 0.12f, 0.42f, 0.94f, 0.86f, popBlue, popWhite, CandyMaterials.MAT_CANDY)
            }

            KartBodyStyle.CHERRY_PIE_KART -> {
                // =========================================================================
                // JUBILEENA BING-BING'S "CHERRY BOMB" PIE SLICE KART (`img_kart_jubileena.jpg`)
                // =========================================================================
                val cherryRed = primary       // Glossy Cherry Pie Filling Red (0xFFE51C38)
                val pieCrust = secondary      // Golden Baked Flaky Pie Crust (0xFFF4C27F)
                val pinkIcing = accent        // Pink Icing Ribbon (0xFFFF758F)
                // Triangular Pie-Slice Wedge Hull (Wide Crimped Rear Crust tapering to Pointed Pie-Tip Nose)
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.50f, 0.88f, 0.34f, 0.88f, pieCrust, cherryRed),
                        LoftStation(-0.48f, 0.82f, 0.34f, 0.84f, cherryRed, pieCrust),
                        LoftStation(0.52f, 0.68f, 0.35f, 0.76f, cherryRed, pieCrust),
                        LoftStation(1.36f, 0.48f, 0.36f, 0.64f, pieCrust, cherryRed),
                        LoftStation(1.84f, 0.22f, 0.38f, 0.52f, pieCrust, pieCrust)
                    ),
                    matType = CandyMaterials.MAT_WAFER
                )
                // Upper Lattice Pie-Crust Strips & Crimped Pie-Crust Edge Scallops
                addPipedFrostingLine(acc, Vec3(-0.82f, 0.82f, -1.36f), Vec3(-0.22f, 0.58f, 1.68f), 11, 0.072f, pieCrust, waferGold)
                addPipedFrostingLine(acc, Vec3(0.82f, 0.82f, -1.36f), Vec3(0.22f, 0.58f, 1.68f), 11, 0.072f, pieCrust, waferGold)
                addPipedFrostingLine(acc, Vec3(-0.84f, 0.90f, -1.42f), Vec3(0.84f, 0.90f, -1.42f), 9, 0.095f, pieCrust, waferGold)
                // Glossy Red Cherries bulging along the Open Pie-Slice Sides & Hood!
                for (side in listOf(-1f, 1f)) {
                    for (ci in 0 until 6) {
                        val cz = -1.10f + ci * 0.42f
                        val cx = side * (0.76f - ci * 0.075f)
                        acc.addSphere(cx, 0.60f, cz, 0.14f, 0.14f, 0.14f, 6, 8, cherryRed, pinkIcing, CandyMaterials.MAT_CANDY)
                    }
                }
                // Giant Cherry-Pie Crust Rear Spoiler with Twin Cherries on Top
                acc.addStrut(Vec3(-0.44f, 0.86f, -1.26f), Vec3(-0.52f, 1.24f, -1.36f), 0.055f, 6, pieCrust, CandyMaterials.MAT_WAFER)
                acc.addStrut(Vec3(0.44f, 0.86f, -1.26f), Vec3(0.52f, 1.24f, -1.36f), 0.055f, 6, pieCrust, CandyMaterials.MAT_WAFER)
                acc.addBeveledBox(0f, 1.24f, -1.36f, 1.64f, 0.11f, 0.40f, 0.94f, 0.86f, pieCrust, cherryRed, CandyMaterials.MAT_WAFER)
                acc.addSphere(0f, 0.72f, 1.32f, 0.18f, 0.18f, 0.18f, 6, 8, cherryRed, pinkIcing, CandyMaterials.MAT_CANDY)
            }

            KartBodyStyle.FRO_CONE_KART -> {
                // =========================================================================
                // SNOWANNA RAINBEAU'S "FRO CONE" SNOW-CONE KART (`img_kart_snowanna.jpg`)
                // =========================================================================
                val conePink = primary        // Hot-Pink Paper Snow-Cone Cup Body (0xFFFF3D9A)
                val grapeScoop = secondary    // Purple Air Scoop & Trim (0xFF7B2CBF)
                val sherbetOrange = accent    // Orange-Sherbet Accents (0xFFFF9E00)
                // Tilted Pink Paper Snow-Cone Cup Cockpit & Rear Cone Tub
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.52f, 0.42f, 0.38f, 0.78f, creamWhite, conePink),
                        LoftStation(-0.62f, 0.76f, 0.36f, 0.92f, conePink, conePink),
                        LoftStation(0.28f, 0.84f, 0.35f, 0.94f, creamWhite, conePink),
                        LoftStation(0.96f, 0.62f, 0.36f, 0.80f, grapeScoop, sherbetOrange),
                        LoftStation(1.72f, 0.52f, 0.38f, 0.72f, 0xFF80F8FFL, creamWhite)
                    ),
                    matType = CandyMaterials.MAT_CANDY
                )
                // White Rolled Paper-Cup Rim around the Middle Cockpit
                acc.addTorus(0f, 0.78f, -0.04f, 0.82f, 0.075f, 16, 6, creamWhite, conePink, matType = CandyMaterials.MAT_MARSHMALLOW)
                // Translucent Ice-Cube Front Engine Block with Purple Intake Scoop (exact match to `img_kart_snowanna.jpg`)
                acc.addBeveledBox(0f, 0.64f, 1.28f, 0.92f, 0.36f, 0.78f, 0.92f, 0.92f, 0xFFB2F5FFL, 0xFFE0FFFFL, CandyMaterials.MAT_GUMMY)
                acc.addBeveledBox(0f, 0.98f, 1.18f, 0.46f, 0.16f, 0.44f, 0.84f, 0.84f, grapeScoop, conePink, CandyMaterials.MAT_CANDY)
                // Red & Orange Shaved-Ice Side Exhaust Pipes
                for (side in listOf(-1f, 1f)) {
                    addCandyCaneTube(acc, Vec3(side * 0.64f, 0.52f, 0.62f), Vec3(side * 0.84f, 0.64f, -0.48f), 0.058f, 6, 0xFFFF1744L, sherbetOrange)
                }
                acc.addBeveledBox(0f, 0.38f, -1.50f, 1.42f, 0.16f, 0.24f, 0.9f, 0.85f, grapeScoop, conePink, CandyMaterials.MAT_CANDY)
            }

            KartBodyStyle.TIRAMISSILE_KART -> {
                // =========================================================================
                // CRUMBELINA DICARAMELLO'S "TIRA-MISSILE #8" (`img_kart_crumbelina.jpg`)
                // =========================================================================
                val mascarponeCream = primary // Whipped Mascarpone Cream White (0xFFFFF8EE)
                val espressoBrown = secondary // Dark Espresso Cocoa Stripes (0xFF4A2511)
                val ladyfingerGold = accent   // Ladyfinger Savoiardi Wafer Gold (0xFFD4A373)
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.48f, 0.76f, 0.35f, 0.88f, espressoBrown, mascarponeCream),
                        LoftStation(-0.42f, 0.84f, 0.34f, 0.90f, mascarponeCream, espressoBrown),
                        LoftStation(0.56f, 0.76f, 0.35f, 0.82f, mascarponeCream, espressoBrown),
                        LoftStation(1.42f, 0.58f, 0.36f, 0.68f, espressoBrown, mascarponeCream),
                        LoftStation(1.80f, 0.36f, 0.38f, 0.52f, mascarponeCream, ladyfingerGold)
                    ),
                    matType = CandyMaterials.MAT_MARSHMALLOW
                )
                // Diagonal Espresso-Cocoa Tiramisu Racing Stripes Across the Hood & Flanks
                for (stripe in -2..2) {
                    val sz = stripe * 0.52f
                    acc.addBeveledBox(0f, 0.78f, sz, 1.36f, 0.08f, 0.18f, 0.90f, 0.85f, espressoBrown, ladyfingerGold, CandyMaterials.MAT_CARAMEL)
                }
                // Ladyfinger Biscuit Side Skirts & "#8" Racing Medallion Roundels on Hood and Doors
                for (side in listOf(-1f, 1f)) {
                    acc.addBeveledBox(side * 0.82f, 0.52f, 0.02f, 0.14f, 0.18f, 2.20f, 0.92f, 0.92f, ladyfingerGold, espressoBrown, CandyMaterials.MAT_WAFER)
                    addPeppermintDisc(acc, side * 0.86f, 0.68f, 0.18f, 0.18f, 0.03f, mascarponeCream, espressoBrown, verticalX = true)
                }
                addPeppermintDisc(acc, 0f, 0.74f, 1.24f, 0.22f, 0.03f, mascarponeCream, espressoBrown, verticalX = false)
                // Espresso Wafer Rear Spoiler
                acc.addStrut(Vec3(-0.44f, 0.84f, -1.24f), Vec3(-0.50f, 1.22f, -1.34f), 0.05f, 6, espressoBrown, CandyMaterials.MAT_CARAMEL)
                acc.addStrut(Vec3(0.44f, 0.84f, -1.24f), Vec3(0.50f, 1.22f, -1.34f), 0.05f, 6, espressoBrown, CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0f, 1.22f, -1.34f, 1.62f, 0.10f, 0.40f, 0.94f, 0.86f, espressoBrown, mascarponeCream, CandyMaterials.MAT_WAFER)
            }

            KartBodyStyle.MINT_CANDY_KART -> {
                // =========================================================================
                // MINTY ZAKI'S "MELON MINT CRUISER" (Mint Ice-Cream & Choco-Chip Racer)
                // =========================================================================
                val mintIce = primary         // Mint-Green Ice Cream Hull (0xFF2CE6B0)
                val darkChip = secondary      // Dark Chocolate Chips (0xFF3B1E12)
                val cherryAccent = accent     // Maraschino Cherry Accent (0xFFFF2E63)
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.46f, 0.76f, 0.35f, 0.88f, mintIce, darkChip),
                        LoftStation(-0.42f, 0.84f, 0.34f, 0.90f, mintIce, mintIce),
                        LoftStation(0.54f, 0.76f, 0.35f, 0.82f, mintIce, darkChip),
                        LoftStation(1.40f, 0.58f, 0.36f, 0.66f, mintIce, creamWhite),
                        LoftStation(1.78f, 0.34f, 0.38f, 0.50f, creamWhite, mintIce)
                    ),
                    matType = CandyMaterials.MAT_CANDY
                )
                // 3D Dark Chocolate Chips Studded Across the Mint Ice-Cream Hood & Rear Deck
                for (row in -2..2) {
                    for (col in -1..1) {
                        acc.addSphere(col * 0.38f, 0.82f, row * 0.48f, 0.08f, 0.05f, 0.08f, 5, 6, darkChip, matType = CandyMaterials.MAT_CARAMEL)
                    }
                }
                addPipedFrostingLine(acc, Vec3(-0.72f, 0.78f, -1.16f), Vec3(-0.56f, 0.68f, 1.38f), 10, 0.068f, creamWhite, mintIce)
                addPipedFrostingLine(acc, Vec3(0.72f, 0.78f, -1.16f), Vec3(0.56f, 0.68f, 1.38f), 10, 0.068f, creamWhite, mintIce)
                acc.addSphere(0f, 0.74f, 1.36f, 0.16f, 0.16f, 0.16f, 6, 8, cherryAccent, creamWhite, CandyMaterials.MAT_CANDY)
                acc.addStrut(Vec3(-0.42f, 0.84f, -1.24f), Vec3(-0.48f, 1.22f, -1.32f), 0.05f, 6, darkChip, CandyMaterials.MAT_CARAMEL)
                acc.addStrut(Vec3(0.42f, 0.84f, -1.24f), Vec3(0.48f, 1.22f, -1.32f), 0.05f, 6, darkChip, CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0f, 1.22f, -1.32f, 1.60f, 0.10f, 0.38f, 0.94f, 0.86f, mintIce, darkChip, CandyMaterials.MAT_CANDY)
            }
        }

        // 2. Sculpted Candy Wheel Fenders with Piped Frosting Trim (except Open-Wheel Official Candy Kart & Peppermint F1)
        if (kart.style != KartBodyStyle.PEPPERMINT_RACER && kart.style != KartBodyStyle.STRAWBERRY_ROCKET) {
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

        // 6. Neck & Sculpted Expressive 3D Anime/Movie Head & Face
        acc.addCylinder(0f, 0.45f, 0.02f, 0.082f, 0.078f, 0.115f, 12, skin)
        val headY = 0.645f
        val headZ = 0.042f
        // Main sculpted head with refined high-resolution anime cranium & jawline
        acc.addSphere(0f, headY + 0.01f, headZ, 0.216f, 0.210f, 0.212f, 14, 18, skin)
        // Sculpted anime cheeks & tapered chin contour
        acc.addSphere(0f, headY - 0.048f, headZ + 0.038f, 0.194f, 0.126f, 0.178f, 10, 14, skin)
        acc.addSphere(0f, headY - 0.115f, headZ + 0.075f, 0.088f, 0.068f, 0.095f, 8, 10, skin)

        // Left & Right Ears with inner ear detail & anime earrings where applicable
        for (side in listOf(-1f, 1f)) {
            acc.addSphere(side * 0.212f, headY - 0.01f, headZ - 0.01f, 0.044f, 0.060f, 0.038f, 6, 8, skin)
            acc.addSphere(side * 0.218f, headY - 0.01f, headZ + 0.005f, 0.024f, 0.035f, 0.020f, 5, 6, 0xFFFF8DA1L)
        }

        // Soft Anime Blush Cheeks + Subtle Cheek Highlight Dot
        for (side in listOf(-1f, 1f)) {
            acc.addSphere(side * 0.124f, headY - 0.040f, headZ + 0.170f, 0.046f, 0.026f, 0.022f, 6, 8, 0xFFFF6699L, matType = CandyMaterials.MAT_CANDY)
            acc.addSphere(side * 0.132f, headY - 0.034f, headZ + 0.184f, 0.014f, 0.009f, 0.010f, 4, 5, 0xFFFFFFFFL, matType = CandyMaterials.MAT_EMISSIVE)
        }

        // Refined 3D Anime Nose Bridge & Tip
        acc.addSphere(0f, headY - 0.010f, headZ + 0.215f, 0.025f, 0.028f, 0.032f, 6, 8, skin)

        // Cheerful Anime Smiling Mouth (upper lip contour, warm smile cavity, bright white teeth)
        acc.addBeveledBox(0f, headY - 0.072f, headZ + 0.190f, 0.086f, 0.030f, 0.028f, 0.8f, 0.8f, 0xFFD81B60L, 0xFFFF80ABL, CandyMaterials.MAT_CANDY)
        acc.addBox(0f, headY - 0.065f, headZ + 0.198f, 0.064f, 0.015f, 0.018f, 0xFFFFFFFFL, matType = CandyMaterials.MAT_EMISSIVE)

        // Large Expressive Multi-Layer Anime Eyes (Sclera + Dual-Ring Iris + Deep Pupil + Triple Catchlights + Eyelashes + Brows)
        for (side in listOf(-1f, 1f)) {
            val ex = side * 0.084f
            val ey = headY + 0.020f
            val ez = headZ + 0.184f
            // White sclera
            acc.addSphere(ex, ey, ez, 0.055f, 0.061f, 0.034f, 8, 10, 0xFFFFFFFFL)
            // Outer vibrant iris ring
            acc.addSphere(ex, ey, ez + 0.016f, 0.041f, 0.047f, 0.023f, 8, 10, vHex, pHex, matType = CandyMaterials.MAT_CANDY)
            // Inner glowing iris gradient core
            acc.addSphere(ex, ey - 0.005f, ez + 0.023f, 0.032f, 0.036f, 0.018f, 6, 8, pHex, vHex, matType = CandyMaterials.MAT_EMISSIVE)
            // Deep dark pupil
            acc.addSphere(ex, ey + 0.002f, ez + 0.030f, 0.022f, 0.026f, 0.015f, 6, 8, 0xFF12061EL)
            // Triple bright white anime specular eye catchlights
            acc.addSphere(ex - side * 0.014f, ey + 0.017f, ez + 0.038f, 0.012f, 0.012f, 0.009f, 5, 6, 0xFFFFFFFFL, matType = CandyMaterials.MAT_EMISSIVE)
            acc.addSphere(ex + side * 0.012f, ey - 0.014f, ez + 0.037f, 0.007f, 0.007f, 0.006f, 4, 5, 0xFFFFFFFFL, matType = CandyMaterials.MAT_EMISSIVE)
            acc.addSphere(ex + side * 0.015f, ey + 0.012f, ez + 0.036f, 0.005f, 0.005f, 0.005f, 4, 5, 0xFFFFFFFFL, matType = CandyMaterials.MAT_EMISSIVE)
            // Sculpted upper eyelash wing & lower lash line
            acc.addBeveledBox(ex, ey + 0.051f, ez + 0.014f, 0.092f, 0.017f, 0.028f, 0.9f, 0.9f, 0xFF1A0926L)
            acc.addSphere(ex + side * 0.044f, ey + 0.046f, ez + 0.012f, 0.018f, 0.010f, 0.016f, 4, 5, 0xFF1A0926L)
            // Expressive arched eyebrow
            acc.addBeveledBox(ex, ey + 0.082f, ez + 0.010f, 0.086f, 0.018f, 0.025f, 0.85f, 0.85f, hHex, sHex)
        }

        // 7. Character-Specific Sculpted 3D Hairstyles & Signature Sugar Rush Movie + Anime Headgear
        when (racer.id) {
            "berry_blitz" -> {
                // VANELLOPE VON SCHWEETZ: Glossy Black Hair + High Ponytail tied with Red Licorice Twist + Candy Sprinkles in Hair + Mint Hoodie Hood
                val blackHair = 0xFF14131AL
                val hairSpec = 0xFF2B2838L
                val licoriceRed = 0xFFFF1744L
                acc.addTorus(0f, 0.44f, -0.06f, 0.19f, 0.068f, 12, 6, 0xFF2CE6B0L, 0xFF1FAF86L, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(0f, headY + 0.055f, headZ - 0.03f, 0.228f, 0.198f, 0.218f, 10, 14, blackHair, hairSpec, matType = CandyMaterials.MAT_CANDY)
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.175f, headY + 0.02f, headZ + 0.09f, 0.062f, 0.125f, 0.075f, 6, 8, blackHair, hairSpec)
                    acc.addSphere(side * 0.075f, headY + 0.135f, headZ + 0.155f, 0.088f, 0.055f, 0.065f, 6, 8, blackHair, hairSpec)
                }
                acc.addTorus(0f, headY + 0.18f, headZ - 0.14f, 0.068f, 0.026f, 10, 5, licoriceRed, 0xFFFF5252L, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(-0.07f, headY + 0.19f, headZ - 0.14f, 0.045f, 0.028f, 0.028f, 5, 6, licoriceRed, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(0.07f, headY + 0.19f, headZ - 0.14f, 0.045f, 0.028f, 0.028f, 5, 6, licoriceRed, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(0f, headY + 0.23f, headZ - 0.24f, 0.138f, 0.170f, 0.148f, 10, 12, blackHair, hairSpec, matType = CandyMaterials.MAT_CANDY)
                for (sp in 0 until 14) {
                    val a = (sp.toFloat() / 14f) * (2f * PI.toFloat())
                    val sx = cos(a) * 0.175f
                    val sz = headZ - 0.02f + sin(a) * 0.165f
                    val col = SPRINKLE_PALETTE[sp % SPRINKLE_PALETTE.size]
                    acc.addSphere(sx, headY + 0.18f + (sp % 3) * 0.025f, sz, 0.032f, 0.024f, 0.032f, 5, 6, col, matType = CandyMaterials.MAT_CANDY)
                }
            }

            "minty_swirl" -> {
                // TAFFYTA MUTTONFUDGE: Golden-Blonde Bob Hair + Pink Strawberry Hard-Candy Helmet with White Dots & Green Leaf Crown
                val blondeHair = 0xFFFFE082L
                val strawberryPink = 0xFFFF2E7EL
                val leafGreen = 0xFF2ECC71L
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.195f, headY - 0.04f, headZ + 0.06f, 0.065f, 0.115f, 0.078f, 6, 8, blondeHair, cream, matType = CandyMaterials.MAT_MARSHMALLOW)
                }
                acc.addSphere(0f, headY + 0.075f, headZ - 0.01f, 0.242f, 0.215f, 0.232f, 10, 14, strawberryPink, 0xFFFF80ABL, matType = CandyMaterials.MAT_CANDY)
                for (i in 0 until 8) {
                    val a = (i.toFloat() / 8f) * (2f * PI.toFloat())
                    acc.addSphere(cos(a) * 0.19f, headY + 0.16f, headZ + sin(a) * 0.18f, 0.026f, 0.026f, 0.026f, 4, 5, cream, matType = CandyMaterials.MAT_MARSHMALLOW)
                }
                acc.addSphere(0f, headY + 0.275f, headZ - 0.01f, 0.135f, 0.038f, 0.135f, 6, 8, leafGreen, 0xFF00E676L, matType = CandyMaterials.MAT_CANDY)
                acc.addCylinder(0f, headY + 0.28f, headZ - 0.01f, 0.024f, 0.018f, 0.08f, 6, leafGreen, matType = CandyMaterials.MAT_CANDY)
            }

            "frosting_faye" -> {
                // CANDLEHEAD: Pastel Mint-Green Hair + Pink Frosted Cupcake Helmet + Lit Birthday Candle & Glowing Flame on Top!
                val mintHair = 0xFF64FFDAL
                val cakePink = 0xFFFF80ABL
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.195f, headY - 0.03f, headZ + 0.06f, 0.068f, 0.125f, 0.082f, 6, 8, mintHair, cream, matType = CandyMaterials.MAT_MARSHMALLOW)
                }
                acc.addSphere(0f, headY + 0.08f, headZ - 0.01f, 0.240f, 0.205f, 0.230f, 10, 14, cakePink, cream, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addTorus(0f, headY + 0.10f, headZ - 0.01f, 0.225f, 0.036f, 12, 6, cream, cakePink, matType = CandyMaterials.MAT_MARSHMALLOW)
                addCandyCaneTube(acc, Vec3(0f, headY + 0.26f, headZ - 0.01f), Vec3(0f, headY + 0.44f, headZ - 0.01f), 0.042f, 5, cream, cakePink)
                acc.addSphere(0f, headY + 0.50f, headZ - 0.01f, 0.046f, 0.085f, 0.046f, 6, 8, 0xFFFFD600L, 0xFFFF6D00L, CandyMaterials.MAT_EMISSIVE)
            }

            "coco_dash" -> {
                // RANCIS FLUGGERBUTTER: Swept Golden-Blonde Hair + Tilted Red/Brown Fluted Reese's Peanut-Butter-Cup Hat
                val blondeHair = 0xFFFFD54FL
                val pbCupRed = 0xFFD82E2EL
                val chocoTrim = 0xFF4E2A1EL
                acc.addSphere(0f, headY + 0.05f, headZ - 0.02f, 0.226f, 0.192f, 0.216f, 10, 14, blondeHair, 0xFFFFF59DL, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(0.06f, headY + 0.135f, headZ + 0.165f, 0.115f, 0.058f, 0.068f, 6, 8, blondeHair, cream, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addCylinder(0f, headY + 0.18f, headZ - 0.01f, 0.175f, 0.215f, 0.135f, 14, pbCupRed, chocoTrim, matType = CandyMaterials.MAT_WAFER)
                acc.addTorus(0f, headY + 0.185f, headZ - 0.01f, 0.185f, 0.026f, 12, 5, gold, chocoTrim, matType = CandyMaterials.MAT_CHROME)
            }

            "jubileena" -> {
                // JUBILEENA BING-BING: Glossy Red Cherry Helmet with Green Stem + Twin Crimson Pigtails
                val cherryRed = 0xFFE51C38L
                val leafGreen = 0xFF2ECC71L
                acc.addSphere(0f, headY + 0.075f, headZ - 0.01f, 0.242f, 0.215f, 0.232f, 10, 14, cherryRed, 0xFFFF758FL, matType = CandyMaterials.MAT_CANDY)
                acc.addStrut(Vec3(0f, headY + 0.27f, headZ - 0.01f), Vec3(0.06f, headY + 0.42f, headZ - 0.06f), 0.024f, 6, leafGreen, CandyMaterials.MAT_CANDY)
                acc.addSphere(-0.06f, headY + 0.28f, headZ, 0.075f, 0.025f, 0.045f, 5, 6, leafGreen, matType = CandyMaterials.MAT_CANDY)
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.235f, headY - 0.03f, headZ - 0.03f, 0.052f, 0.052f, 0.052f, 5, 6, cream, matType = CandyMaterials.MAT_MARSHMALLOW)
                    acc.addSphere(side * 0.295f, headY - 0.10f, headZ - 0.04f, 0.082f, 0.115f, 0.082f, 6, 8, cherryRed, 0xFFFF5252L, matType = CandyMaterials.MAT_CANDY)
                }
            }

            "snowanna" -> {
                // SNOWANNA RAINBEAU: Giant Rainbow Shaved-Ice Afro Hair
                val rainbowCols = longArrayOf(0xFFFF3D9AL, 0xFFFF9E00L, 0xFFFFEA00L, 0xFF00E676L, 0xFF00E5FFL, 0xFFAA00FFL)
                acc.addSphere(0f, headY + 0.14f, headZ - 0.03f, 0.34f, 0.31f, 0.32f, 10, 14, 0xFFFF3D9AL, 0xFFFF9E00L, matType = CandyMaterials.MAT_MARSHMALLOW)
                for (i in 0 until 12) {
                    val a = (i.toFloat() / 12f) * (2f * PI.toFloat())
                    val rx = cos(a) * 0.24f
                    val rz = headZ - 0.03f + sin(a) * 0.22f
                    val ry = headY + 0.14f + (i % 3) * 0.055f
                    val c1 = rainbowCols[i % rainbowCols.size]
                    val c2 = rainbowCols[(i + 1) % rainbowCols.size]
                    acc.addSphere(rx, ry, rz, 0.135f, 0.135f, 0.135f, 6, 8, c1, c2, matType = CandyMaterials.MAT_MARSHMALLOW)
                }
            }

            "crumbelina" -> {
                // CRUMBELINA DICARAMELLO: Espresso-Brown Curls + Whipped-Cream & Cocoa Tiramisu Beret Hat
                val cocoaBrown = 0xFF4A2511L
                val mascarpone = 0xFFFFF8EEL
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.195f, headY - 0.04f, headZ + 0.06f, 0.068f, 0.120f, 0.075f, 6, 8, cocoaBrown, mascarpone, matType = CandyMaterials.MAT_CARAMEL)
                }
                acc.addCylinder(0f, headY + 0.145f, headZ - 0.01f, 0.205f, 0.215f, 0.048f, 12, cocoaBrown, gold, matType = CandyMaterials.MAT_CARAMEL)
                acc.addSphere(0f, headY + 0.235f, headZ - 0.01f, 0.255f, 0.115f, 0.245f, 8, 12, mascarpone, cocoaBrown, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(0f, headY + 0.34f, headZ - 0.01f, 0.065f, 0.055f, 0.065f, 6, 8, cocoaBrown, gold, matType = CandyMaterials.MAT_CARAMEL)
            }

            "mallow_max" -> {
                // ADORABEEZLE WINTERPOP: Red-White-Blue Rocket-Pop Astronaut Helmet + Fur Parka Collar + Cyan Visor
                val popRed = 0xFFE51C38L
                val popWhite = 0xFFFFFFFFL
                val popBlue = 0xFF1E88E5L
                acc.addTorus(0f, 0.46f, 0.0f, 0.185f, 0.062f, 12, 6, popWhite, 0xFF80D8FFL, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addSphere(0f, headY + 0.06f, headZ - 0.01f, 0.244f, 0.195f, 0.234f, 10, 14, popBlue, popWhite, matType = CandyMaterials.MAT_CANDY)
                acc.addCylinder(0f, headY + 0.15f, headZ - 0.01f, 0.215f, 0.165f, 0.095f, 12, popWhite, popRed, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addCylinder(0f, headY + 0.24f, headZ - 0.01f, 0.165f, 0.065f, 0.115f, 12, popRed, popRed, matType = CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0f, headY + 0.125f, headZ + 0.19f, 0.26f, 0.055f, 0.042f, 0.85f, 0.85f, 0xFF00E5FFL, popWhite, CandyMaterials.MAT_EMISSIVE)
            }

            "glazed_gus" -> {
                // GLOYD ORANGEBOAR: Pumpkin / Candy-Corn Trickster Cap
                val pumpkinOrange = 0xFFFF7A00L
                val candyYellow = 0xFFFFD600L
                val darkBrown = 0xFF3E1F0DL
                acc.addSphere(0f, headY + 0.065f, headZ - 0.01f, 0.238f, 0.205f, 0.228f, 10, 14, pumpkinOrange, candyYellow, matType = CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0f, headY + 0.125f, headZ + 0.185f, 0.24f, 0.035f, 0.10f, 0.85f, 0.9f, darkBrown, pumpkinOrange, CandyMaterials.MAT_CARAMEL)
                acc.addCylinder(0f, headY + 0.26f, headZ - 0.01f, 0.045f, 0.022f, 0.09f, 6, 0xFF2ECC71L, candyYellow, matType = CandyMaterials.MAT_CANDY)
            }

            "minty_zaki" -> {
                // MINTY ZAKI: Mint-Green Helmet & Big Red Maraschino Bow on Top
                val mintGreen = 0xFF2CE6B0L
                val bowRed = 0xFFFF2E63L
                acc.addSphere(0f, headY + 0.065f, headZ - 0.01f, 0.238f, 0.205f, 0.228f, 10, 14, mintGreen, cream, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(0f, headY + 0.275f, headZ + 0.02f, 0.048f, 0.048f, 0.048f, 5, 6, bowRed, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(-0.11f, headY + 0.285f, headZ + 0.02f, 0.085f, 0.058f, 0.045f, 6, 8, bowRed, cream, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(0.11f, headY + 0.285f, headZ + 0.02f, 0.085f, 0.058f, 0.045f, 6, 8, bowRed, cream, matType = CandyMaterials.MAT_CANDY)
            }

            "toffee_rex" -> {
                // KING CANDY: Royal Golden Crown with Glowing Gems + Pink Oversized Bow Tie on Chest
                val royalGold = 0xFFFFC107L
                val royalPink = 0xFFFF3D8BL
                acc.addSphere(-0.085f, 0.42f, 0.175f, 0.068f, 0.046f, 0.032f, 5, 6, royalPink, cream, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(0.085f, 0.42f, 0.175f, 0.068f, 0.046f, 0.032f, 5, 6, royalPink, cream, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(0f, 0.42f, 0.182f, 0.035f, 0.035f, 0.030f, 5, 6, royalGold, matType = CandyMaterials.MAT_CHROME)
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.22f, headY + 0.04f, headZ - 0.02f, 0.055f, 0.065f, 0.065f, 5, 6, cream, matType = CandyMaterials.MAT_MARSHMALLOW)
                }
                acc.addCylinder(0f, headY + 0.19f, headZ - 0.01f, 0.145f, 0.175f, 0.135f, 10, royalGold, 0xFFFFA000L, matType = CandyMaterials.MAT_CHROME)
                for (i in 0 until 5) {
                    val a = (i.toFloat() / 5f) * (2f * PI.toFloat())
                    acc.addSphere(cos(a) * 0.16f, headY + 0.34f, headZ - 0.01f + sin(a) * 0.16f, 0.042f, 0.052f, 0.042f, 5, 6, if (i % 2 == 0) royalPink else 0xFF00E5FFL, matType = CandyMaterials.MAT_EMISSIVE)
                }
            }

            "anime_goku" -> {
                // SON GOKU (Super Saiyan): Iconic Golden Glowing Spiky Anime Hair Crown + Forehead Bangs + Blue Gi Sash
                val ssjGold = 0xFFFFEA00L
                val ssjGlow = 0xFFFFB300L
                acc.addSphere(0f, headY + 0.07f, headZ - 0.02f, 0.232f, 0.205f, 0.220f, 8, 12, ssjGold, ssjGlow, matType = CandyMaterials.MAT_EMISSIVE)
                // 9 Iconic Super Saiyan Upward & Outward Golden Spikes
                val spikeDirs = listOf(
                    Vec3(0f, 0.44f, -0.04f),
                    Vec3(-0.26f, 0.36f, -0.02f),
                    Vec3(0.26f, 0.36f, -0.02f),
                    Vec3(-0.34f, 0.20f, -0.05f),
                    Vec3(0.34f, 0.20f, -0.05f),
                    Vec3(-0.18f, 0.40f, -0.18f),
                    Vec3(0.18f, 0.40f, -0.18f),
                    Vec3(-0.10f, 0.26f, 0.24f),
                    Vec3(0.10f, 0.26f, 0.24f)
                )
                for (sp in spikeDirs) {
                    acc.addStrut(Vec3(0f, headY + 0.10f, headZ - 0.02f), Vec3(sp.x, headY + sp.y, headZ + sp.z), 0.072f, 6, ssjGold, CandyMaterials.MAT_EMISSIVE)
                    acc.addSphere(sp.x, headY + sp.y, headZ + sp.z, 0.045f, 0.065f, 0.045f, 5, 6, ssjGlow, ssjGold, matType = CandyMaterials.MAT_EMISSIVE)
                }
            }

            "anime_naruto" -> {
                // NARUTO UZUMAKI: Bright Yellow Spiky Hair + Blue Konoha Headband with Silver Metal Plate + Cheek Whisker Marks
                val narutoYellow = 0xFFFFD600L
                val headbandBlue = 0xFF1565C0L
                val plateSilver = 0xFFE0E6EDL
                // Cheek whisker marks (3 per cheek)
                for (side in listOf(-1f, 1f)) {
                    for (w in -1..1) {
                        acc.addBox(side * 0.135f, headY - 0.028f + w * 0.018f, headZ + 0.175f, 0.042f, 0.007f, 0.012f, 0xFF4E2A1EL)
                    }
                }
                // Blue Ninja Forehead Protector Band + Chrome Metal Plate
                acc.addTorus(0f, headY + 0.115f, headZ + 0.005f, 0.218f, 0.036f, 14, 6, headbandBlue, 0xFF0D47A1L, matType = CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0f, headY + 0.120f, headZ + 0.218f, 0.16f, 0.058f, 0.026f, 0.9f, 0.9f, plateSilver, 0xFFFFFFFFL, CandyMaterials.MAT_CHROME)
                // Spiky Yellow Hair Crown above headband
                acc.addSphere(0f, headY + 0.15f, headZ - 0.02f, 0.225f, 0.165f, 0.215f, 8, 12, narutoYellow, 0xFFFFF176L, matType = CandyMaterials.MAT_CANDY)
                for (i in 0 until 8) {
                    val a = (i.toFloat() / 8f) * (2f * PI.toFloat())
                    acc.addSphere(cos(a) * 0.18f, headY + 0.27f + (i % 2) * 0.04f, headZ - 0.02f + sin(a) * 0.17f, 0.068f, 0.095f, 0.068f, 5, 6, narutoYellow, 0xFFFFEA00L, matType = CandyMaterials.MAT_CANDY)
                }
            }

            "anime_luffy" -> {
                // MONKEY D. LUFFY: Shaggy Black Anime Hair + Iconic Golden Straw Hat with Red Ribbon Brim + Under-Eye Scar
                val blackHair = 0xFF16161EL
                val strawGold = 0xFFFBC02DL
                val ribbonRed = 0xFFD50000L
                // Under-eye scar on left cheek
                acc.addBox(-0.085f, headY - 0.022f, headZ + 0.196f, 0.036f, 0.008f, 0.012f, 0xFF5D2E1EL)
                // Shaggy black hair tufts under straw hat
                acc.addSphere(0f, headY + 0.05f, headZ - 0.02f, 0.228f, 0.185f, 0.218f, 8, 12, blackHair)
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.185f, headY + 0.02f, headZ + 0.08f, 0.058f, 0.105f, 0.068f, 5, 7, blackHair)
                }
                // Wide Straw Hat Brim + Red Band + Straw Dome
                acc.addCylinder(0f, headY + 0.155f, headZ - 0.01f, 0.36f, 0.35f, 0.028f, 16, strawGold, 0xFFF57F17L, matType = CandyMaterials.MAT_WAFER)
                acc.addCylinder(0f, headY + 0.175f, headZ - 0.01f, 0.215f, 0.208f, 0.048f, 14, ribbonRed, 0xFFFF1744L, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(0f, headY + 0.225f, headZ - 0.01f, 0.212f, 0.145f, 0.208f, 10, 14, strawGold, 0xFFFFEB3BL, matType = CandyMaterials.MAT_WAFER)
            }

            "anime_gojo" -> {
                // SATORU GOJO: Snowy White Swept-Up Spiky Hair + Dark Blindfold / Visor Band + High Jujutsu Collar
                val whiteHair = 0xFFF5F8FFL
                val iceBlue = 0xFF00E5FFL
                val blindfoldBlack = 0xFF141622L
                // High dark Jujutsu collar
                acc.addCylinder(0f, 0.455f, 0.01f, 0.175f, 0.185f, 0.085f, 12, blindfoldBlack, 0xFF283593L, matType = CandyMaterials.MAT_CANDY)
                // Sleek black eye-band with glowing Six-Eyes cyan rim
                acc.addTorus(0f, headY + 0.045f, headZ + 0.01f, 0.216f, 0.042f, 14, 6, blindfoldBlack, iceBlue, matType = CandyMaterials.MAT_CANDY)
                // Swept-up silver-white spiky hair crown
                acc.addSphere(0f, headY + 0.13f, headZ - 0.02f, 0.232f, 0.195f, 0.222f, 10, 14, whiteHair, iceBlue, matType = CandyMaterials.MAT_MARSHMALLOW)
                for (i in 0 until 9) {
                    val a = (i.toFloat() / 9f) * (2f * PI.toFloat())
                    acc.addSphere(cos(a) * 0.16f, headY + 0.29f + (i % 2) * 0.05f, headZ - 0.02f + sin(a) * 0.15f, 0.068f, 0.115f, 0.068f, 5, 7, whiteHair, iceBlue, matType = CandyMaterials.MAT_MARSHMALLOW)
                }
            }

            "anime_tanjiro" -> {
                // TANJIRO KAMADO: Burgundy-Black Swept Hair + Flame Forehead Mark + Iconic Hanafuda Earrings
                val burgundyHair = 0xFF5D1420L
                val darkRoot = 0xFF1C1018L
                // Forehead Sun-Breathing Flame Scar on upper-left forehead
                acc.addSphere(-0.095f, headY + 0.115f, headZ + 0.178f, 0.052f, 0.036f, 0.020f, 5, 6, 0xFFB71C1CL, matType = CandyMaterials.MAT_CANDY)
                // Swept-back burgundy hair with spiky tips
                acc.addSphere(0f, headY + 0.085f, headZ - 0.03f, 0.234f, 0.205f, 0.225f, 10, 14, darkRoot, burgundyHair, matType = CandyMaterials.MAT_CANDY)
                for (i in 0 until 7) {
                    val a = (i.toFloat() / 7f) * (2f * PI.toFloat())
                    acc.addSphere(cos(a) * 0.17f, headY + 0.23f, headZ - 0.04f + sin(a) * 0.16f, 0.072f, 0.085f, 0.072f, 5, 7, burgundyHair, 0xFFD50000L, matType = CandyMaterials.MAT_CANDY)
                }
                // Rectangular White & Red Rising-Sun Hanafuda Earrings hanging below ears
                for (side in listOf(-1f, 1f)) {
                    acc.addBox(side * 0.225f, headY - 0.095f, headZ + 0.01f, 0.016f, 0.068f, 0.042f, 0xFFFFFFFFL, matType = CandyMaterials.MAT_EMISSIVE)
                    acc.addSphere(side * 0.230f, headY - 0.085f, headZ + 0.01f, 0.012f, 0.018f, 0.018f, 4, 5, 0xFFD50000L, matType = CandyMaterials.MAT_CANDY)
                }
            }

            "anime_nezuko" -> {
                // NEZUKO KAMADO: Long Black Hair with Flame-Orange Ombre Tips + Green Bamboo Muzzle + Pink Hair Ribbon
                val blackHair = 0xFF16141DL
                val flameOrange = 0xFFFF6D00L
                val bambooGreen = 0xFF2ECC71L
                val ribbonPink = 0xFFFF80ABL
                // Green Bamboo Muzzle across mouth with red side ties
                acc.addCylinder(0f, headY - 0.068f, headZ + 0.205f, 0.032f, 0.032f, 0.036f, 8, bambooGreen, 0xFF00C853L, matType = CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0f, headY - 0.068f, headZ + 0.205f, 0.155f, 0.044f, 0.044f, 0.9f, 0.9f, bambooGreen, 0xFFD50000L, CandyMaterials.MAT_CANDY)
                // Long flowing black hair cap + orange flame-tipped locks + pink hair bow
                acc.addSphere(0f, headY + 0.07f, headZ - 0.03f, 0.234f, 0.205f, 0.226f, 10, 14, blackHair, flameOrange, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(-0.14f, headY + 0.21f, headZ + 0.08f, 0.058f, 0.042f, 0.042f, 5, 6, ribbonPink, matType = CandyMaterials.MAT_CANDY)
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.21f, headY - 0.08f, headZ - 0.04f, 0.078f, 0.155f, 0.085f, 6, 8, blackHair, flameOrange)
                    acc.addSphere(side * 0.22f, headY - 0.22f, headZ - 0.05f, 0.072f, 0.095f, 0.075f, 6, 8, flameOrange, 0xFFFFAB00L, matType = CandyMaterials.MAT_EMISSIVE)
                }
            }

            "anime_zoro" -> {
                // RORONOA ZORO: Signature Emerald-Green Spiky Crop Hair + Triple Golden Left-Ear Earrings + Sword Hilts on Back
                val zoroGreen = 0xFF00E676L
                val darkGreen = 0xFF1B5E20L
                acc.addSphere(0f, headY + 0.08f, headZ - 0.02f, 0.232f, 0.202f, 0.222f, 10, 14, zoroGreen, darkGreen, matType = CandyMaterials.MAT_CANDY)
                for (i in 0 until 8) {
                    val a = (i.toFloat() / 8f) * (2f * PI.toFloat())
                    acc.addSphere(cos(a) * 0.16f, headY + 0.24f, headZ - 0.02f + sin(a) * 0.15f, 0.065f, 0.075f, 0.065f, 5, 6, zoroGreen, darkGreen, matType = CandyMaterials.MAT_CANDY)
                }
                // Triple gold drop earrings on left ear
                for (e in 0..2) {
                    acc.addSphere(-0.226f, headY - 0.055f - e * 0.018f, headZ + (e - 1) * 0.015f, 0.012f, 0.020f, 0.012f, 4, 5, gold, matType = CandyMaterials.MAT_CHROME)
                }
            }

            "anime_Deku" -> {
                // IZUKU MIDORIYA (DEKU): Fluffy Dark-Green & Emerald Curly Spikes + Freckles + Hero Respirator Collar
                val dekuDark = 0xFF114B3EL
                val dekuEmerald = 0xFF00E676L
                acc.addTorus(0f, 0.455f, 0.02f, 0.175f, 0.048f, 12, 6, 0xFFD0D8E0L, dekuEmerald, matType = CandyMaterials.MAT_CHROME)
                acc.addSphere(0f, headY + 0.08f, headZ - 0.02f, 0.236f, 0.205f, 0.226f, 10, 14, dekuDark, dekuEmerald, matType = CandyMaterials.MAT_CANDY)
                for (i in 0 until 10) {
                    val a = (i.toFloat() / 10f) * (2f * PI.toFloat())
                    acc.addSphere(cos(a) * 0.19f, headY + 0.21f + (i % 2) * 0.05f, headZ - 0.02f + sin(a) * 0.18f, 0.082f, 0.092f, 0.082f, 6, 8, if (i % 2 == 0) dekuDark else dekuEmerald, dekuEmerald, matType = CandyMaterials.MAT_CANDY)
                }
            }

            "anime_anya" -> {
                // ANYA FORGER: Pastel Pink Bob Hair + Twin Iconic Black & Gold Cone Hair Ornaments
                val anyaPink = 0xFFFF94B8L
                val coneBlack = 0xFF1A1A24L
                acc.addSphere(0f, headY + 0.07f, headZ - 0.02f, 0.234f, 0.205f, 0.224f, 10, 14, anyaPink, 0xFFFFC1E3L, matType = CandyMaterials.MAT_MARSHMALLOW)
                for (side in listOf(-1f, 1f)) {
                    // Bob side locks
                    acc.addSphere(side * 0.205f, headY - 0.03f, headZ + 0.05f, 0.068f, 0.128f, 0.082f, 6, 8, anyaPink, cream, matType = CandyMaterials.MAT_MARSHMALLOW)
                    // Iconic Black & Gold Cone Ornaments on Top Sides of Head
                    acc.addCylinder(side * 0.155f, headY + 0.22f, headZ + 0.01f, 0.068f, 0.014f, 0.135f, 10, coneBlack, gold, matType = CandyMaterials.MAT_CANDY)
                    acc.addTorus(side * 0.155f, headY + 0.225f, headZ + 0.01f, 0.068f, 0.016f, 10, 5, gold, coneBlack, matType = CandyMaterials.MAT_CHROME)
                }
            }

            "anime_sailor" -> {
                // SAILOR MOON: Golden Blonde Twin Odango Buns + Flowing Pigtails + Golden Crescent Moon Tiara + Red Sailor Bow
                val usagiGold = 0xFFFFEA00L
                val sailorRed = 0xFFFF1744L
                // Red Sailor Chest Bow
                acc.addSphere(-0.085f, 0.39f, 0.175f, 0.072f, 0.048f, 0.032f, 5, 6, sailorRed, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(0.085f, 0.39f, 0.175f, 0.072f, 0.048f, 0.032f, 5, 6, sailorRed, matType = CandyMaterials.MAT_CANDY)
                acc.addSphere(0f, 0.39f, 0.185f, 0.038f, 0.038f, 0.028f, 5, 6, gold, matType = CandyMaterials.MAT_CHROME)
                // Forehead Golden Tiara with Red Gem
                acc.addBeveledBox(0f, headY + 0.105f, headZ + 0.208f, 0.16f, 0.034f, 0.022f, 0.85f, 0.85f, gold, cream, CandyMaterials.MAT_CHROME)
                acc.addSphere(0f, headY + 0.105f, headZ + 0.222f, 0.022f, 0.026f, 0.016f, 5, 6, sailorRed, matType = CandyMaterials.MAT_EMISSIVE)
                // Golden Hair Cap + Twin Odango Buns + Long Flowing Twin-Tails
                acc.addSphere(0f, headY + 0.075f, headZ - 0.02f, 0.232f, 0.205f, 0.222f, 10, 14, usagiGold, 0xFFFFF59DL, matType = CandyMaterials.MAT_CANDY)
                for (side in listOf(-1f, 1f)) {
                    acc.addSphere(side * 0.185f, headY + 0.245f, headZ - 0.02f, 0.082f, 0.082f, 0.082f, 8, 10, usagiGold, sailorRed, matType = CandyMaterials.MAT_CANDY)
                    acc.addSphere(side * 0.185f, headY + 0.245f, headZ + 0.045f, 0.036f, 0.036f, 0.024f, 5, 6, sailorRed, cream, matType = CandyMaterials.MAT_EMISSIVE)
                    acc.addStrut(Vec3(side * 0.20f, headY + 0.20f, headZ - 0.06f), Vec3(side * 0.27f, headY - 0.18f, headZ - 0.14f), 0.052f, 8, usagiGold, CandyMaterials.MAT_CANDY)
                }
            }

            "anime_miku" -> {
                // HATSUNE MIKU: Cyan-Turquoise Twin-Tails + Magenta/Black Futuristic Hair Clips + Cyber Headset & Tie
                val mikuCyan = 0xFF00E5FFL
                val mikuTeal = 0xFF00BFA5L
                val clipMagenta = 0xFFFF1744L
                // Cyan Tie on Chest
                acc.addBeveledBox(0f, 0.31f, 0.165f, 0.052f, 0.18f, 0.024f, 0.7f, 0.9f, mikuCyan, mikuTeal, CandyMaterials.MAT_CANDY)
                // Turquoise Hair Cap + Bangs
                acc.addSphere(0f, headY + 0.075f, headZ - 0.02f, 0.232f, 0.205f, 0.222f, 10, 14, mikuCyan, mikuTeal, matType = CandyMaterials.MAT_CANDY)
                for (side in listOf(-1f, 1f)) {
                    // Cyber Headset Earcups
                    acc.addSphere(side * 0.225f, headY + 0.01f, headZ, 0.038f, 0.065f, 0.065f, 6, 8, 0xFF1A1E29L, mikuCyan, matType = CandyMaterials.MAT_EMISSIVE)
                    // Square Magenta/Black Hair Clips
                    acc.addBeveledBox(side * 0.215f, headY + 0.185f, headZ - 0.02f, 0.055f, 0.055f, 0.055f, 0.9f, 0.9f, clipMagenta, 0xFF1A1E29L, CandyMaterials.MAT_EMISSIVE)
                    // Giant Flowing Cyan Twin-Tails
                    acc.addStrut(Vec3(side * 0.23f, headY + 0.17f, headZ - 0.03f), Vec3(side * 0.32f, headY - 0.22f, headZ - 0.12f), 0.068f, 8, mikuCyan, CandyMaterials.MAT_CANDY)
                }
            }

            "anime_killua" -> {
                // KILLUA ZOLDYCK: Fluffy Silver-White Spiky Anime Hair + Electric Blue Godspeed Sparks
                val silverHair = 0xFFE8EEFCL
                val godspeedBlue = 0xFF00E5FFL
                acc.addSphere(0f, headY + 0.085f, headZ - 0.02f, 0.236f, 0.208f, 0.226f, 10, 14, silverHair, godspeedBlue, matType = CandyMaterials.MAT_MARSHMALLOW)
                for (i in 0 until 10) {
                    val a = (i.toFloat() / 10f) * (2f * PI.toFloat())
                    acc.addSphere(cos(a) * 0.19f, headY + 0.22f + (i % 3) * 0.035f, headZ - 0.02f + sin(a) * 0.18f, 0.075f, 0.095f, 0.075f, 6, 8, silverHair, godspeedBlue, matType = CandyMaterials.MAT_MARSHMALLOW)
                }
            }

            else -> {
                // SWIZZLE "THE SWIZZ" MALARKEY ("fizzy_pop"): Purple & Neon-Green Striped Stunt Helmet + Visor Shades
                val swizzPurple = 0xFF7B2CBFL
                val swizzGreen = 0xFF00E676L
                val swizzBlue = 0xFF00B0FFL
                acc.addSphere(0f, headY + 0.07f, headZ - 0.01f, 0.240f, 0.210f, 0.230f, 10, 14, swizzBlue, swizzPurple, matType = CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0f, headY + 0.24f, headZ - 0.01f, 0.075f, 0.075f, 0.28f, 0.7f, 0.85f, swizzGreen, swizzPurple, CandyMaterials.MAT_CANDY)
                acc.addBeveledBox(0f, headY + 0.125f, headZ + 0.188f, 0.25f, 0.052f, 0.036f, 0.9f, 0.9f, 0xFFFFEA00L, swizzGreen, CandyMaterials.MAT_EMISSIVE)
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
                    // Ribbed pink-and-gold cream-sandwich biscuit ridges + embossed strawberry center medallion
                    for (k in 0 until 12) {
                        val a = (k.toFloat() / 12f) * (2f * PI.toFloat())
                        val ridgeHex = if (k % 2 == 0) 0xFFFF3D8BL else 0xFFFFB74DL
                        acc.addSphere(faceX, cos(a) * 0.37f, sin(a) * 0.37f, 0.024f, 0.046f, 0.046f, 4, 6, ridgeHex, matType = CandyMaterials.MAT_CANDY)
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
                WheelDesign.PEPPERMINT_SWIRL -> if (isSpoke) CandyMaterials.MAT_CANDY else CandyMaterials.MAT_WAFER
                else -> if (isSpoke) CandyMaterials.MAT_CANDY else CandyMaterials.MAT_CHROME
            }
            val y0 = cos(a0) * rimR; val z0 = sin(a0) * rimR
            val y1 = cos(a1) * rimR; val z1 = sin(a1) * rimR
            acc.addTri(halfW, 0f, 0f, halfW, y0, z0, halfW, y1, z1, rr, rg, rb, 1f, 0f, 0f, 1f, 0f, 0.5f, 1f, mat)
            acc.addTri(-halfW, 0f, 0f, -halfW, y1, z1, -halfW, y0, z0, rr, rg, rb, 1f, 0f, 0f, 1f, 0f, 0.5f, 1f, mat)
        }
        // Center strawberry/candy button hubcap spinner
        val hubHex = if (design == WheelDesign.PEPPERMINT_SWIRL) 0xFFFF2438L else design.primaryHex
        acc.addSphere(halfW + 0.025f, 0f, 0f, 0.058f, 0.115f, 0.115f, 6, 8, hubHex, design.rimHex, matType = CandyMaterials.MAT_CANDY)
        acc.addSphere(-halfW - 0.025f, 0f, 0f, 0.058f, 0.115f, 0.115f, 6, 8, hubHex, design.rimHex, matType = CandyMaterials.MAT_CANDY)
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
