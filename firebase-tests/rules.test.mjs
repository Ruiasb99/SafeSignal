import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc, deleteDoc, collection, getDocs, query, where, serverTimestamp, Timestamp, runTransaction } from 'firebase/firestore';

let env;
const code = 'a'.repeat(32);
const future = () => Timestamp.fromMillis(Date.now() + 86400000);
const client = (uid, verified = true) => env.authenticatedContext(uid, { email: `${uid}@example.com`, email_verified: verified }).firestore();
const invitation = (overrides = {}) => ({ fromUid: 'alice', fromName: 'Alice', toUid: '', toName: '', status: 'pending',
  createdAt: Timestamp.now(), updatedAt: Timestamp.now(), expiresAt: future(), ...overrides });
const createData = (overrides = {}) => invitation({ createdAt: serverTimestamp(), updatedAt: serverTimestamp(), ...overrides });
const ref = db => doc(db, 'contactInvitations', code);
const claim = (uid = 'bob', name = 'Bob', status = 'accepted') => ({ toUid: uid, toName: name, status, updatedAt: serverTimestamp() });
async function seed(data = invitation()) {
  await env.withSecurityRulesDisabled(async c => {
    await setDoc(doc(c.firestore(), 'users', 'alice'), { displayName: 'Alice', updatedAt: Timestamp.now() });
    await setDoc(doc(c.firestore(), 'users', 'bob'), { displayName: 'Bob', updatedAt: Timestamp.now() });
    await setDoc(doc(c.firestore(), 'users', 'carol'), { displayName: 'Carol', updatedAt: Timestamp.now() });
    if (data) await setDoc(ref(c.firestore()), data);
  });
}
before(async () => { env = await initializeTestEnvironment({ projectId: 'demo-safesignal',
  firestore: { host: '127.0.0.1', port: 8088, rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') } }); });
beforeEach(async () => { await env.clearFirestore(); });
after(async () => { await env.cleanup(); });

test('profiles are private, verified and restricted to the permitted fields', async () => {
  const alice = client('alice');
  await assertSucceeds(setDoc(doc(alice, 'users', 'alice'), { displayName: 'Alice', updatedAt: serverTimestamp() }));
  await assertSucceeds(getDoc(doc(alice, 'users', 'alice')));
  await assertFails(getDoc(doc(client('bob'), 'users', 'alice')));
  await assertFails(getDocs(collection(alice, 'users')));
  await assertFails(setDoc(doc(client('bob'), 'users', 'alice'), { displayName: 'Fake', updatedAt: serverTimestamp() }));
  await assertFails(setDoc(doc(client('alice', false), 'users', 'alice'), { displayName: 'Alice', updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(alice, 'users', 'alice'), { admin: true }));
  await assertFails(updateDoc(doc(alice, 'users', 'alice'), { displayName: '', updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(alice, 'users', 'alice'), { displayName: 'x'.repeat(81), updatedAt: serverTimestamp() }));
});

test('creator can read a missing code in a transaction and create a pending invite', async () => {
  await seed(null);
  const alice = client('alice');
  await assertSucceeds(runTransaction(alice, async tx => {
    assert.equal((await tx.get(ref(alice))).exists(), false);
    tx.set(ref(alice), createData());
  }));
});

test('forged creator, name, initial acceptance, extra fields and long expiry are denied', async () => {
  await seed(null);
  const alice = client('alice');
  for (const changes of [{ fromUid: 'bob' }, { fromName: 'Someone Else' }, { status: 'accepted', toUid: 'bob', toName: 'Bob' },
    { arbitrary: 'data' }, { expiresAt: Timestamp.fromMillis(Date.now() + 8 * 86400000) }]) {
    await assertFails(setDoc(ref(alice), createData(changes)));
  }
  await assertFails(setDoc(doc(alice, 'contactInvitations', 'short'), createData()));
});

test('anonymous and unverified users cannot preview or create invitations', async () => {
  await seed();
  await assertFails(getDoc(ref(env.unauthenticatedContext().firestore())));
  await assertFails(getDoc(ref(client('bob', false))));
  await assertFails(setDoc(doc(client('alice', false), 'contactInvitations', 'b'.repeat(32)), createData()));
});

test('a code permits preview but never global invitation enumeration', async () => {
  await seed();
  const bob = client('bob');
  await assertSucceeds(getDoc(ref(bob)));
  await assertFails(getDocs(collection(bob, 'contactInvitations')));
  await assertFails(getDocs(query(collection(bob, 'contactInvitations'), where('status', '==', 'pending'))));
});

test('only participant-scoped queries are allowed', async () => {
  await seed();
  const alice = client('alice');
  await assertSucceeds(getDocs(query(collection(alice, 'contactInvitations'), where('fromUid', '==', 'alice'))));
  await assertSucceeds(getDocs(query(collection(client('bob'), 'contactInvitations'), where('toUid', '==', 'bob'))));
  await assertFails(getDocs(query(collection(client('carol'), 'contactInvitations'), where('fromUid', '==', 'alice'))));
});

test('recipient accepts exactly once as themselves; outsiders lose access', async () => {
  await seed();
  const bob = client('bob');
  await assertSucceeds(runTransaction(bob, async tx => { await tx.get(ref(bob)); tx.update(ref(bob), claim()); }));
  await assertSucceeds(getDoc(ref(bob)));
  await assertSucceeds(getDoc(ref(client('alice'))));
  await assertFails(getDoc(ref(client('carol'))));
  await assertFails(updateDoc(ref(client('carol')), claim('carol', 'Carol')));
});

test('cannot accept own invite or impersonate a different recipient', async () => {
  await seed();
  await assertFails(updateDoc(ref(client('alice')), claim('alice', 'Alice')));
  await assertFails(updateDoc(ref(client('bob')), claim('carol', 'Carol')));
  await assertFails(updateDoc(ref(client('bob')), claim('bob', 'Fake')));
  await assertFails(updateDoc(ref(client('bob')), { ...claim(), fromUid: 'carol' }));
  await assertFails(updateDoc(ref(client('bob')), { ...claim(), expiresAt: future() }));
});

test('recipient can decline; sender cannot force acceptance or reuse declined code', async () => {
  await seed();
  await assertSucceeds(updateDoc(ref(client('bob')), claim('bob', 'Bob', 'declined')));
  await assertFails(updateDoc(ref(client('alice')), { status: 'accepted', updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(ref(client('bob')), { status: 'accepted', updatedAt: serverTimestamp() }));
});

test('expired code cannot be previewed by outsiders or accepted', async () => {
  await seed(invitation({ expiresAt: Timestamp.fromMillis(Date.now() - 60000) }));
  await assertSucceeds(getDoc(ref(client('alice'))));
  await assertFails(getDoc(ref(client('bob'))));
  await assertFails(updateDoc(ref(client('bob')), claim()));
});

test('sender can cancel pending code; no reactivation or deletion', async () => {
  await seed();
  await assertSucceeds(updateDoc(ref(client('alice')), { status: 'revoked', updatedAt: serverTimestamp() }));
  await assertFails(getDoc(ref(client('bob'))));
  await assertFails(updateDoc(ref(client('alice')), { status: 'pending', updatedAt: serverTimestamp() }));
  await assertFails(deleteDoc(ref(client('alice'))));
});

test('either participant can revoke an accepted connection, but no third party can', async () => {
  for (const uid of ['alice', 'bob']) {
    await seed(invitation({ status: 'accepted', toUid: 'bob', toName: 'Bob' }));
    await assertFails(updateDoc(ref(client('carol')), { status: 'revoked', updatedAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(ref(client(uid)), { status: 'revoked', updatedAt: serverTimestamp() }));
  }
});

test('accepted relationship cannot have participants renamed or replaced', async () => {
  await seed(invitation({ status: 'accepted', toUid: 'bob', toName: 'Bob' }));
  await assertFails(updateDoc(ref(client('alice')), { toUid: 'carol', toName: 'Carol', updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(ref(client('bob')), { fromName: 'Fake', updatedAt: serverTimestamp() }));
});

test('unrelated collections deny all client reads and writes', async () => {
  const alice = client('alice');
  await assertFails(setDoc(doc(alice, 'incidents', 'example'), { owner: 'alice' }));
  await assertFails(getDoc(doc(alice, 'incidents', 'example')));
});
