package de.tum.`in`.ase.apodini.properties

import de.tum.`in`.ase.apodini.internal.PropertyCollector
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.properties.options.OptionSet
import de.tum.`in`.ase.apodini.properties.options.OptionsBuilder
import de.tum.`in`.ase.apodini.request.Request
import de.tum.`in`.ase.apodini.types.MirroredName
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> parameter(
    name: String? = null,
    noinline init: OptionsBuilder<Parameter<T>>.() -> Unit = {}
): Parameter<T> {
    return parameter(name, typeOf<T>(), init)
}

@PublishedApi
internal fun <T> parameter(
    name: String? = null,
    type: KType,
    init: OptionsBuilder<Parameter<T>>.() -> Unit = {}
): Parameter<T> {
    return Parameter(name, type, OptionSet(init))
}

class Parameter<T> internal constructor(
    val name: String?,
    type: KType,
    options: OptionSet<Parameter<T>>
): DynamicProperty {
    @delegate:MirroredName
    private val value by ParameterBox(name, type, options)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
}

private class ParameterBox<T>(
    val name: String?,
    private val type: KType,
    private val options: OptionSet<Parameter<T>>
): RequestInjectable {
    private val id = UUID.randomUUID()
    private var value: T? = null

    override fun PropertyCollector.collect(propertyName: String) {
        @Suppress("UNCHECKED_CAST")
        registerParameter(id, name ?: propertyName, type, options)
    }

    override fun inject(request: Request) {
        value = request.parameter(id)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (type.isMarkedNullable) {
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        return value ?: throw IllegalArgumentException("Argument not initialized")
    }
}