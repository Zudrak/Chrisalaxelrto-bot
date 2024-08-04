package dev.arbjerg.ukulele.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import dev.lavalink.youtube.YoutubeAudioSourceManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LavaplayerConfig {

    @Bean
    fun playerManager(): AudioPlayerManager {
        val apm = DefaultAudioPlayerManager()
        val ytSourceManager: YoutubeAudioSourceManager = YoutubeAudioSourceManager()
        apm.registerSourceManager(ytSourceManager)
        AudioSourceManagers.registerRemoteSources(apm, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager::class.java)
        return apm
    }

}