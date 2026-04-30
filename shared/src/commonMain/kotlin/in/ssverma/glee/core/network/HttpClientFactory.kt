package `in`.ssverma.glee.core.network

import `in`.ssverma.glee.GleeConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {
    fun create(json: Json): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 3600_000 // 1 hour
                connectTimeoutMillis = 60_000
                socketTimeoutMillis = 3600_000
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("Ktor: $message")
                    }
                }
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }

            defaultRequest {
                if (url.host.endsWith("huggingface.co") && GleeConfig.config.hfToken.isNotBlank()) {
                    header(HttpHeaders.Authorization, "Bearer ${GleeConfig.config.hfToken}")
                }
            }
        }
    }
}
