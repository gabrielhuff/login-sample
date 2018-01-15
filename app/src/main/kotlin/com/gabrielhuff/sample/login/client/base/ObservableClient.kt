package com.gabrielhuff.sample.login.client.base

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

/**
 * An [AbstractClient] that handles all the observation logic, leaving the responsibility of
 * handling state to its subclasses
 */
abstract class ObservableClient : AbstractClient() {

    // Overridden - Querying and observing

    override val state: ClientState get() = stateDataSubject.value.state

    override val userData: UserData? get() = stateData.userData

    override val stateStream: Observable<ClientState>
        get() = stateDataSubject.map { it.state }
                .distinctUntilChanged()

    override val loginFailedEvents: Observable<LoginFailedEvent> get() = loginFailedEventsSubject

    override val signUpFailedEvents: Observable<SignUpFailedEvent> get() = signUpFailedEventsSubject

    // Protected API - Changing state and emitting events

    protected var stateData: StateData
        get() = stateDataSubject.value
        set(value) = stateDataSubject.onNext(value)

    protected fun emitLoginFailedEvent(username: String, password: String, error: ClientError) = loginFailedEventsSubject.onNext(LoginFailedEvent(username, password, error))

    protected fun emitSignUpFailedEvent(userData: UserData, password: String, error: ClientError) = signUpFailedEventsSubject.onNext(SignUpFailedEvent(userData, password, error))

    protected data class StateData(
            val loginDisposable: Disposable? = null,
            val signUpDisposable: Disposable? = null,
            val userData: UserData? = null
    ) {
        val state: ClientState
            get() = when {
                loginDisposable != null -> ClientState.LOGGING_IN
                signUpDisposable != null -> ClientState.SIGNING_UP
                userData != null -> ClientState.LOGGED_IN
                else -> ClientState.LOGGED_OUT
            }
    }

    // Private API - State and event observation sources

    private val stateDataSubject = BehaviorSubject.createDefault<StateData>(StateData())

    private val loginFailedEventsSubject = PublishSubject.create<LoginFailedEvent>()

    private val signUpFailedEventsSubject = PublishSubject.create<SignUpFailedEvent>()
}