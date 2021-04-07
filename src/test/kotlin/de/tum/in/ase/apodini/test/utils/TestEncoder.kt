package de.tum.`in`.ase.apodini.test.utils

import de.tum.`in`.ase.apodini.types.Encoder

class TestEncoder : Encoder {
    var value: Any? = null

    override fun encodeString(string: String) {
        value = string
    }

    override fun encodeBoolean(boolean: Boolean) {
        value = boolean
    }

    override fun encodeInt(int: Int) {
        value = int
    }

    override fun encodeDouble(double: Double) {
        value = double
    }

    override fun encodeNull() {
        value = null
    }

    override fun keyed(init: Encoder.KeyedContainer.() -> Unit) {
        @Suppress("UNCHECKED_CAST")
        value = TestKeyedEncoder((value as? MutableMap<String, Any?>) ?: mutableMapOf()).also(init).map
    }

    override fun unKeyed(init: Encoder.UnKeyedContainer.() -> Unit) {
        @Suppress("UNCHECKED_CAST")
        value = TestUnKeyedEncoder((value as? MutableList<Any?>) ?: mutableListOf()).also(init).list
    }
}

private class TestKeyedEncoder(val map: MutableMap<String, Any?>) : Encoder.KeyedContainer {
    override fun encode(key: String, init: Encoder.() -> Unit) {
        map[key] = TestEncoder().also(init).value
    }
}

private class TestUnKeyedEncoder(val list: MutableList<Any?>) : Encoder.UnKeyedContainer {
    override fun encode(init: Encoder.() -> Unit) {
        list.add(TestEncoder().also(init).value)
    }
}