# CAWatch — Google Play Console prep (Path A)

## 1. Build

```bash
cd CAWatch
./gradlew bundleRelease
```

## 2. App signing

Use Play App Signing. Keep release minify on (already configured).

## 3. Foreground service

Default user path is **Boot + manual** (service stops after scan).  
**Always-on** uses `foregroundServiceType="specialUse"`.

Paste for Sensitive permissions declaration if always-on remains in the build:

> CAWatch is a security/transparency tool. When the user opts into Always-on monitoring, it runs a foreground service to watch the user-installed certificate trust store and alert when a new Certificate Authority is installed. The service shows a persistent “CA Monitor” notification. It does not access location, camera, microphone, or contacts. All certificate data stays on-device.

If Play rejects `specialUse`, keep **Boot + manual** only and remove always-on from the UI.

## 4. Privacy policy (required)

1. Host `PRIVACY_POLICY.md` (GitHub Pages / your site).
2. Link URL in Play Console → App content → Privacy policy and store listing.

Key facts: local-only, no INTERNET, reads issuer/subject/SHA-256 only.

## 5. Store listing copy

**Name:** CAWatch  

**Short description:**  
Alerts you the moment a new Certificate Authority is installed on your device.

**Full description (draft):**

CAWatch watches for *user-installed* Certificate Authorities on your Android device.

On first launch it captures a trusted baseline of your current user CAs. After that, any newly installed CA triggers an alert and appears in your history — useful against malicious apps, shady profiles, or unexpected MITM certificates.

• Fully offline — no accounts, no ads, no analytics  
• Scan on boot and on demand  
• Optional always-on monitor  
• Open system credential settings from an alert  
• Mark intentional CAs as trusted  
• Export a local JSON report for your records  

CAWatch does not delete certificates for you (Android requires that in Settings). It gives you *awareness* the moment something changes.

## 6. Data safety form

- Data collected: No  
- Data shared: No  
- Security practices: Data is encrypted in transit N/A (no network); you may mark “data is encrypted in transit” as no / not applicable  
- Users can request deletion: clearing app data / uninstall removes all local data  

## 7. Pre-launch checklist

- [ ] Privacy policy URL live  
- [ ] Screenshots: onboarding, empty safe state, detection card, detail actions  
- [ ] Feature graphic  
- [ ] Content rating questionnaire  
- [ ] Target API 34+  
- [ ] Test on clean device + device with existing user CA  
