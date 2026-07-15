# CAWatch — Google Play Store Submission Pack

App: CAWatch (com.security.careactivator)
Build: app-release.aab (signed, v1.3.0 / versionCode 4)
targetSdk 35 (Android 15) · minSdk 23 (Android 6.0)

============================================================
1) PLAY CONSOLE — APP CONTENT / STORE LISTING
============================================================

App name: CAWatch

Short description (80 char max):
Alerts you the moment a new Certificate Authority is installed on your device.

Full description:
CAWatch watches your device's trusted certificate store and alerts you the moment a new user-installed Certificate Authority (CA) appears.

Why it matters:
A new user CA can mean a VPN or security app inspecting your traffic, an MDM/profile install at work, or — in the worst case — malware attempting to intercept encrypted connections (tools like Charles Proxy / mitmproxy install user CAs).

How it works:
• First run captures a baseline of the CAs already trusted on your device.
• Later scans compare the current store against that baseline.
• Any CA not in your baseline triggers a notification with issuer details.
• Tap an alert to review, mark it trusted, share its fingerprint, or dismiss it.

Privacy-first by design:
• Fully offline. No internet permission, no analytics, no ads.
• All data stays on your device in a local database.
• Nothing is transmitted anywhere.

Monitoring modes:
• Boot + manual (default): scans on boot and when you tap Scan.
• Always-on: runs a lightweight foreground service for continuous monitoring.

Perfect for security-conscious users, developers testing TLS interception, and anyone who wants to know when their trust store changes.

Category: Tools (or "Communication" if Tools unavailable in your console)
Tags: security, privacy, certificate, VPN, monitoring, CA, TLS, SSL
Content rating: Low maturity (no user-generated content, no violence)
============================================================

2) DATA SAFETY FORM (console: Policy > App content > Data safety)
============================================================

Your app:
• Does NOT collect any data that is transmitted off the device.
• Stores a local baseline of Certificate Authority metadata (issuer, subject,
  SHA-256 fingerprint, detection timestamp) in an on-device Room database.

Answer the form as follows:

Q: Does your app collect or share any data?
A: "No"  (data is collected but NEVER shared/transmitted — Play treats
   "collected but not shared" by selecting the data types below and choosing
   "Data is not shared with third parties" + "Data is processed ephemerally
   or stored locally only".)

Recommended exact selections:
• Data types collected:
  - "App info and performance" → App activity  → Collected: YES
    Purpose: App functionality  → Shared with third parties: NO
    (Choose "Data is stored locally on your device")
  - OR simpler: declare "No data collected" if you consider the local CA
    baseline as not user-provided personal data. Many security tools select
    "No data collected" because nothing leaves the device and nothing is
    account-related. Given the data is device certificate metadata (not
    user identity), "No data collected / shared" is defensible and simplest.

Recommended: select "No" to data collection for the store listing, because
the CA metadata is device/security state, not user personal data, and nothing
is transmitted. Keep the local-storage note in your internal records.

Q: Is all of the data you collect encrypted in transit?  N/A (nothing in transit)
Q: Do you provide a way for users to request deletion?  Data is local only;
   user can Clear history / Reset baseline in-app, or uninstall to wipe all.

============================================================
3) PERMISSION JUSTIFICATIONS (console will ask during review)
============================================================

android.permission.FOREGROUND_SERVICE
  Justification: Required to run the optional "Always-on" monitoring mode
  that continuously watches the device trust store for newly installed CAs
  and shows a persistent status notification. Uses type "specialUse" with
  description "Monitoring the device trust store for newly installed user
  Certificate Authorities".

android.permission.FOREGROUND_SERVICE_SPECIAL_USE
  Justification: Companions the foreground service; the CA-monitoring use
  case does not fit a standard FGS type, so specialUse is declared with the
  explicit purpose above.

android.permission.RECEIVE_BOOT_COMPLETED
  Justification: Allows the app to re-establish CA monitoring after device
  reboot so the baseline comparison stays current (Boot + manual mode).

android.permission.POST_NOTIFICATIONS
  Justification: To alert the user when a new user-installed Certificate
  Authority is detected. Requested at runtime on Android 13+.

(android.permission.INTERNET is declared in the manifest but explicitly
 removed via tools:node="remove" — the app is fully offline. If the console
 flags it, note: no network calls are made; the permission is stripped at
 build time.)

============================================================
4) REQUIRED GRAPHICS (you must supply these — can't auto-generate)
============================================================

• App icon: 512 x 512 px PNG (the launcher art is generated code; for the
  store listing upload a clean 512px version of the same padlock motif, or
  your designer's icon).
• Feature graphic: 1024 x 500 px PNG/JPEG (required for the listing).
• Phone screenshots: at least 2, 16:9 or 9:16, of the main screen + an alert.
  (Capture from a running emulator/device: Onboarding, Main list, Detail.)

============================================================
5) RELEASE UPLOAD STEPS
============================================================

1. Play Console → Create app → fill store listing (section 1) + graphics (4).
   Privacy Policy URL: https://littleabstract.github.io/CAWatch/privacy_policy.html
2. Policy → App content → Data safety (section 2) + permissions (section 3).
3. Publishing → Production → Create release → Upload AAB:
   /Users/Corvo/Dreadful_Wale/user_ca/CAWatch/app/build/outputs/bundle/release/app-release.aab
4. Monetize → Products → Subscriptions / One-time (see section 6).
5. Submit for review.

============================================================
6) PLAY BILLING PRODUCTS (create after merchant account is linked)
============================================================

The app gates "Always-on" monitoring behind Google Play Billing. Product IDs
hardcoded in billing/BillingManager.kt:

  always_on_monthly  — Subscription, base plan WITH a 7-day free trial
  lifetime_unlock     — One-time "lifetime" unlock

Console click-path (Play Console → Monetize → Products):
  A) Subscriptions:
     1. "Subscriptions" → Create subscription → Product ID: always_on_monthly
     2. Add a base plan → pricing: choose your price (e.g. $3.99/mo) and
        billing period Monthly.
     3. Under the base plan → "Add free trial" → 7 days. Save.
     4. Publish the product.
  B) One-time:
     1. "One-time products" → Create product → Product ID: lifetime_unlock
     2. Price: e.g. $19.99. Save + activate.
  Note: Play requires a linked merchant account before paid products can be
  created/published. Free tier (Boot + manual scans) stays fully functional.

============================================================
7) GITHUB REPO + PAGES CLICK-PATH (done — here for reproducibility)
============================================================

Repo: https://github.com/LittleAbstract/CAWatch  (public)
Live privacy URL: https://littleabstract.github.io/CAWatch/privacy_policy.html

To recreate from scratch:
  a) gh auth login  (browser/device-code; needs 'repo' scope)
  b) gh repo create CAWatch --public --source . --remote origin --push
     (or on the web: New repository -> name CAWatch -> public -> Create)
  c) Ensure privacy_policy.html is at the repo ROOT (it is).
  d) Repo -> Settings -> Pages ->
       Source: "Deploy from a branch"
       Branch: main   |  Folder: / (root)
       Save.
  e) Wait ~1 min; visit the URL -> it should return HTTP 200.
  f) Put that URL in Play Console -> App content -> Privacy Policy URL
     (already recorded above in section 5, step 1).

Note: keystore.properties / *.keystore / checkpoint.md are gitignored,
so the push never includes signing secrets. Verify with:
  git ls-files | grep -E "keystore|properties|checkpoint"   (expect: nothing)

============================================================
8) PLAY CONSOLE MERCHANT ACCOUNT (PREREQ for section 6 products)
============================================================

You said you still need to create this. Paid subscriptions/one-time
products CANNOT be published until a merchant account is linked.

Click-path:
  1. Play Console -> (your app) or "All apps".
  2. Left menu -> Payments profile (or "Setup" -> "Payments profile").
  3. "Set up a payments profile" / "Create a merchant account".
  4. Choose account type: Business or Individual.
  5. Fill: legal name, address, tax info (SSN/EIN for US individual),
     contact email, phone.
  6. Add a payout bank account (where Google sends your revenue).
  7. Accept the Merchant Agreement -> Submit.
  8. Wait for verification (minutes to a few days; you get an email).
  9. Confirm status shows "Active / Verified" under Payments profile.

After this is Active, return to section 6 and create:
  - always_on_monthly (subscription, 7-day trial) and
  - lifetime_unlock (one-time $19.99).
The app code already references both IDs and gates Always-on on them.

============================================================
SECURITY NOTES (read once)
============================================================

• Keystore: CAWatch-release-key.keystore  (project root, gitignored)
• Alias: caWatchRelease
• Password: REDACTED — kept in the developer's password manager. Keystore
  backup at ~/Documents/Zero/user_ca/CAWatch-release-key.keystore. Recreate a
  local keystore.properties to sign release builds.
• BACK UP THE KEYSTORE FILE OFFLINE. Losing it = you can never update CAWatch
  on Play. Google cannot recover it.
• keystore.properties and *.keystore are in .gitignore — never commit them.
