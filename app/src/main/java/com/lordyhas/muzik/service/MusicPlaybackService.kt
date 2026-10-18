package com.lordyhas.muzik.service

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.Equalizer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.lordyhas.muzik.MainActivity
import com.lordyhas.muzik.data.repository.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var equalizer: Equalizer? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sleepJob: Job? = null
    private lateinit var libraryRepository: LibraryRepository

    /** Song waiting to be counted as "played" once playback actually starts. */
    private var pendingPlayRecord: String? = null
    private var progressSaverJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachEqualizer(audioSessionId)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Counted here (not in the UI) so plays are tracked even when the app is closed
            pendingPlayRecord = mediaItem?.mediaId
            if (exoPlayer?.playWhenReady == true) recordPendingPlay()
            saveProgress()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                recordPendingPlay()
                startProgressSaver()
            } else {
                progressSaverJob?.cancel()
                saveProgress()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            // "End of track" sleep timer: ExoPlayer paused for us at the end of the item
            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                PlaybackSettings.sleepTimer.value == SleepTimer.EndOfTrack
            ) {
                exoPlayer?.pauseAtEndOfMediaItems = false
                PlaybackSettings.onSleepTimerFinished()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        PlaybackSettings.init(this)
        libraryRepository = LibraryRepository(this)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // true = handle audio focus automatically
            .setHandleAudioBecomingNoisy(true) // pause when headphones unplugged
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exoPlayer = player
        player.addListener(playerListener)
        attachEqualizer(player.audioSessionId)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(MediaSessionCallback())
            .build()

        observeSettings()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        saveProgress()
        serviceScope.cancel()
        PlaybackSettings.cancelSleepTimer()
        releaseEqualizer()
        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }

    /** Keeps the saved session fresh while playing with the UI closed. */
    private fun saveProgress() {
        val player = exoPlayer ?: return
        if (player.mediaItemCount == 0) return
        PlaybackStateStore.saveProgress(
            this,
            songIds = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() },
            index = player.currentMediaItemIndex,
            positionMs = player.currentPosition
        )
    }

    private fun startProgressSaver() {
        progressSaverJob?.cancel()
        progressSaverJob = serviceScope.launch {
            while (true) {
                delay(15_000)
                saveProgress()
            }
        }
    }

    private fun recordPendingPlay() {
        val songId = pendingPlayRecord?.toLongOrNull() ?: return
        pendingPlayRecord = null
        serviceScope.launch(Dispatchers.IO) {
            try {
                libraryRepository.recordPlay(songId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ─── Settings → player ──────────────────────────────────────────────────

    private fun observeSettings() {
        serviceScope.launch {
            PlaybackSettings.speed.collect { speed -> exoPlayer?.setPlaybackSpeed(speed) }
        }
        serviceScope.launch {
            PlaybackSettings.equalizer.collect { applyEqualizer(it) }
        }
        serviceScope.launch {
            PlaybackSettings.sleepTimer.collect { applySleepTimer(it) }
        }
    }

    private fun applySleepTimer(timer: SleepTimer) {
        val player = exoPlayer ?: return
        sleepJob?.cancel()
        player.pauseAtEndOfMediaItems = false
        player.volume = 1f

        when (timer) {
            SleepTimer.Off -> Unit
            SleepTimer.EndOfTrack -> player.pauseAtEndOfMediaItems = true
            is SleepTimer.At -> sleepJob = serviceScope.launch {
                val remaining = timer.endsAtMillis - System.currentTimeMillis()
                if (remaining > FADE_OUT_MS) delay(remaining - FADE_OUT_MS)

                // Gentle fade-out over the last seconds instead of an abrupt stop
                val fadeLeft = (timer.endsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                val steps = 20
                repeat(steps) { step ->
                    player.volume = 1f - (step + 1) / steps.toFloat()
                    delay(fadeLeft / steps)
                }
                player.pause()
                player.volume = 1f
                PlaybackSettings.onSleepTimerFinished()
            }
        }
    }

    // ─── Equalizer ──────────────────────────────────────────────────────────

    private fun attachEqualizer(audioSessionId: Int) {
        releaseEqualizer()
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return

        // Not every device exposes an equalizer; the feature simply stays hidden then
        val eq = try {
            Equalizer(0, audioSessionId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return
        equalizer = eq

        try {
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val presets = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
            val presetLevels = (0 until eq.numberOfPresets).map { preset ->
                eq.usePreset(preset.toShort())
                (0 until bands).map { eq.getBandLevel(it.toShort()).toInt() }
            }
            PlaybackSettings.publishEqualizerInfo(
                EqualizerInfo(
                    bandFrequenciesHz = (0 until bands).map { eq.getCenterFreq(it.toShort()) / 1000 },
                    minLevel = range[0].toInt(),
                    maxLevel = range[1].toInt(),
                    presets = presets,
                    presetLevels = presetLevels
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        applyEqualizer(PlaybackSettings.equalizer.value)
    }

    private fun applyEqualizer(settings: EqualizerSettings) {
        val eq = equalizer ?: return
        try {
            if (settings.enabled) {
                if (settings.preset in 0 until eq.numberOfPresets) {
                    eq.usePreset(settings.preset.toShort())
                } else {
                    settings.bandLevels.forEachIndexed { band, level ->
                        if (band < eq.numberOfBands) eq.setBandLevel(band.toShort(), level.toShort())
                    }
                }
            }
            eq.enabled = settings.enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseEqualizer() {
        equalizer?.release()
        equalizer = null
    }

    private class MediaSessionCallback : MediaSession.Callback {
        // Can override commands or handle custom actions if needed
    }

    private companion object {
        const val FADE_OUT_MS = 10_000L
    }
}
