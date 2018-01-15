package com.gabrielhuff.sample.login.server

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.*

/**
 * A user uniquely indexed by an [id], holder of its own [data] and authenticated by a [password].
 * This is not exposed to the service consumers, as knowing the user ID (and specially his password)
 * is not on the scope of the API
 */
@Entity
data class User(

        /**
         * The user ID or null if this entity is detached
         */
        @Id
        @GeneratedValue(strategy= GenerationType.IDENTITY)
        val id: Long? = null,

        /**
         * The user data
         */
        @OneToOne(cascade = arrayOf(CascadeType.ALL))
        val data: UserData,

        /**
         * The user password.
         *
         * As a side note, **NEVER** store a password like this (as plaintext). We are only doing it
         * here for simplicity.
         */
        val password: String
)

/**
 * The data from a user, to be exposed to the API consumers.
 */
@Entity
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class UserData(

        /**
         * A 1 to 16 characters long (inclusive) unique string representing the username
         */
        @Id
        @Column(unique = true)
        val username: String,

        /**
         * A number between 0 and 1 (inclusive) indicating how proficient the user is with RxJava
         */
        val skillRxJava: Float,

        /**
         * A number between 0 and 1 (inclusive) indicating how proficient the user is with Docker
         */
        val skillDocker: Float,

        /**
         * A number between 0 and 1 (inclusive) indicating how proficient the user is with Kotlin
         */
        val skillKotlin: Float
)

/**
 * Return `true` if and only if the the receiver params are valid (see their documentation)
 */
val UserData.isValid: Boolean get() = username.matches("""^\w{1,16}$""".toRegex()) &&
        skillRxJava in 0.0f..1.0f &&
        skillDocker in 0.0f..1.0f &&
        skillKotlin in 0.0f..1.0f

/**
 * Data access object to be used for accessing [User] instances
 */
@Repository
interface UserRepo : CrudRepository<User, Long> {

    fun findByDataUsernameAndPassword(username: String, password: String): Optional<User>
}