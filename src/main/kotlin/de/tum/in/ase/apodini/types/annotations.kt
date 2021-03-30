package de.tum.`in`.ase.apodini.types

@Target(AnnotationTarget.PROPERTY)
annotation class Hidden

@Target(AnnotationTarget.FIELD)
annotation class MirroredName

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Renamed(val name: String)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Documented(val documentation: String)

internal inline fun <reified T : Annotation> Iterable<Annotation>.contains(): Boolean {
    val type = T::class
    return any { type.isInstance(it) }
}

internal inline fun <reified T : Annotation> Iterable<Annotation>.annotation(): T? {
    forEach { annotation ->
        (annotation as? T)?.let { return it }
    }
    return null
}