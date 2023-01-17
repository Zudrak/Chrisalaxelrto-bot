package dev.arbjerg.ukulele.command

import dev.arbjerg.ukulele.data.GuildPropertiesService
import dev.arbjerg.ukulele.data.MessageRepliesService
import dev.arbjerg.ukulele.data.MessageReply
import dev.arbjerg.ukulele.data.MessageTriggersService
import dev.arbjerg.ukulele.features.HelpContext
import dev.arbjerg.ukulele.jda.Command
import dev.arbjerg.ukulele.jda.CommandContext
import org.springframework.stereotype.Component

@Component
class TestReplyCommand(val messageRepliesService : MessageTriggersService) : Command("test") {
    override suspend fun CommandContext.invoke() {
        val hello = messageRepliesService.getAllForGuild(guild.idLong)
        val hai = hello.block()
        reply(hai.toString())
    }

    override fun HelpContext.provideHelp() {
        addUsage("<text>")
        addDescription("asdgdffghfjgdj the given text")
    }
}
