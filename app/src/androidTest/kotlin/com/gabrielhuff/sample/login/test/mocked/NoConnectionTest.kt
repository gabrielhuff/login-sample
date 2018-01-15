package com.gabrielhuff.sample.login.test.mocked

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import com.gabrielhuff.sample.login.client.Client
import com.gabrielhuff.sample.login.dao.memory.MemoryLocalTokenDAO
import com.gabrielhuff.sample.login.dao.memory.MemoryUserDataDAO
import com.gabrielhuff.sample.login.test.util.LaunchAppTestRule
import com.gabrielhuff.sample.login.test.util.MockedClientTestRule
import com.gabrielhuff.sample.login.test.util.waitUntil
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Tests that share the following assumptions:
 *
 * - When the test starts, the Login screen is being shown
 * - There's no connection between the client and the remote service.
 *
 * This is not a black box test as we are mocking the app dependencies, since there's currently no
 * (reliable) way to emulate a no connection state.
 */
class NoConnectionTest {

    @get:Rule
    val appRule = LaunchAppTestRule()

    /**
     * Inject a mocked [Client] dependency on the app that always fails to communicate with the
     * remote service due to missing connection
     */
    @get:Rule
    val noConnectionClientRule = MockedClientTestRule(

            client = Client(

                    localTokenDAO = MemoryLocalTokenDAO(),

                    userDataDAO = MemoryUserDataDAO(

                            uncertaintyParams = MemoryUserDataDAO.UncertaintyParams(

                                    chanceOfFailingWithNoConnectionError = 1.0f
                            )
                    )
            )
    )

    @Test
    fun loginWithNoConnectionFails() {

        // Fill form with username and password
        onView(withHint("Username")).perform(typeText("user")).perform(closeSoftKeyboard())
        onView(withHint("Password")).perform(typeText("passwd")).perform(closeSoftKeyboard())

        // Try to login
        onView(withText("Login")).perform(click())

        // Wait until error dialog is displayed
        onView(withText("Error")).waitUntil(matches(isDisplayed()), 15, TimeUnit.SECONDS)

        // Assert error message
        onView(withText("There's no active network connection")).check(matches(isDisplayed()))

        // Dismiss dialog by clicking its ok button
        onView(withText("OK")).perform(click())

        // Assert dialog was dismissed
        onView(withText("Error")).check(doesNotExist())
    }

    @Test
    fun signUpWithNoConnectionFails() {
        // Open sign up screen
        onView(withText("Sign up")).perform(click())

        // Fill form with username, password and password confirmation
        onView(withHint("Username")).perform(typeText("user")).perform(closeSoftKeyboard())
        onView(withHint("Password")).perform(typeText("passwd")).perform(closeSoftKeyboard())
        onView(withHint("Confirm password")).perform(typeText("passwd")).perform(closeSoftKeyboard())

        // Try to sign up
        onView(allOf(withText("Sign up"), isClickable())).check(matches(isEnabled())).perform(click())

        // Wait until error dialog is displayed
        onView(withText("Error")).waitUntil(matches(isDisplayed()), 15, TimeUnit.SECONDS)

        // Assert error message
        onView(withText("There's no active network connection")).check(matches(isDisplayed()))

        // Dismiss dialog by clicking its ok button
        onView(withText("OK")).perform(click())

        // Assert dialog was dismissed
        onView(withText("Error")).check(doesNotExist())
    }
}