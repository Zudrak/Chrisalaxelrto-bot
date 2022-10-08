package dev.arbjerg.ukulele.jda

import dev.arbjerg.ukulele.data.Replies
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.random.Random


@Service
class ReplyListener() : ListenerAdapter() {
    private val replies: Replies = Replies()

    private val log: Logger = LoggerFactory.getLogger(EventHandler::class.java)

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (!event.author.isBot) {
            val channel = event.channel
            val member = event.member
            val message = event.message

            val stickers = message.getStickers()

            if(stickers.isNotEmpty()){
                log.info("sticker found, id: ${stickers[0].getId()}")
                for(reply in replies.list) {
                    if (reply.first.containsMatchIn(stickers[0].getId())) {
                        val selectedReply = reply.second[Random.nextInt(reply.second.size)].trim()
                        log.info("Replying $selectedReply to sticker ${stickers[0].getId()} in ${message.contentRaw}")
                        var msg = MessageBuilder().append(selectedReply).setTTS(true).build()
                        channel.sendMessage(msg).queue()
                    }
                }
            }

            for(reply in replies.list){
                if(reply.first.containsMatchIn(message.contentRaw.toLowerCase())){
                    val selectedReply = reply.second[Random.nextInt(reply.second.size)].trim()
                    log.info("Replying $selectedReply to ${message.contentRaw}")
                    var msg = MessageBuilder().append(selectedReply).setTTS(true).build()
                    channel.sendMessage(msg).queue()
                }
            }
    }
    }

}