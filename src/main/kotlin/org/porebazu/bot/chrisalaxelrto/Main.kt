package org.porebazu.bot.chrisalaxelrto

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.porebazu.bot.chrisalaxelrto.config.PropertySourceConfiguration
import org.porebazu.bot.chrisalaxelrto.config.BotConfig
import org.porebazu.bot.chrisalaxelrto.utils.AzureEnvironmentChecker
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment

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
	 * We explicitly add @DependsOn to ensure the BotConfig is injected after property sources are initialized
	 */
	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	@DependsOn("propertySourcesInitializer")
	fun applicationRunner(botConfig: BotConfig, azureEnvironmentChecker: AzureEnvironmentChecker, environment: Environment) : ApplicationRunner {
		return ApplicationRunner {

			val discordToken = if (azureEnvironmentChecker.isRunningInAzure()) {
				botConfig.DiscordToken
			} else {
				environment.getProperty("bot.DiscordTokenGoofy")
			}

			val jda = JDABuilder.createDefault(discordToken)
				.enableIntents(GatewayIntent.GUILD_MESSAGES)
				.build()
		}
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			SpringApplicationBuilder(ChrisalaxelrtoApplication::class.java)
				.initializers(PropertySourceConfiguration())
				.run(*args)
		}
	}
}