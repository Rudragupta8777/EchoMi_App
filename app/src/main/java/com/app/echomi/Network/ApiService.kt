package com.app.echomi.Network

import com.app.echomi.data.BatteryStatusRequest
import com.app.echomi.data.CallLog
import com.app.echomi.data.Contact
import com.app.echomi.data.ContactRequest
import com.app.echomi.data.FirebaseLoginRequest
import com.app.echomi.data.MessageResponse
import com.app.echomi.data.OtpRequest
import com.app.echomi.data.OtpResponse
import com.app.echomi.data.Prompt
import com.app.echomi.data.UpdatePromptRequest
import com.app.echomi.data.UserResponse
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
    suspend fun getSavedContacts(): Response<List<Contact>>

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

    @POST("delivery/otp")
    suspend fun sendOtp(@Body request: OtpRequest): Response<OtpResponse>

    @GET("delivery/otp/{firebaseUid}")
    suspend fun getLatestOtp(
        @Path("firebaseUid") firebaseUid: String,
        @Query("sender") sender: String? = null,
        @Query("orderId") orderId: String? = null
    ): Response<OtpResponse>
}