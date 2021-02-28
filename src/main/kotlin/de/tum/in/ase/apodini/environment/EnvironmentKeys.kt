package de.tum.`in`.ase.apodini.environment

interface EnvironmentKeys

abstract class EnvironmentKey<T> {
    abstract val default: T
}