package de.tum.`in`.ase.apodini.internal

import sun.reflect.ReflectionFactory
import java.lang.reflect.Constructor
import kotlin.reflect.KClass

private val reflectionFactory = ReflectionFactory.getReflectionFactory()
private val constructors = mutableMapOf<KClass<*>, Constructor<*>>()

fun <T : Any> createInstance(type: KClass<T>): T {
    type.constructors
            .firstOrNull { constructor -> constructor.parameters.all { it.isOptional } }
            ?.let {
                return it.call()
            }

    val constructor = constructors[type]
            ?: reflectionFactory.newConstructorForSerialization(type.java).also { constructors[type] = it }

    @Suppress("UNCHECKED_CAST")
    return constructor.newInstance() as T
}