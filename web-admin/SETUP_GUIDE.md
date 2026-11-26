# RAAG AUTOMOBILES – Web Admin Panel Deployment Guide (`https://raagevinsurance.kesug.com/admin`)

This folder contains the standalone Web Admin Panel (`index.html`) that connects live to the exact same Firebase Cloud Firestore database (`ai-studio-android-raagevin-0da03710-2d77-47af-9011-584b2cbb6d98`) as the **RAAG EV Insurance** Android Customer App.

## 3-Step Hosting Setup on `raagevinsurance.kesug.com`

1. **Authorize Your Domain in Firebase Authentication**
   - Open [Firebase Console](https://console.firebase.google.com/) → Project **`gen-lang-client-0567268236`**.
   - Go to **Authentication → Settings → Authorized domains → Add domain**.
   - Add `raagevinsurance.kesug.com`.

2. **Add Your Firebase Web App Config**
   - In Firebase Console → **Project Settings → Your apps**, click **Add app → Web (`</>`)** (if you haven't created a Web app yet).
   - Copy the `apiKey` and `appId` values and paste them into `FIREBASE_CONFIG` at the top of `web-admin/index.html`.

3. **Upload to Your Website Hosting**
   - Open your hosting File Manager / FTP for `raagevinsurance.kesug.com`.
   - Upload `web-admin/index.html` into `/htdocs/admin/index.html` (replacing any old file in `/admin/`).
   - Open `https://raagevinsurance.kesug.com/admin` and click **Sign in with Authorized Admin Google Account**.

## Authorized Super Admin Accounts
Security rules strictly verify Google Sign-In tokens (no hardcoded weak passwords in frontend HTML):
- `suchita9867@gmail.com`
- `wpwebpole@gmail.com`
- `vk725846@gmail.com`
