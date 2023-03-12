
package dev.arbjerg.ukulele.jda


import dev.arbjerg.ukulele.config.BotProps
import dev.arbjerg.ukulele.data.GuildPropertiesService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ReplyAIListener(final var botProps: BotProps, val guildProperties: GuildPropertiesService, commands: Collection<Command>,val contextBeans: CommandContext.Beans) : ListenerAdapter() {
    private val log: Logger = LoggerFactory.getLogger(EventHandler::class.java)
    private val registry: Map<String, Command>

    init {
        val map = mutableMapOf<String, Command>()
        commands.forEach { c ->
            map[c.name] = c
            c.aliases.forEach { map[it] = c }
        }
        registry = map
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val guild = event.guild
        val mention = Regex("^(<@!?${event.guild.selfMember.id}>\\s*)").find(event.message.contentRaw)?.value

        if (event.author.isBot || mention == null) {
            return
        }

        GlobalScope.launch {
            val guildProperties = guildProperties.getAwait(guild.idLong)
            val channel = event.channel
            val name = "talk"
            val trigger = "<@${event.guild.selfMember.id}>"
            val command = registry[name] ?: return@launch

            val ctx = event.member?.let {
                CommandContext(contextBeans, guildProperties, guild, channel,
                    it, event.message, command, botProps.prefix, trigger)
            }

            if (ctx != null) {
                command.invoke0(ctx)
            }
        }
    }

}