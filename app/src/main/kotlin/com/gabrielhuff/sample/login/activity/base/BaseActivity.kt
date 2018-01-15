package com.gabrielhuff.sample.login.activity.base

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.gabrielhuff.sample.login.App
import com.gabrielhuff.sample.login.client.Client
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

/**
 * Base activity that provides common utility for its subclasses. The following features a
 * available:
 *
 * - Allow subclasses to schedule [Disposable] instances for disposal without the need to hold
 * references to them. This can be done by overriding [onCreate] and/or [onResume] and calling the
 * `autoDispose` function. Any receiver [Disposable] will be disposed during [onDestroy] and
 * [onPause] respectively. Note that calling the superclass implementation is not necessary for
 * any of these methods.
 *
 * - Allow subclasses to react to back press events by subscribing to the [onBackPressed]
 * observable. Also, [navigateBack] can be called to navigate back by triggering a back press.
 *
 * - Subclasses have common dependencies inject during [onCreate].
 */
abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var client: Client private set

    private lateinit var onPauseDisposables: CompositeDisposable

    private lateinit var onDestroyDisposables: CompositeDisposable

    private val onBackPressedSubject = PublishSubject.create<Unit>()

    protected val onBackPressed: Observable<Unit> get() = onBackPressedSubject

    protected fun navigateBack() = super.onBackPressed()

    protected open fun onCreate(state: Bundle?, autoDispose: Disposable.() -> Unit) {  }

    protected open fun onResume(autoDispose: Disposable.() -> Unit) {  }

    final override fun onCreate(state: Bundle?) {
        // Call superclass method
        super.onCreate(state)

        // Inject dependencies
        client = (application as App).component.client()

        // Initialise composite disposable
        onDestroyDisposables = CompositeDisposable()

        // Call auto disposable implementation
        onCreate(state) { onDestroyDisposables.add(this) }
    }

    final override fun onResume() {
        // Call superclass method
        super.onResume()

        // Initialise composite disposable
        onPauseDisposables = CompositeDisposable()

        // Call auto disposable implementation
        onResume { onPauseDisposables.add(this) }
    }

    override fun onPause() {
        // Call superclass method
        super.onPause()

        // Dispose
        onPauseDisposables.dispose()
    }

    override fun onDestroy() {
        // Call superclass method
        super.onDestroy()

        // Dispose
        onDestroyDisposables.dispose()
    }

    final override fun onBackPressed() { onBackPressedSubject.onNext(Unit) }
}