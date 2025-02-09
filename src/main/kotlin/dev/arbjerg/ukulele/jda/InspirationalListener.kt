package dev.arbjerg.ukulele.jda

import dev.arbjerg.ukulele.config.BotProps
import dev.arbjerg.ukulele.data.GuildPropertiesService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
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
    private final val registry: Map<String, Command>
    private var taskChannel: VoiceChannel? = null

    init {
        val map = mutableMapOf<String, Command>()
        commands.forEach { c ->
            map[c.name] = c
            c.aliases.forEach { map[it] = c }
        }
        registry = map
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        if (event.channelJoined != null){
            onGuildVoiceJoin(event)
            return
        }

        if (event.channelLeft != null) {
            onGuildVoiceLeave(event)
        }
    }

    fun onGuildVoiceJoin(event: GuildVoiceUpdateEvent) {
        if (!event.member.user.isBot) {
            task?.cancel()
            if(event.channelJoined != taskChannel) task = null
            if (task == null) buildTask(event.channelJoined!!.asVoiceChannel(), event.guild, event.member)
        }
    }

    fun onGuildVoiceLeave(event: GuildVoiceUpdateEvent){
        if (!event.member.user.isBot && event.channelLeft != null) {
            val channel = event.channelLeft!!
            if (taskChannel == channel){
                val users = channel.members.filter{ member -> !member.user.isBot }
                if ( users.isEmpty() ){
                    log.info("Stopped scheduler for inspiration")
                    task?.cancel()
                    task = null

                    var maxMembers = 0
                    var maxChannel: VoiceChannel? = null
                    for (voiceChannel in event.guild.voiceChannels){
                        val members = voiceChannel.members.filter{ member -> !member.user.isBot }
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
        task = timer.scheduleAtFixedRate(0, 120000) {
            val rng = (1..100).random()
            if (rng <= 15) {
                val message = MessageCreateBuilder().addContent("${botProps.prefix}inspire").build()
                runBlocking {
                    launch{
                        val guildProperties = guildProperties.getAwait(guild.idLong)
                        val channel = guild.textChannels.find { guild_channel -> "<#${guild_channel.id}>" == guildProperties.musicChannel }!!
                        val name = "inspire"
                        val trigger = if (guildProperties.prefix == null) botProps.prefix + name else guildProperties.prefix + name
                        val command = registry[name] ?: return@launch

                        val actualMessage = channel.sendMessage(message).complete()

                        val ctx = CommandContext(contextBeans, guildProperties, guild, channel, member, actualMessage, command, botProps.prefix, trigger)

                        log.info("Invocation: ${message.content}")
                        command.invoke0(ctx)
                        delay(120000)
                    }
                }
            }
        }
    }

}