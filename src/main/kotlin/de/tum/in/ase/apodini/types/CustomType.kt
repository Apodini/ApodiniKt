package de.tum.`in`.ase.apodini.types

interface CustomType<Self : CustomType<Self>> {
    fun definition(): TypeDefinition<Self>
}