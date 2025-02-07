package dev.arbjerg.ukulele.command

import dev.arbjerg.ukulele.features.ChrisalaxelrtoOpenAI
import dev.arbjerg.ukulele.features.HelpContext
import dev.arbjerg.ukulele.jda.Command
import dev.arbjerg.ukulele.jda.CommandContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component

@Component
class AITalkCommand(var chatAi: ChrisalaxelrtoOpenAI) : Command("talk") {
    override suspend fun CommandContext.invoke() {
        channel.sendTyping().queue()

        log.info(channel.toString())

        if(argumentText.trim().isEmpty()){
            return
        }

        GlobalScope.launch {
            try {
                reply(chatAi.chatMessageReceived(message.timeCreated, argumentText, invoker))
            } catch (e: Exception) {
                log.error("$e")
            }
        }
    }

    override fun HelpContext.provideHelp() {
        addUsage("<text>")
        addDescription("Requests an answer from OpenAI's GPT-4o model")    }
}
