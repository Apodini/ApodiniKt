package de.tum.`in`.ase.apodini

import de.tum.`in`.ase.apodini.internal.ModifiedHandler
import de.tum.`in`.ase.apodini.modifiers.ModifiableComponent
import de.tum.`in`.ase.apodini.modifiers.Modifier
import kotlin.coroutines.CoroutineContext

interface Handler<Output> : ModifiableComponent<Handler<Output>> {
    suspend fun CoroutineContext.compute(): Output

    override fun modify(modifier: Modifier): Handler<Output> {
        return ModifiedHandler(this, listOf(modifier))
    }
}
