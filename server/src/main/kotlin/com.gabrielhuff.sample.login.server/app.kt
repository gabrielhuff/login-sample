package com.gabrielhuff.sample.login.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.codec.DecodingException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import org.springframework.web.server.UnsupportedMediaTypeStatusException
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono

fun main(args: Array<String>) { runApplication<Application>(*args) }

/**
 * The application to be executed. This is the main entry point when analysing the code, as we are
 * defining all the behaviour here.
 */
@SpringBootApplication
class Application {

    @Bean
    fun routerFunction(userRepo: UserRepo): RouterFunction<ServerResponse> = router {

        // On issue token, take request...
        POST("/token") { it.toMono()

                // ...get authorization...
                .map { it.authorization }

                // ...fail if authorization is not defined...
                .doOnNext { if (it == Authorization.None) fail(ApiError.MISSING_BASIC_AUTH) }

                // ...try to parse as basic auth or fail if not possible...
                .map { it as? Authorization.Basic ?: fail(ApiError.AUTH_SCHEME_NOT_BASIC) }

                // ...look for user with the given credentials or fail if not found...
                .map { userRepo.findByDataUsernameAndPassword(it.username, it.password) }
                .doOnNext { if (!it.isPresent) fail(ApiError.UNAUTHORIZED_CREDENTIALS) }

                // ...get user ID...
                .map { it.get().id!! }

                // ...issue new token with the user ID...
                .map { newAccessToken(it) }

                // ...succeed and return token...
                .flatMap { ServerResponse.ok().syncBody(it) }

                // ...handle errors
                .handleApiErrors()
        }

        // On create user, take authorization / user data pair...
        POST("/user") { Mono.zip(it.authorization.toMono(), it.bodyToMono(UserData::class.java)) { auth, data -> Pair(auth, data) }

                // ...fail if request content type is invalid...
                .onErrorMap(UnsupportedMediaTypeStatusException::class.java) { fail(ApiError.CONTENT_TYPE_NOT_JSON) }

                // ...fail if request body couldn't be parsed to a user data entity...
                .onErrorMap(DecodingException::class.java) { fail(ApiError.BODY_NOT_USER_DATA) }

                // ...fail if the user data params are not valid...
                .doOnNext { (_, data) -> if (!data.isValid) fail(ApiError.BODY_USER_DATA_INVALID) }

                // ...fail if authorization is not defined...
                .doOnNext { (auth, _) -> if (auth == Authorization.None) fail(ApiError.MISSING_BASIC_AUTH) }

                // ...try to parse as basic auth or fail if not possible...
                .doOnNext { (auth, _) -> if (auth !is Authorization.Basic) fail(ApiError.AUTH_SCHEME_NOT_BASIC) }
                .map { (auth, data) -> auth as Authorization.Basic to data }

                // ...fail if credentials format is invalid...
                .doOnNext { (auth, _) -> if (!auth.formatIsValid) fail(ApiError.INVALID_CREDENTIALS_FORMAT) }

                // ...fail if credentials username does not match the one provided as user data...
                .doOnNext { (auth, data) -> if (auth.username != data.username) fail(ApiError.CONTRASTING_REQUEST_USERNAME) }

                // ...create user to be saved...
                .map { (auth, data) -> User(data = data, password = auth.password) }

                // ...save user...
                .map { userRepo.save(it) }

                // ...fail if user can't be added due to duplicate username...
                .onErrorMap(DataIntegrityViolationException::class.java) { fail(ApiError.DUPLICATE_USERNAME) }

                // ...get saved user data...
                .map { it.data }

                // ...succeed and return saved user data...
                .flatMap { ServerResponse.status(HttpStatus.CREATED).syncBody(it) }

                // ...handle errors
                .handleApiErrors()
        }

        // On get user, take request...
        GET("/user") { it.toMono()

                // ...get authorization...
                .map { it.authorization }

                // ...fail if authorization is not defined...
                .doOnNext { if (it == Authorization.None) fail(ApiError.MISSING_BEARER_AUTH) }

                // ...try to parse as bearer token auth or fail if not possible...
                .map { it as? Authorization.Bearer ?: fail(ApiError.AUTH_SCHEME_NOT_BEARER) }

                // ...try to get access token user ID or fail if not possible...
                .map { it.accessToken.userId ?: fail(ApiError.UNAUTHORIZED_TOKEN) }

                // ...get user with the extracted ID or fail if non-existing...
                .map { userRepo.findById(it) }
                .doOnNext { if (!it.isPresent) fail(ApiError.UNAUTHORIZED_TOKEN) }

                // ...get user data...
                .map { it.get().data }

                // ...succeed and return user data...
                .flatMap { ServerResponse.ok().syncBody(it) }

                // ...handle errors
                .handleApiErrors()
        }
    }
}