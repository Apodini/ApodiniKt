package de.tum.`in`.ase.apodini.request

import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import java.util.*
import kotlin.coroutines.CoroutineContext

interface Request : CoroutineContext, EnvironmentStore {
    fun <T> parameter(id: UUID): T
}