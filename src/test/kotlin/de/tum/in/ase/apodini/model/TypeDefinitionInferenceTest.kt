package de.tum.`in`.ase.apodini.model

import de.tum.`in`.ase.apodini.internal.reflection.TypeDefinitionInferenceManager
import de.tum.`in`.ase.apodini.types.*
import de.tum.`in`.ase.apodini.types.Array
import junit.framework.TestCase
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
class TypeDefinitionInferenceTest : TestCase() {

    fun testSimpleObjectDefinition() {
        val manager = TypeDefinitionInferenceManager()
        when (val definition = manager.infer<Foo>(typeOf<Foo>())) {
            is Object -> {
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
        val properties = definition.properties.toTypedArray()
        assertEquals(definition.documentation, "An example type")
        assertEquals(properties.size, 1)
        assertEquals(properties[0].documentation, "Example Property")
    }

    fun testHidden() {
        val manager = TypeDefinitionInferenceManager()
        val definition = manager.infer<Foo2>(typeOf<Foo2>()) as Object<*>
        val properties = definition.properties.toTypedArray()
        assertEquals(definition.documentation, "An example type")
        assertEquals(properties.size, 1)
        assertEquals(properties[0].documentation, "Example Property")
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
        val arrayDefinition = manager.infer<kotlin.Array<String>>(typeOf<List<String>>()) as Array<*>
        assertEquals(arrayDefinition.definition, StringType)
    }

}

private class Foo(val bar: String, val baz: Int)

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

private class Post(val text: String, val author: User)
private class User(val name: String, val posts: Iterable<Post>)