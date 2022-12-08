package dev.arbjerg.ukulele.jda

import dev.arbjerg.ukulele.audio.Player
import dev.arbjerg.ukulele.audio.PlayerRegistry
import dev.arbjerg.ukulele.config.BotProps
import dev.arbjerg.ukulele.data.GuildPropertiesService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate


@Service
class InspirationalListener(
    val botProps: BotProps,
    val guildProperties: GuildPropertiesService,
    val contextBeans: CommandContext.Beans,
    commands: Collection<Command>
    )
    : ListenerAdapter() {

    private val log: Logger = LoggerFactory.getLogger(EventHandler::class.java)
    private val timer: Timer = Timer()
    var task: TimerTask? = null
    private val registry: Map<String, Command>

    init {
        val map = mutableMapOf<String, Command>()
        commands.forEach { c ->
            map[c.name] = c
            c.aliases.forEach { map[it] = c }
        }
        registry = map
    }

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (!event.member.user.isBot) {
            if (task == null) {
                log.info("Started scheduler for inspiration")
                task = timer.scheduleAtFixedRate(0, 60000) {
                    val rng = (1..100).random()
                    if (rng <= 5) {
                        val message = MessageBuilder().append("${botProps.prefix}inspire").build()
                        runBlocking {
                            launch{
                                val voiceChannel = event.channelJoined
                                val guild = event.guild
                                val guildProperties = guildProperties.getAwait(guild.idLong)
                                val channel = guild.textChannels.find { guild_channel -> "<#${guild_channel.id}>" == guildProperties.musicChannel }!!
                                val member = event.member
                                val name = "inspire"
                                val trigger = if (guildProperties.prefix == null) botProps.prefix + name else guildProperties.prefix + name

                                val command = registry[name] ?: return@launch
                                val ctx = CommandContext(contextBeans, guildProperties, guild, channel, member, message, command, botProps.prefix, trigger)

                                log.info("Invocation: ${message.contentRaw}")
                                command.invoke0(ctx)
                                delay(120000)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent){
        if (!event.member.user.isBot) {
            isAlone(event.channelLeft, event.guild)
        }
    }

    fun isAlone(channel: VoiceChannel, guild: Guild){
        val users = channel.members.filter{ member -> !member.user.isBot() }
        if ( users.isEmpty() && guild.audioManager.connectedChannel == channel ){
            log.info("Stopped scheduler for inspiration")
            task?.cancel()
            task = null
        }
    }

}