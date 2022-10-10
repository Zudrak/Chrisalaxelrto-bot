package dev.arbjerg.ukulele.command

import dev.arbjerg.ukulele.config.BotProps
import dev.arbjerg.ukulele.data.GuildPropertiesService
import dev.arbjerg.ukulele.features.HelpContext
import dev.arbjerg.ukulele.jda.Command
import dev.arbjerg.ukulele.jda.CommandContext
import org.springframework.stereotype.Component

@Component
class ChannelCommand(val guildPropertiesService: GuildPropertiesService, val botProps: BotProps) : Command("channel") {
    override suspend fun CommandContext.invoke() = when {
        argumentText == "reset" -> {
            guildPropertiesService.transformAwait(guild.idLong) { it.musicChannel = null }
            reply("Removed music channel from configuration")
        }
        argumentText.isNotBlank() -> {
            val props = guildPropertiesService.transformAwait(guild.idLong) { it.musicChannel = argumentText }
            reply("Set music channel to ${props.musicChannel}")
        }
        else -> {
            replyHelp()
        }
    }

    override fun HelpContext.provideHelp() {
        addUsage("<channel>")
        addDescription("Set music channel to <channel>")
        addUsage("reset")
        addDescription("Removes the bot being limited to a channel for music")
    }
}