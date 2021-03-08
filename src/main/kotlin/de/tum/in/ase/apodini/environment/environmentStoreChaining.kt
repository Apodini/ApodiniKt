package de.tum.`in`.ase.apodini.environment

fun EnvironmentStore.override(store: EnvironmentStore): EnvironmentStore {
    return ChainedEnvironmentStore(default = store, fallback = this)
}

fun EnvironmentStore.override(init: EnvironmentBuilder.() -> Unit): EnvironmentStore {
    return override(EnvironmentStore(init))
}

fun EnvironmentStore.extend(store: EnvironmentStore): EnvironmentStore {
    return ChainedEnvironmentStore(default = this, fallback = store)
}

fun EnvironmentStore.extend(init: EnvironmentBuilder.() -> Unit): EnvironmentStore {
    return extend(EnvironmentStore(init))
}

private class ChainedEnvironmentStore(
    val default: EnvironmentStore,
    val fallback: EnvironmentStore
): EnvironmentStore {

    override val keys: Set<EnvironmentKey<*>>
        get() = default.keys + fallback.keys

    override fun contains(key: EnvironmentKey<*>): Boolean {
        return default.contains(key) || fallback.contains(key)
    }

    override fun <T> get(key: EnvironmentKey<T>): T {
        if (default.contains(key)) {
            return default[key]
        }

        return fallback[key]
    }

}