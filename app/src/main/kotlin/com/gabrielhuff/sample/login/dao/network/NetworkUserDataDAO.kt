package com.gabrielhuff.sample.login.dao.network

import android.content.Context
import com.gabrielhuff.sample.login.BuildConfig
import com.gabrielhuff.sample.login.client.base.ClientError
import com.gabrielhuff.sample.login.client.base.UserData
import com.gabrielhuff.sample.login.dao.UserDataDAO
import com.gabrielhuff.sample.login.util.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.rx.rx_object
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import io.reactivex.Completable
import io.reactivex.Single

/**
 * A [UserDataDAO] that interacts with a remote HTTP service, assumed to provide data according to
 * a predefined API.
 *
 * A [url] should be provided on construction and should be a string pointing to the network
 * service. It should include the everything before the endpoint paths, like scheme, domain, port
 * and so on. This value defaults to [BuildConfig.SERVICE_URL].
 *
 * Instances should most likely be created by calling the secondary constructor and providing a
 * [Context]. The provided [Context] will be used internally only to check if a network connection
 * is available. However, the primary constructor can be called in order to provide a network check
 * callback. This will conveniently decouple the implementation from the Android framework, thus
 * enabling more flexible tests.
 */

class NetworkUserDataDAO(
        private val isNetworkAvailableCallback: () -> Boolean
) : UserDataDAO {

    constructor(context: Context) : this({ context.isNetworkAvailable() })

    private val url: String = BuildConfig.SERVICE_URL

    private val jsonParser = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

    override fun getRemoteToken(username: String, password: String): Single<String> {
        return Fuel.post("$url/token")
                .authenticate(username, password)
                .rx_object(jsonParser.toResponseDeserializerOf<NetworkToken>())
                .failIfNetworkUnavailable()
                .mapError(401) { ClientError.Unauthorized }
                .mapAnyError { ClientError.Unknown }
                .map { it.token }
    }

    override fun getUserData(token: String): Single<UserData> {
        return Fuel.get("$url/user")
                .authenticateBearer(token)
                .rx_object(jsonParser.toResponseDeserializerOf<NetworkUserData>())
                .failIfNetworkUnavailable()
                .mapError(401) { ClientError.Unauthorized }
                .mapAnyError { ClientError.Unknown }
                .map { it.asClientUserData }
    }

    override fun setUserData(userData: UserData, password: String): Completable {
        return Fuel.post("$url/user")
                .authenticate(userData.username, password)
                .header("Content-Type" to "application/json")
                .body(jsonParser.toJson(userData))
                .rx_object(jsonParser.toResponseDeserializerOf<NetworkUserData>())
                .failIfNetworkUnavailable()
                .mapError(409) { ClientError.UsernameUnavailable }
                .mapAnyError { ClientError.Unknown }
                .toCompletable()
    }

    private fun <T> Single<T>.failIfNetworkUnavailable(): Single<T> {
        return doOnSubscribe { if (!isNetworkAvailableCallback()) throw ClientError.NoConnection }
    }

    private data class NetworkToken(@SerializedName("token") val token: String)

    private data class NetworkUserData(
            @SerializedName("username") val username: String,
            @SerializedName("skill_rx_java") val skillRxJava: Float,
            @SerializedName("skill_docker") val skillDocker: Float,
            @SerializedName("skill_kotlin") val skillKotlin: Float
    ) {
        val asClientUserData: UserData
            get() = UserData(
                username = username,
                skillRxJava = skillRxJava,
                skillDocker = skillDocker,
                skillKotlin = skillKotlin
        )
    }
}