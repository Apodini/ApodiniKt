package de.tum.`in`.ase.apodini.types

interface CustomType<Self : CustomType<Self>> {
    fun TypeDefinitionBuilder.definition(): TypeDefinition<Self>
}