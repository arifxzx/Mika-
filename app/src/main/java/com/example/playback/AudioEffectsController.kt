@file:Suppress("DEPRECATION")

package com.example.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import com.example.model.EqPreset
import com.example.model.EqualizerState
import com.example.model.SoundControlSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioEffectsController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null
    private var currentSessionId: Int = 0
    private var latestSoundControl: SoundControlSettings = SoundControlSettings()

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentSessionId) return
        releaseEffects()
        currentSessionId = audioSessionId

        runCatching {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            val bands = eq.numberOfBands.toInt().coerceAtLeast(1)
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            val freqs = (0 until bands).map { idx ->
                (eq.getCenterFreq(idx.toShort()) / 1000).coerceAtLeast(20)
            }

            val currentLevels = _state.value.bandLevelsMb
            val normalizedLevels = (0 until bands).map { idx ->
                currentLevels.getOrElse(idx) { 0 }.coerceIn(minMb, maxMb)
            }

            _state.value = _state.value.copy(
                bandFrequenciesHz = freqs,
                bandLevelsMb = normalizedLevels,
                minBandLevelMb = minMb,
                maxBandLevelMb = maxMb,
                isSupported = true
            )
            applyStateToHardware(_state.value)
        }.onFailure {
            _state.value = _state.value.copy(isSupported = false)
        }

        runCatching {
            bassBoost = BassBoost(0, audioSessionId)
            applyBassBoost(_state.value.enabled, _state.value.bassBoostStrength)
        }

        runCatching {
            virtualizer = Virtualizer(0, audioSessionId)
            applyVirtualizer(_state.value.enabled, _state.value.virtualizerStrength)
        }

        runCatching {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            applyLoudnessEnhancer(latestSoundControl)
        }

        runCatching {
            presetReverb = PresetReverb(0, audioSessionId)
            applyReverb(latestSoundControl.reverbEnabled)
        }
    }

    fun updateState(newState: EqualizerState) {
        val current = _state.value
        val updated = newState.copy(
            bandFrequenciesHz = current.bandFrequenciesHz,
            minBandLevelMb = current.minBandLevelMb,
            maxBandLevelMb = current.maxBandLevelMb,
            isSupported = current.isSupported
        )
        _state.value = updated
        applyStateToHardware(updated)
    }

    fun updateSoundControl(soundControl: SoundControlSettings) {
        latestSoundControl = soundControl
        applyLoudnessEnhancer(soundControl)
        applyReverb(soundControl.reverbEnabled)
    }

    fun presetLevelsFor(preset: EqPreset, bandCount: Int, minMb: Int, maxMb: Int): List<Int> {
        val base5 = when (preset) {
            EqPreset.NORMAL -> listOf(150, 0, 0, 100, 200)
            EqPreset.CLASSICAL -> listOf(350, 200, -100, 250, 400)
            EqPreset.DANCE -> listOf(550, 300, 0, 250, 450)
            EqPreset.FLAT -> listOf(0, 0, 0, 0, 0)
            EqPreset.HIP_HOP -> listOf(600, 400, 0, 150, 300)
            EqPreset.JAZZ -> listOf(300, 150, -150, 250, 350)
            EqPreset.POP -> listOf(-100, 200, 450, 250, -100)
            EqPreset.ROCK -> listOf(450, 250, -150, 300, 500)
            EqPreset.CUSTOM -> _state.value.bandLevelsMb
        }
        return (0 until bandCount).map { idx ->
            val mappedIdx = ((idx.toFloat() / bandCount.coerceAtLeast(1)) * base5.size)
                .toInt()
                .coerceIn(0, base5.lastIndex)
            base5[mappedIdx].coerceIn(minMb, maxMb)
        }
    }

    private fun applyStateToHardware(st: EqualizerState) {
        runCatching {
            equalizer?.let { eq ->
                eq.enabled = st.enabled
                val bands = eq.numberOfBands.toInt()
                for (i in 0 until bands) {
                    val lvl = st.bandLevelsMb.getOrElse(i) { 0 }
                        .coerceIn(st.minBandLevelMb, st.maxBandLevelMb)
                        .toShort()
                    eq.setBandLevel(i.toShort(), lvl)
                }
            }
        }
        applyBassBoost(st.enabled, st.bassBoostStrength)
        applyVirtualizer(st.enabled, st.virtualizerStrength)
    }

    private fun applyBassBoost(enabled: Boolean, strength: Int) {
        runCatching {
            bassBoost?.let { bb ->
                bb.enabled = enabled && strength > 0
                if (bb.strengthSupported) {
                    bb.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
        }
    }

    private fun applyVirtualizer(enabled: Boolean, strength: Int) {
        runCatching {
            virtualizer?.let { v ->
                v.enabled = enabled && strength > 0
                if (v.strengthSupported) {
                    v.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
        }
    }

    private fun applyLoudnessEnhancer(soundControl: SoundControlSettings) {
        runCatching {
            loudnessEnhancer?.let { le ->
                val enabled = soundControl.loudnessEnhancerEnabled && soundControl.preAmpDb > 0.05f
                le.enabled = enabled
                le.setTargetGain((soundControl.preAmpDb * 100f).toInt().coerceIn(0, 1500))
            }
        }
    }

    private fun applyReverb(enabled: Boolean) {
        runCatching {
            presetReverb?.let { rv ->
                rv.preset = if (enabled) PresetReverb.PRESET_MEDIUMHALL else PresetReverb.PRESET_NONE
                rv.enabled = enabled
            }
        }
    }

    fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        runCatching { presetReverb?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        presetReverb = null
        currentSessionId = 0
    }
}
