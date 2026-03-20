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
        logger.info("Before IO switch - parent_id: {}")
        withContext(Dispatchers.IO) {
            logger.info("Inside IO switch - parent_id: {}")
        }
        logger.info("After IO switch - parent_id: {}")
        return userRepository.save(user)
    }
}
