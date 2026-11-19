package com.example.sweetrush.model

object CatalogData {

    val karts: List<KartSpec> = listOf(
        KartSpec(
            id = "strawberry_rocket",
            name = "Candy Kart • سيارة فانيليوبي الرسمية",
            style = KartBodyStyle.STRAWBERRY_ROCKET,
            racingNumber = 1,
            tagline = "السيارة الرسمية للعبة (Made by Vanellope) • شاسيه شوكولاتة وويفر، خطوط كريمة وردية، رشات سكر، أنابيب حلوى وجناح بسكويت!",
            defaultPrimaryHex = 0xFF7B2851,
            defaultSecondaryHex = 0xFFFF2E88,
            accentHex = 0xFF3CE8B5,
            baseStats = KartStats(0.90f, 0.92f, 0.92f, 0.94f, 0.88f),
            physicsTuning = KartPhysicsTuning(190f, 36f, 6.6f, 9.4f, 3.7f, 1.08f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "peppermint_racer",
            name = "Pink Lightning • سيارة تافيتا",
            style = KartBodyStyle.PEPPERMINT_RACER,
            racingNumber = 2,
            tagline = "سيارة Taffyta Muttonfudge الوردية • هيكل فورمولا الحلوى الصلبة مع مرايا مصاصة وعجلات منقطة بالأبيض!",
            defaultPrimaryHex = 0xFFFF2474,
            defaultSecondaryHex = 0xFFFFFFFF,
            accentHex = 0xFFFF85B3,
            baseStats = KartStats(0.92f, 0.88f, 0.94f, 0.88f, 0.82f),
            physicsTuning = KartPhysicsTuning(180f, 36f, 6.8f, 9.6f, 3.3f, 1.18f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "cupcake_kart",
            name = "Ice Cream & Birthday Cake • سيارة كاندل هيد",
            style = KartBodyStyle.CUPCAKE_KART,
            racingNumber = 3,
            tagline = "سيارة Candlehead • كيك شوكولاتة مغطى بالكريمة الوردية و5 شموع عيد ميلاد مشتعلة في الخلف وعجلات نعناع أخضر!",
            defaultPrimaryHex = 0xFF5D2E1E,
            defaultSecondaryHex = 0xFFFF7EB9,
            accentHex = 0xFF3CE8B5,
            baseStats = KartStats(0.84f, 0.88f, 0.86f, 0.90f, 0.86f),
            physicsTuning = KartPhysicsTuning(210f, 33f, 6.2f, 8.6f, 3.5f, 1.04f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "chocolate_cruiser",
            name = "Peanut Butter Cup • سيارة رانسيس",
            style = KartBodyStyle.CHOCOLATE_CRUISER,
            racingNumber = 4,
            tagline = "سيارة Rancis Fluggerbutter • قالب زبدة الفول السوداني بالشوكولاتة الداكنة والذهبية مع عجلات بسكويت الكاكاو!",
            defaultPrimaryHex = 0xFF5A2E1B,
            defaultSecondaryHex = 0xFFFFA000,
            accentHex = 0xFFFFD54F,
            baseStats = KartStats(0.88f, 0.82f, 0.82f, 0.86f, 0.92f),
            physicsTuning = KartPhysicsTuning(235f, 40f, 7.2f, 8.6f, 3.8f, 0.98f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "cherry_pie_kart",
            name = "Cherry Bomb Pie • سيارة جوبيلينا",
            style = KartBodyStyle.CHERRY_PIE_KART,
            racingNumber = 5,
            tagline = "سيارة Jubileena Bing-Bing • شريحة فطيرة الكرز المثلثة بحشوة الكرز الأحمر اللامع وحواف العجين المقرمش!",
            defaultPrimaryHex = 0xFFE51C38,
            defaultSecondaryHex = 0xFFF4C27F,
            accentHex = 0xFFFF758F,
            baseStats = KartStats(0.87f, 0.90f, 0.88f, 0.89f, 0.84f),
            physicsTuning = KartPhysicsTuning(194f, 35f, 6.5f, 9.2f, 3.5f, 1.10f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "fro_cone_kart",
            name = "Fro Cone • سيارة سنوانا",
            style = KartBodyStyle.FRO_CONE_KART,
            racingNumber = 6,
            tagline = "سيارة Snowanna Rainbeau • كوب ثلج مجروش وردي مع محرك مكعبات ثلج شفافة وعجلات كرات السوربيه الملونة!",
            defaultPrimaryHex = 0xFFFF3D9A,
            defaultSecondaryHex = 0xFF7B2CBF,
            accentHex = 0xFFFF9E00,
            baseStats = KartStats(0.85f, 0.89f, 0.91f, 0.88f, 0.83f),
            physicsTuning = KartPhysicsTuning(188f, 34f, 6.3f, 9.3f, 3.3f, 1.13f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "tiramissile_kart",
            name = "Tira-Missile #8 • سيارة كرامبيلينا",
            style = KartBodyStyle.TIRAMISSILE_KART,
            racingNumber = 8,
            tagline = "سيارة Crumbelina DiCaramello رقم 8 • سيارة تيراميسو إيطالية مخططة بالكريمة البيضاء والكاكاو الداكن!",
            defaultPrimaryHex = 0xFFFFF8EE,
            defaultSecondaryHex = 0xFF4A2511,
            accentHex = 0xFFD4A373,
            baseStats = KartStats(0.91f, 0.87f, 0.90f, 0.86f, 0.87f),
            physicsTuning = KartPhysicsTuning(196f, 36f, 6.7f, 9.4f, 3.6f, 1.09f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "marshmallow_speedster",
            name = "Rocket Pop • سيارة أدورابيزل",
            style = KartBodyStyle.MARSHMALLOW_SPEEDSTER,
            racingNumber = 9,
            tagline = "سيارة Adorabeezle Winterpop • صاروخ آيس بوب ثلاثي الألوان (أحمر وأبيض وأزرق ثلجي) مع جناح نفاث!",
            defaultPrimaryHex = 0xFFE51C38,
            defaultSecondaryHex = 0xFFFFFFFF,
            accentHex = 0xFF1E88E5,
            baseStats = KartStats(0.89f, 0.88f, 0.84f, 0.92f, 0.88f),
            physicsTuning = KartPhysicsTuning(202f, 32f, 6.0f, 8.9f, 3.5f, 1.05f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "donut_drifter",
            name = "Pumpkin Corn Racer • سيارة غلويد",
            style = KartBodyStyle.DONUT_DRIFTER,
            racingNumber = 10,
            tagline = "سيارة Gloyd Orangeboar • حلوى الذرة واليقطين البرتقالية المخصصة للدرفت السريع والمقالب!",
            defaultPrimaryHex = 0xFFFF7A00,
            defaultSecondaryHex = 0xFF3E1F0D,
            accentHex = 0xFFFFD600,
            baseStats = KartStats(0.86f, 0.85f, 0.92f, 0.90f, 0.82f),
            physicsTuning = KartPhysicsTuning(192f, 34f, 6.2f, 8.5f, 2.9f, 1.14f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "jellybean_machine",
            name = "Tongue Twister • سيارة سويزل",
            style = KartBodyStyle.JELLYBEAN_MACHINE,
            racingNumber = 11,
            tagline = "سيارة Swizzle 'The Swizz' Malarkey • كبسولة حلوى الجيلي والمصاصات الحلزونية النيون السريعة!",
            defaultPrimaryHex = 0xFF00E5FF,
            defaultSecondaryHex = 0xFFFF2E93,
            accentHex = 0xFFFFEA00,
            baseStats = KartStats(0.86f, 0.91f, 0.89f, 0.88f, 0.82f),
            physicsTuning = KartPhysicsTuning(188f, 31f, 5.9f, 9.1f, 3.4f, 1.11f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "mint_candy_kart",
            name = "Melon Mint Cruiser • سيارة مينتي زاكي",
            style = KartBodyStyle.MINT_CANDY_KART,
            racingNumber = 14,
            tagline = "سيارة Minty Zaki • آيس كريم النعناع الأخضر الياباني مع رقائق الشوكولاتة الداكنة وكرز الماراشينو!",
            defaultPrimaryHex = 0xFF2CE6B0,
            defaultSecondaryHex = 0xFF3B1E12,
            accentHex = 0xFFFF2E63,
            baseStats = KartStats(0.88f, 0.87f, 0.91f, 0.87f, 0.86f),
            physicsTuning = KartPhysicsTuning(190f, 35f, 6.5f, 9.2f, 3.4f, 1.10f),
            unlockCost = 0,
            unlockLevel = 1
        ),
        KartSpec(
            id = "caramel_thunder",
            name = "Royal Candy Sovereign • سيارة الملك كاندي",
            style = KartBodyStyle.CARAMEL_THUNDER,
            racingNumber = 99,
            tagline = "سيارة King Candy الملكية • كوبيه القصر الوردي المذهّب مع التاج الملكي الخلفي ورايات المملكة!",
            defaultPrimaryHex = 0xFFFF5E7E,
            defaultSecondaryHex = 0xFFFFF8E7,
            accentHex = 0xFFFFC107,
            baseStats = KartStats(0.95f, 0.86f, 0.84f, 0.92f, 0.90f),
            physicsTuning = KartPhysicsTuning(218f, 38f, 7.0f, 9.1f, 3.4f, 1.07f),
            unlockCost = 0,
            unlockLevel = 1
        )
    )

    val racers: List<RacerSpec> = listOf(
        RacerSpec(
            id = "berry_blitz",
            name = "Vanellope von Schweetz • فانيليوبي",
            title = "أميرة شوغر راش • The Glitch Princess",
            personality = "سريعة ومرحة، ترتدي هودي النعناع الأخضر مع شريطة عرق السوس الحمراء وحلوى السبرينكلز في شعرها الأسود!",
            suitPrimaryHex = 0xFF2CE6B0,
            suitSecondaryHex = 0xFF4A2535,
            helmetHex = 0xFF16161C,
            visorHex = 0xFFFF2455,
            skinHex = 0xFFFFD8BE,
            favoriteKartId = "strawberry_rocket",
            signaturePowerUp = PowerUpType.SUGAR_BOOST,
            victoryLine = "أنا في السباق! فوز حلو بطعم الغليتش السريع!",
            defeatLine = "مهلاً! كان ذلك مجرد غليتش صغير.. السباق القادم لي!",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "minty_swirl",
            name = "Taffyta Muttonfudge • تافيتا",
            title = "قائدة المتسابقين • Pink Lightning Ace",
            personality = "متسابقة الحلوى الوردية المحترفة بخوذة الفراولة الوردية وشعرها الأشقر، تنافس دائماً على المركز الأول!",
            suitPrimaryHex = 0xFFFF2E7E,
            suitSecondaryHex = 0xFFFFFFFF,
            helmetHex = 0xFFFF3888,
            visorHex = 0xFF3CE8B5,
            skinHex = 0xFFFFDFC8,
            favoriteKartId = "peppermint_racer",
            signaturePowerUp = PowerUpType.PEPPERMINT_BLAST,
            victoryLine = "البرق الوردي لا يُهزم أبداً في مضمار شوغر راش!",
            defeatLine = "تافيتا لا تخسر! لا بد أن الكريمة كانت زلقة!",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "frosting_faye",
            name = "Candlehead • كاندل هيد",
            title = "بطلة شمعة عيد الميلاد • Birthday Candle Star",
            personality = "تعشق الكيك والاحتفالات، بشعرها الأخضر النعناعي وخوذة الكيك التي تعلوها شمعة عيد ميلاد مشتعلة!",
            suitPrimaryHex = 0xFFFF7EB9,
            suitSecondaryHex = 0xFF3CE8B5,
            helmetHex = 0xFFFFA6C9,
            visorHex = 0xFFFFB300,
            skinHex = 0xFFFFDAB9,
            favoriteKartId = "cupcake_kart",
            signaturePowerUp = PowerUpType.RAINBOW_STAR,
            victoryLine = "تمنّوا أمنية واطفئوا الشموع.. لقد فزت بالسباق!",
            defeatLine = "أوه لا! كادت شمعتي أن تنطفئ عند المنعطف!",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "coco_dash",
            name = "Rancis Fluggerbutter • رانسيس",
            title = "أمير زبدة الفول السوداني • Peanut Butter Prodigy",
            personality = "أنيق وواثق، يرتدي سترة الشوكولاتة البنية والبرتقالية مع قبعة قالب زبدة الفول السوداني المائلة!",
            suitPrimaryHex = 0xFF5D3422,
            suitSecondaryHex = 0xFFFF8F00,
            helmetHex = 0xFFD82E2E,
            visorHex = 0xFFFFD54F,
            skinHex = 0xFFFFDFC4,
            favoriteKartId = "chocolate_cruiser",
            signaturePowerUp = PowerUpType.CHOCOLATE_SMOKE,
            victoryLine = "مزيج الشوكولاتة وزبدة الفول السوداني دائماً في الصدارة!",
            defeatLine = "تحتاج سيارتي إلى ضبط الكراميل قبل الجولة القادمة.",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "jubileena",
            name = "Jubileena Bing-Bing • جوبيلينا",
            title = "نجمة فطيرة الكرز • Cherry Pie Sprinter",
            personality = "حلوة ومتحمسة، ترتدي خوذة الكرز الأحمر اللامعة مع ضفيرتين حمراوين وفستان فطيرة الكرز!",
            suitPrimaryHex = 0xFFE51C38,
            suitSecondaryHex = 0xFFFFE5EC,
            helmetHex = 0xFFD0112B,
            visorHex = 0xFF4CAF50,
            skinHex = 0xFFFFDFC8,
            favoriteKartId = "cherry_pie_kart",
            signaturePowerUp = PowerUpType.SUGAR_BOOST,
            victoryLine = "أحلى من فطيرة الكرز الطازجة على خط النهاية!",
            defeatLine = "تناثر عصير الكرز على المسار.. سأعوضها فوراً!",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "snowanna",
            name = "Snowanna Rainbeau • سنوانا",
            title = "ملكة الثلج المجروش • Fro-Cone Disco Queen",
            personality = "تتألق بتسريحة شعر الأفرو بألوان قوس قزح اللامعة وسترة الفرو الوردية والبنفسجية!",
            suitPrimaryHex = 0xFFFF3D9A,
            suitSecondaryHex = 0xFF8E24AA,
            helmetHex = 0xFFFF80AB,
            visorHex = 0xFF00E5FF,
            skinHex = 0xFFB86B3E,
            favoriteKartId = "fro_cone_kart",
            signaturePowerUp = PowerUpType.RAINBOW_STAR,
            victoryLine = "برودة الثلج وألوان قوس قزح تشعل المضمار!",
            defeatLine = "ذاب الثلج المجروش قليلاً.. انتظروني في اللفة القادمة!",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "crumbelina",
            name = "Crumbelina DiCaramello • كرامبيلينا",
            title = "بطلة التيراميسو الإيطالية • Tiramisu Aristocrat",
            personality = "متسابقة التيراميسو والكابتشينو الأنيقة بقبعة البيريه البنية والكريمة البيضاء، تقود السيارة رقم 8!",
            suitPrimaryHex = 0xFF4A2511,
            suitSecondaryHex = 0xFFFFF8EE,
            helmetHex = 0xFF5D3422,
            visorHex = 0xFFD4A373,
            skinHex = 0xFFF7D1B3,
            favoriteKartId = "tiramissile_kart",
            signaturePowerUp = PowerUpType.CARAMEL_TRAP,
            victoryLine = "برافو! قيادة إيطالية بنكهة التيراميسو الفاخر!",
            defeatLine = "انسكبت رغوة الكابتشينو.. سأعود أقوى!",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "mallow_max",
            name = "Adorabeezle Winterpop • أدورابيزل",
            title = "رائدة فضاء الآيس بوب • Rocket Pop Explorer",
            personality = "ترتدي خوذة صاروخ المثلجات الثلاثية الألوان (الأحمر والأبيض والأزرق) ومعطف الفرو الأزرق الثلجي!",
            suitPrimaryHex = 0xFF1E88E5,
            suitSecondaryHex = 0xFFE51C38,
            helmetHex = 0xFFFFFFFF,
            visorHex = 0xFF00E5FF,
            skinHex = 0xFFFFDAB9,
            favoriteKartId = "marshmallow_speedster",
            signaturePowerUp = PowerUpType.GUMMY_SHIELD,
            victoryLine = "انطلاق صاروخي بارد كالثلج نحو المركز الأول!",
            defeatLine = "تجمدت الفرامل في المنعطف الجليدي!",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "glazed_gus",
            name = "Gloyd Orangeboar • غلويد",
            title = "مشاغب الهالوين • Candy Corn Trickster",
            personality = "عاشق المقالب وحلوى الذرة البرتقالية، يرتدي قبعة اليقطين وسترة برتقالية وبنية!",
            suitPrimaryHex = 0xFFFF7A00,
            suitSecondaryHex = 0xFF3E1F0D,
            helmetHex = 0xFFFF8F00,
            visorHex = 0xFFFFEA00,
            skinHex = 0xFFF5CBA7,
            favoriteKartId = "donut_drifter",
            signaturePowerUp = PowerUpType.CARAMEL_TRAP,
            victoryLine = "خدعة أم حلوى؟ الحلوى والكأس لي وحدي!",
            defeatLine = "أفسدتم مقلبي على خط النهاية!",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "fizzy_pop",
            name = "Swizzle 'The Swizz' Malarkey • سويزل",
            title = "بهلوان الحلوى الحامضة • Tongue-Twister Daredevil",
            personality = "يعشق الحركات البهلوانية والسرعة القصوى، يرتدي خوذة مخططة بالأرجواني والأخضر النيون!",
            suitPrimaryHex = 0xFF7B2CBF,
            suitSecondaryHex = 0xFF00E676,
            helmetHex = 0xFF00B0FF,
            visorHex = 0xFFFFEA00,
            skinHex = 0xFFFFD8B8,
            favoriteKartId = "jellybean_machine",
            signaturePowerUp = PowerUpType.JELLY_DASH,
            victoryLine = "ذا سويز يطير فوق الجميع ويخطف اللقب!",
            defeatLine = "قفزة مجنونة أكثر من اللازم!",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "minty_zaki",
            name = "Minty Zaki • مينتي زاكي",
            title = "نجمة النعناع الأخضر • Melon Mint Champion",
            personality = "متسابقة آيس كريم النعناع الأخضر بفيونكة حمراء كبيرة وبدلة النعناع المرقطة بالشوكولاتة!",
            suitPrimaryHex = 0xFF2CE6B0,
            suitSecondaryHex = 0xFF3B1E12,
            helmetHex = 0xFF26C69A,
            visorHex = 0xFFFF2E63,
            skinHex = 0xFFFFE0C8,
            favoriteKartId = "mint_candy_kart",
            signaturePowerUp = PowerUpType.GUMMY_SHIELD,
            victoryLine = "انتعاش النعناع الأخضر يحسم السباق بأناقة!",
            defeatLine = "سأضيف المزيد من النعناع للتيربو في المرة القادمة!",
            unlockCost = 0,
            unlockLevel = 1
        ),
        RacerSpec(
            id = "toffee_rex",
            name = "King Candy • الملك كاندي",
            title = "حاكم مملكة شوغر راش • Sovereign of Sugar Rush",
            personality = "حاكم القلعة المثير للجدل، يرتدي التاج الذهبي وربطة العنق الوردية ويقود السيارة الملكية!",
            suitPrimaryHex = 0xFFFFF3B0,
            suitSecondaryHex = 0xFFFF3D8B,
            helmetHex = 0xFFFFC107,
            visorHex = 0xFF7B2CBF,
            skinHex = 0xFFFFD6BA,
            favoriteKartId = "caramel_thunder",
            signaturePowerUp = PowerUpType.CARAMEL_TRAP,
            victoryLine = "هاهاها! تيربو-تاستيك! الملك كاندي يفوز مجدداً!",
            defeatLine = "مستحيل! أنا ملك هذه اللعبة، كيف تجاوزتني؟!",
            unlockCost = 0,
            unlockLevel = 1
        )
    )

    val tracks: List<TrackSpec> = listOf(
        TrackSpec(
            id = TrackId.PEPPERMINT_PLAZA,
            name = "Sugar Rush Speedway • مضمار القلعة",
            subtitle = "King Candy's Castle & Starting Grid",
            description = "انطلق من أمام قلعة الملك كاندي، عبر أقواس قصب السكر، مدرجات الكيك ومصنع السيارات في قلب المملكة!",
            laps = 3,
            skyTopHex = 0xFF4CC9FF,
            skyBottomHex = 0xFFFFC8E6,
            fogHex = 0xFFFFD8EC,
            groundHex = 0xFF66E2B3,
            roadPrimaryHex = 0xFF3E294D,
            roadStripeHex = 0xFFFF4D8D,
            curbPrimaryHex = 0xFFFF2448,
            curbSecondaryHex = 0xFFFFFFFF,
            featureHighlights = listOf("King Candy's Castle", "Candy Cane Arches", "Kart Bakery Grid"),
            difficultyStars = 1
        ),
        TrackSpec(
            id = TrackId.CUPCAKE_COAST,
            name = "Cakeway & Gumball Gorge • وادي الكيك والعلكة",
            subtitle = "Birthday Cake Hills & Gumball Machines",
            description = "انزلق عبر تلال الكيك المملوءة بالكريمة، جسور الدونات، وأنفاق الويفر وصولاً إلى وادي ماكينات العلكة العملاقة!",
            laps = 3,
            skyTopHex = 0xFF29B6F6,
            skyBottomHex = 0xFFFFE0B2,
            fogHex = 0xFFFFE9CC,
            groundHex = 0xFFFF94BA,
            roadPrimaryHex = 0xFFFFF3E0,
            roadStripeHex = 0xFFFF6EA8,
            curbPrimaryHex = 0xFF3CE8B5,
            curbSecondaryHex = 0xFFFFF8E1,
            featureHighlights = listOf("Cakeway Frosting Hills", "Gumball Gorge Boulders", "Wafer Tunnels"),
            difficultyStars = 2
        ),
        TrackSpec(
            id = TrackId.CHOCOLATE_VOLCANO,
            name = "Diet Cola Mountain • جبل دايت كولا",
            subtitle = "Boiling Cola Crater & Mentos Stalactites",
            description = "تسلق منحدرات الشوكولاتة نحو فوهة جبل الدايت كولا الفوار وتفادَ صواعد المنتوس المتساقطة وأنهار الكراميل الساخنة!",
            laps = 3,
            skyTopHex = 0xFF3D1338,
            skyBottomHex = 0xFFFF6D3B,
            fogHex = 0xFF9E3A3E,
            groundHex = 0xFF3A1C12,
            roadPrimaryHex = 0xFF4E2A1E,
            roadStripeHex = 0xFFFF9E1B,
            curbPrimaryHex = 0xFFFFB300,
            curbSecondaryHex = 0xFF3E1F14,
            featureHighlights = listOf("Diet Cola Volcano", "Mentos Stalactites", "Caramel Lava Rivers"),
            difficultyStars = 3
        ),
        TrackSpec(
            id = TrackId.GUMDROP_GARDEN,
            name = "Candy Cane Forest • غابة قصب السكر",
            subtitle = "Candy Cane Trees & Lollipop Sanctuary",
            description = "انطلق بين أشجار قصب السكر المخططة، جسور عرق السوس، وبحيرات العلكة السرية حيث تدربت فانيليوبي على القيادة!",
            laps = 3,
            skyTopHex = 0xFF6A4C93,
            skyBottomHex = 0xFFA0F0ED,
            fogHex = 0xFFD2F5F2,
            groundHex = 0xFF7BE495,
            roadPrimaryHex = 0xFF5D3A7A,
            roadStripeHex = 0xFF4FFFB0,
            curbPrimaryHex = 0xFFFFEB3B,
            curbSecondaryHex = 0xFFFF4081,
            featureHighlights = listOf("Candy Cane Woods", "Licorice Vines", "Gumdrop Lake"),
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
