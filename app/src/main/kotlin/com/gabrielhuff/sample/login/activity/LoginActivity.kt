package com.gabrielhuff.sample.login.activity

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import com.gabrielhuff.sample.login.R
import com.gabrielhuff.sample.login.activity.base.BaseActivity
import com.gabrielhuff.sample.login.client.base.ClientError
import com.gabrielhuff.sample.login.client.base.ClientState
import com.gabrielhuff.sample.login.util.dismissKeyboard
import com.gabrielhuff.sample.login.util.showSimpleDialog
import com.gabrielhuff.sample.login.util.startActivity
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.enabled
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.editorActions
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import kotlinx.android.synthetic.main.login.*

class LoginActivity : BaseActivity() {

    override fun onCreate(state: Bundle?, autoDispose: Disposable.() -> Unit) {
        // Set view
        setContentView(R.layout.login)

        // When sign up button is clicked, open sign up screen
        signup.clicks().subscribe { startActivity<SignUpActivity>(finish = false) }.autoDispose()

        // When input combination updates...
        Observables.combineLatest(username.textChanges(), password.textChanges())

                //...take validity...
                .map { inputsAreValid() }

                //...enable log in button if inputs are valid or disable it otherwise
                .subscribe(login.enabled()).autoDispose()

        // When either password input is provided or login button clicks happen...
        password.editorActions().filter { it == EditorInfo.IME_ACTION_DONE }.map { Unit }
                .mergeWith(login.clicks()).publish().apply {

            // ...hide keyboard
            subscribe { dismissKeyboard() }.autoDispose()


            // ...when inputs are valid...
            filter { inputsAreValid() }

                    // ...login
                    .subscribe { client.login(username.text.toString(), password.text.toString()) }.autoDispose()

        }.connect(autoDispose)

        // When back button is clicked, check if client is logging in...
        onBackPressed.map { client.state == ClientState.LOGGING_IN }.publish().apply {

            // ...if yes, logout
            filter { it }.subscribe { client.logout() }.autoDispose()

            // ...if not, navigate back
            filter { !it }.subscribe { navigateBack() }.autoDispose()

        }.connect(autoDispose)
    }

    override fun onResume(autoDispose: Disposable.() -> Unit) {
        // When client state updates...
        client.stateStream.publish().apply {

            // ...show inputs if logged out or hide them otherwise
            map { it == ClientState.LOGGED_OUT }.subscribe(inputs.visibility()).autoDispose()

            // ...show progress if logging in or hide it otherwise
            map { it == ClientState.LOGGING_IN }.subscribe(progress.visibility()).autoDispose()

            // ...to logged in...
            filter { it == ClientState.LOGGED_IN }

                    // ...open logged in screen
                    .subscribe { startActivity<LoggedInActivity>() }.autoDispose()

            // ...to signing up...
            filter { it == ClientState.SIGNING_UP }

                    // ...open sign up screen
                    .subscribe { startActivity<SignUpActivity>(finish = false) }.autoDispose()

        }.connect(autoDispose)

        // When log in fails...
        client.loginFailedEvents.publish().apply {

            // ...show login error dialog
            map { it.error }.subscribe { showLoginErrorDialog(it) }.autoDispose()

            // ...set username input
            map { it.username }.subscribe(username.text()).autoDispose()

            // ...set password input
            map { it.password }.subscribe(password.text()).autoDispose()

        }.connect(autoDispose)
    }

    private fun inputsAreValid() = username.text.isNotBlank() && password.text.isNotBlank()

    private fun showLoginErrorDialog(error: ClientError) {
        // Create dialog depending on error, then show it
        when(error) {
            ClientError.NoConnection -> showSimpleDialog(
                    title = getString(R.string.Error),
                    description = getString(R.string.Theres_no_active_network_connection),
                    button = getString(android.R.string.ok)
            )
            ClientError.Unauthorized -> showSimpleDialog(
                    title = getString(R.string.Error),
                    description = getString(R.string.Invalid_username_and_or_password),
                    button = getString(android.R.string.ok)
            )
            else -> showSimpleDialog(
                    title = getString(R.string.Error),
                    description = getString(R.string.There_was_a_problem_when_logging_in_Try_again_in_a_few_seconds),
                    button = getString(android.R.string.ok)
            )
        }
    }
}