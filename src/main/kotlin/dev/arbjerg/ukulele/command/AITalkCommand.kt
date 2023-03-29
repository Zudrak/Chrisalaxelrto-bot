package dev.arbjerg.ukulele.command

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import dev.arbjerg.ukulele.config.BotProps
import dev.arbjerg.ukulele.features.HelpContext
import dev.arbjerg.ukulele.jda.Command
import dev.arbjerg.ukulele.jda.CommandContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.seconds
@Component
class AITalkCommand(final var botProps: BotProps) : Command("talk") {
    private final val MESSAGE_LIMIT = 40

    @OptIn(BetaOpenAI::class)
    private val messages  = mutableListOf<ChatMessage>()
    private final val openAIConfig = OpenAIConfig(token = botProps.openAIToken, timeout = Timeout(socket = 320.seconds))
    val openAI = OpenAI(openAIConfig)

    @OptIn(DelicateCoroutinesApi::class, BetaOpenAI::class)
    override suspend fun CommandContext.invoke() {
        val message = argumentText

        if(argumentText.trim().isEmpty()){
            return
        }

        GlobalScope.launch {
            messages.add(ChatMessage(ChatRole.User, message))
            if(messages.size > MESSAGE_LIMIT){
                messages.removeAt(0);
            }

            val completionRequest = ChatCompletionRequest(
                model = ModelId("gpt-4"),
                messages = messages
            )
            val completion: ChatCompletion = openAI.chatCompletion(completionRequest)
            val res = completion.choices.first().message?.content
            res?.let { ChatMessage(ChatRole.Assistant, it) }?.let { messages.add(it) }
            if(messages.size > MESSAGE_LIMIT){
                messages.removeAt(0);
            }

            res?.chunked(1000)?.forEach {
                reply(it)
            }
        }
    }

    override fun HelpContext.provideHelp() {
        addUsage("<text>")
        addDescription("Requests an answer from OpenAI's GPT-4 model")    }
}