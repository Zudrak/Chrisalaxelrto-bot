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
import net.dv8tion.jda.api.entities.Member
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
    private var task: TimerTask? = null
    private val registry: Map<String, Command>
    private var taskChannel: VoiceChannel? = null

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
            if(event.channelJoined != taskChannel) task = null
            if (task == null) buildTask(event.channelJoined, event.guild, event.member)
        }
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if(!event.member.user.isBot){
            if(event.channelJoined != taskChannel) task = null
            if(task == null) buildTask(event.channelJoined, event.guild, event.member)
        }
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent){
        if (!event.member.user.isBot) {
            val channel = event.channelLeft
            if (taskChannel == channel){
                val users = channel.members.filter{ member -> !member.user.isBot() }
                if ( users.isEmpty() ){
                    log.info("Stopped scheduler for inspiration")
                    task?.cancel()
                    task = null

                    var maxMembers = 0
                    var maxChannel: VoiceChannel? = null
                    for (voiceChannel in event.guild.voiceChannels){
                        val members = voiceChannel.members.filter{ member -> !member.user.isBot() }
                        if (members.isNotEmpty() && members.size > maxMembers){
                            maxMembers = members.size
                            maxChannel = voiceChannel
                        }
                    }
                    if(maxMembers != 0) buildTask(maxChannel!!, event.guild, maxChannel.members[0])
                }
            }
        }
    }

    fun buildTask(channelJoined: VoiceChannel, guild: Guild, member: Member){
        log.info("Started scheduler for inspiration")
        taskChannel = channelJoined
        task?.cancel()
        task = timer.scheduleAtFixedRate(0, 120000) {
            val rng = (1..100).random()
            if (rng <= 1) {
                val message = MessageBuilder().append("${botProps.prefix}inspire").build()
                runBlocking {
                    launch{
                        val guildProperties = guildProperties.getAwait(guild.idLong)
                        val channel = guild.textChannels.find { guild_channel -> "<#${guild_channel.id}>" == guildProperties.musicChannel }!!
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