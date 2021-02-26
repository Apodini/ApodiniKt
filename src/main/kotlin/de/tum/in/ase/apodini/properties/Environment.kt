package de.tum.`in`.ase.apodini.properties

import de.tum.`in`.ase.apodini.environment.EnvironmentKey
import de.tum.`in`.ase.apodini.environment.EnvironmentKeys
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.request.Request
import kotlin.reflect.KProperty

fun <T : Any> environment(key: EnvironmentKeys.() -> EnvironmentKey<T>): Environment<T> {
    return Environment(EnvironmentKeys.key())
}

data class Environment<T : Any> internal constructor(
        private val key: EnvironmentKey<T>
): RequestInjectable {
    lateinit var value: T

    override fun inject(request: Request) {
        value = request.environment(key)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
}