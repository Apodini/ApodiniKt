package de.tum.`in`.ase.apodini.properties.options

import de.tum.`in`.ase.apodini.properties.Parameter

private object ParameterDefaultKey : OptionKey<Parameter<*>, () -> Any>

val <T> OptionKeys<Parameter<T>, () -> T>.default: OptionKey<Parameter<T>, () -> T>
    get() {
        @Suppress("UNCHECKED_CAST")
        return ParameterDefaultKey as OptionKey<Parameter<T>, () -> T>
    }

fun <T> OptionsBuilder<Parameter<T>>.default(value: () -> T) {
    ParameterDefaultKey(value)
}