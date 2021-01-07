package de.tum.`in`.ase.apodini

import de.tum.`in`.ase.apodini.types.Type
import kotlin.coroutines.CoroutineContext

interface Handler<Output : Type<Output>> {
    suspend fun CoroutineContext.compute(): Output
}

internal suspend fun <O : Type<O>> Handler<O>.compute(coroutineContext: CoroutineContext): O {
    return with(coroutineContext) {
        compute()
    }
}