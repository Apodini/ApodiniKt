package de.tum.`in`.ase.apodini.test

import de.tum.`in`.ase.apodini.internal.reflection.TypeDefinitionInferenceManager
import de.tum.`in`.ase.apodini.types.*
import de.tum.`in`.ase.apodini.types.Array
import de.tum.`in`.ase.apodini.types.Enum
import junit.framework.TestCase
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
class TypeDefinitionInferenceTest : TestCase() {

    fun testSimpleObjectDefinition() {
        val manager = TypeDefinitionInferenceManager()
        when (val definition = manager.infer<Foo>(typeOf<Foo>())) {
            is Object -> {
                assertEquals(definition.name, "Foo")
                val properties = definition.properties.toTypedArray()
                assertEquals(properties.size, 2)

                assertEquals(properties[0].name, "bar")
                assertEquals(properties[0].definition, StringType)

                assertEquals(properties[1].name, "baz")
                assertEquals(properties[1].definition, IntType)
            }
            else -> fail("Expected an object")
        }
    }

    fun testDocumentation() {
        val manager = TypeDefinitionInferenceManager()
        val definition = manager.infer<Foo2>(typeOf<Foo2>()) as Object<*>
        assertEquals(definition.name, "Foo2")

        val properties = definition.properties.toTypedArray()
        assertEquals(definition.documentation, "An example type")
        assertEquals(properties.size, 1)
        assertEquals(properties[0].documentation, "Example Property")
    }

    fun testHidden() {
        val manager = TypeDefinitionInferenceManager()
        val definition = manager.infer<Foo3>(typeOf<Foo3>()) as Object<*>
        assertEquals(definition.name, "Foo3")

        val properties = definition.properties.toTypedArray()
        assertEquals(properties.size, 1)
        assertEquals(properties[0].name, "bar")
        assertEquals(properties[0].definition, StringType)
    }

    fun testRecursiveObject() {
        val manager = TypeDefinitionInferenceManager()
        val userDefinition = manager.infer<User>(typeOf<User>()) as Object<User>

        val userProperties = userDefinition.properties.toTypedArray()
        assertEquals(userProperties.size, 2)

        assertEquals(userProperties[0].name, "name")
        assertEquals(userProperties[0].definition, StringType)

        assertEquals(userProperties[1].name, "posts")
        val authorArray = userProperties[1].definition as Array<*>
        val authorDefinition = authorArray.definition as Object<*>
        val authorProperties = authorDefinition.properties.toTypedArray()
        assertEquals(authorProperties.size, 2)

        assertEquals(authorProperties[0].name, "author")
        assertEquals(authorProperties[0].definition, userDefinition)

        assertEquals(authorProperties[1].name, "text")
        assertEquals(authorProperties[1].definition, StringType)
    }

    fun testOptionalObject() {
        val manager = TypeDefinitionInferenceManager()
        val nullableDefinition = manager.infer<Foo?>(typeOf<Foo?>()) as Nullable<*>
        val fooDefinition = nullableDefinition.definition as Object<*>
        val properties = fooDefinition.properties.toTypedArray()
        assertEquals(properties.size, 2)

        assertEquals(properties[0].name, "bar")
        assertEquals(properties[0].definition, StringType)

        assertEquals(properties[1].name, "baz")
        assertEquals(properties[1].definition, IntType)
    }

    fun testOptionalString() {
        val manager = TypeDefinitionInferenceManager()
        val nullableDefinition = manager.infer<String?>(typeOf<String?>()) as Nullable<*>
        assertEquals(nullableDefinition.definition, StringType)
    }

    fun testObjectList() {
        val manager = TypeDefinitionInferenceManager()
        val arrayDefinition = manager.infer<List<Foo>>(typeOf<List<Foo>>()) as Array<*>
        val definition = arrayDefinition.definition as Object<*>
        val properties = definition.properties.toTypedArray()
        assertEquals(properties.size, 2)

        assertEquals(properties[0].name, "bar")
        assertEquals(properties[0].definition, StringType)

        assertEquals(properties[1].name, "baz")
        assertEquals(properties[1].definition, IntType)
    }

    fun testStringList() {
        val manager = TypeDefinitionInferenceManager()
        val arrayDefinition = manager.infer<List<String>>(typeOf<List<String>>()) as Array<*>
        assertEquals(arrayDefinition.definition, StringType)
    }

    fun testStringArray() {
        val manager = TypeDefinitionInferenceManager()
        val arrayDefinition = manager.infer<kotlin.Array<String>>(typeOf<kotlin.Array<String>>()) as Array<*>
        assertEquals(arrayDefinition.definition, StringType)
    }

    fun testEnum() {
        val manager = TypeDefinitionInferenceManager()
        val definition = manager.infer<TestEnum>(typeOf<TestEnum>()) as Enum<TestEnum>
        assertEquals(definition.name, "TestEnum")
        assertEquals(definition.cases.toList(), listOf("One", "Two", "Three"))
    }

    fun testCustomScalar() {
        val manager = TypeDefinitionInferenceManager()
        val definition = manager.infer<URL>(typeOf<URL>()) as Scalar<*, *>
        assertEquals(definition.name, "URL")
        assertEquals(definition.kind, StringType)
    }

}

internal class Foo(val bar: String, val baz: Int)

@Documented("An example type")
private class Foo2(
    @Documented("Example Property")
    val bar: String
)

private class Foo3(
    val bar: String,
    @Hidden
    val baz: Int
)

enum class TestEnum {
    One, Two, Three
}

private class URL(val urlString: String) : CustomType<URL> {
    override fun TypeDefinitionBuilder.definition(): TypeDefinition<URL> {
        return string { urlString }
    }
}

private class Post(val text: String, val author: User)
private class User(val name: String, val posts: Iterable<Post>)