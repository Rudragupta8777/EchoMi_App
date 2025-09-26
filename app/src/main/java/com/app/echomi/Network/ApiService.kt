package com.app.echomi.Network

import com.app.echomi.data.*
import com.echomi.app.network.FcmTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    // Health check endpoint
    @GET("/")
    suspend fun healthCheck(): Response<Unit>

    // Firebase login/registration endpoint
    @POST("api/auth/firebase")
    suspend fun loginWithFirebase(@Body request: FirebaseLoginRequest): Response<UserResponse>

    @GET("api/logs")
    suspend fun getCallLogs(): Response<List<CallLog>>

    @GET("api/prompts")
    suspend fun getPrompts(): Response<List<Prompt>>

    @PUT("api/prompts/{promptType}")
    suspend fun updatePrompt(
        @Path("promptType") promptType: String,
        @Body request: UpdatePromptRequest
    ): Response<Prompt>

    @POST("api/contacts")
    suspend fun saveContacts(
        @Body request: ContactRequest
    ): Response<Unit>

    @GET("api/contacts")
    suspend fun getSavedContacts(): Response<List<Contact>>

    @GET("api/logs/{id}")
    suspend fun getCallLogById(@Path("id") id: String): Response<CallLog>

    @PUT("api/settings/fcm-token")
    suspend fun updateFcmToken(
        @Body request: FcmTokenRequest,
        @Header("Authorization") authToken: String
    ): Response<MessageResponse>

    @GET("delivery/otp/{firebaseUid}")
    suspend fun getLatestOtp(
        @Path("firebaseUid") firebaseUid: String,
        @Query("sender") sender: String? = null,
        @Query("orderId") orderId: String? = null
    ): Response<OtpResponse>

    // Add this new endpoint for storing SMS messages
    // MODIFY this endpoint to accept the Authorization header
    @POST("api/sms/call/store")
    suspend fun storeSmsMessages(
        @Header("Authorization") authToken: String, // First argument (String)
        @Body request: SmsStoreRequest            // Second argument (SmsStoreRequest)
    ): Response<Unit>
}