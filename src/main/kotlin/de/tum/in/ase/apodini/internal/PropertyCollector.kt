package de.tum.`in`.ase.apodini.internal

import java.util.*
import kotlin.reflect.KType

interface PropertyCollector {
    fun registerParameter(id: UUID, name: String, type: KType)
}