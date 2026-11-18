package com.example.sweetrush.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    isBusy: Boolean,
    isRecordingVoice: Boolean,
    isVoiceMuted: Boolean,
    statusMessage: String?,
    onCreateRoom: (teamSize: Int, mode: GameMode, trackId: TrackId, fillWithAi: Boolean) -> Unit,
    onJoinRoom: (roomCode: String) -> Unit,
    onLeaveRoom: () -> Unit,
    onUpdateRoomConfig: (mode: GameMode, trackId: TrackId, teamSize: Int, fillWithAi: Boolean) -> Unit,
    onSelectTeamAndReady: (teamIndex: Int, ready: Boolean) -> Unit,
    onStartMatch: () -> Unit,
    onAddFriend: (friendCode: String) -> Unit,
    onRemoveFriend: (friendUid: String) -> Unit,
    onSendChat: (text: String, teamOnly: Boolean) -> Unit,
    onSendQuickCallout: (text: String, teamOnly: Boolean) -> Unit,
    onStartVoiceRecording: (teamOnly: Boolean) -> Unit,
    onStopVoiceRecording: (teamOnly: Boolean) -> Unit,
    onToggleVoiceMute: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var draftTeamSize by remember { mutableIntStateOf(2) }
    var draftMode by remember { mutableStateOf(GameMode.QUICK_RACE) }
    var draftTrack by remember { mutableStateOf(TrackId.PEPPERMINT_PLAZA) }
    var draftFillAi by remember { mutableStateOf(true) }
    var joinCodeInput by remember { mutableStateOf("") }

    var rightTab by remember { mutableIntStateOf(0) } // 0 = Chat/Voice, 1 = Friends
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
    }

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
            .background(BerryDeep.copy(alpha = 0.86f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CandyScreenHeader(
                title = "Online Squads & Voice",
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
                // LEFT PANE: Create / Join OR Active Squad Room (1 / 2 / 4 Players Per Team)
                Column(
                    modifier = Modifier
                        .weight(1.22f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(BerryPurple.copy(alpha = 0.92f))
                        .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (room == null) {
                        Text("CREATE OR JOIN SQUAD ROOM", style = MaterialTheme.typography.titleLarge, color = CaramelGold)

                        // Squad Size Selector: 1, 2, or 4 Players per Team!
                        Text("TEAM SIZE (فرق من 1 أو 2 أو 4)", style = MaterialTheme.typography.labelSmall, color = SkyCandy)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1 to "SOLO (1)", 2 to "DUO (2)", 4 to "QUAD (4)").forEach { (sz, label) ->
                                val selected = draftTeamSize == sz
                                Box(
                                    modifier = Modifier
                                        .testTag("team_size_$sz")
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) CaramelGold else BerryCard)
                                        .border(1.5.dp, if (selected) Color.White else Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
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

                        // Mode & AI Fill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(GameMode.QUICK_RACE to "RACE", GameMode.FREE_ROAM to "OPEN WORLD").forEach { (m, label) ->
                                val sel = draftMode == m
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) MintCream else BerryCard)
                                        .clickable { draftMode = m },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (sel) BerryDeep else FrostingWhite)
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("AI Bots", style = MaterialTheme.typography.labelSmall, color = FrostingWhite)
                                Switch(
                                    checked = draftFillAi,
                                    onCheckedChange = { draftFillAi = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MintCream, checkedTrackColor = MintDark)
                                )
                            }
                        }

                        // Track Chips (when Race mode selected)
                        if (draftMode == GameMode.QUICK_RACE) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(CatalogData.tracks) { tr ->
                                    val sel = draftTrack == tr.id
                                    Box(
                                        modifier = Modifier
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (sel) CandyPink else BerryCard)
                                            .border(1.dp, Color.White.copy(alpha = if (sel) 0.9f else 0.2f), RoundedCornerShape(12.dp))
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
                            onClick = { onCreateRoom(draftTeamSize, draftMode, draftTrack, draftFillAi) },
                            testTag = "create_room_button",
                            icon = Icons.Filled.Groups,
                            topColor = MintCream,
                            bottomColor = MintDark,
                            textColor = BerryDeep,
                            height = 48.dp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(2.dp))

                        // Join by Room Code
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.uppercase().take(8) },
                                placeholder = { Text("Enter Room Code (e.g. X7K9MP)", style = MaterialTheme.typography.labelSmall) },
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
                                height = 52.dp,
                                modifier = Modifier.width(110.dp)
                            )
                        }
                    } else {
                        // ACTIVE ROOM VIEW
                        val roomTeamSize = room.teamSize.toInt().coerceIn(1, 4)
                        val numTeams = (8 / roomTeamSize).coerceIn(1, 4)
                        val roomTrack = room.parsedTrackId()
                        val roomMode = room.parsedMode()

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
                                Text("ROOM #${room.roomCode}", style = MaterialTheme.typography.titleMedium, color = CaramelGold)
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
                                            .clickable { onUpdateRoomConfig(roomMode, roomTrack, sz, room.fillWithAi) }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${sz}P Team", style = MaterialTheme.typography.labelSmall, color = if (sel) BerryDeep else FrostingWhite)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BerryCard)
                                        .clickable {
                                            val nextMode = if (roomMode == GameMode.QUICK_RACE) GameMode.FREE_ROAM else GameMode.QUICK_RACE
                                            onUpdateRoomConfig(nextMode, roomTrack, roomTeamSize, room.fillWithAi)
                                        }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (roomMode == GameMode.FREE_ROAM) "OPEN WORLD" else CatalogData.trackById(roomTrack).name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkyCandy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (roomMode == GameMode.QUICK_RACE) {
                                    Box(
                                        modifier = Modifier
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CandyPink)
                                            .clickable {
                                                val allTracks = TrackId.entries
                                                val nextTrack = allTracks[(roomTrack.ordinal + 1) % allTracks.size]
                                                onUpdateRoomConfig(roomMode, nextTrack, roomTeamSize, room.fillWithAi)
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
                                text = if (roomMode == GameMode.FREE_ROAM) "Mode: Open World Kingdom"
                                else "Track: ${CatalogData.trackById(roomTrack).name} • AI Bots: ${if (room.fillWithAi) "ON" else "OFF"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = SkyCandy
                            )
                        }

                        // Squad Teams Roster Grid (Tap any Squad Team card to switch teams!)
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

                        // Action Footer: Leave / Ready / Start Match
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
                                modifier = Modifier.width(100.dp)
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
                                    text = "START!",
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

                // RIGHT PANE: Tabbed Voice+Text Chat AND Friends Directory
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
                    // Tab Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("CHAT & VOICE", "FRIENDS (${friends.size})").forEachIndexed { idx, label ->
                            val sel = rightTab == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) CandyPink else BerryCard)
                                    .clickable { rightTab = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
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

                    if (rightTab == 0) {
                        // Channel Toggle: All vs Squad Team Only
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
                            if (room != null) {
                                Text("Hold MIC to talk!", style = MaterialTheme.typography.labelSmall, color = SkyCandy)
                            }
                        }

                        // Quick Radio Callouts
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

                        // Messages Stream
                        val visibleMessages = messages.filter { msg ->
                            !msg.targetTeamOnly || msg.teamIndex.toInt() == localTeamIdx || msg.senderUid == currentUserId
                        }
                        LazyColumn(
                            state = msgListState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(BerryDeep.copy(alpha = 0.65f))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (room == null) {
                                item {
                                    Text(
                                        text = "Create or join a room on the left to chat and talk live with your squad!",
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

                        // Chat Input + Hold-to-Talk Voice Button
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
                            Box(
                                modifier = Modifier
                                    .testTag("lobby_ptt_button")
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecordingVoice) StrawberryRed else MintDark)
                                    .border(2.dp, if (isRecordingVoice) CaramelGold else Color.White, CircleShape)
                                    .pointerInput(room?.roomCode, teamChannelOnly, hasMicPerm) {
                                        detectTapGestures(
                                            onPress = {
                                                if (!hasMicPerm) {
                                                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                    return@detectTapGestures
                                                }
                                                if (room == null) return@detectTapGestures
                                                onStartVoiceRecording(teamChannelOnly)
                                                tryAwaitRelease()
                                                onStopVoiceRecording(teamChannelOnly)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = "Hold to Talk", tint = FrostingWhite)
                            }
                        }
                    } else {
                        // FRIENDS TAB
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
                                Text("YOUR FRIEND CODE (TAP TO COPY)", style = MaterialTheme.typography.labelSmall, color = SkyCandy)
                                Text(friendCode, style = MaterialTheme.typography.titleMedium, color = CaramelGold)
                            }
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Friend Code", tint = FrostingWhite)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = friendCodeInput,
                                onValueChange = { friendCodeInput = it.uppercase().take(12) },
                                placeholder = { Text("Friend code (e.g. SR4K9P)", style = MaterialTheme.typography.labelSmall) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = FrostingWhite,
                                    unfocusedTextColor = FrostingWhite,
                                    focusedBorderColor = MintCream,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    onAddFriend(friendCodeInput)
                                    friendCodeInput = ""
                                },
                                modifier = Modifier
                                    .testTag("add_friend_button")
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MintDark)
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = "Add Friend", tint = FrostingWhite)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (friends.isEmpty()) {
                                item {
                                    Text(
                                        text = "No friends added yet. Share your Friend Code above or enter a friend's code!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = FrostingWhite.copy(alpha = 0.65f)
                                    )
                                }
                            } else {
                                items(friends, key = { it.friendUid }) { f ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(BerryDeep.copy(alpha = 0.72f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${f.displayName} (LVL ${f.level})", style = MaterialTheme.typography.labelLarge, color = FrostingWhite)
                                            Text("Code: ${f.friendCode} • ${CatalogData.kartById(f.selectedKartId).name}", style = MaterialTheme.typography.labelSmall, color = MintCream)
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
            }
        }
    }
}
