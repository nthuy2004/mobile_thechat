package com.nth.thechatptit.data.auth

import com.nth.thechatptit.data.model.AuthState
import com.nth.thechatptit.data.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val state: StateFlow<AuthState>
    val isConfigured: Boolean
    suspend fun restore()
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun register(email: String, password: String, displayName: String): Result<User>
    suspend fun updateProfile(displayName: String, avatarUrl: String? = null): Result<User>
    fun signOut()
}
