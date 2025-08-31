package com.echomi.app.network

import com.echomi.app.data.BatteryStatusRequest
import com.echomi.app.data.CallLog
import com.echomi.app.data.Contact
import com.echomi.app.data.ContactRequest
import com.echomi.app.data.FirebaseLoginRequest
import com.echomi.app.data.MessageResponse
import com.echomi.app.data.OtpRequest
import com.echomi.app.data.OtpResponse
import com.echomi.app.data.Prompt
import com.echomi.app.data.UpdatePromptRequest
import com.echomi.app.data.UserResponse
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

    // Add these new endpoints
    @GET("api/logs")
    suspend fun getCallLogs(): Response<List<CallLog>>

    @GET("api/prompts")
    suspend fun getPrompts(): Response<List<Prompt>>

    // Add this new function
    @PUT("api/prompts/{promptType}")
    suspend fun updatePrompt(
        @Path("promptType") promptType: String,
        @Body request: UpdatePromptRequest
    ): Response<Prompt>

    @POST("api/contacts")
    suspend fun saveContacts(
        @Body request: ContactRequest
    ): Response<Unit>

    // Add this new function
    @GET("api/contacts")
    suspend fun getSavedContacts(): Response<List<Contact>> // Re-using the local Contact model

    // Add this new function
    @GET("api/logs/{id}")
    suspend fun getCallLogById(@Path("id") id: String): Response<CallLog>

    @PUT("api/settings/battery-status")
    suspend fun updateBatteryStatus(@Body request: BatteryStatusRequest): Response<Unit>

    @PUT("api/settings/fcm-token")
    suspend fun updateFcmToken(
        @Body request: FcmTokenRequest,
        @Header("Authorization") authToken: String
    ): Response<MessageResponse>

    // In ApiService interface (com.echomi.app.network.ApiService.kt) - add these methods
    @POST("delivery/otp")
    suspend fun sendOtp(@Body request: OtpRequest): Response<OtpResponse>

    @GET("delivery/otp/{firebaseUid}")
    suspend fun getLatestOtp(
        @Path("firebaseUid") firebaseUid: String,
        @Query("sender") sender: String? = null,
        @Query("orderId") orderId: String? = null
    ): Response<OtpResponse>
}