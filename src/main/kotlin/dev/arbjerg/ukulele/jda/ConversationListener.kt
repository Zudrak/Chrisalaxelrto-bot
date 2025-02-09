package dev.arbjerg.ukulele.jda

import DiscordAudioHandler
import dev.arbjerg.ukulele.features.AzureSpeechToText
import dev.arbjerg.ukulele.features.DiscordAudioStream
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ConversationListener (final var speechToText: AzureSpeechToText) : ListenerAdapter() {
    val log: Logger = LoggerFactory.getLogger(javaClass)
    private var audioChannel : VoiceChannel? = null
    private val conversationHandler = DiscordAudioHandler()

    init {
    }
    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        if(event.member.user.id != event.jda.selfUser.id) {
            return;
        }

        if(event.channelJoined != null) {
            handleVoiceJoin(event.channelJoined!!.asVoiceChannel())
        }else if(event.channelLeft != null) {
            handleVoiceLeft()
        }


    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if(event.message.contentRaw.startsWith("voice-reply")) {
            try {
                event.guild.audioManager.openAudioConnection(event.member?.voiceState?.channel)

                GlobalScope.launch {
                    conversationHandler.clear()
                    val stream = DiscordAudioStream(conversationHandler)
                    val text = speechToText.speechToText(stream)
                    event.channel.sendMessage(text ?: "No text found").queue()
                    stream.saveToFile()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun handleVoiceJoin( voiceChannel : VoiceChannel) {
        this.audioChannel = voiceChannel
        val guild = audioChannel!!.guild
        guild.audioManager.receivingHandler = conversationHandler
    }

    fun  handleVoiceLeft() {
        if(audioChannel == null){
            return
        }

        var guild = audioChannel!!.guild
        guild.audioManager.receivingHandler = null
    }
}