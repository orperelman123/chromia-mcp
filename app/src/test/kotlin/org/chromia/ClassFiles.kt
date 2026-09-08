package org.chromia

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * The parts of a compiled `.class` file [NoTestDoublesTest] needs to say what a
 * class IS rather than how it was spelled.
 *
 * Adversary round 18 (section 6) wrote eight test doubles one token away from the
 * spellings the zero-doubles scan looked for and six were invisible: `by`
 * delegation between the supertype and the brace, a subclass of a production
 * CLASS rather than a listed interface, an import alias, a supertype on the next
 * line, a `java.lang.reflect.Proxy` whose substitute type is not in the source at
 * all, and a SAM lambda on a seam the lambda list omitted. Every one of those is
 * a spelling. In the class file they are all the same thing: a supertype list, a
 * constant-pool reference, an `invokedynamic` descriptor.
 *
 * This is a reader, not a loader. Nothing here calls `Class.forName` on the
 * classes being scanned - a test tree that cannot be loaded (missing runtime
 * dependency, a class that runs work in its initializer) must still be
 * scannable, and loading a double to ask what it is would be a strange way to
 * prove there are none.
 */
data class ClassFacts(
    /** `org/chromia/Foo$Bar`, the JVM's own spelling. */
    val internalName: String,
    val superName: String?,
    val interfaces: List<String>,
    val isInterface: Boolean,
    /** Every `CONSTANT_Class` in the pool - what the class names to do its work. */
    val referencedTypes: Set<String>,
    /**
     * The functional-interface type of every `invokedynamic` in the class: the
     * RETURN type of the call site's descriptor, which for a Kotlin or Java SAM
     * conversion is the interface being implemented. A lambda compiled this way
     * produces no class of its own, so this is the only structural trace it
     * leaves.
     */
    val invokeDynamicTargets: List<String>,
    /** Descriptors of every declared method, `<init>` included. */
    val methodDescriptors: List<String>,
    /** Abstract, non-static methods - one of them is what makes an interface SAM. */
    val abstractInstanceMethods: Int,
    /** The `SourceFile` attribute, so a finding can name the .kt it came from. */
    val sourceFile: String?
) {
    val binaryName: String get() = internalName.replace('/', '.')

    /** `org/chromia/Foo$Bar` -> `Foo$Bar`. */
    val simpleName: String get() = internalName.substringAfterLast('/')
}

object ClassFiles {

    private const val ACC_INTERFACE = 0x0200
    private const val ACC_STATIC = 0x0008
    private const val ACC_ABSTRACT = 0x0400

    /** Every `.class` under [dir], keyed by internal name. Empty when [dir] is absent. */
    fun readTree(dir: Path): Map<String, ClassFacts> {
        if (!Files.isDirectory(dir)) return emptyMap()
        val files = Files.walk(dir).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".class") }.sorted().toList()
        }
        return files.associate { file ->
            val facts = read(Files.readAllBytes(file))
            facts.internalName to facts
        }
    }

    /** The return type of a method descriptor, as an internal name, or null. */
    fun returnedType(descriptor: String): String? {
        val close = descriptor.lastIndexOf(')')
        if (close < 0) return null
        val returned = descriptor.substring(close + 1)
        return if (returned.startsWith("L") && returned.endsWith(";")) returned.drop(1).dropLast(1) else null
    }

    /** Every object type named in a descriptor's PARAMETER list, as internal names. */
    fun parameterTypes(descriptor: String): List<String> {
        val open = descriptor.indexOf('(')
        val close = descriptor.lastIndexOf(')')
        if (open < 0 || close < open) return emptyList()
        val found = mutableListOf<String>()
        var i = open + 1
        while (i < close) {
            when (descriptor[i]) {
                'L' -> {
                    val end = descriptor.indexOf(';', i)
                    if (end < 0) return found
                    found += descriptor.substring(i + 1, end)
                    i = end + 1
                }
                else -> i++
            }
        }
        return found
    }

    @Suppress("CyclomaticComplexMethod")
    fun read(bytes: ByteArray): ClassFacts {
        val input = DataInputStream(ByteArrayInputStream(bytes))
        require(input.readInt() == -0x35014542) { "not a class file" } // 0xCAFEBABE
        input.readUnsignedShort() // minor
        input.readUnsignedShort() // major

        val poolCount = input.readUnsignedShort()
        val tags = IntArray(poolCount)
        val utf = arrayOfNulls<String>(poolCount)
        val classNameIndex = IntArray(poolCount)
        val nameAndTypeDescriptor = IntArray(poolCount)
        val dynamicNameAndType = IntArray(poolCount)

        var i = 1
        while (i < poolCount) {
            val tag = input.readUnsignedByte()
            tags[i] = tag
            when (tag) {
                1 -> utf[i] = input.readUTF()
                3, 4 -> input.skipBytes(4)
                5, 6 -> { input.skipBytes(8); i++ }
                7 -> classNameIndex[i] = input.readUnsignedShort()
                8, 16, 19, 20 -> input.skipBytes(2)
                9, 10, 11 -> input.skipBytes(4)
                12 -> { input.readUnsignedShort(); nameAndTypeDescriptor[i] = input.readUnsignedShort() }
                15 -> input.skipBytes(3)
                17, 18 -> { input.skipBytes(2); dynamicNameAndType[i] = input.readUnsignedShort() }
                else -> error("unknown constant pool tag $tag at $i")
            }
            i++
        }

        fun className(index: Int): String? =
            if (index in 1 until poolCount && tags[index] == 7) utf[classNameIndex[index]] else null

        val accessFlags = input.readUnsignedShort()
        val thisClass = className(input.readUnsignedShort()) ?: error("no this_class")
        val superIndex = input.readUnsignedShort()
        val superName = if (superIndex == 0) null else className(superIndex)
        val interfaceCount = input.readUnsignedShort()
        val interfaces = (0 until interfaceCount).mapNotNull { className(input.readUnsignedShort()) }

        fun skipAttributes() {
            val count = input.readUnsignedShort()
            repeat(count) {
                input.readUnsignedShort()
                val length = input.readInt()
                input.skipBytes(length)
            }
        }

        val fieldCount = input.readUnsignedShort()
        repeat(fieldCount) {
            input.skipBytes(6)
            skipAttributes()
        }

        val methodDescriptors = mutableListOf<String>()
        var abstractInstanceMethods = 0
        val methodCount = input.readUnsignedShort()
        repeat(methodCount) {
            val flags = input.readUnsignedShort()
            input.readUnsignedShort() // name
            val descriptor = utf[input.readUnsignedShort()]
            if (descriptor != null) methodDescriptors += descriptor
            if (flags and ACC_ABSTRACT != 0 && flags and ACC_STATIC == 0) abstractInstanceMethods++
            skipAttributes()
        }

        var sourceFile: String? = null
        val classAttributes = input.readUnsignedShort()
        repeat(classAttributes) {
            val nameIndex = input.readUnsignedShort()
            val length = input.readInt()
            if (utf[nameIndex] == "SourceFile" && length == 2) {
                sourceFile = utf[input.readUnsignedShort()]
            } else {
                input.skipBytes(length)
            }
        }

        val referenced = (1 until poolCount).mapNotNull { index ->
            if (tags[index] == 7) utf[classNameIndex[index]] else null
        }.toSet()

        val indyTargets = (1 until poolCount).mapNotNull { index ->
            if (tags[index] != 18) {
                null
            } else {
                val nat = dynamicNameAndType[index]
                utf[nameAndTypeDescriptor[nat]]?.let { returnedType(it) }
            }
        }

        return ClassFacts(
            internalName = thisClass,
            superName = superName,
            interfaces = interfaces,
            isInterface = accessFlags and ACC_INTERFACE != 0,
            referencedTypes = referenced,
            invokeDynamicTargets = indyTargets,
            methodDescriptors = methodDescriptors,
            abstractInstanceMethods = abstractInstanceMethods,
            sourceFile = sourceFile
        )
    }
}
