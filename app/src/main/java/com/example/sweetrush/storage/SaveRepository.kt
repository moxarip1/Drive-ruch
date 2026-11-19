package com.example.sweetrush.storage

import android.content.Context
import com.example.sweetrush.model.AppLanguage
import com.example.sweetrush.model.CameraPreset
import com.example.sweetrush.model.ControlScheme
import com.example.sweetrush.model.ExhaustStyle
import com.example.sweetrush.model.GameSettings
import com.example.sweetrush.model.GraphicsQuality
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.KartDecoration
import com.example.sweetrush.model.KartUpgradeLevels
import com.example.sweetrush.model.PlayerProfile
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.model.WheelDesign
import com.example.sweetrush.model.WorldRegion
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SaveRepository(private val context: Context) {

    private val primaryFile: File get() = File(context.filesDir, "sweet_rush_save.json")
    private val backupFile: File get() = File(context.filesDir, "sweet_rush_save.json.bak")

    fun loadProfile(): PlayerProfile {
        val candidates = listOf(primaryFile, backupFile)
        for (file in candidates) {
            if (!file.exists()) continue
            try {
                val json = JSONObject(file.readText())
                return parseProfile(json)
            } catch (_: Exception) {
                // Fall through to backup or safe defaults on corrupted save
            }
        }
        return PlayerProfile()
    }

    fun saveProfile(profile: PlayerProfile) {
        try {
            val json = serializeProfile(profile).toString(2)
            val tmpFile = File(context.filesDir, "sweet_rush_save.json.tmp")
            tmpFile.writeText(json)
            if (primaryFile.exists()) {
                primaryFile.copyTo(backupFile, overwrite = true)
            }
            if (!tmpFile.renameTo(primaryFile)) {
                primaryFile.writeText(json)
                tmpFile.delete()
            }
        } catch (_: Exception) {
            // Keep app resilient on disk errors
        }
    }

    fun resetProfile(): PlayerProfile {
        val fresh = PlayerProfile()
        saveProfile(fresh)
        return fresh
    }

    internal fun serializeProfile(p: PlayerProfile): JSONObject {
        val root = JSONObject()
        root.put("xp", p.xp)
        root.put("level", p.level)
        root.put("credits", p.credits)
        root.put("selectedKartId", p.selectedKartId)
        root.put("selectedRacerId", p.selectedRacerId)
        root.put("unlockedKarts", JSONArray(p.unlockedKarts.toList()))
        root.put("unlockedRacers", JSONArray(p.unlockedRacers.toList()))
        root.put("unlockedPaints", JSONArray(p.unlockedPaints.toList()))
        root.put("unlockedWheels", JSONArray(p.unlockedWheels.map { it.name }))
        root.put("unlockedExhausts", JSONArray(p.unlockedExhausts.map { it.name }))
        root.put("unlockedDecorations", JSONArray(p.unlockedDecorations.map { it.name }))
        root.put("completedChallenges", JSONArray(p.completedChallenges.toList()))
        root.put("collectedWorldCoins", JSONArray(p.collectedWorldCoins.toList()))
        root.put("collectedWorldStars", JSONArray(p.collectedWorldStars.toList()))
        root.put("discoveredRegions", JSONArray(p.discoveredRegions.map { it.name }))
        root.put("racesCompleted", p.racesCompleted)
        root.put("racesWon", p.racesWon)
        root.put("grandPrixGoldTrophies", p.grandPrixGoldTrophies)
        root.put("grandPrixBestPoints", p.grandPrixBestPoints)
        root.put("companyLevel", p.companyLevel)
        root.put("companyContractsCompleted", p.companyContractsCompleted)

        val customObj = JSONObject()
        p.kartCustomizations.forEach { (kartId, custom) ->
            val cObj = JSONObject()
            cObj.put("paintId", custom.paintId)
            cObj.put("wheelDesign", custom.wheelDesign.name)
            cObj.put("exhaustStyle", custom.exhaustStyle.name)
            cObj.put("decoration", custom.decoration.name)
            val uObj = JSONObject()
            uObj.put("speed", custom.upgrades.speed)
            uObj.put("acceleration", custom.upgrades.acceleration)
            uObj.put("handling", custom.upgrades.handling)
            uObj.put("boost", custom.upgrades.boost)
            uObj.put("stability", custom.upgrades.stability)
            cObj.put("upgrades", uObj)
            customObj.put(kartId, cObj)
        }
        root.put("kartCustomizations", customObj)

        val lapsObj = JSONObject()
        p.bestLapMsByTrack.forEach { (trackId, ms) ->
            lapsObj.put(trackId.name, ms)
        }
        root.put("bestLapMsByTrack", lapsObj)

        val sObj = JSONObject()
        sObj.put("graphicsQuality", p.settings.graphicsQuality.name)
        sObj.put("targetFps", p.settings.targetFps)
        sObj.put("musicVolume", p.settings.musicVolume.toDouble())
        sObj.put("sfxVolume", p.settings.sfxVolume.toDouble())
        sObj.put("controlScheme", p.settings.controlScheme.name)
        sObj.put("steeringSensitivity", p.settings.steeringSensitivity.toDouble())
        sObj.put("cameraSensitivity", p.settings.cameraSensitivity.toDouble())
        sObj.put("cameraPreset", p.settings.cameraPreset.name)
        sObj.put("cameraDistanceScale", p.settings.cameraDistanceScale.toDouble())
        sObj.put("cameraHeightOffset", p.settings.cameraHeightOffset.toDouble())
        sObj.put("cameraFovOffset", p.settings.cameraFovOffset.toDouble())
        sObj.put("tiltSteeringEnabled", p.settings.tiltSteeringEnabled)
        sObj.put("language", p.settings.language.name)
        sObj.put("showDebugTelemetry", p.settings.showDebugTelemetry)
        root.put("settings", sObj)
        return root
    }

    internal fun parseProfile(root: JSONObject): PlayerProfile {
        val defaults = PlayerProfile()
        val sObj = root.optJSONObject("settings")
        val settings = if (sObj != null) {
            GameSettings(
                graphicsQuality = enumValueOrDefault(sObj.optString("graphicsQuality"), GraphicsQuality.HIGH),
                targetFps = sObj.optInt("targetFps", 60).coerceIn(30, 60),
                musicVolume = sObj.optDouble("musicVolume", 0.70).toFloat().coerceIn(0f, 1f),
                sfxVolume = sObj.optDouble("sfxVolume", 0.85).toFloat().coerceIn(0f, 1f),
                controlScheme = enumValueOrDefault(sObj.optString("controlScheme"), ControlScheme.TOUCH_BUTTONS),
                steeringSensitivity = sObj.optDouble("steeringSensitivity", 1.0).toFloat().coerceIn(0.4f, 1.8f),
                cameraSensitivity = sObj.optDouble("cameraSensitivity", 1.0).toFloat().coerceIn(0.4f, 1.8f),
                cameraPreset = enumValueOrDefault(sObj.optString("cameraPreset"), CameraPreset.DYNAMIC_CHASE),
                cameraDistanceScale = sObj.optDouble("cameraDistanceScale", 1.0).toFloat().coerceIn(0.65f, 1.5f),
                cameraHeightOffset = sObj.optDouble("cameraHeightOffset", 0.0).toFloat().coerceIn(-1.2f, 2.5f),
                cameraFovOffset = sObj.optDouble("cameraFovOffset", 0.0).toFloat().coerceIn(-10f, 16f),
                tiltSteeringEnabled = sObj.optBoolean("tiltSteeringEnabled", false),
                language = enumValueOrDefault(sObj.optString("language"), AppLanguage.EN),
                showDebugTelemetry = sObj.optBoolean("showDebugTelemetry", false)
            )
        } else defaults.settings

        val customMap = mutableMapOf<String, KartCustomization>()
        val customObj = root.optJSONObject("kartCustomizations")
        if (customObj != null) {
            val keys = customObj.keys()
            while (keys.hasNext()) {
                val kId = keys.next()
                val cObj = customObj.optJSONObject(kId) ?: continue
                val uObj = cObj.optJSONObject("upgrades")
                val upgrades = if (uObj != null) {
                    KartUpgradeLevels(
                        speed = uObj.optInt("speed", 0).coerceIn(0, 5),
                        acceleration = uObj.optInt("acceleration", 0).coerceIn(0, 5),
                        handling = uObj.optInt("handling", 0).coerceIn(0, 5),
                        boost = uObj.optInt("boost", 0).coerceIn(0, 5),
                        stability = uObj.optInt("stability", 0).coerceIn(0, 5)
                    )
                } else KartUpgradeLevels()
                customMap[kId] = KartCustomization(
                    paintId = cObj.optString("paintId", "default"),
                    wheelDesign = enumValueOrDefault(cObj.optString("wheelDesign"), WheelDesign.PEPPERMINT_SWIRL),
                    exhaustStyle = enumValueOrDefault(cObj.optString("exhaustStyle"), ExhaustStyle.SUGAR_SPARK),
                    decoration = enumValueOrDefault(cObj.optString("decoration"), KartDecoration.NONE),
                    upgrades = upgrades
                )
            }
        }

        val bestLaps = mutableMapOf<TrackId, Long>()
        val lapsObj = root.optJSONObject("bestLapMsByTrack")
        if (lapsObj != null) {
            for (track in TrackId.entries) {
                if (lapsObj.has(track.name)) {
                    val v = lapsObj.optLong(track.name, -1L)
                    if (v > 0L) bestLaps[track] = v
                }
            }
        }

        return PlayerProfile(
            xp = root.optInt("xp", 0).coerceAtLeast(0),
            level = root.optInt("level", 1).coerceAtLeast(1),
            credits = root.optInt("credits", 350).coerceAtLeast(0),
            selectedKartId = root.optString("selectedKartId", defaults.selectedKartId),
            selectedRacerId = root.optString("selectedRacerId", defaults.selectedRacerId),
            unlockedKarts = parseStringSet(root.optJSONArray("unlockedKarts"), defaults.unlockedKarts),
            unlockedRacers = parseStringSet(root.optJSONArray("unlockedRacers"), defaults.unlockedRacers),
            unlockedPaints = parseStringSet(root.optJSONArray("unlockedPaints"), defaults.unlockedPaints),
            unlockedWheels = parseEnumSet(root.optJSONArray("unlockedWheels"), defaults.unlockedWheels),
            unlockedExhausts = parseEnumSet(root.optJSONArray("unlockedExhausts"), defaults.unlockedExhausts),
            unlockedDecorations = parseEnumSet(root.optJSONArray("unlockedDecorations"), defaults.unlockedDecorations),
            kartCustomizations = customMap,
            bestLapMsByTrack = bestLaps,
            completedChallenges = parseStringSet(root.optJSONArray("completedChallenges"), emptySet()),
            collectedWorldCoins = parseStringSet(root.optJSONArray("collectedWorldCoins"), emptySet()),
            collectedWorldStars = parseStringSet(root.optJSONArray("collectedWorldStars"), emptySet()),
            discoveredRegions = parseEnumSet(root.optJSONArray("discoveredRegions"), defaults.discoveredRegions),
            racesCompleted = root.optInt("racesCompleted", 0).coerceAtLeast(0),
            racesWon = root.optInt("racesWon", 0).coerceAtLeast(0),
            grandPrixGoldTrophies = root.optInt("grandPrixGoldTrophies", 0).coerceAtLeast(0),
            grandPrixBestPoints = root.optInt("grandPrixBestPoints", 0).coerceAtLeast(0),
            companyLevel = root.optInt("companyLevel", 1).coerceIn(1, 10),
            companyContractsCompleted = root.optInt("companyContractsCompleted", 0).coerceAtLeast(0),
            settings = settings
        )
    }

    private fun parseStringSet(arr: JSONArray?, fallback: Set<String>): Set<String> {
        if (arr == null) return fallback
        val out = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optString(i, "")
            if (s.isNotEmpty()) out.add(s)
        }
        return if (out.isEmpty() && fallback.isNotEmpty()) fallback else out
    }

    private inline fun <reified T : Enum<T>> parseEnumSet(arr: JSONArray?, fallback: Set<T>): Set<T> {
        if (arr == null) return fallback
        val out = mutableSetOf<T>()
        for (i in 0 until arr.length()) {
            val name = arr.optString(i, "")
            enumValues<T>().find { it.name == name }?.let { out.add(it) }
        }
        return if (out.isEmpty() && fallback.isNotEmpty()) fallback else out
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return enumValues<T>().find { it.name == raw } ?: fallback
    }
}
