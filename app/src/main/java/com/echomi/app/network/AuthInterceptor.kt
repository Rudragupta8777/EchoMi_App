package com.echomi.app.network

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val currentUser = FirebaseAuth.getInstance().currentUser

        // If the user is not logged in, proceed with the original request
        if (currentUser == null) {
            return chain.proceed(originalRequest)
        }

        // This is a blocking call to get the token synchronously within the interceptor.
        // It's one of the few places where runBlocking is acceptable.
        val token = runBlocking {
            try {
                currentUser.getIdToken(true).await()?.token
            } catch (e: Exception) {
                null
            }
        }

        // If we couldn't get a token, proceed without it
        if (token == null) {
            return chain.proceed(originalRequest)
        }

        // Add the "Authorization: Bearer <token>" header to the request
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}