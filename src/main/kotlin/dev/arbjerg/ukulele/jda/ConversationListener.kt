package dev.arbjerg.ukulele.jda

import ConversationAudioHandler
import dev.arbjerg.ukulele.features.AzureSpeechToText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

@Service
class ConversationListener (final var speechToText: AzureSpeechToText) : ListenerAdapter() {
    private var ah = 0
    private var audioChannel : VoiceChannel? = null
    private val conversationHandler = ConversationAudioHandler()

    init {
        speechToText.setupAudioStream(conversationHandler)
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

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if(event.message.contentRaw.startsWith("voice-reply")) {
            try {
                GlobalScope.launch {
                    var text = speechToText.speechToText()
                    event.channel.sendMessage(text ?: "No text found").queue()
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