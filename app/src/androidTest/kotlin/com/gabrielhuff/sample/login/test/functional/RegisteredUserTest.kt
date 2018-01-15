package com.gabrielhuff.sample.login.test.functional

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.widget.ProgressBar
import android.widget.SeekBar
import com.gabrielhuff.sample.login.test.util.*
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Black box functional tests that share the following assumptions:
 *
 * - When the test starts, the Login screen is being shown
 * - The current [UnusedUsernameTestRule.username] **is** registered on the remote service with the
 *   "passwd" password and the following skills:
 *   - **RxJava**: 0.0
 *   - **Docker**: 0.5
 *   - **Kotlin**: 1.0
 * - During their execution, there is no other interactions with the remote service
 */
class RegisteredUserTest {

    @get:Rule
    val appRule = LaunchAppTestRule()

    @get:Rule
    val usernameRule = UnusedUsernameTestRule()

    @Before
    fun setup() {

        // Open sign up screen
        onView(withText("Sign up")).perform(click())

        // Set RxJava skill to 0.0, Docker to 0.5 (default) and Kotlin to 1.0
        onView(allOf(hasSibling(withText("RxJava")), isAssignableFrom(SeekBar::class.java))).perform(swipeLeftOut())
        onView(allOf(hasSibling(withText("Kotlin")), isAssignableFrom(SeekBar::class.java))).perform(swipeRightOut())

        // Fill form with username, password and password confirmation
        onView(withHint("Username")).perform(typeText(usernameRule.username)).perform(closeSoftKeyboard())
        onView(withHint("Password")).perform(typeText("passwd")).perform(closeSoftKeyboard())
        onView(withHint("Confirm password")).perform(typeText("passwd")).perform(closeSoftKeyboard())

        // Sign up
        onView(allOf(withText("Sign up"), isClickable())).check(matches(isEnabled())).perform(click())

        // Wait until logged in screen is shown
        onView(withText("Profile")).waitUntil(matches(isDisplayed()), 15, TimeUnit.SECONDS)

        // Log out and go to login screen
        onView(withText("Log out")).perform(click())
    }

    @Test
    fun loginWithRegisteredUser() {

        // Fill form with username and password
        onView(withHint("Username")).perform(typeText(usernameRule.username)).perform(closeSoftKeyboard())
        onView(withHint("Password")).perform(typeText("passwd")).perform(closeSoftKeyboard())

        // Login
        onView(withText("Login")).perform(click())

        // Wait until logged in screen is shown
        onView(withText("Profile")).waitUntil(matches(isDisplayed()), 15, TimeUnit.SECONDS)

        // Check profile username is displayed, and RxJava, Docker and Kotlin skills are 0, 0.5 and 1
        onView(withText(usernameRule.username)).check(matches(isDisplayed()))
        onView(allOf(hasSibling(withText("RxJava")), isAssignableFrom(ProgressBar::class.java))).check(matches(withRelativeProgress(0.0f)))
        onView(allOf(hasSibling(withText("Docker")), isAssignableFrom(ProgressBar::class.java))).check(matches(withRelativeProgress(0.5f)))
        onView(allOf(hasSibling(withText("Kotlin")), isAssignableFrom(ProgressBar::class.java))).check(matches(withRelativeProgress(1.0f)))

        // Log out
        onView(withText("Log out")).perform(click())
    }

    @Test
    fun signUpWithRegisteredUserFails() {

        // Open sign up screen
        onView(withText("Sign up")).perform(click())

        // Fill form with username, password and password confirmation
        onView(withHint("Username")).perform(typeText(usernameRule.username)).perform(closeSoftKeyboard())
        onView(withHint("Password")).perform(typeText("passwd")).perform(closeSoftKeyboard())
        onView(withHint("Confirm password")).perform(typeText("passwd")).perform(closeSoftKeyboard())

        // Try to sign up
        onView(allOf(withText("Sign up"), isClickable())).check(matches(isEnabled())).perform(click())

        // Wait until error dialog is displayed
        onView(withText("Error")).waitUntil(matches(isDisplayed()), 15, TimeUnit.SECONDS)

        // Assert error message
        onView(withText("This username is not available")).check(matches(isDisplayed()))

        // Dismiss dialog by clicking its ok button
        onView(withText("OK")).perform(click())

        // Assert dialog was dismissed
        onView(withText("Error")).check(doesNotExist())
    }
}