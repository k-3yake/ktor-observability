package com.example.repository

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

    fun save(request: CreateUserRequest): UserResponse {
        val id = UUID.randomUUID().toString()
        val user = UserResponse(
            id = id,
            name = request.name,
            email = request.email,
            phoneNumber = request.phoneNumber,
            age = request.age
        )
        users[id] = user
        return user
    }
}
