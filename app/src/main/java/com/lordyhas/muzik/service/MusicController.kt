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

    private val _userQueueCount = MutableStateFlow(0)
    val userQueueCount: StateFlow<Int> = _userQueueCount.asStateFlow()

    private val _userQueueList = MutableStateFlow<List<Song>>(emptyList())
    val userQueueList: StateFlow<List<Song>> = _userQueueList.asStateFlow()

    private val _contextQueueList = MutableStateFlow<List<Song>>(emptyList())
    val contextQueueList: StateFlow<List<Song>> = _contextQueueList.asStateFlow()

    private var baseContextQueue: List<Song> = emptyList()

    private fun updateUIQueues() {
        val fullQueue = _currentQueue.value
        val idx = _currentMediaItemIndex.value

        if (idx in fullQueue.indices) {
            val uqStart = idx + 1
            val uqEnd = (uqStart + _userQueueCount.value).coerceAtMost(fullQueue.size)
            _userQueueList.value = fullQueue.subList(uqStart, uqEnd)
            _contextQueueList.value = fullQueue.subList(uqEnd, fullQueue.size)
        } else {
            _userQueueList.value = emptyList()
            _contextQueueList.value = emptyList()
        }
    }

    private fun clampUserQueueCount() {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex
        val maxAvailable = (controller.mediaItemCount - currentIndex - 1).coerceAtLeast(0)
        _userQueueCount.value = _userQueueCount.value.coerceIn(0, maxAvailable)
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentMediaItem.value = mediaItem
            val oldIndex = _currentMediaItemIndex.value
            val newIndex = mediaController?.currentMediaItemIndex ?: 0
            _currentMediaItemIndex.value = newIndex

            val diff = newIndex - oldIndex
            if (diff != 0) {
                _userQueueCount.value = (_userQueueCount.value - diff).coerceAtLeast(0)
            }
            clampUserQueueCount()
            updateUIQueues()

            updateDuration()
            updatePosition()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            _currentMediaItemIndex.value = mediaController?.currentMediaItemIndex ?: 0
            clampUserQueueCount()
            updateUIQueues()
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
            // Ignored: we use custom physical shuffle logic in toggleShuffle()
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
        baseContextQueue = songs
        _userQueueCount.value = 0

        // Handle shuffle for playSongList: Shuffle the remaining context only.
        val playQueue = if (_isShuffle.value) {
            val before = songs.subList(0, startIndex + 1)
            val after = songs.subList(startIndex + 1, songs.size).shuffled()
            before + after
        } else {
            songs
        }

        val mediaItems = playQueue.map { it.toMediaItem() }

        _currentQueue.value = playQueue
        _currentMediaItemIndex.value = startIndex
        updateUIQueues()
        
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

        clampUserQueueCount()
        // Spotify-style: insert after the last user-queued song (FIFO order)
        val currentIndex = controller.currentMediaItemIndex
        val insertIndex = (currentIndex + 1 + _userQueueCount.value).coerceAtMost(controller.mediaItemCount)
        controller.addMediaItem(insertIndex, mediaItem)
        _userQueueCount.value++

        val updatedQueue = _currentQueue.value.toMutableList()
        if (insertIndex <= updatedQueue.size) {
            updatedQueue.add(insertIndex, song)
        } else {
            updatedQueue.add(song)
        }
        _currentQueue.value = updatedQueue
        updateUIQueues()
    }

    fun removeQueueItem(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            val currentIndex = controller.currentMediaItemIndex
            if (index > currentIndex && index <= currentIndex + _userQueueCount.value) {
                _userQueueCount.value = (_userQueueCount.value - 1).coerceAtLeast(0)
            }
            controller.removeMediaItem(index)
            val updatedQueue = _currentQueue.value.toMutableList()
            if (index in updatedQueue.indices) {
                updatedQueue.removeAt(index)
                _currentQueue.value = updatedQueue
            }
            clampUserQueueCount()
            updateUIQueues()
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
            updateUIQueues()
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
            updateUIQueues()
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
        val newShuffle = !_isShuffle.value
        _isShuffle.value = newShuffle

        // Native shuffle is disabled because we physically shuffle the context list 
        // to protect the user queue order (Spotify logic).
        controller.shuffleModeEnabled = false

        val currentIndex = controller.currentMediaItemIndex
        val safeIndex = (currentIndex + 1 + _userQueueCount.value).coerceAtMost(controller.mediaItemCount)

        val currentFullQueue = _currentQueue.value.toMutableList()

        if (newShuffle) {
            if (safeIndex < currentFullQueue.size) {
                val contextRemaining = currentFullQueue.subList(safeIndex, currentFullQueue.size).shuffled()
                for (i in contextRemaining.indices) {
                    currentFullQueue[safeIndex + i] = contextRemaining[i]
                }
            }
        } else {
            if (safeIndex < currentFullQueue.size) {
                val remainingIds = currentFullQueue.subList(safeIndex, currentFullQueue.size).map { it.id }.toSet()
                val restoredContext = baseContextQueue.filter { it.id in remainingIds }
                for (i in restoredContext.indices) {
                    currentFullQueue[safeIndex + i] = restoredContext[i]
                }
            }
        }

        _currentQueue.value = currentFullQueue
        updateUIQueues()

        if (safeIndex < currentFullQueue.size) {
            val newMediaItems = currentFullQueue.subList(safeIndex, currentFullQueue.size).map { it.toMediaItem() }
            controller.replaceMediaItems(safeIndex, controller.mediaItemCount, newMediaItems)
        }
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
