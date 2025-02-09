package dev.arbjerg.ukulele.command

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.arbjerg.ukulele.audio.Player
import dev.arbjerg.ukulele.config.BotProps
import dev.arbjerg.ukulele.features.HelpContext
import dev.arbjerg.ukulele.jda.Command
import dev.arbjerg.ukulele.jda.CommandContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.Permission
import org.springframework.stereotype.Component
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

@Component
class VoiceSpeakCommand (
    val apm: AudioPlayerManager,
    val botProps: BotProps
    ): Command("speak") {
    override suspend fun CommandContext.invoke() {


        val url = URL("https://api.elevenlabs.io/v1/text-to-speech/GBv7mTt0atIp3Br8iCZE")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("accept-Type", "audio/mpeg")
        conn.setRequestProperty("xi-api-key", botProps.elevenLabsToken)
        conn.useCaches = false


        if (!ensureVoiceChannel()) return
        guild.audioManager.sendingHandler = player

        val postData = "" +
                "{\n" +
                "    \"text\": \"${argumentText}\",\n" +
                "    \"model_id\": \"eleven_monolingual_v1\",\n" +
                "    \"voice_settings\": {\n" +
                "      \"stability\": 0.0,\n" +
                "      \"similarity_boost\": 1.0\n" +
                "    }\n" +
                "  }"

        val myFile = File("audio.mp3")

        val commandCtx = this
        withContext(Dispatchers.IO) {
            val audioFile = FileOutputStream(myFile)

            DataOutputStream(conn.outputStream).use { it.writeBytes(postData) }

            conn.inputStream.use {

                if (it.available() == 0){
                    Thread.sleep(100)
                    ///TODO add timeout!
                }

                val res = it.readBytes()
                audioFile.write(res)
            }
            audioFile.close()
            apm.loadItem(myFile.absolutePath, Loader(commandCtx, player, myFile.absolutePath))
        }
    }

    fun CommandContext.ensureVoiceChannel(): Boolean {
        val ourVc = guild.selfMember.voiceState?.channel
        val theirVc = invoker.voiceState?.channel

        if (ourVc == null && theirVc == null) {
            reply("You need to be in a voice channel")
            return false
        }

        if (ourVc != theirVc && theirVc != null)  {
            val canTalk = selfMember.hasPermission(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
            if (!canTalk) {
                reply("I need permission to connect and speak in ${theirVc.name}")
                return false
            }

            guild.audioManager.openAudioConnection(theirVc)
            guild.audioManager.sendingHandler = player
            return true
        }

        return ourVc != null
    }

    inner class Loader(
        private val ctx: CommandContext,
        private val player: Player,
        private val identifier: String
    ) : AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack) {
            ctx.reply("Have some speech")
            val started = player.add(track)
            val queueSize = player.queueSize()
            player.move(queueSize, 0)
            var volume = player.volume
            val duration = player.tracks[0].duration
            player.volume = 100
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            ctx.reply("playlistLoaded")
        }
        override fun noMatches() {
            ctx.reply("noMatches")
        }

        override fun loadFailed(exception: FriendlyException) {
            ctx.reply("loadFailed")
            ctx.handleException(exception)
        }
    }
    override fun HelpContext.provideHelp() {
        addUsage("<text>")
        addDescription("Says the given text using voice synthesis")
    }
}