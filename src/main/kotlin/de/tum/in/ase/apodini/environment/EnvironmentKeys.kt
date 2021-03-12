package de.tum.`in`.ase.apodini.environment

@EnvironmentDsl
interface EnvironmentKeys

abstract class EnvironmentKey<T> {
    abstract val default: T
}