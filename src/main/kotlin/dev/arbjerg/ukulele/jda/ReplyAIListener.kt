
package dev.arbjerg.ukulele.jda
import dev.arbjerg.ukulele.audio.PlayerRegistry
import dev.arbjerg.ukulele.config.BotProps
import dev.arbjerg.ukulele.data.GuildProperties
import dev.arbjerg.ukulele.data.GuildPropertiesService
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
import java.util.concurrent.ScheduledFuture
import kotlin.random.Random
import dev.arbjerg.ukulele.features.ChrisalaxelrtoOpenAI.Mood
import org.springframework.stereotype.Component

@Service
class ReplyAIListener(var chatAi : ChrisalaxelrtoOpenAI, val guildProperties: GuildPropertiesService, val botProps: BotProps) : ListenerAdapter() {
    final val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    var lastChannel : MessageChannel? = null
    var lastDelay: Long = 5L
    private var scheduledFuture: ScheduledFuture<*>? = null

    val usernameList: List<Pair<Regex, String>> = listOf(
        Regex("<@889612265920266251>", RegexOption.IGNORE_CASE) to "@Chrisalaxelrto",
        Regex("<@168194489343672322>", RegexOption.IGNORE_CASE) to "@Axel",
        Regex("<@308674959557918732>", RegexOption.IGNORE_CASE) to "@Darksainor",
        Regex("<@190624436913831938>", RegexOption.IGNORE_CASE) to "@Bladexon",
        Regex("<@298192097184317440>", RegexOption.IGNORE_CASE) to "@PandaKnight",
        Regex("<@686807129470009370>", RegexOption.IGNORE_CASE) to "@Sora"
    )

    val nicknameList: List<String> = listOf(
        "Chrisalaxelrto",
        "Crix",
        "CXL"
    )

    @OptIn(DelicateCoroutinesApi::class)
    private val taskRunnable = Runnable{
        if(lastChannel == null) return@Runnable

        var msg : String? = ""
        val job = GlobalScope.launch {
            msg = chatAi.reply()
        }

        runBlocking {
            while(!job.isCompleted) {
                delay(2000)
            }
            job.join()
            if(!msg.isNullOrBlank()){
                lastChannel!!.sendTyping().queue()
                delay(5000)
                lastChannel!!.sendMessage(msg!!).queue()
            }
            lastDelay = when (chatAi.getMood()){
                Mood.Answering -> Random.nextLong(5, 10)
                Mood.Chatty -> Random.nextLong(8, 12)
                Mood.Waiting -> Random.nextLong(20, 35)
                Mood.Bored -> Random.nextLong(40, 55)
                Mood.Busy -> Random.nextLong(1200, 1800)
            }
        }
        scheduleTask() // Reschedule the task with a new random delay

    }

    private final fun scheduleTask() {
        if(lastChannel == null) return
        scheduledFuture = scheduler.schedule(taskRunnable, lastDelay, TimeUnit.SECONDS)
    }

    fun triggerTaskNow(){
        scheduledFuture?.cancel(false)
        taskRunnable.run()
    }

    private fun replaceAts(input: String): String{
        var msg = input
        for ((regex, replacement) in usernameList) {
            if (regex.containsMatchIn(msg)) {
                msg = regex.replace(msg, replacement)
            }
        }
        return msg
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot || event.member == null) {
            return
        }
        if(event.guild.id == botProps.guildId) {
            runBlocking {
                val guild = guildProperties.getAwait(event.guild.idLong)

                if (lastChannel == null && guild.textChannel != null && "<#${event.channel.id}>" == guild.textChannel) {
                    lastChannel = event.channel
                    scheduleTask()
                }
                if (guild.textChannel == null) {
                    if (lastChannel == null) {
                        lastChannel = event.channel
                        scheduleTask()
                    } else if (event.channel != lastChannel) {
                        lastChannel = event.channel
                    }
                }

                if (guild.textChannel == null || "<#${event.channel.id}>" == guild.textChannel) {
                    var msg = replaceAts(event.message.contentRaw)
                    chatAi.chatMessageReceived(event.message.timeCreated, msg, event.member!!)
                    if(nicknameList.any {msg.contains(it, ignoreCase = true)}) triggerTaskNow()
                }
            }
        }
    }

}