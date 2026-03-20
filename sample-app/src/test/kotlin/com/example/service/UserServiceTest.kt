package com.example.service

import com.example.model.Email
import com.example.model.PhoneNumber
import com.example.model.User
import com.example.repository.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserServiceTest {

    private val userRepository = UserRepository()
    private val userService = UserService(userRepository)

    @Test
    fun `createUser returns UserResponse with generated id`() = runBlocking {
        val user = User(name = "Alice", email = Email("alice@example.com"), phoneNumber = PhoneNumber("090-1234-5678"), age = 25)
        val response = userService.createUser(user)

        assertTrue(response.id.isNotBlank())
        assertEquals("Alice", response.name)
        assertEquals("alice@example.com", response.email)
        assertEquals("090-1234-5678", response.phoneNumber)
        assertEquals(25, response.age)
    }
}
