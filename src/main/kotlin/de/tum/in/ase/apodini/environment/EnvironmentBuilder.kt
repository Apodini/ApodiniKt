package de.tum.`in`.ase.apodini.environment

@EnvironmentDsl
interface EnvironmentBuilder : EnvironmentKeys {
    operator fun <T> EnvironmentKey<T>.invoke(value: () -> T)
}