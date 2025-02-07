
package dev.arbjerg.ukulele.jda

import kotlinx.coroutines.DelicateCoroutinesApi
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Service

@Service
class ReplyAIListener() : ListenerAdapter() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) {
            return
        }

        event.channel.sendMessage("Hello!").queue()

    }

}