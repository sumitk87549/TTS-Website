# Commercial-Grade UX Overhaul — words2voice

## Summary
A comprehensive pass over the entire frontend to elevate the site from a functional prototype to a premium, India-inspired commercial product. Covers layout, UX fixes, new features, and a full visual redesign.

---

## Grouped Change List

### 1. Layout — Full-width Header / Body / Footer
**Files:** `landing.component.scss`, `dashboard.component.scss`, `styles.scss`

- Remove all `max-width` constraints from `.navbar .nav-inner` and `.footer .footer-inner` (currently capped at 1200 px with side padding causing gaps).
- Set `width: 100%` with `padding: 0 2rem` on the containers so they genuinely stretch edge-to-edge.
- Apply same treatment to hero, demo, features, use-cases, CTA, and footer.

---

### 2. MAJOR BUG — Audio Not Appearing Until Theme Toggle
**Files:** `landing.component.ts`, `studio.component.ts`

Root cause: Angular's change detection isn't running because `audioUrl` is set inside a non-Angular callback (`window.URL.createObjectURL` result assigned synchronously to a property, but the subscribe callbacks complete outside the zone in some builds).

**Fix:** Inject `NgZone` and wrap the assignment inside `ngZone.run(() => { this.audioUrl = url; })` in both components' subscribe `next` callbacks. This forces a change-detection cycle immediately after the blob URL is ready.

---

### 3. Logo — Replace Emoji with Real Image
**Files:** All nav/header locations, `public/` folder

- Generate a proper SVG/PNG logo for "words2voice" (microphone + sound-wave, India-flavoured saffron-white-green palette) using `generate_image`.
- Copy into `frontend/public/logo.png`.
- Replace every `🎙️` emoji logo occurrence with `<img src="logo.png" …>` in landing, dashboard header, login/signup pages.

---

### 4. Top-Left Icon → Always Homepage Link
**Files:** `dashboard.component.html`, `landing.component.html`, `login.component.html`, `signup.component.html`

- Ensure sidebar brand link is `routerLink="/"` (not `/studio`).
- Already correct on landing; fix dashboard + auth pages.

---

### 5. Back Buttons Fixed on All Pages
**Files:** `about.component.html`, `contact.component.html`, `features/auth/login/login.component.html`, `features/auth/signup/signup.component.html`

- Change `routerLink="/"` back-links to use Angular `Location.back()` via `(click)="goBack()"` in the component class (so they navigate to the actual previous page, not always `/`).
- Add `Location` injection to the affected component TypeScript files.

---

### 6. Register Button beside Login
**Files:** `landing.component.html`, `login.component.html`

- In landing navbar `nav-actions`: add `<a routerLink="/signup" class="nav-btn btn-accent">Register</a>` right after the "Log In" button.
- In mobile menu: add Register link.
- In login page auth-footer: add Register link alongside existing Sign-up link.

---

### 7. About Page — Add Developer Photo
**Files:** `about.component.html`, `about.component.scss`

- Copy `DP.png` to `frontend/public/dp.png`.
- Add a styled circular avatar card in the hero section of the About page:
  ```html
  <img src="dp.png" class="dev-avatar" alt="Developer" />
  ```
- Style with `border-radius: 50%`, saffron/violet glow ring, responsive sizing.

---

### 8. Dashboard Sidebar — Remove Credits & Today's Usage
**Files:** `dashboard.component.html`, `dashboard.component.scss`

- Delete the entire `.usage-card` block (the "Today's usage" progress bar in `sidebar-bottom`).
- Remove Library and Voices from sidebar nav.
- Sidebar nav will only show: **Studio**, **History**, **Settings**.

---

### 9. Replace "✨ Try an example" with "✨ Fully Free"
**Files:** `studio.component.html`

- Change button label from `✨ Try an example` to `✨ Fully Free`.
- Change `(click)` from `tryRandomPreset()` to `null`/tooltip; repurpose as a static badge showing the service is free (no charge).

---

### 10. Voice Quality — Ultra by Default
**Files:** `studio.component.ts`

- Change `selectedQuality = this.qualityPresets[1]` (Standard) to `selectedQuality = this.qualityPresets[3]` (Ultra).

---

### 11. Voice Preview Play Button (Hindi Greeting TTS)
**Files:** `studio.component.html`, `studio.component.ts`, `studio.component.scss`

- Add a small `▶` play button to each voice card in the list.
- On click: call `/public/tts/preview` with a short Hindi greeting and the selected voice's `engine_voice_id`.
- Show a loading spinner on the button while generating; play audio inline via `Audio()` once ready.
- Track `previewLoadingId: string | null` to show spinner on the right card.

---

### 12. More Colors — Expanded Palette
**Files:** `styles.scss`

Add to `:root` and `[data-theme="day"]`:
- Saffron / marigold accent: `--accent-saffron: #FF9933`
- Teal: `--accent-teal: #00C9A7`
- Rose-gold: `--accent-rose: #E8608A`
- Warm amber: `--accent-amber: #FFBB00`

Apply these across components for variety (e.g., quality buttons, badges, use-case icons).

---

### 13. Settings — Font Visibility & Editable Inputs
**Files:** `settings.component.scss`, `settings.component.html`

- Fix `h3` color: change from `var(--text-muted)` to `var(--text-secondary)` or `var(--text-primary)`.
- Fix `label` color: `var(--text-primary)`.
- Remove the `.readonly` wrapper that hides pointer events — ensure non-`disabled` inputs are fully interactive.
- Increase input font-size to `1rem`.

---

### 14. Profile Image + Emoji/Icon Chooser
**Files:** `settings.component.html`, `settings.component.ts`, `settings.component.scss`, `dashboard.component.html`, `dashboard.component.scss`

- In Settings, add an "Avatar" section:
  - Grid of 12 emoji/icon options (🧑‍💻 🎤 🦚 🌸 🎵 🐯 🦋 🌟 🎨 🏔️ 🧡 💫).
  - A small color-picker for a basic background tint.
  - Saved to `localStorage` as `w2v-avatar` and `w2v-avatar-color`.
- In Dashboard sidebar header, replace the text brand with the chosen emoji rendered in a circle with the chosen color.
- In sidebar, add a hover dropdown on the avatar that links to `/settings`.

---

### 15. Animated People Icons for Voices (instead of first letter)
**Files:** `studio.component.html`, `studio.component.scss`

- Replace the single-letter `.avatar` div with an animated SVG figure.
- Male voices: blue-toned SVG silhouette (standing figure + sound-waves).
- Female voices: pink-toned SVG silhouette.
- Add a subtle CSS pulse animation on hover.

---

### 16. Full Visual/Theme Upgrade — India-Inspired
**Files:** `styles.scss`, `landing.component.scss`, `landing.component.html`

- Add India-inspired geometric background SVG patterns (mandala/rangoli motifs as CSS `background-image` data URIs) to hero section.
- Add warm gradient overlay with saffron → violet color stops.
- Upgrade hero to a split layout: text left, animated waveform right.
- Replace plain white CTA cards with glassmorphism treatment + Indian art pattern border.
- Landing navbar: sticky with frosted glass that changes opacity on scroll.
- Add `@keyframes` float/pulse animations to feature cards and hero elements.
- Footer: richer design with decorative border, social links area.

---

### 17. Better Responsive Buttons/Links
**Files:** Global, multiple component SCSSes

- Increase button padding, add `letter-spacing`, hover `translateY(-2px)` and glow shadows.
- Add `transition: all 0.25s cubic-bezier(0.4,0,0.2,1)` for premium feel.
- Make navbar CTA buttons pulse gently on first load.

---

## Files to Modify

| File | Change |
|------|--------|
| `styles.scss` | Expanded palette, new keyframes, full-width resets |
| `landing.component.html` | Full-width layout, Register btn, India-themed hero, logo img |
| `landing.component.scss` | Full-width, India motifs, better buttons |
| `landing.component.ts` | NgZone fix for audio |
| `dashboard.component.html` | Remove usage card + Library/Voices, logo → `/`, avatar display |
| `dashboard.component.scss` | Avatar styles, sidebar cleanup |
| `dashboard.component.ts` | Avatar localStorage logic |
| `studio.component.html` | "Fully Free" badge, animated voice avatars, play buttons, Ultra default |
| `studio.component.ts` | Ultra default quality, NgZone fix, voice preview logic |
| `studio.component.scss` | Animated avatars, play button styles |
| `about.component.html` | Dev photo, back button fix |
| `about.component.scss` | Avatar styling |
| `about.component.ts` | Add Location injection |
| `settings.component.html` | Fix fonts, avatar chooser, color picker |
| `settings.component.scss` | Fix font/input visibility |
| `settings.component.ts` | Avatar save/load logic |
| `login.component.html` | Register link, back button fix |
| `public/` | Copy logo.png, dp.png |

## Files to Create (New)
| File | Purpose |
|------|---------|
| `frontend/public/logo.png` | Real logo image |
| `frontend/public/dp.png` | Developer photo (symlinked from root DP.png) |

---

## Open Questions

> [!IMPORTANT]
> **Logo**: I'll generate a microphone + sound-wave India-themed logo image. Is there a specific style you prefer (minimal flat, 3D, saffron/violet tones)?

> [!NOTE]
> **Back Button Behavior**: Some "back" links (e.g., on About page) currently always go to `/`. Changing them to `Location.back()` means they go to the actual previous page. This is better UX — confirm you're happy with this.

> [!NOTE]
> **Sidebar simplification**: After removing Library + Voices, the sidebar nav will only show **Studio**, **History**, **Settings**. History is currently a stub — should I hide it too, leaving just Studio + Settings?

> [!CAUTION]
> **NgZone audio fix**: The major audio bug (audio not appearing until theme toggle) will be fixed by running the URL assignment inside `NgZone.run()`. This is the standard Angular fix for callbacks that originate outside Angular's zone. No backend changes needed.
