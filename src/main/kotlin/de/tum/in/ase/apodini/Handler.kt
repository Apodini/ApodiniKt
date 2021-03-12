package de.tum.`in`.ase.apodini

import kotlin.coroutines.CoroutineContext

interface Handler<Output> {
    suspend fun CoroutineContext.compute(): Output
}
