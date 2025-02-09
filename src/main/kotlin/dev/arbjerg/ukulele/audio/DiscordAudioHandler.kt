import net.dv8tion.jda.api.audio.AudioReceiveHandler
import net.dv8tion.jda.api.audio.CombinedAudio
import net.dv8tion.jda.api.audio.UserAudio
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.LinkedBlockingQueue

@Service
class DiscordAudioHandler : AudioReceiveHandler {
    val log: Logger = LoggerFactory.getLogger(javaClass)
    private val buffer = LinkedBlockingQueue<ByteArray>()

    override fun canReceiveCombined(): Boolean {
        return true
    }

    override fun canReceiveUser(): Boolean {
        return false
    }

    override fun handleCombinedAudio(combinedAudio: CombinedAudio) {
        buffer.offer(combinedAudio.getAudioData(1.0)) // Capture audio data
    }

    fun getAudioChunk(): ByteArray? = buffer.poll()

    fun clear() {
        buffer.clear()
    }
    override fun handleUserAudio(userAudio: UserAudio) {
        // Not handling individual user audio in this case
    }
}
