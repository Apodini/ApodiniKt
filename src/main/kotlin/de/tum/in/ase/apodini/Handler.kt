package de.tum.`in`.ase.apodini

import de.tum.`in`.ase.apodini.types.Scalar
import de.tum.`in`.ase.apodini.types.Type
import de.tum.`in`.ase.apodini.types.TypeDefinition
import kotlin.coroutines.CoroutineContext

interface Handler<Output : Type<Output>> {
    suspend fun CoroutineContext.compute(): Output
}

internal suspend fun <O : Type<O>> Handler<O>.compute(coroutineContext: CoroutineContext): O {
    return with(coroutineContext) {
        compute()
    }
}


interface StringHandler : Handler<StringHandler.Output> {
    class Output internal constructor(private val output: String) : Type<Output> {
        override fun definition(): TypeDefinition<Output> = Scalar.string { it.output }
    }

    suspend fun CoroutineContext.impl(): String

    override suspend fun CoroutineContext.compute(): Output {
        val output = impl()
        return Output(output)
    }
}

interface IntHandler : Handler<IntHandler.Output> {
    class Output internal constructor(private val output: Int) : Type<Output> {
        override fun definition(): TypeDefinition<Output> = Scalar.int { it.output }
    }

    suspend fun CoroutineContext.impl(): Int

    override suspend fun CoroutineContext.compute(): Output {
        val output = impl()
        return Output(output)
    }
}

interface BooleanHandler : Handler<BooleanHandler.Output> {
    class Output internal constructor(private val output: Boolean) : Type<Output> {
        override fun definition(): TypeDefinition<Output> = Scalar.boolean { it.output }
    }

    suspend fun CoroutineContext.impl(): Boolean

    override suspend fun CoroutineContext.compute(): Output {
        val output = impl()
        return Output(output)
    }
}

interface DoubleHandler : Handler<DoubleHandler.Output> {
    class Output internal constructor(private val output: Double) : Type<Output> {
        override fun definition(): TypeDefinition<Output> = Scalar.double { it.output }
    }

    suspend fun CoroutineContext.impl(): Double

    override suspend fun CoroutineContext.compute(): Output {
        val output = impl()
        return Output(output)
    }
}