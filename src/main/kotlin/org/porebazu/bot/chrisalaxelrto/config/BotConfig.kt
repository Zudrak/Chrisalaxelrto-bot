package org.porebazu.bot.chrisalaxelrto.config

data class BotConfig(
    /**
     * Data class represents the bot configuration.
     * This class is initialized using Springboot's Environment and PropertySources.
     * It first looks into resources/application.properties, then into Azure Table Storage and finally into Azure Key Vault.
     * It will look for values that have the exact name as the property name.
     */
    val OpenAIApiKey: String = "",
    val DiscordToken: String = "",
    val MusicChannelId: String = ""
)