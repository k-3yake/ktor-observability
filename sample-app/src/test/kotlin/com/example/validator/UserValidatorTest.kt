package com.example.validator

import com.example.repository.CreateUserRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserValidatorTest {

    private val validator = UserValidator()

    @Test
    fun `valid request returns empty list`() {
        val request = CreateUserRequest(name = "Alice", email = "alice@example.com", phoneNumber = "090-1234-5678", age = 25)
        val errors = validator.validate(request)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `blank name returns error`() {
        val request = CreateUserRequest(name = "", email = "alice@example.com", phoneNumber = "090-1234-5678", age = 25)
        val errors = validator.validate(request)
        assertEquals(listOf("Name must not be blank"), errors)
    }

    @Test
    fun `blank email returns error`() {
        val request = CreateUserRequest(name = "Alice", email = "", phoneNumber = "090-1234-5678", age = 25)
        val errors = validator.validate(request)
        assertEquals(listOf("Email must not be blank"), errors)
    }

    @Test
    fun `negative age returns error`() {
        val request = CreateUserRequest(name = "Alice", email = "alice@example.com", phoneNumber = "090-1234-5678", age = -1)
        val errors = validator.validate(request)
        assertEquals(listOf("Age must be 0 or greater"), errors)
    }

    @Test
    fun `multiple errors are returned`() {
        val request = CreateUserRequest(name = "", email = "", phoneNumber = "090-1234-5678", age = -1)
        val errors = validator.validate(request)
        assertEquals(3, errors.size)
    }
}
