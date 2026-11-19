package com.example.playback

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object AudioSpectrumAnalyzer {
    const val BAND_COUNT = 28
    const val HISTORY_BARS = 44

    private val _bandsFlow = MutableStateFlow(FloatArray(BAND_COUNT))
    val bandsFlow: StateFlow<FloatArray> = _bandsFlow.asStateFlow()

    private val _beatEnergyFlow = MutableStateFlow(0f)
    val beatEnergyFlow: StateFlow<Float> = _beatEnergyFlow.asStateFlow()

    private val _waveformHistoryFlow = MutableStateFlow(FloatArray(HISTORY_BARS))
    val waveformHistoryFlow: StateFlow<FloatArray> = _waveformHistoryFlow.asStateFlow()

    private val stateLock = Any()
    private val smoothedBands = FloatArray(BAND_COUNT)
    private val historyRing = FloatArray(HISTORY_BARS)
    private var smoothedBeat = 0f
    private var runningPeak = 0.25f

    internal fun publishSpectrum(rawBands: FloatArray, rawBeat: Float) {
        val next = FloatArray(BAND_COUNT)
        val historyOut = FloatArray(HISTORY_BARS)
        val beatOut: Float
        synchronized(stateLock) {
            var frameMax = 0.05f
            for (i in 0 until BAND_COUNT) {
                if (rawBands[i] > frameMax) frameMax = rawBands[i]
            }
            // Adaptive Auto-Gain Control: snappy attack, smooth release
            runningPeak = if (frameMax > runningPeak) {
                runningPeak + (frameMax - runningPeak) * 0.62f
            } else {
                (runningPeak * 0.94f).coerceAtLeast(0.16f)
            }

            val gainScale = (0.98f / runningPeak).coerceIn(0.85f, 4.4f)
            var energySum = 0f
            for (i in 0 until BAND_COUNT) {
                val target = (rawBands[i] * gainScale).coerceIn(0f, 1f)
                val prev = smoothedBands[i]
                val coeff = if (target > prev) 0.96f else 0.72f
                val v = prev + (target - prev) * coeff
                smoothedBands[i] = v
                next[i] = v
                energySum += v
            }

            val beatTarget = (rawBeat * gainScale * 0.98f).coerceIn(0f, 1f)
            val beatCoeff = if (beatTarget > smoothedBeat) 0.96f else 0.68f
            smoothedBeat += (beatTarget - smoothedBeat) * beatCoeff
            beatOut = smoothedBeat

            val sampleAmp = ((energySum / BAND_COUNT) * 0.65f + beatOut * 0.55f).coerceIn(0.06f, 1f)
            System.arraycopy(historyRing, 1, historyRing, 0, HISTORY_BARS - 1)
            historyRing[HISTORY_BARS - 1] = sampleAmp
            System.arraycopy(historyRing, 0, historyOut, 0, HISTORY_BARS)
        }
        _bandsFlow.value = next
        _beatEnergyFlow.value = beatOut
        _waveformHistoryFlow.value = historyOut
    }

    fun reset() {
        synchronized(stateLock) {
            smoothedBands.fill(0f)
            historyRing.fill(0f)
            smoothedBeat = 0f
            runningPeak = 0.25f
        }
        _bandsFlow.value = FloatArray(BAND_COUNT)
        _beatEnergyFlow.value = 0f
        _waveformHistoryFlow.value = FloatArray(HISTORY_BARS)
    }
}

@OptIn(UnstableApi::class)
class AudioSpectrumProcessor : BaseAudioProcessor() {

    private var sampleRateHz: Int = 44100
    private var channelCount: Int = 2
    private var encoding: Int = C.ENCODING_PCM_16BIT
    private var lastPublishMs: Long = 0L

    private val fftSize = 512
    private val ringMono = FloatArray(fftSize)
    private var ringCursor = 0
    private var ringFilled = 0

    private val real = FloatArray(fftSize)
    private val imag = FloatArray(fftSize)
    private val window = FloatArray(fftSize) { i ->
        (0.5 - 0.5 * cos(2.0 * PI * i / (fftSize - 1))).toFloat()
    }
    private val rawBands = FloatArray(AudioSpectrumAnalyzer.BAND_COUNT)
    private val bandStartBins = IntArray(AudioSpectrumAnalyzer.BAND_COUNT)
    private val bandEndBins = IntArray(AudioSpectrumAnalyzer.BAND_COUNT)
    private val bandTilts = FloatArray(AudioSpectrumAnalyzer.BAND_COUNT) { band ->
        0.85f + (band / AudioSpectrumAnalyzer.BAND_COUNT.toFloat()) * 1.35f
    }

    init {
        recomputeBandMappings(sampleRateHz)
    }

    private fun recomputeBandMappings(rateHz: Int) {
        val half = fftSize / 2
        val maxUsableBin = ((14000.0 / (rateHz / 2.0)) * half).toInt().coerceIn(24, half - 1)
        for (band in 0 until AudioSpectrumAnalyzer.BAND_COUNT) {
            val t0 = band.toDouble() / AudioSpectrumAnalyzer.BAND_COUNT
            val t1 = (band + 1).toDouble() / AudioSpectrumAnalyzer.BAND_COUNT
            val b0 = (maxUsableBin.toDouble().pow(t0)).toInt().coerceIn(1, maxUsableBin)
            val b1 = (maxUsableBin.toDouble().pow(t1)).toInt().coerceIn(b0, maxUsableBin)
            bandStartBins[band] = b0
            bandEndBins[band] = b1
        }
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRateHz = inputAudioFormat.sampleRate.coerceAtLeast(8000)
        channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        encoding = inputAudioFormat.encoding
        recomputeBandMappings(sampleRateHz)
        ringMono.fill(0f)
        ringCursor = 0
        ringFilled = 0
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return

        if (encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT) {
            ingestIntoRing(inputBuffer.duplicate())
            val now = SystemClock.elapsedRealtime()
            if (now - lastPublishMs >= 6L && ringFilled >= 64) {
                lastPublishMs = now
                analyzeRing()
            }
        }

        val out = replaceOutputBuffer(remaining)
        out.put(inputBuffer)
        out.flip()
    }

    private fun ingestIntoRing(dup: ByteBuffer) {
        dup.order(ByteOrder.LITTLE_ENDIAN)
        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val bytesPerFrame = bytesPerSample * channelCount
        if (bytesPerFrame <= 0) return

        val availableFrames = dup.remaining() / bytesPerFrame
        if (availableFrames <= 0) return

        // Consecutive samples from the most recent window (no strided subsampling aliasing)
        val startFrame = (availableFrames - fftSize).coerceAtLeast(0)
        val basePos = dup.position()
        for (f in startFrame until availableFrames) {
            val byteIdx = basePos + f * bytesPerFrame
            var mono = 0f
            for (ch in 0 until channelCount) {
                val sampleByteIdx = byteIdx + ch * bytesPerSample
                val sample = if (encoding == C.ENCODING_PCM_FLOAT) {
                    dup.getFloat(sampleByteIdx).coerceIn(-1f, 1f)
                } else {
                    dup.getShort(sampleByteIdx) / 32768f
                }
                mono += sample
            }
            ringMono[ringCursor] = mono / channelCount.toFloat()
            ringCursor = (ringCursor + 1) % fftSize
            if (ringFilled < fftSize) ringFilled++
        }
    }

    private fun analyzeRing() {
        var energySum = 0f
        for (i in 0 until fftSize) {
            val idx = (ringCursor + i) % fftSize
            val s = ringMono[idx]
            energySum += s * s
            real[i] = s * window[i]
            imag[i] = 0f
        }

        val rms = sqrt(energySum / fftSize)
        if (rms < 0.0004f) {
            rawBands.fill(0f)
            AudioSpectrumAnalyzer.publishSpectrum(rawBands, 0f)
            return
        }

        fftInPlace(real, imag)

        // Exponentially spaced bins across ~43 Hz .. ~14 kHz precomputed in onConfigure()
        var bassAccum = 0f
        var midAccum = 0f
        val invNorm = 1f / (fftSize * 0.18f)

        for (band in 0 until AudioSpectrumAnalyzer.BAND_COUNT) {
            val b0 = bandStartBins[band]
            val b1 = bandEndBins[band]
            var peakMag = 0f
            for (bin in b0..b1) {
                val re = real[bin]
                val im = imag[bin]
                val mag = sqrt(re * re + im * im) * invNorm
                if (mag > peakMag) peakMag = mag
            }
            val valNorm = (ln(1f + peakMag * 10f * bandTilts[band]) / 1.85f).coerceIn(0f, 1f)
            rawBands[band] = valNorm
            if (band in 0..5) bassAccum += valNorm
            if (band in 6..15) midAccum += valNorm
        }

        val bassBeat = ((bassAccum / 6f) * 1.1f + (midAccum / 10f) * 0.35f + rms * 1.4f).coerceIn(0f, 1f)
        AudioSpectrumAnalyzer.publishSpectrum(rawBands, bassBeat)
    }

    private fun fftInPlace(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while ((j and bit) != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }

        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wLenRe = cos(angle).toFloat()
            val wLenIm = sin(angle).toFloat()
            var i = 0
            while (i < n) {
                var wRe = 1f
                var wIm = 0f
                val halfLen = len shr 1
                for (k in 0 until halfLen) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val vRe = re[i + k + halfLen] * wRe - im[i + k + halfLen] * wIm
                    val vIm = re[i + k + halfLen] * wIm + im[i + k + halfLen] * wRe
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[i + k + halfLen] = uRe - vRe
                    im[i + k + halfLen] = uIm - vIm
                    val nextWRe = wRe * wLenRe - wIm * wLenIm
                    wIm = wRe * wLenIm + wIm * wLenRe
                    wRe = nextWRe
                }
                i += len
            }
            len = len shl 1
        }
    }

    override fun onFlush() {
        ringMono.fill(0f)
        ringFilled = 0
        AudioSpectrumAnalyzer.reset()
    }

    override fun onReset() {
        ringMono.fill(0f)
        ringFilled = 0
        AudioSpectrumAnalyzer.reset()
    }
}
