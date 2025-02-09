package dev.arbjerg.ukulele.features

import DiscordAudioHandler
import com.microsoft.cognitiveservices.speech.audio.AudioOutputStream
import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
internal class DiscordAudioStream(private val audioHandler: DiscordAudioHandler) : PullAudioInputStreamCallback() {
    val log: Logger = LoggerFactory.getLogger(javaClass)
    private val fileBuffer = ByteArrayOutputStream()

    private val targetFormat = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        16000f,
        16,
        1,
        2,
        16000f,
        false
    )
    override fun read(buffer: ByteArray): Int {

        var size = 0
        runBlocking {
            val chunk = audioHandler.getAudioChunk()
            val resampledStream = AudioSystem.getAudioInputStream(targetFormat, AudioInputStream(ByteArrayInputStream(chunk), AudioReceiveHandler.OUTPUT_FORMAT, chunk.size.toLong()))
            val resampledChunk = resampledStream.readAllBytes()

            size = resampledChunk.size
            resampledChunk.copyInto(buffer)
            fileBuffer.write(buffer)
        }
        return size
    }

    override fun close() {
        fileBuffer.reset()
    }

    fun saveToFile() {
        // Save the audio to a file
        var file = File("audio.wav")
        var pcmData = fileBuffer.toByteArray()
        AudioSystem.write(AudioInputStream(ByteArrayInputStream(pcmData), targetFormat, pcmData.size.toLong()), AudioFileFormat.Type.WAVE, file)
    }
}

