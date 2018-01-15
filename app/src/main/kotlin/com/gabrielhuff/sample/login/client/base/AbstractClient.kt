package com.gabrielhuff.sample.login.client.base

import io.reactivex.Observable

/**
 * Base abstraction of a client that solely defines its public API
 */
abstract class AbstractClient {

    /**
     * The current client state
     */
    abstract val state: ClientState

    /**
     * The current user data or `null` if state is not [ClientState.LOGGED_IN]
     */
    abstract val userData: UserData?

    /**
     * A stream of client states. The first element will be emitted at subscription and it will be
     * equal to the value returned by [state]
     */
    abstract val stateStream: Observable<ClientState>

    /**
     * Events triggered whenever a manual login operation failed.
     */
    abstract val loginFailedEvents: Observable<LoginFailedEvent>

    /**
     * Events triggered whenever a sign up operation failed.
     */
    abstract val signUpFailedEvents: Observable<SignUpFailedEvent>

    /**
     * Login using the input credentials. The client state will then be updated to
     * [ClientState.LOGGING_IN] and eventually move to either [ClientState.LOGGED_IN] or
     * [ClientState.LOGGED_OUT]. On the second case, a fail event will be emitted on
     * [loginFailedEvents].
     *
     * This operation has a 10 seconds timeout. Failing to achieve the desired state within this
     * time window will result on a fail event emission.
     */
    abstract fun login(username: String, password: String)

    /**
     * Sign up using the input user data and password. The client state will then be updated to
     * [ClientState.SIGNING_UP] and eventually move to either [ClientState.LOGGING_IN] or
     * [ClientState.LOGGED_OUT]. On the second case, a fail event will be emitted on
     * [signUpFailedEvents].
     *
     * This operation has a 10 seconds timeout. Failing to achieve the desired state within this
     * time window will result on a fail event emission.
     */
    abstract fun signUp(userData: UserData, password: String)

    /**
     * Logout if not already logged out. The client state will then be updated to
     * [ClientState.LOGGED_OUT].
     */
    abstract fun logout()
}

/**
 * A description of a state in which a client is.
 */
enum class ClientState {

    /**
     * The client is logged out
     */
    LOGGED_OUT,

    /**
     * The client is logging in
     */
    LOGGING_IN,

    /**
     * The client is signing up
     */
    SIGNING_UP,

    /**
     * The client is logged in
     */
    LOGGED_IN
}

/**
 * Describes the data from a user (either existing or to be registered)
 */
data class UserData(

        /**
         * Tue username
         */
        val username: String,

        /**
         * The user RxJava skills from 0 to 1 (inclusive)
         */
        val skillRxJava: Float,

        /**
         * The user Docker skills from 0 to 1 (inclusive)
         */
        val skillDocker: Float,

        /**
         * The user Kotlin skills from 0 to 1 (inclusive)
         */
        val skillKotlin: Float
)

/**
 * Describes an error to be issued by the client when there's a communication problem
 */
sealed class ClientError : RuntimeException() {

    /**
     * No internet connection
     */
    object NoConnection : ClientError()

    /**
     * Not authorized to perform a requested operation
     */
    object Unauthorized : ClientError()

    /**
     * Requested username is unavailable for registration
     */
    object UsernameUnavailable : ClientError()

    /**
     * Unknown error
     */
    object Unknown : ClientError()
}

/**
 * Represents a failed login attempt
 */
data class LoginFailedEvent(

        /**
         * The username provided when logging in
         */
        val username: String,

        /**
         * The password provided when logging in
         */
        val password: String,

        /**
         * The reason why the login attempt failed. It can be:
         *
         * - [ClientError.NoConnection] - If there's no connection to the service
         * - [ClientError.Unauthorized] - If credentials are invalid
         * - [ClientError.Unknown] - If there's any other communication error
         */
        val error: ClientError
)

/**
 * Represents a failed sign up attempt
 */
data class SignUpFailedEvent(

        /**
         * The user data provided when signing up
         */
        val userData: UserData,

        /**
         * The password provided when signing up
         */
        val password: String,

        /**
         * The reason why the sign up attempt failed. It can be:
         *
         * - [ClientError.NoConnection] - If there's no connection to the service
         * - [ClientError.UsernameUnavailable] - If the input username is already used
         * - [ClientError.Unknown] - If there's any other communication error
         */
        val error: ClientError
)

