package com.gabrielhuff.sample.login.client

import android.support.annotation.MainThread
import com.gabrielhuff.sample.login.client.base.*
import com.gabrielhuff.sample.login.dao.LocalTokenDAO
import com.gabrielhuff.sample.login.dao.UserDataDAO
import com.gabrielhuff.sample.login.util.subscribeBy
import com.gabrielhuff.sample.login.client.base.ClientError
import com.gabrielhuff.sample.login.client.base.ObservableClient
import com.gabrielhuff.sample.login.client.base.UserData
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * A client to be used in order to interact with the service.
 *
 * Callers can do pretty much 2 things with a [Client] instance:
 *
 * - **Sensing**: Querying / Observing its state by accessing its properties
 * - **Actuating**: Issuing commands by calling its methods (which often leads to state changes)
 *
 * A more specific documentation can be found on [AbstractClient]
 *
 * As the implementation is not simple, we are splitting it in 3 different classes:
 *
 * - [AbstractClient] - Defines the public API
 * - [ObservableClient] - Handles observation
 * - [Client] - Manages state
 *
 * **Note**: We are not handling thread safety on the current implementation for simplicity, so
 * make sure to call its actuation methods ([login], [signUp] and [logout]) on the main thread
 */
class Client(

        private val localTokenDAO: LocalTokenDAO,

        private val userDataDAO: UserDataDAO

) : ObservableClient() {

    private val communicationTimeoutSeconds: Long = 10

    init {
        // If there already is a local token
        localTokenDAO.localToken?.let { token ->

            // Clear it as we only want to have one when logging in
            localTokenDAO.localToken = null

            // Start logging in...
            userDataDAO.getUserData(token)
                    .timeout(communicationTimeoutSeconds, TimeUnit.SECONDS)
                    .onErrorResumeNext { Single.error(it as? ClientError ?: ClientError.Unknown) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                            // ...on start, update state
                            onSubscribe = { stateData = StateData(loginDisposable = it) },

                            // ...if success...
                            onSuccess = {
                                // ...update state
                                stateData = StateData(userData = it)

                                // ...set local token
                                localTokenDAO.localToken = token
                            },

                            // ...if error, update state
                            onError = {
                                stateData = StateData()
                            }
                    )
        }
    }

    @MainThread
    override fun login(username: String, password: String) {
        // Cancel tasks
        stateData.apply {
            loginDisposable?.dispose()
            signUpDisposable?.dispose()
        }

        // Start logging in...
        userDataDAO.getRemoteToken(username, password)
                .flatMap { token -> userDataDAO.getUserData(token).map { it to token } }
                .timeout(communicationTimeoutSeconds, TimeUnit.SECONDS)
                .onErrorResumeNext { Single.error(it as? ClientError ?: ClientError.Unknown) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        // ...on start, update state
                        onSubscribe = { stateData = StateData(loginDisposable = it) },

                        // ...if success...
                        onSuccess = { (userData, token) ->
                            // ...update state
                            stateData = StateData(userData = userData)

                            // ...set local token
                            localTokenDAO.localToken = token
                        },

                        // ...if error...
                        onError = {
                            // ...update state
                            stateData = StateData()

                            // ...emit login failed event
                            emitLoginFailedEvent(username, password, it as ClientError)
                        }
                )
    }

    @MainThread
    override fun signUp(userData: UserData, password: String) {
        // Cancel tasks
        stateData.apply {
            loginDisposable?.dispose()
            signUpDisposable?.dispose()
        }

        // Start signing up...
        userDataDAO.setUserData(userData, password)
                .timeout(communicationTimeoutSeconds, TimeUnit.SECONDS)
                .onErrorResumeNext { Completable.error(it as? ClientError ?: ClientError.Unknown) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        // ...on start, update state
                        onSubscribe = { stateData = StateData(signUpDisposable = it) },

                        // ...if success, login
                        onComplete = {
                            login(userData.username, password)
                        },

                        // ...if error...
                        onError = {
                            // ...update state
                            stateData = StateData()

                            // ...emit event
                            emitSignUpFailedEvent(userData, password, it as ClientError)
                        }
                )
    }

    @MainThread
    override fun logout() {
        // Cancel tasks
        stateData.apply {
            loginDisposable?.dispose()
            signUpDisposable?.dispose()
        }

        // Update state
        stateData = StateData()

        // Clear local token
        localTokenDAO.localToken = null
    }

}