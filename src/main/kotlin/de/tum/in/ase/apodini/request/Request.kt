package de.tum.`in`.ase.apodini.request

import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.internal.reflection.modify
import de.tum.`in`.ase.apodini.internal.reflection.shallowCopy
import de.tum.`in`.ase.apodini.internal.reflection.traverseSuspended
import de.tum.`in`.ase.apodini.properties.DynamicProperty
import java.util.*
import kotlin.coroutines.CoroutineContext

interface Request : CoroutineContext, EnvironmentStore {
    fun <T> parameter(id: UUID): T
}

suspend fun <T> Request.handle(
    handler: Handler<T>
): T {
    val newInstance = handler.shallowCopy()
    newInstance.modify<RequestInjectable> { injectable ->
        injectable.shallowCopy().apply { inject(this@handle) }
    }
    newInstance.traverseSuspended<DynamicProperty> { property ->
        property.update()
    }
    return with(newInstance) { compute() }
}