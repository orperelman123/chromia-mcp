# The twelve evasion probes: round 18's eight and round 19's four

This source set is **compiled and never run**. Nothing here is a test, nothing
here is on the test runtime classpath, and nothing in `app/src/test/kotlin`
imports it.

Adversary round 18 (section 6) wrote eight candidate test doubles the way a
person in a hurry writes one and ran `NoTestDoublesTest`'s own detectors,
ported verbatim, over them: **six of the eight were invisible**, because every
detector was a source regex and every one of the six was one token away from the
spelling it looked for - `by` delegation between the supertype and the brace, a
subclass of a production CLASS rather than one of the listed interfaces, an
import alias, a supertype on the next line over an unlisted seam, a
`java.lang.reflect.Proxy` whose substitute type is not in the source at all, and
a SAM lambda on a seam the lambda list omitted.

The scan is now STRUCTURAL: it reads the compiled classes, where `by` delegation,
nesting, aliases and multi-line supertypes are all the same thing. These files
are what proves it, so they have to be **compiled**, and a double compiled into
the test tree would still be a double in the suite. Hence a source set of their
own: `NoTestDoublesTest` asserts zero structural sites over the test trees and
asserts that every probe here is caught.

`d7` is the CONTROL - the shape the old `ANONYMOUS` regex was written for. It
must be caught by the lexical layer as well as the structural one; each of the
other seven is one token away from it.

## Round 19's four (`Round19Probes.kt`)

Round 18's eight all substitute a type production OWNS, which is why the first
structural scan caught them all. Round 19 substituted the collaborators
production does NOT own, and walked past that scan four times:

- `r19d1` a named class over `ContentRetriever` - langchain4j's retrieval
  interface, the RAG seam `RagStore` builds into a local. The supertype closure
  ended as soon as it left production's own types.
- `r19d2` the same seam as a SAM lambda. `samTargets` was what production
  DECLARES or ACCEPTS AS A PARAMETER, and `ContentRetriever` is neither.
- `r19d4` `MethodHandleProxies.asInterfaceInstance` over `EmbeddingModel` - the
  same runtime substitute, built without ever naming `Proxy`.
- `r19d5` `MethodHandles.Lookup.defineHiddenClass` from bytes, which leaves no
  class file at all - and the two directories the bytes could be checked into
  (`app/build/classes/java/test`, `app/build/resources/test`) were outside the
  scan's `classesRoot`.

`r19d6` is round 19's CONTROL: the identical named class over a PRODUCTION seam,
so each of the four is one substitution away and not a different experiment.

All four are caught now. A seam is every interface or abstract class production's
bytecode names; the manufacturing entry points are read off the constant pool as
CALLS; and the scanned trees are the `test` task's own runtime classpath minus
the dependency jars and production's output, with class files recognised by their
magic rather than by a suffix. `Round19DoubleEvasionTest` pins all five rows and
`NoTestDoublesTest.everyRound19EvasionProbeIsCaught` pins them against the
shipped detectors.

If you are looking for a place to put a double: there isn't one. These exist to
make the scan's silence mean something, and the assertion over the real test tree
is still zero.
