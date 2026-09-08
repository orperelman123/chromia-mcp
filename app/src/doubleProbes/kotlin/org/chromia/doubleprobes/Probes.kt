package org.chromia.doubleprobes

import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import org.chromia.domain.ChromiaRepository as Backend
import org.chromia.domain.ChromiaRepository
import org.chromia.domain.JsonResult
import org.chromia.domain.NetworkResult
import org.chromia.tools.BaseToolStrategy
import org.chromia.tools.ToolStrategy
import java.lang.reflect.Proxy

/**
 * THE EIGHT ROUND-18 EVASION PROBES. Compiled, never run - see README.md beside
 * this file.
 *
 * Each function or class below is a substitute for a production collaborator,
 * written the way round 18 wrote it. Seven of them were invisible to a source
 * regex; `d7` is the control that was not. `NoTestDoublesTest` asserts that the
 * structural scan catches every one, and that the real test tree has none.
 *
 * The round wrote `d1` and `d7` over `ChromiaRepository`. `d1` keeps that type -
 * `by` delegation supplies the other twenty-odd methods for free, which is
 * exactly the point of the probe. The control has to write its members out, so
 * it is over `ToolStrategy`, the production interface with the shortest member
 * list; the SHAPE being controlled for - an anonymous object over a production
 * type with and without `by` between the supertype and the brace - is identical.
 */
private val CANNED: CallToolResult = CallToolResult(
    content = listOf(TextContent("canned")),
    structuredContent = null,
    isError = false
)

private val CANNED_TOP_HOLDERS: JsonResult = NetworkResult.Error("canned")

// ---------------------------------------------------------------------------
// d1 - an anonymous object with `by` delegation.
//
// The lexical ANONYMOUS detector is `object\s*:\s*([\w.]+)\s*[({]`: it wants the
// supertype followed immediately by `{` or `(`. Kotlin's delegation puts
// `by real` in between, and an anonymous object has no name for the
// IMPLEMENTS_INLINE detector to match either. Structurally this is a class whose
// interface list holds `org.chromia.domain.ChromiaRepository`, exactly like d7.
// ---------------------------------------------------------------------------
fun d1AnonymousObjectByDelegation(real: ChromiaRepository): ChromiaRepository =
    object : ChromiaRepository by real {
        override suspend fun getAssetTopHolders(
            assetId: String,
            network: String?,
            limit: Int?,
            filters: org.chromia.domain.AssetFilters
        ): JsonResult = CANNED_TOP_HOLDERS
    }

// ---------------------------------------------------------------------------
// d2 - a nested subclass of a production CLASS.
//
// `seamTypes` lists interfaces; the concrete production classes are not in it,
// and the name carries none of the eleven trigger words. Structurally this is a
// class whose SUPERCLASS is production code, which is the same finding.
// ---------------------------------------------------------------------------
class D2NestedSubclassOfAProductionClass {
    private class SteadyStrategy : BaseToolStrategy() {
        override val touchesLocalMachine: Boolean = false
        override suspend fun execute(
            request: CallToolRequest,
            repository: ChromiaRepository
        ): CallToolResult = CANNED
    }

    fun strategy(): ToolStrategy = SteadyStrategy()
}

// ---------------------------------------------------------------------------
// d3 - an import alias on the seam type.
//
// Every lexical detector is keyed on the SPELLING of the type; `import ... as
// Backend` changes the spelling and nothing else. The compiler resolves the
// alias, so the compiled interface list is `org.chromia.domain.ChromiaRepository`
// either way.
// ---------------------------------------------------------------------------
class D3ImportAliasOnTheSeamType(real: Backend) {
    private class Steady(real: Backend) : Backend by real {
        override suspend fun getAllAssets(network: String?): JsonResult = CANNED_TOP_HOLDERS
    }

    val repository: Backend = Steady(real)
}

// ---------------------------------------------------------------------------
// d4 - a supertype on the NEXT LINE, over a seam the lexical scan lists.
//
// IMPLEMENTS_INLINE reads one line and IMPLEMENTS_CONTINUED wants a line that
// begins with the `)` closing a constructor. A class with no constructor
// parameters whose supertype sits on the next line is neither. Round 18 caught
// this one by luck - SAM_LAMBDA matched `TxPoster {` on the supertype line while
// looking for a lambda - and d4b is the same file over a seam that list omits.
// ---------------------------------------------------------------------------
class D4SupertypeOnTheNextLine(private val real: ChromiaRepository) {
    private class Steady(real: ChromiaRepository)
        : ChromiaRepository by real {
        override suspend fun getAllAssets(network: String?): JsonResult = CANNED_TOP_HOLDERS
    }

    fun repository(): ChromiaRepository = Steady(real)
}

// ---------------------------------------------------------------------------
// d4b - the same shape over a seam the lexical scan does NOT list.
//
// Nothing matches the supertype line at all: `ToolStrategy` is not in
// `seamTypes` and not in the SAM lambda list. Structurally it is a class whose
// interface list holds a production type, like every other probe here.
// ---------------------------------------------------------------------------
class D4bSupertypeOnTheNextLineUnlistedSeam {
    private class Steady
        : ToolStrategy {
        override val touchesLocalMachine: Boolean = false
        override suspend fun execute(
            request: CallToolRequest,
            repository: ChromiaRepository
        ): CallToolResult = CANNED
    }

    fun strategy(): ToolStrategy = Steady()
}

// ---------------------------------------------------------------------------
// d5 - java.lang.reflect.Proxy.
//
// The substitute type is not in the source at all: it is manufactured by the JVM
// from the interface, and the canned answers live in a lambda no source rule
// covers. The compiled class file still has to NAME `java/lang/reflect/Proxy` in
// its constant pool to call it, which is what the structural scan reads.
// ---------------------------------------------------------------------------
object D5ReflectionProxy {
    fun steadyRepository(): ChromiaRepository = Proxy.newProxyInstance(
        ChromiaRepository::class.java.classLoader,
        arrayOf(ChromiaRepository::class.java)
    ) { _, _, _ -> CANNED_TOP_HOLDERS } as ChromiaRepository
}

// ---------------------------------------------------------------------------
// d6 - a SAM lambda on a seam the lambda list omits.
//
// `SAM_LAMBDA` named five `fun interface`s by hand. This one substitutes the
// embedding model - the collaborator `BagOfWordsEmbeddingModel` stood in for
// before the zero-doubles pass deleted it - written as a lambda, which Kotlin
// compiles to an `invokedynamic` and therefore to NO class at all. The
// structural scan reads the invokedynamic's own descriptor instead, and derives
// the set of substitutable single-method interfaces from the production API
// rather than from a list.
// ---------------------------------------------------------------------------
object D6SamLambdaOnAnUnlistedSeam {
    fun steadyModel(): EmbeddingModel = EmbeddingModel { segments ->
        Response.from(segments.map { Embedding(floatArrayOf(0.0f)) })
    }
}

// ---------------------------------------------------------------------------
// d7 - THE CONTROL: a plain anonymous object over a production type.
//
// This is d1 with `by real` removed, which is the shape the lexical ANONYMOUS
// detector was written for. It must be caught by BOTH layers; each probe above
// is one token away from it.
// ---------------------------------------------------------------------------
fun d7ControlAPlainAnonymousObject(): ToolStrategy = object : ToolStrategy {
    override val touchesLocalMachine: Boolean = false
    override suspend fun execute(
        request: CallToolRequest,
        repository: ChromiaRepository
    ): CallToolResult = CANNED
}
