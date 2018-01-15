package com.gabrielhuff.sample.login.dao

import com.gabrielhuff.sample.login.client.base.ClientError
import com.gabrielhuff.sample.login.client.base.UserData
import io.reactivex.Completable
import io.reactivex.Single

/**
 * Object capable of reading and writing user related information from a remote data source
 */
interface UserDataDAO {

    /**
     * Get an access token using the input credentials. Can fail with:
     *
     * - [ClientError.NoConnection] - If there's no connection to the service
     * - [ClientError.Unauthorized] - If credentials are invalid
     * - [ClientError.Unknown] - If there's any other communication error
     */
    fun getRemoteToken(username: String, password: String): Single<String>

    /**
     * Get user data using an access token. Can fail with:
     *
     * - [ClientError.NoConnection] - If there's no connection to the service
     * - [ClientError.Unauthorized] - If token is invalid (can also be expired)
     * - [ClientError.Unknown] - If there's any other communication error
     */
    fun getUserData(token: String): Single<UserData>

    /**
     * Registers the input user data using the input password. Can fail with:
     *
     * - [ClientError.NoConnection] - If there's no connection to the service
     * - [ClientError.UsernameUnavailable] - If the input username is already used
     * - [ClientError.Unknown] - If there's any other communication error
     */
    fun setUserData(userData: UserData, password: String): Completable
}