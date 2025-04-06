package org.porebazu.bot.chrisalaxelrto.config

import org.porebazu.bot.chrisalaxelrto.provider.IdentityProvider
import org.porebazu.bot.chrisalaxelrto.provider.KeyVaultProvider
import org.porebazu.bot.chrisalaxelrto.provider.TableStorageProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.ConfigurableEnvironment
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

/**
 * Configuration class to properly register our custom property sources.
 * This ensures they're available for BotConfig even after the application context is created.
 * No default values are provided - missing properties will cause failure.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class PropertySourceConfiguration : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        // Mark initialization in progress
        System.setProperty("custom.property.sources.initializing", "true")
    }
    
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun propertySourcesInitializer(environment: ConfigurableEnvironment, identityProvider: IdentityProvider): PropertySourcesInitializer {
        return PropertySourcesInitializer(environment, identityProvider)
    }
    
    class PropertySourcesInitializer(
        private val environment: ConfigurableEnvironment,
        private val identityProvider: IdentityProvider
    ) {
        @Autowired
        fun initialize() {
            // Create and add our custom property sources
            val tableStorageProvider = TableStorageProvider(environment, identityProvider)
            val keyVaultProvider = KeyVaultProvider(environment, identityProvider)
            
            // Add the property sources to the environment with higher precedence
            environment.propertySources.addLast(tableStorageProvider)
            environment.propertySources.addLast(keyVaultProvider)

            // Initialization complete
            System.clearProperty("custom.property.sources.initializing")
        }
    }
    
    @Bean
    @DependsOn("propertySourcesInitializer")
    fun botConfig(environment: ConfigurableEnvironment): BotConfig {
        // Get the constructor of BotConfig
        val constructor = BotConfig::class.primaryConstructor
            ?: throw IllegalArgumentException("BotConfig must have a primary constructor")
        
        // Create a map to hold the constructor arguments
        val args = mutableMapOf<KParameter, Any?>()
        
        // For each parameter in the constructor
        for (param in constructor.parameters) {
            val name = param.name
            
            if (name != null && param.type.classifier == String::class) {
                // Try to get the value from environment with "bot." prefix
                val value = environment.getProperty("bot.$name") ?: ""
                args[param] = value
            }
        }
        
        // Create and return a new BotConfig instance with the values
        return constructor.callBy(args)
    }
}