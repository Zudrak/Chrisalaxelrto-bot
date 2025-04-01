package org.porebazu.bot.chrisalaxelrto.provider

import com.azure.core.credential.TokenCredential
import com.azure.identity.ManagedIdentityCredentialBuilder
import org.porebazu.bot.chrisalaxelrto.identity.MsalTokenCredential
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class IdentityProvider(environment: Environment) {
    private lateinit var credential: TokenCredential
    init {
        if(environment.getProperty("ENVIRONMENT") == "Azure"){
            val clientId = environment.getProperty("aad-mi-client-id")
            credential = ManagedIdentityCredentialBuilder().clientId(clientId).build()
        }else{
            val tenantId = environment.getProperty("aad-tenant-id")
            val appClientId = environment.getProperty("aad-app-client-id")
            credential = MsalTokenCredential(
                appClientId!!,
                "https://login.microsoftonline.com/$tenantId"
            )
        }
    }
    fun getCredential(): TokenCredential {
        return credential
    }
}