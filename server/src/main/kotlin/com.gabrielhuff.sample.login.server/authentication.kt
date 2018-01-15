package com.gabrielhuff.sample.login.server

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.server.ServerRequest
import java.util.*

/**
 * Authorization to be extracted from a [ServerRequest] by calling [authorization]
 */
sealed class Authorization {

    /**
     * Http Basic auth.
     */
    data class Basic(val username: String, val password: String) : Authorization()

    /**
     * Bearer token auth.
     */
    data class Bearer(val token: String) : Authorization()

    /**
     * Unknown auth.
     */
    object Unknown : Authorization()

    /**
     * No authentication
     */
    object None : Authorization()
}

/**
 * Return the authorization withing the receiver request
 */
val ServerRequest.authorization: Authorization get() {
    // Get Authorization header
    val authHeader = headers().header(HttpHeaders.AUTHORIZATION)

    // Return...
    return when (authHeader.size) {
        // ...none if there's no auth at all
        0 -> Authorization.None

        // ...if there's a single auth header value, split it...
        1 -> authHeader.single().split("\\s+".toRegex())

                // ...take if and only if there are 2 substrings separated by whitespaces...
                .takeIf { it.size == 2 }

                // ...take first as scheme and second as value...
                ?.let { (scheme, value) -> when (scheme) {

                    // ...when scheme is basic...
                    "Basic" -> Base64.getDecoder()

                            // ...decode value...
                            .run { try { decode(value)} catch (e: IllegalArgumentException) {null} }
                            ?.toString(Charsets.UTF_8)

                            // ...try to split into username and password credentials...
                            ?.split(":")?.takeIf { it.size == 2 }

                            // ...basic if credentials are available
                            ?.let { (u, p) -> Authorization.Basic(u, p) }

                    // ...bearer if scheme matches
                    "Bearer" -> Authorization.Bearer(value)

                    // ...null if scheme is unknown
                    else -> null
                } }
                // ...return unknown if value can't be parsed
                ?: Authorization.Unknown

        // ...unknown if there are more than one auth header values
        else -> Authorization.Unknown
    }
}

/**
 * Return `true` if and only if both basic auth username and password are valid.
 *
 * According to the API docs, a valid username consists of a 1-16 character long (both inclusive)
 * alphanumeric string, and so is the the password.
 */
val Authorization.Basic.formatIsValid: Boolean get() =
    username.matches("""^\w{1,16}$""".toRegex()) && password.matches("""^\w{1,16}$""".toRegex())

/**
 * The auth token wrapped into an [AccessToken] instance
 */
val Authorization.Bearer.accessToken: AccessToken get() = AccessToken(token)

/**
 * A token object to be returned as response body, containing an access token string
 */
data class AccessToken(val token: String)

/**
 * A 512-bit key to be used to sign JWTs with HMAC / SHA-512. We are getting it as a base64 encoded
 * string from the Gradle build.
 */
private val JWT_KEY: ByteArray = Base64.getDecoder().decode(BuildConfig.JWT_SIGNING_KEY)

/**
 * The expiration time in minutes from issued JWTs
 */
private const val JWT_EXPIRATION_MINUTES = 60

/**
 * Create a new token to be used by a user with the given ID.
 *
 * The current implementation returns a JWT with the ID as the subject claim signed with [JWT_KEY]
 * using HMAC / SHA-512.
 */
fun newAccessToken(userId: Long): AccessToken = Jwts.builder()
        .setSubject(userId.toString())
        .signWith(SignatureAlgorithm.HS512, JWT_KEY)
        .setExpiration(Calendar.getInstance().apply { add(Calendar.MINUTE, JWT_EXPIRATION_MINUTES) }.time)
        .compact()
        .let { AccessToken(it) }

/**
 * Try to parse the input token and return its user ID or `null` if the parsing fails.
 *
 * The current implementation expects the input token format to be the same as the one used on the
 * [newAccessToken] method.
 */
val AccessToken.userId: Long? get() = try {
    Jwts.parser()
            .setSigningKey(JWT_KEY)
            .parseClaimsJws(token)
            .body
            .subject
            .toLongOrNull()
}
catch (e: JwtException) { null }
catch (e: IllegalArgumentException) { null }