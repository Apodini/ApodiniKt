package de.tum.`in`.ase.apodini.properties

import de.tum.`in`.ase.apodini.properties.options.http
import kotlin.reflect.KProperty

fun pathParameter(
        name: String? = null
): PathParameter {
    val parameter = parameter<String>(name) {
        http {
            path
        }
    }
    return PathParameter(parameter)
}

data class PathParameter internal constructor(
        private val parameter: Parameter<String>
): DynamicProperty {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return parameter.getValue(thisRef, property)
    }
}