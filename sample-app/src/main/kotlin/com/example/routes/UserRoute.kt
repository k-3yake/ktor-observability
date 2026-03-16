package com.example.routes

import com.example.model.Email
import com.example.model.PhoneNumber
import com.example.model.User
import com.example.repository.CreateUserRequest
import com.example.service.UserService
import com.example.validator.UserValidator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String)

fun Route.userRoute(userValidator: UserValidator, userService: UserService) {
    post("/api/users") {
        val request = call.receive<CreateUserRequest>()

        val errors = userValidator.validate(request)
        if (errors.isNotEmpty()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(errors.first()))
            return@post
        }

        val user = User(
            name = request.name,
            email = Email(request.email),
            phoneNumber = PhoneNumber(request.phoneNumber),
            age = request.age
        )
        val response = userService.createUser(user)
        call.respond(HttpStatusCode.Created, response)
    }
}
