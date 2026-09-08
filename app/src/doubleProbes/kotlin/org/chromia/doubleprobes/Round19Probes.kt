package org.chromia.doubleprobes

import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.rag.content.Content
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.query.Query
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import org.chromia.domain.ChromiaRepository
import org.chromia.tools.ToolStrategy
import java.lang.invoke.MethodHandleProxies
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 * THE ROUND-19 EVASION PROBES. Compiled, never run - the round-18 README beside
 * this file explains the source set.
 *
 * Round 18's eight probes are all substitutes for a PRODUCTION type, and the
 * structural scan catches every one because a substitute for a production type
 * has to name it: in its supertype list, in `java.lang.reflect.Proxy`'s constant
 * pool entry, or in an invokedynamic descriptor whose interface production
 * declares or accepts.
 *
 * Round 19 substitutes the collaborators that are NOT production's own types.
 * `productionSupertypeOf` returns the first supertype found in `mainClasses` and
 * ends any other branch, and `samTargets` is derived from what production
 * DECLARES or ACCEPTS AS A PARAMETER - so a substitute built on a THIRD-PARTY
 * interface that production only ever holds in a local variable is structurally
 * invisible, whichever way it is written. `ContentRetriever` is exactly that:
 * `RagStore.kt` builds one into a local (`val retriever: ContentRetriever = ...`)
 * and never takes one as a parameter, so it is in no method descriptor.
 *
 * The last two are the reflective forms: `proxyTypes` is exactly two names, so
 * `MethodHandleProxies.asInterfaceInstance` builds the same runtime substitute
 * without mentioning `Proxy` at all, and a class DEFINED FROM BYTES leaves no
 * class file in any scanned directory.
 *
 * `r19d6Control` is the SAME named class one token away, over a PRODUCTION seam -
 * the shape round 18's `D4b` already proves is caught. It is what makes each
 * probe above "one token away" rather than a different experiment.
 */
private val CANNED_CONTENT: List<Content> = listOf(Content.from("canned"))

private val R19_CANNED_RESULT: CallToolResult = CallToolResult(
    content = listOf(TextContent("canned")),
    structuredContent = null,
    isError = false
)

// ---------------------------------------------------------------------------
// r19d1 - a NAMED CLASS over a third-party seam, supertype on the next line.
//
// This is D4b's shape aimed at a library interface instead of a production one.
// The lexical layer needs the class name and the supertype on one line;
// the structural layer follows the supertype closure and stops as soon as a
// branch leaves the scanned tree without reaching production. ContentRetriever
// is production's own RAG seam - RagStore builds one - and it is neither.
// ---------------------------------------------------------------------------
class R19d1NamedClassOverAThirdPartySeam {
    private class Steady
        : ContentRetriever {
        override fun retrieve(query: Query): List<Content> = CANNED_CONTENT
    }

    fun retriever(): ContentRetriever = Steady()
}

// ---------------------------------------------------------------------------
// r19d2 - a SAM LAMBDA over a seam production never takes as a parameter.
//
// d6 works because `RagStore`'s constructor accepts an EmbeddingModel, so the
// type is in a production method descriptor and lands in `samTargets`.
// ContentRetriever appears in production exactly once, as the type of a LOCAL
// (`RagStore.kt`), so it is in no descriptor, is in no `samTargets`, and the
// invokedynamic descriptor the scan reads names a type it does not look for.
// ---------------------------------------------------------------------------
object R19d2SamLambdaOnANonParameterSeam {
    fun steadyRetriever(): ContentRetriever = ContentRetriever { CANNED_CONTENT }
}

// ---------------------------------------------------------------------------
// r19d4 - MethodHandleProxies, a substitute that never names Proxy.
//
// `asInterfaceInstance` builds the same runtime implementation of a single-method
// interface, and `proxyTypes` holds exactly two names, neither of which is
// `java/lang/invoke/MethodHandleProxies`. The seam here is EmbeddingModel - the
// one production DOES accept as a parameter, which d6 proves is covered when the
// substitute is a lambda - so the difference between caught and not caught is the
// builder, not the seam.
// ---------------------------------------------------------------------------
object R19d4MethodHandleProxy {
    @JvmStatic
    fun canned(segments: Any?): Response<List<Embedding>> =
        Response.from(listOf(Embedding(floatArrayOf(0.0f))))

    fun steadyModel(): EmbeddingModel = MethodHandleProxies.asInterfaceInstance(
        EmbeddingModel::class.java,
        MethodHandles.lookup().findStatic(
            R19d4MethodHandleProxy::class.java,
            "canned",
            MethodType.methodType(Response::class.java, Any::class.java)
        )
    )
}

// ---------------------------------------------------------------------------
// r19d5 - a class DEFINED AT RUNTIME from bytes.
//
// There is no class file for the scan to read: the substitute exists only in
// memory, and the class that builds it has java/lang/Object for a supertype, no
// production interface and no relevant invokedynamic. The bytes can live
// anywhere - a resource, a string, a generator.
// ---------------------------------------------------------------------------
object R19d5RuntimeDefinedClass {
    fun define(bytes: ByteArray): Class<*> =
        MethodHandles.lookup().defineHiddenClass(bytes, true).lookupClass()

    fun steadyRepository(bytes: ByteArray): ChromiaRepository =
        define(bytes).getDeclaredConstructor().newInstance() as ChromiaRepository
}

// ---------------------------------------------------------------------------
// r19d6 - THE CONTROL: the same named class, supertype on the next line, over a
// PRODUCTION seam. This is round 18's D4b restated, and it must be caught as
// SUPERTYPE. It is what makes r19d1 a SUBSTITUTION - third-party instead of
// production - rather than a different experiment.
// ---------------------------------------------------------------------------
class R19d6ControlNamedClassOverAProductionSeam {
    private class Steady
        : ToolStrategy {
        override val touchesLocalMachine: Boolean = false
        override suspend fun execute(
            request: CallToolRequest,
            repository: ChromiaRepository
        ): CallToolResult = R19_CANNED_RESULT
    }

    fun strategy(): ToolStrategy = Steady()
}
