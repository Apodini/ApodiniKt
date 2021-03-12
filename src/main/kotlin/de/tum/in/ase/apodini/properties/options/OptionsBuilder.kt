package de.tum.`in`.ase.apodini.properties.options

@OptionsDsl
interface OptionsBuilder<out P> {
    infix operator fun <T> OptionKey<P, T>.invoke(value: T)
}