package dev.arbjerg.ukulele.jda

import dev.arbjerg.ukulele.data.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
class ReplyListener(val messageRepliesService: MessageRepliesService, val messageTriggersService: MessageTriggersService) : ListenerAdapter() {
    private val replies: Replies = Replies()

    private val log: Logger = LoggerFactory.getLogger(EventHandler::class.java)

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        GlobalScope.launch {
            if (!event.author.isBot) {

                val channel = event.channel
                val member = event.member
                val message = event.message

                val stickers = message.getStickers()

                //New DB method
                var msg = ""

                if (stickers.isNotEmpty()) {
                    log.info("sticker found, id: ${stickers[0].getId()}")
                    msg = "$msg ${stickers[0].getId()}"
                }

                msg = "$msg ${message.contentRaw}"

                val messageTriggers = messageTriggersService.get(event.guild.idLong, msg = msg)
                if (messageTriggers.isNotEmpty()){
                    for (trigger in messageTriggers){
                        val replies = messageRepliesService.getAwait(trigger.triggerId!!)
                        if (replies.isNotEmpty()){
                            val selectedReply = replies[Random.nextInt(replies.size)]
                            val reply = MessageBuilder().append(selectedReply.message).setTTS(true).build()
                            channel.sendMessage(reply).queue()
                        }
                    }
                }

                //Old list method
    //            if(stickers.isNotEmpty()){
    //                log.info("sticker found, id: ${stickers[0].getId()}")
    //                for(reply in replies.list) {
    //                    if (reply.first.containsMatchIn(stickers[0].getId())) {
    //                        val selectedReply = reply.second[Random.nextInt(reply.second.size)].trim()
    //                        log.info("Replying $selectedReply to sticker ${stickers[0].getId()} in ${message.contentRaw}")
    //                        var msg = MessageBuilder().append(selectedReply).setTTS(true).build()
    //                        channel.sendMessage(msg).queue()
    //                    }
    //                }
    //            }
    //
    //            for(reply in replies.list){
    //                if(reply.first.containsMatchIn(message.contentRaw.toLowerCase())){
    //                    val selectedReply = reply.second[Random.nextInt(reply.second.size)].trim()
    //                    log.info("Replying $selectedReply to ${message.contentRaw}")
    //                    var msg = MessageBuilder().append(selectedReply).setTTS(true).build()
    //                    channel.sendMessage(msg).queue()
    //                }
    //            }
            }
        }
    }

}