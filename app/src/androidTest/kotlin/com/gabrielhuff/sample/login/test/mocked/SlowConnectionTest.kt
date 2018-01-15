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
 * - The connection between the client and the remote service is extremely slow, taking a between 20
 *   to 25 seconds to respond each request.
 *
 * This is not a black box test as we are mocking the app dependencies, since there's currently no
 * (reliable) way to emulate a slow connection.
 */
class SlowConnectionTest {

    @get:Rule
    val appRule = LaunchAppTestRule()

    /**
     * Inject a mocked [Client] dependency on the app that always takes 20~25 seconds to respond
     */
    @get:Rule
    val slowConnectionClientRule = MockedClientTestRule(

            client = Client(

                    localTokenDAO = MemoryLocalTokenDAO(),

                    userDataDAO = MemoryUserDataDAO(

                            uncertaintyParams = MemoryUserDataDAO.UncertaintyParams(

                                    averageResponseDelayInMillis = 22_500,

                                    responseDelayDeviationInMillis = 2_500
                            )
                    )
            )
    )

    @Test
    fun loginWithSlowConnectionTimesOut() {

        // Fill form with username and password
        onView(withHint("Username")).perform(typeText("user")).perform(closeSoftKeyboard())
        onView(withHint("Password")).perform(typeText("passwd")).perform(closeSoftKeyboard())

        // Try to login
        onView(withText("Login")).perform(click())

        // Wait until error dialog is displayed
        onView(withText("Error")).waitUntil(matches(isDisplayed()), 15, TimeUnit.SECONDS)

        // Assert error message
        onView(withText("There was a problem when logging in. Try again in a few seconds.")).check(matches(isDisplayed()))

        // Dismiss dialog by clicking its ok button
        onView(withText("OK")).perform(click())

        // Assert dialog was dismissed
        onView(withText("Error")).check(doesNotExist())
    }

    @Test
    fun signUpWithSlowConnectionTimesOut() {
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
        onView(withText("There was a problem when signing up. Try again in a few seconds.")).check(matches(isDisplayed()))

        // Dismiss dialog by clicking its ok button
        onView(withText("OK")).perform(click())

        // Assert dialog was dismissed
        onView(withText("Error")).check(doesNotExist())
    }
}