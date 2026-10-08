package com.calmapps.calmmusic

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.calmapps.calmmusic.data.MediaStoreSongs
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.calmapps.calmmusic.ui.AlbumDetailsScreen
import com.calmapps.calmmusic.ui.AlbumUiModel
import com.calmapps.calmmusic.ui.AlbumsScreen
import com.calmapps.calmmusic.ui.ArtistDetailsScreen
import com.calmapps.calmmusic.ui.ArtistsScreen
import com.calmapps.calmmusic.ui.DownloadsScreen
import com.calmapps.calmmusic.ui.MoreScreen
import com.calmapps.calmmusic.ui.NowPlayingScreen
import com.calmapps.calmmusic.ui.PermissionsOnboardingScreen
import com.calmapps.calmmusic.ui.ArtistResultUiModel
import com.calmapps.calmmusic.ui.PlaylistAddSongsScreen
import com.calmapps.calmmusic.ui.PlaylistDetailsScreen
import com.calmapps.calmmusic.ui.PlaylistEditScreen
import com.calmapps.calmmusic.ui.PlaylistItem
import com.calmapps.calmmusic.ui.PlaylistUiModel
import com.calmapps.calmmusic.ui.PlaylistsScreen
import com.calmapps.calmmusic.ui.RadioScreen
import com.calmapps.calmmusic.ui.SearchScreen
import com.calmapps.calmmusic.ui.SettingsScreen
import com.calmapps.calmmusic.ui.QueueScreen
import com.calmapps.calmmusic.ui.sortedArtistsFor
import com.calmapps.calmmusic.ui.sortedFor
import com.calmapps.calmmusic.ui.SortSheet
import com.calmapps.calmmusic.ui.SortPage
import com.calmapps.calmmusic.ui.SortStore
import com.calmapps.calmmusic.ui.MusicFoldersScreen
import com.calmapps.calmmusic.ui.SongUiModel
import com.calmapps.calmmusic.ui.SongsScreen
import com.calmapps.calmmusic.ui.kit.MmdTheme
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.bottom_sheet.SheetStateMMD
import com.mudita.mmd.components.bottom_sheet.rememberModalBottomSheetMMDState
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.calmapps.calmmusic.ui.kit.PagedList
import com.mudita.mmd.components.snackbar.SnackbarDurationMMD
import com.mudita.mmd.components.snackbar.SnackbarHostMMD
import com.mudita.mmd.components.snackbar.SnackbarHostStateMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app: MonoMusic
        @androidx.annotation.OptIn(UnstableApi::class)
        get() = application as MonoMusic

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 13+: without this the playback notification (and its controls) is hidden.
        // Asked once, on a fresh start only. The Kompakt (Android 12) never asks.
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            val prefs = getSharedPreferences("calmmusic_settings", MODE_PRIVATE)
            if (!prefs.getBoolean("asked_notifications", false)) {
                prefs.edit().putBoolean("asked_notifications", true).apply()
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }
        holdFirstDrawUntilReady()
        setContent {
            MmdTheme {
                MonoMusic(app)
            }
        }
    }

    /**
     * One paint on start (E Ink): the launcher stays on the panel (no starting window) and the
     * app's first frame is held until the library and last queue are read, so the screen arrives
     * complete instead of bar first, then "Loading", then rows. Never longer than [FIRST_DRAW_CEILING_MS].
     */
    private fun holdFirstDrawUntilReady() {
        // the same instance the composition gets from viewModel(): one per activity
        val viewModel = androidx.lifecycle.ViewModelProvider(this, MonoMusicViewModel.factory(app))[MonoMusicViewModel::class.java]
        val content = findViewById<android.view.View>(android.R.id.content)
        val start = android.os.SystemClock.uptimeMillis()
        content.viewTreeObserver.addOnPreDrawListener(
            object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val ready = viewModel.firstScreenReady ||
                        android.os.SystemClock.uptimeMillis() - start > FIRST_DRAW_CEILING_MS
                    if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                    return ready
                }
            },
        )
    }

    private companion object {
        const val FIRST_DRAW_CEILING_MS = 1500L
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoMusic(app: MonoMusic) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val activity = context as? Activity
    val appContext = context.applicationContext
    val searchScope = rememberCoroutineScope()
    val playlistScope = rememberCoroutineScope()
    val libraryScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostStateMMD() }

    val settingsManager = app.settingsManager

    val viewModel: MonoMusicViewModel = viewModel(factory = MonoMusicViewModel.factory(app))
    val playbackState by viewModel.playbackState.collectAsState()
    val downloadStatuses by app.youTubeDownloadManager.downloads.collectAsState()

    // Connected by the ViewModel before the first frame (see MonoMusicViewModel.connectController).
    val localMediaController by viewModel.controller.collectAsState()
    var lastCompletedDownloadUUIDs by remember { mutableStateOf<Set<String>>(emptySet()) }

    val externalMediaState by ExternalMediaRepository.mediaState.collectAsState()
    val showExternalControls = externalMediaState.hasActiveSession && playbackState.nowPlayingSong == null

    LaunchedEffect(downloadStatuses) {
        val currentCompletedDownloads = downloadStatuses
            .filter { it.state == YouTubeDownloadStatus.State.COMPLETED }

        val currentCompletedUUIDs = currentCompletedDownloads.map { it.id }.toSet()
        val newCompletedUUIDs = currentCompletedUUIDs - lastCompletedDownloadUUIDs

        if (newCompletedUUIDs.isNotEmpty()) {
            viewModel.refreshLibraryFromDatabase()

            val newSongIds = currentCompletedDownloads
                .filter { it.id in newCompletedUUIDs }
                .map { it.songId }

            newSongIds.forEach { songId ->
                viewModel.onSongDownloaded(songId, localMediaController)
            }
        }
        lastCompletedDownloadUUIDs = currentCompletedUUIDs
    }

    val includeLocalMusicState = settingsManager.includeLocalMusic.collectAsState()
    val localMusicFoldersState = settingsManager.localMusicFolders.collectAsState()
    val includeLocalMusic = includeLocalMusicState.value
    val localMusicFolders = localMusicFoldersState.value
    val completeAlbumsWithYouTubeState = settingsManager.completeAlbumsWithYouTube.collectAsState()
    val completeAlbumsWithYouTube = completeAlbumsWithYouTubeState.value
    var hasStorageAccess by rememberSaveable { mutableStateOf(MediaStoreSongs.hasReadPermission(context)) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasStorageAccess = granted }
    var hasCompletedPermissionsOnboarding by rememberSaveable {
        mutableStateOf(settingsManager.hasCompletedPermissionsOnboarding())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStorageAccess = MediaStoreSongs.hasReadPermission(context)

                val intent = activity?.intent
                val fromRadio = intent?.getBooleanExtra("FROM_RADIO_TUNER", false) ?: false
                if (fromRadio) {
                    if (viewModel.playbackState.value.isPlaybackPlaying) {
                        viewModel.togglePlayback(localMediaController)
                    }
                    intent?.removeExtra("FROM_RADIO_TUNER")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shouldShowPermissionsOnboarding by remember {
        derivedStateOf {
            !hasCompletedPermissionsOnboarding
        }
    }

    val playlistsViewModel: PlaylistsViewModel = viewModel(factory = PlaylistsViewModel.factory(app))

    val streamResolverLabel by app.streamResolverLabel.collectAsState()
    val addToPlaylistSheetState: SheetStateMMD = rememberModalBottomSheetMMDState(
        skipPartiallyExpanded = true,
    )
    val removeSongsSheetState: SheetStateMMD = rememberModalBottomSheetMMDState(
        skipPartiallyExpanded = true,
    )
    val deletePlaylistsSheetState: SheetStateMMD = rememberModalBottomSheetMMDState(
        skipPartiallyExpanded = true,
    )
    val renameAlbumSheetState: SheetStateMMD = rememberModalBottomSheetMMDState(
        skipPartiallyExpanded = true,
    )
    val editSongSheetState: SheetStateMMD = rememberModalBottomSheetMMDState(
        skipPartiallyExpanded = true,
    )

    val canNavigateBack = navController.previousBackStackEntry != null

    val librarySongs by viewModel.librarySongs.collectAsState()
    val librarySongIds = remember(librarySongs) {
        librarySongs.map { it.id }.toSet()
    }

    val libraryAlbums by viewModel.libraryAlbums.collectAsState()
    val libraryArtists by viewModel.libraryArtists.collectAsState()
    val libraryPlaylistsState by playlistsViewModel.playlists.collectAsState()
    val isLoadingSongs by viewModel.isLoadingSongs.collectAsState()
    val isLoadingAlbums by viewModel.isLoadingAlbums.collectAsState()

    var libraryPlaylists by remember { mutableStateOf<List<PlaylistUiModel>>(emptyList()) }
    var songsError by remember { mutableStateOf<String?>(null) }
    var albumsError by remember { mutableStateOf<String?>(null) }

    // Where the owner was before the app went to the background (see UiStateStore).
    val savedUi = remember {
        app.uiStateStore.load().also { ui ->
            // the page that was open draws straight from these after a restart
            val cache = app.pageCache
            ui.selectedAlbum?.let { a ->
                if (ui.openAlbumSongs.isNotEmpty() && cache.album(a.sourceType, a.id) == null) cache.putAlbum(a.sourceType, a.id, ui.openAlbumSongs)
            }
            ui.selectedArtistId?.let { id ->
                if ((ui.openArtistSongs.isNotEmpty() || ui.openArtistAlbums.isNotEmpty()) && cache.artist(id) == null) {
                    cache.putArtist(id, MonoMusicViewModel.ArtistContent(ui.openArtistSongs, ui.openArtistAlbums, ui.openArtistSingles))
                }
            }
            ui.openPlaylistId?.let { id ->
                if (ui.openPlaylistSongs.isNotEmpty() && cache.playlist(id) == null) cache.putPlaylist(id, ui.openPlaylistSongs)
            }
        }
    }

    var selectedAlbum by remember { mutableStateOf(savedUi.selectedAlbum) }
    var showRenameAlbumDialog by remember { mutableStateOf(false) }
    var renameAlbumText by remember { mutableStateOf("") }
    var renameAlbumArtistText by remember { mutableStateOf("") }
    var songToEdit by remember { mutableStateOf<SongUiModel?>(null) }
    var editSongTitle by remember { mutableStateOf("") }
    var editSongArtist by remember { mutableStateOf("") }

    var selectedPlaylist by remember { mutableStateOf<PlaylistUiModel?>(null) }
    var playlistAddSongsSelectionIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var isPlaylistsEditMode by remember { mutableStateOf(false) }
    var isPlaylistDetailsMenuExpanded by remember { mutableStateOf(false) }
    val playlistEditSelectionIds = remember { mutableSetOf<String>() }
    var playlistEditSelectionCount by remember { mutableStateOf(0) }
    var showDeletePlaylistsConfirmation by remember { mutableStateOf(false) }

    var isPlaylistDetailsEditMode by remember { mutableStateOf(false) }
    val playlistDetailsSelectionIds = remember { mutableSetOf<String>() }
    var playlistDetailsSelectionCount by remember { mutableStateOf(0) }
    var showDeletePlaylistSongsConfirmation by remember { mutableStateOf(false) }

    var selectedArtist by remember { mutableStateOf(savedUi.selectedArtist) }
    var selectedArtistId by remember { mutableStateOf(savedUi.selectedArtistId) }

    val playbackQueue = playbackState.playbackQueue
    val currentSongId = playbackState.currentSongId
    val nowPlayingSong = playbackState.nowPlayingSong
    var isPlaybackPlaying = playbackState.isPlaybackPlaying

    // The open album's download: songs still queued, and whether every song is on the phone.
    val albumDownload: Pair<Int, Boolean> = run {
        val a = selectedAlbum ?: return@run 0 to false
        val songs = app.pageCache.album(a.sourceType, a.id) ?: return@run 0 to false
        val ids = songs.mapTo(HashSet()) { it.id }
        val forAlbum = downloadStatuses.filter { it.songId in ids }
        val left = forAlbum.count { it.state == YouTubeDownloadStatus.State.PENDING || it.state == YouTubeDownloadStatus.State.IN_PROGRESS }
        val done = forAlbum.filter { it.state == YouTubeDownloadStatus.State.COMPLETED || it.state == YouTubeDownloadStatus.State.SKIPPED }
            .mapTo(HashSet()) { it.songId }
        val missing = songs.count { it.sourceType == "YOUTUBE" && it.id !in done }
        left to (left == 0 && missing == 0 && songs.isNotEmpty())
    }

    // Sorting (pick 3A): per page, remembered
    val sortStore = remember { SortStore(app) }
    var sorts by remember { mutableStateOf(SortPage.entries.associateWith { sortStore.get(it) }) }
    var sortSheetPage by remember { mutableStateOf<SortPage?>(null) }
    fun sortOf(page: SortPage) = sorts.getValue(page)

    // The artist page's shuffle, shown in the top bar (owner's pick 3A) once its songs are loaded.
    var artistShuffle by remember { mutableStateOf<(() -> Unit)?>(null) }

    var showNowPlaying by remember { mutableStateOf(savedUi.showNowPlaying) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<SongUiModel?>(null) }
    var pendingAddToNewPlaylistSong by remember { mutableStateOf<SongUiModel?>(null) }

    var searchQuery by remember { mutableStateOf(savedUi.searchQuery) }
    var searchSongs by remember { mutableStateOf(savedUi.searchSongs) }
    var searchAlbums by remember { mutableStateOf(savedUi.searchAlbums) }
    var searchArtists by remember { mutableStateOf(savedUi.searchArtists) }
    // all of YouTube, asked for per search (null = not asked yet)
    var searchVideos by remember { mutableStateOf<List<SongUiModel>?>(null) }
    var isSearchingVideos by remember { mutableStateOf(false) }
    var searchLocalSongs by remember { mutableStateOf(savedUi.searchLocalSongs) }
    var searchSelectedTab by remember { mutableStateOf(savedUi.searchTab) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    var isRescanningLocal by remember { mutableStateOf(false) }
    var localScanProgress by remember { mutableStateOf(0f) }
    var isIngestingLocal by remember { mutableStateOf(false) }
    var localIngestProgress by remember { mutableStateOf(0f) }
    var localScanTotalDiscovered by remember { mutableStateOf<Int?>(null) }
    var localScanSkippedUnchanged by remember { mutableStateOf<Int?>(null) }
    var localScanIndexedNewOrUpdated by remember { mutableStateOf<Int?>(null) }
    var localScanDeletedMissing by remember { mutableStateOf<Int?>(null) }

    val isLibrarySyncInProgress by remember {
        derivedStateOf { isRescanningLocal || isIngestingLocal }
    }
    val hasAnySongs by remember {
        derivedStateOf { librarySongs.isNotEmpty() }
    }

    var settingsSelectedTab by remember { mutableStateOf(0) }

    // ---- Opening pages in one paint -------------------------------------------------------
    // A page is opened only once its content is loaded (PageCache), so its top bar and rows are
    // drawn together. Nothing is shown while it loads (owner, 2026-10-08: no "Loading" messages).
    var openingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun openWhenReady(load: suspend () -> Unit, open: () -> Unit) {
        if (openingJob?.isActive == true) return
        openingJob = libraryScope.launch {
            try {
                load()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // left the screen meanwhile: open nothing
            } catch (_: Exception) {
                // the page shows the error itself
            }
            open()
        }
    }

    fun openAlbum(album: AlbumUiModel) = openWhenReady(
        load = {
            if (viewModel.cachedAlbumSongs(album) == null) viewModel.loadAlbumSongs(album, quick = true)
            if (settingsManager.showAlbumCovers.value) kotlinx.coroutines.withTimeoutOrNull(1500) { app.covers.load(album) }
        },
        open = {
            selectedAlbum = album
            navController.navigate(Screen.AlbumDetails.route) { launchSingleTop = true }
        },
    )

    fun openArtist(name: String, id: String, before: () -> Unit = {}) = openWhenReady(
        load = { if (viewModel.cachedArtistContent(id) == null) viewModel.loadArtistContent(id, name) },
        open = {
            before()
            selectedArtist = name
            selectedArtistId = id
            navController.navigate(Screen.ArtistDetails.route) { launchSingleTop = true }
        },
    )

    // Save where the owner is whenever the app leaves the screen.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val state = com.calmapps.calmmusic.data.UiState(
                    searchQuery = searchQuery,
                    searchTab = searchSelectedTab,
                    searchSongs = searchSongs,
                    searchAlbums = searchAlbums,
                    searchArtists = searchArtists,
                    searchLocalSongs = searchLocalSongs,
                    selectedAlbum = selectedAlbum,
                    selectedArtist = selectedArtist,
                    selectedArtistId = selectedArtistId,
                    showNowPlaying = showNowPlaying,
                    openAlbumSongs = selectedAlbum?.let { app.pageCache.album(it.sourceType, it.id) }.orEmpty(),
                    openArtistSongs = selectedArtistId?.let { app.pageCache.artist(it) }?.songs.orEmpty(),
                    openArtistAlbums = selectedArtistId?.let { app.pageCache.artist(it) }?.albums.orEmpty(),
                    openArtistSingles = selectedArtistId?.let { app.pageCache.artist(it) }?.singles.orEmpty(),
                    openPlaylistId = selectedPlaylist?.id,
                    openPlaylistSongs = selectedPlaylist?.id?.let { app.pageCache.playlist(it) }.orEmpty(),
                )
                // Not a composition scope: that is cancelled when the activity is destroyed.
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { app.uiStateStore.save(state) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun performSearch() {
        if (searchQuery.isBlank()) return

        val query = searchQuery.trim()
        searchLocalSongs = librarySongs.filter { song ->
            song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true) ||
                    (song.album?.contains(query, ignoreCase = true) == true)
        }

        if (isSearching) return

        searchScope.launch {
            isSearching = true
            searchError = null
            searchVideos = null
            searchSelectedTab = 0
            try {
                val songResults = app.youTubeInnertubeClient.searchSongs(
                            query = searchQuery,
                            limit = 25,
                        )
                        val albumResults = app.youTubeInnertubeClient.searchAlbums(
                            query = searchQuery,
                            limit = 25,
                        )
                        val artistResults = try {
                            app.youTubeInnertubeClient.searchArtists(query = searchQuery, limit = 25)
                        } catch (_: Exception) {
                            emptyList()
                        }
                        searchArtists = artistResults.map { ArtistResultUiModel(it.artistId, it.name, it.subtitle) }
                        searchSongs = songResults.map {
                            SongUiModel(
                                id = it.videoId,
                                title = it.title,
                                artist = it.artist,
                                durationText = formatDurationMillis(it.durationMillis),
                                durationMillis = it.durationMillis,
                                trackNumber = null,
                                sourceType = "YOUTUBE",
                                audioUri = it.videoId,
                                album = it.album,
                                artistId = it.artistId,
                                albumId = it.albumId,
                            )
                        }
                        searchAlbums = albumResults.map { album ->
                            AlbumUiModel(
                                id = album.albumId,
                                title = album.title,
                                artist = album.artist,
                                sourceType = "YOUTUBE",
                                releaseYear = album.year,
                                coverUrl = album.coverUrl,
                            )
                        }

                val topVideoIds = songResults.take(5).map { it.videoId }
                app.youTubePrecacheManager.precacheSearchResults(topVideoIds)
                // open on a tab that found something (songs first), in the same frame as the results
                searchSelectedTab = when {
                    searchSongs.isNotEmpty() || searchLocalSongs.isNotEmpty() -> 0
                    searchAlbums.isNotEmpty() -> 1
                    searchArtists.isNotEmpty() -> 2
                    else -> 0
                }
            } catch (e: Exception) {
                searchError = e.message ?: "Search failed"
                searchSongs = emptyList()
                searchAlbums = emptyList()
                searchArtists = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    suspend fun resyncLocalLibrary(
        includeLocal: Boolean,
        folders: Set<String>,
    ) {
        if (isRescanningLocal) return
        isRescanningLocal = true
        localScanProgress = 0f
        isIngestingLocal = false
        localIngestProgress = 0f
        localScanTotalDiscovered = null
        localScanSkippedUnchanged = null
        localScanIndexedNewOrUpdated = null
        localScanDeletedMissing = null
        songsError = null
        try {
            val stats = viewModel.resyncLocalLibrary(
                includeLocal = includeLocal,
                folders = folders,
                onScanProgress = { progress ->
                    localScanProgress = progress.coerceIn(0f, 1f)
                },
            )

            localScanTotalDiscovered = stats.totalFiles
            localScanIndexedNewOrUpdated = stats.addedOrUpdated
            localScanSkippedUnchanged = stats.totalFiles - stats.addedOrUpdated
            localScanDeletedMissing = stats.removed
        } finally {
            isRescanningLocal = false
            isIngestingLocal = false
        }
    }

    fun togglePlayback() {
        nowPlayingSong ?: return
        viewModel.togglePlayback(localMediaController)
        isPlaybackPlaying = !isPlaybackPlaying
    }

    fun startPlaybackFromQueue(
        queue: List<SongUiModel>,
        startIndex: Int,
        isNewQueue: Boolean = true,
    ) {
        if (queue.isEmpty() || startIndex !in queue.indices) return

        val song = queue[startIndex]
        val controller = localMediaController

        val needsLocalController = song.sourceType == "LOCAL_FILE" || song.sourceType == "YOUTUBE" || song.sourceType == "YOUTUBE_DOWNLOAD"
        if (needsLocalController && controller == null) {
            libraryScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Playback service is still starting. Please try again.",
                    withDismissAction = true,
                    duration = SnackbarDurationMMD.Short,
                )
            }
            return
        }

        viewModel.startPlaybackFromQueue(
            queue = queue,
            startIndex = startIndex,
            isNewQueue = isNewQueue,
            localController = controller,
        )

        showNowPlaying = true
    }

    fun startShuffledPlaybackFromQueue(queue: List<SongUiModel>) {
        if (queue.isEmpty()) return

        viewModel.startShuffledPlaybackFromQueue(
            queue = queue,
            localController = localMediaController,
        )

        showNowPlaying = true
    }

    fun addSongToPlaylist(song: SongUiModel, playlist: PlaylistUiModel) {
        playlistScope.launch {
            var snackbarMessage: String?
            try {
                val result = playlistsViewModel.addSongToPlaylist(song, playlist)
                if (result.newSongCount != null) {
                    val count = result.newSongCount
                    libraryPlaylists = libraryPlaylists.map { existingPlaylist ->
                        if (existingPlaylist.id == playlist.id) {
                            existingPlaylist.copy(songCount = count)
                        } else {
                            existingPlaylist
                        }
                    }
                }
                snackbarMessage = when {
                    result.wasAdded -> "Added \"${song.title}\" to \"${playlist.name}\""
                    result.alreadyInPlaylist -> "This song is already in \"${playlist.name}\""
                    else -> null
                }
                if (result.wasAdded || result.alreadyInPlaylist) {
                    showAddToPlaylistDialog = false
                }
            } catch (_: Exception) {
                snackbarMessage = "Couldn't add to playlist"
            }
            snackbarMessage?.let { message ->
                snackbarHostState.showSnackbar(
                    message = message,
                    withDismissAction = true,
                    duration = SnackbarDurationMMD.Short,
                )
            }
        }
    }

    val onAddToPlaylist: (SongUiModel) -> Unit = { song ->
        songToAddToPlaylist = song
        showAddToPlaylistDialog = true
    }

    val onRemoveFromLibrary: (SongUiModel) -> Unit = { song ->
        libraryScope.launch {
            try {
                viewModel.removeSongFromLibrary(song)
                snackbarHostState.showSnackbar(
                    message = "Removed from library",
                    withDismissAction = true,
                    duration = SnackbarDurationMMD.Short,
                )
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Failed to remove: ${e.message}",
                    withDismissAction = true,
                    duration = SnackbarDurationMMD.Short,
                )
            }
        }
    }

    val onDelete: (SongUiModel) -> Unit = { song ->
        libraryScope.launch {
            when (song.sourceType) {
                "YOUTUBE" -> {
                    val download = downloadStatuses.find { it.songId == song.id }
                    if (download != null) {
                        app.youTubeDownloadManager.cancelDownload(download.id)
                        snackbarHostState.showSnackbar(
                            message = "Deleting download...",
                            withDismissAction = true,
                            duration = SnackbarDurationMMD.Short,
                        )
                    }
                }

                "LOCAL_FILE", "YOUTUBE_DOWNLOAD" -> {
                    val success = try {
                        viewModel.deleteLocalMediaSong(song)
                    } catch (_: Exception) {
                        false
                    }

                    snackbarHostState.showSnackbar(
                        message = if (success) "Deleted file" else "Couldn't delete file",
                        withDismissAction = true,
                        duration = SnackbarDurationMMD.Short,
                    )
                }

                else -> {
                    snackbarHostState.showSnackbar(
                        message = "Cannot delete this source type",
                        withDismissAction = true,
                        duration = SnackbarDurationMMD.Short,
                    )
                }
            }
        }
    }

    LaunchedEffect(currentDestination) {
        if (currentDestination?.route == Screen.Search.route) {
            focusRequester.requestFocus()
        } else {
            app.youTubePrecacheManager.clearSearchWindow()
        }
        if (currentDestination?.route != Screen.Playlists.route && isPlaylistsEditMode) {
            isPlaylistsEditMode = false
            playlistEditSelectionIds.clear()
            playlistEditSelectionCount = 0
        }
        val isPlaylistDetails = currentDestination?.route == Screen.PlaylistDetails.route ||
                currentDestination?.route?.startsWith("playlistDetails/") == true

        if (!isPlaylistDetails && isPlaylistDetailsEditMode) {
            isPlaylistDetailsEditMode = false
            playlistDetailsSelectionIds.clear()
            playlistDetailsSelectionCount = 0
        }
    }

    LaunchedEffect(libraryPlaylistsState) {
        libraryPlaylists = libraryPlaylistsState
    }

    LaunchedEffect(localMediaController) {
        val controller = localMediaController ?: return@LaunchedEffect
        viewModel.startLocalPlaybackMonitoring(controller)

        // An unplayable song is skipped without a message (pick 1A: no snackbars).
        PlaybackService.registerSkipNotice(null)
    }

    if (shouldShowPermissionsOnboarding) {
        PermissionsOnboardingScreen(
            hasStorageAccess = hasStorageAccess,
            onRequestStorageAccessClick = {
                storagePermissionLauncher.launch(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                )
            },
            onContinueClick = {
                hasCompletedPermissionsOnboarding = true
                settingsManager.setHasCompletedPermissionsOnboarding(true)
            },
            onSkipClick = {
                hasCompletedPermissionsOnboarding = true
                settingsManager.setHasCompletedPermissionsOnboarding(true)
            },
        )
        return
    }

    LaunchedEffect(Unit) {
        songsError = null
        albumsError = null
    }

    LaunchedEffect(includeLocalMusic, localMusicFolders) {
        delay(500L)
        resyncLocalLibrary(includeLocalMusic, localMusicFolders)
    }

    val openStreamingSettings: () -> Unit = {
        // Streaming = 0, Local = 1
        settingsSelectedTab = 0
        navController.navigate(Screen.Settings.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openLocalSettings: () -> Unit = {
        settingsSelectedTab = 1
        navController.navigate(Screen.Settings.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun NavGraphBuilder.playlistsNavGraph(chrome: @Composable (String, @Composable () -> Unit) -> Unit) {
        composable(Screen.Playlists.route) {
 chrome(Screen.Playlists.route) {
            PlaylistsScreen(
                playlists = libraryPlaylists,
                isInEditMode = isPlaylistsEditMode,
                onPlaylistClick = { playlist: PlaylistUiModel ->
                    openWhenReady(
                        load = { if (playlistsViewModel.cachedPlaylistSongs(playlist.id) == null) playlistsViewModel.loadPlaylistSongs(playlist.id) },
                        open = {
                            selectedPlaylist = playlist
                            navController.navigate("${Screen.PlaylistDetails.route}/${playlist.id}") {
                                launchSingleTop = true
                            }
                        },
                    )
                },
                onAddPlaylistClick = {
                    selectedPlaylist = null
                    navController.navigate(Screen.PlaylistEdit.route) {
                        launchSingleTop = true
                    }
                },
                onSelectionChanged = { selectedIds ->
                    playlistEditSelectionIds.clear()
                    playlistEditSelectionIds.addAll(selectedIds)
                    playlistEditSelectionCount = selectedIds.size
                },
            )
        }
}

        composable(
            route = "${Screen.PlaylistDetails.route}/{playlistId}",
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) { backStackEntry ->
 chrome("${Screen.PlaylistDetails.route}/{playlistId}") {
            val playlistId = backStackEntry.arguments?.getString("playlistId")

            LaunchedEffect(playlistId, libraryPlaylists) {
                if (playlistId != null && (selectedPlaylist == null || selectedPlaylist?.id != playlistId)) {
                    val found = libraryPlaylists.find { it.id == playlistId }
                    if (found != null) {
                        selectedPlaylist = found
                    }
                }
            }

            PlaylistDetailsScreen(
                onShuffleAvailable = { songs ->
                    artistShuffle = songs?.let { { startShuffledPlaybackFromQueue(it) } }
                },
                playlistId = playlistId,
                playbackViewModel = viewModel,
                playlistsViewModel = playlistsViewModel,
                isInEditMode = isPlaylistDetailsEditMode,
                selectedSongIds = playlistDetailsSelectionIds.toSet(),
                onSongSelectionChange = { songId, isSelected ->
                    if (isSelected) {
                        playlistDetailsSelectionIds.add(songId)
                    } else {
                        playlistDetailsSelectionIds.remove(songId)
                    }
                    playlistDetailsSelectionCount = playlistDetailsSelectionIds.size
                },
                onPlaySongClick = { song: SongUiModel, songs: List<SongUiModel> ->
                    val index = songs.indexOfFirst { it.id == song.id }
                    val startIndex = if (index >= 0) index else 0
                    startPlaybackFromQueue(songs, startIndex)
                },
                onAddSongsClick = {
                    if (selectedPlaylist != null) {
                        playlistAddSongsSelectionIds = emptySet()
                        navController.navigate(Screen.PlaylistAddSongs.route) {
                            launchSingleTop = true
                        }
                    }
                },
                onShuffleClick = { songs ->
                    startShuffledPlaybackFromQueue(songs)
                },
                onAddToPlaylistClick = onAddToPlaylist,
                onRemoveFromLibraryClick = onRemoveFromLibrary,
                onDeleteClick = onDelete,
            )
        }
}
        composable(Screen.PlaylistAddSongs.route) {
 chrome(Screen.PlaylistAddSongs.route) {
            var existingIds by remember { mutableStateOf(emptySet<String>()) }
            LaunchedEffect(selectedPlaylist?.id) {
                val id = selectedPlaylist?.id
                if (id != null) {
                    try {
                        val songs = playlistsViewModel.getPlaylistSongs(id)
                        existingIds = songs.map { it.id }.toSet()
                    } catch (_: Exception) {
                    }
                }
            }
            val candidateSongs = librarySongs.filter { it.id !in existingIds }

            PlaylistAddSongsScreen(
                songs = candidateSongs,
                initialSelectedSongIds = playlistAddSongsSelectionIds,
                onSelectionChanged = { selectedIds ->
                    playlistAddSongsSelectionIds = selectedIds
                },
            )
        }
}
        composable(Screen.PlaylistEdit.route) {
 chrome(Screen.PlaylistEdit.route) {
            val editing = selectedPlaylist
            PlaylistEditScreen(
                initialName = editing?.name ?: "",
                isEditing = editing != null,
                onConfirm = { newName ->
                    val trimmed = newName.trim()
                    if (trimmed.isEmpty()) return@PlaylistEditScreen
                    playlistScope.launch {
                        var navigatedToDetails = false
                        var shouldPopBack = false
                        try {
                            val songToAdd = pendingAddToNewPlaylistSong
                            val editingPlaylist = editing

                            val result = playlistsViewModel.createOrUpdatePlaylist(
                                params = PlaylistsViewModel.EditPlaylistParams(
                                    playlistId = editingPlaylist?.id,
                                    name = trimmed,
                                    description = editingPlaylist?.description,
                                    songToAdd = songToAdd,
                                )
                            )

                            val playlists = playlistsViewModel.refreshPlaylists()
                            libraryPlaylists = playlists

                            val finalPlaylistId = result.playlistId
                            val targetPlaylist = libraryPlaylists.firstOrNull { it.id == finalPlaylistId }
                                ?: PlaylistUiModel(
                                    id = finalPlaylistId,
                                    name = trimmed,
                                    description = null,
                                    songCount = result.songCount,
                                )

                            selectedPlaylist = targetPlaylist
                            navigatedToDetails = true

                            navController.navigate("${Screen.PlaylistDetails.route}/$finalPlaylistId") {
                                popUpTo(Screen.Playlists.route) { saveState = true }
                                launchSingleTop = true
                            }
                        } catch (_: Exception) {
                            shouldPopBack = true
                        } finally {
                            pendingAddToNewPlaylistSong = null
                            if (shouldPopBack && !navigatedToDetails) {
                                navController.popBackStack()
                            }
                        }
                    }
                },
                onCancel = {
                    pendingAddToNewPlaylistSong = null
                    navController.popBackStack()
                },
            )
        }
}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Every page draws its own bars (owner, 2026-10-08: "the top bar changes but not the
        // content"). NavHost shows a new destination a frame or more after the back stack
        // changes; bars outside it switched first, so a screen change painted twice. Inside it,
        // bar, page and tabs swap in one frame.
        val chrome: @Composable (String, @Composable () -> Unit) -> Unit = { route, page ->
            Scaffold(
                topBar = {
                Column {
                    MonoMusicTopAppBar(
                        currentRoute = route,
                        canNavigateBack = canNavigateBack,
                        focusRequester = focusRequester,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onPerformSearchClick = { performSearch() },
                        selectedAlbum = selectedAlbum,
                        selectedArtistName = selectedArtist,
                        selectedPlaylist = selectedPlaylist,
                        isPlaylistsEditMode = isPlaylistsEditMode,
                        isPlaylistDetailsEditMode = isPlaylistDetailsEditMode,
                        playlistEditSelectionCount = playlistEditSelectionCount,
                        playlistDetailsSelectionCount = playlistDetailsSelectionCount,
                        isPlaylistDetailsMenuExpanded = isPlaylistDetailsMenuExpanded,
                        // a YouTube album, or one downloaded from YouTube (maybe only partly): the
                        // button is there from the first frame; a tap finds what's still missing
                        albumSongsLeft = albumDownload.first,
                        albumComplete = albumDownload.second,
                        canDownloadSelectedAlbum = selectedAlbum?.let { a ->
                            a.sourceType == "YOUTUBE" ||
                                app.pageCache.album(a.sourceType, a.id)
                                    ?.any { it.sourceType == "YOUTUBE_DOWNLOAD" || it.sourceType == "YOUTUBE" } == true
                        } == true,
                        canRenameSelectedAlbum = selectedAlbum?.sourceType == "LOCAL_FILE" ||
                                selectedAlbum?.sourceType == "YOUTUBE_DOWNLOAD",
                        onArtistShuffleClick = artistShuffle,
                        onOpenPage = { route -> navController.navigate(route) { launchSingleTop = true } },
                        onSortClick = when (route) {
                            Screen.Songs.route -> SortPage.SONGS
                            Screen.Albums.route -> SortPage.ALBUMS
                            Screen.Artists.route -> SortPage.ARTISTS
                            Screen.ArtistDetails.route -> SortPage.ARTIST_PAGE
                            else -> null
                        }?.let { page -> { sortSheetPage = page } },
                        activeDownloads = downloadStatuses.count {
                            it.state == YouTubeDownloadStatus.State.PENDING || it.state == YouTubeDownloadStatus.State.IN_PROGRESS
                        },
                        onBackClick = { navController.navigateUp() },
                        onCancelPlaylistsEditClick = {
                            isPlaylistsEditMode = false
                            playlistEditSelectionIds.clear()
                            playlistEditSelectionCount = 0
                        },
                        onCancelPlaylistDetailsEditClick = {
                            isPlaylistDetailsEditMode = false
                            playlistDetailsSelectionIds.clear()
                            playlistDetailsSelectionCount = 0
                        },
                        onEnterPlaylistsEditClick = { isPlaylistsEditMode = true },
                        onNavigateToSearchClick = {
                            navController.navigate(Screen.Search.route) { launchSingleTop = true }
                        },
                        onPlaylistDetailsMenuToggle = {
                            isPlaylistDetailsMenuExpanded = !isPlaylistDetailsMenuExpanded
                        },
                        onPlaylistDetailsEditClick = {
                            if (!isPlaylistDetailsEditMode) {
                                isPlaylistDetailsMenuExpanded = false
                                isPlaylistDetailsEditMode = true
                                playlistDetailsSelectionIds.clear()
                                playlistDetailsSelectionCount = 0
                            }
                        },
                        onPlaylistDetailsAddSongsClick = {
                            isPlaylistDetailsMenuExpanded = false
                            val playlist = selectedPlaylist
                            if (playlist != null) {
                                playlistAddSongsSelectionIds = emptySet()
                                navController.navigate(Screen.PlaylistAddSongs.route) { launchSingleTop = true }
                            }
                        },
                        onPlaylistDetailsRenameClick = {
                            isPlaylistDetailsMenuExpanded = false
                            pendingAddToNewPlaylistSong = null
                            playlistAddSongsSelectionIds = emptySet()
                            navController.navigate(Screen.PlaylistEdit.route) { launchSingleTop = true }
                        },
                        onPlaylistDetailsDeleteClick = {
                            isPlaylistDetailsMenuExpanded = false
                            val playlist = selectedPlaylist
                            if (playlist != null) {
                                playlistEditSelectionIds.clear()
                                playlistEditSelectionIds.add(playlist.id)
                                playlistEditSelectionCount = 1
                                showDeletePlaylistsConfirmation = true
                            }
                        },
                        onAlbumDownloadClick = {
                            val album = selectedAlbum
                            if (album != null) {
                                libraryScope.launch {
                                    val songs = try {
                                        if (album.sourceType == "YOUTUBE") {
                                            viewModel.getAlbumSongsForDetails(album)
                                        } else {
                                            // partly downloaded: the album's songs that aren't on the phone
                                            viewModel.missingAlbumSongs(album)
                                        }
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        emptyList()
                                    }
                                    val activeIds = downloadStatuses
                                        .filter { it.state == YouTubeDownloadStatus.State.PENDING || it.state == YouTubeDownloadStatus.State.IN_PROGRESS }
                                        .map { it.songId }
                                        .toSet()
                                    val toDownload = app.youTubeDownloadManager.filterNotDownloaded(
                                        songs.filter { it.sourceType == "YOUTUBE" && it.id !in activeIds },
                                    )

                                    // no message (pick 1A): the button turns into "N left", then ✓
                                    toDownload.forEach { song ->
                                        app.youTubeDownloadManager.enqueueDownload(song, album.artist, album.title)
                                    }
                                }
                            }
                        },
                        onAlbumRenameClick = {
                            val album = selectedAlbum
                            if (album != null) {
                                renameAlbumText = album.title
                                renameAlbumArtistText = album.artist.orEmpty()
                                showRenameAlbumDialog = true
                            }
                        },
                        onShowDeletePlaylistSongsConfirmationClick = {
                            if (playlistDetailsSelectionCount > 0) {
                                showDeletePlaylistSongsConfirmation = true
                            }
                        },
                        onShowDeletePlaylistsConfirmationClick = {
                            if (playlistEditSelectionCount > 0) {
                                showDeletePlaylistsConfirmation = true
                            }
                        },
                        onPlaylistAddSongsDoneClick = {
                            val playlist = selectedPlaylist
                            val selectedIds = playlistAddSongsSelectionIds
                            if (playlist == null || selectedIds.isEmpty()) {
                                navController.popBackStack()
                            } else {
                                playlistScope.launch {
                                    var snackbarMessage: String?
                                    try {
                                        val result = playlistsViewModel.addSongsToPlaylist(
                                            playlistId = playlist.id,
                                            selectedSongIds = selectedIds,
                                        )

                                        libraryPlaylists = libraryPlaylists.map { existingPlaylist ->
                                            if (existingPlaylist.id == playlist.id) {
                                                existingPlaylist.copy(songCount = result.totalSongCount)
                                            } else {
                                                existingPlaylist
                                            }
                                        }

                                        snackbarMessage = when {
                                            result.addedCount == 1 ->
                                                "Added 1 song to \"${playlist.name}\""
                                            result.addedCount > 1 ->
                                                "Added ${result.addedCount} songs to \"${playlist.name}\""
                                            result.allSelectedAlreadyPresent ->
                                                "All selected songs are already in \"${playlist.name}\""
                                            else -> null
                                        }
                                    } catch (_: Exception) {
                                        snackbarMessage = "Couldn't add songs to playlist"
                                    } finally {
                                        playlistAddSongsSelectionIds = emptySet()
                                        navController.popBackStack()
                                    }

                                    snackbarMessage?.let { message ->
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            withDismissAction = true,
                                            duration = SnackbarDurationMMD.Short,
                                        )
                                    }
                                }
                            }
                        },
                    )
                    HorizontalDividerMMD(thickness = 3.dp)
                }
            },
                bottomBar = {
                MonoMusicBottomBar(
                    playingTitle = nowPlayingSong?.title,
                    playingArtist = nowPlayingSong?.artist.orEmpty(),
                    isPlaying = isPlaybackPlaying,
                    onOpenNowPlaying = { showNowPlaying = true },
                    onPlayPause = { togglePlayback() },
                    currentRoute = route,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            },
                snackbarHost = {},
            ) { paddingValues ->
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    page()

                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Songs.route,
                modifier = Modifier.fillMaxSize(),
                // E-ink: screen transition animations cause slow, ghosting refreshes.
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                playlistsNavGraph(chrome)

                composable(Screen.Artists.route) {
 chrome(Screen.Artists.route) {
                    ArtistsScreen(
                        artists = remember(libraryArtists, sorts) { libraryArtists.sortedArtistsFor(sortOf(SortPage.ARTISTS), viewModel.playStats()) },
                        isLoading = isLoadingSongs || isLoadingAlbums,
                        errorMessage = null,
                        isSyncInProgress = isLibrarySyncInProgress,
                        hasAnySongs = hasAnySongs,
                        onOpenStreamingSettingsClick = openStreamingSettings,
                        onOpenLocalSettingsClick = openLocalSettings,
                        onArtistClick = { artist ->
                            openArtist(artist.name, artist.id)
                        },
                    )
                }
}
                composable(Screen.Songs.route) {
 chrome(Screen.Songs.route) {
                    val shownSongs = remember(librarySongs, sorts) { librarySongs.sortedFor(sortOf(SortPage.SONGS), viewModel.playStats()) }
                    SongsScreen(
                        songs = shownSongs,
                        isLoading = isLoadingSongs,
                        errorMessage = songsError,
                        currentSongId = currentSongId,
                        isSyncInProgress = isLibrarySyncInProgress,
                        onPlaySongClick = { song: SongUiModel ->
                            // play on from the song in the order shown
                            val index = shownSongs.indexOfFirst { it.id == song.id }
                            val startIndex = if (index >= 0) index else 0
                            startPlaybackFromQueue(shownSongs, startIndex)
                        },
                        onShuffleClick = {
                            startShuffledPlaybackFromQueue(librarySongs)
                        },
                        onAddToPlaylistClick = onAddToPlaylist,
                        onRemoveFromLibraryClick = onRemoveFromLibrary,
                        onDeleteClick = onDelete,
                        onEditClick = { song ->
                            editSongTitle = song.title
                            editSongArtist = song.artist
                            songToEdit = song
                        },
                        onOpenStreamingSettingsClick = openStreamingSettings,
                        onOpenLocalSettingsClick = openLocalSettings,
                    )
                }
}
                composable(Screen.Albums.route) {
 chrome(Screen.Albums.route) {
                    AlbumsScreen(
                        albums = remember(libraryAlbums, sorts) { libraryAlbums.sortedFor(sortOf(SortPage.ALBUMS), viewModel.playStats()) },
                        isLoading = isLoadingAlbums,
                        errorMessage = albumsError,
                        isSyncInProgress = isLibrarySyncInProgress,
                        hasAnySongs = hasAnySongs,
                        onOpenStreamingSettingsClick = openStreamingSettings,
                        onOpenLocalSettingsClick = openLocalSettings,
                        onAlbumClick = { album -> openAlbum(album) },
                    )
                }
}
                composable(Screen.Search.route) {
 chrome(Screen.Search.route) {
                    SearchScreen(
                        isSearching = isSearching,
                        errorMessage = searchError,
                        songs = searchSongs,
                        albums = searchAlbums,
                        artists = searchArtists,
                        localSongs = searchLocalSongs,
                        selectedTab = searchSelectedTab,
                        onSelectedTabChange = { searchSelectedTab = it },
                        onPlaySongClick = { song: SongUiModel ->
                            if (searchLocalSongs.any { it.id == song.id }) {
                                val index = searchLocalSongs.indexOfFirst { it.id == song.id }
                                val startIndex = if (index >= 0) index else 0
                                startPlaybackFromQueue(searchLocalSongs, startIndex)
                            } else {
                                val songs = searchSongs
                                val index = songs.indexOfFirst { it.id == song.id }
                                val startIndex = if (index >= 0) index else 0
                                startPlaybackFromQueue(songs, startIndex)
                            }
                        },
                        onAlbumClick = { album: AlbumUiModel -> openAlbum(album) },
                        onArtistClick = { artist -> openArtist(artist.name, artist.id) },
                        query = searchQuery,
                        onEditSearch = { focusRequester.requestFocus() },
                        videos = searchVideos,
                        isSearchingVideos = isSearchingVideos,
                        onSearchVideos = {
                            if (!isSearchingVideos) searchScope.launch {
                                isSearchingVideos = true
                                searchSelectedTab = 0
                                val q = searchQuery.trim()
                                searchVideos = try {
                                    app.youTubeSearchClient.searchVideos(q, 25).map {
                                        SongUiModel(
                                            id = it.videoId,
                                            title = it.title,
                                            artist = it.artist.orEmpty(),
                                            durationText = formatDurationMillis(it.durationMillis),
                                            durationMillis = it.durationMillis,
                                            sourceType = "YOUTUBE",
                                            audioUri = it.videoId,
                                        )
                                    }
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    emptyList()
                                }
                                isSearchingVideos = false
                            }
                        },
                        onPlayVideoClick = { video ->
                            val list = searchVideos.orEmpty()
                            startPlaybackFromQueue(list, list.indexOfFirst { it.id == video.id }.coerceAtLeast(0))
                        },
                        librarySongIds = librarySongIds,
                    )
                }
}
                composable(Screen.AlbumDetails.route) {
 chrome(Screen.AlbumDetails.route) {
                    AlbumDetailsScreen(
                        onShuffleAvailable = { songs ->
                            artistShuffle = songs?.let { { startShuffledPlaybackFromQueue(it) } }
                        },
                        album = selectedAlbum,
                        viewModel = viewModel,
                        onPlaySongClick = { song, songs ->
                            val index = songs.indexOfFirst { it.id == song.id }
                            val startIndex = if (index >= 0) index else 0
                            startPlaybackFromQueue(songs, startIndex)
                        },
                        onShuffleClick = { songs ->
                            startShuffledPlaybackFromQueue(songs)
                        },
                        onEditSongClick = { song ->
                            editSongTitle = song.title
                            editSongArtist = song.artist
                            songToEdit = song
                        },
                        onAddToPlaylistClick = onAddToPlaylist,
                        onRemoveFromLibraryClick = onRemoveFromLibrary,
                        onDeleteClick = onDelete,
                        librarySongIds = librarySongIds,
                    )
                }
}
                composable(Screen.ArtistDetails.route) {
 chrome(Screen.ArtistDetails.route) {
                    ArtistDetailsScreen(
                        artistId = selectedArtistId ?: libraryArtists.find { it.name == selectedArtist }?.id,
                        artistName = selectedArtist,
                        albumSort = sortOf(SortPage.ARTIST_PAGE),
                        viewModel = viewModel,
                        onPlaySongClick = { song, songs ->
                            val index = songs.indexOfFirst { it.id == song.id }
                            val startIndex = if (index >= 0) index else 0
                            startPlaybackFromQueue(songs, startIndex)
                        },
                        onAlbumClick = { album -> openAlbum(album) },
                        onShuffleAvailable = { songs ->
                            artistShuffle = songs?.let { { startShuffledPlaybackFromQueue(it) } }
                        },
                        onAddToPlaylistClick = onAddToPlaylist,
                        onRemoveFromLibraryClick = onRemoveFromLibrary,
                        onDeleteClick = onDelete,
                    )
                }
}

                composable(Screen.More.route) {
 chrome(Screen.More.route) {
                    MoreScreen(
                        onNavigateToDownloads = {
                            navController.navigate(Screen.Downloads.route) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToRadio = {
                            navController.navigate(Screen.Radio.route) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToSettings = {
                            settingsSelectedTab = 0
                            navController.navigate(Screen.Settings.route) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
}

                composable(Screen.Radio.route) {
 chrome(Screen.Radio.route) {
                    RadioScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onPausePlayback = { viewModel.togglePlayback(localMediaController) },
                        isAppPlaying = playbackState.isPlaybackPlaying
                    )
                }
}

                composable(Screen.Downloads.route) {
 chrome(Screen.Downloads.route) {
                    val downloads by app.youTubeDownloadManager.downloads.collectAsStateWithLifecycle()

                    DownloadsScreen(
                        downloads = downloads,
                        onCancelDownload = { id -> app.youTubeDownloadManager.cancelDownload(id) },
                        onClearFinished = { app.youTubeDownloadManager.clearFinishedDownloads() },
                        onRetry = { ids -> app.youTubeDownloadManager.retry(ids) },
                    )
                }
}

                composable(Screen.Settings.route) {
 chrome(Screen.Settings.route) {
                    val context = LocalContext.current
                    val lifecycleOwner = LocalLifecycleOwner.current


                    SettingsScreen(
                        completeAlbumsWithYouTube = completeAlbumsWithYouTube,
                        onCompleteAlbumsWithYouTubeChange = { enabled ->
                            settingsManager.setCompleteAlbumsWithYouTube(enabled)
                        },
                        includeLocalMusic = includeLocalMusic,
                        localFolders = localMusicFolders.toList(),
                        onIncludeLocalMusicChange = { enabled ->
                            settingsManager.setIncludeLocalMusic(enabled)
                        },
                        onOpenFoldersClick = {
                            navController.navigate(Screen.MusicFolders.route) { launchSingleTop = true }
                        },
                        onRescanLocalMusicClick = {
                            libraryScope.launch {
                                resyncLocalLibrary(includeLocalMusic, localMusicFolders)
                            }
                        },
                        isRescanningLocal = isRescanningLocal,
                        localScanProgress = localScanProgress,
                        isIngestingLocal = isIngestingLocal,
                        localIngestProgress = localIngestProgress,
                        localScanTotalDiscovered = localScanTotalDiscovered,
                        localScanIndexedNewOrUpdated = localScanIndexedNewOrUpdated,
                        localScanDeletedMissing = localScanDeletedMissing,
                    )
                }
}
                composable(Screen.Queue.route) {
 chrome(Screen.Queue.route) {
                    QueueScreen(
                        queue = playbackState.playbackQueue,
                        currentIndex = playbackState.playbackQueueIndex,
                        onPlay = { i -> viewModel.playQueueIndex(localMediaController, i) },
                        onMove = { from, to -> viewModel.moveInQueue(localMediaController, from, to) },
                        onRemove = { i -> viewModel.removeFromQueue(localMediaController, i) },
                    )
                }
}
                composable(Screen.MusicFolders.route) {
 chrome(Screen.MusicFolders.route) {
                    val folderPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree(),
                    ) { uri ->
                        if (uri != null) {
                            val flags =
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            try {
                                context.contentResolver.takePersistableUriPermission(uri, flags)
                            } catch (_: SecurityException) {
                            }
                            settingsManager.addLocalMusicFolder(uri.toString())
                        }
                    }
                    MusicFoldersScreen(
                        folders = localMusicFolders.toList(),
                        onAddFolderClick = { folderPickerLauncher.launch(null) },
                        onRemoveFolderClick = { uri -> settingsManager.removeLocalMusicFolder(uri) },
                    )
                }
}
            }
        }

        sortSheetPage?.let { page ->
            SortSheet(
                page = page,
                state = sortOf(page),
                onChange = { state ->
                    sorts = sorts + (page to state)
                    sortStore.set(page, state)
                },
                onDismissRequest = { sortSheetPage = null },
            )
        }

        if (showExternalControls) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .padding(horizontal = 16.dp)
            ) {
                ExternalMediaWidget(externalMediaState)
            }
        }

        if (showNowPlaying && playbackState.nowPlayingSong != null) {
            val song = playbackState.nowPlayingSong!!

            val isInLibrary = librarySongIds.contains(song.id)

            val displayDuration = when {
                playbackState.nowPlayingDurationMs > 0L -> playbackState.nowPlayingDurationMs
                song.durationMillis != null && song.durationMillis > 0L -> song.durationMillis
                else -> 0L
            }

            val isLocalVideo = if (song.sourceType == "LOCAL_FILE" || song.sourceType == "YOUTUBE_DOWNLOAD") {
                val uriString = song.audioUri ?: song.id
                try {
                    val lastSegment = uriString.toUri().lastPathSegment ?: ""
                    lastSegment.substringAfterLast('.', "").lowercase() == "mp4"
                } catch (_: Exception) {
                    false
                }
            } else {
                false
            }

            BackHandler {
                showNowPlaying = false
            }

            // YouTube songs carry their artist's id; library songs match a library artist by name.
            val artistTarget = song.artistId
                ?: libraryArtists.firstOrNull { it.name.equals(song.artist, ignoreCase = true) }?.id
            val showCovers by settingsManager.showAlbumCovers.collectAsState()
            // Now Playing ticks every second anyway (owner's pick 4C), so its cover may arrive late
            var npCover by remember(song.id) { mutableStateOf(app.covers.peekSong(song)) }
            LaunchedEffect(song.id, showCovers) {
                if (showCovers && npCover == null) npCover = app.covers.loadForSong(song)
            }
            NowPlayingScreen(
                cover = npCover,
                showCover = showCovers,
                // Now Playing closes in the frame the queue opens
                onQueueClick = {
                    showNowPlaying = false
                    navController.navigate(Screen.Queue.route) { launchSingleTop = true }
                },
                onArtistClick = artistTarget?.let { id ->
                    // Now Playing closes in the same frame the artist page opens
                    { openArtist(song.artist, id, before = { showNowPlaying = false }) }
                },
                title = song.title,
                artist = song.artist.ifBlank { if (song.sourceType == "LOCAL_FILE" || song.sourceType == "YOUTUBE_DOWNLOAD") "Local file" else "" },
                album = song.album,
                isPlaying = playbackState.isPlaybackPlaying,
                isLoading = playbackState.isBuffering,
                currentPosition = playbackState.nowPlayingPositionMs.coerceAtMost(displayDuration),
                duration = displayDuration,
                repeatMode = playbackState.repeatMode,
                isShuffleOn = playbackState.isShuffleOn,
                onPlayPauseClick = { togglePlayback() },
                onSeek = { positionMs ->
                    when (song.sourceType) {
                        "LOCAL_FILE", "YOUTUBE", "YOUTUBE_DOWNLOAD" -> {
                            localMediaController?.seekTo(positionMs)
                        }
                    }
                },
                onSeekBackwardClick = {
                    viewModel.playPreviousInQueue(localMediaController)
                },
                onSeekForwardClick = {
                    viewModel.playNextInQueue(localMediaController)
                },
                onShuffleClick = {
                    viewModel.toggleShuffleMode(localMediaController)
                },
                onRepeatClick = {
                    viewModel.cycleRepeatMode(localMediaController)
                },
                onAddToPlaylistClick = {
                    songToAddToPlaylist = playbackState.nowPlayingSong
                    showAddToPlaylistDialog = true
                },
                onBackClick = { showNowPlaying = false },
                isVideo = isLocalVideo,
                player = if (isLocalVideo) localMediaController else null,
                canDownload = (song.sourceType == "YOUTUBE"),
                isDownloadInProgress = downloadStatuses.any { it.songId == song.id && (it.state == YouTubeDownloadStatus.State.PENDING || it.state == YouTubeDownloadStatus.State.IN_PROGRESS) },
                onDownloadClick = {
                    var albumArtist: String? = null

                    if (song.album != null) {
                        val libraryMatch = libraryAlbums.find {
                            it.title.equals(song.album, ignoreCase = true)
                        }
                        if (libraryMatch != null) {
                            albumArtist = libraryMatch.artist
                        } else {
                            if (selectedAlbum?.title.equals(song.album, ignoreCase = true)) {
                                albumArtist = selectedAlbum?.artist
                            }
                        }
                    }

                    // the button itself shows it is downloading; no message
                    app.youTubeDownloadManager.enqueueDownload(song, albumArtist)
                },
                onCancelDownloadClick = {
                    val active = downloadStatuses.firstOrNull { it.songId == song.id && (it.state == YouTubeDownloadStatus.State.PENDING || it.state == YouTubeDownloadStatus.State.IN_PROGRESS) }
                    if (active != null) {
                        app.youTubeDownloadManager.cancelDownload(active.id)
                    }
                },
                canAddToLibrary = (song.sourceType == "YOUTUBE" && !isInLibrary),
                onAddToLibraryClick = {
                    libraryScope.launch {
                        try {
                            viewModel.addStreamingSongToLibrary(song)
                            snackbarHostState.showSnackbar(
                                message = "Added to library",
                                withDismissAction = true,
                                duration = SnackbarDurationMMD.Short,
                            )
                        } catch (_: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "Couldn't add to library",
                                withDismissAction = true,
                                duration = SnackbarDurationMMD.Short,
                            )
                        }
                    }
                },
                isInLibrary = isInLibrary,
                sourceType = song.sourceType,
                streamResolverLabel = if (song.sourceType == "YOUTUBE") streamResolverLabel else null,
            )
        }

        if (showAddToPlaylistDialog && songToAddToPlaylist != null) {
            val song = songToAddToPlaylist!!
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp

            ModalBottomSheetMMD(
                onDismissRequest = { showAddToPlaylistDialog = false },
                sheetState = addToPlaylistSheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextMMD(
                            text = "Add to Playlist",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        IconButton(
                            onClick = { showAddToPlaylistDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Cancel Add to Playlist"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (libraryPlaylists.isEmpty()) {
                        Text(
                            text = "You have not created any playlist yet...",
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        val lastPlaylistId = libraryPlaylists.lastOrNull()?.id
                        PagedList(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = screenHeight * 0.6f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(
                                items = libraryPlaylists,
                                key = { it.id },
                            ) { playlist ->
                                val isLast = playlist.id == lastPlaylistId
                                PlaylistItem(
                                    playlist = playlist,
                                    onClick = {
                                        showAddToPlaylistDialog = false
                                        addSongToPlaylist(song, playlist)
                                    },
                                    showDivider = !isLast,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButtonMMD(
                        onClick = {
                            pendingAddToNewPlaylistSong = song
                            showAddToPlaylistDialog = false
                            showNowPlaying = false
                            selectedPlaylist = null
                            navController.navigate(Screen.PlaylistEdit.route) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        TextMMD(
                            text = "New playlist",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (showRenameAlbumDialog && selectedAlbum != null) {
            val album = selectedAlbum!!

            ModalBottomSheetMMD(
                onDismissRequest = { showRenameAlbumDialog = false },
                sheetState = renameAlbumSheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextMMD(
                            text = "Edit Album",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        IconButton(
                            onClick = { showRenameAlbumDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Cancel rename"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextFieldMMD(
                        value = renameAlbumText,
                        onValueChange = { renameAlbumText = it },
                        label = { TextMMD("Album name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextFieldMMD(
                        value = renameAlbumArtistText,
                        onValueChange = { renameAlbumArtistText = it },
                        label = { TextMMD("Album artist") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButtonMMD(
                        onClick = {
                            val newTitle = renameAlbumText.trim()
                            val newArtist = renameAlbumArtistText.trim()
                            showRenameAlbumDialog = false
                            libraryScope.launch {
                                val updatedAlbum = try {
                                    viewModel.renameAlbum(album, newTitle, newArtist)
                                } catch (_: Exception) {
                                    null
                                }
                                if (updatedAlbum != null) {
                                    selectedAlbum = updatedAlbum
                                    snackbarHostState.showSnackbar(
                                        message = "Album updated",
                                        withDismissAction = true,
                                        duration = SnackbarDurationMMD.Short,
                                    )
                                } else {
                                    snackbarHostState.showSnackbar(
                                        message = "Couldn't rename album",
                                        withDismissAction = true,
                                        duration = SnackbarDurationMMD.Short,
                                    )
                                }
                            }
                        },
                        enabled = renameAlbumText.isNotBlank() && renameAlbumArtistText.isNotBlank() &&
                                (renameAlbumText.trim() != album.title || renameAlbumArtistText.trim() != album.artist.orEmpty()),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        TextMMD(
                            text = "Save",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (songToEdit != null) {
            val song = songToEdit!!

            ModalBottomSheetMMD(
                onDismissRequest = { songToEdit = null },
                sheetState = editSongSheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextMMD(
                            text = "Edit Song",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        IconButton(
                            onClick = { songToEdit = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Cancel edit"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextFieldMMD(
                        value = editSongTitle,
                        onValueChange = { editSongTitle = it },
                        label = { TextMMD("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextFieldMMD(
                        value = editSongArtist,
                        onValueChange = { editSongArtist = it },
                        label = { TextMMD("Artist") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButtonMMD(
                        onClick = {
                            val newTitle = editSongTitle.trim()
                            val newArtist = editSongArtist.trim()
                            songToEdit = null
                            libraryScope.launch {
                                try {
                                    val updated = viewModel.updateSongMetadata(
                                        song.id,
                                        song.audioUri,
                                        newTitle,
                                        newArtist,
                                    )
                                    snackbarHostState.showSnackbar(
                                        message = if (updated) "Song updated" else "Couldn't find this song in the library",
                                        withDismissAction = true,
                                        duration = SnackbarDurationMMD.Short,
                                    )
                                } catch (_: Exception) {
                                    snackbarHostState.showSnackbar(
                                        message = "Couldn't update song",
                                        withDismissAction = true,
                                        duration = SnackbarDurationMMD.Short,
                                    )
                                }
                            }
                        },
                        enabled = editSongTitle.isNotBlank() && editSongArtist.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        TextMMD(
                            text = "Save",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (showDeletePlaylistSongsConfirmation && playlistDetailsSelectionCount > 0 && selectedPlaylist != null) {
            val playlist = selectedPlaylist
            val idsToRemove = playlistDetailsSelectionIds.toSet()
            if (playlist != null && idsToRemove.isNotEmpty()) {
                ModalBottomSheetMMD(
                    onDismissRequest = { showDeletePlaylistSongsConfirmation = false },
                    sheetState = removeSongsSheetState,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextMMD(
                                text = if (playlistDetailsSelectionCount == 1) {
                                    "Remove song from \"${playlist.name}\""
                                } else {
                                    "Remove songs from \"${playlist.name}\""
                                },
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )

                            IconButton(
                                onClick = { showDeletePlaylistSongsConfirmation = false },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Cancel song removal",
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (playlistDetailsSelectionCount == 1) {
                                "This will remove the selected song from this playlist. The song will remain in your library."
                            } else {
                                "This will remove the selected songs from this playlist. The songs will remain in your library."
                            },
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Normal,
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedButtonMMD(
                            onClick = {
                                playlistScope.launch {
                                    var snackbarMessage: String?
                                    try {
                                        val updatedCount = playlistsViewModel.removeSongsFromPlaylist(
                                            playlistId = playlist.id,
                                            songIds = idsToRemove,
                                        )

                                        libraryPlaylists = libraryPlaylists.map { existingPlaylist ->
                                            if (existingPlaylist.id == playlist.id) {
                                                existingPlaylist.copy(songCount = updatedCount)
                                            } else {
                                                existingPlaylist
                                            }
                                        }

                                        val removedCount = idsToRemove.size
                                        snackbarMessage = if (removedCount == 1) {
                                            "Removed 1 song from \"${playlist.name}\""
                                        } else {
                                            "Removed $removedCount songs from \"${playlist.name}\""
                                        }
                                    } catch (_: Exception) {
                                        snackbarMessage = "Couldn't remove songs from playlist"
                                    } finally {
                                        playlistDetailsSelectionIds.clear()
                                        playlistDetailsSelectionCount = 0
                                        isPlaylistDetailsEditMode = false
                                        showDeletePlaylistSongsConfirmation = false
                                    }

                                    snackbarMessage?.let { message ->
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            withDismissAction = true,
                                            duration = SnackbarDurationMMD.Short,
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            TextMMD(
                                text = "Remove",
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButtonMMD(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                            onClick = { showDeletePlaylistSongsConfirmation = false },
                        ) {
                            TextMMD(
                                text = "Back",
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        if (showDeletePlaylistsConfirmation && playlistEditSelectionCount > 0) {
            val idsToDelete = playlistEditSelectionIds.toSet()
            if (idsToDelete.isNotEmpty()) {
                ModalBottomSheetMMD(
                    onDismissRequest = { showDeletePlaylistsConfirmation = false },
                    sheetState = deletePlaylistsSheetState,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextMMD(
                                text = "Delete playlist${if (playlistEditSelectionCount > 1) "s" else ""}",
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )

                            IconButton(
                                onClick = { showDeletePlaylistsConfirmation = false },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Cancel playlist deletion",
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (playlistEditSelectionCount == 1) {
                                "This will permanently remove the selected playlist. Songs in your library will not be deleted."
                            } else {
                                "This will permanently remove the selected playlists. Songs in your library will not be deleted."
                            },
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Normal,
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedButtonMMD(
                            onClick = {
                                playlistScope.launch {
                                    val currentPlaylistsById = libraryPlaylists.associateBy { it.id }
                                    var snackbarMessage: String?

                                    try {
                                        val playlistsToDelete = idsToDelete.mapNotNull { id ->
                                            currentPlaylistsById[id]
                                        }
                                        val remainingPlaylists = playlistsViewModel.deletePlaylists(playlistsToDelete)
                                        libraryPlaylists = remainingPlaylists

                                        val deletedIds = idsToDelete
                                        val currentDetailsPlaylist = selectedPlaylist
                                        if (
                                            currentDestination?.route == Screen.PlaylistDetails.route &&
                                            currentDetailsPlaylist != null &&
                                            deletedIds.contains(currentDetailsPlaylist.id)
                                        ) {
                                            selectedPlaylist = null
                                            navController.popBackStack(
                                                Screen.Playlists.route,
                                                inclusive = false
                                            )
                                        }

                                        val deletedCount = idsToDelete.size
                                        snackbarMessage = if (deletedCount == 1) {
                                            "Deleted 1 playlist"
                                        } else {
                                            "Deleted $deletedCount playlists"
                                        }
                                    } catch (_: Exception) {
                                        snackbarMessage = "Couldn't delete playlists"
                                    } finally {
                                        playlistEditSelectionIds.clear()
                                        playlistEditSelectionCount = 0
                                        isPlaylistsEditMode = false
                                        showDeletePlaylistsConfirmation = false
                                    }

                                    snackbarMessage?.let { message ->
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            withDismissAction = true,
                                            duration = SnackbarDurationMMD.Short,
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            TextMMD(
                                text = "Delete",
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButtonMMD(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                            onClick = { showDeletePlaylistsConfirmation = false },
                        ) {
                            TextMMD(
                                text = "Back",
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        val messageLift = (if (nowPlayingSong != null && !showNowPlaying) 67.dp else 0.dp) +
            (if (currentDestination?.route in navItems.map { it.route }) 60.dp else 0.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = messageLift)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SnackbarHostMMD(
                snackbarHostState
            )
        }
    }
}

@Composable
fun ExternalMediaWidget(state: ExternalMediaState) {
    val context = LocalContext.current
    if (!isNotificationServiceEnabled(context)) {
        Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            OutlinedButtonMMD(
                onClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp)
            ) {
                TextMMD("Tap to Enable Music Control", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
        }
    } else {
        // MMD card: white with a black outline; no grey fill, no faded text, one solid button.
        com.mudita.mmd.components.cards.CardMMD(
            modifier = Modifier.fillMaxWidth(),
            colors = com.mudita.mmd.components.cards.CardDefaultsMMD.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextMMD(
                    text = "Playing on ${state.packageName}",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextMMD(
                    text = state.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextMMD(
                    text = state.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { ExternalMediaRepository.skipToPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, "Previous")
                    }
                    IconButton(onClick = { ExternalMediaRepository.togglePlayPause() }) {
                        Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.isPlaying) "Pause" else "Play")
                    }
                    IconButton(onClick = { ExternalMediaRepository.skipToNext() }) {
                        Icon(Icons.Default.SkipNext, "Next")
                    }
                }
            }
        }
    }
}

fun isNotificationServiceEnabled(context: android.content.Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(context.packageName)
}

@Composable
fun getAppBarTitle(currentRoute: String?): String {
    return when {
        currentRoute == Screen.Playlists.route -> "Playlists"
        currentRoute == Screen.Songs.route -> "Songs"
        currentRoute == Screen.Albums.route -> "Albums"
        currentRoute == Screen.AlbumDetails.route -> "Album"
        currentRoute == Screen.Artists.route -> "Artists"
        currentRoute == Screen.ArtistDetails.route -> "Artist"
        currentRoute == Screen.Search.route -> "Search"
        currentRoute == Screen.More.route -> "More"
        currentRoute == Screen.Radio.route -> "Radio"
        currentRoute == Screen.Downloads.route -> "Downloads"
        currentRoute == Screen.Settings.route -> "Settings"
        currentRoute == Screen.MusicFolders.route -> "Music folders"
        currentRoute == Screen.Queue.route -> "Queue"
        currentRoute == Screen.PlaylistEdit.route -> "Edit Playlist"
        currentRoute == Screen.PlaylistAddSongs.route -> "Add Songs"
        currentRoute == Screen.PlaylistDetails.route -> "Playlist"
        currentRoute?.startsWith("playlistDetails/") == true -> "Playlist"
        else -> ""
    }
}

fun formatDurationMillis(millis: Long?): String? {
    val totalMillis = millis ?: return null
    if (totalMillis <= 0) return null
    val totalSeconds = totalMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}