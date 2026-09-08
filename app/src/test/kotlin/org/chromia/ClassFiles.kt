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
    /** Descriptors of every declared field. */
    val fieldDescriptors: List<String>,
    /**
     * Every object type named in a `LocalVariableTable` - the type a local was
     * DECLARED as, which is the only place a seam a method merely HOLDS appears
     * under its own name rather than under the concrete type it was built from.
     * Present whenever the class carries debug information (Kotlin and javac both
     * emit it by default); empty otherwise, which is why the seam set that reads
     * it also reads the constant pool and the descriptors.
     */
    val localVariableTypes: Set<String>,
    /**
     * `owner#name` for every `CONSTANT_Methodref` and `CONSTANT_InterfaceMethodref`
     * in the pool - what the class CALLS, as opposed to what it merely names. A
     * runtime substitute is manufactured by CALLING a JDK entry point
     * (`Proxy.newProxyInstance`, `MethodHandleProxies.asInterfaceInstance`,
     * `Lookup.defineHiddenClass`, `ClassLoader.defineClass`...), and the call has
     * to be in this set for it to happen at all.
     */
    val methodReferences: Set<String>,
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

    /** `0xCAFEBABE`, as the four bytes a class file starts with. */
    private val MAGIC = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())

    /**
     * A class file is a class file whatever it is called. Adversary round 19
     * (finding r19d5) pointed out that a substitute can be CHECKED IN as bytes
     * and defined at runtime, and `app/build/resources/test` is a directory the
     * test JVM loads from - so the scan reads every regular file and asks the
     * first four bytes, instead of trusting a `.class` suffix.
     */
    private fun isClassFile(file: Path): Boolean {
        if (file.toString().endsWith(".class")) return true
        return runCatching {
            Files.newInputStream(file).use { stream ->
                val head = ByteArray(4)
                var read = 0
                while (read < 4) {
                    val n = stream.read(head, read, 4 - read)
                    if (n < 0) return@use false
                    read += n
                }
                head.contentEquals(MAGIC)
            }
        }.getOrDefault(false)
    }

    /**
     * Every class file under [dir], keyed by internal name - by MAGIC, not by
     * suffix. Empty when [dir] is absent, which is the honest answer for a
     * source set that compiled nothing.
     */
    fun readTree(dir: Path): Map<String, ClassFacts> {
        if (!Files.isDirectory(dir)) return emptyMap()
        val files = Files.walk(dir).use { stream ->
            stream.filter { Files.isRegularFile(it) && isClassFile(it) }.sorted().toList()
        }
        return files.associate { file ->
            // A class file the reader cannot parse is NOT skipped: silence over
            // one is exactly the hole a checked-in substitute would sit in.
            val facts = runCatching { read(Files.readAllBytes(file)) }.getOrElse { failure ->
                throw IllegalStateException(
                    "the zero-doubles scan found a class file it could not read: ${file.fileName} " +
                        "(${failure.message}). It is not skipped - a substitute the reader cannot " +
                        "parse would be proven absent by nothing.",
                    failure
                )
            }
            facts.internalName to facts
        }
    }

    /** Every class file under all of [dirs], keyed by internal name. */
    fun readTrees(dirs: Iterable<Path>): Map<String, ClassFacts> {
        val all = LinkedHashMap<String, ClassFacts>()
        dirs.forEach { all.putAll(readTree(it)) }
        return all
    }

    /** The return type of a method descriptor, as an internal name, or null. */
    fun returnedType(descriptor: String): String? {
        val close = descriptor.lastIndexOf(')')
        if (close < 0) return null
        val returned = descriptor.substring(close + 1)
        return if (returned.startsWith("L") && returned.endsWith(";")) returned.drop(1).dropLast(1) else null
    }

    /**
     * Every object type named ANYWHERE in a descriptor, as internal names: the
     * parameters, the return type, a field's type, a local's type. `[` and the
     * primitive tags carry no name, and a name never contains `;`, so a single
     * left-to-right pass is exact rather than a heuristic.
     */
    fun objectTypes(descriptor: String): List<String> {
        val found = mutableListOf<String>()
        var i = 0
        while (i < descriptor.length) {
            if (descriptor[i] == 'L') {
                val end = descriptor.indexOf(';', i)
                if (end < 0) return found
                found += descriptor.substring(i + 1, end)
                i = end + 1
            } else {
                i++
            }
        }
        return found
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
        val nameAndTypeName = IntArray(poolCount)
        val nameAndTypeDescriptor = IntArray(poolCount)
        val dynamicNameAndType = IntArray(poolCount)
        val referenceOwner = IntArray(poolCount)
        val referenceNameAndType = IntArray(poolCount)

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
                9, 10, 11 -> {
                    referenceOwner[i] = input.readUnsignedShort()
                    referenceNameAndType[i] = input.readUnsignedShort()
                }
                12 -> {
                    nameAndTypeName[i] = input.readUnsignedShort()
                    nameAndTypeDescriptor[i] = input.readUnsignedShort()
                }
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

        val fieldDescriptors = mutableListOf<String>()
        val fieldCount = input.readUnsignedShort()
        repeat(fieldCount) {
            input.skipBytes(4) // access_flags, name_index
            utf[input.readUnsignedShort()]?.let { fieldDescriptors += it }
            skipAttributes()
        }

        val methodDescriptors = mutableListOf<String>()
        val localVariableTypes = mutableSetOf<String>()
        var abstractInstanceMethods = 0
        val methodCount = input.readUnsignedShort()
        repeat(methodCount) {
            val flags = input.readUnsignedShort()
            input.readUnsignedShort() // name
            val descriptor = utf[input.readUnsignedShort()]
            if (descriptor != null) methodDescriptors += descriptor
            if (flags and ACC_ABSTRACT != 0 && flags and ACC_STATIC == 0) abstractInstanceMethods++
            // Not `skipAttributes()`: the Code attribute carries the
            // LocalVariableTable, and a seam a method only HOLDS (RagStore's
            // `val retriever: ContentRetriever = ...`) is declared nowhere else.
            val attributeCount = input.readUnsignedShort()
            repeat(attributeCount) {
                val nameIndex = input.readUnsignedShort()
                val length = input.readInt()
                if (utf.getOrNull(nameIndex) == "Code") {
                    val code = ByteArray(length)
                    input.readFully(code)
                    localVariableTypes += localVariableDescriptors(code, utf).flatMap { objectTypes(it) }
                } else {
                    input.skipBytes(length)
                }
            }
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

        val methodReferences = (1 until poolCount).mapNotNull { index ->
            if (tags[index] != 10 && tags[index] != 11) {
                null
            } else {
                val owner = className(referenceOwner[index])
                val name = utf.getOrNull(nameAndTypeName.getOrElse(referenceNameAndType[index]) { 0 })
                if (owner != null && name != null) "$owner#$name" else null
            }
        }.toSet()

        return ClassFacts(
            internalName = thisClass,
            superName = superName,
            interfaces = interfaces,
            isInterface = accessFlags and ACC_INTERFACE != 0,
            referencedTypes = referenced,
            invokeDynamicTargets = indyTargets,
            methodDescriptors = methodDescriptors,
            fieldDescriptors = fieldDescriptors,
            localVariableTypes = localVariableTypes,
            methodReferences = methodReferences,
            abstractInstanceMethods = abstractInstanceMethods,
            sourceFile = sourceFile
        )
    }

    /**
     * The descriptors in a Code attribute's `LocalVariableTable`, or an empty
     * list when the class carries no debug information.
     */
    private fun localVariableDescriptors(code: ByteArray, utf: Array<String?>): List<String> {
        val input = DataInputStream(ByteArrayInputStream(code))
        return runCatching {
            input.readUnsignedShort() // max_stack
            input.readUnsignedShort() // max_locals
            input.skipBytes(input.readInt()) // the bytecode itself
            input.skipBytes(input.readUnsignedShort() * 8) // the exception table
            val descriptors = mutableListOf<String>()
            repeat(input.readUnsignedShort()) {
                val nameIndex = input.readUnsignedShort()
                val length = input.readInt()
                if (utf.getOrNull(nameIndex) == "LocalVariableTable") {
                    repeat(input.readUnsignedShort()) {
                        input.skipBytes(4) // start_pc, length
                        input.readUnsignedShort() // name_index
                        utf.getOrNull(input.readUnsignedShort())?.let { descriptors += it }
                        input.readUnsignedShort() // index
                    }
                } else {
                    input.skipBytes(length)
                }
            }
            descriptors.toList()
        }.getOrDefault(emptyList())
    }
}
