package org.porebazu.bot.chrisalaxelrto.provider

import com.azure.core.credential.TokenCredential
import com.azure.identity.ManagedIdentityCredentialBuilder
import org.porebazu.bot.chrisalaxelrto.identity.MsalTokenCredential
import org.porebazu.bot.chrisalaxelrto.utils.AzureEnvironmentChecker
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class IdentityProvider(environment: Environment, azureEnvironmentChecker: AzureEnvironmentChecker) {
    private lateinit var credential: TokenCredential
    init {
        credential = if(azureEnvironmentChecker.isRunningInAzure()){
            val clientId = environment.getProperty("aad-mi-client-id")
            ManagedIdentityCredentialBuilder().clientId(clientId).build()
        }else{
            val tenantId = environment.getProperty("aad-tenant-id") ?: "default-tenant-id"
            val appClientId = environment.getProperty("aad-app-client-id") ?: "default-client-id"
            MsalTokenCredential(
                appClientId,
                "https://login.microsoftonline.com/$tenantId"
            )
        }
    }
    fun getCredential(): TokenCredential {
        return credential
    }
}