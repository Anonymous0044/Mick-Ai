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

test("Unauthenticated user cannot read sessions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("sessions").get());
});

test("Alice can create and read her own session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const sessionRef = aliceDb.collection("sessions").doc("sess_1");
  const now = new Date();
  await assertSucceeds(
    sessionRef.set({
      userId: ALICE_UID,
      title: "Story Brainstorming",
      category: "story",
      pinned: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(sessionRef.get());
});

test("Bob cannot read Alice's session", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("sessions").doc("sess_1").set({
      userId: ALICE_UID,
      title: "Alice Secret Story",
      category: "story",
      pinned: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("sessions").doc("sess_1").get());
});

test("Alice can create and read her own sparks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const sparkRef = aliceDb.collection("sparks").doc("spark_1");
  const now = new Date();
  await assertSucceeds(
    sparkRef.set({
      userId: ALICE_UID,
      title: "Quantum Poetry Idea",
      content: "A poem about entangled particles speaking through light.",
      category: "science",
      tags: ["physics", "poetry"],
      isFavorite: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(sparkRef.get());
});

test("Bob cannot read or delete Alice's spark", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("sparks").doc("spark_1").set({
      userId: ALICE_UID,
      title: "Alice Private Spark",
      content: "Content",
      category: "prompt",
      tags: [],
      isFavorite: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("sparks").doc("spark_1").get());
  await assertFails(bobDb.collection("sparks").doc("spark_1").delete());
});

test("Alice can create and update her user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  const now = new Date();
  await assertSucceeds(
    userRef.set({
      userId: ALICE_UID,
      username: "AliceTheCreator",
      bio: "Sci-Fi writer & poet",
      preferredCreativeStyle: "poetic",
      favoriteDomains: ["Sci-Fi", "Music"],
      enableHighThinking: true,
      enableLowLatency: false,
      preferredVoice: "Kore",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(userRef.get());
});

test("Bob can view Alice's profile but cannot modify it", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AliceTheCreator",
      preferredCreativeStyle: "poetic",
      favoriteDomains: ["Sci-Fi"],
      enableHighThinking: false,
      enableLowLatency: false,
      preferredVoice: "Kore",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      username: "HackedByBob",
    })
  );
});
