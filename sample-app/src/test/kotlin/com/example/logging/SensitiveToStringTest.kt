package com.example.logging

import com.example.model.Email
import com.example.model.PhoneNumber
import com.example.model.User
import com.example.repository.CreateUserRequest
import com.example.repository.UserResponse
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class SensitiveToStringTest {

    @Test
    fun `User toString does not contain raw email`() {
        val user = User("Alice", Email("alice@example.com"), PhoneNumber("090-1234-5678"), 30)
        val result = user.toString()

        assertFalse(result.contains("alice@example.com"))
        assertFalse(result.contains("090-1234-5678"))
    }

    @Test
    fun `User toString contains non-sensitive fields as-is`() {
        val user = User("Alice", Email("alice@example.com"), PhoneNumber("090-1234-5678"), 30)
        val result = user.toString()

        assertContains(result, "Alice")
        assertContains(result, "30")
    }

    @Test
    fun `short sensitive value is fully masked`() {
        val user = User("Bob", Email("ab"), PhoneNumber("1234"), 25)
        val result = user.toString()

        assertFalse(result.contains("ab"))
        assertContains(result, "***")
    }

    @Test
    fun `CreateUserRequest toString masks sensitive fields`() {
        val request = CreateUserRequest("Alice", "alice@example.com", "090-1234-5678", 30)
        val result = request.toString()

        assertFalse(result.contains("alice@example.com"))
        assertFalse(result.contains("090-1234-5678"))
        assertContains(result, "Alice")
    }

    @Test
    fun `UserResponse toString masks sensitive fields`() {
        val response = UserResponse("id-1", "Alice", "alice@example.com", "090-1234-5678", 30)
        val result = response.toString()

        assertFalse(result.contains("alice@example.com"))
        assertFalse(result.contains("090-1234-5678"))
        assertContains(result, "Alice")
        assertContains(result, "id-1")
    }

    @Test
    fun `masked value shows prefix and suffix`() {
        val user = User("Alice", Email("alice@example.com"), PhoneNumber("090-1234-5678"), 30)
        val result = user.toString()

        assertContains(result, "ali***com")
        assertContains(result, "090***678")
    }

    @Test
    fun `nullable sensitive field is masked as stars`() {
        data class WithNullable(
            @Sensitive val secret: String?
        ) {
            override fun toString() = toSafeString()
        }

        val obj = WithNullable(null)
        val result = obj.toString()

        assertContains(result, "***")
    }
}
