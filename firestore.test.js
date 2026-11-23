const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    displayName: "Flipper",
    bestScore: 12,
    bestStreak: 4,
    totalRuns: 0,
    totalPerfects: 0,
    selectedSkinId: "penis_classic",
    selectedThemeId: "sunny_home",
    controlModeId: "classic_double_tap",
    soundEnabled: true,
    hapticsEnabled: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validRunPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    score: 25,
    maxStreak: 6,
    perfectCount: 5,
    jumpsCompleted: 14,
    skinId: "water_bottle",
    themeId: "sunset_arcade",
    controlModeId: "classic_double_tap",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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

test("Unauthenticated user: denied all reads and writes on users and runs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("runs").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("runs")
      .doc("run_1")
      .set(validRunPayload(ALICE_UID))
  );
});

test("Authenticated owner: valid UserProfile create, get, and partial updates succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  // Create mirrors GameRepository.ensureUserProfile
  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(profileRef.get());

  // Collection-level list on /users must remain denied
  await assertFails(aliceDb.collection("users").get());

  // Preferences partial update mirrors GameRepository.syncPreferences
  await assertSucceeds(
    profileRef.update({
      displayName: "Alice Flipper",
      selectedSkinId: "trophy_bottle",
      selectedThemeId: "midnight_neon",
      controlModeId: "power_bar",
      soundEnabled: false,
      hapticsEnabled: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Stats partial update mirrors GameRepository.updateBestStats / recordCompletedRun
  await assertSucceeds(
    profileRef.update({
      bestScore: 48,
      bestStreak: 9,
      totalRuns: 1,
      totalPerfects: 6,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Security bounds: UserProfile rejects ghost fields, id field, immutable edits, stat decreases, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  // Reject extra @DocumentId `id` field on create
  await assertFails(
    profileRef.set(validProfilePayload(ALICE_UID, { id: ALICE_UID }))
  );

  // Reject ghost admin field on create
  await assertFails(
    profileRef.set(validProfilePayload(ALICE_UID, { isAdmin: true }))
  );

  // Reject future timestamp
  await assertFails(
    profileRef.set(
      validProfilePayload(ALICE_UID, {
        createdAt: new Date(Date.now() + 60000),
      })
    )
  );

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));

  // Reject immutable createdAt mutation
  await assertFails(
    profileRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject stat decrease
  await assertFails(
    profileRef.update({
      bestScore: 1,
      bestStreak: 4,
      totalRuns: 0,
      totalPerfects: 0,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Reject mixed un-allowlisted ghost field on update
  await assertFails(
    profileRef.update({
      selectedSkinId: "water_bottle",
      shadowFlag: true,
      updatedAt: new Date(Date.now() - 300),
    })
  );
});

test("Cross-user isolation: Bob cannot read, write, or delete Alice's profile or runs", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validProfilePayload(ALICE_UID));
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("runs")
      .doc("run_alice")
      .set(validRunPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).delete());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("runs").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("runs")
      .doc("run_alice")
      .delete()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(BOB_UID)
      .set(validProfilePayload(ALICE_UID))
  );
});

test("Authenticated owner: FlipRun create (with/without failReason), Kotlin orderBy query, immutability, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const runsCol = aliceDb.collection("users").doc(ALICE_UID).collection("runs");

  // Create without optional failReason (mirrors Kotlin .filterValues { it != null })
  await assertSucceeds(runsCol.doc("run_1").set(validRunPayload(ALICE_UID)));

  // Create with optional failReason
  await assertSucceeds(
    runsCol.doc("run_2").set(
      validRunPayload(ALICE_UID, {
        failReason: "Overshot the TV stand!",
      })
    )
  );

  // Reject extra @DocumentId `id` field
  await assertFails(
    runsCol.doc("run_bad_id").set(validRunPayload(ALICE_UID, { id: "run_bad_id" }))
  );

  // Reject oversized failReason (> 120 chars)
  await assertFails(
    runsCol.doc("run_long_reason").set(
      validRunPayload(ALICE_UID, {
        failReason: "x".repeat(121),
      })
    )
  );

  // Exact Kotlin query: orderBy("createdAt", DESCENDING).limit(20)
  await assertSucceeds(runsCol.orderBy("createdAt", "desc").limit(20).get());

  // Runs are immutable once recorded
  await assertFails(
    runsCol.doc("run_1").update({
      score: 999999,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Owner can delete a run from history
  await assertSucceeds(runsCol.doc("run_1").delete());
});
