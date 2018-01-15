package com.gabrielhuff.sample.login.dao.memory

import com.gabrielhuff.sample.login.dao.LocalTokenDAO

/**
 * A [LocalTokenDAO] that simply stores the local token in memory.
 *
 * A pre available token can be provided on construction.
 */
class MemoryLocalTokenDAO(override var localToken: String? = null) : LocalTokenDAO