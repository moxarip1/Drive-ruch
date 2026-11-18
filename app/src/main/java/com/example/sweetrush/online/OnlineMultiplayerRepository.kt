package com.example.sweetrush.online

import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.PlayerProfile
import com.example.sweetrush.model.TrackId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import kotlin.random.Random

class OnlineMultiplayerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Google Sign-In required")

    fun computeFriendCode(uid: String): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val digest = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray())
        val sb = StringBuilder("SR")
        for (i in 0 until 6) {
            val idx = (digest[i].toInt() and 0xFF) % alphabet.length
            sb.append(alphabet[idx])
        }
        return sb.toString()
    }

    fun generateRoomCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rng = Random(System.nanoTime())
        return buildString(6) {
            repeat(6) { append(alphabet[rng.nextInt(alphabet.length)]) }
        }
    }

    private fun isValidDocId(id: String): Boolean =
        id.length in 1..128 && id.all { it.isLetterOrDigit() || it == '_' || it == '-' }

    private fun cleanDisplayName(raw: String?): String =
        raw?.trim()?.take(40)?.takeIf { it.isNotEmpty() } ?: "Candy Racer"

    suspend fun ensureUserProfileAndDirectory(
        uid: String,
        displayName: String?,
        photoUrl: String?,
        localProfile: PlayerProfile,
        localProfileJson: String
    ): Result<CloudUserProfile> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && isValidDocId(uid)) { "Invalid authenticated user" }

        val safeName = cleanDisplayName(displayName)
        val safeCode = computeFriendCode(uid)
        val safePhoto = photoUrl?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
        val safeJson = localProfileJson.take(39500).ifBlank { "{}" }

        val userRef = db.collection("users").document(uid)
        val existingSnap = try {
            userRef.get().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.GET, "users/$uid")
            throw e
        }

        val savedProfile = if (existingSnap.exists()) {
            val existingCloud = existingSnap.toObject(CloudUserProfile::class.java)
            val updates = mutableMapOf<String, Any>(
                "displayName" to safeName,
                "friendCode" to safeCode,
                "level" to localProfile.level.coerceIn(1, 999).toLong(),
                "xp" to localProfile.xp.coerceIn(0, 1000000).toLong(),
                "credits" to localProfile.credits.coerceIn(0, 10000000).toLong(),
                "racesWon" to localProfile.racesWon.coerceAtLeast(0).toLong(),
                "racesCompleted" to localProfile.racesCompleted.coerceAtLeast(0).toLong(),
                "selectedKartId" to localProfile.selectedKartId.take(48),
                "selectedRacerId" to localProfile.selectedRacerId.take(48),
                "profileJson" to safeJson,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (safePhoto != null) updates["photoUrl"] = safePhoto

            try {
                userRef.update(updates).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
                throw e
            }

            existingCloud?.copy(
                displayName = safeName,
                photoUrl = safePhoto ?: existingCloud.photoUrl,
                friendCode = safeCode,
                level = maxOf(existingCloud.level, localProfile.level.toLong()),
                credits = maxOf(existingCloud.credits, localProfile.credits.toLong()),
                profileJson = existingCloud.profileJson.takeIf { it.length > 10 } ?: safeJson
            ) ?: CloudUserProfile(
                id = uid,
                userId = uid,
                displayName = safeName,
                photoUrl = safePhoto,
                friendCode = safeCode,
                profileJson = safeJson
            )
        } else {
            val createPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to safeName,
                "friendCode" to safeCode,
                "level" to localProfile.level.coerceIn(1, 999).toLong(),
                "xp" to localProfile.xp.coerceIn(0, 1000000).toLong(),
                "credits" to localProfile.credits.coerceIn(0, 10000000).toLong(),
                "racesWon" to localProfile.racesWon.coerceAtLeast(0).toLong(),
                "racesCompleted" to localProfile.racesCompleted.coerceAtLeast(0).toLong(),
                "selectedKartId" to localProfile.selectedKartId.take(48),
                "selectedRacerId" to localProfile.selectedRacerId.take(48),
                "profileJson" to safeJson,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (safePhoto != null) createPayload["photoUrl"] = safePhoto

            try {
                userRef.set(createPayload).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.CREATE, "users/$uid")
                throw e
            }

            CloudUserProfile(
                id = uid,
                userId = uid,
                displayName = safeName,
                photoUrl = safePhoto,
                friendCode = safeCode,
                level = localProfile.level.toLong(),
                xp = localProfile.xp.toLong(),
                credits = localProfile.credits.toLong(),
                racesWon = localProfile.racesWon.toLong(),
                racesCompleted = localProfile.racesCompleted.toLong(),
                selectedKartId = localProfile.selectedKartId,
                selectedRacerId = localProfile.selectedRacerId,
                profileJson = safeJson
            )
        }

        upsertDirectoryEntry(uid, safeName, safeCode, localProfile)
        savedProfile
    }

    private suspend fun upsertDirectoryEntry(
        uid: String,
        displayName: String,
        friendCode: String,
        profile: PlayerProfile
    ) {
        val dirRef = db.collection("racerDirectory").document(friendCode)
        val exists = try {
            dirRef.get().await().exists()
        } catch (_: Exception) {
            false
        }
        try {
            if (exists) {
                dirRef.update(
                    mapOf(
                        "displayName" to displayName.take(40),
                        "selectedKartId" to profile.selectedKartId.take(48),
                        "selectedRacerId" to profile.selectedRacerId.take(48),
                        "level" to profile.level.coerceIn(1, 999).toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                dirRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to displayName.take(40),
                        "friendCode" to friendCode,
                        "selectedKartId" to profile.selectedKartId.take(48),
                        "selectedRacerId" to profile.selectedRacerId.take(48),
                        "level" to profile.level.coerceIn(1, 999).toLong(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.WRITE, "racerDirectory/$friendCode")
        }
    }

    suspend fun syncCloudProfile(
        uid: String,
        displayName: String?,
        photoUrl: String?,
        profile: PlayerProfile,
        profileJson: String
    ): Result<Unit> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && isValidDocId(uid))
        val safeName = cleanDisplayName(displayName)
        val safeCode = computeFriendCode(uid)
        val safePhoto = photoUrl?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
        val safeJson = profileJson.take(39500).ifBlank { "{}" }

        val updates = mutableMapOf<String, Any>(
            "displayName" to safeName,
            "friendCode" to safeCode,
            "level" to profile.level.coerceIn(1, 999).toLong(),
            "xp" to profile.xp.coerceIn(0, 1000000).toLong(),
            "credits" to profile.credits.coerceIn(0, 10000000).toLong(),
            "racesWon" to profile.racesWon.coerceAtLeast(0).toLong(),
            "racesCompleted" to profile.racesCompleted.coerceAtLeast(0).toLong(),
            "selectedKartId" to profile.selectedKartId.take(48),
            "selectedRacerId" to profile.selectedRacerId.take(48),
            "profileJson" to safeJson,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (safePhoto != null) updates["photoUrl"] = safePhoto

        try {
            db.collection("users").document(uid).update(updates).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                val createMap = updates.toMutableMap().apply {
                    put("userId", uid)
                    put("createdAt", FieldValue.serverTimestamp())
                }
                db.collection("users").document(uid).set(createMap).await()
            } else {
                handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
                throw e
            }
        }
        upsertDirectoryEntry(uid, safeName, safeCode, profile)
    }

    fun observeFriends(uid: String): Flow<List<FriendRosterEntry>> {
        return db.collection("users")
            .document(uid)
            .collection("friends")
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap -> snap.toObjects(FriendRosterEntry::class.java).sortedBy { it.displayName } }
    }

    suspend fun addFriendByCode(uid: String, rawCode: String): Result<FriendRosterEntry> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && isValidDocId(uid))
        val code = rawCode.trim().uppercase()
        require(code.length in 4..16 && isValidDocId(code)) { "Invalid friend code format" }

        val dirRef = db.collection("racerDirectory").document(code)
        val snap = try {
            dirRef.get().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.GET, "racerDirectory/$code")
            throw e
        }
        require(snap.exists()) { "No racer found with code $code" }
        val card = snap.toObject(RacerDirectoryCard::class.java)
            ?: throw IllegalStateException("Unable to read racer card")
        require(card.userId != uid && isValidDocId(card.userId)) { "You cannot add your own friend code" }

        val friendRef = db.collection("users").document(uid).collection("friends").document(card.userId)
        val existing = try {
            friendRef.get().await().exists()
        } catch (_: Exception) {
            false
        }
        val payload = mutableMapOf<String, Any>(
            "displayName" to card.displayName.take(40),
            "friendCode" to card.friendCode,
            "selectedKartId" to card.selectedKartId.take(48),
            "selectedRacerId" to card.selectedRacerId.take(48),
            "level" to card.level.coerceIn(1L, 999L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            if (existing) {
                friendRef.update(payload).await()
            } else {
                payload["friendUid"] = card.userId
                payload["createdAt"] = FieldValue.serverTimestamp()
                friendRef.set(payload).await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/friends/${card.userId}")
            throw e
        }

        FriendRosterEntry(
            id = card.userId,
            friendUid = card.userId,
            displayName = card.displayName,
            friendCode = card.friendCode,
            selectedKartId = card.selectedKartId,
            selectedRacerId = card.selectedRacerId,
            level = card.level
        )
    }

    suspend fun removeFriend(uid: String, friendUid: String): Result<Unit> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && isValidDocId(friendUid))
        try {
            db.collection("users").document(uid).collection("friends").document(friendUid).delete().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/friends/$friendUid")
            throw e
        }
    }

    suspend fun createRoom(
        uid: String,
        displayName: String,
        profile: PlayerProfile,
        teamSize: Int,
        mode: GameMode,
        trackId: TrackId,
        fillWithAi: Boolean
    ): Result<OnlineRoomDoc> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && isValidDocId(uid))
        val validTeamSize = if (teamSize in listOf(1, 2, 4)) teamSize else 2
        val validMode = if (mode == GameMode.FREE_ROAM) GameMode.FREE_ROAM else GameMode.QUICK_RACE
        val roomCode = generateRoomCode()
        val safeName = "${cleanDisplayName(displayName).take(22)}'s Squad".take(40)

        val roomRef = db.collection("rooms").document(roomCode)
        val roomMap = mapOf(
            "hostUid" to uid,
            "roomCode" to roomCode,
            "roomName" to safeName,
            "status" to "WAITING",
            "mode" to validMode.name,
            "trackId" to trackId.name,
            "teamSize" to validTeamSize.toLong(),
            "maxPlayers" to 8L,
            "fillWithAi" to fillWithAi,
            "memberUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            roomRef.set(roomMap).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.CREATE, "rooms/$roomCode")
            throw e
        }

        upsertPlayerDoc(
            roomCode = roomCode,
            uid = uid,
            displayName = displayName,
            profile = profile,
            teamIndex = 0,
            ready = true,
            visibleToUids = listOf(uid)
        ).getOrThrow()

        OnlineRoomDoc(
            id = roomCode,
            hostUid = uid,
            roomCode = roomCode,
            roomName = safeName,
            status = "WAITING",
            mode = validMode.name,
            trackId = trackId.name,
            teamSize = validTeamSize.toLong(),
            maxPlayers = 8L,
            fillWithAi = fillWithAi,
            memberUids = listOf(uid)
        )
    }

    suspend fun joinRoomByCode(
        uid: String,
        displayName: String,
        profile: PlayerProfile,
        rawRoomCode: String
    ): Result<OnlineRoomDoc> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && isValidDocId(uid))
        val roomCode = rawRoomCode.trim().uppercase()
        require(roomCode.length in 4..16 && isValidDocId(roomCode)) { "Enter a valid room code" }

        val roomRef = db.collection("rooms").document(roomCode)
        val snap = try {
            roomRef.get().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.GET, "rooms/$roomCode")
            throw e
        }
        require(snap.exists()) { "Room $roomCode does not exist" }
        val room = snap.toObject(OnlineRoomDoc::class.java)
            ?: throw IllegalStateException("Unable to read room")

        val alreadyMember = uid in room.memberUids
        val updatedMembers = if (alreadyMember) room.memberUids else room.memberUids + uid

        if (!alreadyMember) {
            require(room.status == "WAITING") { "Race already started in room $roomCode" }
            require(room.memberUids.size < room.maxPlayers) { "Room $roomCode is full" }
            try {
                roomRef.update(
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.UPDATE, "rooms/$roomCode")
                throw e
            }
        }

        val numTeams = (8 / room.teamSize.toInt().coerceIn(1, 4)).coerceIn(1, 4)
        val defaultTeam = ((updatedMembers.indexOf(uid).coerceAtLeast(0)) % numTeams)

        upsertPlayerDoc(
            roomCode = roomCode,
            uid = uid,
            displayName = displayName,
            profile = profile,
            teamIndex = defaultTeam,
            ready = false,
            visibleToUids = updatedMembers
        ).getOrThrow()

        room.copy(memberUids = updatedMembers)
    }

    fun observeRoom(roomCode: String): Flow<OnlineRoomDoc?> {
        return db.collection("rooms")
            .document(roomCode)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap -> if (snap.exists()) snap.toObject(OnlineRoomDoc::class.java) else null }
    }

    fun observeRoomPlayers(roomCode: String, uid: String): Flow<List<RoomPlayerDoc>> {
        return db.collection("rooms")
            .document(roomCode)
            .collection("players")
            .whereArrayContains("visibleToUids", uid)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap -> snap.toObjects(RoomPlayerDoc::class.java).sortedBy { it.teamIndex } }
    }

    fun observeRoomMessages(roomCode: String, uid: String): Flow<List<RoomMessageDoc>> {
        return db.collection("rooms")
            .document(roomCode)
            .collection("messages")
            .whereArrayContains("memberUids", uid)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.toObjects(RoomMessageDoc::class.java)
                    .sortedBy { it.clientSeq }
                    .takeLast(60)
            }
    }

    suspend fun upsertPlayerDoc(
        roomCode: String,
        uid: String,
        displayName: String,
        profile: PlayerProfile,
        teamIndex: Int,
        ready: Boolean,
        visibleToUids: List<String>
    ): Result<Unit> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && isValidDocId(roomCode) && isValidDocId(uid))

        val custom = profile.kartCustomizations[profile.selectedKartId] ?: KartCustomization()
        val safeVisible = (if (uid in visibleToUids) visibleToUids else visibleToUids + uid).distinct().take(8)
        val safeTeam = teamIndex.coerceIn(0, 3).toLong()
        val playerRef = db.collection("rooms").document(roomCode).collection("players").document(uid)

        val exists = try {
            playerRef.get().await().exists()
        } catch (_: Exception) {
            false
        }

        if (exists) {
            try {
                playerRef.update(
                    mapOf(
                        "displayName" to cleanDisplayName(displayName),
                        "racerId" to profile.selectedRacerId.take(48),
                        "kartId" to profile.selectedKartId.take(48),
                        "paintId" to custom.paintId.take(48),
                        "wheelId" to custom.wheelDesign.name.take(48),
                        "exhaustId" to custom.exhaustStyle.name.take(48),
                        "decoId" to custom.decoration.name.take(48),
                        "teamIndex" to safeTeam,
                        "ready" to ready,
                        "visibleToUids" to safeVisible,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.UPDATE, "rooms/$roomCode/players/$uid")
                throw e
            }
        } else {
            val createMap = mapOf(
                "userId" to uid,
                "displayName" to cleanDisplayName(displayName),
                "racerId" to profile.selectedRacerId.take(48),
                "kartId" to profile.selectedKartId.take(48),
                "paintId" to custom.paintId.take(48),
                "wheelId" to custom.wheelDesign.name.take(48),
                "exhaustId" to custom.exhaustStyle.name.take(48),
                "decoId" to custom.decoration.name.take(48),
                "teamIndex" to safeTeam,
                "ready" to ready,
                "x" to 0.0,
                "y" to 0.0,
                "z" to 0.0,
                "yawRad" to 0.0,
                "speed" to 0.0,
                "steerVisualRad" to 0.0,
                "lap" to 1L,
                "raceScore" to 0.0,
                "finished" to false,
                "finishTimeMs" to 0L,
                "drifting" to false,
                "nitroActive" to false,
                "visibleToUids" to safeVisible,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                playerRef.set(createMap).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.CREATE, "rooms/$roomCode/players/$uid")
                throw e
            }
        }
    }

    suspend fun syncVisibleToUids(roomCode: String, uid: String, memberUids: List<String>) {
        if (roomCode.isBlank()) return
        val safeVisible = (if (uid in memberUids) memberUids else memberUids + uid).distinct().take(8)
        try {
            db.collection("rooms")
                .document(roomCode)
                .collection("players")
                .document(uid)
                .update(
                    mapOf(
                        "visibleToUids" to safeVisible,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.UPDATE, "rooms/$roomCode/players/$uid")
        }
    }

    suspend fun pushTelemetry(
        roomCode: String,
        uid: String,
        x: Double,
        y: Double,
        z: Double,
        yawRad: Double,
        speed: Double,
        steerVisualRad: Double,
        lap: Int,
        raceScore: Double,
        finished: Boolean,
        finishTimeMs: Long,
        drifting: Boolean,
        nitroActive: Boolean,
        visibleToUids: List<String>
    ) {
        if (auth.currentUser?.uid != uid) return
        val safeVisible = (if (uid in visibleToUids) visibleToUids else visibleToUids + uid).distinct().take(8)
        try {
            db.collection("rooms")
                .document(roomCode)
                .collection("players")
                .document(uid)
                .update(
                    mapOf(
                        "x" to x,
                        "y" to y,
                        "z" to z,
                        "yawRad" to yawRad,
                        "speed" to speed,
                        "steerVisualRad" to steerVisualRad,
                        "lap" to lap.coerceIn(1, 99).toLong(),
                        "raceScore" to raceScore,
                        "finished" to finished,
                        "finishTimeMs" to finishTimeMs.coerceAtLeast(0L),
                        "drifting" to drifting,
                        "nitroActive" to nitroActive,
                        "visibleToUids" to safeVisible,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.UPDATE, "rooms/$roomCode/players/$uid")
        }
    }

    suspend fun updateRoomConfig(
        roomCode: String,
        mode: GameMode,
        trackId: TrackId,
        teamSize: Int,
        fillWithAi: Boolean
    ): Result<Unit> = runCatching {
        requireUserId()
        val validMode = if (mode == GameMode.FREE_ROAM) GameMode.FREE_ROAM else GameMode.QUICK_RACE
        val validTeamSize = if (teamSize in listOf(1, 2, 4)) teamSize else 2
        try {
            db.collection("rooms").document(roomCode).update(
                mapOf(
                    "mode" to validMode.name,
                    "trackId" to trackId.name,
                    "teamSize" to validTeamSize.toLong(),
                    "maxPlayers" to 8L,
                    "fillWithAi" to fillWithAi,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.UPDATE, "rooms/$roomCode")
            throw e
        }
    }

    suspend fun updateRoomStatus(roomCode: String, status: String): Result<Unit> = runCatching {
        requireUserId()
        require(status in listOf("WAITING", "COUNTDOWN", "RACING", "FINISHED"))
        try {
            db.collection("rooms").document(roomCode).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.UPDATE, "rooms/$roomCode")
            throw e
        }
    }

    suspend fun leaveRoom(roomCode: String, uid: String, isHost: Boolean): Result<Unit> = runCatching {
        if (auth.currentUser == null || !isValidDocId(roomCode)) return@runCatching
        try {
            db.collection("rooms").document(roomCode).collection("players").document(uid).delete().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.DELETE, "rooms/$roomCode/players/$uid")
        }
        try {
            if (isHost) {
                db.collection("rooms").document(roomCode).delete().await()
            } else {
                db.collection("rooms").document(roomCode).update(
                    mapOf(
                        "memberUids" to FieldValue.arrayRemove(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.WRITE, "rooms/$roomCode")
        }
    }

    suspend fun sendMessage(
        roomCode: String,
        uid: String,
        senderName: String,
        teamIndex: Int,
        targetTeamOnly: Boolean,
        msgType: String,
        text: String,
        audioB64: String? = null,
        durationMs: Long = 0L,
        memberUids: List<String>
    ): Result<Unit> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && isValidDocId(roomCode))
        require(msgType in listOf("TEXT", "QUICK", "VOICE"))
        val safeMembers = (if (uid in memberUids) memberUids else memberUids + uid).distinct().take(8)
        val safeText = text.trim().take(240)
        val safeAudio = audioB64?.take(72000)?.takeIf { it.isNotEmpty() }
        val msgId = "msg_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"

        val payload = mutableMapOf<String, Any>(
            "senderUid" to uid,
            "senderName" to cleanDisplayName(senderName),
            "teamIndex" to teamIndex.coerceIn(0, 3).toLong(),
            "targetTeamOnly" to targetTeamOnly,
            "msgType" to msgType,
            "text" to safeText,
            "durationMs" to durationMs.coerceIn(0L, 6000L),
            "clientSeq" to System.currentTimeMillis(),
            "memberUids" to safeMembers,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (safeAudio != null) {
            payload["audioB64"] = safeAudio
        }

        try {
            db.collection("rooms")
                .document(roomCode)
                .collection("messages")
                .document(msgId)
                .set(payload)
                .await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.CREATE, "rooms/$roomCode/messages/$msgId")
            throw e
        }
    }
}
