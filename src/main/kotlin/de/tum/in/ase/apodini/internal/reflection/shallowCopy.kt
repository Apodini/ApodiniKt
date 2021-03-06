package de.tum.`in`.ase.apodini.internal.reflection

internal fun <T> T.shallowCopy(): T {
    if (this == null)
        return this

    return nonNullShallowCopy()
}

private fun <T : Any> T.nonNullShallowCopy(): T {
    val type = this::class

    if (type.isData) {
        val copy = type.members.first { it.name == "copy" }
        @Suppress("UNCHECKED_CAST")
        return copy.call(this) as T
    }

    return createInstance(type).also { newObject ->
        for (field in type.java.fields) {
            val wasAccessible = field.isAccessible
            field.isAccessible = true
            field.set(newObject, field.get(this))
            field.isAccessible = wasAccessible
        }
    }
}