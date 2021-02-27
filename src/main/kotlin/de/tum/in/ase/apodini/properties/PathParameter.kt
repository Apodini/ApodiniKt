package de.tum.`in`.ase.apodini.properties

import de.tum.`in`.ase.apodini.properties.options.http
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

// TODO: Handle multiple parameters of other types in a way that's compatible with the JVM

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : String> pathParameter(
        name: String? = null
): PathParameter<String> {
    return pathParameter(name, typeOf<T>())
}

@PublishedApi
internal fun <T : Any> pathParameter(
        name: String? = null,
        type: KType,
): PathParameter<T> {
    val parameter = parameter<T>(name, type) {
        http {
            path
        }
    }

    return PathParameter(parameter)
}

data class PathParameter<T : Any> internal constructor(
        private val parameter: Parameter<T>
): DynamicProperty {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return parameter.getValue(thisRef, property)
    }
}