package com.gabrielhuff.sample.login.activity

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import com.gabrielhuff.sample.login.R
import com.gabrielhuff.sample.login.activity.base.BaseActivity
import com.gabrielhuff.sample.login.client.base.ClientError
import com.gabrielhuff.sample.login.client.base.ClientState
import com.gabrielhuff.sample.login.client.base.UserData
import com.gabrielhuff.sample.login.util.dismissKeyboard
import com.gabrielhuff.sample.login.util.navigateUp
import com.gabrielhuff.sample.login.util.relativeProgress
import com.gabrielhuff.sample.login.util.showSimpleDialog
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.enabled
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.editorActions
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import kotlinx.android.synthetic.main.sign_up.*
import kotlinx.android.synthetic.main.sign_up_skill.view.*

class SignUpActivity : BaseActivity() {

    override fun onCreate(state: Bundle?, autoDispose: Disposable.() -> Unit) {
        // Set view
        setContentView(R.layout.sign_up)

        // Set flag names
        skillRxJava.name.text = getString(R.string.RxJava)
        skillDocker.name.text = getString(R.string.Docker)
        skillKotlin.name.text = getString(R.string.Kotlin)

        // When navigation button is clicked...
        close.clicks().share().publish().apply {

            // ...when client state is logged out navigate up
            filter { client.state == ClientState.LOGGED_OUT }.subscribe { navigateUp() }.autoDispose()

            // ...logout
            subscribe { client.logout() }.autoDispose()

        }.connect(autoDispose)

        // When input combination updates...
        Observables.combineLatest(
                username.textChanges().map { it.toString() },
                password.textChanges().map { it.toString() },
                password2.textChanges().map { it.toString() }
        ).publish().apply {


            // ...determine if password mismatch error should be shown...
            map { (_, p, p2) -> p2.isNotEmpty() && p != p2 }.publish().apply {

                // ...show password mismatch error if it's necessary
                filter { it }.subscribe { password2.error = getString(R.string.Mismatch) }.autoDispose()

                // ...clear password mismatch error if it's not necessary
                filter { !it }.subscribe { password2.error = null }.autoDispose()
            }.connect(autoDispose)

            //...take input validity...
            map { inputsAreValid() }

                    //...enable sign up button if inputs are valid or disable it otherwise
                    .subscribe(signup.enabled()).autoDispose()

        }.connect(autoDispose)

        // When either password input is provided or sign up button clicks happen...
        password2.editorActions().filter { it == EditorInfo.IME_ACTION_DONE }.map { Unit }
                .mergeWith(signup.clicks()).publish().apply {

            // ...hide keyboard
            subscribe { dismissKeyboard() }.autoDispose()

            // ...when inputs are valid...
            filter { inputsAreValid() }

                    // ...sign up
                    .subscribe { client.signUp(getUserDataInput(), password.text.toString()) }.autoDispose()

        }.connect(autoDispose)

        // When back button is clicked, check if client is signing up...
        onBackPressed.map { client.state == ClientState.SIGNING_UP }.publish().apply {

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

            // ...to either signing up show progress or hide it otherwise
            map { it == ClientState.SIGNING_UP }.subscribe(progress.visibility()).autoDispose()

            // ...to logged in or logging in...
            filter { it == ClientState.LOGGED_IN || it == ClientState.LOGGING_IN }

                    // ...navigate up
                    .subscribe { navigateUp() }.autoDispose()

        }.connect(autoDispose)

        // When sign up fails...
        client.signUpFailedEvents.publish().apply {

            // ...show sign up error dialog
            map { it.error }.subscribe { showSignUpErrorDialog(it) }.autoDispose()

            // ...set skill inputs
            map { it.userData.skillRxJava }.subscribe(skillRxJava.amount.relativeProgress()).autoDispose()
            map { it.userData.skillDocker }.subscribe(skillDocker.amount.relativeProgress()).autoDispose()
            map { it.userData.skillKotlin }.subscribe(skillKotlin.amount.relativeProgress()).autoDispose()

            // ...set username input
            map { it.userData.username }.subscribe(username.text()).autoDispose()

            // ...set password inputs
            map { it.password }.publish().apply {

                subscribe(password.text()).autoDispose()

                subscribe(password2.text()).autoDispose()

            }.connect(autoDispose)

        }.connect(autoDispose)
    }

    private fun inputsAreValid(): Boolean {
        // Return true if username is not blank...
        return username.text.isNotBlank() &&

                // ...password is not blank...
                password.text.isNotBlank() &&

                // ...password confirmation is not blank...
                password2.text.isNotBlank() &&

                // ...and passwords match
                password.text.toString() == password2.text.toString()
    }

    private fun showSignUpErrorDialog(error: ClientError) {
        // Create dialog depending on error, then show it
        when(error) {
            ClientError.NoConnection -> showSimpleDialog(
                    title = getString(R.string.Error),
                    description = getString(R.string.Theres_no_active_network_connection),
                    button = getString(android.R.string.ok)
            )
            ClientError.UsernameUnavailable -> showSimpleDialog(
                    title = getString(R.string.Error),
                    description = getString(R.string.This_username_is_not_available),
                    button = getString(android.R.string.ok)
            )
            else -> showSimpleDialog(
                    title = getString(R.string.Error),
                    description = getString(R.string.There_was_a_problem_when_signing_up_Try_again_in_a_few_seconds),
                    button = getString(android.R.string.ok)
            )
        }
    }

    private fun getUserDataInput() = UserData(
            username = username.text.toString(),
            skillRxJava = skillRxJava.amount.relativeProgress,
            skillDocker = skillDocker.amount.relativeProgress,
            skillKotlin = skillKotlin.amount.relativeProgress
    )
}