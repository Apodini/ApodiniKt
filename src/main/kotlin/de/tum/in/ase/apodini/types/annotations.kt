package de.tum.`in`.ase.apodini.types

@Target(AnnotationTarget.PROPERTY)
annotation class Hidden

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Renamed(val name: String)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Documented(val documentation: String)