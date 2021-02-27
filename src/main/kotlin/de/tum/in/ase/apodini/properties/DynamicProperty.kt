package de.tum.`in`.ase.apodini.properties

interface DynamicProperty {
    suspend fun update() {
        // Default is no-op
    }
}
