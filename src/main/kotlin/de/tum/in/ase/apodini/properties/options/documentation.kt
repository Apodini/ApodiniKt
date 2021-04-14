package de.tum.`in`.ase.apodini.properties.options

import de.tum.`in`.ase.apodini.properties.Parameter

private object DocumentationKey : OptionKey<Parameter<*>, String>

val <T> OptionKeys<Parameter<T>, String>.documentation: OptionKey<Parameter<T>, String>
    get() {
        return DocumentationKey
    }

fun <T> OptionsBuilder<Parameter<T>>.documentation(text: String) {
    DocumentationKey(text)
}