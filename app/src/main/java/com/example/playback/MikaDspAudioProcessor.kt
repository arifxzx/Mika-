package com.example.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.model.SoundControlSettings
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Real-time PCM DSP processor for ExoPlayer's DefaultAudioSink.
 * Applies low-latency effects: Mono mixdown, 8D auto-pan, Instrumental center vocal
 * reduction, Crossfeed binaural bleed, Tube Warmth soft clipping, Dynamic Compressor,
 * Echo delay tap, and Stereo Chorus modulation.
 */
@OptIn(UnstableApi::class)
class MikaDspAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var settings: SoundControlSettings = SoundControlSettings()

    private var sampleRateHz: Int = 44100
    private var channelCount: Int = 2
    private var encoding: Int = C.ENCODING_PCM_16BIT

    // LFO phase for 8D auto-pan and chorus modulation
    private var eightDPhase: Float = 0f
    private var chorusPhase: Float = 0f

    // Crossfeed state (low-pass filtered opposite channel bleed)
    private var lpCrossL: Float = 0f
    private var lpCrossR: Float = 0f

    // Echo & Chorus delay ring buffers (~320 ms max at 48 kHz)
    private val maxDelaySamples = 20480
    private var delayBufL = FloatArray(maxDelaySamples)
    private var delayBufR = FloatArray(maxDelaySamples)
    private var delayCursor = 0

    // Compressor envelope
    private var compEnvelope: Float = 0f

    fun updateSettings(newSettings: SoundControlSettings) {
        settings = newSettings
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRateHz = inputAudioFormat.sampleRate.coerceAtLeast(8000)
        channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        encoding = inputAudioFormat.encoding
        resetBuffers()
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return

        val st = settings
        val anyDspActive = st.monoAudio ||
            st.eightDEnabled ||
            st.instrumentalEnabled ||
            st.crossfeedEnabled ||
            st.tubeWarmthEnabled ||
            st.compressorEnabled ||
            st.echoEnabled ||
            st.chorusEnabled

        val isSupportedPcm = (encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT) && channelCount >= 1
        if (!anyDspActive || !isSupportedPcm) {
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            out.flip()
            return
        }

        val out = replaceOutputBuffer(remaining)
        out.order(ByteOrder.LITTLE_ENDIAN)
        val dup = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)

        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val bytesPerFrame = bytesPerSample * channelCount
        val frameCount = remaining / bytesPerFrame

        val echoDelaySamples = (sampleRateHz * 0.22f).toInt().coerceIn(512, maxDelaySamples - 1)
        val chorusBaseDelay = (sampleRateHz * 0.018f).toInt().coerceIn(64, maxDelaySamples - 256)
        val chorusDepth = (sampleRateHz * 0.005f)
        val eightDInc = (2f * PI.toFloat() * 0.14f) / sampleRateHz.toFloat()
        val chorusInc = (2f * PI.toFloat() * 0.85f) / sampleRateHz.toFloat()

        for (f in 0 until frameCount) {
            val frameOffset = dup.position() + f * bytesPerFrame
            var left = readSample(dup, frameOffset)
            var right = if (channelCount >= 2) readSample(dup, frameOffset + bytesPerSample) else left

            // 1. Instrumental mode (attenuate dead-center vocal energy, preserve stereo instruments)
            if (st.instrumentalEnabled && channelCount >= 2) {
                val mid = (left + right) * 0.5f
                val side = (left - right) * 0.5f
                left = side * 1.15f + mid * 0.28f
                right = -side * 1.15f + mid * 0.28f
            }

            // 2. Crossfeed (natural headphone binaural cross-bleed)
            if (st.crossfeedEnabled && channelCount >= 2) {
                lpCrossL += 0.18f * (right - lpCrossL)
                lpCrossR += 0.18f * (left - lpCrossR)
                left = left * 0.88f + lpCrossL * 0.22f
                right = right * 0.88f + lpCrossR * 0.22f
            }

            // 3. Tube Warmth (subtle even-harmonic soft saturation)
            if (st.tubeWarmthEnabled) {
                left = tanh((left * 1.45f).toDouble()).toFloat() * 0.84f
                right = tanh((right * 1.45f).toDouble()).toFloat() * 0.84f
            }

            // 4. Dynamic Compressor
            if (st.compressorEnabled) {
                val peak = maxOf(abs(left), abs(right))
                val alpha = if (peak > compEnvelope) 0.02f else 0.0008f
                compEnvelope += alpha * (peak - compEnvelope)
                val gain = if (compEnvelope > 0.35f) {
                    (0.35f + (compEnvelope - 0.35f) * 0.42f) / compEnvelope
                } else {
                    1.15f
                }
                left *= gain
                right *= gain
            }

            // 5. Delay ring write & Echo / Chorus taps
            delayBufL[delayCursor] = left
            delayBufR[delayCursor] = right

            if (st.chorusEnabled) {
                chorusPhase += chorusInc
                if (chorusPhase > 2f * PI.toFloat()) chorusPhase -= 2f * PI.toFloat()
                val modDelay = (chorusBaseDelay + sin(chorusPhase) * chorusDepth).toInt().coerceIn(1, maxDelaySamples - 1)
                val readIdx = (delayCursor - modDelay + maxDelaySamples) % maxDelaySamples
                left = left * 0.82f + delayBufL[readIdx] * 0.36f
                right = right * 0.82f + delayBufR[readIdx] * 0.36f
            }

            if (st.echoEnabled) {
                val echoIdx = (delayCursor - echoDelaySamples + maxDelaySamples) % maxDelaySamples
                val tapL = delayBufL[echoIdx]
                val tapR = delayBufR[echoIdx]
                delayBufL[delayCursor] = (left + tapR * 0.32f).coerceIn(-1f, 1f)
                delayBufR[delayCursor] = (right + tapL * 0.32f).coerceIn(-1f, 1f)
                left = left * 0.90f + tapR * 0.30f
                right = right * 0.90f + tapL * 0.30f
            }

            delayCursor = (delayCursor + 1) % maxDelaySamples

            // 6. 8D Audio (smooth binaural circular pan)
            if (st.eightDEnabled && channelCount >= 2) {
                eightDPhase += eightDInc
                if (eightDPhase > 2f * PI.toFloat()) eightDPhase -= 2f * PI.toFloat()
                val pan = sin(eightDPhase) // -1..1
                val gainL = (0.5f * (1f - pan * 0.72f) + 0.25f).coerceIn(0.22f, 1.15f)
                val gainR = (0.5f * (1f + pan * 0.72f) + 0.25f).coerceIn(0.22f, 1.15f)
                val depthWeight = 0.85f + 0.15f * cos(eightDPhase)
                left *= gainL * depthWeight
                right *= gainR * depthWeight
            }

            // 7. Mono mixdown (applied last so mono works across all effects)
            if (st.monoAudio && channelCount >= 2) {
                val mono = (left + right) * 0.5f
                left = mono
                right = mono
            }

            writeSample(out, left.coerceIn(-1f, 1f))
            if (channelCount >= 2) {
                writeSample(out, right.coerceIn(-1f, 1f))
            }
            for (ch in 2 until channelCount) {
                writeSample(out, readSample(dup, frameOffset + ch * bytesPerSample))
            }
        }

        // Consume input buffer completely
        inputBuffer.position(inputBuffer.limit())
        out.flip()
    }

    private fun readSample(buf: ByteBuffer, byteOffset: Int): Float {
        return if (encoding == C.ENCODING_PCM_FLOAT) {
            buf.getFloat(byteOffset).coerceIn(-1f, 1f)
        } else {
            buf.getShort(byteOffset) / 32768f
        }
    }

    private fun writeSample(out: ByteBuffer, value: Float) {
        if (encoding == C.ENCODING_PCM_FLOAT) {
            out.putFloat(value)
        } else {
            out.putShort((value * 32767f).toInt().coerceIn(-32768, 32767).toShort())
        }
    }

    private fun resetBuffers() {
        delayBufL.fill(0f)
        delayBufR.fill(0f)
        delayCursor = 0
        lpCrossL = 0f
        lpCrossR = 0f
        compEnvelope = 0f
    }

    override fun onFlush() {
        resetBuffers()
    }

    override fun onReset() {
        resetBuffers()
    }
}
