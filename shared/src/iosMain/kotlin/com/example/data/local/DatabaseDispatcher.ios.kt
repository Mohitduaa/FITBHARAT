package com.example.data.local

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual fun databaseDispatcher(): CoroutineContext = Dispatchers.IO
