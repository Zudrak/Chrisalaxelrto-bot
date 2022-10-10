package dev.arbjerg.ukulele.command

import dev.arbjerg.ukulele.config.BotProps
import dev.arbjerg.ukulele.features.HelpContext
import dev.arbjerg.ukulele.jda.Command
import dev.arbjerg.ukulele.jda.CommandContext
import net.dv8tion.jda.api.entities.TextChannel
import org.springframework.stereotype.Component

@Component
class PauseCommand() : Command ("pause") {
    override suspend fun CommandContext.invoke() {

        if ("<#${channel.id}>" != guildProperties.musicChannel) return replyWrongMusicChannel()

        if (player.isPaused) return reply("Player already paused. Use `resume` to continue playback.")

        player.pause()
        reply("Playback has been paused.")
    }

    override fun HelpContext.provideHelp() {
        addUsage("")
        addDescription("Pauses the playback.")
    }
}