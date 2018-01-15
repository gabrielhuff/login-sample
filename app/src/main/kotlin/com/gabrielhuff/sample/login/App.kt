package com.gabrielhuff.sample.login

import android.app.Application
import android.support.annotation.VisibleForTesting
import com.gabrielhuff.sample.login.client.Client
import com.gabrielhuff.sample.login.dao.LocalTokenDAO
import com.gabrielhuff.sample.login.dao.UserDataDAO
import com.gabrielhuff.sample.login.dao.memory.MemoryLocalTokenDAO
import com.gabrielhuff.sample.login.dao.memory.MemoryUserDataDAO
import com.gabrielhuff.sample.login.dao.network.NetworkUserDataDAO
import com.gabrielhuff.sample.login.dao.prefs.SharedPrefsLocalTokenDAO
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Application class that provides a [component] capable of providing activity dependencies.
 *
 * The [component] defaults to a dagger implementation that depends on the build configuration. This
 * allows us to define the app behaviour at build time.
 *
 * This [component] can be rewritten in order to provide different dependencies. However, this
 * functionality should only be used for testing (specifically, mocking for unit tests).
 *
 * **Note**: Dagger is being used here, but it's definitely an overkill as we only have a single
 * dependency. Stop using it if we come to the conclusion that it's making the code much harder to
 * understand.
 */
class App : Application() {

    /**
     * The DI component used to provide activity dependencies
     */
    lateinit var component: AppComponent @VisibleForTesting set

    override fun onCreate() {
        // Call superclass implementation
        super.onCreate()

        // Create component
        component = DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .build()
    }
}

@Component(modules = [AppModule::class])
@Singleton
interface AppComponent {

    fun client(): Client
}

@Module
class AppModule(private val app: App) {

    @Provides
    @Singleton
    fun provideClient(localTokenDAO: LocalTokenDAO, userDataDAO: UserDataDAO): Client = Client(localTokenDAO, userDataDAO)

    @Provides
    fun provideLocalTokenDAO(): LocalTokenDAO = when {

        BuildConfig.MOCKED_DATA_ACCESS -> SharedPrefsLocalTokenDAO(app)

        else -> MemoryLocalTokenDAO()
    }

    @Provides
    fun provideUserDataDAO(): UserDataDAO = when {

        BuildConfig.MOCKED_DATA_ACCESS -> MemoryUserDataDAO(

                uncertaintyParams = MemoryUserDataDAO.UncertaintyParams(

                        averageResponseDelayInMillis = 1500,

                        responseDelayDeviationInMillis = 500
                )
        )

        else -> NetworkUserDataDAO(app)
    }
}