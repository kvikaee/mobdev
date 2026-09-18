package io.github.mobdev.api

import java.io.IOException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class UnauthorizedException : IOException("Unauthorized")

object ApiClient {

    private const val BASE_URL = "https://faerytea.name/"
    private const val TOKEN_HEADER = "X-Auth-Token"

    @Volatile
    private var authToken: String? = null

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun setToken(token: String?) {
        authToken = token
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder().apply {
                authToken?.takeIf { it.isNotBlank() }?.let { header(TOKEN_HEADER, it) }
            }.build()
            val response = chain.proceed(request)
            if (response.code == HTTP_UNAUTHORIZED) {
                response.close()
                throw UnauthorizedException()
            }
            response
        }
        .build()

    val api: ChatApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()
        .create(ChatApi::class.java)

    fun thumbUrl(path: String): String = buildImageUrl("thumb", path)

    fun imageUrl(path: String): String = buildImageUrl("img", path)

    private fun buildImageUrl(prefix: String, path: String): String =
        BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments(prefix)
            .addPathSegments(normalizeImagePath(path))
            .build()
            .toString()

    private fun normalizeImagePath(path: String): String {
        val cleaned = path.replace('\\', '/')
        val marker = "/pics/"
        val markerIndex = cleaned.lastIndexOf(marker)
        val relative = if (markerIndex >= 0) {
            cleaned.substring(markerIndex + marker.length)
        } else {
            cleaned
        }
        return relative.trimStart('/')
    }

    private const val JSON_MEDIA_TYPE = "application/json"
    private const val HTTP_UNAUTHORIZED = 401
}
