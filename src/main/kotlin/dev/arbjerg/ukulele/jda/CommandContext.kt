package dev.arbjerg.ukulele.jda

import dev.arbjerg.ukulele.audio.Player
import dev.arbjerg.ukulele.audio.PlayerRegistry
import dev.arbjerg.ukulele.config.BotProps
import dev.arbjerg.ukulele.data.GuildProperties
import dev.arbjerg.ukulele.features.HelpContext
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.springframework.stereotype.Component

class CommandContext(
    val beans: Beans,
    val guildProperties: GuildProperties,
    val guild: Guild,
    val channel: TextChannel,
    val invoker: Member,
    val message: Message,
    val command: Command,
    val prefix: String,
    /** Prefix + command name */
        val trigger: String
) {
    @Component
    class Beans(
            val players: PlayerRegistry,
            val botProps: BotProps
    ) {
        lateinit var commandManager: CommandManager
    }

    enum class ChannelType {
        Music,
        Text
    }

    val player: Player by lazy { beans.players.get(guild, guildProperties) }

    /** The command argument text after the trigger */
    val argumentText: String by lazy {
        message.contentRaw.drop(trigger.length).trim()
    }
    val selfMember: Member get() = guild.selfMember

    fun reply(msg: String) {
        channel.sendMessage(msg).queue()
    }

    fun replyTo(msg: String){
        val messageBuilder = MessageCreateBuilder().addContent(msg)
        message.reply(messageBuilder.build()).queue()
    }

    fun replyTTS(msg: String){
        val messageBuilder = MessageCreateBuilder().addContent(msg).setTTS(true)
        replyMsg(messageBuilder.build())
    }

    fun replyMsg(msg: MessageCreateData) {
        channel.sendMessage(msg).queue()
    }

    fun replyEmbed(embed: MessageEmbed) {
        channel.sendMessageEmbeds(embed).queue()
    }

    fun replyHelp(forCommand: Command = command) {
        val help = HelpContext(this, forCommand)
        forCommand.provideHelp0(help)
        channel.sendMessage(help.buildMessage()).queue()
    }

    fun checkChannel(channelType: ChannelType, channel: String): Boolean {
        when(channelType){
            ChannelType.Music -> {
                if("<#${channel}>" != guildProperties.musicChannel && guildProperties.musicChannel != null){
                    replyTTS("You fucking idiot, go to the correct channel next time.")
                    return false
                }
            }
            ChannelType.Text -> {
                if("<#${channel}>" != guildProperties.textChannel && guildProperties.textChannel != null){
                    return false
                }
            }
        }
        return true
    }

    fun handleException(t: Throwable) {
        command.log.error("Handled exception occurred", t)
        reply("An exception occurred!\n`${t.message}`")
    }
}