package com.gabrielhuff.sample.login.dao.prefs

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.gabrielhuff.sample.login.dao.LocalTokenDAO

/**
 * A [LocalTokenDAO] that stores the local token on the input shared preferences.
 *
 * The token is stored as string mapped by the input [tokenKey] key.
 */
class SharedPrefsLocalTokenDAO(

        /**
         * The preferences used to store the local token
         */
        private val prefs: SharedPreferences,

        /**
         * The key to be used to map the token on the preferences. Defaults to [DEFAULT_TOKEN_KEY]
         */
        private val tokenKey: String = DEFAULT_TOKEN_KEY

) : LocalTokenDAO {

    constructor(

            /**
             * The context who owns the prefs
             */
            context: Context,

            /**
             * The prefs name. Defaults to [DEFAULT_PREFS_NAME]
             */
            prefsName: String = DEFAULT_PREFS_NAME,

            /**
             * The key to be used to map the token on the preferences. Defaults to [DEFAULT_TOKEN_KEY]
             */
            tokenKey: String = DEFAULT_TOKEN_KEY
    ) : this(context.getSharedPreferences(prefsName, MODE_PRIVATE), tokenKey)

    override var localToken: String?
        get() = prefs.getString(tokenKey, null)
        set(value) { prefs.edit().putString(tokenKey, value.toString()).apply() }

    companion object {

        const val DEFAULT_PREFS_NAME = "local_token"

        const val DEFAULT_TOKEN_KEY = "token"
    }
}