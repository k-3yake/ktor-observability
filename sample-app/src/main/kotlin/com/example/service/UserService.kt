package com.example.service

import com.example.model.User
import com.example.repository.UserRepository
import com.example.repository.UserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class UserService(private val userRepository: UserRepository) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    suspend fun createUser(user: User): UserResponse {
        logger.info("Before IO switch")
        withContext(Dispatchers.IO) {
            logger.info("Inside IO switch")
        }
        logger.info("After IO switch")
        return userRepository.save(user)
    }
}
