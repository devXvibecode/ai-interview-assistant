package com.assistant.app.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.assistant.app.utils.Constants

class SpeechService(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var isContinuous = false
    private val handler = Handler(Looper.getMainLooper())

    private var onFinalResult: ((String, Float) -> Unit)? = null
    private var onPartial: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onStarted: (() -> Unit)? = null
    private var onSpeechBegin: (() -> Unit)? = null
    private var onSpeechEnd: (() -> Unit)? = null
    private var onVolumeChanged: ((Float) -> Unit)? = null

    private val speechBuffer = StringBuilder()
    private var bufferFlushRunnable: Runnable? = null
    private var restartCount = 0
    private var lastPartialText = ""
    private var currentSessionMaxRms = 0f
    private var currentSessionAvgRms = 0f
    private var rmsReadings = mutableListOf<Float>()
    private var isSpeaking = false

    // Extended silence detection
    private var silenceRunnable: Runnable? = null
    private var lastSoundTime = 0L

    fun startContinuous(
        onResult: (String, Float) -> Unit,
        onPartialResult: (String) -> Unit = {},
        onErr: (String) -> Unit = {},
        onStarted: () -> Unit = {},
        onSpeechBegin: () -> Unit = {},
        onSpeechEnd: () -> Unit = {},
        onVolume: (Float) -> Unit = {}
    ) {
        this.onFinalResult = onResult
        this.onPartial = onPartialResult
        this.onError = onErr
        this.onStarted = onStarted
        this.onSpeechBegin = onSpeechBegin
        this.onSpeechEnd = onSpeechEnd
        this.onVolumeChanged = onVolume
        isContinuous = true
        restartCount = 0
        lastPartialText = ""
        rmsReadings.clear()
        startFresh()
    }

    fun stopContinuous() {
        isContinuous = false
        handler.removeCallbacksAndMessages(null)
        bufferFlushRunnable = null
        silenceRunnable = null
        speechBuffer.clear()
        lastPartialText = ""
        rmsReadings.clear()
        isSpeaking = false
        destroyRecognizer()
    }

    private fun startFresh() {
        if (!isContinuous) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError?.invoke("Speech recognition unavailable")
            return
        }

        restartCount++
        if (restartCount > Constants.MAX_RESTARTS_BEFORE_CLEANUP) {
            restartCount = 0
            destroyRecognizer()
            handler.postDelayed({
                if (isContinuous) buildAndStart()
            }, 400)
            return
        }

        destroyRecognizer()
        buildAndStart()
    }

    private fun buildAndStart() {
        if (!isContinuous) return

        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            scheduleRestart(800)
            return
        }

        currentSessionMaxRms = 0f
        currentSessionAvgRms = 0f
        rmsReadings.clear()

        recognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                handler.post { onStarted?.invoke() }
            }

            override fun onBeginningOfSpeech() {
                isSpeaking = true
                lastSoundTime = System.currentTimeMillis()
                cancelSilenceCheck()
                cancelBufferFlush()
                rmsReadings.clear()
                handler.post { onSpeechBegin?.invoke() }
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (rmsdB > 0) {
                    lastSoundTime = System.currentTimeMillis()
                    rmsReadings.add(rmsdB)
                    if (rmsdB > currentSessionMaxRms) {
                        currentSessionMaxRms = rmsdB
                    }
                    handler.post { onVolumeChanged?.invoke(rmsdB) }
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onEndOfSpeech() {
                isSpeaking = false
                // Calculate average volume for this speech segment
                if (rmsReadings.isNotEmpty()) {
                    currentSessionAvgRms = rmsReadings.average().toFloat()
                }
                handler.post { onSpeechEnd?.invoke() }

                // Start silence timer — wait to see if more speech comes
                startSilenceCheck()
            }

            override fun onResults(results: Bundle?) {
                isSpeaking = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim() ?: ""

                if (text.isNotBlank()) {
                    if (speechBuffer.isNotEmpty()) speechBuffer.append(" ")
                    speechBuffer.append(text)

                    handler.post {
                        onPartial?.invoke(speechBuffer.toString())
                    }
                }

                // Don't flush yet — wait for silence to confirm speaker is done
                startSilenceCheck()
                scheduleRestart(Constants.RESTART_DELAY_MS)
            }

            override fun onPartialResults(partial: Bundle?) {
                val text = partial
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim() ?: ""

                if (text.isNotBlank() && text != lastPartialText) {
                    lastPartialText = text
                    lastSoundTime = System.currentTimeMillis()

                    val preview = if (speechBuffer.isNotEmpty()) {
                        "${speechBuffer} $text"
                    } else text

                    handler.post { onPartial?.invoke(preview) }
                }
            }

            override fun onError(error: Int) {
                isSpeaking = false
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // No speech detected — if buffer has content, flush it
                        if (speechBuffer.isNotBlank()) {
                            flushBuffer()
                        }
                        scheduleRestart(Constants.RESTART_DELAY_MS)
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        destroyRecognizer()
                        scheduleRestart(600)
                    }
                    SpeechRecognizer.ERROR_CLIENT -> {
                        destroyRecognizer()
                        scheduleRestart(500)
                    }
                    SpeechRecognizer.ERROR_AUDIO -> {
                        handler.post { onError?.invoke("Audio error") }
                        scheduleRestart(1500)
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        handler.post { onError?.invoke("Mic permission denied") }
                    }
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_SERVER -> {
                        handler.post { onError?.invoke("Network issue") }
                        scheduleRestart(2000)
                    }
                    else -> scheduleRestart(400)
                }
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Constants.SPEECH_LANGUAGE)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                Constants.SILENCE_TIMEOUT_MS
            )
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            scheduleRestart(500)
        }
    }

    /**
     * Silence check: after speech ends, wait to see if more comes.
     * If no more speech within BUFFER_FLUSH_DELAY_MS → flush the whole buffer.
     */
    private fun startSilenceCheck() {
        cancelSilenceCheck()
        silenceRunnable = Runnable {
            val timeSinceLastSound = System.currentTimeMillis() - lastSoundTime
            if (timeSinceLastSound >= Constants.BUFFER_FLUSH_DELAY_MS && speechBuffer.isNotBlank()) {
                flushBuffer()
            }
        }
        handler.postDelayed(silenceRunnable!!, Constants.BUFFER_FLUSH_DELAY_MS)
    }

    private fun cancelSilenceCheck() {
        silenceRunnable?.let { handler.removeCallbacks(it) }
        silenceRunnable = null
    }

    private fun cancelBufferFlush() {
        bufferFlushRunnable?.let { handler.removeCallbacks(it) }
        bufferFlushRunnable = null
    }

    private fun flushBuffer() {
        val text = speechBuffer.toString().trim()
        val avgVolume = currentSessionAvgRms
        speechBuffer.clear()
        lastPartialText = ""
        cancelSilenceCheck()
        cancelBufferFlush()

        if (text.isNotBlank()) {
            val wordCount = text.split("\\s+".toRegex()).size
            if (wordCount >= Constants.MIN_WORDS_TO_PROCESS) {
                handler.post { onFinalResult?.invoke(text, avgVolume) }
            }
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!isContinuous) return
        handler.postDelayed({
            if (isContinuous) startFresh()
        }, delayMs)
    }

    private fun destroyRecognizer() {
        isSpeaking = false
        try { recognizer?.stopListening() } catch (_: Exception) {}
        try { recognizer?.cancel() } catch (_: Exception) {}
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }

    fun destroy() {
        isContinuous = false
        handler.removeCallbacksAndMessages(null)
        bufferFlushRunnable = null
        silenceRunnable = null
        speechBuffer.clear()
        rmsReadings.clear()
        lastPartialText = ""
        isSpeaking = false
        destroyRecognizer()
        onFinalResult = null
        onPartial = null
        onError = null
        onStarted = null
        onSpeechBegin = null
        onSpeechEnd = null
        onVolumeChanged = null
    }
}