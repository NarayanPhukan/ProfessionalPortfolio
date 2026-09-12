const { getApps, initializeApp, cert } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getAuth } = require('firebase-admin/auth');
const { getStorage } = require('firebase-admin/storage');
const path = require('path');
const fs = require('fs');

if (!getApps().length) {
  let credential;
  const serviceAccountPath = path.resolve(__dirname, '../firebase-service-account.json');

  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    try {
      const parsed = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
      credential = cert(parsed);
    } catch (e) {
      // If it's base64 encoded
      const decoded = Buffer.from(process.env.FIREBASE_SERVICE_ACCOUNT, 'base64').toString('utf8');
      credential = cert(JSON.parse(decoded));
    }
  } else if (fs.existsSync(serviceAccountPath)) {
    credential = cert(require(serviceAccountPath));
  } else {
    credential = cert(require(serviceAccountPath));
  }

  initializeApp({
    credential,
    projectId: 'narayan-portfolio-app',
    storageBucket: 'narayan-portfolio-app.firebasestorage.app'
  });
}

const db = getFirestore();
const auth = getAuth();
const storage = getStorage();

module.exports = { db, auth, storage };
