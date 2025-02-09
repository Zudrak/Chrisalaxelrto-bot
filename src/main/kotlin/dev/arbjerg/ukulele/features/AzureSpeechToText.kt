package dev.arbjerg.ukulele.features

import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.*
import com.sedmelluq.discord.lavaplayer.container.wav.WaveFormatType
import dev.arbjerg.ukulele.config.BotProps
import kotlinx.coroutines.*
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat

@Service
class AzureSpeechToText(var botProps: BotProps) {
    final var speechConfig : SpeechConfig = SpeechConfig.fromSubscription(botProps.speechToken, "southcentralus")
    val log: Logger = LoggerFactory.getLogger(javaClass)
    var audioConfig : AudioConfig? = null
    init {
        speechConfig.speechRecognitionLanguage = "es-MX"
    }

    fun setupAudioStream(stream: PullAudioInputStreamCallback) {
        val discordFormat = AudioReceiveHandler.OUTPUT_FORMAT
        var audioFormat = AudioStreamFormat.getWaveFormat(discordFormat.sampleRate.toLong(), discordFormat.sampleSizeInBits.toShort(), discordFormat.channels.toShort(), AudioStreamWaveFormat.PCM)

        var audioStream = AudioInputStream.createPullStream(stream, audioFormat)
        audioConfig = AudioConfig.fromStreamInput(stream)
    }
    suspend fun speechToText() : String? {
        var speechRecognizer = SpeechRecognizer(speechConfig, audioConfig)

        var out = withContext(Dispatchers.IO) {
            var text = "-"
            speechRecognizer.recognizing.addEventListener { _, eventArgs ->
                var result = eventArgs.result
                log.info("RECOGNIZING Result Text: ${eventArgs.result.text} Reason: ${eventArgs.result.reason}")
                text = eventArgs.result.text
            }

            speechRecognizer.recognized.addEventListener { _, eventArgs ->
                var result = eventArgs.result
                log.info("RECOGNIZED Result Text: ${eventArgs.result.text} Reason: ${eventArgs.result.reason}")
            }

            speechRecognizer.startContinuousRecognitionAsync()
            runBlocking {
                delay(15000)
                speechRecognizer.stopContinuousRecognitionAsync().get()
            }

            return@withContext text
        }
        return out
    }
}