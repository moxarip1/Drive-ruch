package com.example.sweetrush.model

enum class GameScreen {
    MAIN_MENU,
    ONLINE_LOBBY,
    QUICK_RACE_SETUP,
    GRAND_PRIX_SETUP,
    TIME_TRIAL_SETUP,
    PRACTICE_SETUP,
    CHALLENGES,
    CHARACTER_SELECT,
    GARAGE,
    TRACK_COLLECTION,
    WORLD_MAP,
    SETTINGS,
    RACE,
    RESULTS
}

enum class GameMode {
    FREE_ROAM,
    QUICK_RACE,
    GRAND_PRIX,
    TIME_TRIAL,
    PRACTICE,
    CHALLENGE
}

enum class WorldRegion(
    val displayName: String,
    val subtitle: String,
    val centerX: Float,
    val centerZ: Float,
    val skyTopHex: Long,
    val skyBottomHex: Long,
    val fogHex: Long,
    val accentHex: Long
) {
    CANDY_CITY(
        "Candy City",
        "Peppermint Plazas & Sugar Avenues",
        0f, 0f,
        0xFF4CC9FF, 0xFFFFC8E6, 0xFFFFD8EC, 0xFFFF3D8B
    ),
    CHOCOLATE_MOUNTAINS(
        "Chocolate Mountains",
        "Caramel Waterfalls & Fudge Switchbacks",
        270f, -240f,
        0xFF3D1338, 0xFFFF7E47, 0xFFB8544A, 0xFFFF9800
    ),
    GUMDROP_FOREST(
        "Gumdrop Forest",
        "Lollipop Groves & Secret Shortcuts",
        -265f, -225f,
        0xFF6A4C93, 0xFFA0F0ED, 0xFFCDEEE9, 0xFF3CE8B5
    ),
    CUPCAKE_VALLEY(
        "Cupcake Valley",
        "Frosting Hills & Wafer Bridges",
        -255f, 235f,
        0xFF7B4DFF, 0xFFFFD1E8, 0xFFFAD3EA, 0xFFD980FA
    ),
    PEPPERMINT_COAST(
        "Peppermint Coast",
        "Strawberry Cream Shores & Boardwalk",
        260f, 240f,
        0xFF29B6F6, 0xFFFFE5B8, 0xFFFFEAD2, 0xFF5CE1E6
    )
}

enum class Difficulty(val label: String, val aiSpeedMultiplier: Float, val aiSkill: Float, val boostFreq: Float) {
    EASY("Easy", 0.84f, 0.55f, 0.25f),
    NORMAL("Normal", 0.94f, 0.75f, 0.45f),
    HARD("Hard", 1.01f, 0.90f, 0.70f),
    EXPERT("Expert", 1.07f, 0.98f, 0.90f)
}

enum class KartBodyStyle {
    STRAWBERRY_ROCKET,
    CHOCOLATE_CRUISER,
    PEPPERMINT_RACER,
    JELLYBEAN_MACHINE,
    CUPCAKE_KART,
    DONUT_DRIFTER,
    MARSHMALLOW_SPEEDSTER,
    CARAMEL_THUNDER
}

enum class TrackId {
    PEPPERMINT_PLAZA,
    CUPCAKE_COAST,
    CHOCOLATE_VOLCANO,
    GUMDROP_GARDEN
}

enum class PowerUpType(
    val displayName: String,
    val shortDesc: String,
    val colorArgb: Long
) {
    SUGAR_BOOST("Sugar Boost", "Sweet burst of top speed!", 0xFFFF3D8B),
    GUMMY_SHIELD("Gummy Shield", "Blocks impacts & traps!", 0xFF3CE8B5),
    PEPPERMINT_BLAST("Peppermint Blast", "Drops a spinning candy obstacle!", 0xFFFF3355),
    CARAMEL_TRAP("Caramel Trap", "Sticky caramel slows rivals!", 0xFFFF9800),
    JELLY_DASH("Jelly Dash", "Instant hop + acceleration!", 0xFF9C4DFF),
    RAINBOW_STAR("Rainbow Star", "Invincible mega-speed!", 0xFFFFEB3B),
    CHOCOLATE_SMOKE("Chocolate Smoke", "Blinds nearby opponents!", 0xFF6D4C41)
}

enum class WheelDesign(val id: String, val displayName: String, val cost: Int, val primaryHex: Long, val rimHex: Long) {
    PEPPERMINT_SWIRL("peppermint", "Peppermint Swirl", 0, 0xFFFFFFFF, 0xFFFF2E55),
    CHOCO_COOKIE("cookie", "Choco Cookie", 150, 0xFF4E2A1E, 0xFFD7A86E),
    DONUT_RING("donut", "Glazed Donut", 250, 0xFFF4B678, 0xFFFF6EB4),
    GUMMY_GLOW("gummy", "Neon Gummy", 350, 0xFF3CE8B5, 0xFF00BFA5),
    GOLD_CARAMEL("caramel", "Caramel Gold", 500, 0xFFFFB800, 0xFFFFE082)
}

enum class ExhaustStyle(val id: String, val displayName: String, val cost: Int, val flameHex: Long) {
    SUGAR_SPARK("sugar", "Sugar Spark", 0, 0xFFFF7EB6),
    MINT_FLAME("mint", "Mint Frost Flame", 180, 0xFF3CE8B5),
    CARAMEL_JET("caramel", "Caramel Jet", 300, 0xFFFFB800),
    RAINBOW_STREAM("rainbow", "Rainbow Stream", 480, 0xFF9C4DFF)
}

enum class KartDecoration(val id: String, val displayName: String, val cost: Int) {
    NONE("none", "Clean Aero", 0),
    CHERRY_TOPPER("cherry", "Cherry Fin", 120),
    CANDY_CANE_WING("candycane", "Candy Cane Wing", 240),
    LOLLIPOP_ANTENNA("lollipop", "Lollipop Spinner", 320),
    CROWN_SPRINKLE("crown", "Sugar Crown", 550)
}

enum class GraphicsQuality(val label: String, val decoDensity: Float, val particleMultiplier: Float) {
    LOW("Low", 0.45f, 0.5f),
    MEDIUM("Medium", 0.75f, 0.8f),
    HIGH("High", 1.0f, 1.0f)
}

enum class ControlScheme(val label: String) {
    TOUCH_BUTTONS("Touch Buttons"),
    VIRTUAL_WHEEL("Virtual Wheel"),
    TILT_STEER("Tilt Steering")
}

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    AR("ar", "العربية"),
    ES("es", "Español"),
    FR("fr", "Français")
}

enum class CameraPreset(
    val label: String,
    val labelAr: String,
    val followDist: Float,
    val followHeight: Float,
    val lookAhead: Float,
    val baseFov: Float
) {
    DYNAMIC_CHASE("Dynamic", "ديناميكي", 6.9f, 3.15f, 5.4f, 58f),
    CLOSE_ARCADE("Close", "قريب", 4.9f, 2.25f, 4.6f, 62f),
    WIDE_CINEMATIC("Wide", "واسع", 9.6f, 4.45f, 6.8f, 65f),
    HOOD_CAM("Hood", "المقدمة", 0.35f, 1.38f, 11.5f, 68f),
    FREE_ORBIT("Orbit 360°", "حر 360°", 7.4f, 3.35f, 1.5f, 58f)
}

enum class SquadTeam(val index: Int, val teamName: String, val colorHex: Long) {
    PINK(0, "Pink Panthers", 0xFFFF3D8B),
    MINT(1, "Mint Comets", 0xFF3CE8B5),
    GOLD(2, "Gold Chargers", 0xFFFFB800),
    SKY(3, "Sky Rockets", 0xFF5CE1E6);

    companion object {
        fun fromIndex(idx: Int): SquadTeam = entries.firstOrNull { it.index == idx } ?: PINK
    }
}

data class KartStats(
    val speed: Float,
    val acceleration: Float,
    val handling: Float,
    val boost: Float,
    val stability: Float
)

data class KartPhysicsTuning(
    val massKg: Float,
    val suspensionStiffness: Float,
    val suspensionDamping: Float,
    val lateralGrip: Float,
    val driftGrip: Float,
    val steerResponse: Float
)

data class KartSpec(
    val id: String,
    val name: String,
    val style: KartBodyStyle,
    val racingNumber: Int,
    val tagline: String,
    val defaultPrimaryHex: Long,
    val defaultSecondaryHex: Long,
    val accentHex: Long,
    val baseStats: KartStats,
    val physicsTuning: KartPhysicsTuning,
    val unlockCost: Int,
    val unlockLevel: Int
)

data class RacerSpec(
    val id: String,
    val name: String,
    val title: String,
    val personality: String,
    val suitPrimaryHex: Long,
    val suitSecondaryHex: Long,
    val helmetHex: Long,
    val visorHex: Long,
    val skinHex: Long,
    val favoriteKartId: String,
    val signaturePowerUp: PowerUpType,
    val victoryLine: String,
    val defeatLine: String,
    val unlockCost: Int,
    val unlockLevel: Int
)

data class TrackSpec(
    val id: TrackId,
    val name: String,
    val subtitle: String,
    val description: String,
    val laps: Int,
    val skyTopHex: Long,
    val skyBottomHex: Long,
    val fogHex: Long,
    val groundHex: Long,
    val roadPrimaryHex: Long,
    val roadStripeHex: Long,
    val curbPrimaryHex: Long,
    val curbSecondaryHex: Long,
    val featureHighlights: List<String>,
    val difficultyStars: Int
)

data class PaintJob(
    val id: String,
    val name: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val cost: Int
)

data class KartUpgradeLevels(
    val speed: Int = 0,
    val acceleration: Int = 0,
    val handling: Int = 0,
    val boost: Int = 0,
    val stability: Int = 0
) {
    fun totalLevel(): Int = speed + acceleration + handling + boost + stability
}

data class KartCustomization(
    val paintId: String = "default",
    val wheelDesign: WheelDesign = WheelDesign.PEPPERMINT_SWIRL,
    val exhaustStyle: ExhaustStyle = ExhaustStyle.SUGAR_SPARK,
    val decoration: KartDecoration = KartDecoration.NONE,
    val upgrades: KartUpgradeLevels = KartUpgradeLevels()
)

data class ChallengeSpec(
    val id: String,
    val title: String,
    val description: String,
    val trackId: TrackId,
    val difficulty: Difficulty,
    val targetType: ChallengeTargetType,
    val targetValue: Int,
    val rewardCredits: Int,
    val rewardXp: Int
)

enum class ChallengeTargetType {
    WIN_RACE,
    DRIFT_BOOSTS,
    MAX_SPEED_DURATION,
    AVOID_HAZARDS_WIN
}

data class GameSettings(
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val targetFps: Int = 60,
    val musicVolume: Float = 0.70f,
    val sfxVolume: Float = 0.85f,
    val controlScheme: ControlScheme = ControlScheme.TOUCH_BUTTONS,
    val steeringSensitivity: Float = 1.0f,
    val cameraSensitivity: Float = 1.0f,
    val cameraPreset: CameraPreset = CameraPreset.DYNAMIC_CHASE,
    val cameraDistanceScale: Float = 1.0f,
    val cameraHeightOffset: Float = 0.0f,
    val cameraFovOffset: Float = 0.0f,
    val tiltSteeringEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.EN,
    val showDebugTelemetry: Boolean = false
)

data class PlayerProfile(
    val xp: Int = 0,
    val level: Int = 1,
    val credits: Int = 350,
    val selectedKartId: String = "strawberry_rocket",
    val selectedRacerId: String = "berry_blitz",
    val unlockedKarts: Set<String> = setOf("strawberry_rocket", "chocolate_cruiser", "peppermint_racer"),
    val unlockedRacers: Set<String> = setOf("berry_blitz", "coco_dash", "minty_swirl"),
    val unlockedPaints: Set<String> = setOf("default", "strawberry_glaze", "mint_frost"),
    val unlockedWheels: Set<WheelDesign> = setOf(WheelDesign.PEPPERMINT_SWIRL),
    val unlockedExhausts: Set<ExhaustStyle> = setOf(ExhaustStyle.SUGAR_SPARK),
    val unlockedDecorations: Set<KartDecoration> = setOf(KartDecoration.NONE),
    val kartCustomizations: Map<String, KartCustomization> = emptyMap(),
    val bestLapMsByTrack: Map<TrackId, Long> = emptyMap(),
    val completedChallenges: Set<String> = emptySet(),
    val collectedWorldCoins: Set<String> = emptySet(),
    val collectedWorldStars: Set<String> = emptySet(),
    val discoveredRegions: Set<WorldRegion> = setOf(WorldRegion.CANDY_CITY),
    val racesCompleted: Int = 0,
    val racesWon: Int = 0,
    val grandPrixGoldTrophies: Int = 0,
    val grandPrixBestPoints: Int = 0,
    val settings: GameSettings = GameSettings()
)

data class VehicleDebugTelemetry(
    val speedKmh: Float = 0f,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val vz: Float = 0f,
    val wheelContacts: List<Boolean> = listOf(true, true, true, true), // FL, FR, RL, RR
    val wheelCompression: List<Float> = listOf(0.5f, 0.5f, 0.5f, 0.5f),
    val lateralGrip: Float = 1f,
    val forwardGrip: Float = 1f,
    val slipAngleDeg: Float = 0f,
    val driftState: String = "GRIP",
    val surfaceName: String = "Road"
)

data class RacerResultEntry(
    val racerId: String,
    val racerName: String,
    val kartName: String,
    val isPlayer: Boolean,
    val position: Int,
    val totalTimeMs: Long,
    val bestLapMs: Long,
    val grandPrixPointsAwarded: Int,
    val teamIndex: Int = 0
)

data class RaceSummary(
    val mode: GameMode,
    val trackId: TrackId,
    val difficulty: Difficulty,
    val playerPosition: Int,
    val playerTotalTimeMs: Long,
    val playerBestLapMs: Long,
    val isNewBestLap: Boolean,
    val xpEarned: Int,
    val creditsEarned: Int,
    val challengeCompletedId: String?,
    val standings: List<RacerResultEntry>,
    val grandPrixCompleted: Boolean = false,
    val grandPrixTotalStandings: List<Pair<String, Int>> = emptyList(),
    val isOnlineMatch: Boolean = false,
    val teamSize: Int = 1,
    val teamStandings: List<Pair<Int, Int>> = emptyList()
)

data class GrandPrixSession(
    val difficulty: Difficulty,
    val trackOrder: List<TrackId> = TrackId.entries,
    val currentRaceIndex: Int = 0,
    val cumulativePointsByRacerId: Map<String, Int> = emptyMap()
)
