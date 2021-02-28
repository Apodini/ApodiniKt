package de.tum.`in`.ase.apodini.environment

interface EnvironmentBuilder : EnvironmentKeys {
    operator fun <T> EnvironmentKey<T>.invoke(value: () -> T)
}