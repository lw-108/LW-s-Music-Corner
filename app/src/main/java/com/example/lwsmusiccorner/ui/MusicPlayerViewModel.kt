package com.example.lwsmusiccorner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.lwsmusiccorner.data.Song
import com.example.lwsmusiccorner.data.SongProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOrder {
    A_Z, Z_A, ALBUM, ARTIST, YEAR
}

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    
    private val _sortOrder = MutableStateFlow(SortOrder.A_Z)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    val songs: StateFlow<List<Song>> = combine(_allSongs, _sortOrder, _searchQuery) { songs, order, query ->
        val filtered = if (query.isEmpty()) {
            songs
        } else {
            songs.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }

        when (order) {
            SortOrder.A_Z -> filtered.sortedBy { it.title }
            SortOrder.Z_A -> filtered.sortedByDescending { it.title }
            SortOrder.ALBUM -> filtered.sortedBy { it.album }
            SortOrder.ARTIST -> filtered.sortedBy { it.artist }
            SortOrder.YEAR -> filtered.sortedByDescending { it.year }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentPlaylist = MutableStateFlow<List<Song>>(emptyList())

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = duration
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = currentMediaItemIndex
                val queue = _currentPlaylist.value
                if (index != -1 && index < queue.size) {
                    _currentSong.value = queue[index]
                }
            }
        })
    }

    private var progressJob: Job? = null

    init {
        loadSongs()
        startProgressUpdate()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isInitialLoading.value = true
            // Artificial delay to show the splash screen
            delay(2000)
            val songList = SongProvider(getApplication()).fetchSongs()
            _allSongs.value = songList
            if (songList.isNotEmpty() && _currentPlaylist.value.isEmpty()) {
                setupPlaylist(songList.sortedBy { it.title })
            }
            _isInitialLoading.value = false
        }
    }

    private fun setupPlaylist(songList: List<Song>) {
        _currentPlaylist.value = songList
        val mediaItems = songList.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .build()
        }
        player.setMediaItems(mediaItems)
        player.prepare()
        _currentSong.value = songList.firstOrNull()
    }

    fun playSong(song: Song) {
        if (_currentPlaylist.value != songs.value) {
            _currentPlaylist.value = songs.value
            val mediaItems = songs.value.map { s ->
                MediaItem.Builder()
                    .setMediaId(s.id.toString())
                    .setUri(s.uri)
                    .build()
            }
            player.setMediaItems(mediaItems, false)
            player.prepare()
        }
        
        val index = _currentPlaylist.value.indexOf(song)
        if (index != -1) {
            player.seekTo(index, 0L)
            player.play()
            _currentSong.value = song
        }
    }

    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun skipNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        }
    }

    fun skipPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun setVolume(volume: Float) {
        _volume.value = volume
        player.volume = volume
    }

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                _currentPosition.value = player.currentPosition
                delay(500)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
        progressJob?.cancel()
    }
}
