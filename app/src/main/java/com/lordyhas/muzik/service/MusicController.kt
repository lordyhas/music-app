package com.lordyhas.muzik.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.lordyhas.muzik.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
class MusicController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // Tracks how many songs the user has manually queued after the current song (Spotify-style FIFO)
    private var userQueueCount = 0

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isControllerReady = MutableStateFlow(false)
    val isControllerReady: StateFlow<Boolean> = _isControllerReady.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()

    private val _currentMediaItemIndex = MutableStateFlow(0)
    val currentMediaItemIndex: StateFlow<Int> = _currentMediaItemIndex.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentMediaItem.value = mediaItem
            _currentMediaItemIndex.value = mediaController?.currentMediaItemIndex ?: 0
            userQueueCount = 0 // Reset: user queue is relative to the current song
            updateDuration()
            updatePosition()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            _currentMediaItemIndex.value = mediaController?.currentMediaItemIndex ?: 0
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                updateDuration()
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _isShuffle.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }
    }

    init {
        initController()
    }

    private fun initController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(playerListener)

                // Sync initial states
                mediaController?.let { controller ->
                    _currentMediaItem.value = controller.currentMediaItem
                    _currentMediaItemIndex.value = controller.currentMediaItemIndex
                    _isPlaying.value = controller.isPlaying
                    _isShuffle.value = controller.shuffleModeEnabled
                    _repeatMode.value = controller.repeatMode
                    updateDuration()
                    updatePosition()
                    if (controller.isPlaying) {
                        startProgressTracker()
                    }
                }
                _isControllerReady.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    fun playSongList(songs: List<Song>, startIndex: Int) {
        val controller = mediaController ?: return
        val mediaItems = songs.map { it.toMediaItem() }

        _currentQueue.value = songs
        _currentMediaItemIndex.value = startIndex
        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun addToQueue(song: Song) {
        val controller = mediaController ?: return
        val mediaItem = song.toMediaItem()

        if (controller.mediaItemCount == 0 || _currentQueue.value.isEmpty()) {
            playSongList(listOf(song), 0)
            return
        }

        // Spotify-style: insert after the last user-queued song (FIFO order)
        val currentIndex = controller.currentMediaItemIndex
        val insertIndex = (currentIndex + 1 + userQueueCount).coerceAtMost(controller.mediaItemCount)
        controller.addMediaItem(insertIndex, mediaItem)
        userQueueCount++

        val updatedQueue = _currentQueue.value.toMutableList()
        if (insertIndex <= updatedQueue.size) {
            updatedQueue.add(insertIndex, song)
        } else {
            updatedQueue.add(song)
        }
        _currentQueue.value = updatedQueue
    }

    fun removeQueueItem(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.removeMediaItem(index)
            val updatedQueue = _currentQueue.value.toMutableList()
            if (index in updatedQueue.indices) {
                updatedQueue.removeAt(index)
                _currentQueue.value = updatedQueue
            }
        }
    }

    fun playQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekTo(index, 0L)
            _currentMediaItemIndex.value = index
            if (!controller.isPlaying) {
                controller.play()
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        if (fromIndex in 0 until controller.mediaItemCount && toIndex in 0 until controller.mediaItemCount) {
            controller.moveMediaItem(fromIndex, toIndex)
            val updatedQueue = _currentQueue.value.toMutableList()
            if (fromIndex in updatedQueue.indices && toIndex in updatedQueue.indices) {
                val item = updatedQueue.removeAt(fromIndex)
                updatedQueue.add(toIndex, item)
                _currentQueue.value = updatedQueue
            }
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000) {
            controller.seekTo(0)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0)
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newShuffle = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newShuffle
        _isShuffle.value = newShuffle
    }

    fun toggleRepeat() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    private fun updateDuration() {
        val dur = mediaController?.duration ?: 0L
        _duration.value = if (dur < 0) 0L else dur
    }

    private fun updatePosition() {
        val pos = mediaController?.currentPosition ?: 0L
        _currentPosition.value = if (pos < 0) 0L else pos
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                updatePosition()
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
    }
}
