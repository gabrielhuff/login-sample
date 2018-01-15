package com.gabrielhuff.sample.login.dao

/**
 * Object capable of reading and writing an access token locally and synchronously
 */
interface LocalTokenDAO {

    /**
     * The local token
     */
    var localToken: String?
}