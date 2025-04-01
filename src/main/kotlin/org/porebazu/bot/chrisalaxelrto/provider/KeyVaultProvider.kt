package org.porebazu.bot.chrisalaxelrto.provider

import com.azure.core.exception.ResourceNotFoundException
import com.azure.core.http.policy.HttpLogDetailLevel
import com.azure.core.http.policy.HttpLogOptions
import com.azure.security.keyvault.secrets.SecretClientBuilder
import org.springframework.core.env.Environment
import org.springframework.core.env.PropertySource

class KeyVaultProvider(environment: Environment, identityProvider: IdentityProvider) : PropertySource<Any>("KeyVault") {
    private val keyVaultName: String = environment.getProperty("key-vault-name")!!
    private val keyVaultUrl = "https://${keyVaultName}.vault.azure.net/"

    private val secretClient = SecretClientBuilder()
        .vaultUrl(keyVaultUrl)
        .credential(identityProvider.getCredential())
        .httpLogOptions(HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS))
        .buildClient()
    override fun getProperty(name: String): Any? {
        if(name.startsWith("spring.")){
            return null
        }

        return try {
            secretClient.getSecret(name).value
        }catch (e: ResourceNotFoundException){
            null
        }
    }
}