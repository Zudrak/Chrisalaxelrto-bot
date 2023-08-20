package dev.arbjerg.ukulele.jda

import com.microsoft.azure.cognitiveservices.vision.computervision.models.VisualFeatureTypes
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import com.microsoft.azure.cognitiveservices.vision.computervision.*
import dev.arbjerg.ukulele.config.BotProps
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import kotlin.random.Random

//TODO Separate Azure image scanning from listener
@Service
class ImageListener(botProps: BotProps) : ListenerAdapter() {

    private val log: Logger = LoggerFactory.getLogger(EventHandler::class.java)
    private val subscriptionKey = botProps.azureToken
    private val endpoint = botProps.azureEndpoint
    private val compVisClient = authenticate(subscriptionKey, endpoint)

    private final fun authenticate(subscriptionKey: String, endpoint: String): ComputerVisionClient{
        return ComputerVisionManager.authenticate(subscriptionKey).withEndpoint(endpoint)
    }

    fun describeRemoteImage(url: String?): String {
        val features = ArrayList<VisualFeatureTypes>()
        features.add(VisualFeatureTypes.TAGS)
        features.add(VisualFeatureTypes.DESCRIPTION)
        features.add(VisualFeatureTypes.ADULT)
        features.add(VisualFeatureTypes.BRANDS)
        features.add(VisualFeatureTypes.CATEGORIES)
        features.add(VisualFeatureTypes.COLOR)
        features.add(VisualFeatureTypes.FACES)
        features.add(VisualFeatureTypes.OBJECTS)

        try{
            //var description = compVisClient.computerVision().describeImage().withUrl(url).withVisualFeatures(features).execute()
            //var captions = description.captions()
            val analyze = compVisClient.computerVision().analyzeImage().withUrl(url).withVisualFeatures(features).execute()
            val captions = analyze.description().captions()

            return if (captions.size == 1) {
                val selectedCaption = captions[0]
                val answer = selectedCaption.text()

                log.info("Analyzed image to be $answer with ${selectedCaption.confidence()*100}% certainty")
                answer
            } else if (captions.size > 0) {
                val selectedCaption = captions[Random.nextInt(captions.size - 1)]
                val answer = selectedCaption.text()

                log.info("Analyzed image to be $answer with ${selectedCaption.confidence()*100}% certainty")
                answer
            } else {
                "my brain is completely empty right now, ask me later"
            }
        }

        catch (e: Exception) {
            log.info("Failed at analyzing: ${e.message}")
            return "WHAT THE FUCK IS THAT, MY EYES ARE BURNING, WHY WOULD YOU DO THIS?!"
        }
    }

    private val artWording = listOf(
        "Is that the mythical %s ?",
        "Am I wrong or is that %s ?",
        "Holy crap, you can't just post %s you weirdo",
        "What the hell is wrong with you, why are you showing us %s ?",
        "Oh my favorite, %s",
        "Heck yes, I love %s",
        "I don't even know what im looking at... is it %s or something weirder?",
        "Should I be seeing %s right now?"
    )

    fun respondImage(url: String?, channel: TextChannel, message: Message){
        val selectedWording = artWording[Random.nextInt(artWording.size - 1)]
        val description = selectedWording.format(describeRemoteImage(url))
        log.info("Trying to understand art from ${message.contentRaw}")
        val msg = MessageCreateBuilder().addContent(description).build()
        channel.sendMessage(msg).queue()
    }

    override fun onMessageReceived(event: MessageReceivedEvent){

        if (!event.author.isBot) {
            val channel = event.channel
            val member = event.member
            val message = event.message

            //search if message contains any image embeds
            val embeds = message.embeds
            for (embed in embeds) {
                val site = embed.siteProvider
                if (site?.name == "Tenor") {
                    val url = embed.url
                    var text = url?.replace("https://tenor.com/view", "")
                    if (text != null) {
                        text = text.replace("-"," ")
                        val re = Regex("[^A-Za-z ]")
                        text = re.replace(text, "") // works
                        val msg = MessageCreateBuilder().addContent("wow is that an animated $text").build()
                        channel.sendMessage(msg).queue()
                    }
                }
                if (embed.type == EmbedType.IMAGE) {
                    respondImage(embed?.url, channel.asTextChannel(), message)
                }
            }

            //search if message contains any image files
            val attachments = message.attachments
            for (attachment in attachments) {
                //log.info("Attachment: ${attachment.getContentType()}")
                if (attachment.isImage()) {
                    respondImage(attachment?.proxyUrl, channel.asTextChannel(), message)
                }
            }
        }
    }

}