
package dev.arbjerg.ukulele.jda

import com.aallam.openai.api.ExperimentalOpenAI
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.completion.TextCompletion
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.seconds

@Service
class ReplyAIListener() : ListenerAdapter() {
    @OptIn(ExperimentalOpenAI::class)
    final val openAIConfig = OpenAIConfig(token = "", timeout = Timeout(socket = 320.seconds))
    val openAI = OpenAI(openAIConfig)
    private val log: Logger = LoggerFactory.getLogger(EventHandler::class.java)

    @OptIn(DelicateCoroutinesApi::class)
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) {
            return
        }
            GlobalScope.launch {
            val channel = event.channel
            val message = event.message


            val completionRequest = CompletionRequest(
                model = ModelId("text-davinci-003"),
                prompt = message.contentRaw,
                echo = false,
                maxTokens = 4000  - message.contentRaw.length,
                temperature = 1.1,
                presencePenalty = 1.0
            )
            val completion: TextCompletion = openAI.completion(completionRequest)
            println("-----------------------------------------")
            println(completion.choices.first().text)
            val res = completion.choices.first().text
            res.chunked(1000).forEach {
                channel.sendMessage(it).queue()
            }
        }
    }

}