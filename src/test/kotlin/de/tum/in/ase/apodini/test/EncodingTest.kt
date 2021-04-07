package de.tum.`in`.ase.apodini.test

import de.tum.`in`.ase.apodini.internal.reflection.TypeDefinitionInferenceManager
import de.tum.`in`.ase.apodini.test.example.URL
import de.tum.`in`.ase.apodini.test.utils.TestEncoder
import junit.framework.TestCase
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class EncodingTest : TestCase() {

    fun testEncodeString() {
        assertEquals(encode("Hello"), "Hello")
    }

    fun testEncodeBoolean() {
        assertEquals(encode(true), true)
    }

    fun testEncodeInt() {
        assertEquals(encode(42), 42)
    }

    fun testEncodeDouble() {
        assertEquals(encode(42.1), 42.1)
    }

    fun testEncodeObject() {
        assertEquals(encode(Foo("hello", 42)), mapOf("bar" to "hello", "baz" to 42))
    }

    fun testEncodeCustomScalar() {
        assertEquals(encode(URL("https://google.com")), "https://google.com")
    }

    fun testEncodeOptional() {
        assertEquals(encode<String?>(null), null)
        assertEquals(encode<String?>("hello"), "hello")
    }

    fun testArray() {
        assertEquals(encode(listOf(1, 2, 3)), listOf(1, 2, 3))
    }

    fun testEncodeEnum() {
        assertEquals(encode(TestEnum.One), "One")
    }

}

@OptIn(ExperimentalStdlibApi::class)
private inline fun <reified T> encode(value: T): Any? {
    return encode(value, typeOf<T>())
}

private fun <T> encode(value: T, type: KType): Any? {
    val manager = TypeDefinitionInferenceManager()
    val definition = manager.infer<T>(type)
    val encoder = TestEncoder()
    with(definition) {
        encoder.encode(value)
    }
    return encoder.value
}