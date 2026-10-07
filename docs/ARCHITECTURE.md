# Music MMD — architecture

Paths are under `app/src/main/java/com/calmapps/calmmusic/`.

## Pieces
- **`MonoMusicApplication.kt`** (class `MonoMusic`): singletons — 256 MB `SimpleCache` keyed by video id,
  InnerTube client, NewPipe resolver, precache manager, settings, download manager, `resolveAudioUrl`.
- **`PlaybackService.kt`**: Media3 `MediaSessionService` owning the only ExoPlayer. The whole queue lives in the
  player, so it advances on its own. YouTube items are `youtube.com/watch?v=<id>` URIs; a `ResolvingDataSource`
  swaps in a stream URL at load time. Errors are recovered here (`PlaybackErrors.kt`).
- **`YouTubePrecacheManager.kt`**: stream URLs cached 30 min, first 3 MB prefetched, for ±5 songs around the
  playing one (driven by the service) and the top search results.
- **`YouTubeMusicInnertubeClient.kt`**: own InnerTube (`WEB_REMIX`) client: song and album search, album browse
  (`MPREb_…`), Piped stream lookup.
- **`MainActivity.kt`**: one large composable `MonoMusic()` with most UI state, the NavHost, sheets and the Now
  Playing overlay. Talks to the service through a `MediaController`.
- **`MonoMusicViewModel.kt`**: library (Room), playback state mirrored from the controller once a second,
  queue snapshot saved to SharedPreferences (`NowPlayingStorage`).
- **`data/`**: Room (`Song`, playlists), settings, scanners. **`ui/`**: one file per screen.
