package com.example.sweetrush.online

import androidx.annotation.Keep
import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.TrackId
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Candy Racer",
    val photoUrl: String? = null,
    val friendCode: String = "",
    val level: Long = 1L,
    val xp: Long = 0L,
    val credits: Long = 350L,
    val racesWon: Long = 0L,
    val racesCompleted: Long = 0L,
    val selectedKartId: String = "strawberry_rocket",
    val selectedRacerId: String = "berry_blitz",
    val profileJson: String = "{}",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendRosterEntry(
    @DocumentId val id: String = "",
    val friendUid: String = "",
    val displayName: String = "Friend",
    val friendCode: String = "",
    val selectedKartId: String = "strawberry_rocket",
    val selectedRacerId: String = "berry_blitz",
    val level: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RacerDirectoryCard(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Candy Racer",
    val friendCode: String = "",
    val selectedKartId: String = "strawberry_rocket",
    val selectedRacerId: String = "berry_blitz",
    val level: Long = 1L,
    val isOnline: Boolean = false,
    val lastActiveMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isCurrentlyOnline(nowMs: Long = System.currentTimeMillis()): Boolean =
        isOnline && lastActiveMs > 0L && (nowMs - lastActiveMs) < 60_000L
}

@Keep
@IgnoreExtraProperties
data class OnlineRoomDoc(
    @DocumentId val id: String = "",
    val hostUid: String = "",
    val roomCode: String = "",
    val roomName: String = "Candy Squad Room",
    val status: String = "WAITING", // WAITING, COUNTDOWN, RACING, FINISHED
    val mode: String = GameMode.QUICK_RACE.name,
    val trackId: String = TrackId.PEPPERMINT_PLAZA.name,
    val teamSize: Long = 2L, // 1, 2, or 4
    val maxPlayers: Long = 8L,
    val fillWithAi: Boolean = true,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedTrackId(): TrackId = enumValues<TrackId>().firstOrNull { it.name == trackId } ?: TrackId.PEPPERMINT_PLAZA
    fun parsedMode(): GameMode = when (mode) {
        "GATHERING_HUB", "FREE_ROAM" -> GameMode.FREE_ROAM
        else -> GameMode.QUICK_RACE
    }
    fun isGatheringHub(): Boolean = mode == "GATHERING_HUB"
}

@Keep
@IgnoreExtraProperties
data class FriendRequestDoc(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromFriendCode: String = "",
    val fromKartId: String = "strawberry_rocket",
    val fromRacerId: String = "berry_blitz",
    val fromLevel: Long = 1L,
    val toUid: String = "",
    val toName: String = "",
    val toFriendCode: String = "",
    val toKartId: String = "strawberry_rocket",
    val toRacerId: String = "berry_blitz",
    val toLevel: Long = 1L,
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GameInviteDoc(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val toUid: String = "",
    val roomCode: String = "",
    val roomName: String = "",
    val mode: String = GameMode.QUICK_RACE.name,
    val trackId: String = TrackId.PEPPERMINT_PLAZA.name,
    val teamSize: Long = 2L,
    val inviteType: String = "LIVE_INVITE", // LIVE_INVITE or OFFLINE_NUDGE
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedTrackId(): TrackId = enumValues<TrackId>().firstOrNull { it.name == trackId } ?: TrackId.PEPPERMINT_PLAZA
    fun parsedMode(): GameMode = when (mode) {
        "GATHERING_HUB", "FREE_ROAM" -> GameMode.FREE_ROAM
        else -> GameMode.QUICK_RACE
    }
    fun isGatheringHub(): Boolean = mode == "GATHERING_HUB"
    fun isOfflineNudge(): Boolean = inviteType == "OFFLINE_NUDGE"
}

@Keep
@IgnoreExtraProperties
data class RoomPlayerDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Racer",
    val racerId: String = "berry_blitz",
    val kartId: String = "strawberry_rocket",
    val paintId: String = "default",
    val wheelId: String = "PEPPERMINT_SWIRL",
    val exhaustId: String = "SUGAR_SPARK",
    val decoId: String = "NONE",
    val teamIndex: Long = 0L,
    val ready: Boolean = false,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val yawRad: Double = 0.0,
    val speed: Double = 0.0,
    val steerVisualRad: Double = 0.0,
    val lap: Long = 1L,
    val raceScore: Double = 0.0,
    val finished: Boolean = false,
    val finishTimeMs: Long = 0L,
    val drifting: Boolean = false,
    val nitroActive: Boolean = false,
    val visibleToUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomMessageDoc(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "Racer",
    val teamIndex: Long = 0L,
    val targetTeamOnly: Boolean = false,
    val msgType: String = "TEXT", // TEXT, QUICK, VOICE
    val text: String = "",
    val audioB64: String? = null,
    val durationMs: Long = 0L,
    val clientSeq: Long = 0L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)
