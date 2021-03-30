package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.environment.EnvironmentKey
import de.tum.`in`.ase.apodini.environment.EnvironmentKeys
import de.tum.`in`.ase.apodini.modifiers.ModifiableComponent
import de.tum.`in`.ase.apodini.modifiers.withEnvironment

fun <T : ModifiableComponent<T>> ModifiableComponent<T>.operation(value: Operation.Companion.() -> Operation): T {
    return withEnvironment {
        operation {
            Operation.value()
        }
    }
}

enum class Operation {
    Create, Read, Update, Delete;

    companion object {
        val create: Operation = Create
        val read: Operation = Read
        val update: Operation = Update
        val delete: Operation = Delete
    }
}

val EnvironmentKeys.operation: EnvironmentKey<Operation>
    get() = OperationKey

private object OperationKey : EnvironmentKey<Operation>() {
    override val default = Operation.Read
}