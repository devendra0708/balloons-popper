# Play Store listing — Balloons Popper

Paste-ready copy and asset checklist for Google Play Console.

**App name:** Balloons Popper  
**Package ID:** `com.devendra.balloonspopper`  
**Category:** Games → Casual (or Education / Kids if available in your console flow)  
**Free:** Yes  
**Contains ads:** No  
**In-app purchases:** No

---

## Short description (max 80 characters)

Tap balloons, learn colors, numbers, letters & shapes — offline kids fun!

(78 characters)

---

## Full description

Pop colorful balloons and learn while you play!

Balloons Popper is a cheerful, offline game for little ones. Tap balloons before they float away, earn stars, and unlock new ways to play.

WHAT’S INSIDE
• Balloon Pop — classic tapping fun
• Color Pop — find the matching color
• Number Pop — pop balloons in order
• Letter Pop — find the letter shown
• Shape Pop — circles, stars, hearts, and more
• Speed Challenge — how many can you pop in time?

MADE FOR KIDS
• Bright, simple screens
• Short, calm play sessions
• Sound can be turned off anytime
• No ads
• No in-app purchases
• No account needed
• Works offline

Grown-ups: open the Grown-ups button on the home screen for privacy details and sound credits.

Have fun popping!

---

## Privacy policy URL

Play Console requires a **public https URL**.

Hostable page is ready at:

`docs/privacy.html`

Ways to publish it:
1. **GitHub Pages** — push repo, enable Pages on `/docs`, then use:
   `https://<your-github-user>.github.io/<repo>/privacy.html`
2. **Google Sites / Notion** — paste the same policy text and publish to web
3. Open the local file for review: `docs/privacy.html`

Until hosted, leave this blank in the console — submission will be blocked without it.

Suggested public title for the page: **Privacy Policy — Balloons Popper**

---

## Screenshots checklist

Take these on a real phone (or emulator) at **phone** resolution. Prefer portrait.

Minimum for Play: **2** phone screenshots. Aim for **4–8**.

Recommended shots:
1. **Home** — title, mascot, LET’S POP!
2. **Pick a Game** — mode list with colorful banners
3. **Balloon Pop** gameplay — balloons mid-air
4. **Color / Letter / Number Pop** — prompt badge visible at top
5. **Animal balloon** moment — animal hanging or falling
6. **Speed Challenge** — timer + score if visible

Tips:
- Hide notification bar if possible (fullscreen already helps)
- Avoid empty sky-only frames
- Do not overlay fake “#1 kids app” badges

## Store graphics (ready to upload)

Generated assets are in `store-assets/`:

| Asset | File | Size |
| --- | --- | --- |
| High-res icon | `store-assets/play-icon-512.png` | 512 × 512 |
| Feature graphic | `store-assets/feature-graphic-1024x500.png` | 1024 × 500 |

Source art is also kept as `play-icon-source.png` and `play-feature-graphic-source.png` if you want to tweak later.

### Feature graphic (required)
- Size: **1024 × 500** px
- File ready: `store-assets/feature-graphic-1024x500.png`

### App icon
- Play high-res icon: `store-assets/play-icon-512.png`
- In-app launcher still uses `ic_launcher_balloon.xml`

---

## Content rating & Families

In Play Console:
1. Complete the **IARC content rating** questionnaire (no violence, no user chat, etc.)
2. Target audience: include **ages under 13** if marketing as a kids app
3. Follow **Families / Designed for Families** prompts
4. Data safety form:
   - Data collected: **No** (for personal info / analytics / ads)
   - Note local prefs only if the form asks about on-device storage

---

## Upload package

Upload the signed Android App Bundle:

`app/build/outputs/bundle/release/app-release.aab`

Rebuild after code changes:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleRelease
```

Keep `balloons-popper-release.keystore` and `keystore.properties` backed up safely.

---

## Pre-submit checklist

- [ ] Privacy policy hosted + URL added
- [ ] Short + full description pasted
- [ ] At least 2 phone screenshots uploaded
- [ ] Feature graphic 1024×500 uploaded
- [ ] High-res icon 512×512 uploaded
- [ ] Content rating completed
- [ ] Target audience / Families answered
- [ ] Data safety form completed
- [ ] Release AAB uploaded to a testing track (Internal testing recommended first)
- [ ] Tested install from Play Internal testing on a real device
