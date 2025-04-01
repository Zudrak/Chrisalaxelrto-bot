package org.porebazu.bot.chrisalaxelrto

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.porebazu.bot.chrisalaxelrto.provider.IdentityProvider
import org.porebazu.bot.chrisalaxelrto.provider.KeyVaultProvider
import org.porebazu.bot.chrisalaxelrto.provider.TableStorageProvider
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.core.env.ConfigurableEnvironment

/**
 * This is the main class of the Chrisalaxelrto application.
 * It initializes the Spring Boot application and sets up the Discord bot.

 * @author Porebazu
 * @version 1.0
 */
@SpringBootApplication
class ChrisalaxelrtoApplication {

	/**
	 * This is the entry point of the application. Think of it as the main method.
	 */
	@Bean
	fun applicationRunner(configurableEnvironment: ConfigurableEnvironment, identityProvider : IdentityProvider) : ApplicationRunner {
		return ApplicationRunner {
			val tableStorageProvider = TableStorageProvider(configurableEnvironment, identityProvider)
			val keyVaultProvider = KeyVaultProvider(configurableEnvironment, identityProvider)
			configurableEnvironment.propertySources.addLast(tableStorageProvider)
			configurableEnvironment.propertySources.addLast(keyVaultProvider)

			println("Music Channel ID was: ${configurableEnvironment.getProperty("MusicChannelId")}")
			val jda = JDABuilder.createDefault(configurableEnvironment.getProperty("DiscordToken"))
				.enableIntents(GatewayIntent.GUILD_MESSAGES)
				.build()
		}
	}


	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			SpringApplicationBuilder(ChrisalaxelrtoApplication::class.java)
				.run(*args)
		}
	}
}