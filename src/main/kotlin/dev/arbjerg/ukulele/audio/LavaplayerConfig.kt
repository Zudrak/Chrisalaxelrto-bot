package dev.arbjerg.ukulele.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import dev.arbjerg.ukulele.config.BotProps
import dev.lavalink.youtube.YoutubeAudioSourceManager
import dev.lavalink.youtube.clients.MusicWithThumbnail
import dev.lavalink.youtube.clients.Web
import dev.lavalink.youtube.clients.WebEmbedded
import dev.lavalink.youtube.clients.WebWithThumbnail
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LavaplayerConfig(var botProps: BotProps) {

    @Bean
    fun playerManager(): AudioPlayerManager {
        val apm = DefaultAudioPlayerManager()

        val ytSourceManager: YoutubeAudioSourceManager = YoutubeAudioSourceManager(true, MusicWithThumbnail(), WebWithThumbnail(), Web(), WebEmbedded())

        ytSourceManager.useOauth2(botProps.ytAuthToken, true);
        apm.registerSourceManager(ytSourceManager)
        AudioSourceManagers.registerRemoteSources(apm, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager::class.java)
        return apm
    }

}