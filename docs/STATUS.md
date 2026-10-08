# Music MMD — status

Last updated 2026-10-07. A fork of [MonoMusic](https://github.com/berendsliedrecht/MonoMusic) (GPL-3.0)
for the Mudita Kompakt: YouTube Music search and streaming without an account, plus local files.
Follows the shared docs in `C:\Users\Antonio\mmd\` (MY-UI-PREFERENCES, MMD-GUIDE, UI-PATTERNS, SPEED-LEARNINGS).

Read next: [ARCHITECTURE.md](ARCHITECTURE.md).

## Quick facts

| | |
|---|---|
| Fork | github.com/nebulion/music-mmd, branch `mmd` (CI: build, unit tests, release APK artifact) |
| Upstream | `upstream` remote = berendsliedrecht/MonoMusic |
| Package | `com.musicmmd`, app name **MonoMusic** (the original MonoMusic was uninstalled 2026-10-08 at the owner's request). Code package is still `com.calmapps.calmmusic` |
| Build | AGP 9.4, Kotlin 2.4.10, Compose BOM 2026.08, MMD 1.0.2, Gradle 9.7.1, compileSdk 37, minSdk 29. R8 release, debug-signed |
| Local compile | `bash tools/gradle-low.sh :app:testDebugUnitTest :app:assembleDebug` (gentle: 4 GB, 2 workers, low priority) |

## Device routine

- Hands off the phone: install only while `dumpsys window | grep mCurrentFocus` does not show `com.musicmmd`
  and `dumpsys telephony.registry` shows `mCallState=0`. Never launch, tap or screenshot unless asked.
- `adb install -r` keeps data. Never uninstall. After install: `adb shell cmd package compile -m speed -f com.musicmmd`.

## Phases

| # | Phase | State |
|---|---|---|
| 0 | Fork, Windows build fixes, CI, new app id | Done |
| 1 | Music keeps playing in the background | Done, **device check owed** |
| 2 | The app remembers the last search / album / artist | Done, **device check owed** |
| 3 | Artist search, artist pages with Albums and Singles | Done, **device check owed** |
| 4 | Wireframes (owner picks) | Done: 1A 2B 3A 4C |
| 5 | Kompakt treatment (MMD kit, paging, type, no motion) | Done, **device check owed** |
| 6 | Speed (R8, profiles, one paint) | Done, **not measured** |

### Phase 1 — what changed
- `PlaybackService`: `setWakeMode(C.WAKE_MODE_NETWORK)` + `WAKE_LOCK` — CPU and Wi-Fi stay awake while playing.
- Error recovery moved from the UI into the service (`PlaybackErrors.kt`, tested): network errors and
  expired URLs are retried with a fresh URL (1 s, 5 s, 20 s), then the song pauses — offline never skips
  through the queue. Unplayable videos and broken files are skipped, with a snackbar if the app is open.
- Stream URLs: NewPipe first, Piped only as fallback (`MonoMusic.resolveAudioUrl`).
- Swiping the app away from recents keeps music playing (it stopped before).
- The ±5 song precache window is kept by the service, so it works with the app closed.
- Android 13+ asks for the notification permission once (the Kompakt, Android 12, doesn't need it).

**Device check owed:** play a YouTube album with the screen off for 30+ minutes, and once with the app
swiped away. `adb shell dumpsys power | grep -i wake` should show the app's lock while playing.

### Phase 2 — what changed
- `data/UiStateStore.kt` (tested): last search (query, tab, results), the open album or artist and whether
  Now Playing was open are written to `files/ui_state.json` when the app leaves the screen and read back on
  return — after "Don't keep activities" or after the process was killed. Results show without a new search.
- The saved queue keeps each song's details, so YouTube songs played from search (not in the library) come
  back after a restart; before, they were dropped.
- When music kept playing while the app was gone, the app now takes the queue and position from the player
  instead of the older saved copy.
- Deviation from the plan: album/artist stayed as they were (not moved into route arguments); restoring them
  from the store gives the same result with a much smaller change to `MainActivity.kt`.
- Not done: the instrumented recreation test (needs an emulator; CI runs JVM tests only).

**Device check owed:** search an artist, open an album, press Home, open another app for a while, come back →
same album page, and Back returns to the same search results.

### Phase 3 — what changed
- It was the app, not YouTube Music: the artists search filter and artist pages exist, the app never asked.
- Search has an **Artists** tab (Songs · Albums · Artists · Local). Four tabs breaks MMD's 2–3 rule on purpose
  for now; the Phase 4 wireframes settle the layout.
- Tapping a YouTube artist opens its page: **Albums · Singles · Songs**. Albums and Singles are the full
  discography (the page's "More" lists), not just the ten on the artist page. Albums open as before.
- Song results now keep their artist and album ids (`SongUiModel.artistId` / `albumId`), ready for
  "tap the artist name" in the Phase 5 screens.
- `InnertubeArtistParser.kt`, tested against saved "radiohead" responses in `src/test/resources/innertube/`.
  If YouTube changes its format, these fixtures are the place to refresh.
- Not done: "more results" for searches (continuation pages) — moved to Phase 5 with paging.

**Device check owed:** search "radiohead" → Artists → Radiohead → Albums lists all studio albums (15) →
OK Computer plays; Singles tab shows singles and EPs.

### Phase 5 — what changed
- Toolchain now matches Fit and Macros: AGP 9.4, Kotlin 2.4.10, Compose BOM 2026.08, **MMD 1.0.2**, Gradle 9.7.1.
- Kit copied from Macros into `ui/kit/`: `MmdTheme` (system font, white surfaces), `MmdTokens`, `EinkRefresh`
  (flash every 12 taps, on), `PagedList`, `Rows`, `DashedDividerMMD`; new `ListRow` (fixed 76/64 dp rows,
  label Black 21 / supporting Medium 18) and `EinkSettings`.
- Every list pages instead of scrolling (`PagedList`, one paint per screen). Scrollbar shows only when a list
  runs past a page (MMD rule 1, as Anki and Fit; Macros has none — one rule for all apps is still open).
- Every font size is from MMD's scale (no ad-hoc sizes, nothing under 14 sp).
- **1A:** the playing strip (title · artist · play/pause) sits above the tabs on every screen; the pill is gone.
- **2B:** search tabs Songs · Albums · Artists; library matches lead Songs, marked ✓.
- **3A:** artist page shuffle is in the top bar, not a floating button.
- **4C:** Now Playing slider unchanged. Its spinners are gone: "Loading" appears under the controls after
  0.5 s; downloading shows a static icon (tap to cancel).
- Tapping the artist name on Now Playing (underlined when known) opens the artist page.
- Settings gets an **E Ink** tab: flash on/off and every 6/8/12/16/24 taps.
- External-player card and download bars are MMD (white card, black outline, static bar).

**Not done in Phase 5 (owner's call or later):**
- `MainActivity.kt` was not split into files: it is one composable sharing ~60 state variables, and splitting
  it is a refactor with no visible change. Worth doing before the next feature.
- Song rows still open their menu on long-press (MMD P6 says long-press is not a menu trigger; the owner's
  pattern is hold = select). Options: a ⋮ per row, or actions on Now Playing only.
- Search "more results" (continuation pages).

**Device check owed:** rows equal height and paging by the last row; the playing strip; flash every 12 taps;
Settings → E Ink.

### Phase 6 — what changed
- Release build: R8 + resource shrinking (APK 8.4 MB), keep rules for NewPipe/Rhino and jaudiotagger in
  `app/proguard-rules.pro`. Debug-signed, so it installs over itself.
- `profileinstaller` (compiled on install) and `windowDisablePreview` (no white starting window).
- Installed on the phone 2026-10-07 and AOT-compiled (`cmd package compile -m speed -f com.musicmmd`). Not opened.
- Not done: cold start (`am start -W`) and frames per screen change were not measured, because that means
  opening the app on the phone. No first-draw hold yet. **If streaming fails only in the release build,
  suspect R8 first** (try the debug APK).

### 2026-10-08 — one paint per screen change
Measured on the phone with `dumpsys gfxinfo`: a bottom-tab switch drew 3–4 frames; opening an album or
artist drew the bar first, then "Loading…", then rows.
- **Bars inside every page.** NavHost shows a new destination a frame or more after the back stack changes;
  the top bar, playing strip and tabs sat outside it and switched first. Each destination now draws its own
  Scaffold (`chrome(route) { … }` in `MainActivity.kt`), so bar, page and tabs swap together.
- **Pages open when their content is ready** (`PageCache.kt`, `openAlbum` / `openArtist` / playlist tap):
  album, artist and playlist content is loaded first, then the page opens complete. Over 0.5 s, a static
  "Loading" line shows on the current page meanwhile. Coming back draws from the cache, then refreshes
  quietly (repaints only if something changed). The open page's content is saved with the UI state, so it
  also draws at once after the process was killed.
- **First frame held** until the library is read and the player connected (ceiling 1.5 s); no white window.
- **Scrollbar column always present**; thumb and arrows decided while drawing (before: rows narrowed a frame
  after a page appeared).
- "Loading…" / "Searching…" texts only appear after 0.5 s (`DelayedText`).
- **Bug fixed:** "Error loading artist: the coroutine scope left the composition" — a cancelled load (page
  left, or reloaded on return) was shown as an error. Cancellation is no longer treated as a failure.
- **Now Playing on return:** the ViewModel connects to the player before the first frame and takes its song,
  position and play state; updates now follow player events immediately (was a 1–2 s poll). Checked on the
  phone: correct song, playing, right position the moment the app reopened.
- Now Playing has the standard MMD top bar and 3 dp rule (it had only a small back row).
- **More tab removed:** Downloads, Radio and Settings are in the ⋮ menu of the tab pages; 4 bottom tabs.
- **Finish downloading:** albums downloaded from YouTube show the download icon; it fetches the album's
  track list and downloads what's missing ("Album is complete" if nothing is).

Measured after: cold start 0.79 s (first frame complete), tab switch 2–3 frames (from 3–4) before the last
two fixes; not re-measured after them because the owner was using the phone.

### 2026-10-08 — artist pages, radio
- Artist pages always show the YouTube Music discography, also for library artists (matched by exact name).
  Albums on the phone replace their YouTube twins and open your files; library albums YouTube doesn't list are
  kept. Offline or no match: the library alone. Album rows not on the phone show ☁, like song rows.
- Radio is off (not in the menu; its code and services remain).

### 2026-10-08 — round 2 (picks 1A 2A 3A)
- **No download messages (1A):** an album's top-bar button goes Download → "7 left" (tap: Downloads) → ✓.
  "Download started", "Downloading N songs", "Album is complete" and "Can't play … Skipped" are gone. The
  remaining messages (rename, delete, errors) have an ✕ and sit above the playing strip and tabs.
- **Downloads page by album (2A):** "Downloading" (newest first, "7 of 12 · current song", ✕ cancels the
  album), then "Done" (newest first; "N failed · tap to retry"), then Clear.
- **Sorting (3A):** ⇅ on Songs, Albums, Artists and the artist page opens a sheet of sort keys; tapping the
  key in use flips its direction. Songs: Title, Artist, Album, Recently added. Albums: Title, Artist, Year,
  Recently added. Artists: Name, Songs, Recently added. Artist page: Year, Title, Most popular (YouTube's
  order). Remembered per page. Playing from Songs follows the order shown.
- **Top bar:** Downloads (with a count while downloading) and Settings are icons, no ⋮. Tab pages carry
  ⇅ ⌕ ⤓ ⚙ — four actions, one over MMD's three, by the owner's choice.
- **Settings, Kompakt style:** one page of rows (Music folders › · Rescan with its last result · Flash to
  clear ghosting · Flash every N taps → radio sheet); Music folders is its own page. Removed, now always on:
  "Complete albums with YouTube" (album pages always show the whole track list; a local album opens with
  what YouTube returns within 1.5 s, the rest fills in quietly) and "Include local music".

## Owner's decisions
- 2026-10-07: fork MonoMusic only; CalmMusic compared and not used.
- 2026-10-07: full Kompakt treatment, like Macros and Fit.
- 2026-10-07: the "Now Playing" text button in the top right should become some other control — options
  go in the Phase 4 wireframes.
- 2026-10-07, wireframes (`docs/mockups/music-options.html`):
  - **1A** — a playing strip (song · artist · play/pause) above the bottom tabs replaces the "Now Playing" pill.
  - **2B** — search has three tabs: Songs · Albums · Artists; library matches sit at the top of Songs (marked ✓),
    the Local tab goes away.
  - **3A** — artist page tabs Albums · Singles · Songs (as built); shuffle moves to the top bar.
  - **4C** — Now Playing keeps the slider and position updating every second (owner's choice, like Fit's rest timer).
- 2026-10-08: app name MonoMusic; old MonoMusic uninstalled (its data didn't matter). More tab → top-bar ⋮.
- 2026-10-08: round 2 picks 1A 2A 3A (sorting toggles direction on a second tap); Downloads and Settings as top-bar icons; Radio off; settings that should just be on are removed.
