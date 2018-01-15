package com.gabrielhuff.sample.login.server

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

/**
 * An error to be issued by the application whenever anything goes wrong.
 *
 * Calling [fail] will cause the app to throw an exception.
 *
 * The [handleApiErrors] extension can be used on server response streams to automatically handle
 * issued errors (triggered by calls to [fail]) by modifying the response.
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ApiError(val statusCode: Int, val statusName: String, val cause: String) {

    private constructor(httpStatus: HttpStatus, cause: String) : this(httpStatus.value(), httpStatus.reasonPhrase, cause)

    companion object {

        val MISSING_BASIC_AUTH = ApiError(HttpStatus.UNAUTHORIZED, "Missing authorization. Make sure to use HTTP basic auth.")

        val AUTH_SCHEME_NOT_BASIC = ApiError(HttpStatus.BAD_REQUEST, "Invalid authorization scheme. Make sure to use HTTP basic auth.")

        val UNAUTHORIZED_CREDENTIALS = ApiError(HttpStatus.UNAUTHORIZED, "Invalid username / password combination.")

        val CONTENT_TYPE_NOT_JSON = ApiError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Invalid content type. Make sure to explicitly send an application/json")

        val BODY_NOT_USER_DATA = ApiError(HttpStatus.BAD_REQUEST, "Invalid request body. Make sure to provide a valid user data json.")

        val BODY_USER_DATA_INVALID = ApiError(HttpStatus.BAD_REQUEST, "Invalid user data params. Make sure that all the fields are within the accepted ranges.")

        val INVALID_CREDENTIALS_FORMAT = ApiError(HttpStatus.BAD_REQUEST, "Invalid credentials format. Make sure that both username and password only contain alphanumeric characters and are between 1 and 16 characters long.")

        val CONTRASTING_REQUEST_USERNAME = ApiError(HttpStatus.UNAUTHORIZED, "Invalid credentials. Make sure that the username specified on the credentials match the one from the user data.")

        val DUPLICATE_USERNAME = ApiError(HttpStatus.CONFLICT, "A user with the given username already exists")

        val MISSING_BEARER_AUTH = ApiError(HttpStatus.UNAUTHORIZED, "Missing authorization. Make sure to provide a Bearer token.")

        val AUTH_SCHEME_NOT_BEARER = ApiError(HttpStatus.BAD_REQUEST, "Invalid authorization scheme. Make sure to provide a Bearer token.")

        val UNAUTHORIZED_TOKEN = ApiError(HttpStatus.UNAUTHORIZED, "Invalid access token. Try requesting a new one before trying to access this resource.")

        val INTERNAL = ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong when processing the request.")
    }
}

/**
 * Throw an exception. Exceptions thrown here can be handled by [handleApiErrors]
 */
fun fail(apiError: ApiError): Nothing = throw ApiErrorException(apiError)

/**
 * Intercepts the receiver stream and modifies its response if any error was issued on the upstream
 */
fun Mono<ServerResponse>.handleApiErrors(): Mono<ServerResponse> = onErrorResume { it.apiErrorResponse() }

private class ApiErrorException(val error: ApiError) : Throwable()

private fun Throwable.apiErrorResponse(): Mono<ServerResponse> {
    val resolvedException = this as? ApiErrorException ?: ApiErrorException(ApiError.INTERNAL)
    val error = resolvedException.error
    val httpStatus = HttpStatus.resolve(error.statusCode) ?: HttpStatus.INTERNAL_SERVER_ERROR
    return ServerResponse.status(httpStatus).syncBody(error)
}
