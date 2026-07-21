# words2voice — UI/UX blueprint

Grounded in your actual codebase: Angular standalone components (`studio`, `dashboard`, `projects`, `voice-lab`, `history`, `settings`), the Spring Boot API (`VoiceController`, `UsageController`, `InterestController`, `PublicStatsController`), and the Supertonic-3 service (10 voices, diffusion steps 4/8/16/32 for Draft/Standard/High/Ultra). A working mockup of the redesigned Studio is attached alongside this doc — open it and click through the theme toggle, the device-width toggle, the voice previews, and Generate.

---

## Part 1 — The promise: what to say, what not to say

You're pre-revenue, running one open-weights model, with 3 users and 8 generations in the database right now. That's not a weakness to hide — it's a story to tell honestly. The "Private & Secure / 100% Free / Natural Hindi / Instant Results" cards on your landing page already point the right direction. The main risk is language that quietly promises more than an MVP running one community model can deliver.

**Reframe as beta, on purpose.** Right now nothing in the UI signals "early." A small `BETA` or `Public beta` pill next to the logo (nav + Studio topbar) does two things at once: it lowers the bar for forgiving rough edges, *and* it makes "free" read as a genuine offer instead of a bait-and-switch setup. Pair it with a one-line honest note somewhere reachable (About page, or a footer line): *"words2voice runs on Supertonic-3, an open-source voice model. We're a small team learning what Indian creators actually need — your free usage is what teaches us."* That's more trust-building than any polish pass.

**Audit words that over-promise:**

| Current / tempting copy | Why it's risky | Safer alternative |
|---|---|---|
| "Professional voiceovers" | Sets a bar the model may not hit for every script | "Ready-to-use voiceovers" or "Natural-sounding voiceovers" |
| "Studio-grade (slow)" as an Ultra quality label | Fine as an internal tier name, but don't repeat "studio-grade" elsewhere as a blanket promise | Keep the label, don't echo it in hero copy |
| Implying unlimited/free forever anywhere | You'll want premium models later | "Free during beta" / "Free while we're testing" (you already use this in Settings — extend it everywhere) |
| Landing page stat bar showing "3 users / 8 voices generated" | Reads as *unproven*, not *impressive*, at this scale | Hide the stats section below a threshold (e.g. `totalUsers > 50`), or replace with a static, honest line: "Currently in public beta — built in the open" |
| Silent about data usage | "Private & Secure" card claims scripts "stay yours" | Only ship that claim once it's actually true end-to-end (no logging of raw script text, no third-party analytics on input) — verify before it goes further, this is a promise people will hold you to |

**Don't compare to ElevenLabs/Desivocal by name, ever** — not in copy, not in code comments that could leak, not in support replies. Let the product speak for itself: "natural Hindi voice, instantly, for free" is a complete pitch on its own.

**The freemium story, told early.** You plan to keep Supertonic-3 free long-term and add premium models later. Say that *now*, briefly, so early users don't feel ambushed later: a single line under the pricing-adjacent parts of the app (Settings → Feedback section is a good spot, you already have it half-built) — *"This voice model (Supertonic-3) stays free. We're exploring paid premium voices for the future — tell us if that'd be useful to you."* This is honest, sets expectations, and turns your existing `InterestController` research card into forward-facing product communication instead of just a silent survey.

---

## Part 2 — Information architecture: cut the redundancy first

This is most of why the dashboard feels "confusing and overwhelming" before you even touch the Studio page. Right now the sidebar has five items, and two pairs of them do almost the same job:

- **Voice Lab** (`/voice-lab`) shows the same voice cards (avatar, name, style tag) as the **Studio**'s voice picker — just without the ability to actually use them for anything.
- **Projects** (`/projects`) project-detail view renders literally the same `history-list` markup as the standalone **History** (`/history`) page.

Two nav items that duplicate what's already reachable elsewhere is cognitive tax with no payoff. Cut the sidebar from 5 items to 4, and give each the surviving one a distinct job:

| Before (5 items) | After (4 items) | What changes |
|---|---|---|
| Studio | **Studio** | Unchanged as the entry point — becomes the true home screen after login |
| Voice Lab | **Voices** | Same route, but now it's the *only* place with full voice detail (sample preview, full description) — Studio's picker becomes a lighter-weight shortcut that links here for "hear them all first" |
| Projects | **Library** | Merge Projects + History into one screen: a flat, filterable list of every generation, with a project filter dropdown (default "All") and a lightweight "New project" affordance. Kills the duplicate list markup in both `projects.component.html` and `history.component.html` |
| History | *(folded into Library)* | — |
| Settings | **Settings** | Unchanged |

This is a genuinely small code change (delete one route, merge two Angular components' logic, keep the DB schema exactly as-is — `project_id` on `generation` already supports "filter by project" as a client-side toggle on one list) with an outsized reduction in perceived complexity.

**Top bar, decluttered.** The current Studio top bar has four peer-weight controls fighting for attention: usage chip, script-preset `<select>`, project `<select>`, Single/Dialogue toggle. Keep the usage chip (it's useful, keep it visible always). Turn "Script Presets" into something less form-like — a single ghost button that reads "✨ Try an example" and cycles a random preset into the textarea on click, rather than a raw dropdown asking the user to parse six labels before they've typed anything. Turn the project selector into a quiet text button ("No project ▾") that only matters once someone has projects — most first-session users have none, so it shouldn't visually compete with the things they actually need on day one. "Dialogue" is disabled/coming-soon — either hide it entirely until it ships, or keep it as a single small "Multi-speaker (soon)" badge rather than a toggle button that looks clickable but isn't.

---

## Part 3 — Studio redesign (the core of this brief)

### Diagnosis

Looking at `studio.component.html`: Language, Speed, and Quality are rendered as three parallel control groups of identical visual weight, directly below the text editor, always fully expanded. Alongside them, a five-voice list with tabs sits in its own column. Nothing in the layout tells the eye *what matters first*. A brand-new visitor sees roughly nine interactive control clusters before they've written a word — that's the overwhelm, not any single control being badly designed.

The fix isn't "add more polish to every control." It's **progressive disclosure**: only the controls that matter to *most people, most of the time* stay visible by default. Everything else collapses until asked for.

### The core loop, and what that implies

The actual task is always: **write → pick a voice → generate.** Language, speed, and quality are refinements a minority of sessions need to touch — and you already ship sensible defaults for all three (Auto language, 1.0× speed, Standard quality). That's the key insight: *your defaults are already good. The UI just doesn't trust them.* Once the UI stops showing every dial as equally mandatory, the perceived complexity drops even though no functionality is removed.

### New layout

**Desktop (≥1024px) — two columns, editor-led:**

```
┌───────────────────────────────────────────┬───────────────┐
│  usage chip                 project ▾      │   VOICE       │
├───────────────────────────────────────────┤   (audition   │
│                                             │   panel —     │
│   [ Script ]                    12 / 1000  │   All/M/F     │
│   ┌───────────────────────────────────┐   │   tabs,       │
│   │  rotating example placeholder      │   │   avatar +    │
│   │                                     │   │   name + tag  │
│   └───────────────────────────────────┘   │   + a ▶ sample │
│                                             │   button per   │
│  ⚙ Voice settings   Auto · 1.0× · Standard ▾│   voice, and   │
│  (collapsed — expands to Language/Speed/    │   the selected │
│   Quality pills only when tapped)           │   voice's full │
│                                             │   one-line     │
│   [ result / waveform once generated ]     │   description  │
├───────────────────────────────────────────┴───────────────┤
│   Vihaan · Standard · 1.0×          [ ▶ Generate audio ]   │
└─────────────────────────────────────────────────────────────┘
```

**Mobile (<640px) — single column, bottom tab bar replaces the sidebar drawer:**

The hamburger-drawer pattern (current `dashboard.component.html`) adds a tap-and-wait step every time someone wants to switch sections, which is worse for a tool people return to constantly. Bottom tabs (Studio / Voices / Library / Settings, icon + label, ~56px tall) are reachable with a thumb in one tap. The voice panel drops below the editor as a horizontally-scrolling strip of compact avatar cards instead of a full list — see it live in the attached mockup by clicking **Mobile** in the preview toolbar. Generate stays pinned to the bottom, just above the tab bar, always reachable without scrolling.

### Component specs

**Script editor.** Keep the textarea exactly as sized today (it's fine), but make the placeholder text *rotate* through 3–4 short Hindi/Hinglish/English examples every few seconds when empty and unfocused (the mockup does this). This teaches the "you can mix Hindi and English" capability by demonstration instead of a static line of instructions nobody reads. Character counter and progress bar: keep the current green→amber→red behavior, it already works well.

**Voice settings, collapsed by default.** Language, Speed, and Quality move into a single accordion row: a gear icon, the label "Voice settings," and three small chips showing the *current* values (`Auto · 1.0× · Standard`) so the state is always visible without being expanded. Tapping the row expands the same three control groups you have today — no new interaction pattern to learn, just deferred until wanted. For Quality specifically, add a rough time estimate next to each tier (`~2s`, `~4s`, `~8s`, `~15s` — pull real numbers from your own generation logs once you have enough data) so "Ultra (slow)" becomes a concrete tradeoff instead of a vague warning.

**Voice picker → voice *audition*.** This is the one place worth a genuinely new interaction, because your product's whole value is *how something sounds*, and right now voice selection is a silent list of names. Each voice card gets a small "preview" play button that plays a short stock sample line in that voice (record ~10 one-time sample clips, one per voice, in advance — this does not touch your daily-quota system at all, since it's static pre-rendered audio, not a live generation call). While it plays, the play icon becomes a tiny animated waveform — you already defined `@keyframes soundwave` in `styles.scss` and never used it; this is the moment to. Below the list, show the *selected* voice's full description (the `description` field already exists in your `VOICE_CATALOGUE` in `main.py` — it's just not selected by `VoiceController`'s SQL query or exposed in the `Voice` interface in `studio.component.ts` today). One field you're already storing, currently invisible in the UI, does a lot of work here: *"Vihaan — youthful, upbeat voice with a casual, energetic delivery"* tells someone far more than the tag "M5" ever could.

**Generate bar.** Keep it sticky at the bottom exactly as today — that part already works. Simplify the meta text to voice · quality · speed (drop the separators-heavy style if it feels busy) and keep the button disabled state, but per Anthropic's own interface-writing convention worth borrowing: avoid disabling controls silently where you can instead explain briefly why (e.g., a small `Add some text first` hint that appears near the button rather than just a grayed-out button with no explanation).

**Result panel.** Keep the waveform-style loading state (soundwave animation, which you've already built). Add two small affordances once audio is ready: a "Regenerate" ghost button (same settings, re-roll) sitting next to Download, and — if the person has picked a project — a "Save to [project name]" inline action, so they don't have to leave the Studio to organize their work.

**First-run guidance.** A dismissible strip above the editor, shown once per new account: *"1 Write or paste your script · 2 Preview a voice, then pick one · 3 Generate — download or save it."* Lightweight, in-flow, not a blocking modal tour. Store the dismissal in `localStorage` or a `has_seen_studio_intro` flag on the user record — either is fine at this scale.

---

## Part 4 — Landing page

Structurally this is already solid: hero with a live try-it-now demo, a "why us" grid, use-case cards, a CTA, footer. Three concrete adjustments:

1. **Cold-start stats.** `PublicStatsController` currently powers a stats bar showing raw counts. At single-digit numbers, hide the section (`*ngIf="totalUsers > 50"` or similar threshold) rather than show a number that undersells you. Bring it back once it's a number worth bragging about.
2. **Anonymous try-it-now safety.** The embedded demo on the landing page hits your generation pipeline without login. Rate-limit it separately from the authenticated daily quota (e.g., by IP, a handful of characters per hour) so it can't be used to abuse your GPU/CPU time for free, and so a real visitor never sees a confusing quota error on their very first interaction with the product.
3. **Beta badge in the hero.** Small, next to or below the existing "Free Hindi TTS · Open Source" pill: `Public beta`. Costs nothing, buys you enormous goodwill the first time a voice sounds a little off.

---

## Part 5 — Auth pages (Login / Signup)

The split-screen pattern (gradient panel + form) is a fine, proven layout — keep the structure. Two small notes:

- The current gradient (violet → purple → magenta) is a very common "AI startup" look. It's not wrong, but it's also not distinctly *yours* — nothing about it says "Indian creators, Hindi voice, warmth." Section 6 below proposes a small, low-risk way to make the brand feel more your own without a full repaint.
- Copy check: "Start generating professional Hindi voices for free" on Signup — swap "professional" per the Part 1 guidance, e.g. *"Start creating natural Hindi voiceovers — free."*

---

## Part 6 — Design system additions

Your `styles.scss` token system (CSS custom properties, `[data-theme="night"|"day"]`) is genuinely well-built already — semantic naming, sensible shadow/blur scales, several keyframes defined but under-used. Build on it rather than replacing it.

**Color — add one functional accent, don't rebrand.** Keep `--accent` (`#6c5ce7`) exactly as-is; it already carries brand equity across landing, auth, and the dashboard. Add a second, warmer accent reserved *only* for "preview / listen" actions — a marigold/saffron tone (`#f5a623` night / `#d97706` day) distinct from your existing `--warning`. This gives the interface a clear two-color grammar: **violet = commit** (Generate, primary buttons, selected states), **marigold = preview** (voice sample play buttons). It's a small, deliberate choice tied to the product's own subject matter (this is what makes the redesigned voice cards read as "an instrument to sample," not "a form to fill out") and it happens to nod at Indian visual culture without being a costume — used sparingly, on one control type only.

**Type — give JetBrains Mono a job.** It's already imported in `styles.scss` and, as far as the markup shows, unused. Assign it consistently to *technical/meta* text: character counters, voice IDs, timestamps, quality-tier time estimates, the generate-bar meta line. Inter stays for everything else. This reads as "a real audio tool" (most professional audio software mixes a technical mono face with a humanist UI face) at zero new-asset cost.

**Motion.** `pulse-glow`, `soundwave`, `fade-in`, `slide-up` are already defined and mostly idle. Use `soundwave` for voice preview + generating states (per Part 3). Use `fade-in` for the result panel appearing. Avoid adding anything beyond what's already defined — restraint here reads as confidence, not lack of effort.

**Accessibility.** Focus-visible outlines are already defined globally — good, keep them. Double-check contrast on Day mode: `--text-muted: #a1a1aa` on `--bg-surface: #ffffff` is borderline for small text (run it through a contrast checker on the smallest text using that pairing — the char-count and voice-tag text are the likely offenders). Respect `prefers-reduced-motion` for the soundwave/pulse animations specifically, since those are the ones most likely to run continuously.

---

## Part 7 — Dark/light mode: what's already right, and two gaps

`ThemeService` (signal-based, persisted to `localStorage`, toggled via `data-theme` attribute) is a clean, correct implementation — nothing to rebuild here. Two additions worth making:

1. **Respect the OS by default.** Today the hardcoded default is `night` for a first-time visitor. Read `window.matchMedia('(prefers-color-scheme: dark)')` when there's no stored preference yet, and fall back to that instead of always-night. Small change, and it means the first impression is never "wrong" for someone whose OS is in light mode.
2. **One toggle spot per breakpoint, always reachable.** You already have this right — mobile header icon + sidebar button. Once the sidebar becomes an icon rail (desktop) / bottom tab bar (mobile) per Part 2, keep exactly one theme toggle visible per breakpoint (rail footer on desktop, mobile header on small screens) rather than duplicating it in both places at once, to avoid two controls doing the same job simultaneously.

---

## Part 8 — Tutorials and in-product guidance

Concrete, low-effort additions, roughly in priority order:

1. **Studio first-run strip** (Part 3) — the single highest-leverage tutorial element, because it appears exactly where the confusion currently happens.
2. **Rotating placeholder examples** in the textarea — teaches "you can mix Hindi/English/Hinglish" by demonstration, not instruction.
3. **A short "Writing tips for Hindi/Hinglish scripts" disclosure**, collapsed under the editor or linked from the Script label — 4–5 bullet points such as: write numbers as words for more natural pacing (`"पचास"` reads more reliably than `"50"`), keep sentences under ~25 words for cleaner phrasing, punctuation drives pauses (a `।` or `.` is a real pause, use it), emoji and unusual symbols are best removed before generating. This is exactly the kind of practical, hard-won detail a first-time user has no way to know, and it costs you nothing to write down once.
4. **Voice Lab → "Voices" page becomes the audition room.** Once merged per Part 2, it's the natural home for "not sure which voice to pick? Preview them all here" — link to it from the Studio's voice panel header as a quiet text link, not a nav duplication.
5. **About page**: one short, honest section on how the free tier works today and what's planned (ties back to Part 1's freemium framing) — this doubles as an implicit FAQ and reduces support questions later.

---

## Part 9 — Mobile-specific patterns, summarized

| Pattern | Desktop | Mobile |
|---|---|---|
| Primary nav | Icon rail, left, always visible | Bottom tab bar, 4 icons + labels |
| Voice picker | Docked right column, vertical list | Horizontal-scroll strip above/below editor |
| Voice settings | Inline accordion | Same accordion, full-width |
| Generate | Sticky bottom of content column | Fixed to viewport bottom, above tab bar |
| Theme toggle | Rail footer | Header, always visible |

The attached mockup's **Mobile** toolbar button demonstrates this exact collapse — worth clicking through before starting implementation, since it's easier to feel than to read.

---

## Part 10 — Making room for premium, without building it yet

You don't need any pricing UI today, but a few small choices now save a rebuild later:

- Treat "Voice Engine" as a concept in the data model and UI *now*, even with only one option (Supertonic-3), so adding a second engine later is "add a card," not "redesign the picker." A single small label near the voice panel — `Voice engine: Supertonic-3` — costs nothing today and quietly future-proofs the UI.
- When you do introduce paid voices/models, reuse the exact visual language already established by the Quality tiers (small badge, muted by default) — a `PRO` badge in the same visual family as `Draft/Standard/High/Ultra` will feel like a natural extension of a pattern users already understand, not a new intrusive upsell system.
- Keep the `InterestController` feedback card in Settings exactly as built — it's a genuinely good, low-friction way to gather real willingness-to-pay signal before you build anything. Consider surfacing a one-line teaser of it ("Premium voices are being explored — share your thoughts") from the Studio's voice panel too, since that's the moment someone might actually wish for a different voice.

---

## Part 11 — Implementation map

| Recommendation | Files most affected |
|---|---|
| Merge Projects + History into "Library" | New/merged component; retire `projects.component.*` and `history.component.*` into one; `app.routes.ts` |
| Voice descriptions in the picker | `VoiceController.java` (add `description` to the SQL `SELECT`), `Voice` interface + template in `studio.component.ts`/`.html` |
| Collapsed "Voice settings" accordion | `studio.component.html`/`.scss` — wrap existing `controls-panel` in an accordion, no logic changes needed |
| Rotating placeholder examples | `studio.component.ts` (small interval), `studio.component.html` |
| Voice sample preview (audition) | New static audio assets (one short clip per voice), `studio.component.ts` for play-state, `voice-lab.component.*` for the merged "Voices" page |
| Icon rail (desktop) / bottom tabs (mobile) | `dashboard.component.html`/`.scss` — replace the drawer sidebar with a rail + a `@media` bottom-bar variant |
| `prefers-color-scheme` default | `theme.service.ts` — `loadTheme()` |
| Beta badge, honest-copy pass | `landing.component.html`, `dashboard.component.html` (topbar), `signup.component.html` |
| Cold-start stats hide | `landing.component.html` (`*ngIf` threshold), `PublicStatsController.java` unchanged |
| Anonymous demo rate limiting | Backend — wherever the unauthenticated try-it-now endpoint is wired (likely `TtsController.java` or `GenerationController.java`) |
| Marigold "preview" accent + mono for meta text | `styles.scss` tokens block only |

---

## Part 12 — Suggested order of work

**First (highest impact, lowest effort):**
1. Collapse Voice/Speed/Quality into the settings accordion — this alone fixes most of the "overwhelming" feeling.
2. Add the beta badge + copy pass from Part 1.
3. Hide the landing stats bar until the numbers are worth showing.
4. Wire the `description` field into the voice picker.

**Next:**
5. Merge Projects/History into Library; cut the sidebar to 4 items.
6. Rotating placeholder examples + first-run tips strip.
7. `prefers-color-scheme` default for theme.

**Later (needs new assets or more design time):**
8. Voice sample preview clips + the soundwave audition interaction.
9. Icon rail / bottom tab bar navigation shell.
10. Marigold accent + JetBrains Mono role assignment across the app.

---

Everything above is reflected in the attached interactive mockup (`words2voice-studio-redesign-mockup.html`) — it's a static prototype, not wired to your real API, but every layout, interaction, and copy decision described here is clickable in it: theme toggle, desktop/mobile switch, the collapsed settings accordion, voice preview, and the generate → result flow.
