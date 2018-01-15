/**
 * General utility for functional tests.
 */

package com.gabrielhuff.sample.login.test.util

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.ViewAssertion
import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.action.*
import android.support.test.espresso.matcher.BoundedMatcher
import android.view.View
import android.widget.ProgressBar
import com.gabrielhuff.sample.login.App
import com.gabrielhuff.sample.login.AppComponent
import com.gabrielhuff.sample.login.client.Client
import com.gabrielhuff.sample.login.util.relativeProgress
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

// Espresso actions

/**
 * Swipes from the center to the center left plus some offset
 */
fun swipeLeftOut() = GeneralSwipeAction(
        Swipe.FAST,
        GeneralLocation.CENTER,
        CoordinatesProvider { GeneralLocation.CENTER_LEFT.calculateCoordinates(it).also { it[0] -= 100f } },
        Press.FINGER
)

/**
 * Swipes from the center to the center right plus some offset
 */
fun swipeRightOut() = GeneralSwipeAction(
        Swipe.FAST,
        GeneralLocation.CENTER,
        CoordinatesProvider { GeneralLocation.CENTER_RIGHT.calculateCoordinates(it).also { it[0] += 100f } },
        Press.FINGER
)

// Espresso matchers

/**
 * Return a matcher that matches progress bars with relative progress (from 0 to 1) approximately
 * equal to the input.
 *
 * **Note**: Find a better way (i.e. more functional) to achieve this as we are assuming that the
 * implementation is using a [ProgressBar].
 */
fun withRelativeProgress(relativeProgress: Float) = object : BoundedMatcher<View, ProgressBar>(ProgressBar::class.java) {

    override fun describeTo(description: org.hamcrest.Description) {
        description.appendText("with relative progress: " + relativeProgress)
    }

    override fun matchesSafely(item: ProgressBar): Boolean {
        return abs(item.relativeProgress - relativeProgress) < 0.05
    }
}

// Espresso interactions

/**
 * Block until the given assertion succeeds or throw an exception if the timeout expires
 */
fun ViewInteraction.waitUntil(
        assertion: ViewAssertion,
        timeout: Long = Long.MAX_VALUE,
        timeoutUnit: TimeUnit = TimeUnit.SECONDS) : ViewInteraction {

    val deadline = System.currentTimeMillis() + timeoutUnit.toMillis(timeout)
    while (true) {
        val success = try {
            check(assertion)
            true
        } catch (e: Exception) { false }
        when {
            success -> return this
            System.currentTimeMillis() < deadline -> Thread.sleep(100)
            else -> throw TimeoutException("Failed to assert: $assertion")
        }
    }
}

// Test rules

/**
 * Rule that launches the app by launching its launch intent.
 *
 * We are not using a regular activity test rule here because it only works with explicit intents,
 * and we want the tests to be black boxed, i.e. without any knowledge about the app implementation.
 */
class LaunchAppTestRule : TestRule {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    override fun apply(base: Statement, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                val intent = instrumentation.context.packageManager.getLaunchIntentForPackage(instrumentation.targetContext.packageName)
                val activity = instrumentation.startActivitySync(intent)
                instrumentation.waitForIdleSync()
                try {
                    base.evaluate()
                } finally {
                    activity.finish()
                    instrumentation.waitForIdleSync()
                }
            }
        }
    }
}

/**
 * Rule that provides an unused username to be registered.
 *
 * The current implementation simply generates unique strings across the process lifecycle and
 * assumes that they are unused by the remote service.
 */
class UnusedUsernameTestRule : TestRule {

    private var evaluating = AtomicBoolean(false)

    val username: String get() = if (evaluating.get()) {
        "testuser${count.get()}"
    } else {
        throw IllegalStateException("Username not available yet")
    }

    override fun apply(base: Statement, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                count.incrementAndGet()
                evaluating.set(true)
                try { base.evaluate() }
                finally { evaluating.set(false) }
            }
        }
    }

    private companion object {
        val count = AtomicInteger(0)
    }
}

/**
 * Rule that mocks the app [Client] dependency before each test execution.
 */
class MockedClientTestRule(private val client: Client) : TestRule {

    override fun apply(base: Statement, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                val app = InstrumentationRegistry.getTargetContext().applicationContext as App
                app.component = object : AppComponent { override fun client(): Client = client }
                base.evaluate()
            }
        }
    }
}