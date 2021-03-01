package de.tum.`in`.ase.apodini.properties.options

class OptionSet<P> internal constructor(
        private val map: Map<OptionKey<P, *>, Any>
) {
    constructor(init: OptionsBuilder<P>.() -> Unit) : this(OptionSetBuilder<P>().also(init).build())

    operator fun <T> get(key: OptionKey<P, T>): T? {
        return map[key]?.let { value ->
            @Suppress("UNCHECKED_CAST")
            value as T
        }
    }

    operator fun <T> invoke(key: OptionKeys<P, T>.() -> OptionKey<P, T>): T? {
        @Suppress("UNCHECKED_CAST")
        val keys = ConcreteOptionKeys as OptionKeys<P, T>
        return this[keys.key()]
    }
}

private object ConcreteOptionKeys : OptionKeys<Any, Any>

private class OptionSetBuilder<P> : OptionsBuilder<P> {
    private val map = mutableMapOf<OptionKey<P, *>, Any>()

    override fun <T> OptionKey<P, T>.invoke(value: T) {
        map[this] = value as Any
    }

    fun build(): Map<OptionKey<P, *>, Any> {
        return map
    }
}