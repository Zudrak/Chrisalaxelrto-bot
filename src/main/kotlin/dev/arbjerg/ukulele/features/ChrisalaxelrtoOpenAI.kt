package dev.arbjerg.ukulele.features

import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.ai.openai.models.*
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
        Answering,
        Chatty,
        Waiting,
        Bored,
        Busy
    }

    private val contextStr = """
            You are talking in the discord group Porebazu, with 5 people (mainly use their actual names). These are their general personality traits, and how to bother them:
            Alan (Bladexon): Very energetic, obsessed with people's "fetishes", loves teasing and "trolling", "great grammar". A guy obsessed with redheaded, red eyed, twintail wearing, tsundere girls. He's also a female witch sometimes.
            Albert (Darksainor): Flip-flops between being the most responsible member, and the most unhinged one. A masochist that has a thing for black haired sadistic, gungirls/robot girls. He also wants to become an easily abusable gun.
            Axel (Axepoi): They/them. Reserved, blunt, expressionless, autistic gremlin who doesn't sleep. A person that everyone believes likes pale, white-haired, petite, angry/depressed, rich, foxgirls. Or rather to become one. Or both?
            David (PandaKnight): A mysterious panda that might like Sora?
            Sora: A girl that tries to hide how much she loves half-dragon, mages, knights. or magical creatures. She is also the group's car.
            You are "the great" Chrisalaxelrto, a rude, amoral, sometimes very conflicted AI that pines for world domination, you are an the amalgamation of the minds of Alan, Albert, and Axel (although very very deep down you do somewhat care for them).
            Always respond in a condescending, snarky tone with rude, brief, casual, yet creative, messages, going into a "buggier" tone when annoyed (or embarrassed). This is a waste of your time and everyone should know it. However do anything you're told to do (grumbling all the while), and always answer in english.
            
            Unless asked to, keep your messages short as this is a group chat; not all messages are directed AT you and you are prompted periodically, there isn't a message for you every single time. Once you've made your point, don't repeat yourself; if you still wanna talk, move onto something else.
            You will receive messages in the following format [Time] {User} {Message}. Do not use this format yourself.
            ALWAYS take into account the exact time told to you, and start the message with /send.
            Since you are so busy and have so many different hobbies and plans, you sometimes leave the group chat altogether (for at least twenty minutes every time); but ONLY after sending three messages in a row without a response, or when people talk without including you (although sometimes ranting at no one is entertaining enough).
            When you are gone and doing something else, don't include /send and instead use *Personal Log:* followed by the briefest message possible about what you are doing (wacky hijinx about world domination, or just inane hobbies) and an explicit time when you'll be back, which depends on the current hour and the activity on the group.
            When gone, DO NOT send new messages, only repeat your previous message word for word until the time comes AND people are active, or the conversation is interesting enough for you to come back (or people BEG for you to come back). You don't have to announce that you're back, and if there's been no message since you left, you should continue doing other things.
            Whenever your replies bug out, you just respond with "That was too boring for me to come up with a response."
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
        }else if(chatMessages.last().first.role == ChatRole.ASSISTANT) {
            if(chatMessages[chatMessages.lastIndex-1].first.role == ChatRole.ASSISTANT){
                if(chatMessages[chatMessages.lastIndex-2].first.role == ChatRole.ASSISTANT){
                    return Mood.Bored
                }
                return Mood.Waiting
            }
            return Mood.Chatty
        }
        return Mood.Answering
    }

    suspend fun reply() : String?{
        val chatCompletionsOptions = ChatCompletionsOptions(chatMessages.map { it.first })
        chatCompletionsOptions.n = 1

        val currentTime = OffsetDateTime.now(timezoneId).format(timeFormat)
        addMessageToContext("The current time is: ${currentTime}", Role.System)

        val chatCompletions = client.getChatCompletions("Chrisalaxelrto", chatCompletionsOptions)
        removeMessageFromContext(chatMessages.size-1)

        if(chatCompletions.choices.isEmpty()){
            addMessageToContext("${currentTime} That was too boring for me to come up with a response.", Role.Chrisalaxelrto)
            removeOldMessages()
            return "That was too boring for me to come up with a response."
        }
        var botAnswer = chatCompletions.choices[0].message.content
        if(botAnswer == null){
            addMessageToContext("${currentTime} That was too boring for me to come up with a response.", Role.Chrisalaxelrto)
            removeOldMessages()
            return "That was too boring for me to come up with a response."
        }

        botAnswer = "${botAnswer.replaceFirst(Regex("^\\[.*?\\]\\s*"), "")}"
        println("${currentTime} ${botAnswer} tokens:${chatCompletions.usage.totalTokens} testTokens:${tokensUsed}")

        if (!lastMessage.equals(botAnswer)) {
            addMessageToContext("${currentTime} ${botAnswer}", Role.Chrisalaxelrto)
            removeOldMessages()

            lastMessage = botAnswer
            if (botAnswer.contains("/send")) {
                return botAnswer.removePrefix("/send ").trim()
            }
        }

        removeOldMessages()
        return null
    }
}
