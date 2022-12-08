package dev.arbjerg.ukulele.command

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.arbjerg.ukulele.audio.Player
import dev.arbjerg.ukulele.features.HelpContext
import dev.arbjerg.ukulele.jda.Command
import dev.arbjerg.ukulele.jda.CommandContext
import net.dv8tion.jda.api.Permission
import org.springframework.stereotype.Component
import java.net.URL
import java.util.*
import kotlin.concurrent.schedule

@Component
class InspireCommand(
        val apm: AudioPlayerManager,
) : Command ("inspire") {

    private val timer: Timer = Timer()

    override suspend fun CommandContext.invoke() {

        if ("<#${channel.id}>" != guildProperties.musicChannel) return replyWrongMusicChannel()

        if (!ensureVoiceChannel()) return

        val sessionId = URL("https://inspirobot.me/api?getSessionID=1").readText()
        val apiResponse = URL("https://inspirobot.me/api?generateFlow=1&sessionID=$sessionId").readText()
        val mp3Index = apiResponse.indexOf("\"mp3\": ")
        val mp3 = apiResponse.substring(mp3Index).dropLast(2).drop(8)
        apm.loadItem(mp3, Loader(this, player, mp3))

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
            val started = player.add(track)
            ctx.reply("Have some inspiration")
            val queueSize = player.queueSize()
            player.move(queueSize, 0)

            if(player.isRepeating) {
                player.isRepeating = false
                val duration = player.tracks[0].duration
                timer.schedule(duration+2000) {
                    player.isRepeating = true
                }
            }
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {}
        override fun noMatches() {}

        override fun loadFailed(exception: FriendlyException) {
            ctx.handleException(exception)
        }
    }

    override fun HelpContext.provideHelp() {
        addUsage("")
        addDescription("Gives some inspiration.")
    }
}