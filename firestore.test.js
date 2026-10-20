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

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read their own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice Founder",
      email: "alice@example.com",
      role: "CEO",
      isFounder: true,
      companyId: "AURA-1234",
      companyName: "Aura AI",
      presenceStatus: "Active",
      joinedAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot write another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      name: "Bob Teammate",
      email: "bob@example.com",
      companyId: "AURA-1234",
      joinedAt: new Date(),
    })
  );
});

test("Authenticated user: can create company as founder", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("companies").doc("AURA-1234").set({
      id: "AURA-1234",
      name: "Aura AI",
      founderUid: ALICE_UID,
      founderName: "Alice Founder",
      founderEmail: "alice@example.com",
      memberUids: [ALICE_UID],
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can create and query tasks in company", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tasks").doc("task_1").set({
      id: "task_1",
      companyId: "AURA-1234",
      title: "Launch MVP",
      status: "NOT_STARTED",
      priority: "HIGHEST",
      creatorId: ALICE_UID,
      creatorName: "Alice Founder",
      createdAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("tasks").where("companyId", "==", "AURA-1234").get()
  );
});

test("Authenticated user: can post and query chat messages in company", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("chat_messages").doc("chat_1").set({
      id: "chat_1",
      companyId: "AURA-1234",
      senderId: ALICE_UID,
      senderName: "Alice Founder",
      content: "Welcome team to Aura AI!",
      timestamp: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("chat_messages").where("companyId", "==", "AURA-1234").get()
  );
});
