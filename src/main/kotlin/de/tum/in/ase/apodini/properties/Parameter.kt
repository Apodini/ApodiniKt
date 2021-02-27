package de.tum.`in`.ase.apodini.properties

import de.tum.`in`.ase.apodini.internal.PropertyCollector
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.properties.options.OptionKey
import de.tum.`in`.ase.apodini.properties.options.OptionSet
import de.tum.`in`.ase.apodini.properties.options.OptionsBuilder
import de.tum.`in`.ase.apodini.request.Request
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> parameter(
        name: String? = null,
        noinline init: OptionsBuilder<Parameter<T>>.() -> Unit = {}
): Parameter<T> {
    return parameter(name, typeOf<T>(), init)
}

@PublishedApi
internal fun <T : Any> parameter(
        name: String? = null,
        type: KType,
        init: OptionsBuilder<Parameter<T>>.() -> Unit = {}
): Parameter<T> {
    return Parameter(name, type, OptionSet(init))
}

data class Parameter<T : Any> internal constructor(
        var name: String?,
        private val type: KType,
        val options: OptionSet<Parameter<T>>
): RequestInjectable {
    private val id = UUID.randomUUID()
    lateinit var value: T

    override fun PropertyCollector.collect(property: KProperty<*>) {
        registerParameter(id, name ?: property.name, type)
    }

    override fun inject(request: Request) {
        value = request.parameter(id)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
}