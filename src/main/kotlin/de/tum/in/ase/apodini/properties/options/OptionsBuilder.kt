package de.tum.`in`.ase.apodini.properties.options

interface OptionsBuilder<out P> {
    infix fun <T> OptionKey<P, T>.to(value: T)
}