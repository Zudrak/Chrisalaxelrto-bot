import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class MessageReceiveListener : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        System.out.printf(
            "[%s] %#s: %s\n",
            event.channel,
            event.author,
            event.message.contentDisplay
        )
    }
}