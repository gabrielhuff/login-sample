package com.gabrielhuff.sample.login.activity

import android.os.Bundle
import com.gabrielhuff.sample.login.R
import com.gabrielhuff.sample.login.activity.base.BaseActivity
import com.gabrielhuff.sample.login.client.base.ClientState
import com.gabrielhuff.sample.login.util.relativeProgress
import com.gabrielhuff.sample.login.util.startActivity
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.logged_in.*
import kotlinx.android.synthetic.main.logged_in_skill.view.*

class LoggedInActivity : BaseActivity() {

    override fun onCreate(state: Bundle?, autoDispose: Disposable.() -> Unit) {
        // Set view
        setContentView(R.layout.logged_in)

        // Set flag names
        skillRxJava.name.text = getString(R.string.RxJava)
        skillDocker.name.text = getString(R.string.Docker)
        skillKotlin.name.text = getString(R.string.Kotlin)

        // Navigate back when back button is pressed
        onBackPressed.subscribe { navigateBack() }.autoDispose()
    }

    override fun onResume(autoDispose: Disposable.() -> Unit) {
        // Fill UI
        client.userData?.let {

            username.text = it.username

            skillRxJava.amount.relativeProgress = it.skillRxJava
            skillDocker.amount.relativeProgress = it.skillDocker
            skillKotlin.amount.relativeProgress = it.skillKotlin
        }


        // When client state updates to anything different than logged in...
        client.stateStream.filter { it != ClientState.LOGGED_IN }

                // ...open login
                .subscribe { startActivity<LoginActivity>() }.autoDispose()

        // When log out button is clicked, log out
        logout.clicks().subscribe { client.logout() }.autoDispose()
    }
}