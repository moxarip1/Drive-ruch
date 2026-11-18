package com.example.sweetrush.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sweetrush.model.CatalogData
import com.example.sweetrush.model.GameMode
import com.example.sweetrush.model.PlayerProfile
import com.example.sweetrush.model.SquadTeam
import com.example.sweetrush.model.TrackId
import com.example.sweetrush.online.FriendRequestDoc
import com.example.sweetrush.online.FriendRosterEntry
import com.example.sweetrush.online.OnlineRoomDoc
import com.example.sweetrush.online.RoomMessageDoc
import com.example.sweetrush.online.RoomPlayerDoc
import com.example.ui.theme.BerryCard
import com.example.ui.theme.BerryDeep
import com.example.ui.theme.BerryPurple
import com.example.ui.theme.Butterscotch
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyPinkDark
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.FrostingWhite
import com.example.ui.theme.GrapeJelly
import com.example.ui.theme.MintCream
import com.example.ui.theme.MintDark
import com.example.ui.theme.SkyCandy
import com.example.ui.theme.StrawberryRed

@Composable
fun OnlineLobbyScreen(
    currentUserId: String,
    friendCode: String,
    profile: PlayerProfile,
    room: OnlineRoomDoc?,
    players: List<RoomPlayerDoc>,
    messages: List<RoomMessageDoc>,
    friends: List<FriendRosterEntry>,
    friendRequests: List<FriendRequestDoc>,
    isBusy: Boolean,
    isRecordingVoice: Boolean,
    isVoiceMuted: Boolean,
    statusMessage: String?,
    onCreateRoom: (teamSize: Int, mode: GameMode, trackId: TrackId, fillWithAi: Boolean, isGatheringHub: Boolean) -> Unit,
    onEnterGatheringHub: () -> Unit,
    onJoinRoom: (roomCode: String) -> Unit,
    onLeaveRoom: () -> Unit,
    onUpdateRoomConfig: (mode: GameMode, trackId: TrackId, teamSize: Int, fillWithAi: Boolean, isGatheringHub: Boolean) -> Unit,
    onSelectTeamAndReady: (teamIndex: Int, ready: Boolean) -> Unit,
    onStartMatch: () -> Unit,
    onSendFriendRequest: (friendCode: String) -> Unit,
    onRespondFriendRequest: (FriendRequestDoc, Boolean) -> Unit,
    onDismissFriendRequest: (String) -> Unit,
    onInviteFriend: (FriendRosterEntry) -> Unit,
    onRemoveFriend: (friendUid: String) -> Unit,
    onSendChat: (text: String, teamOnly: Boolean) -> Unit,
    onSendQuickCallout: (text: String, teamOnly: Boolean) -> Unit,
    onToggleLiveMic: (teamOnly: Boolean) -> Unit,
    onToggleVoiceMute: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var draftTeamSize by remember { mutableIntStateOf(2) }
    var draftMode by remember { mutableStateOf(GameMode.QUICK_RACE) }
    var draftIsHub by remember { mutableStateOf(false) }
    var draftTrack by remember { mutableStateOf(TrackId.PEPPERMINT_PLAZA) }
    var draftFillAi by remember { mutableStateOf(true) }
    var joinCodeInput by remember { mutableStateOf("") }

    var rightTab by remember { mutableIntStateOf(0) } // 0 = Chat/Voice, 1 = Friends, 2 = Requests
    var friendCodeInput by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var teamChannelOnly by remember { mutableStateOf(false) }
    var hasMicPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPerm = granted
        if (granted && room != null) {
            onToggleLiveMic(teamChannelOnly)
        }
    }

    val incomingPendingRequests = friendRequests.filter { it.toUid == currentUserId && it.status == "PENDING" }
    val outgoingRequests = friendRequests.filter { it.fromUid == currentUserId }

    val localPlayer = players.firstOrNull { it.userId == currentUserId }
    val localTeamIdx = localPlayer?.teamIndex?.toInt() ?: 0
    val localReady = localPlayer?.ready ?: false
    val isHost = room?.hostUid == currentUserId

    val msgListState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            msgListState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BerryDeep.copy(alpha = 0.88f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CandyScreenHeader(
                title = "Online Squads • Gathering Hub • Friends",
                profile = profile,
                onBack = onBack
            )

            if (statusMessage != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MintCream)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(statusMessage, style = MaterialTheme.typography.labelLarge, color = BerryDeep)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // LEFT PANE: Gathering Hub + Create / Join OR Active Squad Room
                Column(
                    modifier = Modifier
                        .weight(1.20f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(BerryPurple.copy(alpha = 0.92f))
                        .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (room == null) {
                        // Featured 1-Tap Gathering Hub Card (لوبي التجمع)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(CandyPink.copy(alpha = 0.35f), BerryDeep.copy(alpha = 0.88f))
                                    )
                                )
                                .border(1.5.dp, CaramelGold.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("لوبي التجمع • CANDY GATHERING HUB", style = MaterialTheme.typography.labelLarge, color = CaramelGold)
                                Text(
                                    text = "Meet friends in Candy City Plaza, chat with open mic & cruise together",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FrostingWhite.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            CandyButton(
                                text = "ENTER HUB",
                                onClick = onEnterGatheringHub,
                                testTag = "enter_gathering_hub_button",
                                icon = Icons.Filled.Storefront,
                                topColor = CaramelGold,
                                bottomColor = Butterscotch,
                                textColor = BerryDeep,
                                height = 44.dp,
                                modifier = Modifier.width(136.dp)
                            )
                        }

                        Text("CREATE OR JOIN SQUAD ROOM", style = MaterialTheme.typography.titleMedium, color = MintCream)

                        // Squad Size Selector: 1, 2, or 4 Players per Team
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1 to "SOLO (1)", 2 to "DUO (2)", 4 to "QUAD (4)").forEach { (sz, label) ->
                                val selected = draftTeamSize == sz
                                Box(
                                    modifier = Modifier
                                        .testTag("team_size_$sz")
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) CaramelGold else BerryCard)
                                        .border(1.5.dp, if (selected) Color.White else Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                        .clickable { draftTeamSize = sz },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) BerryDeep else FrostingWhite
                                    )
                                }
                            }
                        }

                        // Mode (Race / Open World / Hub) & AI Fill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val modeChips = listOf(
                                Triple(GameMode.QUICK_RACE, false, "RACE"),
                                Triple(GameMode.FREE_ROAM, false, "WORLD"),
                                Triple(GameMode.FREE_ROAM, true, "HUB")
                            )
                            modeChips.forEach { (m, isHub, label) ->
                                val sel = (draftMode == m && draftIsHub == isHub)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) MintCream else BerryCard)
                                        .clickable {
                                            draftMode = m
                                            draftIsHub = isHub
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = if (sel) BerryDeep else FrostingWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("AI", style = MaterialTheme.typography.labelSmall, color = FrostingWhite)
                                Switch(
                                    checked = draftFillAi,
                                    onCheckedChange = { draftFillAi = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MintCream, checkedTrackColor = MintDark)
                                )
                            }
                        }

                        // Track Chips (when Race mode selected)
                        if (draftMode == GameMode.QUICK_RACE && !draftIsHub) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(CatalogData.tracks) { tr ->
                                    val sel = draftTrack == tr.id
                                    Box(
                                        modifier = Modifier
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (sel) CandyPink else BerryCard)
                                            .border(1.dp, Color.White.copy(alpha = if (sel) 0.9f else 0.2f), RoundedCornerShape(10.dp))
                                            .clickable { draftTrack = tr.id }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(tr.name, style = MaterialTheme.typography.labelSmall, color = FrostingWhite)
                                    }
                                }
                            }
                        }

                        CandyButton(
                            text = if (isBusy) "CREATING ROOM..." else "CREATE ONLINE ROOM",
                            onClick = { onCreateRoom(draftTeamSize, draftMode, draftTrack, draftFillAi, draftIsHub) },
                            testTag = "create_room_button",
                            icon = Icons.Filled.Groups,
                            topColor = MintCream,
                            bottomColor = MintDark,
                            textColor = BerryDeep,
                            height = 48.dp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Join by Room Code
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.uppercase().take(12) },
                                placeholder = { Text("Room Code (e.g. CANDYPLAZA)", style = MaterialTheme.typography.labelSmall) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = FrostingWhite,
                                    unfocusedTextColor = FrostingWhite,
                                    focusedBorderColor = MintCream,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("room_code_input")
                            )
                            CandyButton(
                                text = "JOIN",
                                onClick = { onJoinRoom(joinCodeInput) },
                                testTag = "join_room_button",
                                topColor = CaramelGold,
                                bottomColor = Butterscotch,
                                textColor = BerryDeep,
                                height = 50.dp,
                                modifier = Modifier.width(104.dp)
                            )
                        }
                    } else {
                        // ACTIVE ROOM VIEW
                        val roomTeamSize = room.teamSize.toInt().coerceIn(1, 4)
                        val numTeams = (8 / roomTeamSize).coerceIn(1, 4)
                        val roomTrack = room.parsedTrackId()
                        val roomMode = room.parsedMode()
                        val roomIsHub = room.isGatheringHub()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BerryDeep)
                                    .border(1.5.dp, CaramelGold, RoundedCornerShape(14.dp))
                                    .clickable { clipboard.setText(AnnotatedString(room.roomCode)) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (roomIsHub) "HUB #${room.roomCode}" else "ROOM #${room.roomCode}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CaramelGold
                                )
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Room Code", tint = FrostingWhite, modifier = Modifier.size(16.dp))
                            }

                            Text(
                                text = "${players.size}/8 RACERS • TEAM $roomTeamSize",
                                style = MaterialTheme.typography.labelLarge,
                                color = MintCream
                            )
                        }

                        // Host Configuration Bar
                        if (isHost) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(1, 2, 4).forEach { sz ->
                                    val sel = roomTeamSize == sz
                                    Box(
                                        modifier = Modifier
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (sel) CaramelGold else BerryCard)
                                            .clickable { onUpdateRoomConfig(roomMode, roomTrack, sz, room.fillWithAi, roomIsHub) }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${sz}P", style = MaterialTheme.typography.labelSmall, color = if (sel) BerryDeep else FrostingWhite)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BerryCard)
                                        .clickable {
                                            when {
                                                roomIsHub -> onUpdateRoomConfig(GameMode.QUICK_RACE, roomTrack, roomTeamSize, room.fillWithAi, false)
                                                roomMode == GameMode.FREE_ROAM -> onUpdateRoomConfig(GameMode.FREE_ROAM, roomTrack, roomTeamSize, room.fillWithAi, true)
                                                else -> onUpdateRoomConfig(GameMode.FREE_ROAM, roomTrack, roomTeamSize, room.fillWithAi, false)
                                            }
                                        }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when {
                                            roomIsHub -> "GATHERING HUB"
                                            roomMode == GameMode.FREE_ROAM -> "OPEN WORLD"
                                            else -> CatalogData.trackById(roomTrack).name
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkyCandy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (roomMode == GameMode.QUICK_RACE && !roomIsHub) {
                                    Box(
                                        modifier = Modifier
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CandyPink)
                                            .clickable {
                                                val allTracks = TrackId.entries
                                                val nextTrack = allTracks[(roomTrack.ordinal + 1) % allTracks.size]
                                                onUpdateRoomConfig(roomMode, nextTrack, roomTeamSize, room.fillWithAi, false)
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("TRACK >", style = MaterialTheme.typography.labelSmall, color = FrostingWhite)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = when {
                                    roomIsHub -> "Mode: Gathering Hub (لوبي التجمع)"
                                    roomMode == GameMode.FREE_ROAM -> "Mode: Open World Kingdom"
                                    else -> "Track: ${CatalogData.trackById(roomTrack).name} • AI Bots: ${if (room.fillWithAi) "ON" else "OFF"}"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = SkyCandy
                            )
                        }

                        // Squad Teams Roster Grid
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val teamIndices = (0 until numTeams).toList()
                            items(teamIndices) { teamIdx ->
                                val team = SquadTeam.fromIndex(teamIdx)
                                val teamMembers = players.filter { it.teamIndex.toInt() == teamIdx }
                                val teamColor = Color(team.colorHex)
                                val isMyTeam = localTeamIdx == teamIdx
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(BerryDeep.copy(alpha = if (isMyTeam) 0.88f else 0.56f))
                                        .border(
                                            if (isMyTeam) 2.dp else 1.dp,
                                            if (isMyTeam) teamColor else Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onSelectTeamAndReady(teamIdx, localReady) }
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (roomTeamSize == 1) "SLOT ${teamIdx + 1}" else "${team.teamName} (${teamMembers.size}/$roomTeamSize)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = teamColor
                                        )
                                        Text(
                                            text = if (isMyTeam) "YOUR SQUAD" else "TAP TO JOIN TEAM",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = FrostingWhite.copy(alpha = 0.72f)
                                        )
                                    }
                                    if (teamMembers.isEmpty()) {
                                        Text(
                                            text = if (room.fillWithAi) "AI Teammate Slot" else "Open Slot",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = FrostingWhite.copy(alpha = 0.45f)
                                        )
                                    } else {
                                        teamMembers.forEach { member ->
                                            val kartName = CatalogData.kartById(member.kartId).name
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${member.displayName} • $kartName",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = FrostingWhite,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = if (member.userId == room.hostUid) "HOST" else if (member.ready) "READY" else "WAITING",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (member.ready || member.userId == room.hostUid) MintCream else CaramelGold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Action Footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CandyButton(
                                text = "LEAVE",
                                onClick = onLeaveRoom,
                                testTag = "room_leave_button",
                                topColor = StrawberryRed,
                                bottomColor = CandyPinkDark,
                                height = 48.dp,
                                modifier = Modifier.width(96.dp)
                            )
                            CandyButton(
                                text = if (localReady) "READY ✓" else "READY UP",
                                onClick = { onSelectTeamAndReady(localTeamIdx, !localReady) },
                                testTag = "room_ready_button",
                                icon = Icons.Filled.Check,
                                topColor = if (localReady) MintCream else GrapeJelly,
                                bottomColor = if (localReady) MintDark else Color(0xFF4527A0),
                                textColor = if (localReady) BerryDeep else FrostingWhite,
                                height = 48.dp,
                                modifier = Modifier.weight(1f)
                            )
                            if (isHost) {
                                CandyButton(
                                    text = if (roomIsHub) "ENTER WORLD" else "START!",
                                    onClick = onStartMatch,
                                    testTag = "room_start_button",
                                    icon = Icons.Filled.PlayArrow,
                                    topColor = CaramelGold,
                                    bottomColor = Butterscotch,
                                    textColor = BerryDeep,
                                    height = 48.dp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // RIGHT PANE: 3 Tabs -> CHAT & VOICE | FRIENDS | REQUESTS
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(BerryPurple.copy(alpha = 0.92f))
                        .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 3-Tab Bar + Voice Speaker Mute
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val reqBadge = if (incomingPendingRequests.isNotEmpty()) " (${incomingPendingRequests.size})" else ""
                        val tabs = listOf(
                            "CHAT",
                            "FRIENDS (${friends.size})",
                            "REQ$reqBadge"
                        )
                        tabs.forEachIndexed { idx, label ->
                            val sel = rightTab == idx
                            Box(
                                modifier = Modifier
                                    .testTag("lobby_tab_$idx")
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            sel -> CandyPink
                                            idx == 2 && incomingPendingRequests.isNotEmpty() -> CaramelGold.copy(alpha = 0.85f)
                                            else -> BerryCard
                                        }
                                    )
                                    .clickable { rightTab = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (!sel && idx == 2 && incomingPendingRequests.isNotEmpty()) BerryDeep else FrostingWhite,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                        IconButton(
                            onClick = onToggleVoiceMute,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isVoiceMuted) StrawberryRed else MintDark)
                        ) {
                            Icon(
                                imageVector = if (isVoiceMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Toggle Voice Audio",
                                tint = FrostingWhite
                            )
                        }
                    }

                    when (rightTab) {
                        0 -> {
                            // CHAT & OPEN-MIC VOICE TAB
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (!teamChannelOnly) MintCream else BerryCard)
                                            .clickable { teamChannelOnly = false }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("ALL ROOM", style = MaterialTheme.typography.labelSmall, color = if (!teamChannelOnly) BerryDeep else FrostingWhite)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (teamChannelOnly) CaramelGold else BerryCard)
                                            .clickable { teamChannelOnly = true }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("TEAM ONLY", style = MaterialTheme.typography.labelSmall, color = if (teamChannelOnly) BerryDeep else FrostingWhite)
                                    }
                                }
                                Text(
                                    text = if (isRecordingVoice) "MIC LIVE (TAP TO OFF)" else "Tap MIC to toggle ON/OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isRecordingVoice) CaramelGold else SkyCandy
                                )
                            }

                            if (room != null) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val callouts = listOf("Let's go!", "Watch out!", "Boost ready!", "هيا بنا!", "انتبه!")
                                    items(callouts) { phrase ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(GrapeJelly.copy(alpha = 0.85f))
                                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
                                                .clickable { onSendQuickCallout(phrase, teamChannelOnly) }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(phrase, style = MaterialTheme.typography.labelSmall, color = FrostingWhite)
                                        }
                                    }
                                }
                            }

                            val visibleMessages = messages.filter { msg ->
                                !msg.targetTeamOnly || msg.teamIndex.toInt() == localTeamIdx || msg.senderUid == currentUserId
                            }
                            LazyColumn(
                                state = msgListState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BerryDeep.copy(alpha = 0.68f))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (room == null) {
                                    item {
                                        Text(
                                            text = "Join a room or Gathering Hub on the left to chat and talk live with your squad!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = FrostingWhite.copy(alpha = 0.65f)
                                        )
                                    }
                                } else {
                                    items(visibleMessages, key = { it.id.ifBlank { it.clientSeq.toString() } }) { msg ->
                                        val senderTeamColor = Color(SquadTeam.fromIndex(msg.teamIndex.toInt()).colorHex)
                                        val prefix = if (msg.targetTeamOnly) "[TEAM] " else ""
                                        val isVoice = msg.msgType == "VOICE"
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isVoice) MintDark.copy(alpha = 0.35f) else BerryCard.copy(alpha = 0.72f))
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (isVoice) {
                                                Icon(Icons.Filled.Mic, contentDescription = null, tint = MintCream, modifier = Modifier.size(16.dp))
                                            }
                                            Text(
                                                text = "$prefix${msg.senderName}: ${msg.text}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (msg.senderUid == currentUserId) CaramelGold else senderTeamColor
                                            )
                                        }
                                    }
                                }
                            }

                            // Chat Input + Single-Tap Toggle Open Mic Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = chatInput,
                                    onValueChange = { chatInput = it.take(200) },
                                    enabled = room != null,
                                    placeholder = { Text("Message squad...", style = MaterialTheme.typography.labelSmall) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = FrostingWhite,
                                        unfocusedTextColor = FrostingWhite,
                                        focusedBorderColor = CandyPink,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        onSendChat(chatInput, teamChannelOnly)
                                        chatInput = ""
                                    },
                                    enabled = room != null && chatInput.isNotBlank(),
                                    modifier = Modifier
                                        .testTag("chat_send_button")
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(CandyPink)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Chat", tint = FrostingWhite)
                                }
                                IconButton(
                                    onClick = {
                                        if (!hasMicPerm) {
                                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else if (room != null) {
                                            onToggleLiveMic(teamChannelOnly)
                                        }
                                    },
                                    modifier = Modifier
                                        .testTag("lobby_mic_toggle_button")
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (isRecordingVoice) StrawberryRed else MintDark)
                                        .border(2.dp, if (isRecordingVoice) CaramelGold else Color.White, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isRecordingVoice) Icons.Filled.Mic else Icons.Filled.MicOff,
                                        contentDescription = if (isRecordingVoice) "Turn Mic Off" else "Turn Mic On",
                                        tint = FrostingWhite
                                    )
                                }
                            }
                        }

                        1 -> {
                            // FRIENDS ROSTER TAB (With 1-Tap Invite!)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BerryDeep)
                                    .border(1.dp, CaramelGold.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                                    .clickable { clipboard.setText(AnnotatedString(friendCode)) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("YOUR RACER ID (TAP TO COPY)", style = MaterialTheme.typography.labelSmall, color = SkyCandy)
                                    Text(friendCode, style = MaterialTheme.typography.titleMedium, color = CaramelGold)
                                }
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Friend ID", tint = FrostingWhite)
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (friends.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No friends yet. Open the REQ tab to send a Friend Request by ID (like PUBG)!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = FrostingWhite.copy(alpha = 0.68f)
                                        )
                                    }
                                } else {
                                    items(friends, key = { it.friendUid }) { f ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(BerryDeep.copy(alpha = 0.78f))
                                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${f.displayName} (LVL ${f.level})", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                                                Text("ID: ${f.friendCode} • ${CatalogData.kartById(f.selectedKartId).name}", style = MaterialTheme.typography.labelSmall, color = MintCream)
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .testTag("invite_friend_${f.friendUid}")
                                                        .height(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (room != null) MintCream else BerryCard)
                                                        .clickable(enabled = room != null) { onInviteFriend(f) }
                                                        .padding(horizontal = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Mail,
                                                            contentDescription = "Invite Friend",
                                                            tint = if (room != null) BerryDeep else FrostingWhite.copy(alpha = 0.4f),
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Text(
                                                            text = "INVITE",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (room != null) BerryDeep else FrostingWhite.copy(alpha = 0.4f),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                IconButton(onClick = { onRemoveFriend(f.friendUid) }) {
                                                    Icon(Icons.Filled.Delete, contentDescription = "Remove Friend", tint = CandyPink)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // PUBG-STYLE FRIEND REQUESTS TAB (Send by ID + Incoming Accept/Decline + Sent Status)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = friendCodeInput,
                                    onValueChange = { friendCodeInput = it.uppercase().take(12) },
                                    placeholder = { Text("Enter Racer ID (e.g. SR4K9P)", style = MaterialTheme.typography.labelSmall) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = FrostingWhite,
                                        unfocusedTextColor = FrostingWhite,
                                        focusedBorderColor = MintCream,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("friend_id_input")
                                )
                                IconButton(
                                    onClick = {
                                        onSendFriendRequest(friendCodeInput)
                                        friendCodeInput = ""
                                    },
                                    modifier = Modifier
                                        .testTag("send_friend_request_button")
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MintDark)
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = "Send Friend Request", tint = FrostingWhite)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (incomingPendingRequests.isNotEmpty()) {
                                    item {
                                        Text("INCOMING REQUESTS (طلبات الصداقة)", style = MaterialTheme.typography.labelSmall, color = CaramelGold, fontWeight = FontWeight.Bold)
                                    }
                                    items(incomingPendingRequests, key = { "in_${it.id}" }) { req ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(BerryDeep.copy(alpha = 0.85f))
                                                .border(1.5.dp, CaramelGold.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${req.fromName} (LVL ${req.fromLevel})", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                                                Text("ID: ${req.fromFriendCode}", style = MaterialTheme.typography.labelSmall, color = SkyCandy)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                IconButton(
                                                    onClick = { onRespondFriendRequest(req, true) },
                                                    modifier = Modifier
                                                        .testTag("accept_friend_${req.id}")
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(MintCream)
                                                ) {
                                                    Icon(Icons.Filled.Check, contentDescription = "Accept Request", tint = BerryDeep)
                                                }
                                                IconButton(
                                                    onClick = { onRespondFriendRequest(req, false) },
                                                    modifier = Modifier
                                                        .testTag("decline_friend_${req.id}")
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(StrawberryRed)
                                                ) {
                                                    Icon(Icons.Filled.Close, contentDescription = "Decline Request", tint = FrostingWhite)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (outgoingRequests.isNotEmpty()) {
                                    item {
                                        Text("SENT REQUESTS", style = MaterialTheme.typography.labelSmall, color = SkyCandy, fontWeight = FontWeight.Bold)
                                    }
                                    items(outgoingRequests, key = { "out_${it.id}" }) { req ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(BerryDeep.copy(alpha = 0.65f))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${req.toName} (${req.toFriendCode})", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                                                Text("Status: ${req.status}", style = MaterialTheme.typography.labelSmall, color = if (req.status == "PENDING") CaramelGold else CandyPink)
                                            }
                                            IconButton(onClick = { onDismissFriendRequest(req.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Cancel Request", tint = FrostingWhite.copy(alpha = 0.6f))
                                            }
                                        }
                                    }
                                }

                                if (incomingPendingRequests.isEmpty() && outgoingRequests.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No pending friend requests. Enter your friend's Racer ID above to send a request!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = FrostingWhite.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
