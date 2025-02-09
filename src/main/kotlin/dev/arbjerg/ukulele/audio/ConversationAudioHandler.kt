import net.dv8tion.jda.api.audio.AudioReceiveHandler
import net.dv8tion.jda.api.audio.CombinedAudio
import net.dv8tion.jda.api.audio.UserAudio
import java.nio.ByteBuffer
import java.util.*

class ConversationAudioHandler : AudioReceiveHandler {
    private val SAMPLE_RATE = 48000 // 48 kHz
    private val CHANNELS = 2 // Stereo
    private val RECORDING_SECONDS = 20
    private val BUFFER_SIZE = SAMPLE_RATE * CHANNELS * RECORDING_SECONDS // Buffer for 10 seconds

    private val audioBuffer: Queue<ByteArray> = LinkedList()

    override fun canReceiveCombined(): Boolean {
        return true // We want to receive combined audio (all users together)
    }

    override fun handleCombinedAudio(combinedAudio: CombinedAudio) {
        val data = combinedAudio.getAudioData(1.0) // Get the combined audio data
        val secondsToSamples = (RECORDING_SECONDS * 1000) / 20

        // Add data to the buffer
        if (audioBuffer.size >= secondsToSamples) {
            audioBuffer.poll() // Remove the oldest data when buffer is full
        }
        audioBuffer.add(data)
    }

    // Function to get the last 10 seconds of audio
    fun getLast10SecondsOfAudio(): ByteArray {
        val buffer = ByteBuffer.allocate(BUFFER_SIZE * 2)
        val newAudioBuffer = LinkedList(audioBuffer)
        for (chunk in newAudioBuffer) {
            buffer.put(chunk)
        }
        return buffer.array()
    }

    override fun canReceiveUser(): Boolean {
        return false // We don't need individual user audio for this case
    }

    override fun handleUserAudio(userAudio: UserAudio) {
        // Not handling individual user audio in this case
    }
}
