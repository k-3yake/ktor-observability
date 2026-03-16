package com.example.routes

import com.example.repository.CreateUserRequest
import com.example.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String)

fun Route.userRoute(userRepository: UserRepository) {
    post("/api/users") {
        val request = call.receive<CreateUserRequest>()

        if (request.name.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Name must not be blank"))
            return@post
        }
        if (request.email.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Email must not be blank"))
            return@post
        }
        if (request.age < 0) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Age must be 0 or greater"))
            return@post
        }

        val user = userRepository.save(request)
        call.respond(HttpStatusCode.Created, user)
    }
}
