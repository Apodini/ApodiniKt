package de.tum.`in`.ase.apodini.internal.reflection

internal fun <T> T.shallowCopy(): T {
    if (this == null)
        return this

    return nonNullShallowCopy()
}

private fun <T : Any> T.nonNullShallowCopy(): T {
    val type = this::class

    return createInstance(type).also { newObject ->
        for (field in type.java.declaredFields) {
            if (field.name == "\$\$delegatedProperties")
                continue
            val wasAccessible = field.isAccessible
            field.isAccessible = true
            field.set(newObject, field.get(this))
            field.isAccessible = wasAccessible
        }
    }
}