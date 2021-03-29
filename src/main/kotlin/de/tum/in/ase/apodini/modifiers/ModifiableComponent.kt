package de.tum.`in`.ase.apodini.modifiers

interface ModifiableComponent<out T : ModifiableComponent<T>> {
    fun modify(modifier: Modifier): T
}