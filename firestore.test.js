const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user: cannot read or write tokens, app_status, or social_posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tokens").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("tokens").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fcmToken: "token_abc",
      platform: "android",
      liveEnabled: true,
      videosEnabled: true,
      socialEnabled: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(unauthDb.collection("app_status").doc("live").get());
  await assertFails(
    unauthDb.collection("social_posts").where("published", "==", true).get()
  );
});

test("Authenticated owner: valid Kotlin token create, partial update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("tokens").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fcmToken: "fcm_token_xyz_123",
      platform: "android",
      liveEnabled: true,
      videosEnabled: true,
      socialEnabled: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Mirror Kotlin partial update on notification preferences
  await assertSucceeds(
    aliceDb.collection("tokens").doc(ALICE_UID).update({
      socialEnabled: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("tokens").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable userId modification
  await assertFails(
    aliceDb.collection("tokens").doc(ALICE_UID).update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject shadow/ghost field on create
  await assertFails(
    aliceDb.collection("tokens").doc(ALICE_UID + "_extra").set({
      userId: ALICE_UID,
      fcmToken: "t",
      platform: "android",
      liveEnabled: true,
      videosEnabled: true,
      socialEnabled: true,
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("tokens").doc(ALICE_UID).update({
      liveEnabled: false,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(aliceDb.collection("tokens").doc(ALICE_UID).delete());
});

test("Authenticated user: cannot read, list, or write another user's token", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("tokens").doc(BOB_UID).set({
      userId: BOB_UID,
      fcmToken: "bob_secret_token",
      platform: "android",
      liveEnabled: true,
      videosEnabled: true,
      socialEnabled: true,
      createdAt: new Date(Date.now() - 10000),
      updatedAt: new Date(Date.now() - 10000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tokens").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("tokens").get());
  await assertFails(
    aliceDb.collection("tokens").doc(BOB_UID).update({
      liveEnabled: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("tokens").doc(BOB_UID).set({
      userId: ALICE_UID,
      fcmToken: "hijack",
      platform: "android",
      liveEnabled: true,
      videosEnabled: true,
      socialEnabled: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated user: can read app_status docs and published social_posts, cannot write them", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("app_status").doc("live").set({
      liveActive: true,
      title: "Culto Dominical en Vivo",
      updatedAt: new Date(Date.now() - 5000),
    });
    await db.collection("social_posts").doc("post_1").set({
      caption: "Domingo en Cristo Rey",
      imageUrl: "https://example.com/img.jpg",
      postUrl: "https://www.instagram.com/cccristoreyve",
      published: true,
      publishedAt: "2026-11-21",
      createdAt: new Date(Date.now() - 5000),
    });
    await db.collection("social_posts").doc("post_hidden").set({
      caption: "Borrador",
      imageUrl: "https://example.com/hidden.jpg",
      postUrl: "https://www.instagram.com/cccristoreyve",
      published: false,
      publishedAt: "2026-11-21",
      createdAt: new Date(Date.now() - 5000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("app_status").doc("live").get());
  await assertFails(aliceDb.collection("app_status").get());
  await assertFails(
    aliceDb.collection("app_status").doc("live").set({
      liveActive: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Matches Kotlin query: collection("social_posts").whereEqualTo("published", true)
  await assertSucceeds(
    aliceDb.collection("social_posts").where("published", "==", true).get()
  );
  // Unfiltered query must fail
  await assertFails(aliceDb.collection("social_posts").get());
  // Hidden post get must fail
  await assertFails(aliceDb.collection("social_posts").doc("post_hidden").get());
  // Client write must fail
  await assertFails(
    aliceDb.collection("social_posts").doc("post_2").set({
      caption: "Fake post",
      imageUrl: "https://example.com/a.jpg",
      postUrl: "https://example.com",
      published: true,
      createdAt: new Date(Date.now() - 1000),
    })
  );
});
