package com.example.repository

import com.example.logging.Sensitive
import com.example.logging.toSafeString
import com.example.model.User
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class CreateUserRequest(
    val name: String,
    @Sensitive val email: String,
    @Sensitive val phoneNumber: String,
    val age: Int
) {
    override fun toString() = toSafeString()
}

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    @Sensitive val email: String,
    @Sensitive val phoneNumber: String,
    val age: Int
) {
    override fun toString() = toSafeString()
}

class UserRepository {
    private val users = ConcurrentHashMap<String, UserResponse>()

    fun save(user: User): UserResponse {
        val id = UUID.randomUUID().toString()
        val userResponse = UserResponse(
            id = id,
            name = user.name,
            email = user.email.value,
            phoneNumber = user.phoneNumber.value,
            age = user.age
        )
        users[id] = userResponse
        return userResponse
    }
}
