# CAWatch

A privacy-first Android security app that watches your device's trusted
certificate store and alerts you the moment a new **user-installed Certificate
Authority (CA)** appears — a common signal of TLS interception (VPN/app inspection,
MDM profiles, or malware like Charles Proxy / mitmproxy).

- **Fully offline.** No `INTERNET` permission, no analytics, no ads. All CA
  metadata stays in an on-device Room database.
- **Monitoring modes:** Boot + manual (default, free) and Always-on foreground
  monitor (CAWatch Pro).
- **Built with:** Kotlin, Android Gradle Plugin 9, Room (KSP), Jetpack, and
  Google Play Billing for the Pro tier.

| Item | Value |
|------|-------|
| Package | `com.security.careactivator` |
| minSdk | 23 (Android 6.0) |
| targetSdk | 35 (Android 15) |
| compileSdk | 37 |

## Build

```bash
# Debug / unsigned release (no signing secrets required for a clean build)
gradle assembleDebug

# Signed release AAB — requires keystore.properties + the release keystore
# (both gitignored; keep them out of version control). Place keystore.properties
# at the project root pointing at your CAWatch-release-key.keystore.
gradle bundleRelease
```

The `signingConfigs.release` block is always declared; it is populated only when
`keystore.properties` exists, so the project builds fine without the signing
secrets (release just stays unsigned).

## Pro / monetization

"Always-on" monitoring is gated behind Google Play Billing
(`billing/BillingManager.kt`). Entitlement is derived from Play
(`queryPurchasesAsync`), not from local state. Product IDs:

- `always_on_monthly` — subscription with a 7-day free trial
- `lifetime_unlock` — one-time unlock

The free tier keeps Boot + manual scans fully functional (required for Play review).

## Privacy

Privacy policy: [privacy_policy.html](privacy_policy.html)
