package com.example.data.local

import kotlin.coroutines.CoroutineContext

/** Where Room runs its blocking queries; Dispatchers.IO is not part of the common coroutines API. */
expect fun databaseDispatcher(): CoroutineContext
