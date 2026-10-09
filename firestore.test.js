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

test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      savingsTarget: 50000000,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Authenticated user: can read and write own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice",
    savingsTarget: 50000000,
  }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create, read, and delete own account", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const accRef = aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc1");
  await assertSucceeds(accRef.set({
    id: "acc1",
    userId: ALICE_UID,
    name: "BRI",
    type: "liquid",
  }));
  await assertSucceeds(accRef.get());
});

test("Authenticated user: cannot access another user's account", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("accounts").doc("bobs_acc").set({
      id: "bobs_acc",
      userId: BOB_UID,
      name: "Line Bank",
      type: "liquid",
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("accounts").doc("bobs_acc").get());
});

test("Authenticated user: can create and read own transaction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1");
  await assertSucceeds(txRef.set({
    id: "tx1",
    userId: ALICE_UID,
    type: "out",
    description: "Bayar Kos",
    amount: 750000,
    account: "BRI",
    category: "Kebutuhan Kos",
    date: "2026-10-09",
  }));
  await assertSucceeds(txRef.get());
});

test("Authenticated user: rejects transaction with invalid amount or missing fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("bad_tx");
  // Invalid amount <= 0
  await assertFails(txRef.set({
    id: "bad_tx",
    userId: ALICE_UID,
    type: "out",
    description: "Bad",
    amount: -500,
    account: "BRI",
    date: "2026-10-09",
  }));
});
