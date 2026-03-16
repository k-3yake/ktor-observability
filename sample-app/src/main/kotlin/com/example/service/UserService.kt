package com.example.service

import com.example.model.User
import com.example.repository.UserRepository
import com.example.repository.UserResponse
import org.slf4j.LoggerFactory

class UserService(private val userRepository: UserRepository) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    fun createUser(user: User): UserResponse {
        logger.info("Creating user: {}", user)
        return userRepository.save(user)
    }
}
