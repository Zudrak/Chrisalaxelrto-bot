package dev.arbjerg.ukulele.jda

import DiscordAudioHandler
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.arbjerg.ukulele.audio.Player
import dev.arbjerg.ukulele.config.BotProps
import dev.arbjerg.ukulele.data.GuildProperties
import dev.arbjerg.ukulele.data.GuildPropertiesService
import dev.arbjerg.ukulele.features.AzureSpeechToText
import dev.arbjerg.ukulele.features.ChrisalaxelrtoOpenAI
import dev.arbjerg.ukulele.features.DiscordAudioStream
import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Service
class ConversationListener (final var speechToText: AzureSpeechToText, var chatAi : ChrisalaxelrtoOpenAI, val guildProperties: GuildPropertiesService, val botProps: BotProps,
                            commands: Collection<Command>,
                            val contextBeans: CommandContext.Beans) : ListenerAdapter() {
    val log: Logger = LoggerFactory.getLogger(javaClass)
    private var audioChannel : VoiceChannel? = null
    private val conversationHandler = DiscordAudioHandler()
    private final val registry: Map<String, Command>

    init {
        val map = mutableMapOf<String, Command>()
        commands.forEach { c ->
            map[c.name] = c
            c.aliases.forEach { map[it] = c }
        }
        registry = map
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
                    val stream = DiscordAudioStream(conversationHandler)
                    val text = speechToText.speechToText(stream)
                    stream.close()

                    if(text != null) {
                        chatAi.chatMessageReceived(event.message.timeCreated, text, event.member!!)
                        var aiAnswer :String? = null
                        runBlocking {
                            aiAnswer = chatAi.reply()

                            if (aiAnswer != null){
                                event.channel.sendMessage(aiAnswer!!).queue()

                                val message = MessageCreateBuilder().addContent("${botProps.prefix}speak $aiAnswer").build()
                                val guild = event.guild
                                val member = event.member!!

                                runBlocking {
                                    launch{
                                        val guildProperties = guildProperties.getAwait(guild.idLong)
                                        val channel = guild.textChannels.find { guild_channel -> "<#${guild_channel.id}>" == guildProperties.musicChannel }!!
                                        val name = "speak"
                                        val trigger = if (guildProperties.prefix == null) botProps.prefix + name else guildProperties.prefix + name
                                        val command = registry[name] ?: return@launch

                                        val actualMessage = channel.sendMessage(message).complete()

                                        val ctx = CommandContext(contextBeans, guildProperties, guild, channel, member, actualMessage, command, botProps.prefix, trigger)

                                        log.info("Invocation: ${message.content}")
                                        command.invoke0(ctx)
                                    }
                                }

                            }
                        }

                    }else{
                        log.error("No text returned from Azure")
                    }

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