package com.example.data.local

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

actual fun databaseDispatcher(): CoroutineContext = Dispatchers.IO
