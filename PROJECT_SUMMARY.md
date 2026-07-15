# CAWatch — Project Summary

## Overview

Android security utility that **detects newly installed user Certificate Authorities** by comparing the current trust store against a trusted baseline captured on first run.

**Not** an AI app. Path A product: local, offline, freemium-ready utility for privacy/security-conscious users.

## Architecture

```
┌─────────────────────────────────────────────┐
│  OnboardingActivity                         │
│  MainActivity + DetectedCaAdapter           │
│  DetailActivity (remediate / trust / share) │
├─────────────────────────────────────────────┤
│  BootReceiver → CertScannerService          │
│  AppPrefs (boot+manual | always-on)         │
├─────────────────────────────────────────────┤
│  Room: baseline_ca, detected_ca             │
│  AndroidCAStore (user trust anchors)        │
└─────────────────────────────────────────────┘
```

## Features (v1.2.0)

- Trusted baseline on first run
- Diff scan for new user CAs
- Boot scan + manual scan
- Optional always-on foreground monitor
- Alerts (with POST_NOTIFICATIONS)
- Detail screen, Settings deep link, accept-to-baseline, dismiss
- JSON export / share
- Clear history & reset baseline
- No network permission

## Build

See `BUILD.md`. Output: debug APK or release AAB.

## Docs

| File | Purpose |
|------|---------|
| `PATH_A_PRODUCT.md` | Product plan & backlog |
| `PLAY_STORE_PREP.md` | Play Console checklist |
| `PRIVACY_POLICY.md` | Hostable privacy policy |
| `CHANGELOG.md` | Version history |
