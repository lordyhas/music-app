package com.lordyhas.muzik.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SleepTimer {
    data object Off : SleepTimer
    data class At(val endsAtMillis: Long, val totalMillis: Long) : SleepTimer
    data object EndOfTrack : SleepTimer
}

/** User's equalizer choice. [preset] is -1 when the bands were tuned manually. */
data class EqualizerSettings(
    val enabled: Boolean = false,
    val preset: Int = -1,
    val bandLevels: List<Int> = emptyList()
)

/** Device equalizer capabilities, published by the playback service once the effect is attached. */
data class EqualizerInfo(
    val bandFrequenciesHz: List<Int>,
    val minLevel: Int,
    val maxLevel: Int,
    val presets: List<String>,
    val presetLevels: List<List<Int>>
)

/**
 * Process-wide playback settings shared between the UI and [MusicPlaybackService].
 *
 * The UI writes here, the service observes and applies (sleep timer, speed, equalizer), so the
 * settings keep working when the activity is gone. Persistent values are stored in SharedPreferences.
 */
object PlaybackSettings {

    private const val PREFS = "muzik_playback"
    private const val KEY_SPEED = "speed"
    private const val KEY_EQ_ENABLED = "eq_enabled"
    private const val KEY_EQ_PRESET = "eq_preset"
    private const val KEY_EQ_LEVELS = "eq_levels"

    private var prefs: SharedPreferences? = null

    private val _sleepTimer = MutableStateFlow<SleepTimer>(SleepTimer.Off)
    val sleepTimer: StateFlow<SleepTimer> = _sleepTimer.asStateFlow()

    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _equalizer = MutableStateFlow(EqualizerSettings())
    val equalizer: StateFlow<EqualizerSettings> = _equalizer.asStateFlow()

    private val _equalizerInfo = MutableStateFlow<EqualizerInfo?>(null)
    val equalizerInfo: StateFlow<EqualizerInfo?> = _equalizerInfo.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _speed.value = p.getFloat(KEY_SPEED, 1f)
        _equalizer.value = EqualizerSettings(
            enabled = p.getBoolean(KEY_EQ_ENABLED, false),
            preset = p.getInt(KEY_EQ_PRESET, -1),
            bandLevels = p.getString(KEY_EQ_LEVELS, null)
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                .orEmpty()
        )
    }

    // ─── Sleep timer ────────────────────────────────────────────────────────

    fun setSleepTimer(minutes: Int) {
        val total = minutes * 60_000L
        _sleepTimer.value = SleepTimer.At(System.currentTimeMillis() + total, total)
    }

    fun setSleepAtEndOfTrack() {
        _sleepTimer.value = SleepTimer.EndOfTrack
    }

    fun cancelSleepTimer() {
        _sleepTimer.value = SleepTimer.Off
    }

    // ─── Speed ──────────────────────────────────────────────────────────────

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2f)
        _speed.value = clamped
        prefs?.edit()?.putFloat(KEY_SPEED, clamped)?.apply()
    }

    // ─── Equalizer ──────────────────────────────────────────────────────────

    fun setEqualizerEnabled(enabled: Boolean) {
        updateEqualizer(_equalizer.value.copy(enabled = enabled))
    }

    fun selectEqualizerPreset(preset: Int) {
        val levels = _equalizerInfo.value?.presetLevels?.getOrNull(preset) ?: return
        updateEqualizer(_equalizer.value.copy(enabled = true, preset = preset, bandLevels = levels))
    }

    fun setEqualizerBandLevel(band: Int, level: Int) {
        val info = _equalizerInfo.value ?: return
        val current = _equalizer.value.bandLevels
            .takeIf { it.size == info.bandFrequenciesHz.size }
            ?: List(info.bandFrequenciesHz.size) { 0 }
        val updated = current.toMutableList().also { it[band] = level.coerceIn(info.minLevel, info.maxLevel) }
        updateEqualizer(_equalizer.value.copy(enabled = true, preset = -1, bandLevels = updated))
    }

    fun resetEqualizer() {
        val bands = _equalizerInfo.value?.bandFrequenciesHz?.size ?: 0
        updateEqualizer(_equalizer.value.copy(preset = -1, bandLevels = List(bands) { 0 }))
    }

    private fun updateEqualizer(settings: EqualizerSettings) {
        _equalizer.value = settings
        prefs?.edit()
            ?.putBoolean(KEY_EQ_ENABLED, settings.enabled)
            ?.putInt(KEY_EQ_PRESET, settings.preset)
            ?.putString(KEY_EQ_LEVELS, settings.bandLevels.joinToString(","))
            ?.apply()
    }

    // ─── Service side ───────────────────────────────────────────────────────

    internal fun publishEqualizerInfo(info: EqualizerInfo?) {
        _equalizerInfo.value = info
    }

    internal fun onSleepTimerFinished() {
        _sleepTimer.value = SleepTimer.Off
    }
}
