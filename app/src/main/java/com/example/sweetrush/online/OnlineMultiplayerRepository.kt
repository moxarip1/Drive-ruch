package com.example.sweetrush.online

import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.KartCustomization
import com.example.sweetrush.model.PlayerProfile
import com.example.sweetrush.model.TrackId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
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
        val nowMs = System.currentTimeMillis()
        try {
            if (exists) {
                dirRef.update(
                    mapOf(
                        "displayName" to displayName.take(40),
                        "selectedKartId" to profile.selectedKartId.take(48),
                        "selectedRacerId" to profile.selectedRacerId.take(48),
                        "level" to profile.level.coerceIn(1, 999).toLong(),
                        "isOnline" to true,
                        "lastActiveMs" to nowMs,
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
                        "isOnline" to true,
                        "lastActiveMs" to nowMs,
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

    suspend fun updatePresence(uid: String, isOnline: Boolean) {
        val currentUid = auth.currentUser?.uid ?: return
        if (currentUid != uid || !isValidDocId(uid)) return
        val friendCode = computeFriendCode(uid)
        val dirRef = db.collection("racerDirectory").document(friendCode)
        try {
            dirRef.update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastActiveMs" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun fetchFriendsPresence(friends: List<FriendRosterEntry>): Map<String, Boolean> {
        if (auth.currentUser == null || friends.isEmpty()) return emptyMap()
        val nowMs = System.currentTimeMillis()
        val result = mutableMapOf<String, Boolean>()
        for (friend in friends) {
            val code = friend.friendCode.trim().uppercase()
            if (!isValidDocId(code)) {
                result[friend.friendUid] = false
                continue
            }
            val online = try {
                val snap = db.collection("racerDirectory").document(code).get().await()
                val card = snap.toObject(RacerDirectoryCard::class.java)
                card?.isCurrentlyOnline(nowMs) == true
            } catch (_: Exception) {
                false
            }
            result[friend.friendUid] = online
        }
        return result
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

    private suspend fun upsertFriendRosterDoc(
        ownerUid: String,
        friendUid: String,
        displayName: String,
        friendCode: String,
        kartId: String,
        racerId: String,
        level: Long
    ): FriendRosterEntry {
        val friendRef = db.collection("users").document(ownerUid).collection("friends").document(friendUid)
        val existing = try {
            friendRef.get().await().exists()
        } catch (_: Exception) {
            false
        }
        val safeName = cleanDisplayName(displayName)
        val safeKart = kartId.take(48).ifBlank { "strawberry_rocket" }
        val safeRacer = racerId.take(48).ifBlank { "berry_blitz" }
        val safeLevel = level.coerceIn(1L, 999L)
        val payload = mutableMapOf<String, Any>(
            "displayName" to safeName,
            "friendCode" to friendCode,
            "selectedKartId" to safeKart,
            "selectedRacerId" to safeRacer,
            "level" to safeLevel,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            if (existing) {
                friendRef.update(payload).await()
            } else {
                payload["friendUid"] = friendUid
                payload["createdAt"] = FieldValue.serverTimestamp()
                friendRef.set(payload).await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.WRITE, "users/$ownerUid/friends/$friendUid")
            throw e
        }
        return FriendRosterEntry(
            id = friendUid,
            friendUid = friendUid,
            displayName = safeName,
            friendCode = friendCode,
            selectedKartId = safeKart,
            selectedRacerId = safeRacer,
            level = safeLevel
        )
    }

    fun observeFriendRequests(uid: String): Flow<List<FriendRequestDoc>> {
        return db.collection("friendRequests")
            .whereArrayContains("participantUids", uid)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull {
                    it.toObject(FriendRequestDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
            }
    }

    suspend fun sendFriendRequestByCode(
        uid: String,
        displayName: String,
        profile: PlayerProfile,
        rawCode: String
    ): Result<FriendRequestDoc> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && isValidDocId(uid))
        val code = rawCode.trim().uppercase()
        require(code.length in 4..16 && isValidDocId(code)) { "Invalid friend ID format" }

        val dirRef = db.collection("racerDirectory").document(code)
        val snap = try {
            dirRef.get().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.GET, "racerDirectory/$code")
            throw e
        }
        require(snap.exists()) { "No racer found with ID $code" }
        val card = snap.toObject(RacerDirectoryCard::class.java)
            ?: throw IllegalStateException("Unable to read racer card")
        require(card.userId != uid && isValidDocId(card.userId)) { "You cannot send a friend request to yourself" }

        val alreadyFriend = try {
            db.collection("users").document(uid).collection("friends").document(card.userId).get().await().exists()
        } catch (_: Exception) {
            false
        }
        require(!alreadyFriend) { "${card.displayName} is already your friend" }

        val reqId = "${uid}_${card.userId}"
        require(isValidDocId(reqId)) { "Invalid request ID" }
        val reqRef = db.collection("friendRequests").document(reqId)
        val existing = try {
            reqRef.get().await().exists()
        } catch (_: Exception) {
            false
        }

        val myName = cleanDisplayName(displayName)
        val myCode = computeFriendCode(uid)
        val myKart = profile.selectedKartId.take(48).ifBlank { "strawberry_rocket" }
        val myRacer = profile.selectedRacerId.take(48).ifBlank { "berry_blitz" }
        val myLevel = profile.level.coerceIn(1, 999).toLong()

        try {
            if (existing) {
                reqRef.update(
                    mapOf(
                        "fromName" to myName,
                        "fromFriendCode" to myCode,
                        "fromKartId" to myKart,
                        "fromRacerId" to myRacer,
                        "fromLevel" to myLevel,
                        "status" to "PENDING",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                reqRef.set(
                    mapOf(
                        "fromUid" to uid,
                        "fromName" to myName,
                        "fromFriendCode" to myCode,
                        "fromKartId" to myKart,
                        "fromRacerId" to myRacer,
                        "fromLevel" to myLevel,
                        "toUid" to card.userId,
                        "toName" to cleanDisplayName(card.displayName),
                        "toFriendCode" to card.friendCode,
                        "toKartId" to card.selectedKartId.take(48).ifBlank { "strawberry_rocket" },
                        "toRacerId" to card.selectedRacerId.take(48).ifBlank { "berry_blitz" },
                        "toLevel" to card.level.coerceIn(1L, 999L),
                        "status" to "PENDING",
                        "participantUids" to listOf(uid, card.userId),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.WRITE, "friendRequests/$reqId")
            throw e
        }

        FriendRequestDoc(
            id = reqId,
            fromUid = uid,
            fromName = myName,
            fromFriendCode = myCode,
            fromKartId = myKart,
            fromRacerId = myRacer,
            fromLevel = myLevel,
            toUid = card.userId,
            toName = cleanDisplayName(card.displayName),
            toFriendCode = card.friendCode,
            toKartId = card.selectedKartId,
            toRacerId = card.selectedRacerId,
            toLevel = card.level,
            status = "PENDING",
            participantUids = listOf(uid, card.userId)
        )
    }

    suspend fun respondToFriendRequest(
        uid: String,
        request: FriendRequestDoc,
        accept: Boolean
    ): Result<FriendRosterEntry?> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && request.toUid == uid && isValidDocId(request.id))
        val newStatus = if (accept) "ACCEPTED" else "DECLINED"
        try {
            db.collection("friendRequests").document(request.id).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.UPDATE, "friendRequests/${request.id}")
            throw e
        }
        if (accept) {
            upsertFriendRosterDoc(
                ownerUid = uid,
                friendUid = request.fromUid,
                displayName = request.fromName,
                friendCode = request.fromFriendCode,
                kartId = request.fromKartId,
                racerId = request.fromRacerId,
                level = request.fromLevel
            )
        } else {
            null
        }
    }

    suspend fun syncAcceptedFriendOnSender(uid: String, request: FriendRequestDoc): Result<Unit> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && request.fromUid == uid && request.status == "ACCEPTED" && isValidDocId(request.id))
        upsertFriendRosterDoc(
            ownerUid = uid,
            friendUid = request.toUid,
            displayName = request.toName,
            friendCode = request.toFriendCode,
            kartId = request.toKartId,
            racerId = request.toRacerId,
            level = request.toLevel
        )
        try {
            db.collection("friendRequests").document(request.id).delete().await()
        } catch (_: Exception) {
        }
    }

    suspend fun deleteFriendRequest(uid: String, requestId: String): Result<Unit> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && isValidDocId(requestId))
        try {
            db.collection("friendRequests").document(requestId).delete().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.DELETE, "friendRequests/$requestId")
            throw e
        }
    }

    fun observeGameInvites(uid: String): Flow<List<GameInviteDoc>> {
        return db.collection("gameInvites")
            .whereArrayContains("participantUids", uid)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull {
                    it.toObject(GameInviteDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
            }
    }

    suspend fun sendGameInvite(
        uid: String,
        senderName: String,
        friendUid: String,
        room: OnlineRoomDoc,
        inviteType: String = "LIVE_INVITE"
    ): Result<GameInviteDoc> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && friendUid != uid && isValidDocId(friendUid) && isValidDocId(room.roomCode))
        val safeType = if (inviteType == "OFFLINE_NUDGE") "OFFLINE_NUDGE" else "LIVE_INVITE"
        val inviteId = if (safeType == "OFFLINE_NUDGE") {
            "${uid}_${friendUid}_NUDGE".take(128)
        } else {
            "${uid}_${friendUid}_${room.roomCode}".take(128)
        }
        val invRef = db.collection("gameInvites").document(inviteId)
        val exists = try {
            invRef.get().await().exists()
        } catch (_: Exception) {
            false
        }
        val safeName = cleanDisplayName(senderName)
        val safeRoomName = room.roomName.take(40).ifBlank { "Candy Squad" }
        val safeMode = if (room.mode in listOf("QUICK_RACE", "FREE_ROAM", "GATHERING_HUB")) room.mode else "QUICK_RACE"
        val safeTeam = if (room.teamSize.toInt() in listOf(1, 2, 4)) room.teamSize else 2L

        try {
            if (exists) {
                invRef.update(
                    mapOf(
                        "fromName" to safeName,
                        "roomCode" to room.roomCode,
                        "roomName" to safeRoomName,
                        "mode" to safeMode,
                        "trackId" to room.trackId,
                        "teamSize" to safeTeam,
                        "inviteType" to safeType,
                        "status" to "PENDING",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                invRef.set(
                    mapOf(
                        "fromUid" to uid,
                        "fromName" to safeName,
                        "toUid" to friendUid,
                        "roomCode" to room.roomCode,
                        "roomName" to safeRoomName,
                        "mode" to safeMode,
                        "trackId" to room.trackId,
                        "teamSize" to safeTeam,
                        "inviteType" to safeType,
                        "status" to "PENDING",
                        "participantUids" to listOf(uid, friendUid),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.WRITE, "gameInvites/$inviteId")
            throw e
        }

        GameInviteDoc(
            id = inviteId,
            fromUid = uid,
            fromName = safeName,
            toUid = friendUid,
            roomCode = room.roomCode,
            roomName = safeRoomName,
            mode = safeMode,
            trackId = room.trackId,
            teamSize = safeTeam,
            inviteType = safeType,
            status = "PENDING",
            participantUids = listOf(uid, friendUid)
        )
    }

    suspend fun sendOfflinePlayInvite(
        uid: String,
        senderName: String,
        friendUid: String,
        currentRoom: OnlineRoomDoc?
    ): Result<GameInviteDoc> {
        val targetRoom = currentRoom ?: OnlineRoomDoc(
            roomCode = "CANDYPLAZA",
            roomName = "${cleanDisplayName(senderName).take(22)}'s Squad",
            mode = "GATHERING_HUB",
            trackId = TrackId.PEPPERMINT_PLAZA.name,
            teamSize = 4L
        )
        return sendGameInvite(
            uid = uid,
            senderName = senderName,
            friendUid = friendUid,
            room = targetRoom,
            inviteType = "OFFLINE_NUDGE"
        )
    }

    suspend fun respondToGameInvite(uid: String, invite: GameInviteDoc, accept: Boolean): Result<Unit> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && invite.toUid == uid && isValidDocId(invite.id))
        try {
            db.collection("gameInvites").document(invite.id).update(
                mapOf(
                    "status" to if (accept) "ACCEPTED" else "DECLINED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.UPDATE, "gameInvites/${invite.id}")
            throw e
        }
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
        fillWithAi: Boolean,
        isGatheringHub: Boolean = false,
        roomCodeOverride: String? = null,
        roomNameOverride: String? = null
    ): Result<OnlineRoomDoc> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && isValidDocId(uid))
        val validTeamSize = if (teamSize in listOf(1, 2, 4)) teamSize else 2
        val modeString = when {
            isGatheringHub -> "GATHERING_HUB"
            mode == GameMode.FREE_ROAM -> GameMode.FREE_ROAM.name
            else -> GameMode.QUICK_RACE.name
        }
        val roomCode = roomCodeOverride?.takeIf { isValidDocId(it) && it.length in 4..16 } ?: generateRoomCode()
        val safeName = (roomNameOverride ?: if (isGatheringHub) "Candy City Plaza" else "${cleanDisplayName(displayName).take(22)}'s Squad").take(40)

        val roomRef = db.collection("rooms").document(roomCode)
        val roomMap = mapOf(
            "hostUid" to uid,
            "roomCode" to roomCode,
            "roomName" to safeName,
            "status" to "WAITING",
            "mode" to modeString,
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
            mode = modeString,
            trackId = trackId.name,
            teamSize = validTeamSize.toLong(),
            maxPlayers = 8L,
            fillWithAi = fillWithAi,
            memberUids = listOf(uid)
        )
    }

    suspend fun enterGatheringHub(
        uid: String,
        displayName: String,
        profile: PlayerProfile
    ): Result<OnlineRoomDoc> = runCatching {
        val currentUid = requireUserId()
        require(currentUid == uid && isValidDocId(uid))
        val plazaRef = db.collection("rooms").document("CANDYPLAZA")
        val snap = try {
            plazaRef.get().await()
        } catch (_: Exception) {
            null
        }
        val existing = if (snap?.exists() == true) snap.toObject(OnlineRoomDoc::class.java) else null
        when {
            existing == null -> {
                createRoom(
                    uid = uid,
                    displayName = displayName,
                    profile = profile,
                    teamSize = 4,
                    mode = GameMode.FREE_ROAM,
                    trackId = TrackId.PEPPERMINT_PLAZA,
                    fillWithAi = true,
                    isGatheringHub = true,
                    roomCodeOverride = "CANDYPLAZA",
                    roomNameOverride = "Candy City Plaza"
                ).getOrThrow()
            }
            existing.status == "WAITING" && existing.mode == "GATHERING_HUB" &&
                (uid in existing.memberUids || existing.memberUids.size < existing.maxPlayers) -> {
                joinRoomByCode(uid, displayName, profile, "CANDYPLAZA").getOrThrow()
            }
            else -> {
                createRoom(
                    uid = uid,
                    displayName = displayName,
                    profile = profile,
                    teamSize = 4,
                    mode = GameMode.FREE_ROAM,
                    trackId = TrackId.PEPPERMINT_PLAZA,
                    fillWithAi = true,
                    isGatheringHub = true,
                    roomNameOverride = "${cleanDisplayName(displayName).take(16)} Plaza"
                ).getOrThrow()
            }
        }
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
        fillWithAi: Boolean,
        isGatheringHub: Boolean = false
    ): Result<Unit> = runCatching {
        requireUserId()
        val modeString = when {
            isGatheringHub -> "GATHERING_HUB"
            mode == GameMode.FREE_ROAM -> GameMode.FREE_ROAM.name
            else -> GameMode.QUICK_RACE.name
        }
        val validTeamSize = if (teamSize in listOf(1, 2, 4)) teamSize else 2
        try {
            db.collection("rooms").document(roomCode).update(
                mapOf(
                    "mode" to modeString,
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
            val roomRef = db.collection("rooms").document(roomCode)
            val snap = roomRef.get().await()
            val room = if (snap.exists()) snap.toObject(OnlineRoomDoc::class.java) else null
            val remaining = room?.memberUids?.filter { it != uid } ?: emptyList()
            if (room == null || remaining.isEmpty()) {
                if (snap.exists()) roomRef.delete().await()
            } else {
                roomRef.update(
                    mapOf(
                        "memberUids" to remaining,
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
