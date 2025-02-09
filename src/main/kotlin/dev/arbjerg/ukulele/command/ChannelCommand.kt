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
        argumentText.isBlank() -> replyHelp()
        else -> {
            val args = argumentText.split(" ").filter { it.isNotBlank() }

            if (args.size != 2) {
                replyHelp()
            } else {
                val command = args[0].lowercase()
                val secondArg = args[1].lowercase()

                if (command == "reset") {
                    // "reset <channelType>" resets the channel
                    when (secondArg) {
                        "music" -> {
                            guildPropertiesService.transformAwait(guild.idLong) { it.musicChannel = null }
                            reply("Removed music channel from configuration")
                        }

                        "text" -> {
                            guildPropertiesService.transformAwait(guild.idLong) { it.textChannel = null }
                            reply("Removed text channel from configuration")
                        }

                        else -> replyHelp()
                    }
                } else {
                    // Otherwise, we expect the command to be a channel type ("music" or "text")
                    // and the second token to be the channel id.
                    when (command) {
                        "music" -> {
                            val props =
                                guildPropertiesService.transformAwait(guild.idLong) { it.musicChannel = args[1] }
                            reply("Set music channel to ${props.musicChannel}")
                        }

                        "text" -> {
                            val props = guildPropertiesService.transformAwait(guild.idLong) { it.textChannel = args[1] }
                            reply("Set text channel to ${props.textChannel}")
                        }

                        else -> replyHelp()
                    }
                }

            }
        }
    }

    override fun HelpContext.provideHelp() {
        addUsage("<music/text> <channel>")
        addDescription("Set music/text channel to <channel>")
        addUsage("reset <music/text>")
        addDescription("Removes the bot being limited to a channel for music/text")
    }
}