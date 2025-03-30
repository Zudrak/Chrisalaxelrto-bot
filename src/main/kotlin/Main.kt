import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.*


fun main(args: Array<String>) {
    JDABuilder.createLight("", EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
        .addEventListeners(MessageReceiveListener())
        .build()
}