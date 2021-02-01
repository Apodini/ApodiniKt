package de.tum.`in`.ase.apodini.configuration

import de.tum.`in`.ase.apodini.environment.EnvironmentKey
import de.tum.`in`.ase.apodini.exporter.Exporter
import kotlin.reflect.KProperty

interface ConfigurationBuilder {
    fun use(exporter: Exporter)
    fun <T> environment(key: EnvironmentKey<T>, value: T)

    fun <T> environment(key: KProperty<EnvironmentKey<T>>, value: T) {
        environment(key.call(), value)
    }
}