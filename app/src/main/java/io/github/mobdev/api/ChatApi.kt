package io.github.mobdev.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @POST("login")
    suspend fun login(@Body credentials: LoginRequest): String

    @POST("logout")
    suspend fun logout(): Response<Unit>

    @GET("channels")
    suspend fun channels(): List<String>

    @GET("channel/{channel}")
    suspend fun messages(
        @Path("channel") channel: String,
        @Query("limit") limit: Int,
        @Query("lastKnownId") lastKnownId: Long?,
        @Query("reverse") reverse: Boolean,
    ): List<Message>

    @POST("messages")
    suspend fun sendMessage(@Body message: OutgoingMessage): String
}
