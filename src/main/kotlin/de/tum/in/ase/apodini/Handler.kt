package de.tum.`in`.ase.apodini

import de.tum.`in`.ase.apodini.types.CustomType
import kotlin.coroutines.CoroutineContext

interface Handler<Output> {
    suspend fun CoroutineContext.compute(): Output
}

internal suspend fun <O : CustomType<O>> Handler<O>.compute(coroutineContext: CoroutineContext): O {
    return with(coroutineContext) {
        compute()
    }
}

typealias StringHandler = Handler<String>
typealias IntHandler = Handler<String>
typealias BooleanHandler = Handler<Boolean>
typealias DoubleHandler = Handler<Double>