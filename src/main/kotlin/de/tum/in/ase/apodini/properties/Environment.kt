package de.tum.`in`.ase.apodini.properties

import de.tum.`in`.ase.apodini.environment.EnvironmentKey
import de.tum.`in`.ase.apodini.environment.EnvironmentKeys
import de.tum.`in`.ase.apodini.internal.RequestInjectable
import de.tum.`in`.ase.apodini.internal.reflection.contains
import de.tum.`in`.ase.apodini.internal.reflection.modify
import de.tum.`in`.ase.apodini.internal.reflection.shallowCopy
import de.tum.`in`.ase.apodini.request.Request
import kotlin.reflect.KProperty

private object ConcreteEnvironmentKeys : EnvironmentKeys

fun <T> environment(key: EnvironmentKeys.() -> EnvironmentKey<T>): Environment<T> {
    return Environment(ConcreteEnvironmentKeys.key())
}

data class Environment<T> internal constructor(
    private val key: EnvironmentKey<T>
): RequestInjectable {
    private var value: T? = null

    override fun inject(request: Request) {
        value = request[key].let { value ->
            if (value != null && value.contains<RequestInjectable>()) {
                value.shallowCopy().also { newValue ->
                    newValue.modify<RequestInjectable> { injectable ->
                        injectable.shallowCopy().apply { inject(request) }
                    }
                }
            } else {
                value
            }
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: key.default
    }
}