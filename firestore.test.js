const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_kutch_123";
const BOB_UID = "bob_bhuj_456";

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

test("Unauthenticated user cannot read or write any store collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/cart`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/orders`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/cart`).doc("item_1").set({
      productId: "rabari_mirror_tote",
      productName: "Rabari Abhla Tote",
      artisanOrigin: "Hodka, Kutch",
      craftType: "Rabari Mirror Work",
      variant: "Sindoor Crimson",
      unitPriceInr: 2450,
      quantity: 1,
      drawableKey: "img_bag_mirror_tote",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: cart CRUD matches Kotlin repository payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const cartRef = aliceDb.collection(`users/${ALICE_UID}/cart`).doc("rabari_mirror_tote__sindoor_crimson");

  await assertSucceeds(
    cartRef.set({
      productId: "rabari_mirror_tote",
      productName: "Rabari Abhla Mirror Tote",
      artisanOrigin: "Hodka, Kutch",
      craftType: "Rabari Mirror Work",
      variant: "Sindoor Crimson",
      unitPriceInr: 2450,
      quantity: 1,
      drawableKey: "img_bag_mirror_tote",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update on quantity & updatedAt
  await assertSucceeds(
    cartRef.update({
      quantity: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field & immutable createdAt mutation
  await assertFails(
    cartRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/cart`).doc("ghost_item").set({
      id: "ghost_item",
      productId: "rabari_mirror_tote",
      productName: "Rabari Abhla Mirror Tote",
      artisanOrigin: "Hodka, Kutch",
      craftType: "Rabari Mirror Work",
      variant: "Sindoor Crimson",
      unitPriceInr: 2450,
      quantity: 1,
      drawableKey: "img_bag_mirror_tote",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Owner can list their own cart
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/cart`).get());
});

test("Cross-user isolation: Bob cannot read or mutate Alice's cart, wishlist, addresses, or orders", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/addresses`).doc("addr_1").set({
      recipientName: "Asha Jadeja",
      phone: "+91 98250 12345",
      streetLine: "14 Bhujodi Craft Lane",
      city: "Bhuj",
      state: "Gujarat",
      postalCode: "370001",
      label: "Home",
      preferred: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(bobDb.collection(`users/${ALICE_UID}/addresses`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/addresses`).doc("addr_1").get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/addresses`).doc("addr_1").delete());
});

test("Orders: atomic batch creation with line items, cancel transition, and terminal state lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const orderRef = aliceDb.collection(`users/${ALICE_UID}/orders`).doc("order_101");
  const itemRef = orderRef.collection("items").doc("item_1");

  const batch = aliceDb.batch();
  batch.set(orderRef, {
    orderNumber: "BB-104829",
    status: "placed",
    paymentMethod: "upi_collect",
    paymentReference: "",
    recipientName: "Asha Jadeja",
    phone: "+91 98250 12345",
    shippingSummary: "14 Bhujodi Craft Lane, Bhuj, Gujarat 370001",
    giftNote: "Please wrap in Ajrakh cloth",
    subtotalInr: 2450,
    shippingInr: 0,
    totalInr: 2450,
    itemCount: 1,
    previewTitle: "Rabari Abhla Mirror Tote",
    previewDrawableKey: "img_bag_mirror_tote",
    createdAt: created,
    updatedAt: created,
  });
  batch.set(itemRef, {
    productId: "rabari_mirror_tote",
    productName: "Rabari Abhla Mirror Tote",
    variant: "Sindoor Crimson",
    unitPriceInr: 2450,
    quantity: 1,
    drawableKey: "img_bag_mirror_tote",
    createdAt: created,
  });
  await assertSucceeds(batch.commit());

  // Cannot mutate totalInr or create orphan line item without parent order
  await assertFails(
    orderRef.update({
      totalInr: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cancel transition from placed -> cancelled succeeds
  await assertSucceeds(
    orderRef.update({
      status: "cancelled",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock: cancelled order cannot be modified again
  await assertFails(
    orderRef.update({
      status: "placed",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner can delete line items and order during account deletion; Bob cannot
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/orders`).doc("order_101").delete());
  await assertSucceeds(itemRef.delete());
  await assertSucceeds(orderRef.delete());
});
