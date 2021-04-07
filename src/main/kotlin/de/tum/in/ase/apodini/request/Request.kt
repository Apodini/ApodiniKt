package de.tum.`in`.ase.apodini.request

import de.tum.`in`.ase.apodini.environment.EnvironmentStore
import kotlinx.coroutines.CoroutineScope
import java.util.*
import kotlin.coroutines.CoroutineContext

interface Request : CoroutineScope, EnvironmentStore {
    fun <T> parameter(id: UUID): T
}