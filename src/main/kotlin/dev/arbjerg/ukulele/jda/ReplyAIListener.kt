
package dev.arbjerg.ukulele.jda
import kotlinx.coroutines.*
import dev.arbjerg.ukulele.features.ChrisalaxelrtoOpenAI
import kotlinx.coroutines.DelicateCoroutinesApi
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@Service
class ReplyAIListener(var chatAi : ChrisalaxelrtoOpenAI) : ListenerAdapter() {
    final val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    var lastChannel : MessageChannel? = null

    @OptIn(DelicateCoroutinesApi::class)
    private final fun scheduleTask() {
        if(lastChannel == null) {
            return
        }

        val delay = Random.nextLong(5, 15)

        scheduler.schedule({
            var msg = ""
            val job = GlobalScope.launch {
                msg = chatAi.reply()
            }

            val minTime = GlobalScope.launch {
                delay(5000)
            }

            runBlocking {
                while(!job.isCompleted || !minTime.isCompleted) {
                    delay(2000)
                    lastChannel!!.sendTyping().queue()
                }
                job.join()
                lastChannel!!.sendMessage(msg).queue()
            }
            scheduleTask() // Reschedule the task with a new random delay
        }, delay, TimeUnit.SECONDS)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot || event.member == null) {
            return
        }

        if(lastChannel == null) {
            lastChannel = event.channel
            scheduleTask()
        }else if(event.channel != lastChannel) {
            lastChannel = event.channel
        }

        chatAi.chatMessageReceived(event.message.timeCreated, event.message.contentRaw, event.member!!)
    }

}