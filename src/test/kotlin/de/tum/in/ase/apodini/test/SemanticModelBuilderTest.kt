package de.tum.`in`.ase.apodini.test

import de.tum.`in`.ase.apodini.Component
import de.tum.`in`.ase.apodini.ComponentBuilder
import de.tum.`in`.ase.apodini.Handler
import de.tum.`in`.ase.apodini.environment.EnvironmentKey
import de.tum.`in`.ase.apodini.exporter.RESTExporter
import de.tum.`in`.ase.apodini.impl.group
import de.tum.`in`.ase.apodini.impl.text
import de.tum.`in`.ase.apodini.model.Operation
import de.tum.`in`.ase.apodini.model.SemanticModel
import de.tum.`in`.ase.apodini.model.operation
import de.tum.`in`.ase.apodini.modifiers.withEnvironment
import de.tum.`in`.ase.apodini.properties.Parameter
import de.tum.`in`.ase.apodini.properties.options.HTTPParameterMode
import de.tum.`in`.ase.apodini.properties.options.OptionSet
import de.tum.`in`.ase.apodini.properties.options.default
import de.tum.`in`.ase.apodini.properties.options.http
import de.tum.`in`.ase.apodini.properties.parameter
import de.tum.`in`.ase.apodini.properties.pathParameter
import de.tum.`in`.ase.apodini.test.utils.semanticModel
import de.tum.`in`.ase.apodini.types.Documented
import de.tum.`in`.ase.apodini.types.Nullable
import de.tum.`in`.ase.apodini.types.StringType
import junit.framework.TestCase
import kotlinx.coroutines.CoroutineScope
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
internal class SemanticModelBuilderTest : TestCase() {

    fun testHelloWorld() {
        val semanticModel = semanticModel {
            text("Hello World")
        }
        assertEquals(semanticModel.exporters.count(), 1)
        assert(semanticModel.exporters.first() is RESTExporter)
        assert(semanticModel.globalEnvironment.keys.isEmpty())
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.handler::class.simpleName, "Text")
        assertEquals(endpoint.path.count(), 0)
        assertEquals(endpoint.parameters.count(), 0)
        assertEquals(endpoint.typeDefinition, StringType)
        assertEquals(endpoint.documentation, null)
    }

    fun testCustomHandlerWithOptionalString() {
        val semanticModel = semanticModel {
            +object : Handler<String?> {
                override suspend fun CoroutineScope.compute(): String? {
                    return null
                }
            }
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 0)
        assertEquals(endpoint.parameters.count(), 0)
        assertEquals(endpoint.typeDefinition, Nullable(StringType))
        assertEquals(endpoint.documentation, null)
    }

    fun testSimpleParameter() {
        val semanticModel = semanticModel {
            +object : Handler<String> {
                val name by parameter<String?>()

                override suspend fun CoroutineScope.compute(): String {
                    return "Hello, ${name ?: "World"}"
                }
            }
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 0)
        assertEquals(endpoint.parameters.count(), 1)
        assertEquals(endpoint.parameters.first().name, "name")
        assertEquals(endpoint.parameters.first().type, typeOf<String?>())
        assertEquals(endpoint.typeDefinition, StringType)
        assertEquals(endpoint.documentation, null)
    }

    fun testParameterWithOptions() {
        val semanticModel = semanticModel {
            +object : Handler<String> {
                val name by parameter<String> {
                    default("World")
                    http { query }
                }

                override suspend fun CoroutineScope.compute(): String {
                    return "Hello, $name"
                }
            }
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 0)
        assertEquals(endpoint.parameters.count(), 1)
        assertEquals(endpoint.parameters.first().name, "name")
        assertEquals(endpoint.parameters.first().type, typeOf<String>())
        assertEquals(endpoint.parameters.first().defaultValue, "World")

        @Suppress("UNCHECKED_CAST")
        val options = endpoint.parameters.first().options as OptionSet<Parameter<String>>
        assertEquals(options<HTTPParameterMode<String>> { http }, HTTPParameterMode.query)
        assertEquals(endpoint.typeDefinition, StringType)
        assertEquals(endpoint.documentation, null)
    }

    fun testWithOperation() {
        val semanticModel = semanticModel {
            text("Hello World").operation { read }
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 0)
        assertEquals(endpoint.parameters.count(), 0)
        assertEquals(endpoint.typeDefinition, StringType)
        assertEquals(endpoint.documentation, null)
        assertEquals(endpoint.operation, Operation.read)
    }

    fun testDocumentation() {
        val semanticModel = semanticModel {
            +DocumentedHandler
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 0)
        assertEquals(endpoint.parameters.count(), 0)
        assertEquals(endpoint.typeDefinition, StringType)
        assertEquals(endpoint.documentation, "Documented Handler")
    }

    fun testGroup() {
        val semanticModel = semanticModel {
            group("greeting") {
                text("Hello World")
            }
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 1)
        assertEquals(endpoint.path.first(), SemanticModel.PathComponent.StringPathComponent("greeting"))
        assertEquals(endpoint.parameters.count(), 0)
        assertEquals(endpoint.typeDefinition, StringType)
        assertEquals(endpoint.documentation, null)
    }

    fun testGroupWithParameter() {
        val pathId = pathParameter()
        val semanticModel = semanticModel {
            group("user", pathId) {
                +object : Handler<String> {
                    val id by pathId

                    override suspend fun CoroutineScope.compute(): String {
                        return "User, $id"
                    }
                }
            }
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 2)
        assertEquals(endpoint.path.last(), SemanticModel.PathComponent.ParameterPathComponent(pathId))
        assertEquals(endpoint.parameters.count(), 1)
        assertEquals(endpoint.parameters.first().id, pathId.parameter.id)
        assertEquals(endpoint.typeDefinition, StringType)
        assertEquals(endpoint.documentation, null)
    }

    fun testEnvironmentOnHandlerWithExtensions() {
        val semanticModel = semanticModel {
            text("Hello World").withEnvironment {
                SomeKey {
                    42
                }
            }
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 0)
        assertEquals(endpoint.parameters.count(), 0)
        assertEquals(endpoint.typeDefinition, StringType)
        assert(endpoint.environment.keys.contains(SomeKey))
        assertEquals(endpoint.environment[SomeKey], 42)
        assertEquals(endpoint.documentation, null)
    }

    fun testEnvironmentOnHandlerWithUnaryPlus() {
        val semanticModel = semanticModel {
            +object : Handler<String> {
                override suspend fun CoroutineScope.compute(): String {
                    return "Hello World"
                }
            }.withEnvironment {
                SomeKey {
                    42
                }
            }
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 0)
        assertEquals(endpoint.parameters.count(), 0)
        assertEquals(endpoint.typeDefinition, StringType)
        assert(endpoint.environment.keys.contains(SomeKey))
        assertEquals(endpoint.environment[SomeKey], 42)
        assertEquals(endpoint.documentation, null)
    }

    fun testEnvironmentOnComponentWithExtensions() {
        val semanticModel = semanticModel {
            group("greeting") {
                text("Hello World").withEnvironment {
                    SomeKey {
                        42
                    }
                }
            }
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 1)
        assertEquals(endpoint.parameters.count(), 0)
        assertEquals(endpoint.typeDefinition, StringType)
        assert(endpoint.environment.keys.contains(SomeKey))
        assertEquals(endpoint.environment[SomeKey], 42)
        assertEquals(endpoint.documentation, null)
    }

    fun testEnvironmentOnComponentWithUnaryPlus() {
        val semanticModel = semanticModel {
            +object : Component {
                override fun ComponentBuilder.invoke() {
                    text("Hello World")
                }
            }.withEnvironment {
                SomeKey {
                    42
                }
            }
        }
        assertEquals(semanticModel.endpoints.count(), 1)

        val endpoint = semanticModel.endpoints.first()
        assertEquals(endpoint.path.count(), 0)
        assertEquals(endpoint.parameters.count(), 0)
        assertEquals(endpoint.typeDefinition, StringType)
        assert(endpoint.environment.keys.contains(SomeKey))
        assertEquals(endpoint.environment[SomeKey], 42)
        assertEquals(endpoint.documentation, null)
    }

    fun testEndpointNeighbors() {
        val semanticModel = semanticModel {
            group("group") {
                text("parent")
                group("child1") {
                    text("child1")
                }
                group("child2") {
                    text("child2")
                }
            }
        }
        assertEquals(semanticModel.endpoints.count(), 3)

        val child1 = semanticModel.endpoints[1]
        assertEquals(child1.path.count(), 2)
        assertEquals(child1.path.last(), SemanticModel.PathComponent.StringPathComponent("child1"))
        assertEquals(child1.neighbors.count(), 1)
        val child2 = child1.neighbors.first()
        assertEquals(child2, semanticModel.endpoints[2])
        assertEquals(child2.path.count(), 2)
        assertEquals(child2.path.last(), SemanticModel.PathComponent.StringPathComponent("child2"))
        assertEquals(child2.neighbors.count(), 1)
        assertEquals(child1, child2.neighbors.first())
    }

    fun testEndpointChildren() {
        val semanticModel = semanticModel {
            group("group") {
                text("parent")
                group("child1") {
                    text("child1")
                }
                group("child2") {
                    text("child2")
                }
            }
        }
        assertEquals(semanticModel.endpoints.count(), 3)

        val parent = semanticModel.endpoints[0]
        assertEquals(parent.path.count(), 1)
        assertEquals(parent.path.last(), SemanticModel.PathComponent.StringPathComponent("group"))
        assertEquals(parent.children.count(), 2)

        val child1 = parent.children[0]
        val child2 = parent.children[1]
        assertEquals(child1.path.count(), 2)
        assertEquals(child1.path.last(), SemanticModel.PathComponent.StringPathComponent("child1"))

        assertEquals(child2.path.count(), 2)
        assertEquals(child2.path.last(), SemanticModel.PathComponent.StringPathComponent("child2"))
    }

    fun testEndpointParent() {
        val semanticModel = semanticModel {
            group("group") {
                text("parent")
                group("child1") {
                    text("child1")
                }
                group("child2") {
                    text("child2")
                }
            }
        }
        assertEquals(semanticModel.endpoints.count(), 3)

        val parent = semanticModel.endpoints[0]
        assertEquals(parent.path.count(), 1)
        assertEquals(parent.path.last(), SemanticModel.PathComponent.StringPathComponent("group"))
        assertEquals(parent.children.count(), 2)

        val child1 = semanticModel.endpoints[1]
        val child2 = semanticModel.endpoints[2]
        assertEquals(child1.path.last(), SemanticModel.PathComponent.StringPathComponent("child1"))
        assertEquals(child1.parent, parent)

        assertEquals(child2.path.last(), SemanticModel.PathComponent.StringPathComponent("child2"))
        assertEquals(child2.parent, parent)
    }
}

private val boxField: KProperty1<Parameter<*>, *> by lazy {
    Parameter::class.memberProperties.toList()[1].also { it.isAccessible = true }
}

private val Parameter<*>.id: UUID
    get() {
        val box = boxField.getDelegate(this) as Any
        @Suppress("UNCHECKED_CAST")
        val idField = box::class.declaredMemberProperties.first().also { it.isAccessible = true } as KCallable<UUID>
        return idField.call(box)
    }

@Documented("Documented Handler")
private object DocumentedHandler : Handler<String> {
    override suspend fun CoroutineScope.compute(): String {
        return "Hello, World!"
    }
}

private object SomeKey : EnvironmentKey<Int>() {
    override val default = 0
}