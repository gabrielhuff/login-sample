package com.gabrielhuff.sample.login.test.functional

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.widget.ProgressBar
import android.widget.SeekBar
import com.gabrielhuff.sample.login.test.util.*
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Black box functional tests that share the following assumptions:
 *
 * - When the test starts, the Login screen is being shown
 * - The current [UnusedUsernameTestRule.username] **is not** registered on the remote service
 * - During their execution, there is no other interactions with the remote service
 */
class UnregisteredUserTest {

    @get:Rule
    val appRule = LaunchAppTestRule()

    @get:Rule
    val usernameRule = UnusedUsernameTestRule()

    @Test
    fun signUpAndLoginWithUnregisteredUser() {

        // Open sign up screen
        onView(withText("Sign up")).perform(click())

        // Set RxJava skill to 1.0, Docker to 0.5 (default) and Kotlin to 0.0
        onView(allOf(hasSibling(withText("RxJava")), isAssignableFrom(SeekBar::class.java))).perform(swipeRightOut())
        onView(allOf(hasSibling(withText("Kotlin")), isAssignableFrom(SeekBar::class.java))).perform(swipeLeftOut())

        // Fill form with username, password and password confirmation
        onView(withHint("Username")).perform(typeText(usernameRule.username)).perform(closeSoftKeyboard())
        onView(withHint("Password")).perform(typeText("passwd")).perform(closeSoftKeyboard())
        onView(withHint("Confirm password")).perform(typeText("passwd")).perform(closeSoftKeyboard())

        // Sign up
        onView(allOf(withText("Sign up"), isClickable())).check(matches(isEnabled())).perform(click())

        // Wait until logged in screen is shown
        onView(withText("Profile")).waitUntil(matches(isDisplayed()), 15, TimeUnit.SECONDS)

        // Check profile username is displayed, and RxJava, Docker and Kotlin skills are 1, 0.5 and 0
        onView(withText(usernameRule.username)).check(matches(isDisplayed()))
        onView(allOf(hasSibling(withText("RxJava")), isAssignableFrom(ProgressBar::class.java))).check(matches(withRelativeProgress(1.0f)))
        onView(allOf(hasSibling(withText("Docker")), isAssignableFrom(ProgressBar::class.java))).check(matches(withRelativeProgress(0.5f)))
        onView(allOf(hasSibling(withText("Kotlin")), isAssignableFrom(ProgressBar::class.java))).check(matches(withRelativeProgress(0.0f)))

        // Log out
        onView(withText("Log out")).perform(click())

        // Assert login screen is displayed
        onView(withText("Login")).check(matches(isDisplayed()))
    }

    @Test
    fun loginWithUnregisteredUserFails() {

        // Fill form with username and password
        onView(withHint("Username")).perform(typeText(usernameRule.username)).perform(closeSoftKeyboard())
        onView(withHint("Password")).perform(typeText("passwd")).perform(closeSoftKeyboard())

        // Try to login
        onView(withText("Login")).perform(click())

        // Wait until error dialog is displayed
        onView(withText("Error")).waitUntil(matches(isDisplayed()), 15, TimeUnit.SECONDS)

        // Assert error message
        onView(withText("Invalid username and / or password")).check(matches(isDisplayed()))

        // Dismiss dialog by clicking its ok button
        onView(withText("OK")).perform(click())

        // Assert dialog was dismissed
        onView(withText("Error")).check(doesNotExist())
    }

    @Test
    fun signUpWithoutAllParametersFails() {

        // Open sign up screen
        onView(withText("Sign up")).perform(click())

        // Assert can't sign up yet as form is not filled
        onView(allOf(withText("Sign up"), isClickable())).check(matches(not(isEnabled())))

        // Fill username
        onView(withHint("Username")).perform(typeText(usernameRule.username)).perform(closeSoftKeyboard())

        // Assert can't sign up yet as passwords are missing
        onView(allOf(withText("Sign up"), isClickable())).check(matches(not(isEnabled())))

        // Fill password and mismatched password confirmation
        onView(withHint("Password")).perform(typeText("passwd")).perform(closeSoftKeyboard())
        onView(withHint("Confirm password")).perform(typeText("wrongpasswd")).perform(closeSoftKeyboard())

        // Assert can't sign up yet as passwords don't match
        onView(allOf(withText("Sign up"), isClickable())).check(matches(not(isEnabled())))

        // Rewrite password confirmation to match actual password
        onView(withHint("Confirm password")).perform(clearText()).perform(typeText("passwd")).perform(closeSoftKeyboard())

        // Assert can sign up
        onView(allOf(withText("Sign up"), isClickable())).check(matches(isEnabled()))

        // Clear user name
        onView(withHint("Username")).perform(clearText()).perform(closeSoftKeyboard())

        // Assert can't sign up yet as the username is not there anymore
        onView(allOf(withText("Sign up"), isClickable())).check(matches(not(isEnabled())))
    }

    @Test
    fun signUpWithMismatchedPasswordsFails() {

        // Open sign up screen
        onView(withText("Sign up")).perform(click())

        // Fill form with username, password and mismatched password confirmation
        onView(withHint("Password")).perform(typeText("passwd")).perform(closeSoftKeyboard())
        onView(withHint("Confirm password")).perform(typeText("wrongpasswd")).perform(closeSoftKeyboard())

        // Assert mismatch error message is displayed
        onView(withHint("Confirm password")).check(matches(hasErrorText("Mismatch")))

        // Rewrite password confirmation to match actual password
        onView(withHint("Confirm password")).perform(clearText()).perform(typeText("passwd")).perform(closeSoftKeyboard())

        // Assert mismatch error message is not displayed
        onView(withHint("Confirm password")).check(matches(hasErrorText(nullValue(String::class.java))))
    }

    @Test
    fun navigateBackFromSignUpToLogin() {

        // Open sign up screen
        onView(withText("Sign up")).perform(click())

        // Navigate up
        Espresso.pressBack()

        // Assert login screen is displayed
        onView(withText("Login")).check(matches(isDisplayed()))
    }

    @Test
    fun navigateUpFromSignUpToLogin() {

        // Open sign up screen
        onView(withText("Sign up")).perform(click())

        // Navigate up
        onView(withContentDescription("Close")).perform(click())

        // Assert login screen is displayed
        onView(withText("Login")).check(matches(isDisplayed()))
    }
}