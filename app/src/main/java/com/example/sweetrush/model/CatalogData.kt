package com.example.sweetrush.model

object CatalogData {

    val karts: List<KartSpec> = listOf(
        KartSpec(
            id = "strawberry_rocket",
            name = "Strawberry Rocket",
            style = KartBodyStyle.STRAWBERRY_ROCKET,
            racingNumber = 7,
            tagline = "Sculpted berry torpedo with whipped-cream fins & twin rocket pipes",
            defaultPrimaryHex = 0xFFFF3B7F,
            defaultSecondaryHex = 0xFFFFF5F8,
            accentHex = 0xFF3CE8B5,
            baseStats = KartStats(0.76f, 0.80f, 0.82f, 0.78f, 0.74f),
            physicsTuning = KartPhysicsTuning(195f, 34f, 6.4f, 8.8f, 3.6f, 1.05f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "chocolate_cruiser",
            name = "Chocolate Thunder",
            style = KartBodyStyle.CHOCOLATE_CRUISER,
            racingNumber = 12,
            tagline = "Muscular dark-fudge wedge with wafer grille & quad exhausts",
            defaultPrimaryHex = 0xFF5A2E1B,
            defaultSecondaryHex = 0xFFD99B52,
            accentHex = 0xFFFFC107,
            baseStats = KartStats(0.84f, 0.68f, 0.68f, 0.74f, 0.92f),
            physicsTuning = KartPhysicsTuning(245f, 40f, 7.2f, 8.2f, 3.9f, 0.92f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "peppermint_racer",
            name = "Peppermint Racer",
            style = KartBodyStyle.PEPPERMINT_RACER,
            racingNumber = 1,
            tagline = "Open-wheel candy formula chassis with swirled splitter & F1 wing",
            defaultPrimaryHex = 0xFFFDFDFD,
            defaultSecondaryHex = 0xFFFF2448,
            accentHex = 0xFF4AF2C5,
            baseStats = KartStats(0.80f, 0.76f, 0.92f, 0.76f, 0.70f),
            physicsTuning = KartPhysicsTuning(180f, 36f, 6.8f, 9.6f, 3.3f, 1.18f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "jellybean_machine",
            name = "Rainbow Jelly Kart",
            style = KartBodyStyle.JELLYBEAN_MACHINE,
            racingNumber = 24,
            tagline = "Translucent neon-jelly canopy racer with bouncy grip suspension",
            defaultPrimaryHex = 0xFF33D9B2,
            defaultSecondaryHex = 0xFFFFFA65,
            accentHex = 0xFFFF5252,
            baseStats = KartStats(0.74f, 0.88f, 0.84f, 0.82f, 0.76f),
            physicsTuning = KartPhysicsTuning(188f, 30f, 5.8f, 9.1f, 3.4f, 1.10f),
            unlockCost = 220,
            unlockLevel = 2
        ),
        KartSpec(
            id = "cupcake_kart",
            name = "Cupcake Kart",
            style = KartBodyStyle.CUPCAKE_KART,
            racingNumber = 18,
            tagline = "Fluted wrapper chassis piped with whipped frosting & cherry spoiler",
            defaultPrimaryHex = 0xFFB86BFF,
            defaultSecondaryHex = 0xFFFFB8DE,
            accentHex = 0xFFFF2E55,
            baseStats = KartStats(0.74f, 0.82f, 0.78f, 0.86f, 0.82f),
            physicsTuning = KartPhysicsTuning(210f, 33f, 6.2f, 8.6f, 3.5f, 1.00f),
            unlockCost = 340,
            unlockLevel = 3
        ),
        KartSpec(
            id = "donut_drifter",
            name = "Donut Drifter",
            style = KartBodyStyle.DONUT_DRIFTER,
            racingNumber = 33,
            tagline = "Glazed ring body tuned for long sprinkle-spark power slides",
            defaultPrimaryHex = 0xFFF6A95E,
            defaultSecondaryHex = 0xFFFF64B4,
            accentHex = 0xFF40E0FF,
            baseStats = KartStats(0.78f, 0.78f, 0.88f, 0.88f, 0.74f),
            physicsTuning = KartPhysicsTuning(192f, 34f, 6.2f, 8.3f, 2.8f, 1.12f),
            unlockCost = 450,
            unlockLevel = 4
        ),
        KartSpec(
            id = "marshmallow_speedster",
            name = "Marshmallow Speedster",
            style = KartBodyStyle.MARSHMALLOW_SPEEDSTER,
            racingNumber = 9,
            tagline = "Twin-pontoon plush catamaran with soft-landing suspension",
            defaultPrimaryHex = 0xFFF8F4FF,
            defaultSecondaryHex = 0xFF6EC6FF,
            accentHex = 0xFFFF70A6,
            baseStats = KartStats(0.82f, 0.86f, 0.76f, 0.82f, 0.86f),
            physicsTuning = KartPhysicsTuning(205f, 29f, 5.5f, 8.7f, 3.5f, 1.02f),
            unlockCost = 580,
            unlockLevel = 5
        ),
        KartSpec(
            id = "caramel_thunder",
            name = "Caramel Speedster",
            style = KartBodyStyle.CARAMEL_THUNDER,
            racingNumber = 99,
            tagline = "Molten-toffee hyper-wedge with split-deck wing & triple manifolds",
            defaultPrimaryHex = 0xFFFF9F1A,
            defaultSecondaryHex = 0xFF3E1F0D,
            accentHex = 0xFFFFEE58,
            baseStats = KartStats(0.94f, 0.80f, 0.76f, 0.90f, 0.86f),
            physicsTuning = KartPhysicsTuning(220f, 38f, 7.0f, 9.0f, 3.4f, 1.06f),
            unlockCost = 750,
            unlockLevel = 6
        )
    )

    val racers: List<RacerSpec> = listOf(
        RacerSpec(
            id = "berry_blitz",
            name = "Berry Blitz",
            title = "Strawberry Prodigy",
            personality = "Fearless, bouncy, and always hungry for the apex.",
            suitPrimaryHex = 0xFFFF3D8B,
            suitSecondaryHex = 0xFFFFF0F5,
            helmetHex = 0xFFFF246B,
            visorHex = 0xFF28113B,
            skinHex = 0xFFFFD1B3,
            favoriteKartId = "strawberry_rocket",
            signaturePowerUp = PowerUpType.SUGAR_BOOST,
            victoryLine = "Berry sweet victory! Nobody catches the sugar rush!",
            defeatLine = "Oof, spun out on frosting! Next race is mine!",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "coco_dash",
            name = "Coco Dash",
            title = "Fudge Strategist",
            personality = "Smooth under pressure with razor-sharp corner lines.",
            suitPrimaryHex = 0xFF5D3422,
            suitSecondaryHex = 0xFFFFC107,
            helmetHex = 0xFF3B1E12,
            visorHex = 0xFFFFD54F,
            skinHex = 0xFFC6865C,
            favoriteKartId = "chocolate_cruiser",
            signaturePowerUp = PowerUpType.CHOCOLATE_SMOKE,
            victoryLine = "Rich, smooth, and unstoppable—just like triple fudge.",
            defeatLine = "A bitter finish... time to retune the cocoa injectors.",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "minty_swirl",
            name = "Minty Swirl",
            title = "Peppermint Ace",
            personality = "Cool-headed drift specialist with crisp precision.",
            suitPrimaryHex = 0xFF2ED8B6,
            suitSecondaryHex = 0xFFFF355E,
            helmetHex = 0xFFFFFFFF,
            visorHex = 0xFF144E4A,
            skinHex = 0xFFFCE0D2,
            favoriteKartId = "peppermint_racer",
            signaturePowerUp = PowerUpType.PEPPERMINT_BLAST,
            victoryLine = "Mint condition from start to checkered flag!",
            defeatLine = "Too hot on the throttle—I'll chill out next lap.",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "fizzy_pop",
            name = "Fizzy Pop",
            title = "Soda Daredevil",
            personality = "Hyperactive nitro junkie fueled by citrus fizz.",
            suitPrimaryHex = 0xFF00C2FF,
            suitSecondaryHex = 0xFFFFE600,
            helmetHex = 0xFF0091EA,
            visorHex = 0xFF1A237E,
            skinHex = 0xFFE5B28A,
            favoriteKartId = "jellybean_machine",
            signaturePowerUp = PowerUpType.JELLY_DASH,
            victoryLine = "Fizz, pop, ZOOM! Did you see that mega boost?!",
            defeatLine = "My soda went flat right before the finish line!",
            unlockCost = 200,
            unlockLevel = 2
        ),
        RacerSpec(
            id = "frosting_faye",
            name = "Frosting Faye",
            title = "Cupcake Champion",
            personality = "Royal bakery stylist who wins with flair.",
            suitPrimaryHex = 0xFFA55EEA,
            suitSecondaryHex = 0xFFFFD3E8,
            helmetHex = 0xFFC07BFF,
            visorHex = 0xFF2D1048,
            skinHex = 0xFFFFDAB9,
            favoriteKartId = "cupcake_kart",
            signaturePowerUp = PowerUpType.RAINBOW_STAR,
            victoryLine = "Whipped the competition and topped it with a cherry!",
            defeatLine = "My frosting got smudged—rematch right now!",
            unlockCost = 320,
            unlockLevel = 3
        ),
        RacerSpec(
            id = "glazed_gus",
            name = "Glazed Gus",
            title = "Drift Virtuoso",
            personality = "Laid-back donut baker who lives sideways in corners.",
            suitPrimaryHex = 0xFFFF8C42,
            suitSecondaryHex = 0xFFFF6EB4,
            helmetHex = 0xFFF5A962,
            visorHex = 0xFF3E2723,
            skinHex = 0xFFD99B72,
            favoriteKartId = "donut_drifter",
            signaturePowerUp = PowerUpType.GUMMY_SHIELD,
            victoryLine = "Pure glazed perfection—zero holes in that lap!",
            defeatLine = "Rolled a little wide on turn four, dude.",
            unlockCost = 420,
            unlockLevel = 4
        ),
        RacerSpec(
            id = "mallow_max",
            name = "Mallow Max",
            title = "Sky Stunt Rookie",
            personality = "Loves giant jumps and soft landings.",
            suitPrimaryHex = 0xFFF4F7FF,
            suitSecondaryHex = 0xFF4DA6FF,
            helmetHex = 0xFFFFFFFF,
            visorHex = 0xFF0D47A1,
            skinHex = 0xFF8D5524,
            favoriteKartId = "marshmallow_speedster",
            signaturePowerUp = PowerUpType.GUMMY_SHIELD,
            victoryLine = "Fluffy, bouncy, and first across the line!",
            defeatLine = "Got toasted out there—still had a blast!",
            unlockCost = 520,
            unlockLevel = 5
        ),
        RacerSpec(
            id = "toffee_rex",
            name = "Toffee Rex",
            title = "Caramel Legend",
            personality = "Undefeated volcano circuit veteran with golden grit.",
            suitPrimaryHex = 0xFFFF9800,
            suitSecondaryHex = 0xFF2C1608,
            helmetHex = 0xFFFFB300,
            visorHex = 0xFF1B0D04,
            skinHex = 0xFFB8734B,
            favoriteKartId = "caramel_thunder",
            signaturePowerUp = PowerUpType.CARAMEL_TRAP,
            victoryLine = "Bow to the king of the Caramel Caldera!",
            defeatLine = "Impressive pace, rookie. You earned my respect.",
            unlockCost = 680,
            unlockLevel = 6
        )
    )

    val tracks: List<TrackSpec> = listOf(
        TrackSpec(
            id = TrackId.PEPPERMINT_PLAZA,
            name = "Peppermint Plaza",
            subtitle = "Candy City Grand Prix",
            description = "Zoom past gingerbread cottages, cupcake spires, candy cane arches, and swirled peppermint boulevards.",
            laps = 3,
            skyTopHex = 0xFF4CC9FF,
            skyBottomHex = 0xFFFFC8E6,
            fogHex = 0xFFFFD8EC,
            groundHex = 0xFF66E2B3,
            roadPrimaryHex = 0xFF3E294D,
            roadStripeHex = 0xFFFF4D8D,
            curbPrimaryHex = 0xFFFF2448,
            curbSecondaryHex = 0xFFFFFFFF,
            featureHighlights = listOf("Candy Cane Arches", "Peppermint Plaza", "Cupcake Towers"),
            difficultyStars = 1
        ),
        TrackSpec(
            id = TrackId.CUPCAKE_COAST,
            name = "Cupcake Coast",
            subtitle = "Peppermint Coast & Valley",
            description = "Drift along marshmallow beaches, cross glazed donut bridges, dash through wafer tunnels, and dodge coastal palms.",
            laps = 3,
            skyTopHex = 0xFF29B6F6,
            skyBottomHex = 0xFFFFE0B2,
            fogHex = 0xFFFFE9CC,
            groundHex = 0xFFFF94BA,
            roadPrimaryHex = 0xFFFFF3E0,
            roadStripeHex = 0xFFFF6EA8,
            curbPrimaryHex = 0xFF3CE8B5,
            curbSecondaryHex = 0xFFFFF8E1,
            featureHighlights = listOf("Wafer Tunnels", "Donut Bridges", "Marshmallow Shores"),
            difficultyStars = 2
        ),
        TrackSpec(
            id = TrackId.CHOCOLATE_VOLCANO,
            name = "Chocolate Volcano",
            subtitle = "Chocolate Mountains Peak",
            description = "Conquer steep cookie cliffs beside flowing chocolate waterfalls, glowing caramel rivers, and sizzling toffee hazards.",
            laps = 3,
            skyTopHex = 0xFF3D1338,
            skyBottomHex = 0xFFFF6D3B,
            fogHex = 0xFF9E3A3E,
            groundHex = 0xFF3A1C12,
            roadPrimaryHex = 0xFF4E2A1E,
            roadStripeHex = 0xFFFF9E1B,
            curbPrimaryHex = 0xFFFFB300,
            curbSecondaryHex = 0xFF3E1F14,
            featureHighlights = listOf("Chocolate Waterfalls", "Caramel Lava", "Cookie Cliffs"),
            difficultyStars = 3
        ),
        TrackSpec(
            id = TrackId.GUMDROP_GARDEN,
            name = "Gumdrop Garden",
            subtitle = "Gumdrop Forest Sanctuary",
            description = "Weave through giant lollipop woods, marshmallow tunnels, gummy bear monuments, and glowing sugar crystal meadows.",
            laps = 3,
            skyTopHex = 0xFF6A4C93,
            skyBottomHex = 0xFFA0F0ED,
            fogHex = 0xFFD2F5F2,
            groundHex = 0xFF7BE495,
            roadPrimaryHex = 0xFF5D3A7A,
            roadStripeHex = 0xFF4FFFB0,
            curbPrimaryHex = 0xFFFFEB3B,
            curbSecondaryHex = 0xFFFF4081,
            featureHighlights = listOf("Lollipop Forests", "Gummy Bear Statues", "Marshmallow Tunnels"),
            difficultyStars = 2
        )
    )

    val paintJobs: List<PaintJob> = listOf(
        PaintJob("default", "Factory Candy", 0xFFFF3D8B, 0xFFFFFFFF, 0),
        PaintJob("strawberry_glaze", "Strawberry Glaze", 0xFFFF2E63, 0xFFFFE3EC, 0),
        PaintJob("mint_frost", "Mint Frosting", 0xFF2CE6B0, 0xFFFFFFFF, 0),
        PaintJob("midnight_cocoa", "Midnight Cocoa", 0xFF3C1E14, 0xFFFFC107, 150),
        PaintJob("bubblegum_pop", "Bubblegum Pop", 0xFFFF6EC7, 0xFF40E0FF, 220),
        PaintJob("sour_lemon", "Sour Lemon Drop", 0xFFFFDF00, 0xFF28C76F, 280),
        PaintJob("galaxy_grape", "Galaxy Grape", 0xFF7B2CBF, 0xFFFF9E00, 380),
        PaintJob("royal_caramel", "Royal Caramel", 0xFFFF9500, 0xFFFFF3B0, 480)
    )

    val challenges: List<ChallengeSpec> = listOf(
        ChallengeSpec(
            id = "ch_plaza_sprint",
            title = "Plaza Sweet Champion",
            description = "Win a race at Peppermint Plaza on Normal difficulty or higher.",
            trackId = TrackId.PEPPERMINT_PLAZA,
            difficulty = Difficulty.NORMAL,
            targetType = ChallengeTargetType.WIN_RACE,
            targetValue = 1,
            rewardCredits = 250,
            rewardXp = 180
        ),
        ChallengeSpec(
            id = "ch_coast_drift",
            title = "Donut Drift Master",
            description = "Land 5 charged Drift Boosts in a single race at Cupcake Coast.",
            trackId = TrackId.CUPCAKE_COAST,
            difficulty = Difficulty.NORMAL,
            targetType = ChallengeTargetType.DRIFT_BOOSTS,
            targetValue = 5,
            rewardCredits = 320,
            rewardXp = 220
        ),
        ChallengeSpec(
            id = "ch_garden_clean",
            title = "Gummy Guardian",
            description = "Win at Gumdrop Garden on Hard without hitting any hazards.",
            trackId = TrackId.GUMDROP_GARDEN,
            difficulty = Difficulty.HARD,
            targetType = ChallengeTargetType.AVOID_HAZARDS_WIN,
            targetValue = 1,
            rewardCredits = 420,
            rewardXp = 300
        ),
        ChallengeSpec(
            id = "ch_volcano_king",
            title = "Volcano Legend",
            description = "Defeat Toffee Rex and win Chocolate Volcano on Expert difficulty!",
            trackId = TrackId.CHOCOLATE_VOLCANO,
            difficulty = Difficulty.EXPERT,
            targetType = ChallengeTargetType.WIN_RACE,
            targetValue = 1,
            rewardCredits = 650,
            rewardXp = 500
        )
    )

    fun kartById(id: String): KartSpec = karts.find { it.id == id } ?: karts.first()
    fun racerById(id: String): RacerSpec = racers.find { it.id == id } ?: racers.first()
    fun trackById(id: TrackId): TrackSpec = tracks.find { it.id == id } ?: tracks.first()
    fun paintById(id: String): PaintJob = paintJobs.find { it.id == id } ?: paintJobs.first()

    fun effectiveStats(kart: KartSpec, custom: KartCustomization): KartStats {
        val up = custom.upgrades
        return KartStats(
            speed = (kart.baseStats.speed + up.speed * 0.04f).coerceAtMost(1.0f),
            acceleration = (kart.baseStats.acceleration + up.acceleration * 0.04f).coerceAtMost(1.0f),
            handling = (kart.baseStats.handling + up.handling * 0.04f).coerceAtMost(1.0f),
            boost = (kart.baseStats.boost + up.boost * 0.04f).coerceAtMost(1.0f),
            stability = (kart.baseStats.stability + up.stability * 0.04f).coerceAtMost(1.0f)
        )
    }

    fun xpForLevel(level: Int): Int = 150 + (level - 1) * 120

    fun localized(language: AppLanguage, key: String): String {
        return when (language) {
            AppLanguage.EN -> when (key) {
                "free_roam" -> "FREE ROAM"
                "play" -> "QUICK RACE"
                "online" -> "ONLINE SQUADS"
                "gathering_hub" -> "GATHERING HUB"
                "grand_prix" -> "GRAND PRIX"
                "time_trial" -> "TIME TRIAL"
                "practice" -> "PRACTICE"
                "characters" -> "CHARACTERS"
                "garage" -> "GARAGE"
                "world_map" -> "WORLD MAP"
                "tracks" -> "TRACKS"
                "settings" -> "SETTINGS"
                "challenges" -> "CHALLENGES"
                else -> key
            }
            AppLanguage.AR -> when (key) {
                "free_roam" -> "جولة حرة"
                "play" -> "سباق سريع"
                "online" -> "أونلاين وفرق"
                "gathering_hub" -> "لوبي التجمع"
                "grand_prix" -> "الجائزة الكبرى"
                "time_trial" -> "ضد الوقت"
                "practice" -> "تدريب"
                "characters" -> "الشخصيات"
                "garage" -> "الكراج"
                "world_map" -> "خريطة العالم"
                "tracks" -> "المضامير"
                "settings" -> "الإعدادات"
                "challenges" -> "التحديات"
                else -> key
            }
            AppLanguage.ES -> when (key) {
                "free_roam" -> "MUNDO LIBRE"
                "play" -> "CARRERA RÁPIDA"
                "online" -> "EQUIPOS ONLINE"
                "gathering_hub" -> "PLAZA SOCIAL"
                "grand_prix" -> "GRAN PREMIO"
                "time_trial" -> "CONTRARRELOJ"
                "practice" -> "PRÁCTICA"
                "characters" -> "PILOTOS"
                "garage" -> "GARAJE"
                "world_map" -> "MAPA MUNDIAL"
                "tracks" -> "PISTAS"
                "settings" -> "AJUSTES"
                "challenges" -> "DESAFÍOS"
                else -> key
            }
            AppLanguage.FR -> when (key) {
                "free_roam" -> "EXPLORATION"
                "play" -> "COURSE RAPIDE"
                "online" -> "ÉQUIPES EN LIGNE"
                "gathering_hub" -> "PLACE PUBLIQUE"
                "grand_prix" -> "GRAND PRIX"
                "time_trial" -> "CONTRE-LA-MONTRE"
                "practice" -> "ENTRAÎNEMENT"
                "characters" -> "PILOTES"
                "garage" -> "GARAGE"
                "world_map" -> "CARTE DU MONDE"
                "tracks" -> "CIRCUITS"
                "settings" -> "PARAMÈTRES"
                "challenges" -> "DÉFIS"
                else -> key
            }
        }
    }
}
