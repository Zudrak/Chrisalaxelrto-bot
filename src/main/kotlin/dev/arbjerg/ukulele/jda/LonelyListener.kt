package dev.arbjerg.ukulele.jda

import dev.arbjerg.ukulele.audio.Player
import dev.arbjerg.ukulele.data.GuildPropertiesService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LonelyListener(
    val guildProperties: GuildPropertiesService,
    val contextBeans: CommandContext.Beans
    )
    : ListenerAdapter() {

    private val log: Logger = LoggerFactory.getLogger(EventHandler::class.java)

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent){
        if (!event.member.user.isBot && event.channelLeft != null) {
            isAlone(event.channelLeft!!.asVoiceChannel(), event.guild)
        }
    }


    fun isAlone(channel: VoiceChannel, guild: Guild){
        val users = channel.members.filter{ member -> !member.user.isBot() }
        if ( users.isEmpty() && guild.audioManager.connectedChannel == channel ){
            runBlocking {
                launch{
                    val guildProperties = guildProperties.getAwait(guild.idLong)
                    val player: Player by lazy { contextBeans.players.get(guild, guildProperties) }
                    guild.audioManager.closeAudioConnection()
                    player.stop()
                }
            }
        }
    }

}