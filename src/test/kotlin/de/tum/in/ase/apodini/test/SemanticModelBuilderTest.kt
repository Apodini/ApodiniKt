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
import de.tum.`in`.ase.apodini.properties.PathParameter
import de.tum.`in`.ase.apodini.properties.options.*
import de.tum.`in`.ase.apodini.properties.parameter
import de.tum.`in`.ase.apodini.properties.pathParameter
import de.tum.`in`.ase.apodini.test.utils.semanticModel
import de.tum.`in`.ase.apodini.types.*
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
                    documentation("Name that should be greeted")
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
        assertEquals(endpoint.parameters.first().documentation, "Name that should be greeted")

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
        assertEquals((endpoint.path.last() as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, pathId.parameter.id)
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

    fun testLinks() {
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
        assertEquals(parent.links.count(), 2)

        val child1 = parent.links[0]
        val child2 = parent.links[1]
        assertEquals(child1.name, "child1")
        assertEquals(child1.destination, semanticModel.endpoints[1])
        assertEquals(child2.name, "child2")
        assertEquals(child2.destination, semanticModel.endpoints[2])
    }

    fun testRelationshipsViaId() {
        val personId = pathParameter()
        val contentId = pathParameter()
        val semanticModel = semanticModel {
            group("person", personId) {
                +PersonHandler(personId)
            }
            group("content", contentId) {
                +ContentHandler(contentId)
            }
        }
        assertEquals(semanticModel.endpoints.count(), 2)

        val person = semanticModel.endpoints.first()
        val content = semanticModel.endpoints.last()
        assertEquals(content.links.count(), 1)
        assertEquals(content.links.first().destination, person)
    }

    fun testRelationshipsViaPath() {
        val personId = pathParameter()
        val contentId = pathParameter()
        val semanticModel = semanticModel {
            group("person", personId) {
                +PersonHandlerWithoutId
            }
            group("content", contentId) {
                +ContentHandlerWithoutId
            }
        }
        assertEquals(semanticModel.endpoints.count(), 2)

        val person = semanticModel.endpoints.first()
        val content = semanticModel.endpoints.last()
        assertEquals(content.links.count(), 1)
        assertEquals(content.links.first().destination, person)
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

    fun testAllTheGroupPermutations() {
        val param0 = pathParameter()
        val param1 = pathParameter()
        val param2 = pathParameter()

        assertEquals(
            semanticModel {
                group {
                    text("hello")
                }
            }.endpoints.first().path,
            emptyList<SemanticModel.PathComponent>()
        )

        var path = semanticModel {
            group("one", "two") {
                text("hello")
            }
        }.endpoints.first().path
        assertEquals(path[0], SemanticModel.PathComponent.StringPathComponent("one"))
        assertEquals(path[1], SemanticModel.PathComponent.StringPathComponent("two"))

        path = semanticModel {
            group(param0, "one", "two") {
                text("hello")
            }
        }.endpoints.first().path
        assertEquals((path[0] as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, param0.parameter.id)
        assertEquals(path[1], SemanticModel.PathComponent.StringPathComponent("one"))
        assertEquals(path[2], SemanticModel.PathComponent.StringPathComponent("two"))

        path = semanticModel {
            group("one", param0, "two") {
                text("hello")
            }
        }.endpoints.first().path
        assertEquals(path[0], SemanticModel.PathComponent.StringPathComponent("one"))
        assertEquals((path[1] as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, param0.parameter.id)
        assertEquals(path[2], SemanticModel.PathComponent.StringPathComponent("two"))

        path = semanticModel {
            group("one", "two", param0) {
                text("hello")
            }
        }.endpoints.first().path
        assertEquals(path[0], SemanticModel.PathComponent.StringPathComponent("one"))
        assertEquals(path[1], SemanticModel.PathComponent.StringPathComponent("two"))
        assertEquals((path[2] as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, param0.parameter.id)

        path = semanticModel {
            group(param0, param1,"one") {
                text("hello")
            }
        }.endpoints.first().path
        assertEquals((path[0] as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, param0.parameter.id)
        assertEquals((path[1] as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, param1.parameter.id)
        assertEquals(path[2], SemanticModel.PathComponent.StringPathComponent("one"))

        path = semanticModel {
            group(param0,"one", param1) {
                text("hello")
            }
        }.endpoints.first().path
        assertEquals((path[0] as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, param0.parameter.id)
        assertEquals(path[1], SemanticModel.PathComponent.StringPathComponent("one"))
        assertEquals((path[2] as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, param1.parameter.id)

        path = semanticModel {
            group("one", param0, param1) {
                text("hello")
            }
        }.endpoints.first().path
        assertEquals(path[0], SemanticModel.PathComponent.StringPathComponent("one"))
        assertEquals((path[1] as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, param0.parameter.id)
        assertEquals((path[2] as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, param1.parameter.id)

        path = semanticModel {
            group(param0, param1, param2) {
                text("hello")
            }
        }.endpoints.first().path

        assertEquals((path[0] as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, param0.parameter.id)
        assertEquals((path[1] as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, param1.parameter.id)
        assertEquals((path[2] as SemanticModel.PathComponent.ParameterPathComponent).parameter.id, param2.parameter.id)
    }
}

private val boxField: KProperty1<Parameter<*>, *> by lazy {
    Parameter::class.memberProperties.toList()[0].also { it.isAccessible = true }
}

private val Parameter<*>.id: UUID
    get() {
        val box = boxField.getDelegate(this) as Any
        @Suppress("UNCHECKED_CAST")
        val idField = box::class.declaredMemberProperties.first().also { it.isAccessible = true } as KCallable<UUID>
        return idField.call(box)
    }

private class PersonHandler(id: PathParameter) : Handler<Person> {
    val id by id

    override suspend fun CoroutineScope.compute(): Person {
        return Person("Test")
    }
}

private class ContentHandler(id: PathParameter) : Handler<Content> {
    val id by id

    override suspend fun CoroutineScope.compute(): Content {
        return Content("hello world", "1")
    }
}

private object PersonHandlerWithoutId : Handler<Person> {
    override suspend fun CoroutineScope.compute(): Person {
        return Person("Test")
    }
}

private object ContentHandlerWithoutId : Handler<Content> {
    override suspend fun CoroutineScope.compute(): Content {
        return Content("hello world", "1")
    }
}

private class Person(val name: String)

private class Content(val text: String, @Hidden val personId: String) : CustomType<Content> {
    override fun TypeDefinitionBuilder.definition(): TypeDefinition<Content> {
        return `object` {
            inferFromStructure()

            relationship<Person>("id") { personId }
        }
    }
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