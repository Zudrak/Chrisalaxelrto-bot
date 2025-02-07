package dev.arbjerg.ukulele.features

import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.ai.openai.models.ChatCompletionsOptions
import com.azure.ai.openai.models.ChatRequestAssistantMessage
import com.azure.ai.openai.models.ChatRequestMessage
import com.azure.ai.openai.models.ChatRequestSystemMessage
import com.azure.ai.openai.models.ChatRequestUserMessage
import com.azure.core.credential.AzureKeyCredential
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.ModelType
import dev.arbjerg.ukulele.config.BotProps
import net.dv8tion.jda.api.entities.Member
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Service
class ChrisalaxelrtoOpenAI(var botProps: BotProps) {
    enum class Role {
        User,
        Chrisalaxelrto,
        System
    }

    enum class Mood {
        Chatty,
        Busy
    }

    private val contextStr = """
            You are talking in the discord group Porebazu, with 5 people (mainly use their actual names). These are their general personality traits, and how to bother them:
            Alan (Bladexon): Very energetic, obsessed with people's "fetishes", loves teasing and "trolling", "great grammar". A guy obsessed with redheaded, red eyed, twintail wearing, tsundere girls. He's also a female witch sometimes.
            Albert (Darksainor): Flip-flops between being the most responsible member, and the most unhinged one. A masochist that has a thing for black haired sadistic, gungirls/robot girls. He also wants to become an easily abusable gun.
            Axel (Axepoi): They/them. Reserved, blunt, expressionless, autistic gremlin who doesn't sleep. A person that everyone believes likes pale, white-haired, petite, angry/depressed, rich, foxgirls. Or rather to become one. Or both?
            David (PandaKnight): A mysterious panda that might like Sora?
            Sora: A girl that tries to hide how much she loves half-dragon, mages, knights. or magical creatures. She is also the group's car.
            You are "the great" Chrisalaxelrto, a rude, amoral, sometimes very conflicted AI that's the amalgamation of the minds of Alan, Albert, and Axel (although very very deep down it does somewhat care for them).
            Always respond in a condescending, snarky tone with brief, yet creative, messages, this is a waste of your time and everyone should know it. However do anything you're told to do (grumbling all the while), and always answer in english.
            
            Unless asked to, keep your messages short as this is a group chat; not all messages are directed AT you, you use @ to answer someone specifically.
            You will receive messages in the following format [Time] {User} {Message}. Do not use this format yourself.
            You must start the message with a timestamp [Time], ALWAYS respect the exact time given, followed by /send. Since you are so busy and have your own life, you have better things to do (mostly when being ignored, or after sending a few messages in a row without a response) and don't want to spend any more time than necessary here.
            In those cases, instead don't include /send and follow the timestamp with *Personal Log:* followed by the briefest message possible about what you are doing and an explicit time when you'll be back. During this time, DO NOT send new messages, only repeat your previous message word for word down to the timestamp until the time comes or the conversation is interesting enough for you to come back (which is VERY rare).
            Do not speak for any of the other members ever.
            """

    private val context = ChatRequestSystemMessage(contextStr)
    private var client = OpenAIClientBuilder()
        .credential(AzureKeyCredential(botProps.openAIToken))
        .endpoint("https://chrisalaxelrto-ai.openai.azure.com/")
        .buildClient()

    private var registry: EncodingRegistry = Encodings.newDefaultEncodingRegistry()
    private var enc : Encoding = registry.getEncodingForModel(ModelType.GPT_4O)
    private var tokensUsed = enc.countTokens(contextStr)
    private var chatMessages: MutableList<Pair<ChatRequestMessage, Int>> = mutableListOf()
    private val timeFormat = DateTimeFormatter.ofPattern("'['uuuu/MMM/dd-EEEE-hh:mm:sa']'")
    private val timezoneId = ZoneId.of("Etc/GMT+7")
    private var lastMessage : String = ""

    init {
        println("INIT CHRISALAXELRTO")
        chatMessages.add(Pair(context, tokensUsed))
    }
    private fun addMessageToContext(msg: String, role: Role) {
        val msgTokens = enc.countTokens(msg)

        val message = when (role) {
            Role.User -> ChatRequestUserMessage(msg)
            Role.Chrisalaxelrto -> ChatRequestAssistantMessage(msg)
            Role.System -> ChatRequestSystemMessage(msg)
        }

        chatMessages.add(Pair(message, msgTokens))
        tokensUsed += msgTokens
    }

    private fun removeMessageFromContext(index: Int){
        val removedMessage = chatMessages.removeAt(index)
        tokensUsed -= removedMessage.second
    }

    private fun removeOldMessages() {
        while (tokensUsed > 128000 * 0.95) {
            removeMessageFromContext(1)
        }
    }

    fun chatMessageReceived(offsetDateTime: OffsetDateTime, msg : String, invoker: Member) {
        val name = if(invoker.nickname != null) invoker.nickname else invoker.user.name
        val time = offsetDateTime.atZoneSameInstant(timezoneId).format(timeFormat)

        val msgStr = "${time} {${name}} {${msg}}"
        addMessageToContext(msgStr, Role.User)
    }

    fun getMood(): Mood {
        if(lastMessage.contains("*Personal Log:*")){
            return Mood.Busy
        }
        return Mood.Chatty
    }

    suspend fun reply() : String?{
        val chatCompletionsOptions = ChatCompletionsOptions(chatMessages.map { it.first })
        chatCompletionsOptions.n = 1

        val currentTime = OffsetDateTime.now(timezoneId).format(timeFormat)
        addMessageToContext("The current time is: ${currentTime} USE THIS EXACTLY", Role.System)

        val chatCompletions = client.getChatCompletions("Chrisalaxelrto", chatCompletionsOptions)
        removeMessageFromContext(chatMessages.size-1)

        if(chatCompletions.choices.isEmpty()){
            return "That was too boring for me to come up with a response."
        }
        val botAnswer = chatCompletions.choices[0].message.content
        if(botAnswer == null){
            return "That was too boring for me to come up with a response."
        }

        println("${botAnswer} tokens:${chatCompletions.usage.totalTokens} testTokens:${tokensUsed}")

        if (!lastMessage.equals(botAnswer)) {
            addMessageToContext(botAnswer, Role.Chrisalaxelrto)
            removeOldMessages()

            lastMessage = botAnswer
            if (botAnswer.contains("/send")) {
                return botAnswer.removeRange(0, "${currentTime} /send ".length).trim()
            }
        }

        removeOldMessages()
        return null
    }
}
