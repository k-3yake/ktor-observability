package com.example.validator

import com.example.repository.CreateUserRequest

class UserValidator {
    fun validate(request: CreateUserRequest): List<String> {
        val errors = mutableListOf<String>()
        if (request.name.isBlank()) errors.add("Name must not be blank")
        if (request.email.isBlank()) errors.add("Email must not be blank")
        if (request.age < 0) errors.add("Age must be 0 or greater")
        return errors
    }
}
