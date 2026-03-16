package com.example.repository

import com.example.model.User
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String,
    val phoneNumber: String,
    val age: Int
)

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val age: Int
)

class UserRepository {
    private val users = ConcurrentHashMap<String, UserResponse>()

    fun save(user: User): UserResponse {
        val id = UUID.randomUUID().toString()
        val userResponse = UserResponse(
            id = id,
            name = user.name,
            email = user.email,
            phoneNumber = user.phoneNumber,
            age = user.age
        )
        users[id] = userResponse
        return userResponse
    }
}
