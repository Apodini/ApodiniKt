package de.tum.`in`.ase.apodini.environment

interface EnvironmentStore : EnvironmentKeys {
    val keys: Set<EnvironmentKey<*>>
    fun contains(key: EnvironmentKey<*>): Boolean

    operator fun <T> get(key: EnvironmentKey<T>) : T

    operator fun <T> EnvironmentKey<T>.invoke(): T {
        return this@EnvironmentStore[this]
    }

    companion object {
        val empty: EnvironmentStore = StandardEnvironmentStore(emptyMap())

        operator fun invoke(init: EnvironmentBuilder.() -> Unit): EnvironmentStore {
            return StandardEnvironmentStoreBuilder().also(init).build()
        }
    }
}

private class StandardEnvironmentStore(
    private val store: Map<EnvironmentKey<*>, Any?>
) : EnvironmentStore {
    override val keys: Set<EnvironmentKey<*>>
        get() = store.keys

    override fun contains(key: EnvironmentKey<*>): Boolean {
        return store.containsKey(key)
    }

    override operator fun <T> get(key: EnvironmentKey<T>) : T {
        return store[key]?.let { value ->
            @Suppress("UNCHECKED_CAST")
            value as T
        } ?: key.default
    }
}

private class StandardEnvironmentStoreBuilder : EnvironmentBuilder {
    private val map = mutableMapOf<EnvironmentKey<*>, Any?>()

    override fun <T> EnvironmentKey<T>.invoke(value: () -> T) {
        map[this] = value()
    }

    fun build(): StandardEnvironmentStore {
        return StandardEnvironmentStore(map)
    }
}