import net.dv8tion.jda.api.audio.AudioReceiveHandler
import net.dv8tion.jda.api.audio.CombinedAudio
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.LinkedBlockingQueue


@Service
class DiscordAudioHandler : AudioReceiveHandler {
    val log: Logger = LoggerFactory.getLogger(javaClass)
    private val audioQueue = LinkedBlockingQueue<ByteArray>()

    override fun canReceiveCombined(): Boolean {
        return true
    }

    override fun canReceiveUser(): Boolean {
        return false
    }

    override fun handleCombinedAudio(combinedAudio: CombinedAudio) {
        audioQueue.offer(combinedAudio.getAudioData(1.0))
    }

    fun getAudioChunk(): ByteArray {
        return audioQueue.take()
    }

    fun clear() {
        audioQueue.clear()
    }
}
