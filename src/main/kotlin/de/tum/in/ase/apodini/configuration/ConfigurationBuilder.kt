package de.tum.`in`.ase.apodini.configuration

import de.tum.`in`.ase.apodini.environment.EnvironmentKey
import de.tum.`in`.ase.apodini.environment.EnvironmentKeys
import de.tum.`in`.ase.apodini.exporter.Exporter
import kotlin.reflect.KProperty1

interface ConfigurationBuilder {
    fun use(exporter: Exporter)
    fun <T> environment(key: EnvironmentKey<T>, value: T)

    fun <T> environment(key: KProperty1<EnvironmentKeys, EnvironmentKey<T>>, value: T) {
        environment(key.get(EnvironmentKeys), value)
    }
}