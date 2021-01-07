package de.tum.`in`.ase.apodini.properties

import de.tum.`in`.ase.apodini.internal.PropertyCollector
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.request.Request
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> parameter(name: String? = null): Parameter<T> {
    return parameter(name, typeOf<T>())
}

@PublishedApi
internal fun <T : Any> parameter(name: String? = null, type: KType): Parameter<T> {
    return Parameter(name, type)
}

data class Parameter<T : Any> internal constructor(
        var name: String?,
        private val type: KType
): RequestInjectable {
    private val id = UUID.randomUUID()
    lateinit var value: T

    override fun PropertyCollector.collect(property: KProperty<*>) {
        name = name ?: property.name
        registerParameter(id, name!!, type)
    }

    override fun inject(request: Request) {
        value = request.parameter(id)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        TODO("Not implemented yet")
    }
}