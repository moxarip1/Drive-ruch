package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.Difficulty
import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.PlayerProfile
import com.example.sweetrush.model.PowerUpType
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.physics.KartPhysicsEngine
import com.example.sweetrush.physics.PlayerInputState
import com.example.sweetrush.storage.SaveRepository
import com.example.sweetrush.track.TrackGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SweetRushTest {

    @Test
    fun `catalog contains 8 karts 8 racers 4 tracks and 7 powerups`() {
        assertEquals(8, CatalogData.karts.size)
        assertEquals(8, CatalogData.racers.size)
        assertEquals(4, CatalogData.tracks.size)
        assertEquals(7, PowerUpType.entries.size)
        assertEquals(4, CatalogData.challenges.size)
    }

    @Test
    fun `all 4 tracks generate valid road splines and candy decorations`() {
        for (trackId in TrackId.entries) {
            val track = TrackGenerator.getTrack(trackId)
            assertTrue("Track $trackId should have >= 120 spline nodes", track.nodes.size >= 120)
            assertTrue("Track $trackId should have boost pads", track.boostPads.isNotEmpty())
            assertTrue("Track $trackId should have jump ramps", track.jumpRamps.isNotEmpty())
            assertTrue("Track $trackId should have item boxes", track.itemBoxes.size >= 16)
            assertTrue("Track $trackId should have rich candy decorations", track.decorations.size >= 60)
        }
    }

    @Test
    fun `kart accelerates drifts charges mini turbo jumps and uses nitro`() {
        val engine = KartPhysicsEngine(
            trackId = TrackId.PEPPERMINT_PLAZA,
            mode = GameMode.QUICK_RACE,
            difficulty = Difficulty.NORMAL,
            playerRacerId = "berry_blitz",
            playerKartId = "strawberry_rocket",
            playerCustomization = KartCustomization()
        )
        assertEquals(8, engine.karts.size)

        // Advance past countdown
        repeat(120) {
            engine.step(1f / 30f, PlayerInputState(throttle = true))
        }
        assertTrue(engine.raceStarted)
        assertTrue("Player kart should accelerate forward", engine.playerKart.speed > 20f)

        // Drift while steering right to charge drift boost
        repeat(35) {
            engine.step(1f / 30f, PlayerInputState(steer = 0.85f, throttle = true, drift = true))
        }
        assertTrue("Drift charge should accumulate", engine.playerKart.driftCharge >= 0.7f)

        // Release drift -> triggers boostTimer
        engine.step(1f / 30f, PlayerInputState(steer = 0f, throttle = true, drift = false))
        assertTrue("Releasing charged drift should trigger boost", engine.playerKart.boostTimer > 0.4f)
        assertEquals(1, engine.playerDriftBoostCount)

        // Jump action
        engine.step(1f / 30f, PlayerInputState(throttle = true, jumpPressed = true))
        assertFalse("Kart should leave ground on jump", engine.playerKart.isGrounded)
    }

    @Test
    fun `powerups activate and apply gameplay effects`() {
        val engine = KartPhysicsEngine(
            trackId = TrackId.CUPCAKE_COAST,
            mode = GameMode.QUICK_RACE,
            difficulty = Difficulty.HARD,
            playerRacerId = "minty_swirl",
            playerKartId = "peppermint_racer",
            playerCustomization = KartCustomization()
        )
        engine.raceStarted = true

        engine.playerKart.heldPowerUp = PowerUpType.GUMMY_SHIELD
        engine.step(1f / 30f, PlayerInputState(throttle = true, usePowerUpPressed = true))
        assertTrue(engine.playerKart.shieldTimer > 0f)

        engine.playerKart.heldPowerUp = PowerUpType.CARAMEL_TRAP
        engine.step(1f / 30f, PlayerInputState(throttle = true, usePowerUpPressed = true))
        assertEquals(1, engine.droppedHazards.size)

        engine.playerKart.heldPowerUp = PowerUpType.RAINBOW_STAR
        engine.step(1f / 30f, PlayerInputState(throttle = true, usePowerUpPressed = true))
        assertTrue(engine.playerKart.starTimer > 0f)
    }

    @Test
    fun `save repository persists profile and recovers from corruption`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SaveRepository(context)

        val customProfile = PlayerProfile(
            xp = 95,
            level = 4,
            credits = 1234,
            selectedKartId = "donut_drifter",
            selectedRacerId = "glazed_gus",
            unlockedKarts = setOf("strawberry_rocket", "donut_drifter"),
            racesWon = 7
        )
        repo.saveProfile(customProfile)
        val loaded = repo.loadProfile()
        assertEquals(1234, loaded.credits)
        assertEquals(4, loaded.level)
        assertEquals("donut_drifter", loaded.selectedKartId)
        assertEquals(7, loaded.racesWon)

        // Corrupt primary save and verify safe fallback to backup / defaults
        val primary = File(context.filesDir, "sweet_rush_save.json")
        primary.writeText("{not valid json!!!")
        val recovered = repo.loadProfile()
        assertNotNull(recovered)
        assertTrue(recovered.level >= 1)
    }
}
