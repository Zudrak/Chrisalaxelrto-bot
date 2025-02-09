package dev.arbjerg.ukulele.jda

import ConversationAudioHandler
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
class ConversationListener () : ListenerAdapter() {

    private var audioChannel : VoiceChannel? = null
    private val conversationHandler = ConversationAudioHandler()
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
        if(event.message.contentRaw.startsWith("record")) {
            // After receiving some audio data in the handler...
            val pcmData = conversationHandler.getLast10SecondsOfAudio()
            val outputPath = "output.wav"

            try {
                val file = File(outputPath)
                AudioSystem.write(AudioInputStream(ByteArrayInputStream(pcmData), AudioReceiveHandler.OUTPUT_FORMAT, pcmData.size.toLong()), AudioFileFormat.Type.WAVE, file)
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