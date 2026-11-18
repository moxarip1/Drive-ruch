const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const EVE_UID = "eve_uid_003";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are blocked from all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("rooms").doc("ROOM1").get());
  await assertFails(unauthDb.collection("rooms").get());
  await assertFails(unauthDb.collection("racerDirectory").doc("SRALIC1").get());
});

test("User profile enforces strict owner isolation, schema keys, and immutable createdAt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Candy",
      friendCode: "SRALIC1",
      level: 3,
      xp: 45,
      credits: 500,
      racesWon: 2,
      racesCompleted: 4,
      selectedKartId: "strawberry_rocket",
      selectedRacerId: "berry_blitz",
      profileJson: "{\"level\":3}",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial update preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      credits: 650,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot read or update Alice's private profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ credits: 9999, updatedAt: new Date(Date.now() - 200) }));

  // Shadow update with ghost field & future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      credits: 700,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Racer directory allows friend-code lookup by get while blocking unfiltered scraping", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("racerDirectory").doc("SRALIC1").set({
      userId: ALICE_UID,
      displayName: "Alice Candy",
      friendCode: "SRALIC1",
      selectedKartId: "strawberry_rocket",
      selectedRacerId: "berry_blitz",
      level: 3,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(bobDb.collection("racerDirectory").doc("SRALIC1").get());
  await assertFails(bobDb.collection("racerDirectory").get());
  await assertSucceeds(aliceDb.collection("racerDirectory").where("userId", "==", ALICE_UID).get());
});

test("Rooms support teams of 1, 2, or 4, friend joining, telemetry, and voice/text chat", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  const past = new Date(Date.now() - 2000);

  // Create room with teamSize = 2
  await assertSucceeds(
    aliceDb.collection("rooms").doc("CANDY2").set({
      hostUid: ALICE_UID,
      roomCode: "CANDY2",
      roomName: "Candy Squad Duo",
      status: "WAITING",
      mode: "QUICK_RACE",
      trackId: "PEPPERMINT_PLAZA",
      teamSize: 2,
      maxPlayers: 8,
      fillWithAi: true,
      memberUids: [ALICE_UID],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Invalid teamSize = 3 must fail
  await assertFails(
    aliceDb.collection("rooms").doc("BADTEAM").set({
      hostUid: ALICE_UID,
      roomCode: "BADTEAM",
      roomName: "Bad Team",
      status: "WAITING",
      mode: "QUICK_RACE",
      trackId: "PEPPERMINT_PLAZA",
      teamSize: 3,
      maxPlayers: 8,
      fillWithAi: true,
      memberUids: [ALICE_UID],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob joins room CANDY2
  await assertSucceeds(
    bobDb.collection("rooms").doc("CANDY2").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Filtered member query succeeds; unfiltered query fails
  await assertSucceeds(aliceDb.collection("rooms").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(eveDb.collection("rooms").get());

  // Alice and Bob register player docs & telemetry
  const alicePlayerPayload = {
    userId: ALICE_UID,
    displayName: "Alice Candy",
    racerId: "berry_blitz",
    kartId: "strawberry_rocket",
    paintId: "default",
    wheelId: "PEPPERMINT_SWIRL",
    exhaustId: "SUGAR_SPARK",
    decoId: "NONE",
    teamIndex: 0,
    ready: true,
    x: 0.0,
    y: 0.0,
    z: 0.0,
    yawRad: 0.0,
    speed: 32.5,
    steerVisualRad: 0.1,
    lap: 1,
    raceScore: 120.0,
    finished: false,
    finishTimeMs: 0,
    drifting: false,
    nitroActive: true,
    visibleToUids: [ALICE_UID, BOB_UID],
    createdAt: past,
    updatedAt: past,
  };
  await assertSucceeds(aliceDb.collection("rooms").doc("CANDY2").collection("players").doc(ALICE_UID).set(alicePlayerPayload));

  // Bob queries visible players; Eve (non-member) cannot read
  await assertSucceeds(
    bobDb.collection("rooms").doc("CANDY2").collection("players").where("visibleToUids", "array-contains", BOB_UID).get()
  );
  await assertFails(eveDb.collection("rooms").doc("CANDY2").collection("players").doc(ALICE_UID).get());

  // Text & Voice message creation
  await assertSucceeds(
    aliceDb.collection("rooms").doc("CANDY2").collection("messages").doc("msg_1").set({
      senderUid: ALICE_UID,
      senderName: "Alice Candy",
      teamIndex: 0,
      targetTeamOnly: true,
      msgType: "VOICE",
      text: "Voice message (1.4s)",
      audioB64: "AAAA",
      durationMs: 1400,
      clientSeq: 1700000000001,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    bobDb.collection("rooms").doc("CANDY2").collection("messages").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(
    eveDb.collection("rooms").doc("CANDY2").collection("messages").doc("msg_1").get()
  );
});

test("PUBG-style friendRequests enforce sender/recipient approvals and participant isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  const past = new Date(Date.now() - 2000);
  const reqId = `${ALICE_UID}_${BOB_UID}`;

  // Alice sends friend request to Bob
  await assertSucceeds(
    aliceDb.collection("friendRequests").doc(reqId).set({
      fromUid: ALICE_UID,
      fromName: "Alice Candy",
      fromFriendCode: "SRALIC1",
      fromKartId: "strawberry_rocket",
      fromRacerId: "berry_blitz",
      fromLevel: 3,
      toUid: BOB_UID,
      toName: "Bob Sugar",
      toFriendCode: "SRBOB22",
      toKartId: "choco_thunder",
      toRacerId: "cocoa_king",
      toLevel: 4,
      status: "PENDING",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Alice cannot self-accept her own outgoing request
  await assertFails(
    aliceDb.collection("friendRequests").doc(reqId).update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob queries incoming/participant requests; Eve cannot read or scrape
  await assertSucceeds(
    bobDb.collection("friendRequests").where("participantUids", "array-contains", BOB_UID).get()
  );
  await assertFails(eveDb.collection("friendRequests").doc(reqId).get());
  await assertFails(eveDb.collection("friendRequests").get());

  // Bob accepts friend request
  await assertSucceeds(
    bobDb.collection("friendRequests").doc(reqId).update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Alice deletes/cleans up synced request
  await assertSucceeds(aliceDb.collection("friendRequests").doc(reqId).delete());
});

test("GATHERING_HUB rooms and gameInvites work cleanly with strict access control", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  const past = new Date(Date.now() - 2000);

  // Alice creates a GATHERING_HUB room
  await assertSucceeds(
    aliceDb.collection("rooms").doc("CANDYPLAZA").set({
      hostUid: ALICE_UID,
      roomCode: "CANDYPLAZA",
      roomName: "Candy City Plaza",
      status: "WAITING",
      mode: "GATHERING_HUB",
      trackId: "PEPPERMINT_PLAZA",
      teamSize: 4,
      maxPlayers: 8,
      fillWithAi: true,
      memberUids: [ALICE_UID],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Alice sends game invite to Bob
  const invId = `${ALICE_UID}_${BOB_UID}_CANDYPLAZA`;
  await assertSucceeds(
    aliceDb.collection("gameInvites").doc(invId).set({
      fromUid: ALICE_UID,
      fromName: "Alice Candy",
      toUid: BOB_UID,
      roomCode: "CANDYPLAZA",
      roomName: "Candy City Plaza",
      mode: "GATHERING_HUB",
      trackId: "PEPPERMINT_PLAZA",
      teamSize: 4,
      status: "PENDING",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob reads his invites and accepts
  await assertSucceeds(
    bobDb.collection("gameInvites").where("participantUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("gameInvites").doc(invId).update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Eve cannot access Bob's invite
  await assertFails(eveDb.collection("gameInvites").doc(invId).get());
  await assertFails(eveDb.collection("gameInvites").get());

  // Bob joins GATHERING_HUB room, Alice leaves cleanly leaving Bob inside
  await assertSucceeds(
    bobDb.collection("rooms").doc("CANDYPLAZA").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 150),
    })
  );
  await assertSucceeds(
    aliceDb.collection("rooms").doc("CANDYPLAZA").update({
      memberUids: [BOB_UID],
      updatedAt: new Date(Date.now() - 100),
    })
  );
  // Last remaining member Bob can delete room when leaving
  await assertSucceeds(bobDb.collection("rooms").doc("CANDYPLAZA").delete());
});

