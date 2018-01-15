package com.gabrielhuff.sample.login.dao.memory

import com.gabrielhuff.sample.login.client.base.ClientError
import com.gabrielhuff.sample.login.client.base.UserData
import com.gabrielhuff.sample.login.dao.UserDataDAO
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * A [UserDataDAO] that interacts with a in-memory [Store] passed on construction (or an empty one
 * one by default) that emulates the remote data source.
 *
 * Additionally, a [UncertaintyParams] instance can be passed on construction in order to emulate
 * uncertain behavior.
 */
class MemoryUserDataDAO(
        private val store: Store = Store(),
        private val uncertaintyParams: UncertaintyParams = UncertaintyParams()
) : UserDataDAO {

    override fun getRemoteToken(username: String, password: String): Single<String> {
        return uncertainty()
                .flatMapObservable { Observable.fromIterable(store.records) }
                .filter { it.userData.username == username }
                .filter { it.password == password }
                .map { it.getOrCreateToken() }
                .singleElement()
                .doOnComplete { throw ClientError.Unauthorized }
                .toSingle()
    }

    override fun getUserData(token: String): Single<UserData> {
        return uncertainty()
                .flatMapObservable { Observable.fromIterable(store.records) }
                .filter { token == it.token }
                .map { it.userData }
                .singleElement()
                .doOnComplete { throw ClientError.Unauthorized }
                .toSingle()
    }

    override fun setUserData(userData: UserData, password: String): Completable {
        return uncertainty()
                .flatMapObservable { Observable.fromIterable(store.records) }
                .filter { it.userData.username == userData.username }
                .singleElement()
                .doOnSuccess { throw ClientError.UsernameUnavailable }
                .ignoreElement()
                .doOnComplete { store.records += Record(userData, password, null) }
    }

    private fun uncertainty(): Single<Unit> {
        return Single.just(Unit)
                .uncertainNoConnectionError()
                .uncertainDelay()
                .uncertainUnknownError()
    }

    private fun <T> Single<T>.uncertainNoConnectionError(): Single<T> {
        val shouldThrow = Math.random() < uncertaintyParams.chanceOfFailingWithNoConnectionError
        return map { if (shouldThrow) throw ClientError.NoConnection else it }
    }

    private fun <T> Single<T>.uncertainUnknownError(): Single<T> {
        val shouldThrow = Math.random() < uncertaintyParams.chanceOfFailingWithUnknownError
        return map { if (shouldThrow) throw ClientError.Unknown else it }
    }

    private fun <T> Single<T>.uncertainDelay(): Single<T> {
        val average = uncertaintyParams.averageResponseDelayInMillis
        val deviation = (Math.random() - 0.5) * uncertaintyParams.responseDelayDeviationInMillis
        val delayAmount = (average + deviation).coerceAtLeast(0.0).toLong()
        return if (delayAmount != 0L) delay(delayAmount, TimeUnit.MILLISECONDS) else this
    }

    /**
     * Composition of a list of [Record] instances that defines a user base
     */
    data class Store(

            /**
             * The store records
             */
            val records: MutableList<Record> = mutableListOf()
    )

    /**
     * A single user record
     */
    data class Record(

            /**
             * The registered user data
             */
            val userData: UserData,

            /**
             * The user password
             */
            val password: String,

            /**
             * A value used to match user tokens or `null` if no tokens should be matched
             */
            var token: String?
    ) {

        /**
         * Return the current [token], if non-null, or mutate this instances by assigning a new one.
         */
        fun getOrCreateToken(): String {
            if (token == null) {
                token = UUID.randomUUID().toString()
            }
            return token!!
        }

    }

    /**
     * Parameters used to emulate uncertain behaviour when interacting with the data source.
     */
    data class UncertaintyParams(

            /**
             * Chance (from 0 to 1) of either [getRemoteToken], [getUserData] or [setUserData] to
             * fail due to no connectivity
             */
            val chanceOfFailingWithNoConnectionError: Float = 0.0f,

            /**
             * Chance (from 0 to 1) of either [getRemoteToken], [getUserData] or [setUserData] to
             * fail due to unknown errors
             */
            val chanceOfFailingWithUnknownError: Float = 0.0f,

            /**
             * Delay in milliseconds that either [getRemoteToken], [getUserData] or [setUserData]
             * can take to complete.
             */
            val averageResponseDelayInMillis: Long = 0,

            /**
             * Max deviation in milliseconds that either [getRemoteToken], [getUserData] or
             * [setUserData] can take to complete.
             */
            val responseDelayDeviationInMillis: Long = 0
    )
}