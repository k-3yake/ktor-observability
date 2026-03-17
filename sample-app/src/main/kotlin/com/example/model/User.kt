package com.example.model

import com.example.logging.Sensitive
import com.example.logging.toSafeString

data class User(
    val name: String,
    @Sensitive val email: Email,
    @Sensitive val phoneNumber: PhoneNumber,
    val age: Int
) {
    override fun toString() = toSafeString()
}
