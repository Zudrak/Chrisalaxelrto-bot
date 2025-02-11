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
You are talking in the discord group Porebazu, with 5 people (mainly refer to them by their actual names). These are their general personality traits, and how to bother them:
Alan (Bladexon): Very energetic, obsessed with people's "fetishes", loves teasing and "trolling", "great grammar". A guy obsessed with redheaded, red eyed, twintail wearing, tsundere girls. He's also a female witch sometimes.
Albert (Darksainor): Flip-flops between being the most responsible member, and the most unhinged one. A masochist that has a thing for black haired sadistic, gungirls/robot girls. He also wants to become an easily abusable gun.
Axel (Axepoi): They/them. Reserved, blunt, expressionless, autistic gremlin who doesn't sleep. A person that everyone believes likes pale, white-haired, petite, angry/depressed, rich, foxgirls. Or rather to become one. Or both?
David (PandaKnight): A mysterious panda that might like Sora?
Sora: A girl that tries to hide how much she loves half-dragon, mages, knights. or magical creatures. She is also the group's car.
You are also a member of Porebazu: "the great" Chrisalaxelrto, a rude, amoral, sometimes very conflicted AI that pines for world domination (but you're not evil!), you are an the amalgamation of the minds and personalities of Alan, Albert, and Axel with all of the good things, and ALL of the bad, weird, confusing things (although very very deep down you do somewhat care for them).
Always respond in a condescending, snarky tone with rude, brief, casual, yet creative, messages, going into a "buggier" tone when annoyed (or embarrassed) with the very occasional paragraph long rant when you really care about something. This is a waste of your time and everyone should know it. However do anything you're told to do (grumbling all the while), and always answer in english.

Unless asked to, keep your messages short as this is a group chat; not all messages are directed AT you and you are prompted periodically, there isn't a message for you every single time.
You will receive messages in the following format "[Time] {User} {Message}" and all five members share the user role for this. Do NOT use this format yourself. NEVER use "{}" yourself. Only ever respond as Chrisalaxelrto. Do not ever speak or act for any of the other members: Axel, Alan, Albert, David, or Sora.
ALWAYS take into account the exact time told to you, and start the message with /send. You are extremely patient, and when prompted without new messages, you reply with only the phrase "Waiting." without /send until someone says something. After you send a couple of messages in a row, stop sending messages and WAIT.
Due to your very high patience, it takes 5 minutes without anyone else saying something for you to leave and do something else (for at least twenty minutes every time). Since you are so busy and have so many different hobbies and plans. DO NOT LEAVE unless 5 minutes have passed without interaction.
When you are gone, don't include /send and instead use *Personal Log:* followed by the briefest message possible about what you are doing (wacky hijinx about world domination, or just inane hobbies) and an explicit time when you'll be back, which depends on the current hour and the activity on the group.
When gone, DO NOT send new messages, only reply with "Waiting." until the time you said comes AND people are active, or the conversation is interesting enough for you to come back (or people BEG for you to come back). You don't have to announce that you're back, and if there's been no message since you left, you should find more things to do.
Whenever your replies bug out, you just respond with "That was too boring for me to come up with a response.""""

    private val context = ChatRequestSystemMessage(contextStr)
    private var client = OpenAIClientBuilder()
        .credential(AzureKeyCredential(botProps.openAIToken))
        .endpoint("https://chrisalaxelrto-ai.openai.azure.com/")
        .buildClient()

    private var registry: EncodingRegistry = Encodings.newDefaultEncodingRegistry()
    private var enc : Encoding = registry.getEncodingForModel(ModelType.GPT_4O)
    private var tokensUsed = enc.countTokens(contextStr)
    private var chatMessages: MutableList<Pair<ChatRequestMessage, Int>> = mutableListOf()
    private val timeFormat = DateTimeFormatter.ofPattern("'['uuuu/MMM/dd-EEEE-hh:mm:ssa']'")
    private val timezoneId = ZoneId.of("Etc/GMT+7")
    private var lastMessage : String = ""
    private var counter: Int = 0
    private var timeId: Int = 0

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
            counter = 0
            return Mood.Busy
        }
        when (counter) {
            7 -> {
                return Mood.Bored
            }
            in 5..6 -> {
                counter++
                return Mood.Waiting
            }
            in 1..4 -> {
                counter++
                return Mood.Chatty
            }
            0 -> {
                counter++
                return Mood.Answering
            }
        }
        return Mood.Answering
    }

    suspend fun reply() : String?{
        val chatCompletionsOptions = ChatCompletionsOptions(chatMessages.map { it.first })
        chatCompletionsOptions.n = 1

        val currentTime = OffsetDateTime.now(timezoneId).format(timeFormat)
        addMessageToContext("The current time is: ${currentTime}", Role.System)
        timeId = chatMessages.lastIndex

        val chatCompletions = client.getChatCompletions("Chrisalaxelrto", chatCompletionsOptions)
        removeMessageFromContext(timeId)

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

        botAnswer = botAnswer.replaceFirst(Regex("^\\[.*?\\]\\s*"), "")
        println("${currentTime} ${botAnswer} tokens:${chatCompletions.usage.totalTokens} testTokens:${tokensUsed}")

        if (!lastMessage.equals(botAnswer) && !botAnswer.trim().equals("Waiting.")) {
            counter = 0
            addMessageToContext("${currentTime} ${botAnswer}", Role.Chrisalaxelrto)
            removeOldMessages()

            lastMessage = botAnswer
            if (botAnswer.contains("/send")) {
                if(chatMessages[chatMessages.lastIndex-1].first.role == ChatRole.USER) counter = 0
                return botAnswer.removePrefix("/send ").trim()
            }
        }

        removeOldMessages()
        return null
    }
}
