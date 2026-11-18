package com.example.sweetrush.render3d

import com.example.sweetrush.model.CatalogData
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
 * Builds detailed multi-part 3D candy racing karts:
 * - Sculpted lofted chassis, nose cone, aero splitter & wheel well arches
 * - Front & rear bumpers
 * - Cockpit tub, contoured racing seat, steering column & animated steering wheel
 * - Emissive dashboard gauge cluster & chrome headlight bezels
 * - Twin/quad chrome exhausts & rear spoiler
 * - Emissive headlights + separate dynamic brake lights
 * - Independent sculpted treaded tires, alloy rims, brake discs/calipers, and suspension wishbones/struts
 * - Separate animated seated driver character with suit, arms, gloves, helmet & visor
 */
object VehicleMeshBuilder {

    fun buildKartBundle(
        kart: KartSpec,
        racer: RacerSpec,
        custom: KartCustomization
    ): KartModelBundle {
        val chassisMesh = buildDetailedChassis(kart, custom)
        val steeringWheelMesh = buildSteeringWheelMesh(kart, custom)
        val driverMesh = buildDriverMesh(racer)
        val brakeLightMesh = buildBrakeLights(kart)
        val tireMesh = buildSculptedTireMesh()
        val rimMesh = buildAlloyRimMesh(custom.wheelDesign)
        val suspensionMesh = buildSuspensionAssemblyMesh(kart)
        val shadowBlobMesh = MeshAccumulator().apply {
            addShadowOval(rx = 1.48f, rz = 1.95f, segs = 16, alpha = 0.44f)
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
        val darkCarbon = 0xFF1F1629L
        val chrome = 0xFFE5EAF0L

        // Undertray floor pan & sculpted side rails common to precision kart chassis
        acc.addBeveledBox(0f, 0.34f, 0.05f, 1.42f, 0.12f, 2.75f, 0.94f, 0.94f, darkCarbon, darkCarbon, CandyMaterials.MAT_CANDY)

        when (kart.style) {
            KartBodyStyle.STRAWBERRY_ROCKET -> {
                // Sleek torpedo fuselage with whipped-cream sidepods & swept rear rocket fins
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.45f, 0.62f, 0.36f, 0.84f, primary, secondary),
                        LoftStation(-0.55f, 0.74f, 0.36f, 0.88f, primary, secondary),
                        LoftStation(0.45f, 0.68f, 0.38f, 0.82f, primary, primary),
                        LoftStation(1.22f, 0.50f, 0.38f, 0.68f, secondary, secondary),
                        LoftStation(1.70f, 0.24f, 0.40f, 0.52f, primary, secondary)
                    )
                )
                // Berry-cream sidepod intakes
                acc.addBeveledBox(-0.74f, 0.56f, -0.15f, 0.34f, 0.32f, 1.25f, 0.78f, 0.86f, secondary, accent)
                acc.addBeveledBox(0.74f, 0.56f, -0.15f, 0.34f, 0.32f, 1.25f, 0.78f, 0.86f, secondary, accent)
                // Sculpted front splitter bumper
                acc.addBeveledBox(0f, 0.36f, 1.62f, 1.54f, 0.14f, 0.36f, 0.92f, 0.78f, chrome, secondary, CandyMaterials.MAT_CHROME)
                // Twin rocket rear fins & bi-plane spoiler
                acc.addBeveledBox(-0.68f, 0.90f, -1.16f, 0.12f, 0.54f, 0.66f, 0.9f, 0.75f, secondary, accent)
                acc.addBeveledBox(0.68f, 0.90f, -1.16f, 0.12f, 0.54f, 0.66f, 0.9f, 0.75f, secondary, accent)
                acc.addBeveledBox(0f, 1.18f, -1.24f, 1.56f, 0.10f, 0.40f, 0.95f, 0.85f, accent, primary)
                // Rear diffuser bumper
                acc.addBeveledBox(0f, 0.38f, -1.52f, 1.48f, 0.18f, 0.24f, 0.9f, 0.85f, darkCarbon)
            }

            KartBodyStyle.PEPPERMINT_RACER -> {
                // Open-wheel candy Formula-1 silhouette with stepped nose & multi-element wing
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.42f, 0.48f, 0.35f, 0.88f, primary, secondary),
                        LoftStation(-0.45f, 0.62f, 0.34f, 0.82f, primary, primary),
                        LoftStation(0.55f, 0.44f, 0.36f, 0.74f, secondary, primary),
                        LoftStation(1.38f, 0.26f, 0.38f, 0.56f, primary, secondary),
                        LoftStation(1.76f, 0.14f, 0.38f, 0.46f, secondary, secondary)
                    )
                )
                // Formula sidepods
                acc.addBeveledBox(-0.62f, 0.52f, -0.25f, 0.44f, 0.34f, 1.15f, 0.82f, 0.78f, secondary, primary)
                acc.addBeveledBox(0.62f, 0.52f, -0.25f, 0.44f, 0.34f, 1.15f, 0.82f, 0.78f, secondary, primary)
                // Wide front aero wing & endplates
                acc.addBeveledBox(0f, 0.34f, 1.66f, 1.76f, 0.10f, 0.38f, 0.98f, 0.85f, secondary, primary)
                acc.addBox(-0.88f, 0.42f, 1.66f, 0.08f, 0.28f, 0.44f, accent)
                acc.addBox(0.88f, 0.42f, 1.66f, 0.08f, 0.28f, 0.44f, accent)
                // Tall F1 rear wing + DRS struts
                acc.addStrut(Vec3(-0.32f, 0.82f, -1.24f), Vec3(-0.45f, 1.24f, -1.30f), 0.045f, 6, chrome)
                acc.addStrut(Vec3(0.32f, 0.82f, -1.24f), Vec3(0.45f, 1.24f, -1.30f), 0.045f, 6, chrome)
                acc.addBeveledBox(0f, 1.26f, -1.32f, 1.64f, 0.10f, 0.42f, 0.95f, 0.85f, secondary, primary)
                acc.addBox(-0.82f, 1.20f, -1.32f, 0.08f, 0.34f, 0.48f, accent)
                acc.addBox(0.82f, 1.20f, -1.32f, 0.08f, 0.34f, 0.48f, accent)
                // Rear bumper bar
                acc.addBeveledBox(0f, 0.36f, -1.52f, 1.28f, 0.16f, 0.22f, 0.9f, 0.85f, darkCarbon)
            }

            KartBodyStyle.CHOCOLATE_CRUISER -> {
                // Muscular widebody dark-fudge GT wedge with wafer hood scoop & flared fenders
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.48f, 0.82f, 0.36f, 0.92f, primary, secondary),
                        LoftStation(-0.50f, 0.86f, 0.34f, 0.88f, primary, primary),
                        LoftStation(0.50f, 0.84f, 0.34f, 0.86f, primary, secondary),
                        LoftStation(1.38f, 0.78f, 0.36f, 0.76f, primary, secondary),
                        LoftStation(1.66f, 0.66f, 0.38f, 0.64f, secondary, secondary)
                    )
                )
                // Supercharged wafer hood intake
                acc.addBeveledBox(0f, 0.92f, 0.85f, 0.68f, 0.22f, 0.78f, 0.82f, 0.75f, secondary, accent, CandyMaterials.MAT_WAFER)
                // Muscular front & rear bullbars
                acc.addBeveledBox(0f, 0.44f, 1.70f, 1.68f, 0.24f, 0.26f, 0.95f, 0.80f, chrome, secondary, CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 0.44f, -1.56f, 1.68f, 0.24f, 0.24f, 0.95f, 0.85f, chrome, darkCarbon, CandyMaterials.MAT_CHROME)
                // Rear ducktail spoiler
                acc.addBeveledBox(0f, 1.06f, -1.34f, 1.66f, 0.16f, 0.36f, 0.96f, 0.80f, accent, secondary, CandyMaterials.MAT_CARAMEL)
            }

            KartBodyStyle.JELLYBEAN_MACHINE -> {
                // Translucent neon-jelly canopy racer with curved pod & bubble cowl
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.40f, 0.66f, 0.38f, 0.86f, secondary, primary),
                        LoftStation(-0.35f, 0.80f, 0.36f, 0.92f, primary, primary),
                        LoftStation(0.65f, 0.72f, 0.36f, 0.82f, primary, secondary),
                        LoftStation(1.52f, 0.42f, 0.40f, 0.62f, secondary, primary)
                    ),
                    matType = CandyMaterials.MAT_GUMMY
                )
                acc.addSphere(0f, 0.84f, 0.65f, 0.54f, 0.24f, 0.68f, 6, 10, 0xFF80FFE5, primary, matType = CandyMaterials.MAT_GUMMY)
                acc.addBeveledBox(0f, 0.38f, 1.56f, 1.48f, 0.16f, 0.26f, 0.9f, 0.8f, chrome, accent, CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 0.38f, -1.48f, 1.44f, 0.18f, 0.24f, 0.9f, 0.8f, darkCarbon)
                acc.addBeveledBox(0f, 1.10f, -1.26f, 1.48f, 0.10f, 0.36f, 0.92f, 0.85f, accent, matType = CandyMaterials.MAT_GUMMY)
            }

            KartBodyStyle.CARAMEL_THUNDER -> {
                // Low-slung hyper-wedge Caramel Speedster with split-deck wing & sculpted fenders
                acc.addLoftedHull(
                    listOf(
                        LoftStation(-1.46f, 0.76f, 0.34f, 0.86f, primary, secondary),
                        LoftStation(-0.40f, 0.82f, 0.34f, 0.84f, primary, primary),
                        LoftStation(0.60f, 0.74f, 0.34f, 0.76f, primary, accent),
                        LoftStation(1.42f, 0.56f, 0.34f, 0.56f, secondary, primary),
                        LoftStation(1.74f, 0.34f, 0.36f, 0.44f, accent, secondary)
                    ),
                    matType = CandyMaterials.MAT_CARAMEL
                )
                // Flared wheel fenders
                acc.addBeveledBox(-0.82f, 0.62f, 0.92f, 0.28f, 0.24f, 0.74f, 0.85f, 0.85f, accent, matType = CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0.82f, 0.62f, 0.92f, 0.28f, 0.24f, 0.74f, 0.85f, 0.85f, accent, matType = CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0f, 0.34f, 1.72f, 1.66f, 0.10f, 0.34f, 0.96f, 0.82f, chrome, accent, CandyMaterials.MAT_CHROME)
                acc.addStrut(Vec3(-0.46f, 0.82f, -1.28f), Vec3(-0.56f, 1.22f, -1.36f), 0.05f, 6, chrome)
                acc.addStrut(Vec3(0.46f, 0.82f, -1.28f), Vec3(0.56f, 1.22f, -1.36f), 0.05f, 6, chrome)
                acc.addBeveledBox(0f, 1.24f, -1.36f, 1.74f, 0.12f, 0.44f, 0.96f, 0.85f, accent, primary, CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0f, 0.38f, -1.52f, 1.54f, 0.18f, 0.24f, 0.9f, 0.85f, darkCarbon)
            }

            KartBodyStyle.CUPCAKE_KART -> {
                acc.addCylinder(0f, 0.34f, 0.04f, 0.78f, 0.96f, 0.56f, 14, primary, secondary, matType = CandyMaterials.MAT_WAFER)
                acc.addSphere(0f, 0.88f, 0.32f, 0.84f, 0.32f, 0.86f, 6, 12, secondary, 0xFFFFFFFF, matType = CandyMaterials.MAT_MARSHMALLOW)
                acc.addBeveledBox(0f, 0.36f, 1.48f, 1.44f, 0.14f, 0.28f, 0.9f, 0.8f, chrome, accent, CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 0.36f, -1.38f, 1.40f, 0.16f, 0.24f, 0.9f, 0.8f, darkCarbon)
                acc.addSphere(0f, 1.22f, -0.92f, 0.24f, 0.24f, 0.24f, 6, 8, accent, matType = CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0f, 1.12f, -1.22f, 1.42f, 0.10f, 0.32f, 0.92f, 0.85f, secondary, matType = CandyMaterials.MAT_MARSHMALLOW)
            }

            KartBodyStyle.DONUT_DRIFTER -> {
                acc.addTorus(0f, 0.64f, 0.04f, 0.84f, 0.34f, 18, 8, primary, secondary, matType = CandyMaterials.MAT_CARAMEL)
                acc.addBeveledBox(0f, 0.36f, 1.52f, 1.50f, 0.14f, 0.28f, 0.92f, 0.8f, chrome, accent, CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 0.36f, -1.42f, 1.46f, 0.16f, 0.24f, 0.92f, 0.8f, darkCarbon)
                acc.addStrut(Vec3(-0.4f, 0.84f, -1.16f), Vec3(-0.48f, 1.12f, -1.24f), 0.045f, 6, chrome)
                acc.addStrut(Vec3(0.4f, 0.84f, -1.16f), Vec3(0.48f, 1.12f, -1.24f), 0.045f, 6, chrome)
                acc.addBeveledBox(0f, 1.14f, -1.24f, 1.50f, 0.10f, 0.38f, 0.94f, 0.85f, secondary, accent)
            }

            KartBodyStyle.MARSHMALLOW_SPEEDSTER -> {
                acc.addHorizontalCylinderZ(-0.56f, 0.58f, 0.1f, 0.34f, 0.30f, 2.55f, 12, primary, secondary, CandyMaterials.MAT_MARSHMALLOW)
                acc.addHorizontalCylinderZ(0.56f, 0.58f, 0.1f, 0.34f, 0.30f, 2.55f, 12, primary, secondary, CandyMaterials.MAT_MARSHMALLOW)
                acc.addBeveledBox(0f, 0.50f, 0.1f, 1.18f, 0.24f, 2.25f, 0.9f, 0.85f, secondary, accent, CandyMaterials.MAT_MARSHMALLOW)
                acc.addBeveledBox(0f, 0.38f, 1.56f, 1.52f, 0.14f, 0.28f, 0.92f, 0.8f, chrome, primary, CandyMaterials.MAT_CHROME)
                acc.addBeveledBox(0f, 1.14f, -1.22f, 1.48f, 0.10f, 0.36f, 0.92f, 0.85f, accent)
            }
        }

        // Sculpted Wheel Well Arches & Inner Mudguards (open-wheel Peppermint Racer keeps F1 aero wings)
        if (kart.style != KartBodyStyle.PEPPERMINT_RACER) {
            val archMat = if (kart.style == KartBodyStyle.JELLYBEAN_MACHINE) CandyMaterials.MAT_GUMMY
            else if (kart.style == KartBodyStyle.CARAMEL_THUNDER) CandyMaterials.MAT_CARAMEL
            else CandyMaterials.MAT_CANDY
            for ((wx, wz) in listOf(-0.84f to 0.96f, 0.84f to 0.96f, -0.88f to -0.96f, 0.88f to -0.96f)) {
                acc.addBeveledBox(wx, 0.90f, wz, 0.36f, 0.08f, 0.68f, 0.85f, 0.78f, secondary, accent, archMat)
                acc.addBeveledBox(wx, 0.70f, wz - 0.34f, 0.32f, 0.32f, 0.10f, 0.90f, 0.85f, darkCarbon)
            }
        }

        // Front Headlights (chrome bezels + bright warm LED lenses)
        val headlightZ = when (kart.style) {
            KartBodyStyle.CHOCOLATE_CRUISER, KartBodyStyle.CARAMEL_THUNDER -> 1.66f
            else -> 1.50f
        }
        acc.addHorizontalCylinderZ(-0.44f, 0.58f, headlightZ - 0.03f, 0.15f, 0.14f, 0.10f, 8, chrome, chrome, CandyMaterials.MAT_CHROME)
        acc.addHorizontalCylinderZ(0.44f, 0.58f, headlightZ - 0.03f, 0.15f, 0.14f, 0.10f, 8, chrome, chrome, CandyMaterials.MAT_CHROME)
        acc.addSphere(-0.44f, 0.58f, headlightZ + 0.01f, 0.12f, 0.10f, 0.08f, 5, 8, 0xFFFFF8D6, matType = CandyMaterials.MAT_EMISSIVE)
        acc.addSphere(0.44f, 0.58f, headlightZ + 0.01f, 0.12f, 0.10f, 0.08f, 5, 8, 0xFFFFF8D6, matType = CandyMaterials.MAT_EMISSIVE)

        // Detailed Cockpit: Recessed dash cowl + Emissive Digital Gauge Cluster + Contoured Bucket Seat + Steering Column
        acc.addBeveledBox(0f, 0.72f, -0.14f, 0.76f, 0.24f, 0.82f, 0.86f, 0.86f, darkCarbon)
        // Seat backrest + side bolsters + headrest pad
        acc.addBeveledBox(0f, 1.00f, -0.44f, 0.62f, 0.52f, 0.18f, 0.82f, 0.85f, darkCarbon, accent)
        acc.addBeveledBox(0f, 1.30f, -0.44f, 0.34f, 0.18f, 0.16f, 0.82f, 0.82f, accent, secondary)
        acc.addBox(-0.30f, 0.92f, -0.26f, 0.10f, 0.36f, 0.42f, accent)
        acc.addBox(0.30f, 0.92f, -0.26f, 0.10f, 0.36f, 0.42f, accent)
        // Dash cowl + illuminated tachometer & telemetry bar
        acc.addBeveledBox(0f, 0.90f, 0.44f, 0.66f, 0.18f, 0.34f, 0.82f, 0.78f, darkCarbon, secondary)
        acc.addBox(0f, 0.94f, 0.31f, 0.42f, 0.10f, 0.04f, 0xFF141024)
        acc.addBox(-0.10f, 0.94f, 0.29f, 0.14f, 0.06f, 0.03f, 0xFF3CE8B5, matType = CandyMaterials.MAT_EMISSIVE)
        acc.addBox(0.08f, 0.94f, 0.29f, 0.14f, 0.06f, 0.03f, 0xFFFF3D8B, matType = CandyMaterials.MAT_EMISSIVE)
        // Steering column shaft (connects chassis to animated steering wheel hub at 0, 1.04, 0.42)
        acc.addStrut(Vec3(0f, 0.76f, 0.58f), Vec3(0f, 1.04f, 0.42f), 0.045f, 6, chrome)

        // Sculpted Chrome Exhaust Pipes with flared tips
        val exhaustHex = custom.exhaustStyle.flameHex
        val pipeOffsets = if (kart.style == KartBodyStyle.CHOCOLATE_CRUISER || kart.style == KartBodyStyle.CARAMEL_THUNDER) {
            listOf(-0.48f, -0.26f, 0.26f, 0.48f)
        } else {
            listOf(-0.36f, 0.36f)
        }
        for (px in pipeOffsets) {
            acc.addHorizontalCylinderZ(px, 0.56f, -1.52f, 0.11f, 0.085f, 0.46f, 8, chrome, chrome, CandyMaterials.MAT_CHROME)
            acc.addHorizontalCylinderZ(px, 0.56f, -1.74f, 0.08f, 0.08f, 0.08f, 8, exhaustHex, exhaustHex, CandyMaterials.MAT_EMISSIVE)
        }

        // Optional Kart Decoration
        when (custom.decoration) {
            KartDecoration.NONE -> Unit
            KartDecoration.CHERRY_TOPPER -> {
                acc.addCylinder(0f, 1.24f, -1.22f, 0.04f, 0.04f, 0.45f, 6, 0xFF2ECC71)
                acc.addSphere(0f, 1.72f, -1.22f, 0.22f, 0.22f, 0.22f, 6, 8, 0xFFFF1744)
            }
            KartDecoration.CANDY_CANE_WING -> {
                acc.addBox(0f, 1.36f, -1.26f, 1.68f, 0.12f, 0.36f, 0xFFFF2448)
                acc.addBox(0f, 1.44f, -1.26f, 1.24f, 0.08f, 0.36f, 0xFFFFFFFF)
            }
            KartDecoration.LOLLIPOP_ANTENNA -> {
                acc.addCylinder(-0.56f, 0.90f, -1.16f, 0.035f, 0.035f, 0.85f, 6, chrome, matType = CandyMaterials.MAT_CHROME)
                acc.addSphere(-0.56f, 1.80f, -1.16f, 0.26f, 0.26f, 0.10f, 6, 10, 0xFFFF3D8B, 0xFFFFFFFF)
            }
            KartDecoration.CROWN_SPRINKLE -> {
                acc.addCylinder(0f, 1.38f, -0.44f, 0.18f, 0.24f, 0.20f, 8, 0xFFFFD700, 0xFFFF9800, matType = CandyMaterials.MAT_CHROME)
            }
        }

        return acc.build()
    }

    /**
     * Separate animated steering wheel centered around origin (placed at (0, 1.04, 0.42) by renderer).
     */
    private fun buildSteeringWheelMesh(kart: KartSpec, custom: KartCustomization): GpuMesh {
        val acc = MeshAccumulator()
        val paint = CatalogData.paintById(custom.paintId)
        val secondary = if (custom.paintId == "default") kart.defaultSecondaryHex else paint.secondaryHex
        val darkCarbon = 0xFF1F1629L
        val chrome = 0xFFE5EAF0L

        acc.addTorus(0f, 0f, 0f, 0.21f, 0.044f, 14, 6, darkCarbon, secondary, verticalArch = true)
        acc.addBox(0f, 0f, 0f, 0.38f, 0.052f, 0.042f, chrome, matType = CandyMaterials.MAT_CHROME)
        acc.addBox(0f, -0.09f, 0f, 0.052f, 0.18f, 0.042f, chrome, matType = CandyMaterials.MAT_CHROME)
        acc.addSphere(0f, 0f, -0.01f, 0.068f, 0.068f, 0.05f, 5, 8, kart.accentHex, matType = CandyMaterials.MAT_EMISSIVE)
        return acc.build()
    }

    /**
     * Separate animated driver character centered at hip pivot (placed at (0, 0.85, -0.14) by renderer).
     */
    private fun buildDriverMesh(racer: RacerSpec): GpuMesh {
        val acc = MeshAccumulator()
        // Torso & racing suit stripes
        acc.addBeveledBox(0f, 0.22f, 0f, 0.52f, 0.52f, 0.38f, 0.88f, 0.88f, racer.suitPrimaryHex, racer.suitSecondaryHex)
        acc.addBox(0f, 0.25f, 0f, 0.20f, 0.50f, 0.41f, racer.suitSecondaryHex)
        // Shoulders, arms reaching forward toward wheel, & gloved hands
        acc.addStrut(Vec3(-0.28f, 0.30f, 0.08f), Vec3(-0.20f, 0.20f, 0.50f), 0.075f, 6, racer.suitPrimaryHex, CandyMaterials.MAT_CANDY)
        acc.addStrut(Vec3(0.28f, 0.30f, 0.08f), Vec3(0.20f, 0.20f, 0.50f), 0.075f, 6, racer.suitPrimaryHex, CandyMaterials.MAT_CANDY)
        acc.addSphere(-0.20f, 0.20f, 0.52f, 0.10f, 0.10f, 0.10f, 5, 6, racer.suitSecondaryHex)
        acc.addSphere(0.20f, 0.20f, 0.52f, 0.10f, 0.10f, 0.10f, 5, 6, racer.suitSecondaryHex)
        // Neck & Helmet + curved reflective visor
        acc.addCylinder(0f, 0.44f, 0.04f, 0.13f, 0.13f, 0.18f, 8, racer.skinHex)
        acc.addSphere(0f, 0.72f, 0.06f, 0.34f, 0.33f, 0.36f, 8, 12, racer.helmetHex, racer.suitSecondaryHex)
        acc.addSphere(0f, 0.71f, 0.29f, 0.26f, 0.14f, 0.20f, 6, 10, racer.visorHex, racer.visorHex, matType = CandyMaterials.MAT_CHROME)
        return acc.build()
    }

    private fun buildBrakeLights(kart: KartSpec): GpuMesh {
        val acc = MeshAccumulator()
        val zBack = when (kart.style) {
            KartBodyStyle.CHOCOLATE_CRUISER, KartBodyStyle.CARAMEL_THUNDER -> -1.52f
            else -> -1.48f
        }
        // u = 0.95f signals CandyShaders.LIT_FRAGMENT_SHADER to modulate intensity via uBrakeGlow!
        acc.addBox(-0.52f, 0.64f, zBack, 0.26f, 0.12f, 0.10f, 0xFFFF2244, matType = CandyMaterials.MAT_EMISSIVE, uTag = 0.95f)
        acc.addBox(0.52f, 0.64f, zBack, 0.26f, 0.12f, 0.10f, 0xFFFF2244, matType = CandyMaterials.MAT_EMISSIVE, uTag = 0.95f)
        return acc.build()
    }

    private fun buildSculptedTireMesh(): GpuMesh {
        val acc = MeshAccumulator()
        val (tr, tg, tb) = MeshAccumulator.hexToRgb(0xFF211B26)
        val segs = 18
        val outerR = 0.46f
        val shoulderR = 0.43f
        val innerR = 0.29f
        val halfW = 0.20f
        val treadHalfW = 0.16f

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
                tr * 0.9f, tg * 0.9f, tb * 0.9f, 1f, 0.2f, v0, 0.35f, v1, CandyMaterials.MAT_TIRE
            )
            acc.addQuad(
                treadHalfW, c0 * outerR, s0 * outerR,
                treadHalfW, c1 * outerR, s1 * outerR,
                halfW, c1 * shoulderR, s1 * shoulderR,
                halfW, c0 * shoulderR, s0 * shoulderR,
                tr * 0.9f, tg * 0.9f, tb * 0.9f, 1f, 0.65f, v0, 0.8f, v1, CandyMaterials.MAT_TIRE
            )
            // Sidewalls down to alloy rim bead
            acc.addQuad(
                halfW, c0 * innerR, s0 * innerR,
                halfW, c1 * innerR, s1 * innerR,
                halfW, c1 * shoulderR, s1 * shoulderR,
                halfW, c0 * shoulderR, s0 * shoulderR,
                tr * 1.1f, tg * 1.1f, tb * 1.1f, 1f, 0.82f, v0, 0.96f, v1, CandyMaterials.MAT_TIRE
            )
            acc.addQuad(
                -halfW, c0 * shoulderR, s0 * shoulderR,
                -halfW, c1 * shoulderR, s1 * shoulderR,
                -halfW, c1 * innerR, s1 * innerR,
                -halfW, c0 * innerR, s0 * innerR,
                tr * 1.1f, tg * 1.1f, tb * 1.1f, 1f, 0.04f, v0, 0.18f, v1, CandyMaterials.MAT_TIRE
            )
        }
        return acc.build()
    }

    private fun buildAlloyRimMesh(design: WheelDesign): GpuMesh {
        val acc = MeshAccumulator()
        val (r1, g1, b1) = MeshAccumulator.hexToRgb(design.primaryHex)
        val (r2, g2, b2) = MeshAccumulator.hexToRgb(design.rimHex)
        val rimR = 0.295f
        val halfW = 0.205f
        val segs = 12
        // Outer metallic rim dish & hub cap
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
            val isSpoke = (i % 2 == 0)
            val (rr, rg, rb) = if (isSpoke) Triple(r1, g1, b1) else Triple(r2, g2, b2)
            val mat = if (isSpoke) CandyMaterials.MAT_CHROME else CandyMaterials.MAT_CANDY
            val y0 = cos(a0) * rimR; val z0 = sin(a0) * rimR
            val y1 = cos(a1) * rimR; val z1 = sin(a1) * rimR
            acc.addTri(halfW, 0f, 0f, halfW, y0, z0, halfW, y1, z1, rr, rg, rb, 1f, 0f, 0f, 1f, 0f, 0.5f, 1f, mat)
            acc.addTri(-halfW, 0f, 0f, -halfW, y1, z1, -halfW, y0, z0, rr, rg, rb, 1f, 0f, 0f, 1f, 0f, 0.5f, 1f, mat)
        }
        // Center lug spinner nut
        acc.addSphere(halfW + 0.02f, 0f, 0f, 0.05f, 0.09f, 0.09f, 4, 6, 0xFFFFFFFF, matType = CandyMaterials.MAT_CHROME)
        acc.addSphere(-halfW - 0.02f, 0f, 0f, 0.05f, 0.09f, 0.09f, 4, 6, 0xFFFFFFFF, matType = CandyMaterials.MAT_CHROME)
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
            // Upper & lower wishbone A-arms + vertical coilover shock strut + steering tie rod + brake caliper hub
            acc.addStrut(Vec3(innerX, 0.44f, w.z + 0.14f), Vec3(w.x - side * 0.12f, w.y, w.z), 0.042f, 6, chrome)
            acc.addStrut(Vec3(innerX, 0.44f, w.z - 0.14f), Vec3(w.x - side * 0.12f, w.y, w.z), 0.042f, 6, chrome)
            acc.addStrut(Vec3(innerX + side * 0.12f, 0.72f, w.z), Vec3(w.x - side * 0.14f, w.y + 0.04f, w.z), 0.055f, 6, springHex, CandyMaterials.MAT_CANDY)
            if (w.z > 0f) {
                acc.addStrut(Vec3(innerX, 0.50f, w.z - 0.20f), Vec3(w.x - side * 0.16f, w.y, w.z - 0.12f), 0.026f, 5, chrome)
            }
            // Brake disc & caliper housing on inner hub
            acc.addSphere(w.x - side * 0.19f, w.y, w.z, 0.04f, 0.19f, 0.19f, 4, 8, chrome, matType = CandyMaterials.MAT_CHROME)
            acc.addBox(w.x - side * 0.18f, w.y + 0.10f, w.z - 0.10f, 0.08f, 0.12f, 0.14f, springHex)
        }
        return acc.build()
    }
}
