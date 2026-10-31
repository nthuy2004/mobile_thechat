package com.nth.thechatptit.data.auth

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.nth.thechatptit.data.model.AuthState
import com.nth.thechatptit.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor() : AuthRepository {
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    private val _state = MutableStateFlow<AuthState>(AuthState.Restoring)
    override val state: StateFlow<AuthState> = _state.asStateFlow()
    override val isConfigured: Boolean = auth != null

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            _state.value = firebaseAuth.currentUser?.toDomain()?.let(AuthState::LoggedIn)
                ?: AuthState.LoggedOut
        }
    }

    override suspend fun restore() {
        _state.value = AuthState.Restoring
        _state.value = auth?.currentUser?.toDomain()?.let(AuthState::LoggedIn)
            ?: AuthState.LoggedOut
    }

    override suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        val firebaseUser = auth?.signInWithEmailAndPassword(email.trim(), password)?.await()?.user
            ?: error("Firebase chưa được cấu hình")
        firebaseUser.toDomain().also { _state.value = AuthState.LoggedIn(it) }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        (auth ?: error("Firebase chưa được cấu hình"))
            .sendPasswordResetEmail(email.trim())
            .await()
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Result<User> = runCatching {
        val firebaseUser = auth?.createUserWithEmailAndPassword(email.trim(), password)?.await()?.user
            ?: error("Firebase chưa được cấu hình")
        val name = displayName.trim().ifBlank { email.substringBefore("@") }
        runCatching {
            firebaseUser.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(name).build()
            ).await()
        }
        firebaseUser.toDomain().copy(displayName = name).also {
            _state.value = AuthState.LoggedIn(it)
        }
    }

    override suspend fun updateProfile(displayName: String, avatarUrl: String?): Result<User> = runCatching {
        val firebaseUser = auth?.currentUser ?: error("Chưa đăng nhập")
        val name = displayName.trim()
        val request = UserProfileChangeRequest.Builder().setDisplayName(name)
        avatarUrl?.let { request.setPhotoUri(Uri.parse(it)) }
        firebaseUser.updateProfile(request.build()).await()
        firebaseUser.toDomain().copy(displayName = name).also {
            _state.value = AuthState.LoggedIn(it)
        }
    }

    override fun signOut() {
        auth?.signOut()
        _state.value = AuthState.LoggedOut
    }

    private fun com.google.firebase.auth.FirebaseUser.toDomain() = User(
        id = uid,
        email = email.orEmpty(),
        displayName = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@").orEmpty().ifBlank { "Walker" },
        avatarUrl = photoUrl?.toString(),
        createdAtEpochMillis = metadata?.creationTimestamp ?: System.currentTimeMillis(),
        updatedAtEpochMillis = metadata?.lastSignInTimestamp ?: System.currentTimeMillis()
    )
}
