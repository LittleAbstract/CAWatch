# Privacy Policy — CAWatch

**Last updated:** 2026-07-12  
**App:** CAWatch (`com.security.careactivator`)

## Summary

CAWatch is a **fully local** security utility. It does **not** collect personal data, does **not** use analytics SDKs, and does **not** send information off your device.

## What the app reads

- User-installed Certificate Authority (CA) certificates from the Android trust store
- For each certificate: issuer name, subject name, and SHA-256 fingerprint of the certificate bytes

## What the app stores

- A **trusted baseline** of user CAs present when you first set up the app (or after you reset the baseline)
- A **local history** of newly detected CAs (after the baseline)

Storage is an on-device SQLite database (Room) and app preferences. Data is not uploaded.

## What the app does **not** do

- No internet access (network permission is not requested / is stripped)
- No accounts or sign-in
- No advertising identifiers
- No crash or analytics telemetry
- No sale or sharing of data with third parties
- No access to contacts, location, camera, microphone, SMS, or files beyond certificate trust store APIs

## Notifications

If you grant notification permission, the app may show:

- Alerts when a new user CA is detected
- An optional persistent “CA Monitor” notification only if you enable **Always-on** monitoring

## Sharing / export

If you use **Export** or **Share**, you choose the destination (email, files app, messaging). The app only prepares a local JSON or text payload; it does not transmit it itself.

## Children

The app is not directed at children under 13 and does not knowingly collect children’s data.

## Changes

We may update this policy when the app changes. Material changes will be reflected in the “Last updated” date and in the app listing.

## Contact

For privacy questions about this open/local build, use the contact channel listed on the store listing or project repository once published.

---

Host this file (GitHub Pages, your site, etc.) and paste the public URL into Google Play Console → App content → Privacy policy.
