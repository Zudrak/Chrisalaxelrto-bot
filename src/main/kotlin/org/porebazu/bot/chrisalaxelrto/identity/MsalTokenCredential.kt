package org.porebazu.bot.chrisalaxelrto.identity
import com.azure.core.credential.AccessToken
import com.azure.core.credential.TokenRequestContext
import com.azure.core.credential.TokenCredential
import com.microsoft.aad.msal4j.*
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

class MsalTokenCredential(
    clientId: String,
    authority: String
) : TokenCredential {
    private val cacheAccessAspect = SafeTokenCacheAccessAspect()
    private val app: PublicClientApplication = PublicClientApplication
        .builder(clientId)
        .authority(authority)
        .setTokenCacheAccessAspect(cacheAccessAspect)
        .build()

    override fun getToken(request: TokenRequestContext?): Mono<AccessToken> {
        val scopes = request?.scopes?.toSet() ?: emptySet()
        var authRes : IAuthenticationResult? = null

        try {
            authRes = app.acquireTokenSilently(
                SilentParameters.builder(scopes)
                    .build()
            ).get()

        }catch (e: Exception) {
            // If silent token acquisition fails, fall back to device code flow
            println("Silent token acquisition failed: ${e.message}")
            authRes = app.acquireToken(
                DeviceCodeFlowParameters.builder(scopes) { deviceCode ->
                    println("Go to: ${deviceCode.verificationUri()} and enter the code: ${deviceCode.userCode()}")
                }.build()
            ).get()
        }finally {
            if (authRes == null) {
                throw RuntimeException("Failed to acquire token")
            }
        }

        return AccessToken(authRes.accessToken(), OffsetDateTime.now().plusSeconds(authRes.expiresOnDate().time - System.currentTimeMillis() / 1000))
            .let { token ->
                Mono.just(token)
            }
    }
}
