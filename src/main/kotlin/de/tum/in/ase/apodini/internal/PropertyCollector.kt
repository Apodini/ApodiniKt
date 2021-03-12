package de.tum.`in`.ase.apodini.internal

import de.tum.`in`.ase.apodini.properties.Parameter
import de.tum.`in`.ase.apodini.properties.options.OptionSet
import java.util.*
import kotlin.reflect.KType

internal interface PropertyCollector {
    fun <T> registerParameter(id: UUID, name: String, type: KType, options: OptionSet<Parameter<T>>)
}