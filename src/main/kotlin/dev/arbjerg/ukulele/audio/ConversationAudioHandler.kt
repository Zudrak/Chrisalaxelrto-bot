import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStream
import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.audio.CombinedAudio
import net.dv8tion.jda.api.audio.UserAudio
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*


@Service
class ConversationAudioHandler : AudioReceiveHandler, PullAudioInputStreamCallback() {
    private val SAMPLE_RATE = 48000 // 48 kHz
    private val CHANNELS = 2 // Stereo
    private val RECORDING_SECONDS = 15
    private val BUFFER_SIZE = SAMPLE_RATE * CHANNELS * RECORDING_SECONDS // Buffer for 10 seconds

    private val audioStream = ShiftAddByteBuffer(BUFFER_SIZE)
    private var lastPCMData : ByteArray? = null
    private val inputStream: InputStream = ByteArrayInputStream(audioStream.buffer)
    override fun canReceiveCombined(): Boolean {
        return true // We want to receive combined audio (all users together)
    }

    override fun handleCombinedAudio(combinedAudio: CombinedAudio) {
        val audioData = combinedAudio.getAudioData(1.0) // Get audio data with volume adjustment
        lastPCMData = audioData.clone()

        synchronized(audioStream) {
            audioStream.write(audioData)
        }
    }

    override fun canReceiveUser(): Boolean {
        return false // We don't need individual user audio for this case
    }

    override fun handleUserAudio(userAudio: UserAudio) {
        // Not handling individual user audio in this case
    }

    override fun read(dataBuffer: ByteArray?): Int {
        if (dataBuffer == null) {
            return 0
        }
        synchronized(audioStream) {
            return inputStream.read(dataBuffer, 0, dataBuffer.size)
        }
    }

    override fun close() {
        synchronized(audioStream) {
            inputStream.close()
        }
    }

    class ShiftAddByteBuffer(private val capacity: Int) {
        val buffer = ByteArray(capacity)
        private var size = 0

        fun write(data: ByteArray) {
            val dataLength = data.size
            if (dataLength >= capacity) {
                // If the new data is larger than the buffer, only keep the last part
                System.arraycopy(data, dataLength - capacity, buffer, 0, capacity)
                size = capacity
            } else {
                // Shift existing data to the left
                val shiftLength = minOf(size + dataLength - capacity, size)
                if (shiftLength > 0) {
                    System.arraycopy(buffer, shiftLength, buffer, 0, size - shiftLength)
                }
                // Add new data at the end
                System.arraycopy(data, 0, buffer, size - shiftLength, dataLength)
                size = minOf(size + dataLength, capacity)
            }
        }

        fun read(dataBuffer: ByteArray): Int {
            val bytesToRead = minOf(dataBuffer.size, size)
            System.arraycopy(buffer, 0, dataBuffer, 0, bytesToRead)
            return bytesToRead
        }

        fun reset() {
            size = 0
        }
    }
}
