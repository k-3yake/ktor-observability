package com.example.service

import com.example.model.User
import com.example.repository.UserRepository
import com.example.repository.UserResponse

class UserService(private val userRepository: UserRepository) {
    fun createUser(user: User): UserResponse {
        return userRepository.save(user)
    }
}
