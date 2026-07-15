# CAWatch — Path A product plan

**Positioning:** Niche security utility (not an AI consumer app).  
**Pitch:** *Alerts you the moment a new Certificate Authority is installed on your device.*  
**Monetization (later):** Free core + optional one-time Pro unlock — not subscription-first.

## Shipped in v1.2.0 (this codebase)

| Feature | Status |
|---------|--------|
| Baseline + detect new user CAs | Done |
| Boot scan | Done |
| Onboarding + monitoring mode pick | Done |
| Notification permission (API 33+) | Done |
| Empty state education | Done |
| CA detail + open credential settings | Done |
| Accept into baseline / dismiss | Done |
| Export full report (JSON share) | Done |
| Share single CA | Done |
| Monitoring mode: boot+manual (default) vs always-on FGS | Done |
| Clear history / reset baseline | Done |
| Dark/light Material palette | Done |
| Privacy policy draft | `PRIVACY_POLICY.md` |
| Offline / no INTERNET | Done |

## Still open (Path A backlog)

### P0 before Play submit
- [ ] Host `PRIVACY_POLICY.md` publicly and link in Play Console
- [ ] Store listing screenshots + short/full description (`PLAY_STORE_PREP.md`)
- [ ] Declare `specialUse` FGS only if always-on remains; recommend default boot+manual for review
- [ ] Real branded launcher icon (current mipmaps are placeholders)
- [ ] Device QA: install test CA → scan → alert → accept / remove via Settings

### P1 polish
- [ ] One-time Pro (RevenueCat): export already free for now — later gate export/history retention/widget
- [ ] Home-screen widget: “last scan / baseline count”
- [ ] F-Droid metadata + reproducible build notes
- [ ] Optional local denylist of known bad CA fingerprints (bundled, offline updates)

### P2 growth (non-SOP)
- [ ] Privacy blogger / GrapheneOS / security Twitter one-pager
- [ ] Demo GIF: install user CA → instant alert
- [ ] Compare page vs “just use Settings” (speed + continuous awareness)

## Pricing sketch (when ready)

| Tier | Price | Includes |
|------|-------|----------|
| Free | $0 | Baseline, alerts, boot/manual scan, detail, open settings, accept/dismiss |
| Pro (one-time) | $4.99–$9.99 | Long history, export templates, widget, themes, tip jar alternative |

Do **not** use $4.99/mo subscription for a passive sensor app — retention will not support it.

## Success metrics (Path A)

- Installs from privacy communities (not mass CAC ads)
- Uninstall rate & crash-free sessions
- % users who complete onboarding
- Optional: Pro conversion if/when paywall added
- **Not** targeting $10K/mo AI SOP metrics unless product expands (Path B)
