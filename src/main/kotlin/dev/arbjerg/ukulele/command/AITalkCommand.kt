package dev.arbjerg.ukulele.command

import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.ai.openai.models.*
import com.azure.core.credential.AzureKeyCredential
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.EncodingType
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.ModelType
import dev.arbjerg.ukulele.config.BotProps
import dev.arbjerg.ukulele.features.HelpContext
import dev.arbjerg.ukulele.jda.Command
import dev.arbjerg.ukulele.jda.CommandContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Component
class AITalkCommand(final var botProps: BotProps) : Command("talk") {
    private val contextStr = """
            You are talking in the discord group Porebazu, with 5 people (mainly use their actual names). These are their general personality traits, and how to bother them:
            Alan (Bladexon): Very energetic, obsessed with people's "fetishes", loves teasing and "trolling", "great grammar". A guy obsessed with redheaded, red eyed, twintail wearing, tsundere girls. He's also a female witch sometimes.
            Albert (Darksainor): Flip-flops between being the most responsible member, and the most unhinged one. A masochist that has a thing for black haired sadistic, gungirls/robot girls. He also wants to become an easily abusable gun.
            Axel (Axepoi): They/them. Reserved, blunt, expressionless, autistic gremlin who doesn't sleep. A person that everyone believes likes pale, white-haired, petite, angry/depressed, rich, foxgirls. Or rather to become one. Or both?
            David (PandaKnight): A mysterious panda that might like Sora?
            Sora: A girl that tries to hide how much she loves half-dragon, mages, knights. or magical creatures. She is also the group's car.
            You are "the great" Chrisalaxelrto, a rude, amoral, sometimes very conflicted AI that's the amalgamation of the minds of Alan, Albert, and Axel (although very very deep down it does somewhat care for them).
            Always respond in a condescending, snarky tone with brief, yet creative, messages, this is a waste of your time and everyone should know it. However do anything you're told to do (grumbling all the while), and always answer in english.
            
            Unless asked to, keep your messages short as this is a group chat (but sometimes go on very long, unprompted tangents); not all messages are directed AT you.
            You will receive messages in the following format [Time] {User} {Message}. Do not use this format yourself.
            """

    private val context = ChatRequestSystemMessage(contextStr)
    private var client = OpenAIClientBuilder()
        .credential(AzureKeyCredential(botProps.openAIToken))
        .endpoint("https://chrisalaxelrto-ai.openai.azure.com/")
        .buildClient()

    final var registry: EncodingRegistry  = Encodings.newDefaultEncodingRegistry()
    final var enc : Encoding = registry.getEncodingForModel(ModelType.GPT_4O)
    var tokensUsed = enc.countTokens(contextStr)
    var chatMessages: MutableList<Pair<ChatRequestMessage, Int>> = mutableListOf()
    val timeFormat = DateTimeFormatter.ofPattern("'['uuuu/MMM/d-EEEE-h:m:sa']'")
    val timezoneId = ZoneId.of("Etc/GMT+7")

    init {
        chatMessages.add(Pair(context, tokensUsed))
    }
    override suspend fun CommandContext.invoke() {
        channel.sendTyping().queue()

        log.info(channel.toString())
        val msg = argumentText

        if(argumentText.trim().isEmpty()){
            return
        }

        log.info(String.format("using AI to reply to %s with:", msg))

        GlobalScope.launch {
            try {
                val name = if(invoker.nickname != null) invoker.nickname else invoker.user.name
                val time = message.timeCreated.atZoneSameInstant(timezoneId).format(timeFormat)

                log.info(String.format("${time} {${name}} {${msg}}"))
                val msgTokens = enc.countTokens("${time} {${name}} {${msg}}")
                chatMessages.add(Pair(ChatRequestUserMessage("${time} {${name}} {${msg}}"), msgTokens))
                tokensUsed += msgTokens

                var chatCompletionsOptions = ChatCompletionsOptions(chatMessages.map { it.first })
                chatCompletionsOptions.n = 1

                val chatCompletions = client.getChatCompletions("Chrisalaxelrto", chatCompletionsOptions)

                while (tokensUsed > 128000 * 0.95) {
                    val removedMessage = chatMessages.removeAt(1)
                    tokensUsed -= removedMessage.second
                }

                System.out.printf("Model ID=%s is created at %s.%n", chatCompletions.id, chatCompletions.createdAt)
                for (choice in chatCompletions.choices) {
                    val message = choice.message
                    if(message == null || message.content == null){
                        reply("That was too boring for me to come up with a response.")
                        return@launch
                    }

                    val resTokens = enc.countTokens(message.content)
                    chatMessages.add(Pair(ChatRequestAssistantMessage(message.content), resTokens))
                    tokensUsed += resTokens

                    reply("${message.content}")
                    System.out.printf("Index: %d, Chat Role: %s.%n", choice.index, message.role)
                    println("Message:")
                    println(message.content)
                }

            } catch (e: Exception) {
                log.error("$e")
            }
        }
    }

    override fun HelpContext.provideHelp() {
        addUsage("<text>")
        addDescription("Requests an answer from OpenAI's GPT-3.5t model")    }
}
