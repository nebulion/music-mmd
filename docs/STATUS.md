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
| Package | `com.musicmmd` (installs beside MonoMusic), app name "Music". Code package is still `com.calmapps.calmmusic` |
| Build | AGP 8.2, Kotlin 1.9.10, Gradle 8.11.1, compileSdk 35, minSdk 29. Release is debug-signed |
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
| 3 | Artist search, artist pages with Albums and Singles | — |
| 4 | Wireframes (owner picks) | — |
| 5 | Kompakt treatment (MMD kit, paging, type, no motion) | — |
| 6 | Speed (R8, profiles, one paint) | — |

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

## Owner's decisions
- 2026-10-07: fork MonoMusic only; CalmMusic compared and not used.
- 2026-10-07: full Kompakt treatment, like Macros and Fit.
- 2026-10-07: the "Now Playing" text button in the top right should become some other control — options
  go in the Phase 4 wireframes.
