package dev.arbjerg.ukulele.features

import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.*
import dev.arbjerg.ukulele.config.BotProps
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AzureSpeechToText(val botProps: BotProps) {
    final var speechConfig : SpeechConfig = SpeechConfig.fromSubscription(botProps.speechToken, "southcentralus")
    val log: Logger = LoggerFactory.getLogger(javaClass)
    init {
        speechConfig.speechRecognitionLanguage = "es-MX"
    }

    suspend fun speechToText(stream: PullAudioInputStreamCallback) : String? {
        var out = "-"
        runBlocking {
            val audioFormat = AudioStreamFormat.getWaveFormatPCM(16000, 16, 1)
            val audioStream = AudioInputStream.createPullStream(stream, audioFormat)
            val audioConfig = AudioConfig.fromStreamInput(audioStream)

            val speechRecognizer = SpeechRecognizer(speechConfig, audioConfig)

            speechRecognizer.recognizing.addEventListener { _, eventArgs ->
                var result = eventArgs.result
                log.info("RECOGNIZING Result Text: ${eventArgs.result.text} Reason: ${eventArgs.result.reason}")
            }

            speechRecognizer.recognized.addEventListener { _, eventArgs ->
                var result = eventArgs.result
                log.info("RECOGNIZED Result Text: ${eventArgs.result.text} Reason: ${eventArgs.result.reason}")
                out += result.text
            }

            speechRecognizer.startContinuousRecognitionAsync().get()
            log.info("Starting Speech Recognition....")

            runBlocking {
                delay(20000)
                speechRecognizer.stopContinuousRecognitionAsync().get()
            }

            log.info("Done Speech Recognition")
        }

        return out
    }
}