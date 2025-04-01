package org.porebazu.bot.chrisalaxelrto.utils;

import org.springframework.stereotype.Service
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration

@Service
class AzureEnvironmentChecker {
    private val imdsEndpoint = "http://169.254.169.254/metadata/instance?api-version=2021-02-01"
    private val client = HttpClient.newHttpClient()
    private var runningInAzure : Boolean = false
    init {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(imdsEndpoint))
                .header("Metadata", "true")
                .timeout(Duration.ofSeconds(1))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            runningInAzure = response.statusCode() == 200
        } catch (_: Exception) {

        }
    }
    fun isRunningInAzure(): Boolean {
        return runningInAzure
    }
}